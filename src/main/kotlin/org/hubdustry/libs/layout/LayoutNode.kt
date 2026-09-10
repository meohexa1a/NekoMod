package org.hubdustry.libs.layout

import org.hubdustry.libs.layout.policies.BoxLayoutPolicy

/**
 * Node ảo trong cây layout (Virtual Layout Node), thuần hình học và toán học.
 * Không chứa logic đồ họa, màu sắc hay input listener.
 */
open class LayoutNode {
    var parent: LayoutNode? = null
    val children: MutableList<LayoutNode> = ArrayList()

    var visible: Boolean = true

    // Tọa độ & Kích thước kết quả (Được tính toán bởi policy)
    var x: Float = 0f
    var y: Float = 0f
    var width: Float = 0f
    var height: Float = 0f

    // Ràng buộc kích thước tự thân (Intrinsic Constraints)
    var minWidth: Float = 0f
    var minHeight: Float = 0f
    var maxWidth: Float = Float.MAX_VALUE
    var maxHeight: Float = Float.MAX_VALUE

    // Inset (Padding nội tại của Node)
    var paddingLeft: Float = 0f
    var paddingTop: Float = 0f
    var paddingRight: Float = 0f
    var paddingBottom: Float = 0f

    // Cấu hình Anchor & SizeFlags theo mô hình Godot Control
    val anchor: AnchorData = AnchorData()
    var sizeFlagHorizontal: SizeFlag = SizeFlag.FILL
    var sizeFlagVertical: SizeFlag = SizeFlag.FILL
    var stretchRatio: Float = 1f

    // Căn chỉnh khi SHRINK trong slot
    var alignHorizontal: Alignment = Alignment.START
    var alignVertical: Alignment = Alignment.START

    // Chiến lược bố cục (Mặc định là BoxLayoutPolicy)
    var policy: LayoutPolicy = BoxLayoutPolicy

    // Scratchpad dùng nội bộ cho các LayoutPolicy đạt chuẩn Zero-GC
    internal var tempMain: Float = 0f
    internal var isFrozen: Boolean = false

    fun isAncestorOf(node: LayoutNode): Boolean {
        var p = node.parent
        while (p != null) {
            if (p === this) return true
            p = p.parent
        }
        return false
    }

    fun addChild(child: LayoutNode) {
        if (child === this) return // Chặn chu trình self-reference
        if (child.isAncestorOf(this)) return // Chặn chu trình lặp tổ tiên A -> B -> A
        child.parent?.removeChild(child)
        child.parent = this
        children.add(child)
    }

    fun removeChild(child: LayoutNode): Boolean {
        if (children.remove(child)) {
            child.parent = null
            return true
        }
        return false
    }

    fun setPadding(left: Float, top: Float, right: Float, bottom: Float) {
        paddingLeft = left
        paddingTop = top
        paddingRight = right
        paddingBottom = bottom
    }

    fun setPadding(all: Float) {
        setPadding(all, all, all, all)
    }

    /**
     * Entry point: Bắt đầu tính toán toàn bộ cây layout từ node này.
     */
    fun layout(availableWidth: Float, availableHeight: Float) {
        val safeAvailableWidth = when {
            availableWidth.isNaN() || availableWidth < 0f -> 0f
            else -> availableWidth
        }
        val safeAvailableHeight = when {
            availableHeight.isNaN() || availableHeight < 0f -> 0f
            else -> availableHeight
        }

        policy.computeMinSize(this)

        val safeMinWidth = if (minWidth.isNaN() || minWidth < 0f) 0f else minWidth
        val safeMinHeight = if (minHeight.isNaN() || minHeight < 0f) 0f else minHeight
        val safeMaxWidth = if (maxWidth.isNaN() || maxWidth < safeMinWidth) safeMinWidth else maxWidth
        val safeMaxHeight = if (maxHeight.isNaN() || maxHeight < safeMinHeight) safeMinHeight else maxHeight

        // Kích thước của chính node này
        val finalW = when {
            anchor.isEnabled -> safeAvailableWidth
            safeAvailableWidth == Float.MAX_VALUE || safeAvailableWidth.isInfinite() -> safeMinWidth
            else -> maxOf(safeMinWidth, safeAvailableWidth)
        }.coerceIn(safeMinWidth, safeMaxWidth)

        val finalH = when {
            anchor.isEnabled -> safeAvailableHeight
            safeAvailableHeight == Float.MAX_VALUE || safeAvailableHeight.isInfinite() -> safeMinHeight
            else -> maxOf(safeMinHeight, safeAvailableHeight)
        }.coerceIn(safeMinHeight, safeMaxHeight)

        this.width = finalW
        this.height = finalH

        val safePadLeft = if (paddingLeft.isNaN() || paddingLeft < 0f) 0f else paddingLeft
        val safePadTop = if (paddingTop.isNaN() || paddingTop < 0f) 0f else paddingTop
        val safePadRight = if (paddingRight.isNaN() || paddingRight < 0f) 0f else paddingRight
        val safePadBottom = if (paddingBottom.isNaN() || paddingBottom < 0f) 0f else paddingBottom

        val innerX = safePadLeft
        val innerY = safePadTop
        val innerW = maxOf(0f, this.width - safePadLeft - safePadRight)
        val innerH = maxOf(0f, this.height - safePadTop - safePadBottom)

        policy.arrangeChildren(this, innerX, innerY, innerW, innerH)
    }
}

