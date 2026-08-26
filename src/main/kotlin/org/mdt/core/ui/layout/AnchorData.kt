package org.mdt.core.ui.layout

/**
 * ## SizeFlags
 *
 * Container slot allocation and expansion flags inspired by Godot Engine's UI architecture.
 *
 * See: docs/layout-engine/layout_engine_en.md
 */
object SizeFlags {
    /** Do not expand; place at the start of the allocated slot. */
    const val SHRINK_BEGIN = 0

    /** Fill the entire allocated slot. */
    const val FILL = 1

    /** Expand to take up available free space in the container. */
    const val EXPAND = 2

    /** Expand to take up available space and fill the entire slot. */
    const val EXPAND_FILL = FILL or EXPAND

    /** Do not expand; center within the allocated slot. */
    const val SHRINK_CENTER = 4

    /** Do not expand; place at the end of the allocated slot. */
    const val SHRINK_END = 8
}

/**
 * ## LayoutPreset
 *
 * Common 2D anchor configurations for positioning UI nodes relative to their parents.
 */
enum class LayoutPreset {
    TOP_LEFT,
    TOP_RIGHT,
    BOTTOM_LEFT,
    BOTTOM_RIGHT,
    CENTER_LEFT,
    CENTER_TOP,
    CENTER_RIGHT,
    CENTER_BOTTOM,
    CENTER,
    LEFT_WIDE,
    TOP_WIDE,
    RIGHT_WIDE,
    BOTTOM_WIDE,
    VCENTER_WIDE,
    HCENTER_WIDE,
    FULL_RECT
}

/**
 * ## AnchorData
 *
 * Dual-coordinate anchor and margin offset model for absolute and responsive layout positioning.
 *
 * See: docs/layout-engine/layout_engine_en.md
 */
class AnchorData {

    // --- ANCHOR RATIOS (0.0f .. 1.0f) ---

    var anchorLeft: Float = 0f
    var anchorTop: Float = 0f
    var anchorRight: Float = 0f
    var anchorBottom: Float = 0f

    // --- MARGIN OFFSETS (Pixels) ---

    var offsetLeft: Float = 0f
    var offsetTop: Float = 0f
    var offsetRight: Float = 0f
    var offsetBottom: Float = 0f

    // --- PRESETS & CONFIGURATION ---

    fun setPreset(preset: LayoutPreset) {
        when (preset) {
            LayoutPreset.TOP_LEFT -> setAnchors(0f, 0f, 0f, 0f)
            LayoutPreset.TOP_RIGHT -> setAnchors(1f, 0f, 1f, 0f)
            LayoutPreset.BOTTOM_LEFT -> setAnchors(0f, 1f, 0f, 1f)
            LayoutPreset.BOTTOM_RIGHT -> setAnchors(1f, 1f, 1f, 1f)
            LayoutPreset.CENTER_LEFT -> setAnchors(0f, 0.5f, 0f, 0.5f)
            LayoutPreset.CENTER_TOP -> setAnchors(0.5f, 0f, 0.5f, 0f)
            LayoutPreset.CENTER_RIGHT -> setAnchors(1f, 0.5f, 1f, 0.5f)
            LayoutPreset.CENTER_BOTTOM -> setAnchors(0.5f, 1f, 0.5f, 1f)
            LayoutPreset.CENTER -> setAnchors(0.5f, 0.5f, 0.5f, 0.5f)
            LayoutPreset.LEFT_WIDE -> setAnchors(0f, 0f, 0f, 1f)
            LayoutPreset.TOP_WIDE -> setAnchors(0f, 0f, 1f, 0f)
            LayoutPreset.RIGHT_WIDE -> setAnchors(1f, 0f, 1f, 1f)
            LayoutPreset.BOTTOM_WIDE -> setAnchors(0f, 1f, 1f, 1f)
            LayoutPreset.VCENTER_WIDE -> setAnchors(0.5f, 0f, 0.5f, 1f)
            LayoutPreset.HCENTER_WIDE -> setAnchors(0f, 0.5f, 1f, 0.5f)
            LayoutPreset.FULL_RECT -> setAnchors(0f, 0f, 1f, 1f)
        }
    }

    fun setAnchors(left: Float, top: Float, right: Float, bottom: Float) {
        anchorLeft = left
        anchorTop = top
        anchorRight = right
        anchorBottom = bottom
    }

    fun setOffsets(left: Float, top: Float, right: Float, bottom: Float) {
        offsetLeft = left
        offsetTop = top
        offsetRight = right
        offsetBottom = bottom
    }

    fun copyFrom(other: AnchorData) {
        anchorLeft = other.anchorLeft
        anchorTop = other.anchorTop
        anchorRight = other.anchorRight
        anchorBottom = other.anchorBottom
        offsetLeft = other.offsetLeft
        offsetTop = other.offsetTop
        offsetRight = other.offsetRight
        offsetBottom = other.offsetBottom
    }
}
