package gg.grounds.i18n

import kotlin.test.Test
import kotlin.test.assertEquals
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.TranslatableComponent
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer

class CategoryPrefixTest {

    private val palette = MiniMessage.builder().editTags { it.resolver(Palette.TAGS) }.build()

    private fun plain(component: Component) =
        PlainTextComponentSerializer.plainText().serialize(component)

    private fun mini(component: Component) = MiniMessage.miniMessage().serialize(component)

    /** The colour [text] is drawn in, inherited from ancestors as the client does. */
    private fun colourOf(
        component: Component,
        text: String,
        inherited: TextColor? = null,
    ): TextColor? {
        val colour = component.color() ?: inherited
        if (component is TextComponent && component.content().contains(text)) return colour
        return component.children().firstNotNullOfOrNull { colourOf(it, text, colour) }
    }

    @Test
    fun `category names use the shared system layout rather than a private header`() {
        val line = Prefix.category("SurvivalGames").applyTo(Component.text("Grace ends."))
        assertEquals("[Grounds] SurvivalGames | Grace ends.", plain(line))
    }

    @Test
    fun `known system categories are localized when a locale is supplied`() {
        val line =
            Prefix.category("Friends").applyTo(Component.text("Retry"), java.util.Locale.GERMAN)
        assertEquals("[Grounds] Freunde | Retry", plain(line))
    }

    @Test
    fun `a plain body gets muted as its base colour`() {
        val body = palette.deserialize("The grace period ends in <text>10</text> seconds.")
        val line = Prefix.category("SurvivalGames").applyTo(body)
        assertEquals(Palette.TEXT_MUTED, colourOf(line, "The grace period ends in"))
        assertEquals(Palette.TEXT, colourOf(line, "10"))
        assertEquals(Palette.TEXT_MUTED, colourOf(line, "seconds."))
    }

    @Test
    fun `a body with its own colour keeps it`() {
        val body = palette.deserialize("<success>You won!")
        val line = Prefix.category("SurvivalGames").applyTo(body)
        assertEquals(Palette.SUCCESS, colourOf(line, "You won!"))
    }

    @Test
    fun `a translatable name gets the category styling`() {
        val name = Component.translatable("grounds.category.queue")
        val line = Prefix.category(name).applyTo(Component.text("Joined."))
        fun descendants(node: Component): Sequence<Component> = sequence {
            yield(node)
            node.children().forEach { yieldAll(descendants(it)) }
        }
        val rendered = descendants(line).filterIsInstance<TranslatableComponent>().single()
        assertEquals("grounds.category.queue", rendered.key())
    }

    @Test
    fun `send through Translations uses the category prefix`() {
        val sent = mutableListOf<Component>()
        val audience =
            object : Audience {
                override fun sendMessage(message: Component) {
                    sent += message
                }
            }
        val translations =
            Translations.forBundle(
                "gg.grounds.i18n.testmessages",
                javaClass.classLoader,
                prefix = Prefix.category("SurvivalGames"),
            )
        translations.sendRendered(
            audience,
            object : MessageKey {
                override val id = "unused"
            },
            Component.text("Fight!"),
        )
        assertEquals("[Grounds] SurvivalGames | Fight!", plain(sent.single()))
        assertEquals(Palette.TEXT_MUTED, sent.single().children()[1].color())
    }

    @Test
    fun `game category follows verified per-send readiness and keeps its status`() {
        var ready = false
        val sent = mutableListOf<Component>()
        val audience =
            object : Audience {
                override fun sendMessage(message: Component) {
                    sent += message
                }
            }
        val translations =
            Translations.forBundleWithSystemRenderer(
                "gg.grounds.i18n.testmessages",
                javaClass.classLoader,
                SystemMessageRenderer.withLogoReadiness { ready },
                prefix = Prefix.category("GetDown"),
            )
        val key =
            object : SystemMessageKey {
                override val id = "greeting"
                override val category = SystemCategory.MATCH
                override val level = SystemLevel.ERROR
            }
        val body =
            Component.text("Retry")
                .clickEvent(net.kyori.adventure.text.event.ClickEvent.suggestCommand("/retry"))
        translations.sendRendered(audience, key, body)
        ready = true
        translations.sendRendered(audience, key, body)
        ready = false
        translations.sendRendered(audience, key, body)
        assertEquals(
            listOf(
                "[Grounds] GetDown | Retry",
                "\uE100 · GetDown | Retry",
                "[Grounds] GetDown | Retry",
            ),
            sent.map(::plain),
        )
        sent.forEach { line ->
            assertEquals(TextColor.color(0xF28B82), colourOf(line, "GetDown"))
            assertEquals(body.clickEvent(), line.children().last().clickEvent())
        }
        assertEquals(
            "Hello, Ada!",
            plain(translations.render(key, java.util.Locale.ENGLISH, "player" to "Ada")),
        )
    }
}
