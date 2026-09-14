package org.hubdustry.core.compose.view

import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastForEachIndexed
import arc.input.KeyCode
import org.hubdustry.core.compose.input.ConsumedData
import org.hubdustry.core.compose.input.IntSize
import org.hubdustry.core.compose.input.Offset
import org.hubdustry.core.compose.input.PointerButton
import org.hubdustry.core.compose.input.PointerEvent
import org.hubdustry.core.compose.input.PointerEventPass
import org.hubdustry.core.compose.input.PointerEventType
import org.hubdustry.core.compose.input.PointerId
import org.hubdustry.core.compose.input.PointerInputChange
import org.hubdustry.core.compose.input.PointerType
import org.hubdustry.core.layout.LayoutNode

/**
 * Interface đón nhận các sự kiện bàn phím từ [InputDispatcher].
 */
interface KeyboardInputHandler {
    fun onKeyTyped(character: Char): Boolean
    fun onKeyDown(keycode: KeyCode?): Boolean
    fun onKeyUp(keycode: KeyCode?): Boolean = false
    fun onFocusLost() {}
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

/**
 * Bộ điều phối sự kiện con trỏ và cử chỉ độc lập cho [ComposeView].
 *
 * TÁCH BIỆT TRÁCH NHIỆM (Đợt 2 Refactor):
 * 1. Chịu trách nhiệm Hit-Testing duyệt cây tìm chuỗi node nhận tương tác.
 * 2. Phân phối sự kiện 3-pass (Initial -> Main -> Final) theo chuẩn Jetpack Compose AOSP.
 * 3. Theo dõi vòng đời con trỏ (Touch/Mouse/Hover Tracking) và giải phóng sạch sẽ.
 * 4. Tái sử dụng scratchpads và object pools để triệt tiêu cấp phát heap trong hot-path (Zero-GC).
 *
 * Sơ đồ phân phối sự kiện 3-pass (chuỗi Root → Leaf, 3 node ví dụ):
 *
 *  PointerEvent
 *      │
 *      ▼ Initial Pass (Tunneling: Root → Leaf)
 *  [Root] ──► [NodeA] ──► [NodeB leaf]
 *
 *      ▼ Main Pass (Bubbling: Leaf → Root)
 *  [Root] ◄── [NodeA] ◄── [NodeB leaf]
 *
 *      ▼ Final Pass (Bubbling: Leaf → Root)
 *  [Root] ◄── [NodeA] ◄── [NodeB leaf]
 *
 *  ConsumedData.isConsumed được chia sẻ qua toàn bộ 3 pass.
 */
class InputDispatcher(private val rootLayoutNode: LayoutNode) {

    // Bộ theo dõi con trỏ đang hoạt động (Pointer Tracking)
    private val trackedPointers = HashMap<Int, PointerHitRecord>()
    private val hitScratch = ArrayList<LayoutNodeHit>()
    private val hitPool = ArrayList<LayoutNodeHit>()
    private var hitPoolIndex = 0

    // Scratchpad tái sử dụng trong dispatch3Pass để triệt tiêu cấp phát collection trong hot-path
    private val eventsScratch = ArrayList<PointerEvent>()
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

    /**
     * Entry point nhận sự kiện con trỏ tổng quát (sử dụng trong kiểm thử và tích hợp mở rộng).
     * Tọa độ [x], [y] là tọa độ Top-Left (Y-down) nội bộ của NekoMod Compose.
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
    ): Boolean {
        return when (type) {
            PointerEventType.Press -> touchDown(x, y, pointer, uptimeMillis, button, pointerType)
            PointerEventType.Move -> {
                if (trackedPointers.containsKey(pointer)) {
                    touchMove(x, y, pointer, uptimeMillis)
                    true
                } else {
                    mouseMove(x, y, uptimeMillis)
                }
            }
            PointerEventType.Release -> {
                touchUp(x, y, pointer, uptimeMillis, button, pointerType)
                true
            }
            PointerEventType.Scroll -> scroll(x, y, scrollDelta, uptimeMillis)
            PointerEventType.Exit -> {
                mouseExit(uptimeMillis)
                cancelAllActivePointers(uptimeMillis)
                true
            }
            PointerEventType.Enter, PointerEventType.Unknown -> false
        }
    }

    fun touchDown(
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

        if (hitScratch.isEmpty()) return false

        val chain = ArrayList<LayoutNodeHit>(hitScratch.size)
        hitScratch.fastForEach { h ->
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

    fun touchMove(composeX: Float, composeY: Float, pointer: Int, uptime: Long) {
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

    fun touchUp(
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

    /**
     * Điều phối sự kiện con lăn chuột (Mouse Wheel / Scroll) qua chuỗi 3-pass theo chuẩn Compose.
     */
    fun scroll(
        composeX: Float,
        composeY: Float,
        scrollDelta: Offset,
        uptime: Long = System.currentTimeMillis()
    ): Boolean {
        hitPoolIndex = 0
        hitScratch.clear()
        hitTestChain(rootLayoutNode, parentAbsX = 0f, parentAbsY = 0f, targetX = composeX, targetY = composeY, result = hitScratch)

        if (hitScratch.isEmpty()) return false

        return dispatch3Pass(
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
            eventType = PointerEventType.Scroll,
            pointerType = PointerType.Mouse,
            button = null,
            scrollDelta = scrollDelta
        )
    }

    fun mouseMove(composeX: Float, composeY: Float, uptime: Long): Boolean {
        hitPoolIndex = 0
        hitScratch.clear()
        hitTestChain(rootLayoutNode, parentAbsX = 0f, parentAbsY = 0f, targetX = composeX, targetY = composeY, result = hitScratch)

        // 1. Tìm các node đã rời khỏi tầm chuột (Exited nodes)
        exitedHoverScratch.clear()
        previousHoverChain.fastForEach { prevHit ->
            if (!hitScratch.fastAny { it.node === prevHit.node }) {
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

    /**
     * Thông báo con trỏ chuột đã rời khỏi phạm vi View.
     * Chỉ giải phóng chuỗi hover, KHÔNG làm gián đoạn thao tác drag của các con trỏ đang nhấn (Fix N17).
     */
    fun mouseExit(uptime: Long = System.currentTimeMillis()) {
        if (previousHoverChain.isEmpty()) return

        dispatch3Pass(
            chain = previousHoverChain,
            composeX = -1000f,
            composeY = -1000f,
            pointer = 0,
            uptime = uptime,
            pressed = false,
            previousUptime = uptime,
            previousComposeX = -1000f,
            previousComposeY = -1000f,
            previousPressed = false,
            eventType = PointerEventType.Exit,
            pointerType = PointerType.Mouse,
            button = null
        )
        previousHoverChain.clear()
    }

    private fun updatePreviousHoverChain(currentHits: List<LayoutNodeHit>) {
        previousHoverChain.clear()
        currentHits.fastForEachIndexed { i, src ->
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
        clipMinX: Float = -100000f,
        clipMinY: Float = -100000f,
        clipMaxX: Float = 100000f,
        clipMaxY: Float = 100000f,
        targetX: Float,
        targetY: Float,
        result: MutableList<LayoutNodeHit>
    ): Boolean {
        if (!node.visible) return false

        // 1. Kiểm tra điểm tương tác có nằm trong vùng cắt gọt thừa kế từ tổ tiên không
        if (targetX < clipMinX || targetX > clipMaxX || targetY < clipMinY || targetY > clipMaxY) {
            return false
        }

        val absX = parentAbsX + node.x + node.offsetX
        val absY = parentAbsY + node.y + node.offsetY
        val w = node.width
        val h = node.height

        // 2. Kiểm tra hình chữ nhật AABB của chính node
        val isInAabb = targetX >= absX && targetX <= (absX + w) && targetY >= absY && targetY <= (absY + h)

        // 3. Nếu node có clip nhưng điểm chạm rơi ra ngoài AABB của trục được clip -> reject
        if ((node.clipHorizontal && (targetX < absX || targetX > absX + w)) ||
            (node.clipVertical && (targetY < absY || targetY > absY + h))) {
            return false
        }

        // 4. Nếu điểm chạm nằm trong AABB nhưng node có bo góc, kiểm tra xem có bị xén ở 4 góc cong không
        val isInShape = isInAabb && isInsideRoundedCorners(
            localX = targetX - absX,
            localY = targetY - absY,
            width = w,
            height = h,
            rTopStart = if (node.hasClipCorners) node.clipRadiusTopStart else node.cornerRadiusTopStart,
            rTopEnd = if (node.hasClipCorners) node.clipRadiusTopEnd else node.cornerRadiusTopEnd,
            rBottomEnd = if (node.hasClipCorners) node.clipRadiusBottomEnd else node.cornerRadiusBottomEnd,
            rBottomStart = if (node.hasClipCorners) node.clipRadiusBottomStart else node.cornerRadiusBottomStart
        )

        // Nếu node có clip bo góc và điểm chạm rơi vào góc bị xén -> reject
        if (node.hasClipCorners && !isInShape) {
            return false
        }

        val initialSize = result.size

        // Node nhận hit nếu điểm nằm trong hình dạng thực tế và có bộ lọc cử chỉ
        if (isInShape && node.pointerInputFilters.isNotEmpty()) {
            result.add(obtainHit(node, absX, absY))
        }

        // 5. Tính toán vùng clip lũy tiến cho các node con
        val nextClipMinX = if (node.clipHorizontal) maxOf(clipMinX, absX) else clipMinX
        val nextClipMaxX = if (node.clipHorizontal) minOf(clipMaxX, absX + w) else clipMaxX
        val nextClipMinY = if (node.clipVertical) maxOf(clipMinY, absY) else clipMinY
        val nextClipMaxY = if (node.clipVertical) minOf(clipMaxY, absY + h) else clipMaxY

        val childParentAbsX = absX - node.scrollX
        val childParentAbsY = absY - node.scrollY

        val children = node.children
        val count = children.size
        for (i in count - 1 downTo 0) {
            if (hitTestChain(
                    node = children[i],
                    parentAbsX = childParentAbsX,
                    parentAbsY = childParentAbsY,
                    clipMinX = nextClipMinX,
                    clipMinY = nextClipMinY,
                    clipMaxX = nextClipMaxX,
                    clipMaxY = nextClipMaxY,
                    targetX = targetX,
                    targetY = targetY,
                    result = result
                )
            ) {
                break
            }
        }

        return result.size > initialSize
    }

    /**
     * Kiểm tra điểm [localX], [localY] có nằm trong hình chữ nhật bo góc không (Top-Left Y-down).
     * Áp dụng khoảng cách bình phương (Rule 3.3 Zero-GC, không gọi sqrt).
     */
    private fun isInsideRoundedCorners(
        localX: Float,
        localY: Float,
        width: Float,
        height: Float,
        rTopStart: Float,
        rTopEnd: Float,
        rBottomEnd: Float,
        rBottomStart: Float
    ): Boolean {
        if (rTopStart <= 0.001f && rTopEnd <= 0.001f && rBottomEnd <= 0.001f && rBottomStart <= 0.001f) {
            return true
        }

        val halfW = width * 0.5f
        val halfH = height * 0.5f

        // Góc Top-Left
        val rTs = minOf(rTopStart, halfW, halfH)
        if (rTs > 0.001f && localX < rTs && localY < rTs) {
            val dx = localX - rTs
            val dy = localY - rTs
            if (dx * dx + dy * dy > rTs * rTs) return false
        }

        // Góc Top-Right
        val rTe = minOf(rTopEnd, halfW, halfH)
        if (rTe > 0.001f && localX > width - rTe && localY < rTe) {
            val dx = localX - (width - rTe)
            val dy = localY - rTe
            if (dx * dx + dy * dy > rTe * rTe) return false
        }

        // Góc Bottom-Right
        val rBe = minOf(rBottomEnd, halfW, halfH)
        if (rBe > 0.001f && localX > width - rBe && localY > height - rBe) {
            val dx = localX - (width - rBe)
            val dy = localY - (height - rBe)
            if (dx * dx + dy * dy > rBe * rBe) return false
        }

        // Góc Bottom-Left
        val rBs = minOf(rBottomStart, halfW, halfH)
        if (rBs > 0.001f && localX < rBs && localY > height - rBs) {
            val dx = localX - rBs
            val dy = localY - (height - rBs)
            if (dx * dx + dy * dy > rBs * rBs) return false
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
        button: PointerButton? = null,
        scrollDelta: Offset = Offset.Zero,
        isConsumed: Boolean = false
    ): Boolean {
        val count = chain.size
        if (count == 0) return false

        val pointerId = PointerId(pointer.toLong())
        val consumed = ConsumedData(isConsumed)

        if (count == 1) {
            val hit = chain[0]
            val filters = hit.node.pointerInputFilters
            val fCount = filters.size
            if (fCount == 0) return false
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
                button = button,
                scrollDelta = scrollDelta
            )
            val event = PointerEvent(java.util.Collections.singletonList(change), eventType)
            val bounds = IntSize(
                kotlin.math.ceil(hit.node.width).toInt(),
                kotlin.math.ceil(hit.node.height).toInt()
            )

            // Initial Pass: outer to inner
            filters.fastForEach { filter ->
                filter.dispatchPointerEvent(event, PointerEventPass.Initial, bounds)
            }
            // Main Pass: inner to outer (fCount - 1 downTo 0)
            for (f in fCount - 1 downTo 0) {
                filters[f].dispatchPointerEvent(event, PointerEventPass.Main, bounds)
            }
            // Final Pass: inner to outer (fCount - 1 downTo 0)
            for (f in fCount - 1 downTo 0) {
                filters[f].dispatchPointerEvent(event, PointerEventPass.Final, bounds)
            }
            return consumed.isConsumed
        }

        eventsScratch.clear()

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
                button = button,
                scrollDelta = scrollDelta
            )
            eventsScratch.add(PointerEvent(java.util.Collections.singletonList(change), eventType))
        }

        // 1. Initial Pass: Root -> Leaf (Tunneling)
        chain.fastForEachIndexed { i, hit ->
            val bounds = IntSize(
                kotlin.math.ceil(hit.node.width).toInt(),
                kotlin.math.ceil(hit.node.height).toInt()
            )
            hit.node.pointerInputFilters.fastForEach { filter ->
                filter.dispatchPointerEvent(eventsScratch[i], PointerEventPass.Initial, bounds)
            }
        }

        // 2. Main Pass: Leaf -> Root (Bubbling)
        for (i in count - 1 downTo 0) {
            val hit = chain[i]
            val bounds = IntSize(
                kotlin.math.ceil(hit.node.width).toInt(),
                kotlin.math.ceil(hit.node.height).toInt()
            )
            val filters = hit.node.pointerInputFilters
            val fCount = filters.size
            for (f in fCount - 1 downTo 0) {
                filters[f].dispatchPointerEvent(eventsScratch[i], PointerEventPass.Main, bounds)
            }
        }

        // 3. Final Pass: Leaf -> Root (Post-processing)
        for (i in count - 1 downTo 0) {
            val hit = chain[i]
            val bounds = IntSize(
                kotlin.math.ceil(hit.node.width).toInt(),
                kotlin.math.ceil(hit.node.height).toInt()
            )
            val filters = hit.node.pointerInputFilters
            val fCount = filters.size
            for (f in fCount - 1 downTo 0) {
                filters[f].dispatchPointerEvent(eventsScratch[i], PointerEventPass.Final, bounds)
            }
        }

        eventsScratch.clear()
        return consumed.isConsumed
    }

    fun cancelPointer(pointer: Int, uptime: Long = System.currentTimeMillis()) {
        val record = trackedPointers.remove(pointer) ?: return
        dispatch3Pass(
            chain = record.chain,
            composeX = record.lastComposeX,
            composeY = record.lastComposeY,
            pointer = pointer,
            uptime = uptime,
            pressed = false,
            previousUptime = record.lastUptime,
            previousComposeX = record.lastComposeX,
            previousComposeY = record.lastComposeY,
            previousPressed = true,
            eventType = PointerEventType.Release,
            pointerType = record.pointerType,
            button = record.button,
            isConsumed = true
        )
    }

    /**
     * Giải phóng dứt điểm tất cả con trỏ đang nhấn khi ComposeView bị hủy hoặc reset.
     */
    fun cancelAllActivePointers(uptime: Long = System.currentTimeMillis()) {
        if (trackedPointers.isEmpty()) return
        val pointers = ArrayList(trackedPointers.keys)
        pointers.fastForEach { cancelPointer(it, uptime) }
    }

    /**
     * Dọn dẹp triệt để các tham chiếu tới node khi node bị gỡ khỏi cây Virtual DOM (Fix O2 Ghost Node).
     */
    fun onNodeRemoved(node: LayoutNode) {
        previousHoverChain.removeAll { it.node === node }
        if (trackedPointers.isEmpty()) return

        val pointersToCancel = ArrayList<Int>()
        for (entry in trackedPointers.entries) {
            if (entry.value.chain.fastAny { it.node === node }) {
                pointersToCancel.add(entry.key)
            }
        }
        pointersToCancel.fastForEach { cancelPointer(it) }
    }

    // ── KEYBOARD & FOCUS ROUTING ─────────────────────────────────────────────

    var focusedKeyHandler: KeyboardInputHandler? = null
        set(value) {
            if (field !== value) {
                val previous = field
                field = value
                previous?.onFocusLost()
            }
        }

    fun keyTyped(character: Char): Boolean =
        focusedKeyHandler?.onKeyTyped(character) ?: false

    fun keyDown(keycode: KeyCode?): Boolean =
        focusedKeyHandler?.onKeyDown(keycode) ?: false

    fun keyUp(keycode: KeyCode?): Boolean =
        focusedKeyHandler?.onKeyUp(keycode) ?: false

    fun dispose() {
        cancelAllActivePointers()
        trackedPointers.clear()

        hitScratch.clear()
        hitPool.clear()
        hitPoolIndex = 0

        eventsScratch.clear()
        previousHoverChain.clear()
        exitedHoverScratch.clear()
        hoverPool.clear()

        focusedKeyHandler = null
    }
}
