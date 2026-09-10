package org.hubdustry.libs.layout

/**
 * Các preset neo (Anchor) thông dụng của Godot Control, ánh xạ sang hệ tọa độ Y-down (Top-Left origin).
 */
enum class AnchorPreset(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
) {
    TOP_LEFT(0f, 0f, 0f, 0f),
    TOP_RIGHT(1f, 0f, 1f, 0f),
    BOTTOM_LEFT(0f, 1f, 0f, 1f),
    BOTTOM_RIGHT(1f, 1f, 1f, 1f),
    CENTER_LEFT(0f, 0.5f, 0f, 0.5f),
    CENTER_TOP(0.5f, 0f, 0.5f, 0f),
    CENTER_RIGHT(1f, 0.5f, 1f, 0.5f),
    CENTER_BOTTOM(0.5f, 1f, 0.5f, 1f),
    CENTER(0.5f, 0.5f, 0.5f, 0.5f),
    TOP_WIDE(0f, 0f, 1f, 0f),
    BOTTOM_WIDE(0f, 1f, 1f, 1f),
    LEFT_WIDE(0f, 0f, 0f, 1f),
    RIGHT_WIDE(1f, 0f, 1f, 1f),
    FULL_RECT(0f, 0f, 1f, 1f);
}

/**
 * Dữ liệu neo và khoảng bù offset của [LayoutNode] bên trong container cha.
 */
class AnchorData {
    var isEnabled: Boolean = false

    var anchorLeft: Float = 0f
    var anchorTop: Float = 0f
    var anchorRight: Float = 0f
    var anchorBottom: Float = 0f

    var offsetLeft: Float = 0f
    var offsetTop: Float = 0f
    var offsetRight: Float = 0f
    var offsetBottom: Float = 0f

    /** Kiểm tra xem neo ngang có bị kéo giãn (anchorLeft < anchorRight). */
    val isStretchedHorizontal: Boolean get() = anchorLeft < anchorRight

    /** Kiểm tra xem neo dọc có bị kéo giãn (anchorTop < anchorBottom). */
    val isStretchedVertical: Boolean get() = anchorTop < anchorBottom

    /** Kiểm tra xem kích thước ngang có được xác định rõ ràng (kéo giãn hoặc qua offset). */
    val hasExplicitWidth: Boolean get() = isStretchedHorizontal || offsetRight > offsetLeft

    /** Kiểm tra xem kích thước dọc có được xác định rõ ràng (kéo giãn hoặc qua offset). */
    val hasExplicitHeight: Boolean get() = isStretchedVertical || offsetBottom > offsetTop

    fun resolveLeft(innerX: Float, innerWidth: Float): Float =
        innerX + innerWidth * anchorLeft + offsetLeft

    fun resolveRight(innerX: Float, innerWidth: Float): Float =
        innerX + innerWidth * anchorRight + offsetRight

    fun resolveTop(innerY: Float, innerHeight: Float): Float =
        innerY + innerHeight * anchorTop + offsetTop

    fun resolveBottom(innerY: Float, innerHeight: Float): Float =
        innerY + innerHeight * anchorBottom + offsetBottom

    fun setPreset(preset: AnchorPreset) {
        isEnabled = true
        anchorLeft = preset.left
        anchorTop = preset.top
        anchorRight = preset.right
        anchorBottom = preset.bottom
    }

    fun setOffsets(left: Float, top: Float, right: Float, bottom: Float) {
        this.offsetLeft = left
        this.offsetTop = top
        this.offsetRight = right
        this.offsetBottom = bottom
    }
}

/**
 * Thuật toán giải neo (Anchor Math Resolver) thuần toán học.
 */
object AnchorMath {
    /**
     * Định vị tọa độ và kích thước cho [child] bên trong vùng nội dung [innerX, innerY, innerWidth, innerHeight] của cha.
     */
    fun resolve(
        child: LayoutNode,
        innerX: Float,
        innerY: Float,
        innerWidth: Float,
        innerHeight: Float
    ) {
        val anchor = child.anchor
        val targetLeft = anchor.resolveLeft(innerX, innerWidth)
        val targetRight = anchor.resolveRight(innerX, innerWidth)
        val targetTop = anchor.resolveTop(innerY, innerHeight)
        val targetBottom = anchor.resolveBottom(innerY, innerHeight)

        // --- Giải trục X ---
        val finalW = when {
            anchor.hasExplicitWidth -> maxOf(child.minWidth, targetRight - targetLeft)
            child.width > 0f -> child.width
            else -> child.minWidth
        }.coerceIn(child.minWidth, child.maxWidth)

        val finalX = when {
            anchor.hasExplicitWidth -> targetLeft
            else -> targetLeft - finalW * anchor.anchorLeft
        }

        // --- Giải trục Y ---
        val finalH = when {
            anchor.hasExplicitHeight -> maxOf(child.minHeight, targetBottom - targetTop)
            child.height > 0f -> child.height
            else -> child.minHeight
        }.coerceIn(child.minHeight, child.maxHeight)

        val finalY = when {
            anchor.hasExplicitHeight -> targetTop
            else -> targetTop - finalH * anchor.anchorTop
        }

        child.x = finalX
        child.y = finalY
        child.width = finalW
        child.height = finalH
    }
}
