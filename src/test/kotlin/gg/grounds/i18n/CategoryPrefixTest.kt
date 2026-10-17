package gg.grounds.i18n

import kotlin.test.Test
import kotlin.test.assertEquals
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.TranslatableComponent
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.format.TextDecoration
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
    fun `renders the category bold in accent-deep with a faint separator`() {
        val line = Prefix.category("SurvivalGames").applyTo(Component.text("Grace ends."))
        assertEquals("SurvivalGames › Grace ends.", plain(line))
        assertEquals(
            "<#38b4ab><bold>SurvivalGames</bold></#38b4ab> <#bcb4ad>›</#bcb4ad> " +
                "<#d5cec6>Grace ends.",
            mini(line),
        )
    }

    @Test
    fun `the header carries the palette colours and bold only on the name`() {
        val header = Prefix.category("SurvivalGames").applyTo(Component.text("x")).children()[0]
        val (name, space, separator) = header.children()
        assertEquals(Palette.ACCENT_DEEP, name.color())
        assertEquals(TextDecoration.State.TRUE, name.decoration(TextDecoration.BOLD))
        assertEquals(TextDecoration.State.NOT_SET, space.decoration(TextDecoration.BOLD))
        assertEquals(Palette.TEXT_FAINT, separator.color())
        assertEquals(TextDecoration.State.NOT_SET, separator.decoration(TextDecoration.BOLD))
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
        val rendered = line.children()[0].children()[0]
        assertEquals("grounds.category.queue", (rendered as TranslatableComponent).key())
        assertEquals(Palette.ACCENT_DEEP, rendered.color())
        assertEquals(TextDecoration.State.TRUE, rendered.decoration(TextDecoration.BOLD))
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
        assertEquals("SurvivalGames › Fight!", plain(sent.single()))
        assertEquals(Palette.TEXT_MUTED, sent.single().children()[1].color())
    }
}
