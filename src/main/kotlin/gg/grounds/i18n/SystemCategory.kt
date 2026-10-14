package gg.grounds.i18n

import java.util.Locale
import java.util.ResourceBundle

/** Meaning of a system message, independent of its translated text or text colour. */
enum class SystemCategory {
    INFO,
    SUCCESS,
    WARNING,
    ERROR,
    FRIENDS,
    PARTY,
    MATCH,
    STORY;

    internal fun label(locale: Locale): String =
        ResourceBundle.getBundle(
                "gg.grounds.i18n.system",
                locale,
                SystemCategory::class.java.classLoader,
                ResourceBundle.Control.getNoFallbackControl(
                    ResourceBundle.Control.FORMAT_PROPERTIES
                ),
            )
            .getString(name.lowercase(Locale.ROOT))
}

/** Optional metadata for existing MessageKey implementations; older keys remain valid. */
interface SystemMessageKey : MessageKey {
    val category: SystemCategory
}
