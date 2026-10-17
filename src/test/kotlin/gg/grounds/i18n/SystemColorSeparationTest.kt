package gg.grounds.i18n

import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.event.HoverEvent
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.minimessage.MiniMessage

class SystemColorSeparationTest {
    private val levelColors =
        mapOf(
            SystemLevel.INFO to "#f3f0ea",
            SystemLevel.SUCCESS to "#a4ce87",
            SystemLevel.WARNING to "#f0c36a",
            SystemLevel.ERROR to "#f28b82",
        )
    private val categoryColors =
        mapOf(
            SystemCategory.GENERAL to "#c1b9aa",
            SystemCategory.FRIENDS to "#79c9be",
            SystemCategory.PARTY to "#b8a0da",
            SystemCategory.MATCH to "#8eb8e8",
            SystemCategory.STORY to "#e7b84b",
        )

    @Test
    fun `context color stays fixed while level colors the body in every locale and pack mode`() {
        val message =
            Component.text("Result", NamedTextColor.RED)
                .clickEvent(ClickEvent.suggestCommand("/retry"))
        for (locale in listOf(Locale.ENGLISH, Locale.GERMAN)) {
            for (ready in listOf(false, true)) {
                for ((category, categoryColor) in categoryColors) {
                    for ((level, bodyColor) in levelColors) {
                        val line = SystemMessages.format(message, category, ready, locale, level)
                        assertEquals(
                            categoryColor,
                            find(line, category.label(locale)).color()?.asHexString(),
                        )
                        assertEquals(bodyColor, line.children().last().color()?.asHexString())
                        assertEquals(message.clickEvent(), line.children().last().clickEvent())
                        if (ready) assertEquals(NamedTextColor.WHITE, find(line, "\uE100").color())
                    }
                }
            }
        }
        assertEquals(NamedTextColor.RED, message.color())
    }

    @Test
    fun `named module prefixes have stable independent colors through both host renderers`() {
        val names =
            mapOf(
                "GetDown" to "#dda16f",
                "BedWars" to "#dd9ea8",
                "Duel" to "#c4a1e3",
                "Keystone" to "#67d9ec",
                "SurvivalGames" to "#bbc99b",
                "Lobby" to "#d8c7a0",
                "Chat" to "#85c5cb",
                "Settings" to "#b8c2d1",
            )
        for (renderer in
            listOf(
                SystemMessageRenderer.TEXT_ONLY,
                SystemMessageRenderer.withLogoReadiness { true },
            )) {
            for ((name, color) in names) {
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
                        renderer,
                        prefix = Prefix.category(name),
                    )
                for ((level, bodyColor) in levelColors) {
                    val key =
                        object : SystemMessageKey {
                            override val id = "greeting"
                            override val category = SystemCategory.MATCH
                            override val level = level
                        }
                    messages.sendRendered(audience, key, Component.text("Result"))
                    assertEquals(color, find(sent, name).color()?.asHexString())
                    assertEquals(bodyColor, sent.children().last().color()?.asHexString())
                }
            }
        }
    }

    @Test
    fun `level rebases legacy body color while preserving rich highlights events and fonts`() {
        val target =
            Component.text("Ada", Palette.TEXT)
                .clickEvent(ClickEvent.suggestCommand("/msg Ada "))
                .hoverEvent(HoverEvent.showText(Component.text("Player")))
        val head = Component.text("\uE101", NamedTextColor.WHITE).font(Key.key("grounds:chat"))
        val message = Component.text("Could not reach ", Palette.DANGER).append(target).append(head)
        val rendered = SystemMessages.format(message, level = SystemLevel.ERROR).children().last()
        assertEquals("#f28b82", rendered.color()?.asHexString())
        assertEquals(listOf(target, head), rendered.children())
        assertSame(target, rendered.children()[0])
        assertSame(head, rendered.children()[1])
    }

    @Test
    fun `translated body defaults to warm white but unprefixed rendering retains authored color`() {
        val body = MiniMessage.miniMessage().deserialize("<red>Old base <white>Ada</white>")
        val rendered = SystemMessages.format(body).children().last()
        assertEquals(Palette.TEXT, rendered.color())
        assertEquals(body.children(), rendered.children())
        assertSame(body, Prefix.NONE.applyTo(body))
    }

    @Test
    fun `explicit custom context color is independent of error status`() {
        val context = Component.text("Custom", NamedTextColor.LIGHT_PURPLE)
        val line =
            SystemMessages.formatNamed(
                Component.text("Failure"),
                context,
                level = SystemLevel.ERROR,
            )
        assertEquals(NamedTextColor.LIGHT_PURPLE, find(line, "Custom").color())
        assertEquals(TextColor.color(0xf28b82), line.children().last().color())
    }

    private fun find(component: Component, text: String): Component =
        findOrNull(component, text) ?: error("Missing component: $text")

    private fun findOrNull(component: Component, text: String): Component? =
        if (component is TextComponent && component.content() == text) component
        else component.children().firstNotNullOfOrNull { findOrNull(it, text) }
}
