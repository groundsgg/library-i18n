package gg.grounds.i18n

import java.util.Locale
import java.util.ResourceBundle
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder

/** Shared Minecraft system line; a graphical prefix requires explicit per-recipient readiness. */
object SystemMessages {
    private val GOLD = TextColor.color(0xE7B84B)
    private val SECONDARY = TextColor.color(0xC1B9AA)
    private val FONT = Key.key("grounds:chat")
    private val LAYOUT = MiniMessage.miniMessage()

    @JvmStatic
    @JvmOverloads
    fun format(
        message: Component,
        category: SystemCategory = SystemCategory.GENERAL,
        logoReady: Boolean = false,
        locale: Locale = Locale.ENGLISH,
        level: SystemLevel = SystemLevel.INFO,
    ): Component =
        formatNamed(
            message,
            Component.text(category.label(locale), SystemMessageColors.category(category)),
            logoReady,
            locale,
            level,
        )

    /** A game/area context shares the exact same template, status treatment and pack gating. */
    @JvmStatic
    @JvmOverloads
    fun formatNamed(
        message: Component,
        category: Component,
        logoReady: Boolean = false,
        locale: Locale = Locale.ENGLISH,
        level: SystemLevel = SystemLevel.INFO,
    ): Component {
        val levelColor = SystemMessageColors.level(level)
        val bundle =
            ResourceBundle.getBundle(
                "gg.grounds.i18n.system",
                locale,
                javaClass.classLoader,
                ResourceBundle.Control.getNoFallbackControl(
                    ResourceBundle.Control.FORMAT_PROPERTIES
                ),
            )
        val logo =
            Component.text("\uE100", NamedTextColor.WHITE)
                .font(FONT)
                .decoration(TextDecoration.BOLD, false)
                .decoration(TextDecoration.ITALIC, false)
        val prefix =
            LAYOUT.deserialize(
                    bundle.getString(
                        if (logoReady) "layout.graphical-prefix" else "layout.text-prefix"
                    ),
                    Placeholder.component("brand", Component.text(bundle.getString("brand"), GOLD)),
                    Placeholder.component(
                        "category",
                        category.colorIfAbsent(SystemMessageColors.category(SystemCategory.GENERAL)),
                    ),
                    Placeholder.component("level", Component.text(level.label(locale), levelColor)),
                    Placeholder.component("logo", if (logoReady) logo else Component.empty()),
                )
                .colorIfAbsent(SECONDARY)
        // Level owns the body base color, including legacy bundle root colors. Explicit child
        // highlights, fonts and interaction survive; recoloring the whole subtree would tint
        // assets.
        return Component.empty().append(prefix).append(message.color(levelColor))
    }
}
