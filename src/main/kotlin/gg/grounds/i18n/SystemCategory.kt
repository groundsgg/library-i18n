package gg.grounds.i18n

import java.util.Locale
import java.util.ResourceBundle

/** Subject context of a system message, independent of status and visual treatment. */
enum class SystemCategory {
    GENERAL,
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
        get() = SystemCategory.GENERAL

    val level: SystemLevel
        get() = SystemLevel.INFO
}
