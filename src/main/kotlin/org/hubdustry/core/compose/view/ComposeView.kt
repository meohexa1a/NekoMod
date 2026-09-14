package org.hubdustry.core.compose.view

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Composition
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.util.fastForEach
import arc.Core
import arc.graphics.Color
import arc.graphics.g2d.Draw
import arc.scene.Element
import arc.scene.Scene
import arc.scene.ui.layout.Cell
import arc.scene.ui.layout.Table
import arc.util.Log
import org.hubdustry.core.compose.input.KeyboardInputHandler
import org.hubdustry.core.compose.input.Offset
import org.hubdustry.core.compose.input.PointerButton
import org.hubdustry.core.compose.input.PointerEventType
import org.hubdustry.core.compose.input.PointerType
import org.hubdustry.core.compose.runtime.CompositionManager
import org.hubdustry.core.compose.runtime.LayoutNodeApplier
import org.hubdustry.core.graphics.UIBatch
import org.hubdustry.core.layout.LayoutNode

private const val MAX_LAYOUT_ITERATIONS = 100

/**
 * Cửa khẩu Mount-Point mỏng (Slim Coordinator) giữa Arc Scene2D và Jetpack Compose Runtime.
 *
 * KIẾN TRÚC V3 (Sau tái cấu trúc Đợt 2):
 * 1. Đóng vai trò là một [Element] chuẩn của Arc Scene2D.
 * 2. Độc lập hóa trách nhiệm:
 *    - Điều phối sự kiện con trỏ và cử chỉ: Ủy quyền cho [InputDispatcher].
 *    - Dựng hình Virtual DOM đệ quy ra GPU: Ủy quyền cho [NodeRenderer].
 *    - Điều phối vòng đời Recomposition: Tích hợp với [CompositionManager].
 * 3. Tự động đồng bộ kích thước hai chiều (Two-Way Auto-Sync):
 *    - Arc -> Compose: Truyền kích thước Scene2D vào [LayoutNode.layout].
 *    - Compose -> Arc: Cung cấp [getPrefWidth] và [getPrefHeight] dựa trên intrinsic min size.
 * 4. Bức tường Berlin:
 *    - Input từ Arc vào: `composeY = height - arcY`.
 *    - Render ra Arc: Bù trừ tọa độ Y-up trong [NodeRenderer].
 */
val LocalComposeView = staticCompositionLocalOf<ComposeView?> { null }

open class ComposeView : Element() {

    // ─────────────────────────────────────────────────────────────────────────
    // 1. VIRTUAL DOM ROOT & ADAPTER STATE
    // ─────────────────────────────────────────────────────────────────────────

    internal val rootLayoutNode: LayoutNode = LayoutNode()
    internal val inputDispatcher: InputDispatcher = InputDispatcher(rootLayoutNode)

    private var composition: Composition? = null
    private var composableContent: (@Composable () -> Unit)? = null

    private var lastLayoutWidth = -1f
    private var lastLayoutHeight = -1f
    private var layoutIteration = 0
    private var isLayoutDirty = true

    /** Màu nền của toàn bộ ComposeView (gán trực tiếp cho rootLayoutNode). */
    var backgroundColor: Color?
        get() = rootLayoutNode.backgroundColor
        set(value) {
            rootLayoutNode.backgroundColor = value
        }

    private val inputAdapter = ArcInputAdapter(this, inputDispatcher)

    init {
        // Khởi động bộ máy CompositionManager nếu chưa chạy
        CompositionManager.start()

        // Gắn ArcInputAdapter đón bắt sự kiện thô và quản lý scrollFocus
        addListener(inputAdapter)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 2. COMPOSITION LIFECYCLE & MOUNT MANAGEMENT
    // ─────────────────────────────────────────────────────────────────────────

    fun setContent(content: @Composable () -> Unit) {
        this.composableContent = content

        ensureCompositionStarted()
        composition?.setContent {
            CompositionLocalProvider(LocalComposeView provides this) {
                content()
            }
        }

        rootLayoutNode.policy.computeMinSize(rootLayoutNode)
        invalidateHierarchy()
    }

    override fun setScene(stage: Scene?) {
        val oldScene = scene
        super.setScene(stage)

        if (stage != null && oldScene == null) {
            // GẮN VÀO SCENE: Khởi động composition nếu chưa được khởi động từ trước
            ensureCompositionStarted()
            requestLayout()
            invalidateHierarchy()
        } else if (stage == null && oldScene != null) {
            // GỠ KHỎI SCENE: Giải phóng composition và node
            dispose()
        }
    }

    /**
     * Đánh dấu cây layout cần được tính toán lại trước khi render tiếp theo.
     */
    fun requestLayout() {
        isLayoutDirty = true
    }

    private fun ensureCompositionStarted() {
        if (composition != null) return

        val newComposition = Composition(
            applier = LayoutNodeApplier(
                root = rootLayoutNode,
                onNodeRemovedCallback = { node ->
                    disposeNodeRecursive(node)
                    inputDispatcher.onNodeRemoved(node)
                },
                onEndChangesCallback = {
                    requestLayout()
                }
            ),
            parent = CompositionManager.recomposer
        )
        this.composition = newComposition
        composableContent?.let { content ->
            newComposition.setContent {
                CompositionLocalProvider(LocalComposeView provides this) {
                    content()
                }
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 3. DETERMINISTIC TEARDOWN & DISPOSAL
    // ─────────────────────────────────────────────────────────────────────────

    open fun dispose() {
        // Phase 1: Cancel active interactions before tearing down
        inputAdapter.releaseScrollFocus()
        clearKeyboardFocus()
        inputDispatcher.cancelAllActivePointers(System.currentTimeMillis())
        disposeNodeRecursive(rootLayoutNode)

        // Phase 2: Dispose the Compose Runtime composition
        try {
            composition?.dispose()
        } catch (throwable: Throwable) {
            Log.err("[ComposeView] Error disposing composition", throwable)
        }
        composition = null

        // Phase 3: Reset dispatcher and layout bookkeeping
        inputDispatcher.dispose()
        lastLayoutWidth = -1f
        lastLayoutHeight = -1f
        rootLayoutNode.clearChildren()
    }

    private fun disposeNodeRecursive(node: LayoutNode) {
        node.pointerInputFilters.fastForEach { it.reset() }
        node.clearPointerInputFilters()
        node.children.fastForEach { disposeNodeRecursive(it) }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 4. POINTER & KEYBOARD INPUT DELEGATION
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Entry point nhận sự kiện con trỏ tổng quát (sử dụng trong kiểm thử và tích hợp mở rộng).
     */
    fun sendPointerInput(
        type: PointerEventType,
        x: Float,
        y: Float,
        pointer: Int = 0,
        uptimeMillis: Long = System.currentTimeMillis(),
        button: PointerButton? = PointerButton.Primary,
        pointerType: PointerType = PointerType.Touch,
        scrollDelta: Offset = Offset.Zero
    ): Boolean = inputDispatcher.sendPointerInput(
        type = type,
        x = x,
        y = y,
        pointer = pointer,
        uptimeMillis = uptimeMillis,
        button = button,
        pointerType = pointerType,
        scrollDelta = scrollDelta
    )

    fun handleScroll(
        composeX: Float,
        composeY: Float,
        scrollDelta: Offset,
        uptime: Long = System.currentTimeMillis()
    ): Boolean = inputDispatcher.scroll(composeX, composeY, scrollDelta, uptime)

    fun cancelAllActivePointers(uptime: Long = System.currentTimeMillis()) =
        inputDispatcher.cancelAllActivePointers(uptime)

    /**
     * Yêu cầu tiêu điểm bàn phím cho [handler] và đồng bộ [keyboardFocus] trên Stage Scene2D.
     */
    fun requestKeyboardFocus(handler: KeyboardInputHandler) {
        inputDispatcher.focusedKeyHandler = handler
        (scene ?: Core.scene)?.keyboardFocus = this
    }

    /**
     * Hủy tiêu điểm bàn phím của [handler] (hoặc toàn bộ nếu [handler] là null) và dọn dẹp Stage Scene2D.
     */
    fun clearKeyboardFocus(handler: KeyboardInputHandler? = null) {
        if (handler != null && inputDispatcher.focusedKeyHandler !== handler) return

        inputDispatcher.focusedKeyHandler = null
        val activeScene = scene ?: Core.scene
        if (activeScene?.keyboardFocus === this) {
            activeScene.keyboardFocus = null
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 5. SCENE2D SIZING & AUTO-LAYOUT INTEGRATION
    // ─────────────────────────────────────────────────────────────────────────

    override fun sizeChanged() {
        super.sizeChanged()
        isLayoutDirty = true
        updateDimensionsAndLayout()
    }

    override fun layout() {
        super.layout()
        updateDimensionsAndLayout()
    }

    override fun getPrefWidth(): Float {
        rootLayoutNode.policy.computeMinSize(rootLayoutNode)
        val minWidth = rootLayoutNode.minWidth
        return if (minWidth > 0f) minWidth else super.getPrefWidth()
    }

    override fun getPrefHeight(): Float {
        rootLayoutNode.policy.computeMinSize(rootLayoutNode)
        val minHeight = rootLayoutNode.minHeight
        return if (minHeight > 0f) minHeight else super.getPrefHeight()
    }

    override fun getMinWidth(): Float = getPrefWidth()
    override fun getMinHeight(): Float = getPrefHeight()

    override fun hit(x: Float, y: Float, touchable: Boolean): Element? {
        if (touchable && this.touchable != arc.scene.event.Touchable.enabled) return null
        if (!visible) return null
        val effectiveWidth = if (width > 0f) width else rootLayoutNode.width
        val effectiveHeight = if (height > 0f) height else rootLayoutNode.height
        return if (x in 0f..effectiveWidth && y in 0f..effectiveHeight) this else null
    }

    override fun act(delta: Float) {
        super.act(delta)
        // Đập nhịp đồng bộ
        CompositionManager.frame()
        updateDimensionsAndLayout()

        // Đồng bộ mất keyboard focus hai chiều với Arc Scene2D Stage
        val activeScene = scene ?: Core.scene
        if (activeScene != null && inputDispatcher.focusedKeyHandler != null && activeScene.keyboardFocus !== this) {
            inputDispatcher.focusedKeyHandler = null
        }
    }

    private fun updateDimensionsAndLayout() {
        val currentWidth = width
        val currentHeight = height
        if (currentWidth <= 0f || currentHeight <= 0f) return
        if (!isLayoutDirty && currentWidth == lastLayoutWidth && currentHeight == lastLayoutHeight) return

        layoutIteration++
        if (layoutIteration > MAX_LAYOUT_ITERATIONS) {
            Log.err("[ComposeView] Potential infinite layout loop detected ($layoutIteration iterations)! Clamping layout.")
            isLayoutDirty = false
            layoutIteration = 0
            return
        }

        // Host Window phân phối ràng buộc kích thước chuẩn xuống Node gốc trong 1 pass duy nhất
        rootLayoutNode.layout(currentWidth, currentHeight, exact = true)
        lastLayoutWidth = currentWidth
        lastLayoutHeight = currentHeight
        isLayoutDirty = false
        layoutIteration = 0
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 6. BERLIN WALL DRAW GATEWAY
    // ─────────────────────────────────────────────────────────────────────────

    override fun draw() {
        if (!visible || width <= 0f || height <= 0f) return
        validate()

        // Safety net: draw() có thể được gọi trực tiếp bởi Stage mà không qua act()
        if (isLayoutDirty) {
            rootLayoutNode.layout(width, height, exact = true)
            lastLayoutWidth = width
            lastLayoutHeight = height
            isLayoutDirty = false
        }

        val viewHeight = if (height > 0f) height else rootLayoutNode.height
        try {
            UIBatch.begin()
            try {
                NodeRenderer.render(
                    rootNode = rootLayoutNode,
                    viewX = this.x,
                    viewY = this.y,
                    viewHeight = viewHeight
                )
            } finally {
                UIBatch.end()
            }
        } finally {
            try {
                Draw.reset()
            } catch (_: Throwable) {
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 7. SCENE2D TABLE INTEGRATION DSL
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Cú pháp DSL thuận tiện để nhúng trực tiếp [ComposeView] vào bất kỳ [Table] nào của Arc Scene2D.
 * Trả về [Cell] để caller tự do chain các thuộc tính layout Scene2D (.size, .grow, .pad, .row,...).
 */
fun Table.compose(
    content: @Composable () -> Unit
): Cell<ComposeView> {
    val view = ComposeView().apply {
        setContent(content)
    }
    return this.add(view)
}
