package org.hubdustry.core.compose.input

import androidx.compose.runtime.getValue
import arc.graphics.Color
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.hubdustry.core.compose.primitive.Box
import org.hubdustry.ui.components.Button
import org.hubdustry.core.compose.primitive.Column
import org.hubdustry.core.compose.runtime.CompositionManager
import org.hubdustry.core.compose.view.ComposeView
import org.hubdustry.core.compose.modifier.Modifier
import org.hubdustry.core.compose.primitive.Row
import org.hubdustry.core.compose.primitive.Text
import org.hubdustry.core.compose.input.gestures.awaitAllPointersUp
import org.hubdustry.core.compose.input.gestures.awaitEachGesture
import org.hubdustry.core.compose.input.gestures.awaitFirstDown
import org.hubdustry.core.compose.input.gestures.detectHorizontalDragGestures
import org.hubdustry.core.compose.input.gestures.detectTapGestures
import org.hubdustry.core.compose.input.gestures.detectVerticalDragGestures
import org.hubdustry.core.compose.input.gestures.waitForUpOrCancellation
import org.hubdustry.core.compose.modifier.*
import org.hubdustry.core.layout.Alignment
import org.hubdustry.core.layout.AnchorPreset
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class PointerInputTest {

    @Test
    fun testValidTapTriggersOnTap() = runTest {
        val filter = SuspendingPointerInputFilter().apply {
            size = IntSize(100, 100)
        }

        var tapped = false
        var tappedOffset: Offset? = null

        val gestureJob = launch {
            filter.detectTapGestures(
                onTap = { offset ->
                    tapped = true
                    tappedOffset = offset
                }
            )
        }
        runCurrent()

        // 1. Gửi Down tại (20, 30)
        filter.sendDown(20f, 30f)
        runCurrent()

        // 2. Gửi Up tại (20, 30)
        filter.sendUp(20f, 30f, 20f, 30f)
        runCurrent()

        assertTrue(tapped, "onTap phải được kích hoạt khi click hợp lệ trong bounds")
        assertEquals(Offset(20f, 30f), tappedOffset)

        gestureJob.cancel()
    }

    @Test
    fun testMoveExceedingSlopCancelsTap() = runTest {
        val filter = SuspendingPointerInputFilter().apply {
            size = IntSize(100, 100)
        }

        var tapped = false

        val gestureJob = launch {
            filter.detectTapGestures(
                onTap = {
                    tapped = true
                }
            )
        }
        runCurrent()

        // 1. Down tại (10, 10)
        filter.sendDown(10f, 10f)
        runCurrent()

        // 2. Di chuyển tới (30, 10) -> khoảng cách 20px > touchSlop (8px)
        filter.sendMove(30f, 10f, 10f, 10f)
        runCurrent()

        // 3. Up tại (30, 10)
        filter.sendUp(30f, 10f, 30f, 10f)
        runCurrent()

        assertFalse(tapped, "onTap KHÔNG được kích hoạt khi con trỏ di chuyển vượt quá touch-slop")

        gestureJob.cancel()
    }

    @Test
    fun testOutOfBoundsCancelsTap() = runTest {
        val filter = SuspendingPointerInputFilter().apply {
            size = IntSize(100, 100)
        }

        var tapped = false

        val gestureJob = launch {
            filter.detectTapGestures(
                onTap = {
                    tapped = true
                }
            )
        }
        runCurrent()

        // 1. Down tại (90, 90) trong bounds
        filter.sendDown(90f, 90f)
        runCurrent()

        // 2. Di chuyển ra ngoài bounds (110, 90)
        filter.sendMove(110f, 90f, 90f, 90f)
        runCurrent()

        // 3. Up ngoài bounds
        filter.sendUp(110f, 90f, 110f, 90f)
        runCurrent()

        assertFalse(tapped, "onTap KHÔNG được kích hoạt khi con trỏ ra ngoài bounds")

        gestureJob.cancel()
    }

    @Test
    fun testLongPressTriggersAfter500Ms() = runTest {
        val filter = SuspendingPointerInputFilter().apply {
            size = IntSize(100, 100)
        }

        var longPressed = false
        var tapped = false
        var longPressOffset: Offset? = null

        val gestureJob = launch {
            filter.detectTapGestures(
                onLongPress = { offset ->
                    longPressed = true
                    longPressOffset = offset
                },
                onTap = {
                    tapped = true
                }
            )
        }
        runCurrent()

        // 1. Down tại (25, 25)
        filter.sendDown(25f, 25f)
        runCurrent()

        // 2. Tua nhanh thời gian ảo qua 500ms mà KHÔNG cần Thread.sleep
        advanceTimeBy(501)
        runCurrent()

        assertTrue(longPressed, "onLongPress phải được kích hoạt sau 500ms giữ yên")
        assertEquals(Offset(25f, 25f), longPressOffset)

        // 3. Nhả chuột sau khi đã kích hoạt long-press
        filter.sendUp(25f, 25f, 25f, 25f)
        runCurrent()

        assertFalse(tapped, "onTap KHÔNG được gọi nếu long-press đã kích hoạt trước đó")

        gestureJob.cancel()
    }

    @Test
    fun testInteractionSourceSequenceOnSuccessfulTap() = runTest {
        val interactionSource = MutableInteractionSource()
        val filter = SuspendingPointerInputFilter().apply {
            size = IntSize(100, 100)
        }

        val emittedList = ArrayList<Interaction>()
        val collectorJob = launch {
            interactionSource.interactions.collect {
                emittedList.add(it)
            }
        }

        val gestureJob = launch {
            filter.detectTapGestures(
                onPress = { offset ->
                    val press = PressInteraction.Press(offset)
                    interactionSource.emit(press)
                    val success = tryAwaitRelease()
                    if (success) {
                        interactionSource.emit(PressInteraction.Release(press))
                    } else {
                        interactionSource.emit(PressInteraction.Cancel(press))
                    }
                }
            )
        }
        runCurrent()

        // Down -> Up hợp lệ
        filter.sendDown(15f, 15f)
        runCurrent()
        filter.sendUp(15f, 15f, 15f, 15f)
        runCurrent()

        assertEquals(2, emittedList.size, "Phải phát đúng 2 tương tác: Press rồi Release")
        assertIs<PressInteraction.Press>(emittedList[0])
        assertIs<PressInteraction.Release>(emittedList[1])

        val press = emittedList[0] as PressInteraction.Press
        val release = emittedList[1] as PressInteraction.Release
        assertEquals(press, release.press, "Release interaction phải trỏ đúng về Press tương ứng")

        collectorJob.cancel()
        gestureJob.cancel()
    }

    @Test
    fun testInteractionSourceSequenceOnCancelledTap() = runTest {
        val interactionSource = MutableInteractionSource()
        val filter = SuspendingPointerInputFilter().apply {
            size = IntSize(100, 100)
        }

        val emittedList = ArrayList<Interaction>()
        val collectorJob = launch {
            interactionSource.interactions.collect {
                emittedList.add(it)
            }
        }

        val gestureJob = launch {
            filter.detectTapGestures(
                onPress = { offset ->
                    val press = PressInteraction.Press(offset)
                    interactionSource.emit(press)
                    val success = tryAwaitRelease()
                    if (success) {
                        interactionSource.emit(PressInteraction.Release(press))
                    } else {
                        interactionSource.emit(PressInteraction.Cancel(press))
                    }
                }
            )
        }
        runCurrent()

        // Down -> Move quá slop
        filter.sendDown(10f, 10f)
        runCurrent()
        filter.sendMove(35f, 10f, 10f, 10f)
        runCurrent()
        filter.sendUp(35f, 10f, 35f, 10f)
        runCurrent()

        assertEquals(2, emittedList.size, "Phải phát đúng 2 tương tác: Press rồi Cancel")
        assertIs<PressInteraction.Press>(emittedList[0])
        assertIs<PressInteraction.Cancel>(emittedList[1])

        val press = emittedList[0] as PressInteraction.Press
        val cancel = emittedList[1] as PressInteraction.Cancel
        assertEquals(press, cancel.press, "Cancel interaction phải trỏ đúng về Press tương ứng")

        collectorJob.cancel()
        gestureJob.cancel()
    }

    @Test
    fun testComposeViewSendPointerInputAndHitTesting() {
        val view = ComposeView()
        var clicked = false

        view.setContent {
            Box(
                modifier = Modifier
                    .clickable { clicked = true }
                    .size(100f, 50f)
            ) {
                Text(text = "Click me")
            }
        }

        // Bơm frame để compose cây node và khởi tạo LaunchedEffect
        CompositionManager.frame()

        view.setSize(200f, 100f)
        view.layout()

        // 1. Click trúng Box tại (20, 20) trong Compose Top-Left Y-down
        val hitDown = view.sendPointerInput(PointerEventType.Press, 20f, 20f)
        assertTrue(hitDown, "sendPointerInput Press phải trúng node có clickable")

        val hitUp = view.sendPointerInput(PointerEventType.Release, 20f, 20f)
        assertTrue(hitUp, "sendPointerInput Release phải thành công")

        assertTrue(clicked, "Click callback của Modifier.clickable phải được gọi")

        view.dispose()
    }

    @Test
    fun testButtonReactivePressedColor() {
        val view = ComposeView()
        val interactionSource = MutableInteractionSource()
        var clicked = false

        view.setContent {
            Button(
                onClick = { clicked = true },
                modifier = Modifier.size(80f, 40f),
                interactionSource = interactionSource,
                backgroundColor = Color.blue,
                pressedColor = Color.red
            ) {
                Text(text = "Button")
            }
        }

        CompositionManager.frame()
        view.setSize(100f, 100f)
        view.layout()

        val buttonNode = view.rootLayoutNode.children[0]
        assertEquals(Color.blue, buttonNode.backgroundColor, "Màu ban đầu phải là backgroundColor (Blue)")

        // Nhấn xuống
        view.sendPointerInput(PointerEventType.Press, 10f, 10f)
        CompositionManager.frame()

        assertEquals(Color.red, buttonNode.backgroundColor, "Khi nhấn xuống, màu phải reactive đổi sang pressedColor (Red)")

        // Nhả ra
        view.sendPointerInput(PointerEventType.Release, 10f, 10f)
        CompositionManager.frame()

        assertEquals(Color.blue, buttonNode.backgroundColor, "Khi nhả ra, màu phải reactive phục hồi lại backgroundColor (Blue)")
        assertTrue(clicked, "Button onClick phải được kích hoạt")

        view.dispose()
    }

    @Test
    fun testComposeViewMouseMoveHoverAndDispatch() {
        val view = ComposeView()
        var moveReceived = false
        var movePos: Offset? = null

        view.setContent {
            Box(
                modifier = Modifier
                    .pointerInput(Unit) {
                        awaitPointerEventScope {
                            val event = awaitPointerEvent(PointerEventPass.Main)
                            moveReceived = true
                            movePos = event.changes[0].position
                        }
                    }
                    .size(100f, 50f)
            )
        }

        CompositionManager.frame()
        view.setSize(200f, 100f)
        view.layout()

        // Di chuyển trúng Box tại (25, 25)
        val hitMove = view.sendPointerInput(PointerEventType.Move, 25f, 25f)
        assertTrue(hitMove, "sendPointerInput Move phải trúng node có pointerInputFilter")
        assertTrue(moveReceived, "Coroutine awaitPointerEvent phải nhận được Move event")
        assertEquals(Offset(25f, 25f), movePos)

        // Di chuyển ra ngoài Box tại (150, 80)
        val missMove = view.sendPointerInput(PointerEventType.Move, 150f, 80f)
        assertFalse(missMove, "sendPointerInput Move ra ngoài node không có filter phải trả về false")

        view.dispose()
    }

    @Test
    fun testNested3PassDispatchOrder() {
        val view = ComposeView()
        val passLog = ArrayList<String>()

        view.setContent {
            Box(
                modifier = Modifier
                    .pointerInput("parent") {
                        awaitPointerEventScope {
                            awaitPointerEvent(PointerEventPass.Initial)
                            passLog.add("Parent-Initial")
                            awaitPointerEvent(PointerEventPass.Main)
                            passLog.add("Parent-Main")
                        }
                    }
                    .size(100f, 100f)
            ) {
                Box(
                    modifier = Modifier
                        .pointerInput("child") {
                            awaitPointerEventScope {
                                awaitPointerEvent(PointerEventPass.Initial)
                                passLog.add("Child-Initial")
                                awaitPointerEvent(PointerEventPass.Main)
                                passLog.add("Child-Main")
                            }
                        }
                        .size(50f, 50f)
                )
            }
        }

        CompositionManager.frame()
        view.setSize(100f, 100f)
        view.layout()

        view.sendPointerInput(PointerEventType.Press, 20f, 20f)

        // Thứ tự mong đợi theo Jetpack Compose AOSP 3-pass:
        // Initial Pass (Tunneling: Root -> Leaf): Parent-Initial -> Child-Initial
        // Main Pass (Bubbling: Leaf -> Root): Child-Main -> Parent-Main
        assertEquals(listOf("Parent-Initial", "Child-Initial", "Child-Main", "Parent-Main"), passLog)

        view.dispose()
    }

    @Test
    fun testSampleComposeDialogResetButton() {
        val view = ComposeView()
        val countState = androidx.compose.runtime.mutableStateOf(0)

        view.setContent {
            val count by androidx.compose.runtime.remember { countState }
            val dynamicColor = when (count % 4) {
                0 -> Color.royal
                1 -> Color.forest
                2 -> Color.coral
                else -> Color.gold
            }

            Column(
                modifier = Modifier
                    .background(Color.darkGray)
                    .padding(12f),
                gap = 10f
            ) {
                // 1. Header Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 36f)
                        .background(Color.black)
                        .padding(8f)
                ) {
                    Text(text = "NekoMod v3 Layout Engine", textColor = Color.sky)
                    Box(
                        modifier = Modifier
                            .anchor(AnchorPreset.TOP_RIGHT)
                            .size(46f, 20f)
                            .background(Color.scarlet)
                            .padding(2f)
                    ) {
                        Text(text = "LIVE", textColor = Color.white)
                    }
                }

                // 2. Reactive Counter Card
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 44f),
                    gap = 8f
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(dynamicColor)
                            .padding(10f)
                    ) {
                        Text(text = "Count: $count", textColor = Color.white)
                    }

                    Button(
                        onClick = { countState.value++ },
                        modifier = Modifier
                            .fillMaxHeight()
                            .padding(10f),
                        backgroundColor = Color.forest,
                        pressedColor = Color.green
                    ) {
                        Text(text = "Tap (+1)", textColor = Color.white)
                    }

                    Button(
                        onClick = { countState.value = 0 },
                        modifier = Modifier
                            .fillMaxHeight()
                            .widthIn(min = 72f)
                            .padding(10f),
                        backgroundColor = Color.scarlet,
                        pressedColor = Color.crimson
                    ) {
                        Text(text = "Reset", textColor = Color.white)
                    }
                }

                // 3. Showcase
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 40f),
                    gap = 8f
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(Color.slate)
                            .padding(6f)
                    ) {
                        Text(text = "Flex: 1x", textColor = Color.white)
                    }
                    Box(
                        modifier = Modifier
                            .weight(2f)
                            .background(Color.navy)
                            .padding(6f)
                    ) {
                        Text(text = "Flex: 2x (Double Width)", textColor = Color.white)
                    }
                }

                // 4. Footer
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.CENTER),
                    gap = 8f
                ) {
                    Text(text = "Zero-GC Hot Paths * Berlin Wall Y-Down * AOSP Gestures", textColor = Color.gray)
                }
            }
        }

        CompositionManager.frame()
        view.setSize(560f, 260f)
        view.layout()

        val column = view.rootLayoutNode.children[0]
        val row = column.children[1] // Item 1 is the Counter Row
        val tapBtn = row.children[1]
        val resetBtn = row.children[2]

        val colAbsX = column.x
        val colAbsY = column.y
        val rowAbsX = colAbsX + row.x
        val rowAbsY = colAbsY + row.y

        val tapAbsX = rowAbsX + tapBtn.x
        val tapAbsY = rowAbsY + tapBtn.y
        val resetAbsX = rowAbsX + resetBtn.x
        val resetAbsY = rowAbsY + resetBtn.y

        // Test tap 25 times
        for (i in 1..25) {
            val tapCenterX = tapAbsX + tapBtn.width / 2f
            val tapCenterY = tapAbsY + tapBtn.height / 2f
            view.sendPointerInput(PointerEventType.Press, tapCenterX, tapCenterY)
            view.sendPointerInput(PointerEventType.Release, tapCenterX, tapCenterY)
            CompositionManager.frame()
        }

        assertEquals(25, countState.value)

        // Test reset click
        val resetCenterX = resetAbsX + resetBtn.width / 2f
        val resetCenterY = resetAbsY + resetBtn.height / 2f
        val hitDown = view.sendPointerInput(PointerEventType.Press, resetCenterX, resetCenterY)
        assertTrue(hitDown, "Hit-test phải trúng nút Reset")
        val hitUp = view.sendPointerInput(PointerEventType.Release, resetCenterX, resetCenterY)
        assertTrue(hitUp)
        CompositionManager.frame()

        assertEquals(0, countState.value, "Count phải được reset về 0 sau khi click Reset")

        // Test click near border with micro-jitter (di chuột nhẹ 2px ra ngoài mép nút khi nhả)
        countState.value = 10
        val resetNearBorderX = resetAbsX + resetBtn.width - 0.5f
        val resetJitterX = resetAbsX + resetBtn.width + 2.0f // 2px ngoài mép, nằm trong touchSlop (12px)

        val hitDownBorder = view.sendPointerInput(PointerEventType.Press, resetNearBorderX, resetCenterY)
        assertTrue(hitDownBorder, "Hit-test phải trúng nút Reset gần mép")
        view.sendPointerInput(PointerEventType.Move, resetJitterX, resetCenterY)
        val hitUpBorder = view.sendPointerInput(PointerEventType.Release, resetJitterX, resetCenterY)
        assertTrue(hitUpBorder)
        CompositionManager.frame()

        assertEquals(0, countState.value, "Count phải reset về 0 ngay cả khi chuột rung nhẹ qua mép nút trong ngưỡng touchSlop")

        view.dispose()
    }

    @Test
    fun testComposeViewHitAndInputOverflowContent() {
        val view = ComposeView()

        view.setContent {
            Box(modifier = Modifier.widthIn(min = 700f).heightIn(min = 300f)) {
                Button(
                    onClick = { },
                    modifier = Modifier.size(80f, 40f)
                ) {
                    Text("OverflowBtn")
                }
            }
        }

        CompositionManager.frame()
        // Giả lập Arc Scene2D gán kích thước của Cửa sổ mẹ
        view.setSize(560f, 260f)
        view.layout()

        // Node gốc TỰ ĐỘNG MỞ ĐÚNG THEO KÍCH THƯỚC CỬA SỔ MẸ (560f x 260f)
        assertEquals(560f, view.rootLayoutNode.width, "Node gốc phải tự động mở đúng theo chiều rộng của cửa sổ mẹ")
        assertEquals(260f, view.rootLayoutNode.height, "Node gốc phải tự động mở đúng theo chiều cao của cửa sổ mẹ")

        // Arc Scene2D Element.hit chấp nhận click bên trong kích thước cửa sổ mẹ
        val arcHitInside = view.hit(500f, 100f, true)
        assertEquals(view, arcHitInside, "ComposeView.hit phải chấp nhận tọa độ nằm trong bounds của cửa sổ mẹ")

        // Tọa độ sát biên
        val arcHitBorder = view.hit(560f, 260f, true)
        assertEquals(view, arcHitBorder, "ComposeView.hit phải chấp nhận tọa độ sát mép biên")

        // Tọa độ ngoài bounds của cửa sổ mẹ
        val arcHitOutside = view.hit(561f, 100f, true)
        assertEquals(null, arcHitOutside, "ComposeView.hit phải từ chối tọa độ vượt ra ngoài cửa sổ mẹ")

        view.dispose()
    }

    @Test
    fun testBorderClickWithMicroJitterSucceeds() = runTest {
        val filter = SuspendingPointerInputFilter().apply {
            size = IntSize(100, 100)
        }

        var tapped = false
        var tappedOffset: Offset? = null

        val gestureJob = launch {
            filter.detectTapGestures(
                onTap = { offset ->
                    tapped = true
                    tappedOffset = offset
                }
            )
        }
        runCurrent()

        // 1. Down tại (99, 50) - sát mép phải (100)
        filter.sendDown(99f, 50f)
        runCurrent()

        // 2. Chuột rung nhẹ 2.5px sang (101.5, 50) - vượt qua mép phải nhưng nằm trong touchSlop (12px)
        filter.sendMove(101.5f, 50f, 99f, 50f)
        runCurrent()

        // 3. Nhả chuột tại (101.5, 50)
        filter.sendUp(101.5f, 50f, 101.5f, 50f)
        runCurrent()

        assertTrue(tapped, "onTap phải kích hoạt thành công khi rung chuột nhẹ qua mép trong ngưỡng touch-slop")
        assertEquals(Offset(101.5f, 50f), tappedOffset)

        gestureJob.cancel()
    }

    @Test
    fun testSecondaryTapRightClick() = runTest {
        val filter = SuspendingPointerInputFilter().apply {
            size = IntSize(100, 100)
        }

        var secondaryTapped = false
        var normalTapped = false

        val gestureJob = launch {
            filter.detectTapGestures(
                onTap = { normalTapped = true },
                onSecondaryTap = { secondaryTapped = true }
            )
        }
        runCurrent()

        // 1. Click chuột phải (PointerButton.Secondary)
        filter.sendDown(50f, 50f, button = PointerButton.Secondary, pointerType = PointerType.Mouse)
        runCurrent()
        filter.sendUp(50f, 50f, 50f, 50f)
        runCurrent()

        assertTrue(secondaryTapped, "onSecondaryTap phải được kích hoạt khi click chuột phải")
        assertFalse(normalTapped, "onTap không được kích hoạt khi click chuột phải")

        gestureJob.cancel()
    }

    @Test
    fun testHoverEnterAndExitInteractions() {
        val view = ComposeView()
        val interactionSource = MutableInteractionSource()
        val emittedInteractions = ArrayList<Interaction>()

        view.setContent {
            Box(
                modifier = Modifier
                    .hoverable(interactionSource = interactionSource)
                    .size(100f, 100f)
            ) {
                Text("HoverBox")
            }
        }
        CompositionManager.frame()
        view.setSize(200f, 200f)
        view.layout()

        val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Unconfined)
        val job = scope.launch {
            interactionSource.interactions.collect {
                emittedInteractions.add(it)
            }
        }

        // 1. Rê chuột vào (50, 50) - bên trong Box (100x100)
        view.sendPointerInput(PointerEventType.Move, 50f, 50f, pointer = 0)
        CompositionManager.frame()

        assertEquals(1, emittedInteractions.size, "Phải phát HoverInteraction.Enter khi chuột vào box")
        assertIs<HoverInteraction.Enter>(emittedInteractions[0])

        // 2. Rê chuột ra ngoài (150, 150) - ngoài Box
        view.sendPointerInput(PointerEventType.Move, 150f, 150f, pointer = 0)
        CompositionManager.frame()

        assertEquals(2, emittedInteractions.size, "Phải phát HoverInteraction.Exit khi chuột rời box")
        assertIs<HoverInteraction.Exit>(emittedInteractions[1])

        job.cancel()
        view.dispose()
    }

    @Test
    fun testAwaitEachGestureLifecycle() = runTest {
        val filter = SuspendingPointerInputFilter().apply {
            size = IntSize(100, 100)
        }

        var gestureCount = 0

        val job = launch {
            filter.awaitEachGesture {
                awaitFirstDown()
                gestureCount++
                waitForUpOrCancellation()
            }
        }
        runCurrent()

        // Lượt 1
        filter.sendDown(10f, 10f)
        runCurrent()
        filter.sendUp(10f, 10f, 10f, 10f)
        runCurrent()
        assertEquals(1, gestureCount)

        // Lượt 2
        filter.sendDown(20f, 20f)
        runCurrent()
        filter.sendUp(20f, 20f, 20f, 20f)
        runCurrent()
        assertEquals(2, gestureCount)

        job.cancel()
    }

    // --- Helper Extensions ---

    private fun SuspendingPointerInputFilter.sendDown(
        x: Float,
        y: Float,
        pointerId: Long = 0L,
        uptime: Long = System.currentTimeMillis(),
        button: PointerButton? = PointerButton.Primary,
        pointerType: PointerType = PointerType.Touch
    ) {
        val pos = Offset(x, y)
        val change = PointerInputChange(
            id = PointerId(pointerId),
            uptimeMillis = uptime,
            position = pos,
            pressed = true,
            previousUptimeMillis = uptime,
            previousPosition = pos,
            previousPressed = false,
            button = button,
            type = pointerType
        )
        val event = PointerEvent(listOf(change), PointerEventType.Press)
        dispatchPointerEvent(event, PointerEventPass.Initial)
        dispatchPointerEvent(event, PointerEventPass.Main)
        dispatchPointerEvent(event, PointerEventPass.Final)
    }

    private fun SuspendingPointerInputFilter.sendMove(x: Float, y: Float, prevX: Float, prevY: Float, pointerId: Long = 0L, uptime: Long = System.currentTimeMillis()) {
        val pos = Offset(x, y)
        val prevPos = Offset(prevX, prevY)
        val change = PointerInputChange(
            id = PointerId(pointerId),
            uptimeMillis = uptime,
            position = pos,
            pressed = true,
            previousUptimeMillis = uptime - 16,
            previousPosition = prevPos,
            previousPressed = true
        )
        val event = PointerEvent(listOf(change), PointerEventType.Move)
        dispatchPointerEvent(event, PointerEventPass.Initial)
        dispatchPointerEvent(event, PointerEventPass.Main)
        dispatchPointerEvent(event, PointerEventPass.Final)
    }

    private fun SuspendingPointerInputFilter.sendUp(x: Float, y: Float, prevX: Float, prevY: Float, pointerId: Long = 0L, uptime: Long = System.currentTimeMillis()) {
        val pos = Offset(x, y)
        val prevPos = Offset(prevX, prevY)
        val change = PointerInputChange(
            id = PointerId(pointerId),
            uptimeMillis = uptime,
            position = pos,
            pressed = false,
            previousUptimeMillis = uptime - 16,
            previousPosition = prevPos,
            previousPressed = true
        )
        val event = PointerEvent(listOf(change), PointerEventType.Release)
        dispatchPointerEvent(event, PointerEventPass.Initial)
        dispatchPointerEvent(event, PointerEventPass.Main)
        dispatchPointerEvent(event, PointerEventPass.Final)
    }

    @Test
    fun testMouseScrollWheelEventDispatchedThroughComposeView() = runTest {
        val view = ComposeView()
        var receivedScrollDelta: Offset? = null
        var receivedEventType: PointerEventType? = null

        view.setContent {
            Box(
                modifier = Modifier
                    .size(200f, 200f)
                    .pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent(PointerEventPass.Main)
                                receivedEventType = event.type
                                val change = event.changes.firstOrNull()
                                receivedScrollDelta = change?.scrollDelta
                                change?.consume()
                            }
                        }
                    }
            )
        }
        CompositionManager.frame()

        view.setSize(200f, 200f)
        view.layout()

        // Gửi sự kiện cuộn con lăn chuột tại (50, 50) với delta = (0, 5)
        val handled = view.sendPointerInput(
            type = PointerEventType.Scroll,
            x = 50f,
            y = 50f,
            scrollDelta = Offset(0f, 5f)
        )

        assertTrue(handled, "Scroll event must be handled and consumed by the targeted node")
        assertEquals(PointerEventType.Scroll, receivedEventType)
        assertEquals(Offset(0f, 5f), receivedScrollDelta)
        view.dispose()
    }

    @Test
    fun testMouseScrollWheelEventUnconsumedReturnsFalse() = runTest {
        val view = ComposeView()
        view.setContent {
            Box(
                modifier = Modifier
                    .size(200f, 200f)
                    .pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                awaitPointerEvent(PointerEventPass.Main)
                                // Không consume!
                            }
                        }
                    }
            )
        }
        CompositionManager.frame()
        view.setSize(200f, 200f)
        view.layout()

        val handled = view.sendPointerInput(
            type = PointerEventType.Scroll,
            x = 50f,
            y = 50f,
            scrollDelta = Offset(0f, 5f)
        )

        assertFalse(handled, "Scroll event not consumed must return false so outer Arc ScrollPane can scroll")
        view.dispose()
    }

    @Test
    fun testMouseExitCancelsActivePressedState() = runTest {
        val view = ComposeView()
        val interactionSource = MutableInteractionSource()
        var isPressedState = false
        var clickCount = 0

        view.setContent {
            Button(
                onClick = { clickCount++ },
                interactionSource = interactionSource,
                modifier = Modifier.size(100f, 50f)
            ) {
                Text("Click Me")
            }
        }
        CompositionManager.frame()

        view.setSize(100f, 50f)
        view.layout()

        val job = launch {
            interactionSource.interactions.collect { interaction ->
                when (interaction) {
                    is PressInteraction.Press -> isPressedState = true
                    is PressInteraction.Release, is PressInteraction.Cancel -> isPressedState = false
                }
            }
        }
        runCurrent()

        // 1. Nhấn chuột xuống tại (30, 20) -> Phải kích hoạt Pressed
        val hitDown = view.sendPointerInput(PointerEventType.Press, 30f, 20f)
        runCurrent()
        assertTrue(hitDown, "sendPointerInput Press phải trúng Button")
        assertTrue(isPressedState, "Button must be in Pressed state after touchDown")

        // 2. Chuột rời khỏi cửa sổ Arc trong khi đang nhấn -> Giải phóng và Cancel cử chỉ
        view.sendPointerInput(PointerEventType.Exit, -100f, -100f)
        runCurrent()
        assertFalse(isPressedState, "Button must not be in Pressed state after mouse exit")
        assertEquals(0, clickCount, "Click callback KHÔNG được gọi khi bị hủy do Exit")

        job.cancel()
        view.dispose()
    }

    @Test
    fun testAwaitAllPointersUpReturnsImmediatelyWhenNoPointersDown() = runTest {
        val filter = SuspendingPointerInputFilter().apply {
            size = IntSize(100, 100)
        }
        var completed = false
        val job = launch {
            filter.awaitPointerEventScope {
                awaitAllPointersUp()
                completed = true
            }
        }
        runCurrent()
        assertTrue(completed, "awaitAllPointersUp must return immediately when currentEvent has no pressed pointers")
        job.cancel()
    }

    @Test
    fun testVerticalDragDoesNotTriggerOnHorizontalSwipe() = runTest {
        val filter = SuspendingPointerInputFilter().apply {
            size = IntSize(200, 200)
        }
        var dragStarted = false
        var verticalDragAmount = 0f

        val job = launch {
            filter.detectVerticalDragGestures(
                onDragStart = { dragStarted = true },
                onVerticalDrag = { _, amount -> verticalDragAmount += amount }
            )
        }
        runCurrent()

        // 1. Down ti (50, 50)
        filter.sendDown(50f, 50f)
        runCurrent()

        // 2. Vu`t ngang 30px sang phi (quA touchSlop 12px), khA'ng di chuyn d?c (deltaY = 0)
        filter.sendMove(80f, 50f, 50f, 50f)
        runCurrent()

        assertFalse(dragStarted, "Vertical drag must NOT start on horizontal swipe")
        assertEquals(0f, verticalDragAmount, "onVerticalDrag must NOT be called on horizontal swipe")

        // 3. Up
        filter.sendUp(80f, 50f, 80f, 50f)
        runCurrent()

        job.cancel()
    }

    @Test
    fun testVerticalDragTriggersOnVerticalSwipe() = runTest {
        val filter = SuspendingPointerInputFilter().apply {
            size = IntSize(200, 200)
        }
        var dragStarted = false
        var verticalDragAmount = 0f

        val job = launch {
            filter.detectVerticalDragGestures(
                onDragStart = { dragStarted = true },
                onVerticalDrag = { _, amount -> verticalDragAmount += amount }
            )
        }
        runCurrent()

        // 1. Down ti (50, 50)
        filter.sendDown(50f, 50f)
        runCurrent()

        // 2. Vu`t d?c xu`ng 25px (quA touchSlop 12px)
        filter.sendMove(50f, 75f, 50f, 50f)
        runCurrent()

        assertTrue(dragStarted, "Vertical drag MUST start on vertical swipe")
        assertEquals(25f, verticalDragAmount, "onVerticalDrag must receive the accumulated vertical displacement")

        // 3. Tip tc kAco thAAm 10px
        filter.sendMove(50f, 85f, 50f, 75f)
        runCurrent()
        assertEquals(35f, verticalDragAmount)

        filter.sendUp(50f, 85f, 50f, 85f)
        runCurrent()

        job.cancel()
    }

    @Test
    fun testHorizontalDragDoesNotTriggerOnVerticalSwipe() = runTest {
        val filter = SuspendingPointerInputFilter().apply {
            size = IntSize(200, 200)
        }
        var dragStarted = false
        var horizontalDragAmount = 0f

        val job = launch {
            filter.detectHorizontalDragGestures(
                onDragStart = { dragStarted = true },
                onHorizontalDrag = { _, amount -> horizontalDragAmount += amount }
            )
        }
        runCurrent()

        // 1. Down ti (50, 50)
        filter.sendDown(50f, 50f)
        runCurrent()

        // 2. Vu`t d?c xu`ng 30px (quA touchSlop 12px), khA'ng di chuyn ngang (deltaX = 0)
        filter.sendMove(50f, 80f, 50f, 50f)
        runCurrent()

        assertFalse(dragStarted, "Horizontal drag must NOT start on vertical swipe")
        assertEquals(0f, horizontalDragAmount, "onHorizontalDrag must NOT be called on vertical swipe")

        filter.sendUp(50f, 80f, 50f, 80f)
        runCurrent()

        job.cancel()
    }

    @Test
    fun testHorizontalDragTriggersOnHorizontalSwipe() = runTest {
        val filter = SuspendingPointerInputFilter().apply {
            size = IntSize(200, 200)
        }
        var dragStarted = false
        var horizontalDragAmount = 0f

        val job = launch {
            filter.detectHorizontalDragGestures(
                onDragStart = { dragStarted = true },
                onHorizontalDrag = { _, amount -> horizontalDragAmount += amount }
            )
        }
        runCurrent()

        // 1. Down ti (50, 50)
        filter.sendDown(50f, 50f)
        runCurrent()

        // 2. Vu`t ngang sang phi 25px (quA touchSlop 12px)
        filter.sendMove(75f, 50f, 50f, 50f)
        runCurrent()

        assertTrue(dragStarted, "Horizontal drag MUST start on horizontal swipe")
        assertEquals(25f, horizontalDragAmount, "onHorizontalDrag must receive accumulated horizontal displacement")

        filter.sendUp(75f, 50f, 75f, 50f)
        runCurrent()

        job.cancel()
    }
}
