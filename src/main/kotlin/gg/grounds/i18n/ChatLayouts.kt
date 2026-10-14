package gg.grounds.i18n

import java.util.Locale
import java.util.ResourceBundle
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.minimessage.tag.Tag
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver

/** Localizable component ordering; callers supply safe visual sender and message components. */
object ChatLayouts {
    private val SECONDARY: TextColor = TextColor.color(0xC1B9AA)

    @JvmStatic
    @JvmOverloads
    fun player(sender: Component, message: Component, locale: Locale = Locale.ENGLISH): Component {
        val bundle =
            ResourceBundle.getBundle(
                "gg.grounds.i18n.system",
                locale,
                javaClass.classLoader,
                ResourceBundle.Control.getNoFallbackControl(
                    ResourceBundle.Control.FORMAT_PROPERTIES
                ),
            )
        return MiniMessage.miniMessage()
            .deserialize(
                bundle.getString("layout.player"),
                Placeholder.component("sender", sender),
                Placeholder.component("message", message),
                TagResolver.resolver("secondary", Tag.styling(SECONDARY)),
            )
    }
}
