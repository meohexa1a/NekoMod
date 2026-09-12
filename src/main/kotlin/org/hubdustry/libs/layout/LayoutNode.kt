package org.hubdustry.libs.layout

import arc.graphics.Color
import org.hubdustry.libs.compose.input.SuspendingPointerInputFilter
import org.hubdustry.libs.layout.policies.BoxLayoutPolicy

/**
 * Thực thể Virtual DOM tự trị (Unified Virtual Layout Node) của NekoMod.
 * Đóng gói toàn bộ hình học không gian, thuộc tính hiển thị (Visual Tokens)
 * và chuỗi bộ lọc cử chỉ (Pointer Input Filter Chain) trên các trường phẳng đạt chuẩn Zero-GC.
 */
open class LayoutNode {
    var parent: LayoutNode? = null
        internal set

    private val _children = ArrayList<LayoutNode>()
    val children: List<LayoutNode> get() = _children

    var visible: Boolean = true

    // Tọa độ & Kích thước kết quả (Được tính toán bởi policy)
    var x: Float = 0f
        set(value) {
            field = if (value.isNaN()) 0f else value
        }
    var y: Float = 0f
        set(value) {
            field = if (value.isNaN()) 0f else value
        }
    var offsetX: Float = 0f
        set(value) {
            field = if (value.isNaN()) 0f else value
        }
    var offsetY: Float = 0f
        set(value) {
            field = if (value.isNaN()) 0f else value
        }
    var width: Float = 0f
        set(value) {
            field = if (value.isNaN() || value < 0f) 0f else value
        }
    var height: Float = 0f
        set(value) {
            field = if (value.isNaN() || value < 0f) 0f else value
        }

    // Ràng buộc kích thước tự thân (Intrinsic Constraints) - Gateway Sanitization
    var minWidth: Float = 0f
        set(value) {
            field = if (value.isNaN() || value < 0f) 0f else value
            if (maxWidth < field) maxWidth = field
        }

    var minHeight: Float = 0f
        set(value) {
            field = if (value.isNaN() || value < 0f) 0f else value
            if (maxHeight < field) maxHeight = field
        }

    var maxWidth: Float = Float.MAX_VALUE
        set(value) {
            val safeMin = minWidth
            field = if (value.isNaN() || value < safeMin) safeMin else value
        }

    var maxHeight: Float = Float.MAX_VALUE
        set(value) {
            val safeMin = minHeight
            field = if (value.isNaN() || value < safeMin) safeMin else value
        }

    // Inset (Padding nội tại của Node) - Gateway Sanitization
    var paddingLeft: Float = 0f
        set(value) {
            field = if (value.isNaN() || value < 0f) 0f else value
        }

    var paddingTop: Float = 0f
        set(value) {
            field = if (value.isNaN() || value < 0f) 0f else value
        }

    var paddingRight: Float = 0f
        set(value) {
            field = if (value.isNaN() || value < 0f) 0f else value
        }

    var paddingBottom: Float = 0f
        set(value) {
            field = if (value.isNaN() || value < 0f) 0f else value
        }

    // Margin (Khoảng đệm ngoài của Node đối với Container cha) - Gateway Sanitization
    var marginLeft: Float = 0f
        set(value) {
            field = if (value.isNaN() || value < 0f) 0f else value
        }

    var marginTop: Float = 0f
        set(value) {
            field = if (value.isNaN() || value < 0f) 0f else value
        }

    var marginRight: Float = 0f
        set(value) {
            field = if (value.isNaN() || value < 0f) 0f else value
        }

    var marginBottom: Float = 0f
        set(value) {
            field = if (value.isNaN() || value < 0f) 0f else value
        }

    fun setMargin(all: Float) {
        val safe = if (all.isNaN() || all < 0f) 0f else all
        marginLeft = safe
        marginTop = safe
        marginRight = safe
        marginBottom = safe
    }

    fun setMargin(horizontal: Float, vertical: Float) {
        val safeH = if (horizontal.isNaN() || horizontal < 0f) 0f else horizontal
        val safeV = if (vertical.isNaN() || vertical < 0f) 0f else vertical
        marginLeft = safeH
        marginTop = safeV
        marginRight = safeH
        marginBottom = safeV
    }

    fun setMargin(left: Float, top: Float, right: Float, bottom: Float) {
        marginLeft = if (left.isNaN() || left < 0f) 0f else left
        marginTop = if (top.isNaN() || top < 0f) 0f else top
        marginRight = if (right.isNaN() || right < 0f) 0f else right
        marginBottom = if (bottom.isNaN() || bottom < 0f) 0f else bottom
    }

    // Cấu hình Anchor & SizeFlags theo mô hình Godot Control
    val anchor: AnchorData = AnchorData()
    var sizeFlagHorizontal: SizeFlag = SizeFlag.FILL
    var sizeFlagVertical: SizeFlag = SizeFlag.FILL
    var stretchRatio: Float = 1f
        set(value) {
            field = if (value.isNaN() || value < 0f) 0f else value
        }

    // Căn chỉnh khi SHRINK trong slot
    var alignHorizontal: Alignment = Alignment.START
    var alignVertical: Alignment = Alignment.START

    // Chiến lược bố cục (Mặc định là BoxLayoutPolicy)
    var policy: LayoutPolicy = BoxLayoutPolicy

    // Scratchpad dùng nội bộ cho các LayoutPolicy đạt chuẩn Zero-GC
    internal var tempMain: Float = 0f
    internal var isFrozen: Boolean = false

    // Thuộc tính hiển thị trực quan (Visual Tokens) cho Virtual DOM
    var backgroundColor: Color? = null
    var text: String? = null
    var textColor: Color = Color.white

    var alpha: Float = 1f
        set(value) {
            field = if (value.isNaN()) 1f else value.coerceIn(0f, 1f)
        }

    // Bộ lọc xử lý cử chỉ con trỏ (Pointer Input Engine) - Hỗ trợ nhiều filter theo chuỗi modifier
    private val _pointerInputFilters = ArrayList<SuspendingPointerInputFilter>()
    val pointerInputFilters: List<SuspendingPointerInputFilter> get() = _pointerInputFilters

    fun addPointerInputFilter(filter: SuspendingPointerInputFilter) {
        if (!_pointerInputFilters.contains(filter)) {
            _pointerInputFilters.add(filter)
        }
    }

    fun removePointerInputFilter(filter: SuspendingPointerInputFilter): Boolean =
        _pointerInputFilters.remove(filter)

    fun clearPointerInputFilters() {
        _pointerInputFilters.clear()
    }

    /**
     * Khôi phục toàn bộ các thuộc tính có thể bị biến đổi bởi Modifier về giá trị mặc định,
     * ngăn chặn rò rỉ trạng thái giữa các lần Recomposition.
     */
    fun resetModifierState(
        defaultSizeFlagH: SizeFlag = SizeFlag.FILL,
        defaultSizeFlagV: SizeFlag = SizeFlag.FILL
    ) {
        minWidth = 0f
        minHeight = 0f
        maxWidth = Float.MAX_VALUE
        maxHeight = Float.MAX_VALUE
        paddingLeft = 0f
        paddingTop = 0f
        paddingRight = 0f
        paddingBottom = 0f
        marginLeft = 0f
        marginTop = 0f
        marginRight = 0f
        marginBottom = 0f
        sizeFlagHorizontal = defaultSizeFlagH
        sizeFlagVertical = defaultSizeFlagV
        stretchRatio = 1f
        alignHorizontal = Alignment.START
        alignVertical = Alignment.START
        offsetX = 0f
        offsetY = 0f
        backgroundColor = null
        alpha = 1f
        _pointerInputFilters.clear()
        anchor.isEnabled = false
    }

    fun isAncestorOf(node: LayoutNode): Boolean {
        var p = node.parent
        while (p != null) {
            if (p === this) return true
            p = p.parent
        }
        return false
    }

    fun addChild(child: LayoutNode) {
        addChild(child, _children.size)
    }

    fun addChild(child: LayoutNode, index: Int) {
        if (child === this) return // Chặn chu trình self-reference
        if (child.isAncestorOf(this)) return // Chặn chu trình lặp tổ tiên A -> B -> A
        if (child.parent === this && _children.contains(child)) {
            _children.remove(child)
        } else {
            child.parent?.removeChild(child)
        }
        child.parent = this
        val safeIndex = index.coerceIn(0, _children.size)
        _children.add(safeIndex, child)
    }

    fun addChild(index: Int, child: LayoutNode) = addChild(child, index)

    fun removeChild(child: LayoutNode): Boolean {
        if (_children.remove(child)) {
            child.parent = null
            return true
        }
        return false
    }

    fun removeChildren(index: Int, count: Int) {
        if (count <= 0 || _children.isEmpty()) return
        val safeIndex = index.coerceIn(0, _children.size)
        val safeCount = count.coerceIn(0, _children.size - safeIndex)
        for (i in (safeIndex + safeCount - 1) downTo safeIndex) {
            val removed = _children.removeAt(i)
            removed.parent = null
        }
    }

    fun moveChildren(from: Int, to: Int, count: Int = 1) {
        if (from == to || count <= 0 || _children.isEmpty()) return
        val safeFrom = from.coerceIn(0, _children.size - 1)
        val safeCount = count.coerceIn(0, _children.size - safeFrom)
        if (safeCount <= 0) return
        val dest = if (to > safeFrom) to - safeCount else to
        val safeDest = dest.coerceIn(0, _children.size - safeCount)
        val moved = ArrayList<LayoutNode>(safeCount)
        repeat(safeCount) {
            moved.add(_children.removeAt(safeFrom))
        }
        _children.addAll(safeDest, moved)
    }

    fun clearChildren() {
        val count = _children.size
        for (i in 0 until count) {
            _children[i].parent = null
        }
        _children.clear()
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

    // --- 1D Axis Projections ---
    fun minSize(orientation: Orientation): Float = when (orientation) {
        Orientation.HORIZONTAL -> minWidth
        Orientation.VERTICAL -> minHeight
    }

    fun maxSize(orientation: Orientation): Float = when (orientation) {
        Orientation.HORIZONTAL -> maxWidth
        Orientation.VERTICAL -> maxHeight
    }

    fun sizeFlag(orientation: Orientation): SizeFlag = when (orientation) {
        Orientation.HORIZONTAL -> sizeFlagHorizontal
        Orientation.VERTICAL -> sizeFlagVertical
    }

    fun alignment(orientation: Orientation): Alignment = when (orientation) {
        Orientation.HORIZONTAL -> alignHorizontal
        Orientation.VERTICAL -> alignVertical
    }

    fun paddingMain(orientation: Orientation): Float = when (orientation) {
        Orientation.HORIZONTAL -> paddingLeft + paddingRight
        Orientation.VERTICAL -> paddingTop + paddingBottom
    }

    fun paddingCross(orientation: Orientation): Float = when (orientation) {
        Orientation.HORIZONTAL -> paddingTop + paddingBottom
        Orientation.VERTICAL -> paddingLeft + paddingRight
    }

    fun marginMain(orientation: Orientation): Float = when (orientation) {
        Orientation.HORIZONTAL -> marginLeft + marginRight
        Orientation.VERTICAL -> marginTop + marginBottom
    }

    fun marginCross(orientation: Orientation): Float = when (orientation) {
        Orientation.HORIZONTAL -> marginTop + marginBottom
        Orientation.VERTICAL -> marginLeft + marginRight
    }

    fun marginLeading(orientation: Orientation): Float = when (orientation) {
        Orientation.HORIZONTAL -> marginLeft
        Orientation.VERTICAL -> marginTop
    }

    fun marginTrailing(orientation: Orientation): Float = when (orientation) {
        Orientation.HORIZONTAL -> marginRight
        Orientation.VERTICAL -> marginBottom
    }

    fun marginCrossLeading(orientation: Orientation): Float = when (orientation) {
        Orientation.HORIZONTAL -> marginTop
        Orientation.VERTICAL -> marginLeft
    }

    fun setMinSizeByAxis(orientation: Orientation, main: Float, cross: Float) {
        when (orientation) {
            Orientation.HORIZONTAL -> {
                minWidth = maxOf(minWidth, main)
                minHeight = maxOf(minHeight, cross)
            }
            Orientation.VERTICAL -> {
                minWidth = maxOf(minWidth, cross)
                minHeight = maxOf(minHeight, main)
            }
        }
    }

    // --- Self-Arranging & Anchors ---
    /**
     * Định vị tọa độ và kích thước cho node, sau đó kích hoạt bố cục nội dung con.
     */
    fun arrange(newX: Float, newY: Float, newWidth: Float, newHeight: Float) {
        this.x = newX
        this.y = newY
        this.width = newWidth
        this.height = newHeight
        arrangeContent()
    }

    /**
     * Định vị node theo hệ trục chính (main) và trục phụ (cross).
     */
    fun arrangeAxis(
        orientation: Orientation,
        mainPos: Float,
        crossPos: Float,
        mainSize: Float,
        crossSize: Float
    ) {
        if (orientation == Orientation.HORIZONTAL) {
            arrange(mainPos, crossPos, mainSize, crossSize)
        } else {
            arrange(crossPos, mainPos, crossSize, mainSize)
        }
    }

    /**
     * Tự động tính toán inner bounds (vùng khả dụng sau khi trừ padding)
     * và chuyển giao cho policy bố cục các con.
     */
    fun arrangeContent() {
        val innerX = paddingLeft
        val innerY = paddingTop
        val innerW = maxOf(0f, width - paddingLeft - paddingRight)
        val innerH = maxOf(0f, height - paddingTop - paddingBottom)
        policy.arrangeChildren(this, innerX, innerY, innerW, innerH)
    }

    /**
     * Tự giải neo trong không gian của cha và kích hoạt bố cục nội dung con.
     */
    fun resolveAnchors(innerX: Float, innerY: Float, innerWidth: Float, innerHeight: Float) {
        val targetLeft = anchor.resolveLeft(innerX, innerWidth)
        val targetRight = anchor.resolveRight(innerX, innerWidth)
        val targetTop = anchor.resolveTop(innerY, innerHeight)
        val targetBottom = anchor.resolveBottom(innerY, innerHeight)

        val finalW = when {
            anchor.hasExplicitWidth -> maxOf(minWidth, targetRight - targetLeft)
            width > 0f -> width
            else -> minWidth
        }.coerceIn(minWidth, maxWidth)

        val finalX = when {
            anchor.hasExplicitWidth -> targetLeft
            else -> targetLeft - finalW * anchor.anchorLeft
        }

        val finalH = when {
            anchor.hasExplicitHeight -> maxOf(minHeight, targetBottom - targetTop)
            height > 0f -> height
            else -> minHeight
        }.coerceIn(minHeight, maxHeight)

        val finalY = when {
            anchor.hasExplicitHeight -> targetTop
            else -> targetTop - finalH * anchor.anchorTop
        }

        arrange(finalX, finalY, finalW, finalH)
    }

    /**
     * Entry point: Bắt đầu tính toán toàn bộ cây layout từ node này.
     */
    @JvmOverloads
    fun layout(availableWidth: Float, availableHeight: Float, exact: Boolean = false) {
        val safeAvailableWidth = when {
            availableWidth.isNaN() || availableWidth < 0f -> 0f
            else -> availableWidth
        }
        val safeAvailableHeight = when {
            availableHeight.isNaN() || availableHeight < 0f -> 0f
            else -> availableHeight
        }

        policy.computeMinSize(this)

        val safeMinWidth = minWidth
        val safeMinHeight = minHeight
        val safeMaxWidth = maxWidth
        val safeMaxHeight = maxHeight

        // Kích thước của chính node này
        val finalW = when {
            anchor.isEnabled -> safeAvailableWidth
            exact && safeAvailableWidth > 0f -> safeAvailableWidth
            safeAvailableWidth == Float.MAX_VALUE || safeAvailableWidth.isInfinite() -> safeMinWidth
            else -> maxOf(safeMinWidth, safeAvailableWidth)
        }.coerceIn(if (exact && safeAvailableWidth > 0f) 0f else safeMinWidth, safeMaxWidth)

        val finalH = when {
            anchor.isEnabled -> safeAvailableHeight
            exact && safeAvailableHeight > 0f -> safeAvailableHeight
            safeAvailableHeight == Float.MAX_VALUE || safeAvailableHeight.isInfinite() -> safeMinHeight
            else -> maxOf(safeMinHeight, safeAvailableHeight)
        }.coerceIn(if (exact && safeAvailableHeight > 0f) 0f else safeMinHeight, safeMaxHeight)

        this.width = finalW
        this.height = finalH

        arrangeContent()
    }
}

