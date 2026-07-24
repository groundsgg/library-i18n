package gg.grounds.i18n

import java.util.Locale
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.identity.Identity

/**
 * Decides which language an audience reads.
 *
 * The default asks the client. This is the seam for a stored per-player override (`/lang de`) once
 * one exists — wrap [FROM_AUDIENCE] and consult the player store first:
 * ```kotlin
 * LocaleResolver { audience ->
 *     audience.get(Identity.UUID).map(overrides::get).orElse(null)
 *         ?: LocaleResolver.FROM_AUDIENCE.localeOf(audience)
 * }
 * ```
 */
fun interface LocaleResolver {
    /** The audience's language, or `null` if it is unknown and the fallback should be used. */
    fun localeOf(audience: Audience): Locale?

    companion object {
        /**
         * Reads the locale the client announced. Works unchanged on Paper, Velocity and Minestom —
         * all three expose it through the same Adventure pointer.
         */
        @JvmField
        val FROM_AUDIENCE: LocaleResolver = LocaleResolver { it.get(Identity.LOCALE).orElse(null) }
    }
}
