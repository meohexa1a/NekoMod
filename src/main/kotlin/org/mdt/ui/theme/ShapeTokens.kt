package org.mdt.ui.theme

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
