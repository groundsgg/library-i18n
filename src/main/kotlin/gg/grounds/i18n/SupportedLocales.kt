package gg.grounds.i18n

import java.util.Locale

/**
 * The languages a player can pick, in the order a picker lists them.
 *
 * One list for the platform, so the language picker and [BundleCheck] cannot drift apart: adding a
 * language here makes every plugin's bundle check fail until that plugin translates it.
 */
object SupportedLocales {
    @JvmField
    val ALL: List<Locale> = listOf(Locale.ENGLISH, Locale.GERMAN, Locale.FRENCH, Locale.of("es"))
}
