// [AGENT INVARIANT] Synchronously update @property, @param, and @see KDocs when modifying this file.

package org.mdt.core.ui.unit

/**
 * ## Offset
 *
 * Immutable 2D geometric vector representing a point or displacement in pixels.
 *
 * @property x Horizontal X displacement in pixels.
 * @property y Vertical Y displacement in pixels.
 *
 * @see Size
 * @see Rect
 */
data class Offset(
    val x: Float = 0.0f,
    val y: Float = 0.0f,
) {
    operator fun plus(other: Offset): Offset = Offset(x + other.x, y + other.y)
    operator fun minus(other: Offset): Offset = Offset(x - other.x, y - other.y)
    operator fun times(scale: Float): Offset = Offset(x * scale, y * scale)

    companion object {
        val Zero = Offset(0.0f, 0.0f)
        val Unspecified = Offset(Float.NaN, Float.NaN)
    }
}

/**
 * ## Size
 *
 * Immutable 2D dimensions representing width and height in pixels.
 *
 * @property width Width in pixels.
 * @property height Height in pixels.
 *
 * @see Offset
 * @see Rect
 */
data class Size(
    val width: Float = 0.0f,
    val height: Float = 0.0f,
) {
    companion object {
        val Zero = Size(0.0f, 0.0f)
        val Unspecified = Size(Float.NaN, Float.NaN)
    }
}

/**
 * ## Rect
 *
 * Mutable 2D bounding box defined by `(x, y)` origin and `(width, height)` dimensions in OpenGL bottom-left screen coordinates.
 * Designed for high-frequency Zero-GC frame mutations.
 *
 * @property x Bottom-left X coordinate in pixels.
 * @property y Bottom-left Y coordinate in pixels.
 * @property width Rectangle width in pixels.
 * @property height Rectangle height in pixels.
 *
 * @see Insets
 * @see Offset
 */
class Rect(
    var x: Float = 0.0f,
    var y: Float = 0.0f,
    var width: Float = 0.0f,
    var height: Float = 0.0f,
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

    fun copy(
        x: Float = this.x,
        y: Float = this.y,
        width: Float = this.width,
        height: Float = this.height,
    ): Rect = Rect(x, y, width, height)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Rect) return false
        return x == other.x && y == other.y && width == other.width && height == other.height
    }

    override fun hashCode(): Int {
        var result = x.hashCode()
        result = 31 * result + y.hashCode()
        result = 31 * result + width.hashCode()
        result = 31 * result + height.hashCode()
        return result
    }

    override fun toString(): String = "Rect(x=$x, y=$y, width=$width, height=$height)"
}

/**
 * ## Insets
 *
 * 2D edge distance spacing metric for margin and padding calculations.
 *
 * @property left Outward/inward edge distance on the left side.
 * @property top Outward/inward edge distance on the top side.
 * @property right Outward/inward edge distance on the right side.
 * @property bottom Outward/inward edge distance on the bottom side.
 */
class Insets(
    var left: Float = 0.0f,
    var top: Float = 0.0f,
    var right: Float = 0.0f,
    var bottom: Float = 0.0f,
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

    fun copy(
        left: Float = this.left,
        top: Float = this.top,
        right: Float = this.right,
        bottom: Float = this.bottom,
    ): Insets = Insets(left, top, right, bottom)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Insets) return false
        return left == other.left && top == other.top && right == other.right && bottom == other.bottom
    }

    override fun hashCode(): Int {
        var result = left.hashCode()
        result = 31 * result + top.hashCode()
        result = 31 * result + right.hashCode()
        result = 31 * result + bottom.hashCode()
        return result
    }

    override fun toString(): String = "Insets(left=$left, top=$top, right=$right, bottom=$bottom)"

    companion object {
        val Zero = Insets(0.0f, 0.0f, 0.0f, 0.0f)
    }
}
