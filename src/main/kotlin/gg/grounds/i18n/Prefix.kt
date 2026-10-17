package gg.grounds.i18n

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.format.TextDecoration

/**
 * The tag in front of a line the platform says to a player.
 *
 * Chat is one surface, written by a dozen plugins. Without a shared tag a player cannot tell a
 * friend request from a match countdown from another player talking, and every plugin invents its
 * own answer — `[Party]`, `[STAFF]`, nothing at all. [GROUNDS] is that shared answer, and
 * [Translations] applies it on `send` by default, so a plugin conforms by doing nothing and
 * deviating is an explicit, greppable call to [Translations.sendUnprefixed].
 *
 * What gets a prefix is *the platform talking*: confirmations, errors, announcements. What does not
 * is *players talking* — chat lines, private messages, staff chat. Those are conversations and are
 * shaped by whoever owns the channel, which today is `plugin-chat`.
 */
class Prefix
private constructor(private val component: Component, private val bodyColour: TextColor? = null) {

    /** [message] with this prefix in front of it, or [message] itself for [NONE]. */
    fun applyTo(message: Component): Component =
        if (this === GROUNDS) SystemMessages.format(message)
        else if (component == Component.empty()) message
        else
            Component.empty()
                .append(component)
                .append(bodyColour?.let(message::colorIfAbsent) ?: message)

    companion object {
        /**
         * Shared branded category header from the system layout bundle. [SystemMessages] owns its
         * visual tokens and [Translations] selects the recipient locale.
         */
        @JvmField val GROUNDS: Prefix = Prefix(Component.empty())

        /**
         * No prefix.
         *
         * For a bundle that holds conversation lines rather than system output — `plugin-chat`'s
         * own message shapes — not as a way out of the house style.
         */
        @JvmField val NONE: Prefix = Prefix(Component.empty())

        /**
         * A prefix of your own.
         *
         * Exists for tests and for a channel that genuinely is not the platform speaking. If you
         * reach for it to give a plugin its own brand tag, that is the thing this class exists to
         * stop.
         */
        @JvmStatic fun of(component: Component): Prefix = Prefix(component)

        /**
         * A game or system-area line: `SurvivalGames › The grace period ends in 10 seconds.`
         *
         * [name] is drawn bold in [Palette.ACCENT_DEEP], followed by a [Palette.TEXT_FAINT] `›`.
         * The body gets [Palette.TEXT_MUTED] as its base colour, so a bundle line can start with
         * plain text; a body that sets its own colour (`<success>…`) keeps it. No brand and no logo
         * — the category is the whole tag.
         */
        @JvmStatic fun category(name: String): Prefix = category(Component.text(name))

        /**
         * [category] with a [Component] name, e.g. a [Component.translatable] that the platform
         * translates per recipient. A colour or bold state the name already sets is kept.
         */
        @JvmStatic
        fun category(name: Component): Prefix =
            Prefix(
                Component.empty()
                    .append(
                        name
                            .colorIfAbsent(Palette.ACCENT_DEEP)
                            .decorationIfAbsent(TextDecoration.BOLD, TextDecoration.State.TRUE)
                    )
                    .append(Component.text(" "))
                    .append(Component.text("›", Palette.TEXT_FAINT))
                    .append(Component.text(" ")),
                Palette.TEXT_MUTED,
            )
    }
}
