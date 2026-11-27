package gg.grounds.i18n

import java.util.Locale
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration

/** Presentation selected using the recipient's confirmed resource-pack capabilities. */
enum class SystemPrefixStyle {
    TEXT,
    LOGO,
    BADGE,
}

/** Row and column order match the platform pack's system badge atlas. */
internal object SystemBadgeGlyphs {
    private val FONT = Key.key("grounds:system_badges")
    private val LANGUAGES = listOf("en", "de", "es", "fr", "tr")
    private val CATEGORIES =
        listOf(
            SystemCategory.GENERAL,
            SystemCategory.FRIENDS,
            SystemCategory.PARTY,
            SystemCategory.MATCH,
            SystemCategory.STORY,
        )
    private const val FIRST_GLYPH = 0xE000
    private const val GAP = '\uE019'

    fun prefix(category: SystemCategory, locale: Locale): Component {
        val row = LANGUAGES.indexOf(locale.language).coerceAtLeast(0)
        val column = CATEGORIES.indexOf(category)
        val glyph = (FIRST_GLYPH + row * CATEGORIES.size + column).toChar()
        return Component.text("$glyph$GAP", NamedTextColor.WHITE)
            .font(FONT)
            .decoration(TextDecoration.BOLD, false)
            .decoration(TextDecoration.ITALIC, false)
    }
}
