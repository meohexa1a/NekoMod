package org.mdt.core.ui.unit

/**
 * ## Rect
 *
 * Mutable 2D geometric bounding rectangle defined by origin ([x], [y]) and dimensions ([width], [height]).
 * Standardized on [Float] primitives across the UI engine.
 *
 * See: docs/ui-engine/ui_engine_en.md
 */
data class Rect(
    var x: Float = 0.0f,
    var y: Float = 0.0f,
    var width: Float = 0.0f,
    var height: Float = 0.0f
) {
    /** Checks whether point ([pointX], [pointY]) is contained within this rectangle. */
    fun contains(pointX: Float, pointY: Float): Boolean =
        pointX >= x && pointX <= x + width && pointY >= y && pointY <= y + height

    /** Sets rectangle dimensions and coordinates. */
    fun set(x: Float, y: Float, width: Float, height: Float) {
        this.x = x
        this.y = y
        this.width = width
        this.height = height
    }

    /** Copies coordinates and dimensions from another [Rect]. */
    fun set(other: Rect) = set(other.x, other.y, other.width, other.height)
}

/**
 * ## Insets
 *
 * Outward/inward edge distance spacing for margin and padding calculations.
 *
 * See: docs/ui-engine/ui_engine_en.md
 */
data class Insets(
    var left: Float = 0.0f,
    var top: Float = 0.0f,
    var right: Float = 0.0f,
    var bottom: Float = 0.0f
) {
    /** Sets individual edge insets. */
    fun set(left: Float, top: Float, right: Float, bottom: Float) {
        this.left = left
        this.top = top
        this.right = right
        this.bottom = bottom
    }

    /** Sets equal insets on all 4 sides. */
    fun set(all: Float) = set(all, all, all, all)
}
