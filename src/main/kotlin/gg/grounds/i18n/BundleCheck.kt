package gg.grounds.i18n

import java.util.Locale
import java.util.Properties

/**
 * Finds what is wrong with a plugin's bundle before a player does.
 *
 * Meant for one test per plugin, so a missing translation fails the build of the PR that added the
 * English string — not a player's chat, as a raw key or as English in the middle of German:
 * ```kotlin
 * @Test
 * fun `messages are complete`() {
 *     assertEquals(
 *         emptyList(),
 *         BundleCheck.problems("gg.grounds.social.messages", javaClass.classLoader, SocialMessage.entries),
 *     )
 * }
 * ```
 *
 * It reports:
 * - a [MessageKey] with no line in the source bundle,
 * - a translation that is missing, lacks a source key, or still holds a key the source dropped,
 * - a translation whose tags differ from the source line. Placeholders are the point: a translated
 *   `<target>` renders as nothing, and a forgotten one silently drops the player's name.
 */
object BundleCheck {

    /**
     * @param keys every key the plugin can send, usually `SomeMessage.entries`.
     * @param source language of `messages.properties`.
     * @param translations languages that must be fully translated. Defaults to every language a
     *   player can pick, because a picked language without a bundle reads as broken.
     * @return one human-readable line per problem, empty when the bundle is complete.
     */
    @JvmStatic
    @JvmOverloads
    fun problems(
        baseName: String,
        classLoader: ClassLoader,
        keys: Iterable<MessageKey>,
        source: Locale = Locale.ENGLISH,
        translations: Collection<Locale> = SupportedLocales.ALL - source,
    ): List<String> {
        val sourcePath = path(baseName, null)
        val sourceLines = load(classLoader, sourcePath) ?: return listOf("$sourcePath: not found")

        val problems = mutableListOf<String>()
        keys
            .map { it.id }
            .filterNot(sourceLines::containsKey)
            .sorted()
            .forEach { problems += "$sourcePath: no line for key '$it'" }

        for (locale in translations) {
            val path = path(baseName, locale)
            val lines = load(classLoader, path)
            if (lines == null) {
                problems += "$path: not found"
                continue
            }
            for (id in (sourceLines.keys + lines.keys).sorted()) {
                val original = sourceLines[id]
                val translated = lines[id]
                when {
                    translated == null -> problems += "$path: missing '$id'"
                    original == null -> problems += "$path: '$id' is not in the source bundle"
                    else ->
                        tagDifference(original, translated)?.let { problems += "$path: '$id' $it" }
                }
            }
        }
        return problems
    }

    private fun path(baseName: String, locale: Locale?): String =
        baseName.replace('.', '/') + (locale?.let { "_$it" } ?: "") + ".properties"

    /**
     * The file alone, not a [java.util.ResourceBundle]: a bundle inherits from its parent, so a
     * missing German line would quietly resolve to the English one and the check would see nothing.
     */
    private fun load(classLoader: ClassLoader, path: String): Map<String, String>? =
        classLoader.getResourceAsStream(path)?.use { stream ->
            val properties = Properties()
            properties.load(stream.reader(Charsets.UTF_8))
            properties.stringPropertyNames().associateWith(properties::getProperty)
        }

    private fun tagDifference(original: String, translated: String): String? {
        val expected = tags(original)
        val actual = tags(translated)
        if (expected == actual) return null
        val missing = (expected - actual).sorted().joinToString(" ") { "<$it>" }
        val extra = (actual - expected).sorted().joinToString(" ") { "<$it>" }
        return listOfNotNull(
                missing.ifEmpty { null }?.let { "lacks $it" },
                extra.ifEmpty { null }?.let { "adds $it" },
            )
            .joinToString(", ")
    }

    /**
     * Opening tag names, `<click:run_command:/x>` counting as `click`. Closing tags are ignored
     * because MiniMessage closes them for you and a translation may legitimately leave them out;
     * `\<` is MiniMessage's escape and not a tag.
     */
    private fun tags(line: String): Set<String> =
        TAG.findAll(line).map { it.groupValues[1].lowercase() }.toSet()

    private val TAG = Regex("""(?<!\\)<([!#]?[a-zA-Z0-9_\-]+)""")
}
