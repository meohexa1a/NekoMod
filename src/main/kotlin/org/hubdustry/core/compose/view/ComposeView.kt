package org.hubdustry.core.compose.view

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Composition
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.util.fastForEach
import arc.Core
import arc.graphics.Color
import arc.graphics.g2d.Draw
import arc.input.KeyCode
import arc.scene.Element
import arc.scene.Scene
import arc.scene.event.InputEvent
import arc.scene.event.InputListener
import arc.util.Log
import org.hubdustry.core.compose.runtime.CompositionManager
import org.hubdustry.core.compose.runtime.LayoutNodeApplier
import org.hubdustry.core.compose.input.ime.SdlReflectionImeBridge
import org.hubdustry.core.compose.input.Offset
import org.hubdustry.core.compose.input.PointerButton
import org.hubdustry.core.compose.input.PointerEventType
import org.hubdustry.core.compose.input.PointerType
import org.hubdustry.core.graphics.UIBatch
import org.hubdustry.core.layout.LayoutNode

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

    internal val rootLayoutNode: LayoutNode = LayoutNode()
    internal val inputDispatcher: InputDispatcher = InputDispatcher(rootLayoutNode)

    private var composition: Composition? = null
    private var composableContent: (@Composable () -> Unit)? = null

    private var lastLayoutW = -1f
    private var lastLayoutH = -1f
    private var layoutIteration = 0
    private var isLayoutDirty = true

    /** Màu nền của toàn bộ ComposeView (gán trực tiếp cho rootLayoutNode). */
    var backgroundColor: Color?
        get() = rootLayoutNode.backgroundColor
        set(value) {
            rootLayoutNode.backgroundColor = value
        }

    private fun toComposeY(arcY: Float): Float =
        (if (height > 0f) height else rootLayoutNode.height) - arcY

    private fun KeyCode?.toPointerButton(): PointerButton = when (this) {
        KeyCode.mouseLeft -> PointerButton.Primary
        KeyCode.mouseRight -> PointerButton.Secondary
        KeyCode.mouseMiddle -> PointerButton.Tertiary
        else -> PointerButton.Primary
    }

    private fun KeyCode?.toPointerType(): PointerType =
        if (this != null) PointerType.Mouse else PointerType.Touch

    init {
        // Khởi động bộ máy CompositionManager nếu chưa chạy
        CompositionManager.start()

        // Gắn InputListener thuần của Arc để đón bắt sự kiện con trỏ thô mà không dùng ClickListener
        addListener(object : InputListener() {
            override fun touchDown(event: InputEvent?, x: Float, y: Float, pointer: Int, button: KeyCode?): Boolean =
                inputDispatcher.touchDown(x, toComposeY(y), pointer, System.currentTimeMillis(), button.toPointerButton(), button.toPointerType())

            override fun touchDragged(event: InputEvent?, x: Float, y: Float, pointer: Int) {
                inputDispatcher.touchMove(x, toComposeY(y), pointer, System.currentTimeMillis())
            }

            override fun touchUp(event: InputEvent?, x: Float, y: Float, pointer: Int, button: KeyCode?) {
                inputDispatcher.touchUp(x, toComposeY(y), pointer, System.currentTimeMillis(), button.toPointerButton(), button.toPointerType())
            }

            override fun mouseMoved(event: InputEvent?, x: Float, y: Float): Boolean =
                inputDispatcher.mouseMove(x, toComposeY(y), System.currentTimeMillis())

            override fun scrolled(event: InputEvent?, x: Float, y: Float, amountX: Float, amountY: Float): Boolean {
                val isShift = try { arc.Core.input?.shift() == true } catch (_: Throwable) { false }
                val delta = if (isShift) Offset(amountY, 0f) else Offset(amountX, amountY)
                return inputDispatcher.scroll(x, toComposeY(y), delta, System.currentTimeMillis())
            }

            override fun exit(event: InputEvent?, x: Float, y: Float, pointer: Int, toActor: Element?) {
                if (pointer == -1) {
                    inputDispatcher.mouseExit(System.currentTimeMillis())
                }
            }

            override fun keyDown(event: InputEvent?, keycode: KeyCode?): Boolean =
                inputDispatcher.keyDown(keycode)

            override fun keyUp(event: InputEvent?, keycode: KeyCode?): Boolean =
                inputDispatcher.keyUp(keycode)

            override fun keyTyped(event: InputEvent?, character: Char): Boolean =
                inputDispatcher.keyTyped(character)
        })
    }

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
            // GẮN VÀO SCENE: Khởi động composition nếu có nội dung
            ensureCompositionStarted()
            composableContent?.let { content ->
                composition?.setContent {
                    CompositionLocalProvider(LocalComposeView provides this) {
                        content()
                    }
                }
            }
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
        if (composition == null) {
            val comp = Composition(
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
            this.composition = comp
            composableContent?.let { content ->
                comp.setContent {
                    CompositionLocalProvider(LocalComposeView provides this) {
                        content()
                    }
                }
            }
        }
    }

    open fun dispose() {
        // Phase 1: Cancel active interactions before tearing down
        SdlReflectionImeBridge.stopSession()
        clearKeyboardFocus()
        inputDispatcher.cancelAllActivePointers(System.currentTimeMillis())
        disposeNodeRecursive(rootLayoutNode)

        // Phase 2: Dispose the Compose Runtime composition
        try {
            composition?.dispose()
        } catch (t: Throwable) {
            Log.err("[ComposeView] Error disposing composition", t)
        }
        composition = null

        // Phase 3: Reset dispatcher and layout bookkeeping
        inputDispatcher.dispose()
        lastLayoutW = -1f
        lastLayoutH = -1f
        rootLayoutNode.clearChildren()
    }

    private fun disposeNodeRecursive(node: LayoutNode) {
        node.pointerInputFilters.fastForEach { it.reset() }
        node.clearPointerInputFilters()
        node.children.fastForEach { disposeNodeRecursive(it) }
    }

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
    ): Boolean = inputDispatcher.sendPointerInput(type, x, y, pointer, uptimeMillis, button, pointerType, scrollDelta)

    fun handleScroll(
        composeX: Float,
        composeY: Float,
        scrollDelta: Offset,
        uptime: Long = System.currentTimeMillis()
    ): Boolean = inputDispatcher.scroll(composeX, composeY, scrollDelta, uptime)

    fun cancelAllActivePointers(uptime: Long = System.currentTimeMillis()) {
        inputDispatcher.cancelAllActivePointers(uptime)
    }

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
        if (handler == null || inputDispatcher.focusedKeyHandler === handler) {
            inputDispatcher.focusedKeyHandler = null
            val activeScene = scene ?: Core.scene
            if (activeScene != null && activeScene.keyboardFocus === this) {
                activeScene.keyboardFocus = null
            }
        }
    }

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
        val minW = rootLayoutNode.minWidth
        return if (minW > 0f) minW else super.getPrefWidth()
    }

    override fun getPrefHeight(): Float {
        rootLayoutNode.policy.computeMinSize(rootLayoutNode)
        val minH = rootLayoutNode.minHeight
        return if (minH > 0f) minH else super.getPrefHeight()
    }

    override fun getMinWidth(): Float = getPrefWidth()
    override fun getMinHeight(): Float = getPrefHeight()

    override fun hit(x: Float, y: Float, touchable: Boolean): Element? {
        if (touchable && this.touchable != arc.scene.event.Touchable.enabled) return null
        if (!visible) return null
        val effectiveW = if (width > 0f) width else rootLayoutNode.width
        val effectiveH = if (height > 0f) height else rootLayoutNode.height
        return if (x in 0f..effectiveW && y in 0f..effectiveH) this else null
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
        val currentW = width
        val currentH = height
        if (currentW <= 0f || currentH <= 0f) return
        if (!isLayoutDirty && currentW == lastLayoutW && currentH == lastLayoutH) return

        layoutIteration++
        if (layoutIteration > 100) {
            Log.err("[ComposeView] Potential infinite layout loop detected ($layoutIteration iterations)! Clamping layout.")
            isLayoutDirty = false
            layoutIteration = 0
            return
        }

        // Host Window phân phối ràng buộc kích thước chuẩn xuống Node gốc trong 1 pass duy nhất
        rootLayoutNode.layout(currentW, currentH, exact = true)
        lastLayoutW = currentW
        lastLayoutH = currentH
        isLayoutDirty = false
        layoutIteration = 0
    }

    override fun draw() {
        if (!visible || width <= 0f || height <= 0f) return
        validate()

        // Safety net: draw() có thể được gọi trực tiếp bởi Stage mà không qua act()
        if (isLayoutDirty) {
            rootLayoutNode.layout(width, height, exact = true)
            lastLayoutW = width
            lastLayoutH = height
            isLayoutDirty = false
        }

        val viewH = if (height > 0f) height else rootLayoutNode.height
        try {
            UIBatch.begin()
            try {
                NodeRenderer.render(rootLayoutNode, this.x, this.y, viewH)
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

/**
 * Cú pháp DSL thuận tiện để nhúng trực tiếp [ComposeView] vào bất kỳ [arc.scene.ui.layout.Table] nào của Arc Scene2D.
 * Trả về [arc.scene.ui.layout.Cell] để caller tự do chain các thuộc tính layout Scene2D (.size, .grow, .pad, .row,...).
 */
fun arc.scene.ui.layout.Table.compose(
    content: @Composable () -> Unit
): arc.scene.ui.layout.Cell<ComposeView> {
    val view = ComposeView().apply {
        setContent(content)
    }
    return this.add(view)
}
