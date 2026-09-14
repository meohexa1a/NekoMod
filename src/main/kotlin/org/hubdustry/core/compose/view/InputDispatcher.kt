package org.hubdustry.core.compose.view

import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastForEachIndexed
import arc.input.KeyCode
import org.hubdustry.core.compose.input.ConsumedData
import org.hubdustry.core.compose.input.IntSize
import org.hubdustry.core.compose.input.KeyboardInputHandler
import org.hubdustry.core.compose.input.Offset
import org.hubdustry.core.compose.input.PointerButton
import org.hubdustry.core.compose.input.PointerEvent
import org.hubdustry.core.compose.input.PointerEventPass
import org.hubdustry.core.compose.input.PointerEventType
import org.hubdustry.core.compose.input.PointerId
import org.hubdustry.core.compose.input.PointerInputChange
import org.hubdustry.core.compose.input.PointerType
import org.hubdustry.core.layout.LayoutNode

// ─── Data Contracts & Offscreen Constants ─────────────────────────

private const val OFFSCREEN_COORDINATE = -1000f

/**
 * Bản ghi con trỏ đang được theo dõi trong suốt vòng đời nhấn giữ (Touch Tracking).
 */
internal class PointerHitRecord(
    val chain: List<LayoutNodeHit>,
    var lastComposeX: Float,
    var lastComposeY: Float,
    var lastUptime: Long,
    val button: PointerButton? = PointerButton.Primary,
    val pointerType: PointerType = PointerType.Touch
)

// ─── Input Dispatcher Core ────────────────────────────────────────

/**
 * [InputDispatcher] — Bộ điều phối sự kiện cử chỉ con trỏ và bàn phím tập trung của NekoMod.
 *
 * TRÁCH NHIỆM (Phân Tách Module Đợt 3):
 * 1. Tiếp nhận sự kiện tương tác chuẩn hóa từ [ArcInputAdapter] hoặc ComposeView.
 * 2. Tập trung vào thuật toán phân phối 3-Pass (Tunneling -> Bubbling) theo chuẩn Jetpack Compose AOSP.
 * 3. Theo dõi vòng đời con trỏ nhấn giữ ([trackedPointers]) và phím bấm ([focusedKeyHandler]).
 * 4. Kỷ luật Zero-GC trong hot-paths (Move, Touch, Scroll).
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
class InputDispatcher(rootLayoutNode: LayoutNode) {

    internal val hitTestManager = HitTestManager(rootLayoutNode)
    private val trackedPointers = HashMap<Int, PointerHitRecord>()
    private val eventsScratch = ArrayList<PointerEvent>()
    private val scratchSingletonLists = ArrayList<ArrayList<PointerInputChange>>()

    private fun obtainSingletonList(change: PointerInputChange, index: Int = 0): List<PointerInputChange> {
        while (scratchSingletonLists.size <= index) {
            scratchSingletonLists.add(ArrayList(1))
        }
        val list = scratchSingletonLists[index]
        list.clear()
        list.add(change)
        return list
    }

    // ─── Pointer Event Entry Points ───────────────────────────────────

    fun sendPointerInput(
        type: PointerEventType,
        x: Float,
        y: Float,
        pointer: Int = 0,
        uptimeMillis: Long = System.currentTimeMillis(),
        button: PointerButton? = PointerButton.Primary,
        pointerType: PointerType = PointerType.Touch,
        scrollDelta: Offset = Offset.Zero
    ): Boolean = when (type) {
        PointerEventType.Press -> touchDown(x, y, pointer, uptimeMillis, button, pointerType)
        PointerEventType.Move -> {
            if (trackedPointers.containsKey(pointer)) {
                touchMove(x, y, pointer, uptimeMillis)
                true
            } else {
                mouseMove(x, y, uptimeMillis)
            }
        }
        PointerEventType.Release -> touchUp(x, y, pointer, uptimeMillis, button, pointerType)
        PointerEventType.Scroll -> scroll(x, y, scrollDelta, uptimeMillis)
        PointerEventType.Exit -> {
            mouseExit(uptimeMillis)
            cancelAllActivePointers(uptimeMillis)
            true
        }
        else -> false
    }

    fun touchDown(
        composeX: Float,
        composeY: Float,
        pointer: Int,
        uptime: Long,
        button: PointerButton? = PointerButton.Primary,
        pointerType: PointerType = PointerType.Touch
    ): Boolean {
        val hits = hitTestManager.findHitChain(composeX, composeY)
        if (hits.isEmpty()) return false

        val chain = ArrayList<LayoutNodeHit>(hits.size)
        hits.fastForEach { hitNode ->
            chain.add(LayoutNodeHit(node = hitNode.node, absX = hitNode.absX, absY = hitNode.absY))
        }
        trackedPointers[pointer] = PointerHitRecord(
            chain = chain,
            lastComposeX = composeX,
            lastComposeY = composeY,
            lastUptime = uptime,
            button = button,
            pointerType = pointerType
        )

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
    ): Boolean {
        val record = trackedPointers.remove(pointer)
        val chain = record?.chain ?: hitTestManager.findHitChain(composeX, composeY)
        if (chain.isEmpty()) return false
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
        return true
    }

    fun scroll(
        composeX: Float,
        composeY: Float,
        scrollDelta: Offset,
        uptime: Long = System.currentTimeMillis()
    ): Boolean {
        val hits = hitTestManager.findHitChain(composeX, composeY)
        if (hits.isEmpty()) return false

        return dispatch3Pass(
            chain = hits,
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

    fun mouseMove(composeX: Float, composeY: Float, uptime: Long = System.currentTimeMillis()): Boolean {
        val hits = hitTestManager.findHitChain(composeX, composeY)

        // 1. Phân phối sự kiện Exit cho các node mà chuột vừa rời khỏi
        val exited = hitTestManager.findExitedHoverNodes(hits)
        if (exited.isNotEmpty()) {
            dispatch3Pass(
                chain = exited,
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
        }

        // 2. Phân phối sự kiện Move cho chuỗi hit hiện tại
        if (hits.isNotEmpty()) {
            dispatch3Pass(
                chain = hits,
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

        // 3. Cập nhật chuỗi hover
        hitTestManager.updatePreviousHoverChain(hits)
        return hits.isNotEmpty()
    }

    fun mouseExit(uptime: Long = System.currentTimeMillis()) {
        val previous = hitTestManager.takePreviousHoverChain()
        if (previous.isEmpty()) return

        dispatch3Pass(
            chain = previous,
            composeX = OFFSCREEN_COORDINATE,
            composeY = OFFSCREEN_COORDINATE,
            pointer = 0,
            uptime = uptime,
            pressed = false,
            previousUptime = uptime,
            previousComposeX = OFFSCREEN_COORDINATE,
            previousComposeY = OFFSCREEN_COORDINATE,
            previousPressed = false,
            eventType = PointerEventType.Exit,
            pointerType = PointerType.Mouse,
            button = null
        )
    }

    // ─── 3-Pass Dispatch Algorithm ────────────────────────────────────

    private fun dispatchSingleHitFastPath(
        hit: LayoutNodeHit,
        pointerId: PointerId,
        composeX: Float,
        composeY: Float,
        previousComposeX: Float,
        previousComposeY: Float,
        uptime: Long,
        previousUptime: Long,
        pressed: Boolean,
        previousPressed: Boolean,
        consumed: ConsumedData,
        eventType: PointerEventType,
        pointerType: PointerType,
        button: PointerButton?,
        scrollDelta: Offset
    ): Boolean {
        val filters = hit.node.pointerInputFilters
        val filterCount = filters.size
        if (filterCount == 0) return false

        val localPosition = Offset(composeX - hit.absX, composeY - hit.absY)
        val previousLocalPosition = Offset(previousComposeX - hit.absX, previousComposeY - hit.absY)
        val change = PointerInputChange(
            id = pointerId,
            uptimeMillis = uptime,
            position = localPosition,
            pressed = pressed,
            previousUptimeMillis = previousUptime,
            previousPosition = previousLocalPosition,
            previousPressed = previousPressed,
            consumed = consumed,
            type = pointerType,
            button = button,
            scrollDelta = scrollDelta
        )
        val event = PointerEvent(obtainSingletonList(change, 0), eventType)
        val bounds = IntSize(
            kotlin.math.ceil(hit.node.width).toInt(),
            kotlin.math.ceil(hit.node.height).toInt()
        )

        // Initial Pass: outer to inner
        filters.fastForEach { it.dispatchPointerEvent(event, PointerEventPass.Initial, bounds) }
        // Main Pass: inner to outer
        for (filterIndex in filterCount - 1 downTo 0) {
            filters[filterIndex].dispatchPointerEvent(event, PointerEventPass.Main, bounds)
        }
        // Final Pass: inner to outer
        for (filterIndex in filterCount - 1 downTo 0) {
            filters[filterIndex].dispatchPointerEvent(event, PointerEventPass.Final, bounds)
        }
        return consumed.isConsumed
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

        // Tối ưu hóa cho node đơn lẻ (Single Hit Fast-Path)
        if (count == 1) {
            return dispatchSingleHitFastPath(
                hit = chain[0],
                pointerId = pointerId,
                composeX = composeX,
                composeY = composeY,
                previousComposeX = previousComposeX,
                previousComposeY = previousComposeY,
                uptime = uptime,
                previousUptime = previousUptime,
                pressed = pressed,
                previousPressed = previousPressed,
                consumed = consumed,
                eventType = eventType,
                pointerType = pointerType,
                button = button,
                scrollDelta = scrollDelta
            )
        }

        eventsScratch.clear()
        for (i in 0 until count) {
            val hit = chain[i]
            val localPosition = Offset(composeX - hit.absX, composeY - hit.absY)
            val previousLocalPosition = Offset(previousComposeX - hit.absX, previousComposeY - hit.absY)
            val change = PointerInputChange(
                id = pointerId,
                uptimeMillis = uptime,
                position = localPosition,
                pressed = pressed,
                previousUptimeMillis = previousUptime,
                previousPosition = previousLocalPosition,
                previousPressed = previousPressed,
                consumed = consumed,
                type = pointerType,
                button = button,
                scrollDelta = scrollDelta
            )
            eventsScratch.add(PointerEvent(obtainSingletonList(change, i), eventType))
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
            val filters = hit.node.pointerInputFilters
            val filterCount = filters.size
            if (filterCount == 0) continue

            val bounds = IntSize(
                kotlin.math.ceil(hit.node.width).toInt(),
                kotlin.math.ceil(hit.node.height).toInt()
            )
            for (filterIndex in filterCount - 1 downTo 0) {
                filters[filterIndex].dispatchPointerEvent(eventsScratch[i], PointerEventPass.Main, bounds)
            }
        }

        // 3. Final Pass: Leaf -> Root (Post-processing)
        for (i in count - 1 downTo 0) {
            val hit = chain[i]
            val filters = hit.node.pointerInputFilters
            val filterCount = filters.size
            if (filterCount == 0) continue

            val bounds = IntSize(
                kotlin.math.ceil(hit.node.width).toInt(),
                kotlin.math.ceil(hit.node.height).toInt()
            )
            for (filterIndex in filterCount - 1 downTo 0) {
                filters[filterIndex].dispatchPointerEvent(eventsScratch[i], PointerEventPass.Final, bounds)
            }
        }

        eventsScratch.clear()
        return consumed.isConsumed
    }

    // ─── Pointer Cancellation & Cleanup ───────────────────────────────

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

    fun cancelAllActivePointers(uptime: Long = System.currentTimeMillis()) {
        if (trackedPointers.isEmpty()) return
        val pointers = ArrayList(trackedPointers.keys)
        pointers.fastForEach { cancelPointer(it, uptime) }
    }

    fun onNodeRemoved(node: LayoutNode) {
        hitTestManager.onNodeRemoved(node)
        if (trackedPointers.isEmpty()) return

        val pointersToCancel = ArrayList<Int>()
        for (entry in trackedPointers.entries) {
            if (entry.value.chain.fastAny { it.node === node }) {
                pointersToCancel.add(entry.key)
            }
        }
        pointersToCancel.fastForEach { cancelPointer(it) }
    }

    // ─── Keyboard & Focus Routing ─────────────────────────────────────

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

    fun keyDown(keyCode: KeyCode?): Boolean =
        focusedKeyHandler?.onKeyDown(keyCode) ?: false

    fun keyUp(keyCode: KeyCode?): Boolean =
        focusedKeyHandler?.onKeyUp(keyCode) ?: false

    fun dispose() {
        cancelAllActivePointers()
        trackedPointers.clear()
        hitTestManager.dispose()
        eventsScratch.clear()
        scratchSingletonLists.clear()
        focusedKeyHandler = null
    }
}
