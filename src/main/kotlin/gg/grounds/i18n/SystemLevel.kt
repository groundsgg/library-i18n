package gg.grounds.i18n

import java.util.Locale
import java.util.ResourceBundle

/** Semantic outcome or urgency; the renderer decides its visual treatment. */
enum class SystemLevel {
    INFO,
    SUCCESS,
    WARNING,
    ERROR;

    internal fun label(locale: Locale): String =
        ResourceBundle.getBundle(
                "gg.grounds.i18n.system",
                locale,
                SystemLevel::class.java.classLoader,
                ResourceBundle.Control.getNoFallbackControl(
                    ResourceBundle.Control.FORMAT_PROPERTIES
                ),
            )
            .getString("level." + name.lowercase(Locale.ROOT))
}
