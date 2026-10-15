package gg.grounds.i18n

import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer

class SystemMetadataTest {
    private val plain = PlainTextComponentSerializer.plainText()

    @Test
    fun `every context supports every level without changing its label or message subtree`() {
        val colours =
            mapOf(
                SystemLevel.INFO to 0xC1B9AA,
                SystemLevel.SUCCESS to 0xA4CE87,
                SystemLevel.WARNING to 0xF0C36A,
                SystemLevel.ERROR to 0xF28B82,
            )
        val content = Component.text("Retry").clickEvent(ClickEvent.suggestCommand("/retry"))
        for (category in SystemCategory.entries) {
            val label = category.label(Locale.ENGLISH)
            for ((level, colour) in colours) {
                val line = SystemMessages.format(content, category, level = level)
                assertEquals("[Grounds] $label | Retry", plain.serialize(line))
                assertEquals(TextColor.color(colour), findLabel(line, label)?.color())
                assertSame(content, line.children().last())
            }
        }
    }

    @Test
    fun `typed keys default to general info and plain keys remain supported`() {
        val typed =
            object : SystemMessageKey {
                override val id = "greeting"
            }
        assertEquals(SystemCategory.GENERAL, typed.category)
        assertEquals(SystemLevel.INFO, typed.level)
        val untyped =
            object : MessageKey {
                override val id = "greeting"
            }
        for (key in listOf(typed, untyped)) {
            val line =
                SystemMessageRenderer.TEXT_ONLY.render(
                    Audience.empty(),
                    key,
                    Component.text("Hello"),
                    Locale.GERMAN,
                )
            assertEquals("[Grounds] Allgemein | Hello", plain.serialize(line))
        }
    }

    @Test
    fun `layout may opt into a translated level without replacing its context`() {
        val line =
            SystemMessages.format(
                Component.text("Hello"),
                SystemCategory.FRIENDS,
                locale = Locale.forLanguageTag("zz"),
                level = SystemLevel.ERROR,
            )
        assertEquals("Friends (Error) · [Grounds] › Hello", plain.serialize(line))
    }

    @Test
    fun `both host renderers retain the key context and error level`() {
        val key =
            object : SystemMessageKey {
                override val id = "greeting"
                override val category = SystemCategory.FRIENDS
                override val level = SystemLevel.ERROR
            }
        val content = Component.text("Hello")
        for (renderer in
            listOf(
                SystemMessageRenderer.TEXT_ONLY,
                SystemMessageRenderer.withLogoReadiness { true },
            )) {
            val line = renderer.render(Audience.empty(), key, content, Locale.GERMAN)
            assertTrue(plain.serialize(line).contains("Freunde | Hello"))
            assertEquals(TextColor.color(0xF28B82), findLabel(line, "Freunde")?.color())
            assertSame(content, line.children().last())
        }
    }

    @Test
    fun `all supported locales translate contexts and levels independently`() {
        for (language in listOf("en", "de", "es", "fr", "tr")) {
            val locale = Locale.forLanguageTag(language)
            for (category in SystemCategory.entries) assertTrue(category.label(locale).isNotBlank())
            for (level in SystemLevel.entries) assertTrue(level.label(locale).isNotBlank())
        }
    }

    private fun findLabel(component: Component, label: String): Component? {
        if (component is TextComponent && component.content() == label) return component
        return component.children().firstNotNullOfOrNull { findLabel(it, label) }
    }
}
