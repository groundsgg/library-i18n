package gg.grounds.i18n

import java.lang.System.Logger.Level
import java.util.Locale
import java.util.MissingResourceException
import java.util.ResourceBundle
import java.util.concurrent.ConcurrentHashMap
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.ComponentLike
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver

/**
 * Renders a [MessageKey] into the language its reader speaks.
 *
 * One instance per plugin, over that plugin's own resource bundle. Keeping bundles per-plugin means
 * a wording change is a change in the plugin that emits it, not a release of this library followed
 * by a version bump everywhere.
 *
 * ```kotlin
 * private val messages = Translations.forBundle("gg.grounds.social.messages", javaClass.classLoader)
 *
 * messages.send(player, SocialMessage.FRIEND_REQUEST_SENT, "target" to target.username)
 * ```
 *
 * Bundle values are MiniMessage, so colour lives with the translator rather than in the call site.
 *
 * [send] puts [Prefix.GROUNDS] in front of the line. The three entry points differ only in that:
 * [send] is the platform talking to a player and is prefixed, [sendUnprefixed] is a conversation
 * line and is not, and [render] returns the bare component for lore, titles and inventory names —
 * none of which are chat and none of which want a tag.
 */
class Translations
private constructor(
    private val baseName: String,
    private val classLoader: ClassLoader,
    private val fallback: Locale,
    private val miniMessage: MiniMessage,
    private val localeResolver: LocaleResolver,
    private val prefix: Prefix,
) {

    private val warnedMissing = ConcurrentHashMap.newKeySet<String>()

    /** The language [audience] reads, or [fallback] if the audience did not say. */
    fun localeOf(audience: Audience): Locale = localeResolver.localeOf(audience) ?: fallback

    /**
     * Renders [key] in [locale], without a prefix.
     *
     * Each argument becomes a MiniMessage placeholder: `"target" to name` fills `<target>`. A
     * [ComponentLike] argument is inserted as-is; anything else is inserted as **unparsed** text,
     * so a player called `<red>x</red>` cannot smuggle markup into a message about them. Pass a
     * [Component] when you actually want formatting.
     *
     * Placeholder names must be lowercase MiniMessage tag names (`[a-z0-9_-]`).
     */
    fun render(key: MessageKey, locale: Locale, vararg args: Pair<String, Any>): Component {
        val pattern = lookup(key, locale) ?: return missing(key)
        return miniMessage.deserialize(pattern, TagResolver.resolver(args.map(::resolverFor)))
    }

    /** Renders [key] in the language [audience] reads, without a prefix. */
    fun render(key: MessageKey, audience: Audience, vararg args: Pair<String, Any>): Component =
        render(key, localeOf(audience), *args)

    /**
     * Renders [key] for [audience] and sends it, prefixed.
     *
     * This is the platform talking, so the line carries the platform's tag — `[Grounds]` unless the
     * instance was built with another [Prefix]. Nothing at the call site opts in; that is the
     * point.
     */
    fun send(audience: Audience, key: MessageKey, vararg args: Pair<String, Any>) {
        audience.sendMessage(prefix.applyTo(render(key, localeOf(audience), *args)))
    }

    /**
     * Renders [key] for [audience] and sends it **without** the prefix.
     *
     * For lines that are not the platform speaking: a chat message, a private message, staff chat.
     * A system message sent through here is a bug a reviewer can see, which is why the exception is
     * a different method rather than a flag.
     */
    fun sendUnprefixed(audience: Audience, key: MessageKey, vararg args: Pair<String, Any>) {
        audience.sendMessage(render(key, localeOf(audience), *args))
    }

    private fun lookup(key: MessageKey, locale: Locale): String? =
        stringOrNull(locale, key.id) ?: stringOrNull(fallback, key.id)

    private fun stringOrNull(locale: Locale, id: String): String? =
        try {
            ResourceBundle.getBundle(baseName, locale, classLoader, NO_JVM_DEFAULT_FALLBACK)
                .getString(id)
        } catch (_: MissingResourceException) {
            null
        }

    /**
     * A key with no translation anywhere renders as the key itself. Sending nothing would look like
     * a dropped packet to the player and like nothing at all to us; the raw key is ugly on purpose
     * and reaches the log once.
     */
    private fun missing(key: MessageKey): Component {
        if (warnedMissing.add(key.id)) {
            LOG.log(Level.WARNING, "no translation for '${key.id}' in bundle '$baseName'")
        }
        return Component.text(key.id)
    }

    private fun resolverFor(arg: Pair<String, Any>): TagResolver =
        when (val value = arg.second) {
            is ComponentLike -> Placeholder.component(arg.first, value)
            else -> Placeholder.unparsed(arg.first, value.toString())
        }

    companion object {
        private val LOG: System.Logger = System.getLogger("gg.grounds.i18n")

        /**
         * Stops [ResourceBundle] falling back to the JVM's default locale. Without it a server
         * booted in a German container serves German to everyone whose own language is missing,
         * which looks like a translation bug and is a hosting detail.
         */
        private val NO_JVM_DEFAULT_FALLBACK: ResourceBundle.Control =
            ResourceBundle.Control.getNoFallbackControl(ResourceBundle.Control.FORMAT_PROPERTIES)

        /**
         * @param baseName bundle base name in class-name form, e.g. `gg.grounds.social.messages`
         *   for `src/main/resources/gg/grounds/social/messages.properties`.
         * @param classLoader **the calling plugin's** loader. Paper and Velocity give every plugin
         *   its own; passing someone else's finds no bundle.
         * @param fallback language of the untranslated source bundle (`messages.properties`).
         * @param prefix what [send] puts in front of the line. Leave it alone unless the bundle
         *   holds conversation lines rather than system output.
         * @param miniMessage defaults to an instance that resolves the [Palette] tags (`<danger>`,
         *   `<muted>`, …) on top of the standard ones. Replacing it takes the palette away.
         */
        @JvmStatic
        @JvmOverloads
        fun forBundle(
            baseName: String,
            classLoader: ClassLoader,
            fallback: Locale = Locale.ENGLISH,
            localeResolver: LocaleResolver = LocaleResolver.FROM_AUDIENCE,
            miniMessage: MiniMessage = PALETTE_MINI_MESSAGE,
            prefix: Prefix = Prefix.GROUNDS,
        ): Translations =
            Translations(baseName, classLoader, fallback, miniMessage, localeResolver, prefix)

        /**
         * MiniMessage with the Grounds semantic colours added.
         *
         * Standard tags keep working, so no bundle breaks. The point is that `<danger>` is now the
         * shorter thing to write than `<red>`, and the wrong one is visible next to the right one.
         */
        private val PALETTE_MINI_MESSAGE: MiniMessage =
            MiniMessage.builder().editTags { it.resolver(Palette.TAGS) }.build()
    }
}
