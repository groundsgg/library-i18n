package gg.grounds.i18n

import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.minimessage.MiniMessage

private const val BUNDLE = "gg.grounds.i18n.testmessages"

private enum class PaletteMessage(override val id: String) : MessageKey {
    ON_PALETTE("palette.on")
}

class PaletteTest {

    private val mini = MiniMessage.builder().editTags { it.resolver(Palette.TAGS) }.build()

    private fun colourOf(component: Component) =
        component.color() ?: component.children().firstNotNullOfOrNull { it.color() }

    @Test
    fun `a semantic tag resolves to its token`() {
        assertEquals(Palette.DANGER, colourOf(mini.deserialize("<danger>gone")))
        assertEquals(Palette.WARNING, colourOf(mini.deserialize("<warning>careful")))
        assertEquals(Palette.TEXT_MUTED, colourOf(mini.deserialize("<muted>quiet")))
    }

    @Test
    fun `the accent and its deep sibling are different colours`() {
        assertEquals(Palette.ACCENT, colourOf(mini.deserialize("<accent>click")))
        assertEquals(Palette.ACCENT_DEEP, colourOf(mini.deserialize("<accent-deep>brand")))
        assertEquals(false, Palette.ACCENT == Palette.ACCENT_DEEP)
    }

    @Test
    fun `standard colours still parse - no bundle breaks on upgrade`() {
        assertEquals(NamedTextColor.RED, colourOf(mini.deserialize("<red>legacy")))
    }

    @Test
    fun `a bundle gets the palette without wiring anything`() {
        val translations = Translations.forBundle(BUNDLE, javaClass.classLoader)
        val rendered = translations.render(PaletteMessage.ON_PALETTE, Locale.ENGLISH)
        assertEquals(Palette.DANGER, colourOf(rendered))
    }

    @Test
    fun `danger is the warm salmon, not a vivid red`() {
        // Over a near-black ground a vivid red does not clear APCA Lc 60. If this ever equals
        // NamedTextColor.RED, someone "fixed" the palette by eye.
        assertEquals(false, Palette.DANGER == NamedTextColor.RED)
        assertEquals("#f9a49a", Palette.DANGER.asHexString())
    }
}
