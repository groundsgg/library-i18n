package gg.grounds.i18n

import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.minimessage.tag.Tag
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver

/**
 * The Grounds semantic colours, as a bundle author writes them.
 *
 * These are the **dark** set from the design tokens, because that is the ground Minecraft chat is
 * drawn on. The light set exists for the web and is not reachable from here — a client cannot tell
 * us what its chat background looks like, and there is only one right answer over a dark world.
 *
 * The colours are exposed twice, on purpose:
 * - as [TextColor] constants, for code that builds a [net.kyori.adventure.text.Component] by hand;
 * - as MiniMessage tags via [TAGS], wired into the instance [Translations] renders with, so a
 *   bundle uses semantic tags for authored UI styles and inline highlights without hex codes.
 *
 * The standard MiniMessage colours (`<red>`, `<gray>`, …) still parse — nothing forbids them. What
 * changes is that they now read as a deviation next to `<danger>` and `<muted>`, which is the only
 * enforcement that survives contact with a translator.
 *
 * ### One colour, one meaning
 *
 * [ACCENT] is the interactive colour: text you can click. System headers and body base colours
 * follow the separate Minecraft Mini-CD palette in [SystemMessageColors]. These tags remain
 * available for deliberate child highlights and unprefixed UI or conversation styles.
 *
 * [DANGER] is a warm salmon rather than a vivid red. Over a near-black ground an accent has to be
 * light to be legible, and at that lightness sRGB has nothing redder that still clears APCA Lc 60.
 */
object Palette {

    /** Body text. `--text` */
    @JvmField val TEXT: TextColor = hex("#f3f0ea")

    /** Secondary inline hints and subordinate values. `--text-muted` */
    @JvmField val TEXT_MUTED: TextColor = hex("#d5cec6")

    /** Punctuation and brackets; the quietest thing still meant to be read. `--text-faint` */
    @JvmField val TEXT_FAINT: TextColor = hex("#bcb4ad")

    /** Brand mint. Interactive text only — links, click-to-run. `--accent` */
    @JvmField val ACCENT: TextColor = hex("#3de6db")

    /** Subdued mint for authored accents. `--accent-deep` */
    @JvmField val ACCENT_DEEP: TextColor = hex("#38b4ab")

    /** Something worked. `--success` */
    @JvmField val SUCCESS: TextColor = hex("#3fe075")

    /** Something is off but recoverable — wrong syntax, a rate limit. `--warning` */
    @JvmField val WARNING: TextColor = hex("#f2b036")

    /** Something failed. `--danger` */
    @JvmField val DANGER: TextColor = hex("#f9a49a")

    /** Neutral notice — a whisper marker, a status line. `--info` */
    @JvmField val INFO: TextColor = hex("#7ec7f9")

    /**
     * The same colours as MiniMessage tags: `<text>`, `<muted>`, `<faint>`, `<accent>`,
     * `<accent-deep>`, `<success>`, `<warning>`, `<danger>`, `<info>`.
     *
     * [Translations] resolves these by default, so a bundle needs no wiring.
     */
    @JvmField
    val TAGS: TagResolver =
        TagResolver.resolver(
            styling("text", TEXT),
            styling("muted", TEXT_MUTED),
            styling("faint", TEXT_FAINT),
            styling("accent", ACCENT),
            styling("accent-deep", ACCENT_DEEP),
            styling("success", SUCCESS),
            styling("warning", WARNING),
            styling("danger", DANGER),
            styling("info", INFO),
        )

    private fun hex(value: String): TextColor =
        requireNotNull(TextColor.fromHexString(value)) { "not a hex colour: $value" }

    private fun styling(name: String, colour: TextColor): TagResolver =
        TagResolver.resolver(name, Tag.styling(colour))
}
