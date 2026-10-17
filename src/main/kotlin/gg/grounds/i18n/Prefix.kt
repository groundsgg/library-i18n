package gg.grounds.i18n

import java.util.Locale
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextColor

/** Shared system prefix. Named game/area contexts use the same localized layout as typed keys. */
class Prefix
private constructor(
    private val component: Component,
    private val bodyColour: TextColor? = null,
    internal val categoryName: Component? = null,
    private val standardCategory: SystemCategory? = null,
) {
    fun applyTo(message: Component): Component = applyTo(message, Locale.ENGLISH)

    /** Locale-aware text fallback for contexts rendered without a concrete recipient. */
    fun applyTo(message: Component, locale: Locale): Component =
        when {
            this === GROUNDS -> SystemMessages.format(message, locale = locale)
            categoryName != null ->
                SystemMessages.formatNamed(
                    prepare(message),
                    standardCategory?.let { Component.text(it.label(locale)) } ?: categoryName,
                    locale = locale,
                )
            component == Component.empty() -> message
            else -> Component.empty().append(component).append(prepare(message))
        }

    internal fun prepare(message: Component): Component =
        bodyColour?.let(message::colorIfAbsent) ?: message

    internal fun key(original: MessageKey): MessageKey =
        categoryName?.let { CategoryMessageKey(original, it, standardCategory) } ?: original

    companion object {
        @JvmField val GROUNDS: Prefix = Prefix(Component.empty())
        @JvmField val NONE: Prefix = Prefix(Component.empty())

        @JvmStatic fun of(component: Component): Prefix = Prefix(component)

        /** Named context, e.g. `[Grounds] GetDown | …` or `[Logo] · GetDown | …`. */
        @JvmStatic
        fun category(name: String): Prefix =
            Prefix(
                Component.empty(),
                Palette.TEXT_MUTED,
                Component.text(name),
                SystemCategory.entries.firstOrNull { it.name.equals(name, ignoreCase = true) },
            )

        /** Rich context names remain components; readiness is supplied by the host renderer. */
        @JvmStatic
        fun category(name: Component): Prefix = Prefix(Component.empty(), Palette.TEXT_MUTED, name)
    }
}
