package gg.grounds.i18n

import java.util.Locale
import net.kyori.adventure.text.format.TextColor

/** Minecraft Mini-CD: context identifies the sender; level colors the message body. */
object SystemMessageColors {
    @JvmStatic
    fun category(category: SystemCategory): TextColor =
        TextColor.color(
            when (category) {
                SystemCategory.GENERAL -> 0xC1B9AA
                SystemCategory.FRIENDS -> 0x79C9BE
                SystemCategory.PARTY -> 0xB8A0DA
                SystemCategory.MATCH -> 0x8EB8E8
                SystemCategory.STORY -> 0xE7B84B
            }
        )

    /** Stable context identifiers, never translated labels or status inferred from message text. */
    @JvmStatic
    fun namedCategory(name: String): TextColor {
        SystemCategory.entries
            .firstOrNull { it.name.equals(name.trim(), ignoreCase = true) }
            ?.let {
                return category(it)
            }
        return when (name.trim().lowercase(Locale.ROOT)) {
            "getdown" -> TextColor.color(0xDDA16F)
            "bedwars" -> TextColor.color(0xDD9EA8)
            "duel" -> TextColor.color(0xC4A1E3)
            "keystone" -> TextColor.color(0x67D9EC)
            "survivalgames" -> TextColor.color(0xBBC99B)
            "lobby" -> TextColor.color(0xD8C7A0)
            "chat" -> TextColor.color(0x85C5CB)
            "settings" -> TextColor.color(0xB8C2D1)
            else -> category(SystemCategory.GENERAL)
        }
    }

    @JvmStatic
    fun level(level: SystemLevel): TextColor =
        when (level) {
            SystemLevel.INFO -> Palette.TEXT
            SystemLevel.SUCCESS -> TextColor.color(0xA4CE87)
            SystemLevel.WARNING -> TextColor.color(0xF0C36A)
            SystemLevel.ERROR -> TextColor.color(0xF28B82)
        }
}
