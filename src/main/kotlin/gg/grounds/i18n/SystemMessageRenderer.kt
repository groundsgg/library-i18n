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
            withPrefixStyle {
                if (isReady(it)) SystemPrefixStyle.LOGO else SystemPrefixStyle.TEXT
            }

        @JvmStatic
        fun withPrefixStyle(styleFor: (Audience) -> SystemPrefixStyle): SystemMessageRenderer =
            SystemMessageRenderer { audience, key, message, locale ->
                format(key, message, locale, styleFor(audience))
            }

        @JvmField
        val TEXT_ONLY = SystemMessageRenderer { _, key, message, locale ->
            format(key, message, locale, SystemPrefixStyle.TEXT)
        }

        private fun format(
            key: MessageKey,
            message: Component,
            locale: Locale,
            style: SystemPrefixStyle,
        ): Component =
            if (
                key is CategoryMessageKey &&
                    (style != SystemPrefixStyle.BADGE || key.standard == null)
            )
                SystemMessages.formatNamed(
                    message,
                    key.label(locale),
                    style != SystemPrefixStyle.TEXT,
                    locale,
                    key.level,
                )
            else
                SystemMessages.format(
                    message,
                    (key as? SystemMessageKey)?.category ?: SystemCategory.GENERAL,
                    style,
                    locale,
                    (key as? SystemMessageKey)?.level ?: SystemLevel.INFO,
                )
    }
}
