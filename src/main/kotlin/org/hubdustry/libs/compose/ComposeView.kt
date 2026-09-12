package org.hubdustry.libs.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Composition
import arc.graphics.Color
import arc.graphics.g2d.Draw
import arc.graphics.g2d.Fill
import arc.graphics.g2d.Lines
import arc.input.KeyCode
import arc.scene.Element
import arc.scene.Scene
import arc.scene.event.InputEvent
import arc.scene.event.InputListener
import arc.util.Log
import mindustry.ui.Fonts
import org.hubdustry.libs.compose.input.ConsumedData
import org.hubdustry.libs.compose.input.IntSize
import org.hubdustry.libs.compose.input.Offset
import org.hubdustry.libs.compose.input.PointerButton
import org.hubdustry.libs.compose.input.PointerEvent
import org.hubdustry.libs.compose.input.PointerEventPass
import org.hubdustry.libs.compose.input.PointerEventType
import org.hubdustry.libs.compose.input.PointerId
import org.hubdustry.libs.compose.input.PointerInputChange
import org.hubdustry.libs.compose.input.PointerType
import org.hubdustry.libs.layout.LayoutNode

/**
 * Cửa khẩu Mount-Point giữa Arc Scene2D và Jetpack Compose Runtime.
 *
 * BẢO TỒN VÀ NÂNG CẤP V3:
 * 1. Đóng vai trò là một [Element] chuẩn của Arc Scene2D.
 * 2. Tự động đồng bộ kích thước hai chiều (Two-Way Auto-Sync):
 *    - Arc -> Compose: Tự động truyền kích thước Scene2D vào [LayoutNode.layout].
 *    - Compose -> Arc: Cung cấp [getPrefWidth] và [getPrefHeight] dựa trên [LayoutNode.minWidth]/[LayoutNode.minHeight].
 * 3. Vòng đời gắn chặt vào `setScene(stage)`:
 *    - `stage != null`: Khởi tạo [Composition] cục bộ kết nối với [CompositionManager.recomposer].
 *    - `stage == null`: Tự động gọi `dispose()` giải phóng triệt để tài nguyên, cắt đứt tham chiếu.
 * 4. Hot-Path Zero-GC:
 *    - `act(delta)`: Kích hoạt frame tick và layout lại root node nếu kích thước thay đổi.
 *    - `draw()`: Bức tường Berlin trục Y:
 *      `arcX = this.x + node.x`
 *      `arcY = this.y + this.height - (node.y + node.height)`
 *      Duyệt đệ quy cây con bằng vòng lặp chỉ mục thuần túy (`for (i in 0 until count)`).
 * 5. Bộ máy Pointer Input AOSP bóc tách có chọn lọc:
 *    - Bức tường Berlin tọa độ đầu vào: `composeX = arcX`, `composeY = height - arcY`.
 *    - Hit-Testing tìm chuỗi node chứa con trỏ có [LayoutNode.pointerInputFilter].
 *    - Điều phối sự kiện 3-pass (Initial -> Main -> Final) không dùng ClickListener của Arc.
 */
open class ComposeView : Element() {

    val rootLayoutNode: LayoutNode = LayoutNode()

    private var composition: Composition? = null
    private var composableContent: (@Composable () -> Unit)? = null

    // Bộ theo dõi con trỏ đang hoạt động (Pointer Tracking)
    private val trackedPointers = HashMap<Int, PointerHitRecord>()
    private val hitScratch = ArrayList<LayoutNodeHit>()
    private val hitPool = ArrayList<LayoutNodeHit>()
    private var hitPoolIndex = 0

    // Scratchpad tái sử dụng trong dispatch3Pass để triệt tiêu cấp phát collection trong hot-path
    private val eventsScratch = ArrayList<PointerEvent>()
    private val boundsScratch = ArrayList<IntSize>()
    private val previousHoverChain = ArrayList<LayoutNodeHit>()
    private val exitedHoverScratch = ArrayList<LayoutNodeHit>()
    private val hoverPool = ArrayList<LayoutNodeHit>()

    private fun obtainHit(node: LayoutNode, absX: Float, absY: Float): LayoutNodeHit {
        val hit = if (hitPoolIndex < hitPool.size) {
            val existing = hitPool[hitPoolIndex]
            existing.node = node
            existing.absX = absX
            existing.absY = absY
            existing
        } else {
            val newHit = LayoutNodeHit(node, absX, absY)
            hitPool.add(newHit)
            newHit
        }
        hitPoolIndex++
        return hit
    }

    /** Màu nền của toàn bộ ComposeView (gán trực tiếp cho rootLayoutNode). */
    var backgroundColor: Color?
        get() = rootLayoutNode.backgroundColor
        set(value) {
            rootLayoutNode.backgroundColor = value
        }

    /**
     * Bật/tắt chế độ debug trực quan: Tự động vẽ khung viền (Gold outline) bao quanh
     * chính xác Hit-Test Bounding Box của tất cả các widget có thể tương tác ([LayoutNode.pointerInputFilter]),
     * đồng thời hiển thị dấu chữ thập tại vị trí click của con trỏ chuột.
     */
    var debugDrawHitBounds: Boolean = false

    private var lastClickComposeX = -1f
    private var lastClickComposeY = -1f
    private var lastClickTime = 0L
    private var lastClickHitSuccess = false

    init {
        // Khởi động bộ máy CompositionManager nếu chưa chạy
        CompositionManager.start()

        // Gắn InputListener thuần của Arc để đón bắt sự kiện con trỏ thô mà không dùng ClickListener
        addListener(object : InputListener() {
            override fun touchDown(event: InputEvent?, x: Float, y: Float, pointer: Int, button: KeyCode?): Boolean {
                // BỨC TƯỜNG BERLIN TRỤC Y: Chuyển đổi Arc Bottom-Left sang NekoMod Top-Left (Y-down)
                val viewH = if (height > 0f) height else rootLayoutNode.height
                val composeX = x
                val composeY = viewH - y
                val pType = if (button != null) PointerType.Mouse else PointerType.Touch
                val pButton = when (button) {
                    KeyCode.mouseLeft -> PointerButton.Primary
                    KeyCode.mouseRight -> PointerButton.Secondary
                    KeyCode.mouseMiddle -> PointerButton.Tertiary
                    else -> PointerButton.Primary
                }
                return handleTouchDown(composeX, composeY, pointer, System.currentTimeMillis(), pButton, pType)
            }

            override fun touchDragged(event: InputEvent?, x: Float, y: Float, pointer: Int) {
                val viewH = if (height > 0f) height else rootLayoutNode.height
                val composeX = x
                val composeY = viewH - y
                handleTouchMove(composeX, composeY, pointer, System.currentTimeMillis())
            }

            override fun touchUp(event: InputEvent?, x: Float, y: Float, pointer: Int, button: KeyCode?) {
                val viewH = if (height > 0f) height else rootLayoutNode.height
                val composeX = x
                val composeY = viewH - y
                val pType = if (button != null) PointerType.Mouse else PointerType.Touch
                val pButton = when (button) {
                    KeyCode.mouseLeft -> PointerButton.Primary
                    KeyCode.mouseRight -> PointerButton.Secondary
                    KeyCode.mouseMiddle -> PointerButton.Tertiary
                    else -> PointerButton.Primary
                }
                handleTouchUp(composeX, composeY, pointer, System.currentTimeMillis(), pButton, pType)
            }

            override fun mouseMoved(event: InputEvent?, x: Float, y: Float): Boolean {
                val viewH = if (height > 0f) height else rootLayoutNode.height
                val composeX = x
                val composeY = viewH - y
                return handleMouseMove(composeX, composeY, System.currentTimeMillis())
            }

            override fun exit(event: InputEvent?, x: Float, y: Float, pointer: Int, toActor: Element?) {
                if (pointer == -1) {
                    handleMouseMove(-1000f, -1000f, System.currentTimeMillis())
                }
            }
        })
    }

    fun setContent(content: @Composable () -> Unit) {
        this.composableContent = content
        ensureCompositionStarted()
        composition?.setContent(content)
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
                composition?.setContent(content)
            }
        } else if (stage == null && oldScene != null) {
            // GỠ KHỎI SCENE: Giải phóng dứt điểm
            dispose()
        }
    }

    private var isLayoutDirty = true

    /**
     * Đánh dấu cây layout cần được tính toán lại trước khi render tiếp theo.
     */
    fun requestLayout() {
        isLayoutDirty = true
    }

    private fun ensureCompositionStarted() {
        if (composition == null) {
            val comp = Composition(
                applier = LayoutNodeApplier(rootLayoutNode) {
                    requestLayout()
                },
                parent = CompositionManager.recomposer
            )
            this.composition = comp
            composableContent?.let { content ->
                comp.setContent(content)
            }
        }
    }

    open fun dispose() {
        disposeNodeRecursive(rootLayoutNode)
        try {
            composition?.dispose()
        } catch (t: Throwable) {
            Log.err("[ComposeView] Error disposing composition", t)
        }
        composition = null
        composableContent = null
        trackedPointers.clear()
        hitScratch.clear()
        hitPool.clear()
        hitPoolIndex = 0
        eventsScratch.clear()
        boundsScratch.clear()
        previousHoverChain.clear()
        hoverPool.clear()
        exitedHoverScratch.clear()
        lastLayoutW = -1f
        lastLayoutH = -1f
        rootLayoutNode.clearChildren()
    }

    private fun disposeNodeRecursive(node: LayoutNode) {
        val filters = node.pointerInputFilters
        val fCount = filters.size
        for (i in 0 until fCount) {
            filters[i].reset()
        }
        node.clearPointerInputFilters()
        val children = node.children
        val count = children.size
        for (i in 0 until count) {
            disposeNodeRecursive(children[i])
        }
    }

    /**
     * Gửi sự kiện con trỏ trực tiếp (dùng cho Headless Unit Testing hoặc điều khiển ngoại vi).
     * Tọa độ [x], [y] là tọa độ Top-Left (Y-down) nội bộ của NekoMod Compose.
     */
    fun sendPointerInput(
        type: PointerEventType,
        x: Float,
        y: Float,
        pointer: Int = 0,
        uptimeMillis: Long = System.currentTimeMillis(),
        button: PointerButton? = PointerButton.Primary,
        pointerType: PointerType = PointerType.Touch
    ): Boolean {
        return when (type) {
            PointerEventType.Press -> handleTouchDown(x, y, pointer, uptimeMillis, button, pointerType)
            PointerEventType.Move -> {
                if (trackedPointers.containsKey(pointer)) {
                    handleTouchMove(x, y, pointer, uptimeMillis)
                    true
                } else {
                    handleMouseMove(x, y, uptimeMillis)
                }
            }
            PointerEventType.Release -> {
                handleTouchUp(x, y, pointer, uptimeMillis, button, pointerType)
                true
            }
            PointerEventType.Enter, PointerEventType.Exit, PointerEventType.Scroll, PointerEventType.Unknown -> false
        }
    }

    private fun handleTouchDown(
        composeX: Float,
        composeY: Float,
        pointer: Int,
        uptime: Long,
        button: PointerButton? = PointerButton.Primary,
        pointerType: PointerType = PointerType.Touch
    ): Boolean {
        hitPoolIndex = 0
        hitScratch.clear()
        hitTestChain(rootLayoutNode, parentAbsX = 0f, parentAbsY = 0f, targetX = composeX, targetY = composeY, result = hitScratch)

        lastClickComposeX = composeX
        lastClickComposeY = composeY
        lastClickTime = uptime
        lastClickHitSuccess = hitScratch.isNotEmpty()

        if (debugDrawHitBounds) {
            Log.info("[ComposeView] touchDown: compose=($composeX, $composeY) | view=(${width}x${height}) root=(${rootLayoutNode.width}x${rootLayoutNode.height}) hitSuccess=$lastClickHitSuccess chainSize=${hitScratch.size}")
        }

        if (hitScratch.isEmpty()) return false

        val hitCount = hitScratch.size
        val chain = ArrayList<LayoutNodeHit>(hitCount)
        for (i in 0 until hitCount) {
            val h = hitScratch[i]
            chain.add(LayoutNodeHit(h.node, h.absX, h.absY))
        }
        trackedPointers[pointer] = PointerHitRecord(chain, composeX, composeY, uptime, button, pointerType)

        dispatch3Pass(
            chain = chain,
            composeX = composeX,
            composeY = composeY,
            pointer = pointer,
            uptime = uptime,
            pressed = true,
            previousUptime = uptime,
            previousComposeX = composeX,
            previousComposeY = composeY,
            previousPressed = false,
            eventType = PointerEventType.Press,
            pointerType = pointerType,
            button = button
        )
        return true
    }

    private fun handleTouchMove(composeX: Float, composeY: Float, pointer: Int, uptime: Long) {
        val record = trackedPointers[pointer] ?: return
        val prevX = record.lastComposeX
        val prevY = record.lastComposeY
        val prevUptime = record.lastUptime
        record.lastComposeX = composeX
        record.lastComposeY = composeY
        record.lastUptime = uptime

        dispatch3Pass(
            chain = record.chain,
            composeX = composeX,
            composeY = composeY,
            pointer = pointer,
            uptime = uptime,
            pressed = true,
            previousUptime = prevUptime,
            previousComposeX = prevX,
            previousComposeY = prevY,
            previousPressed = true,
            eventType = PointerEventType.Move,
            pointerType = record.pointerType,
            button = record.button
        )
    }

    private fun handleTouchUp(
        composeX: Float,
        composeY: Float,
        pointer: Int,
        uptime: Long,
        button: PointerButton? = PointerButton.Primary,
        pointerType: PointerType = PointerType.Touch
    ) {
        val record = trackedPointers.remove(pointer)
        val chain = record?.chain ?: run {
            hitPoolIndex = 0
            hitScratch.clear()
            hitTestChain(rootLayoutNode, parentAbsX = 0f, parentAbsY = 0f, targetX = composeX, targetY = composeY, result = hitScratch)
            hitScratch
        }
        val prevX = record?.lastComposeX ?: composeX
        val prevY = record?.lastComposeY ?: composeY
        val prevUptime = record?.lastUptime ?: uptime
        val effectiveType = record?.pointerType ?: pointerType
        val effectiveButton = record?.button ?: button

        dispatch3Pass(
            chain = chain,
            composeX = composeX,
            composeY = composeY,
            pointer = pointer,
            uptime = uptime,
            pressed = false,
            previousUptime = prevUptime,
            previousComposeX = prevX,
            previousComposeY = prevY,
            previousPressed = true,
            eventType = PointerEventType.Release,
            pointerType = effectiveType,
            button = effectiveButton
        )
    }

    private fun handleMouseMove(composeX: Float, composeY: Float, uptime: Long): Boolean {
        hitPoolIndex = 0
        hitScratch.clear()
        hitTestChain(rootLayoutNode, parentAbsX = 0f, parentAbsY = 0f, targetX = composeX, targetY = composeY, result = hitScratch)

        // 1. Tìm các node đã rời khỏi tầm chuột (Exited nodes)
        exitedHoverScratch.clear()
        val prevCount = previousHoverChain.size
        for (i in 0 until prevCount) {
            val prevHit = previousHoverChain[i]
            var stillHit = false
            val currentCount = hitScratch.size
            for (j in 0 until currentCount) {
                if (hitScratch[j].node === prevHit.node) {
                    stillHit = true
                    break
                }
            }
            if (!stillHit) {
                exitedHoverScratch.add(prevHit)
            }
        }

        // Dispatch sự kiện ra ngoài bounds cho các node đã rời đi
        if (exitedHoverScratch.isNotEmpty()) {
            dispatch3Pass(
                chain = exitedHoverScratch,
                composeX = composeX,
                composeY = composeY,
                pointer = 0,
                uptime = uptime,
                pressed = false,
                previousUptime = uptime,
                previousComposeX = composeX,
                previousComposeY = composeY,
                previousPressed = false,
                eventType = PointerEventType.Exit,
                pointerType = PointerType.Mouse,
                button = null
            )
            exitedHoverScratch.clear()
        }

        // 2. Dispatch sự kiện Move cho các node đang trúng chuột
        if (hitScratch.isNotEmpty()) {
            dispatch3Pass(
                chain = hitScratch,
                composeX = composeX,
                composeY = composeY,
                pointer = 0,
                uptime = uptime,
                pressed = false,
                previousUptime = uptime,
                previousComposeX = composeX,
                previousComposeY = composeY,
                previousPressed = false,
                eventType = PointerEventType.Move,
                pointerType = PointerType.Mouse,
                button = null
            )
        }

        // 3. Cập nhật previousHoverChain mà không cấp phát mới (Zero-GC)
        updatePreviousHoverChain(hitScratch)

        return hitScratch.isNotEmpty()
    }

    private fun updatePreviousHoverChain(currentHits: List<LayoutNodeHit>) {
        previousHoverChain.clear()
        val count = currentHits.size
        for (i in 0 until count) {
            val src = currentHits[i]
            val hit = if (i < hoverPool.size) {
                val existing = hoverPool[i]
                existing.node = src.node
                existing.absX = src.absX
                existing.absY = src.absY
                existing
            } else {
                val newHit = LayoutNodeHit(src.node, src.absX, src.absY)
                hoverPool.add(newHit)
                newHit
            }
            previousHoverChain.add(hit)
        }
    }

    private fun hitTestChain(
        node: LayoutNode,
        parentAbsX: Float,
        parentAbsY: Float,
        targetX: Float,
        targetY: Float,
        result: MutableList<LayoutNodeHit>
    ): Boolean {
        if (!node.visible) return false
        val absX = parentAbsX + node.x + node.offsetX
        val absY = parentAbsY + node.y + node.offsetY
        val w = node.width
        val h = node.height

        if (targetX < absX || targetX > absX + w || targetY < absY || targetY > absY + h) {
            return false
        }

        if (node.pointerInputFilters.isNotEmpty()) {
            result.add(obtainHit(node, absX, absY))
        }

        val children = node.children
        val count = children.size
        for (i in count - 1 downTo 0) {
            if (hitTestChain(children[i], absX, absY, targetX, targetY, result)) {
                break
            }
        }

        return true
    }

    private fun dispatch3Pass(
        chain: List<LayoutNodeHit>,
        composeX: Float,
        composeY: Float,
        pointer: Int,
        uptime: Long,
        pressed: Boolean,
        previousUptime: Long,
        previousComposeX: Float,
        previousComposeY: Float,
        previousPressed: Boolean,
        eventType: PointerEventType,
        pointerType: PointerType = PointerType.Touch,
        button: PointerButton? = null
    ) {
        val count = chain.size
        if (count == 0) return

        val pointerId = PointerId(pointer.toLong())
        val consumed = ConsumedData()

        if (count == 1) {
            val hit = chain[0]
            val filters = hit.node.pointerInputFilters
            val fCount = filters.size
            if (fCount == 0) return
            val pos = Offset(composeX - hit.absX, composeY - hit.absY)
            val prevPos = Offset(previousComposeX - hit.absX, previousComposeY - hit.absY)
            val change = PointerInputChange(
                id = pointerId,
                uptimeMillis = uptime,
                position = pos,
                pressed = pressed,
                previousUptimeMillis = previousUptime,
                previousPosition = prevPos,
                previousPressed = previousPressed,
                consumed = consumed,
                type = pointerType,
                button = button
            )
            val event = PointerEvent(listOf(change), eventType)
            val bounds = IntSize(
                kotlin.math.ceil(hit.node.width).toInt(),
                kotlin.math.ceil(hit.node.height).toInt()
            )

            // Initial Pass: outer to inner (0 until fCount)
            for (f in 0 until fCount) {
                filters[f].dispatchPointerEvent(event, PointerEventPass.Initial, bounds)
            }
            // Main Pass: inner to outer (fCount - 1 downTo 0)
            for (f in fCount - 1 downTo 0) {
                filters[f].dispatchPointerEvent(event, PointerEventPass.Main, bounds)
            }
            // Final Pass: inner to outer (fCount - 1 downTo 0)
            for (f in fCount - 1 downTo 0) {
                filters[f].dispatchPointerEvent(event, PointerEventPass.Final, bounds)
            }
            return
        }

        eventsScratch.clear()
        boundsScratch.clear()

        for (i in 0 until count) {
            val hit = chain[i]
            val pos = Offset(composeX - hit.absX, composeY - hit.absY)
            val prevPos = Offset(previousComposeX - hit.absX, previousComposeY - hit.absY)
            val change = PointerInputChange(
                id = pointerId,
                uptimeMillis = uptime,
                position = pos,
                pressed = pressed,
                previousUptimeMillis = previousUptime,
                previousPosition = prevPos,
                previousPressed = previousPressed,
                consumed = consumed,
                type = pointerType,
                button = button
            )
            eventsScratch.add(PointerEvent(listOf(change), eventType))
            boundsScratch.add(
                IntSize(
                    kotlin.math.ceil(hit.node.width).toInt(),
                    kotlin.math.ceil(hit.node.height).toInt()
                )
            )
        }

        // 1. Initial Pass: Root -> Leaf (Tunneling)
        for (i in 0 until count) {
            val filters = chain[i].node.pointerInputFilters
            val fCount = filters.size
            for (f in 0 until fCount) {
                filters[f].dispatchPointerEvent(eventsScratch[i], PointerEventPass.Initial, boundsScratch[i])
            }
        }

        // 2. Main Pass: Leaf -> Root (Bubbling)
        for (i in count - 1 downTo 0) {
            val filters = chain[i].node.pointerInputFilters
            val fCount = filters.size
            for (f in fCount - 1 downTo 0) {
                filters[f].dispatchPointerEvent(eventsScratch[i], PointerEventPass.Main, boundsScratch[i])
            }
        }

        // 3. Final Pass: Leaf -> Root (Post-processing)
        for (i in count - 1 downTo 0) {
            val filters = chain[i].node.pointerInputFilters
            val fCount = filters.size
            for (f in fCount - 1 downTo 0) {
                filters[f].dispatchPointerEvent(eventsScratch[i], PointerEventPass.Final, boundsScratch[i])
            }
        }

        eventsScratch.clear()
        boundsScratch.clear()
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
        return if (x in 0f..effectiveW && y >= 0f && y <= effectiveH) this else null
    }

    override fun act(delta: Float) {
        super.act(delta)

        // Đập nhịp đồng bộ
        CompositionManager.frame()

        updateDimensionsAndLayout()
    }

    private var lastLayoutW = -1f
    private var lastLayoutH = -1f

    private fun updateDimensionsAndLayout() {
        val currentW = width
        val currentH = height
        if (currentW > 0f && currentH > 0f) {
            if (isLayoutDirty || currentW != lastLayoutW || currentH != lastLayoutH) {
                // Host Window phân phối ràng buộc kích thước chuẩn xuống Node gốc trong 1 pass duy nhất
                rootLayoutNode.layout(currentW, currentH, exact = true)
                lastLayoutW = currentW
                lastLayoutH = currentH
                isLayoutDirty = false
            }
        }
    }

    override fun draw() {
        if (!visible || width <= 0f || height <= 0f) return
        validate()

        if (isLayoutDirty && width > 0f && height > 0f) {
            rootLayoutNode.layout(width, height, exact = true)
            lastLayoutW = width
            lastLayoutH = height
            isLayoutDirty = false
        }

        val viewH = if (height > 0f) height else rootLayoutNode.height
        try {
            renderNodeRecursive(rootLayoutNode, parentLocalX = 0f, parentLocalY = 0f, parentEffectiveAlpha = 1f, viewH = viewH)

            // Trực quan hóa Debug: Vẽ điểm chạm và tâm click của cú click gần nhất (Lime = Hit trúng, Scarlet = Hit trượt)
            if (debugDrawHitBounds && lastClickComposeX >= 0f && (System.currentTimeMillis() - lastClickTime) < 2000L) {
                try {
                    val clickArcX = this.x + lastClickComposeX
                    val clickArcY = this.y + viewH - lastClickComposeY
                    val markerColor = if (lastClickHitSuccess) Color.lime else Color.scarlet

                    Lines.stroke(2f)
                    Draw.color(markerColor)
                    Lines.line(clickArcX - 8f, clickArcY, clickArcX + 8f, clickArcY)
                    Lines.line(clickArcX, clickArcY - 8f, clickArcX, clickArcY + 8f)
                    Lines.circle(clickArcX, clickArcY, 5f)
                } catch (_: Throwable) {
                }
            }
        } finally {
            try {
                Draw.reset()
            } catch (_: Throwable) {
            }
        }
    }

    private fun renderNodeRecursive(
        node: LayoutNode,
        parentLocalX: Float,
        parentLocalY: Float,
        parentEffectiveAlpha: Float,
        viewH: Float
    ) {
        if (!node.visible) return

        val effectiveAlpha = (parentEffectiveAlpha * node.alpha).coerceIn(0f, 1f)
        if (effectiveAlpha <= 0f) return

        // Tích lũy tọa độ tuyệt đối theo cây cha con trong Local Space (Top-Left Y-down)
        val nodeLocalX = parentLocalX + node.x + node.offsetX
        val nodeLocalY = parentLocalY + node.y + node.offsetY

        val nodeW = node.width
        val nodeH = node.height

        // BỨC TƯỜNG BERLIN TRỤC Y: Ánh xạ sang tọa độ Bottom-Left của Arc Scene2D
        val arcX = this.x + nodeLocalX
        val arcY = this.y + viewH - (nodeLocalY + nodeH)

        // Vẽ nền nếu có
        val bg = node.backgroundColor
        if (bg != null) {
            if (effectiveAlpha < 1f) {
                Draw.color(bg.r, bg.g, bg.b, bg.a * effectiveAlpha)
            } else {
                Draw.color(bg)
            }
            Fill.crect(arcX, arcY, nodeW, nodeH)
        }

        // Trực quan hóa Debug: Vẽ khung viền Hit-Test Bounds cho node có tương tác (Button, clickable, pointerInput)
        if (debugDrawHitBounds && node.pointerInputFilters.isNotEmpty()) {
            try {
                Lines.stroke(1.5f)
                Draw.color(Color.gold)
                Lines.rect(arcX, arcY, nodeW, nodeH)
            } catch (_: Throwable) {
            }
        }

        // Vẽ chữ nếu có
        val textContent = node.text
        if (textContent != null) {
            if (effectiveAlpha < 1f) {
                Draw.color(node.textColor.r, node.textColor.g, node.textColor.b, node.textColor.a * effectiveAlpha)
            } else {
                Draw.color(node.textColor)
            }
            try {
                val font = Fonts.def
                if (font != null) {
                    val fontCap = font.data.capHeight
                    val drawY = if (nodeH > fontCap) arcY + (nodeH + fontCap) * 0.5f else arcY + nodeH
                    font.draw(textContent, arcX, drawY)
                }
            } catch (_: Throwable) {
                // Tránh crash trong headless test environment khi Font chưa load
            }
        }

        // Duyệt con bằng vòng lặp chỉ mục thuần túy (Zero-GC), truyền nodeLocalX, nodeLocalY & effectiveAlpha
        val children = node.children
        val count = children.size
        for (i in 0 until count) {
            renderNodeRecursive(children[i], nodeLocalX, nodeLocalY, effectiveAlpha, viewH)
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

/**
 * Ghi lại thông tin node trúng hit-test và tọa độ tuyệt đối của node trong ComposeView.
 */
data class LayoutNodeHit(
    var node: LayoutNode,
    var absX: Float,
    var absY: Float
)

internal class PointerHitRecord(
    val chain: List<LayoutNodeHit>,
    var lastComposeX: Float,
    var lastComposeY: Float,
    var lastUptime: Long,
    val button: PointerButton? = PointerButton.Primary,
    val pointerType: PointerType = PointerType.Touch
)


