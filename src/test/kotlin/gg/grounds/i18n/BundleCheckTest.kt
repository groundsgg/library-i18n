package gg.grounds.i18n

import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals

private enum class CompleteMessage(override val id: String) : MessageKey {
    SENT("friend.sent"),
    ACCEPT("friend.accept"),
    LITERAL("literal"),
}

private enum class BrokenMessage(override val id: String) : MessageKey {
    SENT("friend.sent"),
    REMOVED("friend.removed"),
    UNWRITTEN("friend.unwritten"),
}

class BundleCheckTest {

    private val loader = javaClass.classLoader

    @Test
    fun `a complete bundle has no problems`() {
        val problems =
            BundleCheck.problems(
                "gg.grounds.i18n.checkcomplete",
                loader,
                CompleteMessage.entries,
                translations = listOf(Locale.GERMAN),
            )
        assertEquals(emptyList(), problems)
    }

    @Test
    fun `reports every kind of problem`() {
        val problems =
            BundleCheck.problems(
                "gg.grounds.i18n.checkbroken",
                loader,
                BrokenMessage.entries,
                translations = listOf(Locale.GERMAN),
            )
        val de = "gg/grounds/i18n/checkbroken_de.properties"
        assertEquals(
            listOf(
                "gg/grounds/i18n/checkbroken.properties: no line for key 'friend.unwritten'",
                "$de: 'friend.old' is not in the source bundle",
                "$de: missing 'friend.removed'",
                "$de: 'friend.sent' lacks <success> <target>, adds <red>",
            ),
            problems,
        )
    }

    @Test
    fun `every pickable language is checked by default`() {
        val problems =
            BundleCheck.problems("gg.grounds.i18n.checkcomplete", loader, CompleteMessage.entries)
        assertEquals(
            listOf(
                "gg/grounds/i18n/checkcomplete_fr.properties: not found",
                "gg/grounds/i18n/checkcomplete_es.properties: not found",
            ),
            problems,
        )
    }

    @Test
    fun `a missing source bundle is one problem`() {
        val problems =
            BundleCheck.problems("gg.grounds.i18n.nothing", loader, CompleteMessage.entries)
        assertEquals(listOf("gg/grounds/i18n/nothing.properties: not found"), problems)
    }
}
