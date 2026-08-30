// [AGENT INVARIANT] Synchronously update @property, @param, and @see KDocs when modifying this file.

package org.mdt.core.ui.unit

/**
 * ## Rect
 *
 * Mutable 2D bounding box defined by `(x, y)` origin and `(width, height)` dimensions in OpenGL bottom-left screen coordinates.
 *
 * @property x Bottom-left X coordinate in pixels.
 * @property y Bottom-left Y coordinate in pixels.
 * @property width Rectangle width in pixels.
 * @property height Rectangle height in pixels.
 *
 * @see Insets
 * @see org.mdt.core.ui.node.UINode
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

    /** Checks whether this rectangle overlaps with [other]. */
    fun intersects(other: Rect): Boolean =
        x < other.x + other.width && x + width > other.x &&
        y < other.y + other.height && y + height > other.y

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
 * ## Insets [2D Edge Spacing Metric]
 *
 * Outward/inward edge distance spacing for margin and padding calculations.
 */
data class Insets(
    var left: Float = 0.0f,
    var top: Float = 0.0f,
    var right: Float = 0.0f,
    var bottom: Float = 0.0f
) {
    /** Total horizontal inset width (`left + right`). */
    val horizontal: Float get() = left + right

    /** Total vertical inset height (`top + bottom`). */
    val vertical: Float get() = top + bottom

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
