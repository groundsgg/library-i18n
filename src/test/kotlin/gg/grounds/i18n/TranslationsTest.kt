package gg.grounds.i18n

import java.util.Locale
import java.util.ResourceBundle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.identity.Identity
import net.kyori.adventure.pointer.Pointers
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer

private const val BUNDLE = "gg.grounds.i18n.testmessages"

private enum class TestMessage(override val id: String) : MessageKey {
    GREETING("greeting"),
    ONLY_IN_SOURCE("only-in-source"),
    ABSENT("nowhere.at.all"),
}

private class FakeAudience(locale: Locale?) : Audience {
    val sent = mutableListOf<Component>()

    private val pointers =
        Pointers.builder().apply { locale?.let { withStatic(Identity.LOCALE, it) } }.build()

    override fun pointers(): Pointers = pointers

    override fun sendMessage(message: Component) {
        sent += message
    }
}

class TranslationsTest {

    private val translations = Translations.forBundle(BUNDLE, javaClass.classLoader)

    private fun plain(component: Component) =
        PlainTextComponentSerializer.plainText().serialize(component)

    private fun hasColour(component: Component, colour: NamedTextColor): Boolean =
        component.color() == colour || component.children().any { hasColour(it, colour) }

    @Test
    fun `renders the source language`() {
        val rendered = translations.render(TestMessage.GREETING, Locale.ENGLISH, "player" to "Ada")
        assertEquals("Hello, Ada!", plain(rendered))
    }

    @Test
    fun `renders a translated language and keeps its markup`() {
        val rendered = translations.render(TestMessage.GREETING, Locale.GERMAN, "player" to "Ada")
        assertEquals("Hallo, Ada!", plain(rendered))
        assertTrue(hasColour(rendered, NamedTextColor.GREEN))
    }

    @Test
    fun `a regional locale uses its language bundle`() {
        val rendered = translations.render(TestMessage.GREETING, Locale.GERMANY, "player" to "Ada")
        assertEquals("Hallo, Ada!", plain(rendered))
    }

    @Test
    fun `a key missing from a translation falls back to the source`() {
        val rendered = translations.render(TestMessage.ONLY_IN_SOURCE, Locale.GERMAN)
        assertEquals("source only", plain(rendered))
    }

    @Test
    fun `an unknown key renders as itself instead of throwing`() {
        val rendered = translations.render(TestMessage.ABSENT, Locale.ENGLISH)
        assertEquals("nowhere.at.all", plain(rendered))
    }

    @Test
    fun `argument markup is not parsed`() {
        val rendered =
            translations.render(TestMessage.GREETING, Locale.ENGLISH, "player" to "<red>evil</red>")
        assertEquals("Hello, <red>evil</red>!", plain(rendered))
    }

    @Test
    fun `a component argument keeps its formatting`() {
        val name = Component.text("Ada", NamedTextColor.GOLD)
        val rendered = translations.render(TestMessage.GREETING, Locale.ENGLISH, "player" to name)
        assertTrue(hasColour(rendered, NamedTextColor.GOLD))
    }

    @Test
    fun `an untranslated locale uses the source, not the JVM default`() {
        val previous = Locale.getDefault()
        try {
            Locale.setDefault(Locale.GERMANY)
            ResourceBundle.clearCache(javaClass.classLoader)
            val fresh = Translations.forBundle(BUNDLE, javaClass.classLoader)
            val rendered = fresh.render(TestMessage.GREETING, Locale.FRENCH, "player" to "Ada")
            assertEquals("Hello, Ada!", plain(rendered))
        } finally {
            Locale.setDefault(previous)
            ResourceBundle.clearCache(javaClass.classLoader)
        }
    }

    @Test
    fun `send picks the language the audience announced`() {
        val audience = FakeAudience(Locale.GERMANY)
        translations.send(audience, TestMessage.GREETING, "player" to "Ada")
        assertEquals(listOf("[Grounds] Info › Hallo, Ada!"), audience.sent.map(::plain))
    }

    @Test
    fun `an audience with no locale gets the fallback`() {
        val audience = FakeAudience(null)
        translations.send(audience, TestMessage.GREETING, "player" to "Ada")
        assertEquals(listOf("[Grounds] Info › Hello, Ada!"), audience.sent.map(::plain))
    }

    @Test
    fun `a custom resolver overrides the announced locale`() {
        val overriding =
            Translations.forBundle(
                BUNDLE,
                javaClass.classLoader,
                localeResolver = LocaleResolver { Locale.GERMAN },
            )
        val audience = FakeAudience(Locale.ENGLISH)
        overriding.send(audience, TestMessage.GREETING, "player" to "Ada")
        assertEquals(listOf("[Grounds] Info › Hallo, Ada!"), audience.sent.map(::plain))
    }

    @Test
    fun `sendUnprefixed leaves the line bare`() {
        val audience = FakeAudience(Locale.ENGLISH)
        translations.sendUnprefixed(audience, TestMessage.GREETING, "player" to "Ada")
        assertEquals(listOf("Hello, Ada!"), audience.sent.map(::plain))
    }

    @Test
    fun `render is never prefixed - it also feeds lore and titles`() {
        val rendered = translations.render(TestMessage.GREETING, Locale.ENGLISH, "player" to "Ada")
        assertEquals("Hello, Ada!", plain(rendered))
    }

    @Test
    fun `a bundle of conversation lines opts out at construction`() {
        val bare = Translations.forBundle(BUNDLE, javaClass.classLoader, prefix = Prefix.NONE)
        val audience = FakeAudience(Locale.ENGLISH)
        bare.send(audience, TestMessage.GREETING, "player" to "Ada")
        assertEquals(listOf("Hello, Ada!"), audience.sent.map(::plain))
    }

    @Test
    fun `the prefix does not bleed its colour into the message`() {
        val audience = FakeAudience(Locale.GERMAN)
        translations.send(audience, TestMessage.GREETING, "player" to "Ada")
        assertTrue(hasColour(audience.sent.single(), NamedTextColor.GREEN))
    }
}
