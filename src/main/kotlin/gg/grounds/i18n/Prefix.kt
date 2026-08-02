package gg.grounds.i18n

import net.kyori.adventure.text.Component

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
class Prefix private constructor(private val component: Component) {

    /** [message] with this prefix in front of it, or [message] itself for [NONE]. */
    fun applyTo(message: Component): Component =
        if (component == Component.empty()) message
        else Component.empty().append(component).append(message)

    companion object {
        /**
         * `[Grounds] ` — every system message on every platform.
         *
         * The word is [Palette.ACCENT_DEEP], not [Palette.ACCENT]: the bright mint means "you can
         * click this", and a tag on every single line would spend it on decoration. Deliberately
         * not configurable per plugin — a prefix each plugin picks is the state this replaced.
         */
        @JvmField
        val GROUNDS: Prefix =
            Prefix(
                Component.text("[", Palette.TEXT_FAINT)
                    .append(Component.text("Grounds", Palette.ACCENT_DEEP))
                    .append(Component.text("] ", Palette.TEXT_FAINT))
            )

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
    }
}
