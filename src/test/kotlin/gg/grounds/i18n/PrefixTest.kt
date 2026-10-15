package gg.grounds.i18n

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer

class PrefixTest {

    private fun plain(component: Component) =
        PlainTextComponentSerializer.plainText().serialize(component)

    @Test
    fun `the platform tag reads as one word with a trailing space`() {
        val line = Prefix.GROUNDS.applyTo(Component.text("Friend request sent."))
        assertEquals("[Grounds] General | Friend request sent.", plain(line))
    }

    @Test
    fun `no prefix returns the very same component`() {
        val message = Component.text("Ada: hello")
        assertSame(message, Prefix.NONE.applyTo(message))
    }

    @Test
    fun `the message keeps its own colour`() {
        val message = Component.text("Player not found.", NamedTextColor.RED)
        val line = Prefix.GROUNDS.applyTo(message)
        assertEquals(NamedTextColor.RED, line.children()[1].color())
    }

    /**
     * The wrapper is an uncoloured empty root, not the prefix itself: appending to a coloured
     * component makes every child inherit that colour, which would paint the whole line brand mint.
     */
    @Test
    fun `the wrapping root carries no colour of its own`() {
        val line = Prefix.GROUNDS.applyTo(Component.text("Player not found."))
        assertEquals(null, line.color())
    }

    @Test
    fun `a custom prefix is used as given`() {
        val prefix = Prefix.of(Component.text("[Test] "))
        assertEquals("[Test] hi", plain(prefix.applyTo(Component.text("hi"))))
    }
}
