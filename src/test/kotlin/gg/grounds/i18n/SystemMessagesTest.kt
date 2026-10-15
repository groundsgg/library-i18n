package gg.grounds.i18n

import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer

class SystemMessagesTest {
    @Test
    fun `localized layout can reorder brand and category`() {
        val line =
            SystemMessages.format(Component.text("Hello"), locale = Locale.forLanguageTag("zz"))
        assertEquals("General (Info) · [Grounds] › Hello", plain(line))
    }

    @Test
    fun `host renderer checks readiness anew for each send`() {
        var ready = false
        var sent: Component = Component.empty()
        val audience =
            object : Audience {
                override fun sendMessage(message: Component) {
                    sent = message
                }
            }
        val messages =
            Translations.forBundleWithSystemRenderer(
                "gg.grounds.i18n.testmessages",
                javaClass.classLoader,
                SystemMessageRenderer.withLogoReadiness { ready },
            )
        val key =
            object : MessageKey {
                override val id = "greeting"
            }
        messages.send(audience, key, "player" to "Ada")
        assertTrue(plain(sent).startsWith("[Grounds] General | "))
        ready = true
        messages.send(audience, key, "player" to "Ada")
        assertTrue(plain(sent).startsWith("\uE100 · General | "))
        ready = false
        messages.sendRendered(audience, key, Component.text("Bye"))
        assertEquals("[Grounds] General | Bye", plain(sent))
    }

    private fun plain(component: Component) =
        PlainTextComponentSerializer.plainText().serialize(component)

    @Test
    fun `German errors carry the localized category and preserve clickable content`() {
        val content = Component.text("Retry").clickEvent(ClickEvent.suggestCommand("/queue"))
        val line =
            SystemMessages.format(
                content,
                SystemCategory.FRIENDS,
                locale = Locale.GERMAN,
                level = SystemLevel.ERROR,
            )
        assertEquals("[Grounds] Freunde | Retry", plain(line))
        assertSame(content, line.children().last())
        assertEquals(content.clickEvent(), line.children().last().clickEvent())
    }

    @Test
    fun `only an explicitly ready logo uses the custom font`() {
        val fallback = SystemMessages.format(Component.text("Hello"))
        assertFalse(plain(fallback).any { it.code in 0xE100..0xE108 })
        val graphical = SystemMessages.format(Component.text("Hello"), logoReady = true)
        assertEquals("\uE100 · General | Hello", plain(graphical))
        fun hasChatFont(component: Component): Boolean =
            component.font()?.toString() == "grounds:chat" ||
                component.children().any(::hasChatFont)
        assertTrue(hasChatFont(graphical))
    }

    @Test
    fun `bundle sends use typed category while render remains unprefixed`() {
        val key =
            object : SystemMessageKey {
                override val id = "greeting"
                override val category = SystemCategory.PARTY
                override val level = SystemLevel.SUCCESS
            }
        var sent: Component = Component.empty()
        val audience =
            object : Audience {
                override fun sendMessage(message: Component) {
                    sent = message
                }
            }
        val messages = Translations.forBundle("gg.grounds.i18n.testmessages", javaClass.classLoader)
        messages.send(audience, key, "player" to "<red>Ada</red>")
        assertEquals("[Grounds] Party | Hello, <red>Ada</red>!", plain(sent))
        assertEquals("Hello, Ada!", plain(messages.render(key, Locale.ENGLISH, "player" to "Ada")))
    }
}
