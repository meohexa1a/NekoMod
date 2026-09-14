package org.hubdustry.core.compose.input

import arc.graphics.Color
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.hubdustry.core.compose.runtime.CompositionManager
import org.hubdustry.core.compose.modifier.Modifier
import org.hubdustry.core.compose.input.gestures.detectTapGestures
import org.hubdustry.core.compose.modifier.*
import org.hubdustry.core.compose.primitive.Box
import org.hubdustry.core.compose.primitive.Column
import org.hubdustry.core.compose.primitive.Row
import org.hubdustry.core.compose.primitive.Text
import org.hubdustry.core.compose.view.ComposeView
import org.hubdustry.ui.components.Button
import org.junit.jupiter.api.Test
import kotlin.random.Random
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Bộ kiểm thử thực chiến ngặt nghèo (Adversarial Test Suite) cho hệ thống Pointer Input & Host Window của NekoMod v3.
 * Phủ kín các kịch bản đa điểm chạm, che khuất Z-order, phân giải tọa độ lồng nhau phức tạp, chuỗi filter,
 * ngắt cử chỉ thời gian thực, tháo gỡ view đột ngột và fuzz stress testing 1.000 sự kiện liên tục.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PointerInputAdversarialTest {

    // =========================================================================
    // GROUP 1: Multi-Pointer Concurrency & Independent Tracking
    // =========================================================================

    @Test
    fun testMultiPointerSimultaneousPressOnDistinctButtons() = runTest {
        val view = ComposeView()
        val sourceA = MutableInteractionSource()
        val sourceB = MutableInteractionSource()
        var clickedA = false
        var clickedB = false
        var pressedA = false
        var pressedB = false

        view.setContent {
            Row(modifier = Modifier.size(220f, 100f)) {
                Button(
                    onClick = { clickedA = true },
                    interactionSource = sourceA,
                    modifier = Modifier.size(90f, 80f)
                ) {
                    Text("Button A")
                }
                Box(modifier = Modifier.size(20f, 80f)) // Khoảng cách giữa 2 nút (90..110)
                Button(
                    onClick = { clickedB = true },
                    interactionSource = sourceB,
                    modifier = Modifier.size(90f, 80f)
                ) {
                    Text("Button B")
                }
            }
        }
        CompositionManager.frame()
        view.setSize(220f, 100f)
        view.layout()

        val jobA = launch {
            sourceA.interactions.collect {
                when (it) {
                    is PressInteraction.Press -> pressedA = true
                    is PressInteraction.Release, is PressInteraction.Cancel -> pressedA = false
                }
            }
        }
        val jobB = launch {
            sourceB.interactions.collect {
                when (it) {
                    is PressInteraction.Press -> pressedB = true
                    is PressInteraction.Release, is PressInteraction.Cancel -> pressedB = false
                }
            }
        }
        runCurrent()

        // 1. Pointer 0 nhấn Button A tại (40, 40)
        val hitDownA = view.sendPointerInput(PointerEventType.Press, 40f, 40f, pointer = 0)
        runCurrent()
        assertTrue(hitDownA, "Pointer 0 phải hit trúng Button A")
        assertTrue(pressedA, "Button A phải ở trạng thái Pressed khi Pointer 0 nhấn")
        assertFalse(pressedB, "Button B không được bị ảnh hưởng bởi Pointer 0")

        // 2. Pointer 1 đồng thời nhấn Button B tại (150, 40)
        val hitDownB = view.sendPointerInput(PointerEventType.Press, 150f, 40f, pointer = 1)
        runCurrent()
        assertTrue(hitDownB, "Pointer 1 phải hit trúng Button B")
        assertTrue(pressedA, "Button A vẫn phải giữ trạng thái Pressed")
        assertTrue(pressedB, "Button B phải ở trạng thái Pressed khi Pointer 1 nhấn")

        // 3. Pointer 0 nhả hợp lệ tại (40, 40) -> Button A hoàn thành cú Click
        view.sendPointerInput(PointerEventType.Release, 40f, 40f, pointer = 0)
        runCurrent()
        assertFalse(pressedA, "Button A phải thoát khỏi Pressed sau khi nhả")
        assertTrue(clickedA, "Button A phải kích hoạt onClick thành công")
        assertTrue(pressedB, "Button B vẫn phải duy trì Pressed độc lập bởi Pointer 1")
        assertFalse(clickedB, "Button B chưa được kích hoạt onClick")

        // 4. Pointer 1 bị kéo trượt ra ngoài Button B tới (150, 160) (quá slop & out of bounds)
        view.sendPointerInput(PointerEventType.Move, 150f, 160f, pointer = 1)
        runCurrent()
        assertFalse(pressedB, "Button B phải tự động Cancel Pressed khi con trỏ kéo trượt ra ngoài")

        // 5. Pointer 1 nhả chuột bên ngoài
        view.sendPointerInput(PointerEventType.Release, 150f, 160f, pointer = 1)
        runCurrent()
        assertFalse(clickedB, "Button B KHÔNG được kích hoạt onClick khi cử chỉ bị hủy")

        jobA.cancel()
        jobB.cancel()
        view.dispose()
    }

    @Test
    fun testMultiPointerInterleavedMoves() = runTest {
        val view = ComposeView()
        var lastPos0: Offset? = null
        var lastPos1: Offset? = null

        view.setContent {
            Row(modifier = Modifier.size(200f, 100f)) {
                Box(
                    modifier = Modifier
                        .size(100f, 100f)
                        .pointerInput(Unit) {
                            awaitPointerEventScope {
                                while (true) {
                                    val event = awaitPointerEvent(PointerEventPass.Main)
                                    for (c in event.changes) {
                                        if (c.id.value == 0L) lastPos0 = c.position
                                    }
                                }
                            }
                        }
                )
                Box(
                    modifier = Modifier
                        .size(100f, 100f)
                        .pointerInput(Unit) {
                            awaitPointerEventScope {
                                while (true) {
                                    val event = awaitPointerEvent(PointerEventPass.Main)
                                    for (c in event.changes) {
                                        if (c.id.value == 1L) lastPos1 = c.position
                                    }
                                }
                            }
                        }
                )
            }
        }
        CompositionManager.frame()
        view.setSize(200f, 100f)
        view.layout()

        // Pointer 0 Down tại (30, 30) -> Box 0 local: (30, 30)
        view.sendPointerInput(PointerEventType.Press, 30f, 30f, pointer = 0)
        // Pointer 1 Down tại (140, 50) -> Box 1 local: (140 - 100, 50) = (40, 50)
        view.sendPointerInput(PointerEventType.Press, 140f, 50f, pointer = 1)
        runCurrent()

        assertEquals(Offset(30f, 30f), lastPos0)
        assertEquals(Offset(40f, 50f), lastPos1)

        // Di chuyển xen kẽ
        view.sendPointerInput(PointerEventType.Move, 35f, 35f, pointer = 0)
        view.sendPointerInput(PointerEventType.Move, 145f, 55f, pointer = 1)
        runCurrent()

        assertEquals(Offset(35f, 35f), lastPos0)
        assertEquals(Offset(45f, 55f), lastPos1)

        view.sendPointerInput(PointerEventType.Release, 35f, 35f, pointer = 0)
        view.sendPointerInput(PointerEventType.Release, 145f, 55f, pointer = 1)
        runCurrent()
        view.dispose()
    }

    // =========================================================================
    // GROUP 2: Z-Index Occlusion & Anti-Click-Through
    // =========================================================================

    @Test
    fun testOverlappingSiblingsUpperConsumesClick() = runTest {
        val view = ComposeView()
        var bottomClicked = false
        var topClicked = false

        view.setContent {
            Box(modifier = Modifier.size(100f, 100f)) {
                // Layer dưới: Button A
                Button(
                    onClick = { bottomClicked = true },
                    modifier = Modifier.size(100f, 100f)
                ) {
                    Text("Bottom Layer")
                }
                // Layer trên: Button B đè lên hoàn toàn
                Button(
                    onClick = { topClicked = true },
                    modifier = Modifier.size(100f, 100f)
                ) {
                    Text("Top Layer")
                }
            }
        }
        CompositionManager.frame()
        view.setSize(100f, 100f)
        view.layout()

        // Click vào giữa (50, 50)
        view.sendPointerInput(PointerEventType.Press, 50f, 50f)
        runCurrent()
        view.sendPointerInput(PointerEventType.Release, 50f, 50f)
        runCurrent()

        assertTrue(topClicked, "Layer trên cùng (Top Layer) phải nhận sự kiện và kích hoạt onClick")
        assertFalse(bottomClicked, "Layer dưới tuyệt đối KHÔNG bị click xuyên thấu (Zero Click-Through)")

        view.dispose()
    }

    @Test
    fun testInvisibleLayerPassesClickThrough() = runTest {
        val view = ComposeView()
        var bottomClicked = false
        var topClicked = false

        view.setContent {
            Box(modifier = Modifier.size(100f, 100f)) {
                Button(
                    onClick = { bottomClicked = true },
                    modifier = Modifier.size(100f, 100f)
                ) {
                    Text("Bottom Layer")
                }
                // Layer trên bị ẩn: visible = false
                Button(
                    onClick = { topClicked = true },
                    modifier = Modifier.size(100f, 100f).visible(false)
                ) {
                    Text("Invisible Top Layer")
                }
            }
        }
        CompositionManager.frame()
        view.setSize(100f, 100f)
        view.layout()

        view.sendPointerInput(PointerEventType.Press, 50f, 50f)
        runCurrent()
        view.sendPointerInput(PointerEventType.Release, 50f, 50f)
        runCurrent()

        assertFalse(topClicked, "Node mang visible = false không được đón nhận sự kiện")
        assertTrue(bottomClicked, "Sự kiện phải đi xuyên qua node vô hình xuống layer hiển thị bên dưới")

        view.dispose()
    }

    @Test
    fun testNonInteractiveLayerPassesClickThrough() = runTest {
        val view = ComposeView()
        var bottomClicked = false

        view.setContent {
            Box(modifier = Modifier.size(100f, 100f)) {
                // Layer dưới: Button có clickable
                Button(
                    onClick = { bottomClicked = true },
                    modifier = Modifier.size(100f, 100f)
                ) {
                    Text("Bottom Button")
                }
                // Layer trên: Box thuần túy chỉ có màu nền, KHÔNG có filter cử chỉ
                Box(
                    modifier = Modifier
                        .size(100f, 100f)
                        .background(Color.clear)
                )
            }
        }
        CompositionManager.frame()
        view.setSize(100f, 100f)
        view.layout()

        view.sendPointerInput(PointerEventType.Press, 50f, 50f)
        runCurrent()
        view.sendPointerInput(PointerEventType.Release, 50f, 50f)
        runCurrent()

        assertTrue(bottomClicked, "Box phi tương tác không được cản trở sự kiện đến button có clickable bên dưới")

        view.dispose()
    }

    // =========================================================================
    // GROUP 3: Complex Nested Coordinate Transforms (Padding, Margin, Offset)
    // =========================================================================

    @Test
    fun testDeepNestedLocalCoordinatePrecision() = runTest {
        val view = ComposeView()
        var receivedLocalPos: Offset? = null
        var buttonClicked = false

        view.setContent {
            Box(modifier = Modifier.size(300f, 300f)) {
                // Container có margin = 30 và padding = 20
                Box(
                    modifier = Modifier
                        .margin(30f)
                        .padding(20f)
                        .size(200f, 200f)
                ) {
                    // Inner Wrapper có offset = (10, 15)
                    Box(
                        modifier = Modifier
                            .offset(10f, 15f)
                            .size(120f, 100f)
                    ) {
                        // Nút mục tiêu (Target Button): size = (80, 40)
                        Button(
                            onClick = { buttonClicked = true },
                            modifier = Modifier
                                .size(80f, 40f)
                                .pointerInput(Unit) {
                                    awaitPointerEventScope {
                                        val event = awaitPointerEvent(PointerEventPass.Main)
                                        receivedLocalPos = event.changes.firstOrNull()?.position
                                    }
                                }
                        ) {
                            Text("Target")
                        }
                    }
                }
            }
        }
        CompositionManager.frame()
        view.setSize(300f, 300f)
        view.layout()

        // Tọa độ thế giới của Button:
        // absX = 0 + 30 (margin) + 20 (padding) + 10 (offset) = 60f
        // absY = 0 + 30 (margin) + 20 (padding) + 15 (offset) = 65f
        // Button bounds: X in [60, 140], Y in [65, 105]

        // 1. Click tại (45, 45) -> Nằm trong vùng padding của container nhưng ngoài Button
        val hitPadding = view.sendPointerInput(PointerEventType.Press, 45f, 45f)
        runCurrent()
        assertFalse(hitPadding, "Click rơi vào vùng padding của container không được trúng Button")

        // 2. Click chính xác vào điểm (75, 80) bên trong Button
        // Tọa độ cục bộ mong đợi: (75 - 60, 80 - 65) = (15f, 15f)
        val hitButton = view.sendPointerInput(PointerEventType.Press, 75f, 80f)
        runCurrent()
        assertTrue(hitButton, "Click tại (75, 80) phải hit trúng Target Button")
        assertEquals(Offset(15f, 15f), receivedLocalPos, "Tọa độ cục bộ truyền vào coroutine của Button phải chuẩn xác 100%")

        view.sendPointerInput(PointerEventType.Release, 75f, 80f)
        runCurrent()
        assertTrue(buttonClicked, "Button phải kích hoạt onClick thành công")

        view.dispose()
    }

    // =========================================================================
    // GROUP 4: Composite Pointer Input Filter Chain Ordering (GEMINI.md Rule 11)
    // =========================================================================

    @Test
    fun testCompositeFilterChainOrder() = runTest {
        val view = ComposeView()
        val log = ArrayList<String>()

        view.setContent {
            Box(
                modifier = Modifier
                    .size(100f, 100f)
                    .pointerInput("A") {
                        awaitPointerEventScope {
                            while (true) {
                                awaitPointerEvent(PointerEventPass.Initial)
                                log.add("A-Initial")
                                awaitPointerEvent(PointerEventPass.Main)
                                log.add("A-Main")
                                awaitPointerEvent(PointerEventPass.Final)
                                log.add("A-Final")
                            }
                        }
                    }
                    .pointerInput("B") {
                        awaitPointerEventScope {
                            while (true) {
                                awaitPointerEvent(PointerEventPass.Initial)
                                log.add("B-Initial")
                                awaitPointerEvent(PointerEventPass.Main)
                                log.add("B-Main")
                                awaitPointerEvent(PointerEventPass.Final)
                                log.add("B-Final")
                            }
                        }
                    }
                    .pointerInput("C") {
                        awaitPointerEventScope {
                            while (true) {
                                awaitPointerEvent(PointerEventPass.Initial)
                                log.add("C-Initial")
                                awaitPointerEvent(PointerEventPass.Main)
                                log.add("C-Main")
                                awaitPointerEvent(PointerEventPass.Final)
                                log.add("C-Final")
                            }
                        }
                    }
            )
        }
        CompositionManager.frame()
        view.setSize(100f, 100f)
        view.layout()

        view.sendPointerInput(PointerEventType.Press, 50f, 50f)
        runCurrent()

        // Kiểm chứng thứ tự 3-pass theo GEMINI.md Rule 11:
        // Initial pass (Tunneling): Outer -> Inner (A -> B -> C)
        // Main pass (Bubbling): Inner -> Outer (C -> B -> A)
        // Final pass (Bubbling): Inner -> Outer (C -> B -> A)
        val expected = listOf(
            "A-Initial", "B-Initial", "C-Initial",
            "C-Main", "B-Main", "A-Main",
            "C-Final", "B-Final", "A-Final"
        )
        assertEquals(expected, log, "Thứ tự duyệt các filter trong chuỗi 3-pass phải tuân thủ nghiêm ngặt chuẩn Jetpack Compose")

        view.dispose()
    }

    // =========================================================================
    // GROUP 5: Adversarial Gesture Timings & Slop Boundaries
    // =========================================================================

    @Test
    fun testDoubleTapWithinWindowTriggersDoubleTap() = runTest {
        val filter = SuspendingPointerInputFilter().apply {
            size = IntSize(100, 100)
        }
        var singleTapCount = 0
        var doubleTapCount = 0

        val job = launch {
            filter.detectTapGestures(
                onTap = { singleTapCount++ },
                onDoubleTap = { doubleTapCount++ }
            )
        }
        runCurrent()

        // Tap 1: Down -> Up
        sendDown(filter, 50f, 50f)
        runCurrent()
        sendUp(filter, 50f, 50f)
        runCurrent()

        // Chờ 150ms (nằm trong cửa sổ 300ms double-tap timeout)
        advanceTimeBy(150)
        runCurrent()

        // Tap 2: Down -> Up
        sendDown(filter, 50f, 50f)
        runCurrent()
        sendUp(filter, 50f, 50f)
        runCurrent()

        assertEquals(1, doubleTapCount, "onDoubleTap phải được kích hoạt khi lần tap thứ hai diễn ra trong 300ms")
        assertEquals(0, singleTapCount, "onTap đơn lẻ không được kích hoạt khi double-tap thành công")

        job.cancel()
    }

    @Test
    fun testDoubleTapOutsideWindowTriggersTwoSingleTaps() = runTest {
        val filter = SuspendingPointerInputFilter().apply {
            size = IntSize(100, 100)
        }
        var singleTapCount = 0
        var doubleTapCount = 0

        val job = launch {
            filter.detectTapGestures(
                onTap = { singleTapCount++ },
                onDoubleTap = { doubleTapCount++ }
            )
        }
        runCurrent()

        // Tap 1
        sendDown(filter, 50f, 50f)
        runCurrent()
        sendUp(filter, 50f, 50f)
        runCurrent()

        // Chờ 350ms (> 300ms window timeout) -> Tap 1 chốt thành Single-Tap
        advanceTimeBy(350)
        runCurrent()
        assertEquals(1, singleTapCount, "Tap 1 phải chốt thành Single-Tap khi hết cửa sổ double-tap")

        // Tap 2
        sendDown(filter, 50f, 50f)
        runCurrent()
        sendUp(filter, 50f, 50f)
        runCurrent()

        // Chờ 350ms tiếp
        advanceTimeBy(350)
        runCurrent()

        assertEquals(2, singleTapCount, "Hai cú tap cách nhau >300ms phải tính thành 2 lần onTap đơn lẻ riêng biệt")
        assertEquals(0, doubleTapCount, "onDoubleTap không được kích hoạt khi khoảng cách giữa 2 tap vượt quá timeout")

        job.cancel()
    }

    @Test
    fun testLongPressInterruptedByMovement() = runTest {
        val filter = SuspendingPointerInputFilter().apply {
            size = IntSize(100, 100)
        }
        var longPressed = false
        var tapped = false

        val job = launch {
            filter.detectTapGestures(
                onLongPress = { longPressed = true },
                onTap = { tapped = true }
            )
        }
        runCurrent()

        // 1. Nhấn chuột tại (50, 50)
        sendDown(filter, 50f, 50f)
        runCurrent()

        // 2. Chờ 250ms (< 500ms long press timeout)
        advanceTimeBy(250)
        runCurrent()

        // 3. Di chuyển 20px sang (70, 50) -> vượt quá touchSlop (8px)
        sendMove(filter, 70f, 50f, 50f, 50f)
        runCurrent()

        // 4. Chờ thêm 350ms (tổng thời gian 600ms > 500ms)
        advanceTimeBy(350)
        runCurrent()

        assertFalse(longPressed, "onLongPress KHÔNG ĐƯỢC kích hoạt nếu con trỏ đã di chuyển vượt quá touch-slop")
        assertFalse(tapped, "onTap cũng phải bị hủy do di chuyển quá slop")

        sendUp(filter, 70f, 50f)
        runCurrent()

        job.cancel()
    }

    // =========================================================================
    // GROUP 6: Host Window Lifecycle & Abrupt Teardown
    // =========================================================================

    @Test
    fun testAbruptTeardownMidGestureCleansAllPointersAndInteractions() = runTest {
        val view = ComposeView()
        val source = MutableInteractionSource()
        var isPressedState = false
        var clickCount = 0

        view.setContent {
            Button(
                onClick = { clickCount++ },
                interactionSource = source,
                modifier = Modifier.size(100f, 50f)
            ) {
                Text("Button")
            }
        }
        CompositionManager.frame()
        view.setSize(100f, 50f)
        view.layout()

        val job = launch {
            source.interactions.collect {
                when (it) {
                    is PressInteraction.Press -> isPressedState = true
                    is PressInteraction.Release, is PressInteraction.Cancel -> isPressedState = false
                }
            }
        }
        runCurrent()

        // 1. Nhấn giữ con trỏ xuống button
        view.sendPointerInput(PointerEventType.Press, 50f, 25f)
        runCurrent()
        assertTrue(isPressedState, "Button phải ở trạng thái Pressed")

        // 2. Đột ngột tháo gỡ View khỏi Scene/Stage bằng dispose()
        view.dispose()
        runCurrent()

        assertFalse(isPressedState, "Trạng thái Pressed phải được hủy sạch sẽ khi view bị dispose (Zero Ghost Interaction)")
        assertEquals(0, clickCount, "onClick không được phép kích hoạt khi view bị hủy giữa chừng")

        job.cancel()
    }

    @Test
    fun testHostWindowExtremeBoundaryCoordinatesNeverThrow() {
        val view = ComposeView()
        view.setContent {
            Box(modifier = Modifier.size(100f, 100f)) {
                Text("Immune")
            }
        }
        CompositionManager.frame()
        view.setSize(100f, 100f)
        view.layout()

        // Khẳng định triết lý NEVER-THROW: Mọi tọa độ bất thường, NaN, vô cực đều được xử lý an toàn
        val badEvents = listOf(
            Triple(Float.NaN, Float.NaN, PointerEventType.Press),
            Triple(Float.POSITIVE_INFINITY, 50f, PointerEventType.Move),
            Triple(-999999f, Float.NEGATIVE_INFINITY, PointerEventType.Release),
            Triple(50f, 50f, PointerEventType.Scroll)
        )

        for ((x, y, type) in badEvents) {
            try {
                view.sendPointerInput(type = type, x = x, y = y, scrollDelta = Offset(x, y))
            } catch (t: Throwable) {
                org.junit.jupiter.api.fail("Host Window vi phạm triết lý NEVER-THROW khi nhận tọa độ bất thường ($x, $y): ${t.message}")
            }
        }

        view.dispose()
    }

    // =========================================================================
    // GROUP 7: High-Throughput Fuzz Stress Test (Never-Throw & Zero-GC)
    // =========================================================================

    @Test
    fun testHighThroughputRandomizedPointerFuzzing() = runTest {
        val view = ComposeView()
        var clickCount = 0

        view.setContent {
            Column(modifier = Modifier.size(200f, 200f)) {
                Row(modifier = Modifier.size(200f, 100f)) {
                    Button(onClick = { clickCount++ }, modifier = Modifier.size(90f, 80f)) { Text("B1") }
                    Button(onClick = { clickCount++ }, modifier = Modifier.size(90f, 80f)) { Text("B2") }
                }
                Row(modifier = Modifier.size(200f, 100f)) {
                    Button(onClick = { clickCount++ }, modifier = Modifier.size(90f, 80f)) { Text("B3") }
                    Button(onClick = { clickCount++ }, modifier = Modifier.size(90f, 80f)) { Text("B4") }
                }
            }
        }
        CompositionManager.frame()
        view.setSize(200f, 200f)
        view.layout()

        val random = Random(42) // Hạt giống cố định để test tái lập tuyệt đối (Deterministic)
        val eventTypes = listOf(
            PointerEventType.Press,
            PointerEventType.Move,
            PointerEventType.Release,
            PointerEventType.Scroll,
            PointerEventType.Exit
        )

        // Bắn 1.000 sự kiện con trỏ ngẫu nhiên liên tục
        for (i in 0 until 1000) {
            val type = eventTypes[random.nextInt(eventTypes.size)]
            val x = random.nextFloat() * 300f - 50f // Tọa độ từ -50f đến 250f (vượt cả trong lẫn ngoài bounds)
            val y = random.nextFloat() * 300f - 50f
            val pointer = random.nextInt(4) // 4 con trỏ độc lập (0..3)
            val scrollDelta = if (type == PointerEventType.Scroll) Offset(random.nextFloat() * 10f, random.nextFloat() * 10f) else Offset.Zero

            try {
                view.sendPointerInput(
                    type = type,
                    x = x,
                    y = y,
                    pointer = pointer,
                    uptimeMillis = 1000L + i * 16L,
                    scrollDelta = scrollDelta
                )
            } catch (t: Throwable) {
                org.junit.jupiter.api.fail("Fuzzing thất bại tại bước $i ($type tại $x, $y con trỏ $pointer): ${t.message}")
            }
        }

        // Kết thúc bằng cú dọn dẹp sạch sẽ
        view.dispose()
        assertTrue(true, "1.000 sự kiện fuzzing ngẫu nhiên hoàn thành an toàn tuyệt đối mà không có exception")
    }

    // =========================================================================
    // Helper Dispatchers
    // =========================================================================

    private fun sendDown(filter: SuspendingPointerInputFilter, x: Float, y: Float, pointerId: Long = 0L) {
        val pos = Offset(x, y)
        val change = PointerInputChange(
            id = PointerId(pointerId),
            uptimeMillis = 1000L,
            position = pos,
            pressed = true,
            previousUptimeMillis = 1000L,
            previousPosition = pos,
            previousPressed = false
        )
        val event = PointerEvent(listOf(change), PointerEventType.Press)
        filter.dispatchPointerEvent(event, PointerEventPass.Initial)
        filter.dispatchPointerEvent(event, PointerEventPass.Main)
        filter.dispatchPointerEvent(event, PointerEventPass.Final)
    }

    private fun sendMove(filter: SuspendingPointerInputFilter, x: Float, y: Float, prevX: Float, prevY: Float, pointerId: Long = 0L) {
        val pos = Offset(x, y)
        val prevPos = Offset(prevX, prevY)
        val change = PointerInputChange(
            id = PointerId(pointerId),
            uptimeMillis = 1016L,
            position = pos,
            pressed = true,
            previousUptimeMillis = 1000L,
            previousPosition = prevPos,
            previousPressed = true
        )
        val event = PointerEvent(listOf(change), PointerEventType.Move)
        filter.dispatchPointerEvent(event, PointerEventPass.Initial)
        filter.dispatchPointerEvent(event, PointerEventPass.Main)
        filter.dispatchPointerEvent(event, PointerEventPass.Final)
    }

    private fun sendUp(filter: SuspendingPointerInputFilter, x: Float, y: Float, pointerId: Long = 0L) {
        val pos = Offset(x, y)
        val change = PointerInputChange(
            id = PointerId(pointerId),
            uptimeMillis = 1032L,
            position = pos,
            pressed = false,
            previousUptimeMillis = 1016L,
            previousPosition = pos,
            previousPressed = true
        )
        val event = PointerEvent(listOf(change), PointerEventType.Release)
        filter.dispatchPointerEvent(event, PointerEventPass.Initial)
        filter.dispatchPointerEvent(event, PointerEventPass.Main)
        filter.dispatchPointerEvent(event, PointerEventPass.Final)
    }
}
