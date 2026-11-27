package gg.grounds.i18n

import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.event.HoverEvent

class SystemBadgeTest {
    @Test
    fun `standard badges match the pack atlas and preserve the body`() {
        val body =
            Component.text("body")
                .clickEvent(ClickEvent.runCommand("/test"))
                .hoverEvent(HoverEvent.showText(Component.text("hover")))
        listOf("en", "de", "es", "fr", "tr", "zz").forEachIndexed { row, language ->
            SystemCategory.entries.forEachIndexed { column, category ->
                val line =
                    SystemMessages.format(
                        body,
                        category,
                        SystemPrefixStyle.BADGE,
                        Locale.forLanguageTag(language),
                        SystemLevel.ERROR,
                    )
                val prefix = line.children().first() as TextComponent
                val index = (if (row == 5) 0 else row) * 5 + column
                assertEquals("${(0xE000 + index).toChar()}\uE019", prefix.content())
                assertEquals(Key.key("grounds:system_badges"), prefix.font())
                assertEquals(
                    body.color(SystemMessageColors.level(SystemLevel.ERROR)),
                    line.children().last(),
                )
            }
        }
    }

    @Test
    fun `legacy and named prefixes still render`() {
        val key =
            object : SystemMessageKey {
                override val id = "test"
                override val category = SystemCategory.MATCH
            }
        val message = Component.text("body")
        val named = Prefix.category("BedWars").key(key)
        val badge = SystemMessageRenderer.withPrefixStyle { SystemPrefixStyle.BADGE }
        val logo = SystemMessageRenderer.withLogoReadiness { true }
        assertEquals(
            logo.render(Audience.empty(), named, message, Locale.ENGLISH),
            badge.render(Audience.empty(), named, message, Locale.ENGLISH),
        )
        val text =
            SystemMessageRenderer.TEXT_ONLY.render(Audience.empty(), key, message, Locale.ENGLISH)
        assertNull(text.font())
        val standard = Prefix.category("Party").key(key)
        val party = badge.render(Audience.empty(), standard, message, Locale.ENGLISH)
        assertEquals("\uE002\uE019", (party.children().first() as TextComponent).content())
    }
}
