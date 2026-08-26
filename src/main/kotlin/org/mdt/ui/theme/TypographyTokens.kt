package org.mdt.ui.theme

import arc.graphics.g2d.Font
import mindustry.ui.Fonts

/**
 * ## TypographyTokens
 *
 * Semantic typography styles for bitmap atlas fonts (BMFonts).
 * Complies with Rule 5 (rendering BMFonts at integer scale 1.0f).
 *
 * See: docs/design-system/design_system_en.md
 */
data class TypographyTokens(
    /** Prominent screen and section headers. */
    val title: Font = Fonts.def,

    /** Tech / monospaced header text for stat counters and coordinates. */
    val headline: Font = Fonts.tech,

    /** Standard body copy, descriptions, and list item text. */
    val body: Font = Fonts.def,

    /** Monospaced text for code, script, and technical data. */
    val mono: Font = Fonts.monospace,

    /** Small badge text and system captions. */
    val caption: Font = Fonts.tech
)
