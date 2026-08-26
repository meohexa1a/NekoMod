package org.mdt.ui.theme

import arc.graphics.g2d.Font
import mindustry.ui.Fonts

/**
 * ## ShapeTokens
 *
 * Geometric corner radii standards inspired by Apple iOS design aesthetics.
 * Uses strict [Float] primitives across the UI framework (Rule 1).
 *
 * See: docs/design-system/design_system_en.md
 */
data class ShapeTokens(
    /** Micro elements, small badges, and fine chips (4px). */
    val small: Float = 4f,

    /** Secondary buttons, text fields, and list item inner elements (8f). */
    val medium: Float = 8f,

    /** Standard cards, surface sheets, and segmented containers (14f). */
    val large: Float = 14f,

    /** Prominent modal dialogs, popovers, and large hero panels (20f). */
    val xLarge: Float = 20f,

    /** Full pill / capsule curves for sliders, toggles, badges, and action buttons (999f). */
    val pill: Float = 999f
)

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
