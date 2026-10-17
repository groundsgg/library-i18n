package gg.grounds.i18n

import java.util.Locale
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.text.Component

/** Host-specific seam for capability-aware system output; default output requires no pack. */
fun interface SystemMessageRenderer {
    fun render(audience: Audience, key: MessageKey, message: Component, locale: Locale): Component

    companion object {
        /** The host supplies verified recipient readiness; missing capabilities retain text. */
        @JvmStatic
        fun withLogoReadiness(isReady: (Audience) -> Boolean): SystemMessageRenderer =
            SystemMessageRenderer { audience, key, message, locale ->
                format(key, message, locale, isReady(audience))
            }

        @JvmField
        val TEXT_ONLY = SystemMessageRenderer { _, key, message, locale ->
            format(key, message, locale, false)
        }

        private fun format(
            key: MessageKey,
            message: Component,
            locale: Locale,
            ready: Boolean,
        ): Component =
            if (key is CategoryMessageKey)
                SystemMessages.formatNamed(message, key.label(locale), ready, locale, key.level)
            else
                SystemMessages.format(
                    message,
                    (key as? SystemMessageKey)?.category ?: SystemCategory.GENERAL,
                    ready,
                    locale,
                    (key as? SystemMessageKey)?.level ?: SystemLevel.INFO,
                )
    }
}
