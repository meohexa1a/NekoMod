package org.hubdustry.core.layout

import androidx.compose.ui.util.fastForEach
import arc.graphics.Color
import org.hubdustry.core.compose.input.SuspendingPointerInputFilter
import org.hubdustry.core.graphics.RoundedCorners
import org.hubdustry.core.layout.policies.BoxLayoutPolicy
import org.hubdustry.core.layout.policies.LayoutPolicy

/**
 * Thực thể Virtual DOM tự trị (Unified Virtual Layout Node) của NekoMod.
 * Đóng gói toàn bộ hình học không gian, thuộc tính hiển thị (Visual Tokens)
 * và chuỗi bộ lọc cử chỉ (Pointer Input Filter Chain) trên các trường phẳng đạt chuẩn Zero-GC.
 */
class LayoutNode {
    // ─────────────────────────────────────────────────────────────────────────
    // 1. HIERARCHY & VIRTUAL DOM TREE
    // ─────────────────────────────────────────────────────────────────────────
    var parent: LayoutNode? = null
        internal set

    private val _children = ArrayList<LayoutNode>()
    val children: List<LayoutNode> get() = _children

    var visible: Boolean = true
    var clipHorizontal: Boolean = false
    var clipVertical: Boolean = false

    var clip: Boolean
        get() = clipHorizontal || clipVertical
        set(value) {
            clipHorizontal = value
            clipVertical = value
        }

    // ─────────────────────────────────────────────────────────────────────────
    // 2. COMPUTED GEOMETRY & INTRINSIC CONSTRAINTS
    // ─────────────────────────────────────────────────────────────────────────
    // Tọa độ & Kích thước kết quả (Được tính toán bởi policy)
    var x: Float = 0f
        internal set(value) {
            field = if (value.isNaN()) 0f else value
        }
    var y: Float = 0f
        internal set(value) {
            field = if (value.isNaN()) 0f else value
        }
    var offsetX: Float = 0f
        internal set(value) {
            field = if (value.isNaN()) 0f else value
        }
    var offsetY: Float = 0f
        internal set(value) {
            field = if (value.isNaN()) 0f else value
        }
    var width: Float = 0f
        internal set(value) {
            field = if (value.isNaN() || value < 0f) 0f else value
        }
    var height: Float = 0f
        internal set(value) {
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

    // ─────────────────────────────────────────────────────────────────────────
    // 3. INSETS (PADDING & MARGIN)
    // ─────────────────────────────────────────────────────────────────────────
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

    fun setPadding(left: Float, top: Float, right: Float, bottom: Float) {
        paddingLeft = left
        paddingTop = top
        paddingRight = right
        paddingBottom = bottom
    }

    fun setPadding(all: Float) = setPadding(all, all, all, all)

    fun setPadding(horizontal: Float, vertical: Float) {
        val safeH = if (horizontal.isNaN() || horizontal < 0f) 0f else horizontal
        val safeV = if (vertical.isNaN() || vertical < 0f) 0f else vertical
        paddingLeft = safeH
        paddingTop = safeV
        paddingRight = safeH
        paddingBottom = safeV
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 4. LAYOUT POLICY, ANCHORS & ALIGNMENT
    // ─────────────────────────────────────────────────────────────────────────
    // Cấu hình Anchor & SizeFlags theo mô hình Godot Control
    val anchor: AnchorData = AnchorData()
    var sizeFlagHorizontal: SizeFlag = SizeFlag.FILL
    var sizeFlagVertical: SizeFlag = SizeFlag.FILL
    var stretchRatio: Float = 0f
        set(value) {
            field = if (value.isNaN() || value < 0f) 0f else value
        }

    // Căn chỉnh khi SHRINK trong slot
    var alignHorizontal: Alignment = Alignment.START
    var alignVertical: Alignment = Alignment.START

    // Chiến lược bố cục (Mặc định là BoxLayoutPolicy)
    var policy: LayoutPolicy = BoxLayoutPolicy

    // ─────────────────────────────────────────────────────────────────────────
    // 5. VISUAL TOKENS, SHAPE & BORDER
    // ─────────────────────────────────────────────────────────────────────────
    // Thuộc tính hiển thị trực quan (Visual Tokens) cho Virtual DOM
    var backgroundColor: Color? = null
    var text: String? = null
    var textColor: Color = Color.white
    var font: arc.graphics.g2d.Font? = null

    var alpha: Float = 1f
        set(value) {
            field = if (value.isNaN()) 1f else value.coerceIn(0f, 1f)
        }

    // Bo góc (Corner Radii) cho SDF Shader - Gateway Sanitization
    var cornerRadiusTopStart: Float = 0f
        set(value) {
            field = if (value.isNaN() || value < 0f) 0f else value
        }
    var cornerRadiusTopEnd: Float = 0f
        set(value) {
            field = if (value.isNaN() || value < 0f) 0f else value
        }
    var cornerRadiusBottomEnd: Float = 0f
        set(value) {
            field = if (value.isNaN() || value < 0f) 0f else value
        }
    var cornerRadiusBottomStart: Float = 0f
        set(value) {
            field = if (value.isNaN() || value < 0f) 0f else value
        }

    fun setCornerRadius(uniform: Float) {
        val safe = if (uniform.isNaN() || uniform < 0f) 0f else uniform
        cornerRadiusTopStart = safe
        cornerRadiusTopEnd = safe
        cornerRadiusBottomEnd = safe
        cornerRadiusBottomStart = safe
    }

    fun setCornerRadius(shape: RoundedCorners) {
        cornerRadiusTopStart = shape.safeTopStart
        cornerRadiusTopEnd = shape.safeTopEnd
        cornerRadiusBottomEnd = shape.safeBottomEnd
        cornerRadiusBottomStart = shape.safeBottomStart
    }

    fun setCornerRadius(topStart: Float, topEnd: Float, bottomEnd: Float, bottomStart: Float) {
        cornerRadiusTopStart = if (topStart.isNaN() || topStart < 0f) 0f else topStart
        cornerRadiusTopEnd = if (topEnd.isNaN() || topEnd < 0f) 0f else topEnd
        cornerRadiusBottomEnd = if (bottomEnd.isNaN() || bottomEnd < 0f) 0f else bottomEnd
        cornerRadiusBottomStart = if (bottomStart.isNaN() || bottomStart < 0f) 0f else bottomStart
    }

    val hasRoundedCorners: Boolean
        get() = cornerRadiusTopStart > 0.001f || cornerRadiusTopEnd > 0.001f ||
                cornerRadiusBottomEnd > 0.001f || cornerRadiusBottomStart > 0.001f

    // Viền (Border) cho SDF Shader - Gateway Sanitization
    var borderWidth: Float = 0f
        set(value) {
            field = if (value.isNaN() || value < 0f) 0f else value
        }
    var borderColor: Color = Color.clear

    val hasBorder: Boolean
        get() = borderWidth > 0.001f && borderColor.a > 0.001f

    // Bo góc cắt gọt (Clip Corner Radii) cho SDF Shader - Gateway Sanitization
    var clipRadiusTopStart: Float = 0f
        set(value) {
            field = if (value.isNaN() || value < 0f) 0f else value
        }
    var clipRadiusTopEnd: Float = 0f
        set(value) {
            field = if (value.isNaN() || value < 0f) 0f else value
        }
    var clipRadiusBottomEnd: Float = 0f
        set(value) {
            field = if (value.isNaN() || value < 0f) 0f else value
        }
    var clipRadiusBottomStart: Float = 0f
        set(value) {
            field = if (value.isNaN() || value < 0f) 0f else value
        }

    fun setClipCornerRadius(uniform: Float) {
        val safe = if (uniform.isNaN() || uniform < 0f) 0f else uniform
        clipRadiusTopStart = safe
        clipRadiusTopEnd = safe
        clipRadiusBottomEnd = safe
        clipRadiusBottomStart = safe
    }

    fun setClipCornerRadius(shape: RoundedCorners) {
        clipRadiusTopStart = shape.safeTopStart
        clipRadiusTopEnd = shape.safeTopEnd
        clipRadiusBottomEnd = shape.safeBottomEnd
        clipRadiusBottomStart = shape.safeBottomStart
    }

    fun setClipCornerRadius(topStart: Float, topEnd: Float, bottomEnd: Float, bottomStart: Float) {
        clipRadiusTopStart = if (topStart.isNaN() || topStart < 0f) 0f else topStart
        clipRadiusTopEnd = if (topEnd.isNaN() || topEnd < 0f) 0f else topEnd
        clipRadiusBottomEnd = if (bottomEnd.isNaN() || bottomEnd < 0f) 0f else bottomEnd
        clipRadiusBottomStart = if (bottomStart.isNaN() || bottomStart < 0f) 0f else bottomStart
    }

    val hasClipCorners: Boolean
        get() = clipRadiusTopStart > 0.001f || clipRadiusTopEnd > 0.001f ||
                clipRadiusBottomEnd > 0.001f || clipRadiusBottomStart > 0.001f

    /**
     * Kiểm tra xem một điểm trong hệ tọa độ cục bộ ([localX], [localY]) của node
     * có nằm trong vùng hình học thực tế (bao gồm khung AABB và 4 góc bo) hay không.
     * Thuần toán học (Pure Math), khoảng cách bình phương (Zero-GC Rule 3.3, không gọi sqrt).
     */
    fun containsPoint(localX: Float, localY: Float): Boolean {
        if (localX < 0f || localX > width || localY < 0f || localY > height) {
            return false
        }
        if (!hasRoundedCorners && !hasClipCorners) {
            return true
        }

        val rTopStart = if (hasClipCorners) clipRadiusTopStart else cornerRadiusTopStart
        val rTopEnd = if (hasClipCorners) clipRadiusTopEnd else cornerRadiusTopEnd
        val rBottomEnd = if (hasClipCorners) clipRadiusBottomEnd else cornerRadiusBottomEnd
        val rBottomStart = if (hasClipCorners) clipRadiusBottomStart else cornerRadiusBottomStart

        if (rTopStart <= 0.001f && rTopEnd <= 0.001f && rBottomEnd <= 0.001f && rBottomStart <= 0.001f) {
            return true
        }

        val halfW = width * 0.5f
        val halfH = height * 0.5f

        // Góc Top-Start
        val rTs = minOf(rTopStart, halfW, halfH)
        if (rTs > 0.001f && localX < rTs && localY < rTs) {
            val dx = localX - rTs
            val dy = localY - rTs
            if (dx * dx + dy * dy > rTs * rTs) return false
        }

        // Góc Top-End
        val rTe = minOf(rTopEnd, halfW, halfH)
        if (rTe > 0.001f && localX > width - rTe && localY < rTe) {
            val dx = localX - (width - rTe)
            val dy = localY - rTe
            if (dx * dx + dy * dy > rTe * rTe) return false
        }

        // Góc Bottom-End
        val rBe = minOf(rBottomEnd, halfW, halfH)
        if (rBe > 0.001f && localX > width - rBe && localY > height - rBe) {
            val dx = localX - (width - rBe)
            val dy = localY - (height - rBe)
            if (dx * dx + dy * dy > rBe * rBe) return false
        }

        // Góc Bottom-Start
        val rBs = minOf(rBottomStart, halfW, halfH)
        if (rBs > 0.001f && localX < rBs && localY > height - rBs) {
            val dx = localX - rBs
            val dy = localY - (height - rBs)
            if (dx * dx + dy * dy > rBs * rBs) return false
        }

        return true
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 6. POINTER INTERACTION & GESTURE FILTER CHAIN
    // ─────────────────────────────────────────────────────────────────────────
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

    // ─────────────────────────────────────────────────────────────────────────
    // 7. SCROLLING & VIEWPORT STATE
    // ─────────────────────────────────────────────────────────────────────────
    var isScrollableVertical: Boolean = false
    var isScrollableHorizontal: Boolean = false

    var verticalScrollState: org.hubdustry.core.compose.foundation.ScrollState? = null
    var horizontalScrollState: org.hubdustry.core.compose.foundation.ScrollState? = null

    var scrollX: Float = 0f
        get() = horizontalScrollState?.let { if (maxScrollX > 0f) it.value.coerceIn(0f, maxScrollX) else it.value } ?: field
        set(value) {
            val safe = if (value.isNaN() || value < 0f) 0f else value
            field = safe
            horizontalScrollState?.let { it.dispatchRawDelta(safe - it.value) }
        }
    var scrollY: Float = 0f
        get() = verticalScrollState?.let { if (maxScrollY > 0f) it.value.coerceIn(0f, maxScrollY) else it.value } ?: field
        set(value) {
            val safe = if (value.isNaN() || value < 0f) 0f else value
            field = safe
            verticalScrollState?.let { it.dispatchRawDelta(safe - it.value) }
        }
    var maxScrollX: Float = 0f
        set(value) {
            field = if (value.isNaN() || value < 0f) 0f else value
        }
    var maxScrollY: Float = 0f
        set(value) {
            field = if (value.isNaN() || value < 0f) 0f else value
        }

    var contentWidth: Float = 0f
        internal set
    var contentHeight: Float = 0f
        internal set

    // ─────────────────────────────────────────────────────────────────────────
    // 8. STATE RESET & TREE HIERARCHY MANIPULATION
    // ─────────────────────────────────────────────────────────────────────────
    /**
     * Khôi phục toàn bộ các thuộc tính có thể bị biến đổi bởi Modifier về giá trị mặc định,
     * ngăn chặn rò rỉ trạng thái giữa các lần Recomposition.
     */
    fun resetModifierState(
        defaultSizeFlagH: SizeFlag = SizeFlag.FILL,
        defaultSizeFlagV: SizeFlag = SizeFlag.FILL
    ) {
        // Intrinsic size constraints
        minWidth = 0f;  minHeight = 0f
        maxWidth = Float.MAX_VALUE;  maxHeight = Float.MAX_VALUE

        // Insets
        paddingLeft = 0f;  paddingTop = 0f;  paddingRight = 0f;  paddingBottom = 0f
        marginLeft = 0f;   marginTop = 0f;   marginRight = 0f;   marginBottom = 0f

        // Layout policy & alignment
        sizeFlagHorizontal = defaultSizeFlagH;  sizeFlagVertical = defaultSizeFlagV
        stretchRatio = 0f
        alignHorizontal = Alignment.START;  alignVertical = Alignment.START
        offsetX = 0f;  offsetY = 0f

        // Visual tokens
        backgroundColor = null
        textColor = Color.white;  font = null;  text = null;  alpha = 1f

        // Shape, border & clip
        cornerRadiusTopStart = 0f;  cornerRadiusTopEnd = 0f
        cornerRadiusBottomEnd = 0f;  cornerRadiusBottomStart = 0f
        borderWidth = 0f;  borderColor = Color.clear
        clipHorizontal = false;  clipVertical = false
        clipRadiusTopStart = 0f;  clipRadiusTopEnd = 0f
        clipRadiusBottomEnd = 0f;  clipRadiusBottomStart = 0f

        // Pointer interaction
        _pointerInputFilters.clear()
        anchor.reset()

        // Scroll state
        isScrollableVertical = false;  isScrollableHorizontal = false
        maxScrollX = 0f;  maxScrollY = 0f
        scrollX = 0f;  scrollY = 0f
        contentWidth = 0f;  contentHeight = 0f
        verticalScrollState = null;  horizontalScrollState = null
    }

    fun isAncestorOf(node: LayoutNode): Boolean {
        var p = node.parent
        while (p != null) {
            if (p === this) return true
            p = p.parent
        }
        return false
    }

    fun addChild(child: LayoutNode) = addChild(child, _children.size)

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
        val moved = ArrayList<LayoutNode>(safeCount)
        repeat(safeCount) {
            moved.add(_children.removeAt(safeFrom))
        }
        val targetIndex = if (to > safeFrom) to - safeCount else to
        val safeDest = targetIndex.coerceIn(0, _children.size)
        _children.addAll(safeDest, moved)
    }

    fun clearChildren() {
        _children.fastForEach { it.parent = null }
        _children.clear()
    }

    /**
     * Duyệt đệ quy toàn bộ cây con xuất phát từ node này (bao gồm chính node này).
     */
    fun forEachInSubtree(action: (LayoutNode) -> Unit) {
        action(this)
        _children.fastForEach { it.forEachInSubtree(action) }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 9. 1D AXIS PROJECTIONS & SYMMETRY
    // ─────────────────────────────────────────────────────────────────────────
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

    fun setContentSize(orientation: Orientation, main: Float, cross: Float) {
        when (orientation) {
            Orientation.HORIZONTAL -> { contentWidth = main;  contentHeight = cross }
            Orientation.VERTICAL   -> { contentWidth = cross; contentHeight = main  }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 10. MEASUREMENT, ANCHORS & LAYOUT EXECUTION
    // ─────────────────────────────────────────────────────────────────────────
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
        when (orientation) {
            Orientation.HORIZONTAL -> arrange(mainPos, crossPos, mainSize, crossSize)
            Orientation.VERTICAL   -> arrange(crossPos, mainPos, crossSize, mainSize)
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

        val arrangeW = if (isScrollableHorizontal && contentWidth > innerW) contentWidth else innerW
        val arrangeH = if (isScrollableVertical && contentHeight > innerH) contentHeight else innerH

        policy.arrangeChildren(this, innerX, innerY, arrangeW, arrangeH)

        maxScrollX = maxOf(0f, contentWidth - innerW)
        maxScrollY = maxOf(0f, contentHeight - innerH)

        verticalScrollState?.syncIfChanged(viewport = innerH, max = maxScrollY)
        horizontalScrollState?.syncIfChanged(viewport = innerW, max = maxScrollX)
    }

    private fun org.hubdustry.core.compose.foundation.ScrollState.syncIfChanged(viewport: Float, max: Float) {
        if (kotlin.math.abs(viewportSize - viewport) > 0.001f) {
            viewportSize = viewport
        }
        if (kotlin.math.abs(maxValue - max) > 0.001f) {
            maxValue = max
        }
    }

    /**
     * Tự giải neo trong không gian của cha và kích hoạt bố cục nội dung con.
     */
    fun resolveAnchors(innerX: Float, innerY: Float, innerWidth: Float, innerHeight: Float) {
        // Horizontal axis
        val targetLeft  = anchor.resolveLeft(innerX, innerWidth)
        val targetRight = anchor.resolveRight(innerX, innerWidth)
        val finalW = when {
            anchor.hasExplicitWidth -> maxOf(minWidth, targetRight - targetLeft)
            width > 0f              -> width
            else                    -> minWidth
        }.coerceIn(minWidth, maxWidth)
        val finalX = if (anchor.hasExplicitWidth) targetLeft else targetLeft - finalW * anchor.anchorLeft

        // Vertical axis
        val targetTop    = anchor.resolveTop(innerY, innerHeight)
        val targetBottom = anchor.resolveBottom(innerY, innerHeight)
        val finalH = when {
            anchor.hasExplicitHeight -> maxOf(minHeight, targetBottom - targetTop)
            height > 0f              -> height
            else                     -> minHeight
        }.coerceIn(minHeight, maxHeight)
        val finalY = if (anchor.hasExplicitHeight) targetTop else targetTop - finalH * anchor.anchorTop

        arrange(finalX, finalY, finalW, finalH)
    }

    /**
     * Entry point: Bắt đầu tính toán toàn bộ cây layout từ node này.
     */
    @JvmOverloads
    fun layout(availableWidth: Float, availableHeight: Float, exact: Boolean = false) {
        val safeW = if (availableWidth.isNaN() || availableWidth < 0f) 0f else availableWidth
        val safeH = if (availableHeight.isNaN() || availableHeight < 0f) 0f else availableHeight

        policy.computeMinSize(this)

        val isExactW = exact && safeW > 0f
        this.width = when {
            anchor.isEnabled || isExactW -> safeW
            safeW == Float.MAX_VALUE || safeW.isInfinite() -> minWidth
            sizeFlagHorizontal == SizeFlag.SHRINK -> minWidth
            else -> maxOf(minWidth, safeW)
        }.coerceIn(if (isExactW) 0f else minWidth, maxWidth)

        val isExactH = exact && safeH > 0f
        this.height = when {
            anchor.isEnabled || isExactH -> safeH
            safeH == Float.MAX_VALUE || safeH.isInfinite() -> minHeight
            sizeFlagVertical == SizeFlag.SHRINK -> minHeight
            else -> maxOf(minHeight, safeH)
        }.coerceIn(if (isExactH) 0f else minHeight, maxHeight)

        arrangeContent()
    }
}
