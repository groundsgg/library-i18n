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
                SystemMessages.format(
                    message,
                    (key as? SystemMessageKey)?.category ?: SystemCategory.GENERAL,
                    logoReady = isReady(audience),
                    locale = locale,
                    level = (key as? SystemMessageKey)?.level ?: SystemLevel.INFO,
                )
            }

        @JvmField
        val TEXT_ONLY = SystemMessageRenderer { _, key, message, locale ->
            SystemMessages.format(
                message,
                (key as? SystemMessageKey)?.category ?: SystemCategory.GENERAL,
                locale = locale,
                level = (key as? SystemMessageKey)?.level ?: SystemLevel.INFO,
            )
        }
    }
}
