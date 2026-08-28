package org.mdt.core.ui.layout

// --- SIZE FLAGS & SLOTS ---

/**
 * ## SizeFlags [Container Slot Expansion Bitmask]
 *
 * Container slot allocation and expansion flags inspired by Godot Engine's UI architecture.
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

/**
 * ## AnchorData [2D Dual-Coordinate Anchor Model]
 *
 * ### 1. 📖 Feature Specification & Core Architecture:
 * - Dual-coordinate anchor and margin offset model for absolute and responsive layout positioning.
 * - Stores normalized ratio bounds (`anchorLeft`..`anchorBottom` in 0.0f..1.0f) and pixel offsets (`offsetLeft`..`offsetBottom`).
 * - Features an explicit [isEnabled] flag to eliminate ambiguous 0f heuristic checks.
 *
 * ### 2. ⚡ Invariants & Non-Negotiable Rules:
 * - **Rule 1 (Bottom-Left OpenGL Origin):** Y coordinates map $0.0f = \text{top edge}$ and $1.0f = \text{bottom edge}$ in preset ratio definitions.
 *
 * ### 3. 🔗 Related Files & Subsystem Map:
 * - 📐 **Layout Engine:** `src/main/kotlin/org/mdt/core/ui/layout/GodotLayout.kt`
 * - 🌲 **Target Virtual Node:** `src/main/kotlin/org/mdt/core/ui/node/UINode.kt`
 * - 🌲 **Root Virtual Node:** `src/main/kotlin/org/mdt/core/ui/node/CanvasNode.kt`
 *
 * ### 4. ✅ Behavioral Verification Checklist:
 * - [x] `setPreset` enables anchor mode (`isEnabled = true`) and sets anchor ratios.
 * - [x] `reset()` restores disabled state (`isEnabled = false`) and zeroes all ratios/offsets.
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

    // --- PRESETS & CONFIGURATION ---

    /** Active preset configuration. */
    var activePreset: LayoutPreset = LayoutPreset.TOP_LEFT

    fun setPreset(preset: LayoutPreset) {
        this.activePreset = preset
        isEnabled = true
        when (preset) {
            LayoutPreset.TOP_LEFT -> setAnchors(0.0f, 0.0f, 0.0f, 0.0f)
            LayoutPreset.TOP_RIGHT -> setAnchors(1.0f, 0.0f, 1.0f, 0.0f)
            LayoutPreset.BOTTOM_LEFT -> setAnchors(0.0f, 1.0f, 0.0f, 1.0f)
            LayoutPreset.BOTTOM_RIGHT -> setAnchors(1.0f, 1.0f, 1.0f, 1.0f)
            LayoutPreset.CENTER_LEFT -> setAnchors(0.0f, 0.5f, 0.0f, 0.5f)
            LayoutPreset.CENTER_TOP -> setAnchors(0.5f, 0.0f, 0.5f, 0.0f)
            LayoutPreset.CENTER_RIGHT -> setAnchors(1.0f, 0.5f, 1.0f, 0.5f)
            LayoutPreset.CENTER_BOTTOM -> setAnchors(0.5f, 1.0f, 0.5f, 1.0f)
            LayoutPreset.CENTER -> setAnchors(0.5f, 0.5f, 0.5f, 0.5f)
            LayoutPreset.LEFT_WIDE -> setAnchors(0.0f, 0.0f, 0.0f, 1.0f)
            LayoutPreset.TOP_WIDE -> setAnchors(0.0f, 0.0f, 1.0f, 0.0f)
            LayoutPreset.RIGHT_WIDE -> setAnchors(1.0f, 0.0f, 1.0f, 1.0f)
            LayoutPreset.BOTTOM_WIDE -> setAnchors(0.0f, 1.0f, 1.0f, 1.0f)
            LayoutPreset.VCENTER_WIDE -> setAnchors(0.5f, 0.0f, 0.5f, 1.0f)
            LayoutPreset.HCENTER_WIDE -> setAnchors(0.0f, 0.5f, 1.0f, 0.5f)
            LayoutPreset.FULL_RECT -> setAnchors(0.0f, 0.0f, 1.0f, 1.0f)
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
    }
}
