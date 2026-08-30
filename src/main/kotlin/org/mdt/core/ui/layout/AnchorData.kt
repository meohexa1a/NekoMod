// [AGENT INVARIANT] Synchronously update @property, @param, and @see KDocs when modifying this file.

package org.mdt.core.ui.layout

// --- SIZE FLAGS & SLOTS ---

/**
 * ## SizeFlags [Container Slot Expansion Bitmask]
 *
 * Container slot allocation and expansion flags inspired by Godot Engine's UI architecture.
 */
object SizeFlags {
    /** Explicitly place at the start/top of the allocated slot. */
    const val SHRINK_BEGIN = 16

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

// --- LAYOUT PRESETS ---

/**
 * ## LayoutPreset [2D Anchor Presets]
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

// --- GROW DIRECTION (GODOT 4 ALIGNED) ---

/**
 * ## GrowDirection [Anchor Growth Vector]
 *
 * Defines the direction an anchored node expands when its minimum size or content changes,
 * aligned directly with Godot Engine's `Control::GrowDirection`.
 */
enum class GrowDirection {
    /** Control grows towards the left or top (negative offset). */
    BEGIN,
    /** Control grows towards the right or bottom (positive offset). */
    END,
    /** Control grows outward equally in both directions from the anchor pivot. */
    BOTH
}

/**
 * ## AnchorData
 *
 * Stores responsive anchor ratios (`0.0f..1.0f`) and pixel offsets ([offsetLeft]..[offsetBottom]).
 * Controls how a UI element pins to or stretches across its parent container's edges in Godot-style 2D layouts.
 *
 * @property isEnabled Whether explicit anchor positioning is active for this node.
 * @property anchorLeft Left anchor ratio normalized `0.0f..1.0f`.
 * @property anchorTop Top anchor ratio normalized `0.0f..1.0f`.
 * @property anchorRight Right anchor ratio normalized `0.0f..1.0f`.
 * @property anchorBottom Bottom anchor ratio normalized `0.0f..1.0f`.
 * @property offsetLeft Left pixel offset from anchor point.
 * @property offsetTop Top pixel offset from anchor point.
 * @property offsetRight Right pixel offset from anchor point.
 * @property offsetBottom Bottom pixel offset from anchor point.
 * @property growHorizontal Horizontal expansion direction ([GrowDirection]).
 * @property growVertical Vertical expansion direction ([GrowDirection]).
 *
 * @see LayoutPreset
 * @see GrowDirection
 * @see GodotLayout
 */
class AnchorData {

    /** Whether explicit anchor positioning is active for this node. */
    var isEnabled: Boolean = false

    // --- ANCHOR RATIOS (0.0f .. 1.0f) ---

    var anchorLeft: Float = 0.0f
    var anchorTop: Float = 0.0f
    var anchorRight: Float = 0.0f
    var anchorBottom: Float = 0.0f

    // --- MARGIN OFFSETS (Pixels) ---

    var offsetLeft: Float = 0.0f
    var offsetTop: Float = 0.0f
    var offsetRight: Float = 0.0f
    var offsetBottom: Float = 0.0f

    // --- GROW DIRECTIONS ---

    var growHorizontal: GrowDirection = GrowDirection.END
    var growVertical: GrowDirection = GrowDirection.END

    // --- PRESETS & CONFIGURATION ---

    /** Active preset configuration. */
    var activePreset: LayoutPreset = LayoutPreset.TOP_LEFT

    fun setPreset(preset: LayoutPreset) {
        if (this.activePreset == preset && isEnabled) return

        this.activePreset = preset
        isEnabled = true
        when (preset) {
            LayoutPreset.TOP_LEFT -> {
                setAnchors(0.0f, 0.0f, 0.0f, 0.0f)
                growHorizontal = GrowDirection.END
                growVertical = GrowDirection.END
            }
            LayoutPreset.TOP_RIGHT -> {
                setAnchors(1.0f, 0.0f, 1.0f, 0.0f)
                growHorizontal = GrowDirection.BEGIN
                growVertical = GrowDirection.END
            }
            LayoutPreset.BOTTOM_LEFT -> {
                setAnchors(0.0f, 1.0f, 0.0f, 1.0f)
                growHorizontal = GrowDirection.END
                growVertical = GrowDirection.BEGIN
            }
            LayoutPreset.BOTTOM_RIGHT -> {
                setAnchors(1.0f, 1.0f, 1.0f, 1.0f)
                growHorizontal = GrowDirection.BEGIN
                growVertical = GrowDirection.BEGIN
            }
            LayoutPreset.CENTER_LEFT -> {
                setAnchors(0.0f, 0.5f, 0.0f, 0.5f)
                growHorizontal = GrowDirection.END
                growVertical = GrowDirection.BOTH
            }
            LayoutPreset.CENTER_TOP -> {
                setAnchors(0.5f, 0.0f, 0.5f, 0.0f)
                growHorizontal = GrowDirection.BOTH
                growVertical = GrowDirection.END
            }
            LayoutPreset.CENTER_RIGHT -> {
                setAnchors(1.0f, 0.5f, 1.0f, 0.5f)
                growHorizontal = GrowDirection.BEGIN
                growVertical = GrowDirection.BOTH
            }
            LayoutPreset.CENTER_BOTTOM -> {
                setAnchors(0.5f, 1.0f, 0.5f, 1.0f)
                growHorizontal = GrowDirection.BOTH
                growVertical = GrowDirection.BEGIN
            }
            LayoutPreset.CENTER -> {
                setAnchors(0.5f, 0.5f, 0.5f, 0.5f)
                growHorizontal = GrowDirection.BOTH
                growVertical = GrowDirection.BOTH
            }
            LayoutPreset.LEFT_WIDE -> {
                setAnchors(0.0f, 0.0f, 0.0f, 1.0f)
                growHorizontal = GrowDirection.END
                growVertical = GrowDirection.BOTH
            }
            LayoutPreset.TOP_WIDE -> {
                setAnchors(0.0f, 0.0f, 1.0f, 0.0f)
                growHorizontal = GrowDirection.BOTH
                growVertical = GrowDirection.END
            }
            LayoutPreset.RIGHT_WIDE -> {
                setAnchors(1.0f, 0.0f, 1.0f, 1.0f)
                growHorizontal = GrowDirection.BEGIN
                growVertical = GrowDirection.BOTH
            }
            LayoutPreset.BOTTOM_WIDE -> {
                setAnchors(0.0f, 1.0f, 1.0f, 1.0f)
                growHorizontal = GrowDirection.BOTH
                growVertical = GrowDirection.BEGIN
            }
            LayoutPreset.VCENTER_WIDE -> {
                setAnchors(0.5f, 0.0f, 0.5f, 1.0f)
                growHorizontal = GrowDirection.BOTH
                growVertical = GrowDirection.BOTH
            }
            LayoutPreset.HCENTER_WIDE -> {
                setAnchors(0.0f, 0.5f, 1.0f, 0.5f)
                growHorizontal = GrowDirection.BOTH
                growVertical = GrowDirection.BOTH
            }
            LayoutPreset.FULL_RECT -> {
                setAnchors(0.0f, 0.0f, 1.0f, 1.0f)
                growHorizontal = GrowDirection.BOTH
                growVertical = GrowDirection.BOTH
            }
        }
    }

    fun setAnchors(left: Float, top: Float, right: Float, bottom: Float) {
        isEnabled = true
        anchorLeft = left
        anchorTop = top
        anchorRight = right
        anchorBottom = bottom
    }

    fun setOffsets(left: Float, top: Float, right: Float, bottom: Float) {
        isEnabled = true
        offsetLeft = left
        offsetTop = top
        offsetRight = right
        offsetBottom = bottom
    }

    fun reset() {
        isEnabled = false
        anchorLeft = 0.0f
        anchorTop = 0.0f
        anchorRight = 0.0f
        anchorBottom = 0.0f
        offsetLeft = 0.0f
        offsetTop = 0.0f
        offsetRight = 0.0f
        offsetBottom = 0.0f
        growHorizontal = GrowDirection.END
        growVertical = GrowDirection.END
    }

    fun copyFrom(other: AnchorData) {
        isEnabled = other.isEnabled
        anchorLeft = other.anchorLeft
        anchorTop = other.anchorTop
        anchorRight = other.anchorRight
        anchorBottom = other.anchorBottom
        offsetLeft = other.offsetLeft
        offsetTop = other.offsetTop
        offsetRight = other.offsetRight
        offsetBottom = other.offsetBottom
        growHorizontal = other.growHorizontal
        growVertical = other.growVertical
    }
}
