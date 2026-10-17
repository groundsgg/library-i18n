package gg.grounds.i18n

import java.util.Locale
import net.kyori.adventure.text.Component

/** Rendering-only wrapper: preserves key identity/status metadata while adding a named context. */
internal class CategoryMessageKey(
    private val original: MessageKey,
    private val name: Component,
    private val standard: SystemCategory?,
) : SystemMessageKey {
    override val id: String
        get() = original.id

    override val category: SystemCategory
        get() = standard ?: (original as? SystemMessageKey)?.category ?: SystemCategory.GENERAL

    override val level: SystemLevel
        get() = (original as? SystemMessageKey)?.level ?: SystemLevel.INFO

    fun label(locale: Locale): Component =
        standard?.let { Component.text(it.label(locale)) } ?: name
}
