package gg.grounds.i18n

import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer

class ChatLayoutsTest {
    @Test
    fun `locale bundle can reorder the complete player layout`() {
        val line =
            ChatLayouts.player(
                Component.text("Ada"),
                Component.text("hello"),
                Locale.forLanguageTag("zz"),
            )
        assertEquals("hello ‹ Ada", PlainTextComponentSerializer.plainText().serialize(line))
    }

    @Test
    fun `player layout inserts components safely and preserves interaction`() {
        val sender = Component.text("<red>Ada</red>")
        val message = Component.text("hello").clickEvent(ClickEvent.suggestCommand("/reply"))
        val line = ChatLayouts.player(sender, message, Locale.GERMAN)
        assertEquals(
            "<red>Ada</red> › hello",
            PlainTextComponentSerializer.plainText().serialize(line),
        )
        fun clickable(c: Component): Boolean =
            c.clickEvent() == message.clickEvent() || c.children().any(::clickable)
        kotlin.test.assertTrue(clickable(line))
    }

    @Test
    fun `player layout colours only its separator`() {
        val message = Component.text("hello").clickEvent(ClickEvent.suggestCommand("/reply"))
        val line = ChatLayouts.player(Component.text("Ada"), message)

        assertEquals(null, line.color())
        assertTrue(descendants(line).any { it == message })
        assertEquals(null, message.color())
        val separator =
            descendants(line).filterIsInstance<TextComponent>().single { it.content() == " › " }
        assertEquals("#c1b9aa", separator.color()?.asHexString()?.lowercase())
    }

    private fun descendants(component: Component): Sequence<Component> = sequence {
        yield(component)
        component.children().forEach { yieldAll(descendants(it)) }
    }
}
