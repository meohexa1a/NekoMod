package org.hubdustry.core.compose.foundation

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.hubdustry.core.compose.runtime.CompositionManager
import org.hubdustry.core.compose.modifier.Modifier
import org.hubdustry.core.compose.input.Offset
import org.hubdustry.core.compose.input.PointerEventType
import org.hubdustry.core.compose.modifier.clickable
import org.hubdustry.core.compose.modifier.horizontalScroll
import org.hubdustry.core.compose.modifier.size
import org.hubdustry.core.compose.modifier.verticalScroll
import org.hubdustry.core.compose.modifier.ScrollModifier
import org.hubdustry.core.compose.primitive.Box
import org.hubdustry.core.compose.primitive.Column
import org.hubdustry.core.compose.view.ComposeView
import org.hubdustry.core.layout.LayoutNode
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ScrollTest {

    @Test
    fun testScrollModifierSetsPropertiesOnLayoutNode() {
        val node = LayoutNode()
        val vState = ScrollState(15f)
        val hState = ScrollState(25f)

        val vMod = ScrollModifier(vState, isVertical = true, enabled = true)
        vMod.applyTo(node)

        assertTrue(node.clip, "ScrollModifier phải kích hoạt clipping trên node")
        assertTrue(node.clipVertical, "ScrollModifier dọc phải bật clipVertical")
        assertFalse(node.clipHorizontal, "ScrollModifier dọc không được bật clipHorizontal")
        assertTrue(node.isScrollableVertical)
        assertFalse(node.isScrollableHorizontal)
        assertEquals(vState, node.verticalScrollState)
        assertEquals(15f, node.scrollY)

        val hMod = ScrollModifier(hState, isVertical = false, enabled = true)
        hMod.applyTo(node)

        assertTrue(node.clipHorizontal, "ScrollModifier ngang phải bật clipHorizontal")
        assertTrue(node.isScrollableHorizontal)
        assertEquals(hState, node.horizontalScrollState)
        assertEquals(25f, node.scrollX)

        node.resetModifierState()
        assertFalse(node.clip, "resetModifierState phải tắt toàn bộ clip")
        assertFalse(node.clipVertical, "resetModifierState phải tắt clipVertical")
        assertFalse(node.clipHorizontal, "resetModifierState phải tắt clipHorizontal")
        assertFalse(node.isScrollableVertical)
        assertFalse(node.isScrollableHorizontal)
        assertEquals(null, node.verticalScrollState)
        assertEquals(null, node.horizontalScrollState)
        assertEquals(0f, node.scrollX)
        assertEquals(0f, node.scrollY)
    }

    @Test
    fun testVerticalScrollContentOverflowAndClamping() {
        val view = ComposeView()
        val state = ScrollState()

        view.setContent {
            Column(
                modifier = Modifier
                    .size(100f, 200f)
                    .verticalScroll(state)
            ) {
                Box(modifier = Modifier.size(100f, 100f))
                Box(modifier = Modifier.size(100f, 100f))
                Box(modifier = Modifier.size(100f, 100f))
                Box(modifier = Modifier.size(100f, 100f))
                Box(modifier = Modifier.size(100f, 100f))
            }
        }
        CompositionManager.frame()
        view.setSize(100f, 200f)
        view.layout()

        val root = view.rootLayoutNode
        val scrollColumn = root.children[0]

        // Content cao 500f, Viewport cao 200f -> maxScrollY = 300f
        assertEquals(500f, scrollColumn.contentHeight)
        assertEquals(200f, scrollColumn.height)
        assertEquals(300f, scrollColumn.maxScrollY)
        assertEquals(300f, state.maxValue)
        assertEquals(200f, state.viewportSize)

        // Dispatch delta 150f
        val consumed1 = state.dispatchRawDelta(150f)
        assertEquals(150f, consumed1)
        assertEquals(150f, state.value)
        assertEquals(150f, scrollColumn.scrollY)

        // Dispatch delta vượt quá maxValue
        val consumed2 = state.dispatchRawDelta(200f)
        assertEquals(150f, consumed2, "Chỉ được tiêu thụ đến maxValue (300f - 150f = 150f)")
        assertEquals(300f, state.value)
        assertEquals(300f, scrollColumn.scrollY)

        // Dispatch delta âm về dưới 0
        val consumed3 = state.dispatchRawDelta(-500f)
        assertEquals(-300f, consumed3)
        assertEquals(0f, state.value)
        assertEquals(0f, scrollColumn.scrollY)
    }

    @Test
    fun testMouseWheelScrollUpdatesScrollState() {
        val view = ComposeView()
        val state = ScrollState()

        view.setContent {
            Column(
                modifier = Modifier
                    .size(150f, 200f)
                    .verticalScroll(state)
            ) {
                Box(modifier = Modifier.size(150f, 300f))
                Box(modifier = Modifier.size(150f, 300f))
            }
        }
        CompositionManager.frame()
        view.setSize(150f, 200f)
        view.layout()

        assertEquals(0f, state.value)

        // Cuộn con lăn chuột xuống với amountY = 2f tại (50f, 50f)
        val handled = view.sendPointerInput(
            type = PointerEventType.Scroll,
            x = 50f,
            y = 50f,
            scrollDelta = Offset(0f, 2f)
        )
        assertTrue(handled, "Sự kiện Scroll phải được xử lý bởi verticalScroll")
        assertEquals(64f, state.value, "amountY * 32f = 64f")

        // Cuộn tiếp 1 đơn vị
        view.sendPointerInput(
            type = PointerEventType.Scroll,
            x = 50f,
            y = 50f,
            scrollDelta = Offset(0f, 1f)
        )
        assertEquals(96f, state.value, "64f + 32f = 96f")
    }

    @Test
    fun testTouchDragScrollUpdatesScrollState() {
        val view = ComposeView()
        val state = ScrollState()

        view.setContent {
            Column(
                modifier = Modifier
                    .size(200f, 200f)
                    .verticalScroll(state)
            ) {
                Box(modifier = Modifier.size(200f, 600f))
            }
        }
        CompositionManager.frame()
        view.setSize(200f, 200f)
        view.layout()

        // 1. Nhấn ngón tay xuống tại (100f, 150f)
        view.sendPointerInput(PointerEventType.Press, 100f, 150f, pointer = 0)
        assertEquals(0f, state.value)

        // 2. Kéo ngón tay lên (100f, 100f) -> deltaY = -50f, vượt quá touch-slop (10px)
        // delta gửi vào scrollState là -(-50f) = +50f
        view.sendPointerInput(PointerEventType.Move, 100f, 100f, pointer = 0)
        assertEquals(50f, state.value, "Kéo trượt 50px lên phải tăng scrollState thêm 50px")

        // 3. Kéo thêm 30px lên (100f, 70f)
        view.sendPointerInput(PointerEventType.Move, 100f, 70f, pointer = 0)
        assertEquals(80f, state.value, "50f + 30f = 80f")

        // 4. Nhả ngón tay
        view.sendPointerInput(PointerEventType.Release, 100f, 70f, pointer = 0)
        assertEquals(80f, state.value)
    }

    @Test
    fun testHitTestTranslatesCoordinatesWithScroll() {
        val view = ComposeView()
        val state = ScrollState()
        var clickedIndex = -1

        view.setContent {
            Column(
                modifier = Modifier
                    .size(100f, 200f)
                    .verticalScroll(state)
            ) {
                Box(
                    modifier = Modifier
                        .size(100f, 100f)
                        .clickable { clickedIndex = 0 }
                )
                Box(
                    modifier = Modifier
                        .size(100f, 100f)
                        .clickable { clickedIndex = 1 }
                )
                Box(
                    modifier = Modifier
                        .size(100f, 100f)
                        .clickable { clickedIndex = 2 }
                )
                Box(
                    modifier = Modifier
                        .size(100f, 100f)
                        .clickable { clickedIndex = 3 }
                )
            }
        }
        CompositionManager.frame()
        view.setSize(100f, 200f)
        view.layout()

        // Khi scrollY = 0f:
        // Item 0: y = 0..100
        // Item 1: y = 100..200
        view.sendPointerInput(PointerEventType.Press, 50f, 50f)
        view.sendPointerInput(PointerEventType.Release, 50f, 50f)
        assertEquals(0, clickedIndex, "Tại y=50 phải trúng item 0 khi scrollY=0")

        view.sendPointerInput(PointerEventType.Press, 50f, 150f)
        view.sendPointerInput(PointerEventType.Release, 50f, 150f)
        assertEquals(1, clickedIndex, "Tại y=150 phải trúng item 1 khi scrollY=0")

        // Bây giờ cuộn xuống 100px:
        // Item 0: y = -100..0 (bị cuộn khuất)
        // Item 1: y = 0..100 (chiếm nửa trên viewport)
        // Item 2: y = 100..200 (chiếm nửa dưới viewport)
        state.dispatchRawDelta(100f)
        assertEquals(100f, state.value)

        // Click lại tại y = 50f -> giờ phải trúng Item 1!
        view.sendPointerInput(PointerEventType.Press, 50f, 50f)
        view.sendPointerInput(PointerEventType.Release, 50f, 50f)
        assertEquals(1, clickedIndex, "Tại y=50 phải trúng item 1 khi scrollY=100")

        // Click tại y = 150f -> giờ phải trúng Item 2!
        view.sendPointerInput(PointerEventType.Press, 50f, 150f)
        view.sendPointerInput(PointerEventType.Release, 50f, 150f)
        assertEquals(2, clickedIndex, "Tại y=150 phải trúng item 2 khi scrollY=100")
    }

    @Test
    fun testHitTestRejectsOutOfViewportTouches() {
        val view = ComposeView()
        val state = ScrollState()
        var clicked = false

        view.setContent {
            Box(modifier = Modifier.size(300f, 300f)) {
                Column(
                    modifier = Modifier
                        .size(100f, 100f)
                        .verticalScroll(state)
                ) {
                    Box(
                        modifier = Modifier
                            .size(100f, 200f)
                            .clickable { clicked = true }
                    )
                }
            }
        }
        CompositionManager.frame()
        view.setSize(300f, 300f)
        view.layout()

        // Thử click tại (50f, 150f) - nằm ngoài Column viewport (cao 100f) nhưng nằm trong Box cha (300x300)
        // Item dài 200f nhưng bị kẹp trong Column 100f có clip=true.
        val hit = view.sendPointerInput(PointerEventType.Press, 50f, 150f)
        assertFalse(hit, "Chạm vào ngoài viewport của scroll container phải bị từ chối")
        assertFalse(clicked)
    }

    @Test
    fun testBidirectionalScrollIndependentStates() {
        val view = ComposeView()
        val vState = ScrollState()
        val hState = ScrollState()

        view.setContent {
            Box(
                modifier = Modifier
                    .size(200f, 200f)
                    .horizontalScroll(hState)
                    .verticalScroll(vState)
            ) {
                Box(modifier = Modifier.size(500f, 600f))
            }
        }
        CompositionManager.frame()
        view.setSize(200f, 200f)
        view.layout()

        val root = view.rootLayoutNode
        val scrollBox = root.children[0]

        assertEquals(500f, scrollBox.contentWidth)
        assertEquals(600f, scrollBox.contentHeight)
        assertEquals(300f, hState.maxValue, "500 - 200 = 300")
        assertEquals(400f, vState.maxValue, "600 - 200 = 400")

        hState.dispatchRawDelta(120f)
        assertEquals(120f, hState.value)
        assertEquals(120f, scrollBox.scrollX)
        assertEquals(0f, vState.value)
        assertEquals(0f, scrollBox.scrollY)

        vState.dispatchRawDelta(250f)
        assertEquals(120f, scrollBox.scrollX)
        assertEquals(250f, scrollBox.scrollY)
    }

    @Test
    fun testScrollStateGatewaySanitization() {
        val state = ScrollState(initial = -10f)
        assertEquals(0f, state.value, "Initial âm phải được chuẩn hóa về 0f")

        val stateNaN = ScrollState(initial = Float.NaN)
        assertEquals(0f, stateNaN.value, "Initial NaN phải được chuẩn hóa về 0f")

        state.maxValue = 200f
        val consumed = state.dispatchRawDelta(Float.NaN)
        assertEquals(0f, consumed, "Delta NaN phải bị từ chối")
        assertEquals(0f, state.value)

        state.snapTo(Float.NaN)
        assertEquals(0f, state.value, "snapTo NaN không được thay đổi state")

        state.snapTo(-50f)
        assertEquals(0f, state.value, "snapTo âm phải bị kẹp về 0f")

        state.snapTo(500f)
        assertEquals(200f, state.value, "snapTo vượt max phải bị kẹp về maxValue")
    }

    @Test
    fun testAnimateScrollToAndBy() = runTest {
        val state = ScrollState(0f)
        state.maxValue = 300f

        // Animate tới 150f
        state.animateScrollTo(150f, durationMillis = 50)
        assertEquals(150f, state.value)

        // Animate tương đối thêm 50f
        state.animateScrollBy(50f, durationMillis = 50)
        assertEquals(200f, state.value)

        // Animate vượt quá maxValue
        state.animateScrollBy(200f, durationMillis = 50)
        assertEquals(300f, state.value)

        // Animate về âm
        state.animateScrollTo(-100f, durationMillis = 50)
        assertEquals(0f, state.value)
    }

    @Test
    fun testTouchDragFlingWithKineticInertia() {
        val view = ComposeView()
        val state = ScrollState()

        view.setContent {
            Column(
                modifier = Modifier
                    .size(200f, 200f)
                    .verticalScroll(state, fling = true)
            ) {
                Box(modifier = Modifier.size(200f, 1000f))
            }
        }
        CompositionManager.frame()
        view.setSize(200f, 200f)
        view.layout()

        // Giả lập vuốt nhanh (flick): di chuyển 150px trong 80ms (vận tốc ~ 1875 px/s)
        val t0 = 10000L
        view.sendPointerInput(PointerEventType.Press, 100f, 200f, uptimeMillis = t0)
        view.sendPointerInput(PointerEventType.Move, 100f, 160f, uptimeMillis = t0 + 25L)
        view.sendPointerInput(PointerEventType.Move, 100f, 110f, uptimeMillis = t0 + 50L)
        view.sendPointerInput(PointerEventType.Move, 100f, 50f, uptimeMillis = t0 + 80L)

        val draggedValue = state.value
        assertTrue(draggedValue >= 140f, "Kéo trượt phải tăng scroll state ít nhất 140px")

        // Nhấc tay nhả ngón -> kích hoạt Fling
        view.sendPointerInput(PointerEventType.Release, 100f, 50f, uptimeMillis = t0 + 80L)

        // Fling coroutine bắt đầu đẩy scroll state vượt xa vị trí kéo ban đầu
        assertTrue(state.value > draggedValue, "Quán tính fling phải tiếp tục cuộn vượt quá vị trí kéo thả")
    }

    @Test
    fun testTouchToStopCancelsActiveFling() {
        val view = ComposeView()
        val state = ScrollState()

        view.setContent {
            Column(
                modifier = Modifier
                    .size(200f, 200f)
                    .verticalScroll(state, fling = true)
            ) {
                Box(modifier = Modifier.size(200f, 1000f))
            }
        }
        CompositionManager.frame()
        view.setSize(200f, 200f)
        view.layout()

        val t0 = 10000L
        view.sendPointerInput(PointerEventType.Press, 100f, 200f, uptimeMillis = t0)
        view.sendPointerInput(PointerEventType.Move, 100f, 150f, uptimeMillis = t0 + 30L)
        view.sendPointerInput(PointerEventType.Move, 100f, 100f, uptimeMillis = t0 + 60L)
        view.sendPointerInput(PointerEventType.Release, 100f, 100f, uptimeMillis = t0 + 60L)

        val flungValue = state.value

        // Chạm tay xuống ngay lập tức (Touch-to-Stop)
        view.sendPointerInput(PointerEventType.Press, 100f, 100f, uptimeMillis = t0 + 70L)
        val stoppedValue = state.value

        assertEquals(flungValue, stoppedValue, "Chạm ngón tay phải dừng lập tức quán tính cuộn")
    }

    @Test
    fun testMouseWheelScrollAndScrollFocusLifecycle() {
        val view = ComposeView()
        val state = ScrollState()

        view.setContent {
            Column(
                modifier = Modifier
                    .size(200f, 200f)
                    .verticalScroll(state)
            ) {
                Box(modifier = Modifier.size(200f, 1000f))
            }
        }
        CompositionManager.frame()
        view.setSize(200f, 200f)
        view.layout()

        // 1. Khởi tạo MockGraphics và MockGL20 cho headless test môi trường Arc
        val prevGraphics = arc.Core.graphics
        val prevGl = arc.Core.gl
        val prevGl20 = arc.Core.gl20
        try {
            if (arc.Core.graphics == null) {
                arc.Core.graphics = arc.mock.MockGraphics()
            }
            if (arc.Core.gl == null && runCatching { Class.forName("arc.mock.MockGL20") }.isSuccess) {
                arc.Core.gl = Class.forName("arc.mock.MockGL20").getDeclaredConstructor().newInstance() as arc.graphics.GL20
                arc.Core.gl20 = arc.Core.gl
            }
            val scene = arc.scene.Scene()
            scene.add(view)

            // Chuột di chuyển vào -> phải tự động xin scrollFocus từ Scene
            val inputAdapter = view.listeners.find { it is org.hubdustry.core.compose.view.ArcInputAdapter }
                as? org.hubdustry.core.compose.view.ArcInputAdapter
            kotlin.test.assertNotNull(inputAdapter, "ComposeView phải sở hữu ArcInputAdapter")

            inputAdapter.enter(null, 50f, 50f, -1, null)
            assertEquals(view, scene.scrollFocus, "Rê chuột vào phải xin quyền scrollFocus từ Scene")

            // 2. Con lăn chuột quay -> cuộn nội dung
            val scrolled = inputAdapter.scrolled(null, 50f, 50f, 0f, 2f)
            assertTrue(scrolled, "scrolled() phải trả về true khi có bộ cuộn tiêu thụ")
            assertTrue(state.value > 0f, "Con lăn chuột phải làm tăng ScrollState")

            // 3. Chuột rời khỏi -> giải phóng scrollFocus
            inputAdapter.exit(null, -10f, -10f, -1, null)
            assertNull(scene.scrollFocus, "Chuột rời khỏi phải giải phóng scrollFocus của Scene")
        } finally {
            view.dispose()
            arc.Core.graphics = prevGraphics
            arc.Core.gl = prevGl
            arc.Core.gl20 = prevGl20
        }
    }

    @Test
    fun testDiagonalSwipeDoesNotFreezeDragDetector() {
        val view = ComposeView()
        val state = ScrollState()

        view.setContent {
            Column(
                modifier = Modifier
                    .size(200f, 200f)
                    .verticalScroll(state)
            ) {
                Box(modifier = Modifier.size(200f, 1000f))
            }
        }
        CompositionManager.frame()
        view.setSize(200f, 200f)
        view.layout()

        // Giả lập vuốt chéo: dx = 30px, dy = 10px trong vài frame đầu (lệch ngang > dọc)
        val t0 = 20000L
        view.sendPointerInput(PointerEventType.Press, 100f, 100f, uptimeMillis = t0)
        view.sendPointerInput(PointerEventType.Move, 130f, 90f, uptimeMillis = t0 + 16L)

        // Sau đó người dùng tiếp tục vuốt mạnh theo chiều dọc (dy = 60px)
        view.sendPointerInput(PointerEventType.Move, 135f, 40f, uptimeMillis = t0 + 32L)

        // Bộ nhận diện không bị freeze, khi dy vượt touch slop thì vertical drag vẫn hoạt động!
        assertTrue(state.value > 0f, "Vuốt chéo không được làm tê liệt bộ nhận diện kéo dọc")

        // Nhả ngón tay
        view.sendPointerInput(PointerEventType.Release, 135f, 40f, uptimeMillis = t0 + 48L)

        // Lần vuốt tiếp theo phải bắt đầu sạch sẽ ngay lập tức (không bị kẹt trong awaitFirstDown)
        val prevVal = state.value
        view.sendPointerInput(PointerEventType.Press, 100f, 100f, uptimeMillis = t0 + 100L)
        view.sendPointerInput(PointerEventType.Move, 100f, 60f, uptimeMillis = t0 + 120L)
        assertTrue(state.value > prevVal, "Cú vuốt tiếp theo phải hoạt động bình thường ngay sau khi nhả ngón")

        view.dispose()
    }

    @Test
    fun testDraggingPastBoundaryKeepsTrackingAndReversesFluidly() {
        val view = ComposeView()
        val state = ScrollState(0f)

        view.setContent {
            Column(
                modifier = Modifier
                    .size(200f, 200f)
                    .verticalScroll(state)
            ) {
                Box(modifier = Modifier.size(200f, 1000f))
            }
        }
        CompositionManager.frame()
        view.setSize(200f, 200f)
        view.layout()

        val t0 = 30000L
        // 1. Đang ở đỉnh (0px), cố kéo xuống dưới (delta < 0, vượt biên âm)
        view.sendPointerInput(PointerEventType.Press, 100f, 50f, uptimeMillis = t0)
        view.sendPointerInput(PointerEventType.Move, 100f, 90f, uptimeMillis = t0 + 20L)
        assertEquals(0f, state.value, "Kéo vượt biên âm phải được kẹp tại 0")

        // 2. Không nhả tay, đảo chiều kéo ngược lên trên (delta > 0)
        view.sendPointerInput(PointerEventType.Move, 100f, 10f, uptimeMillis = t0 + 40L)
        assertTrue(state.value > 0f, "Đảo chiều kéo sau khi vượt biên phải cuộn mượt mà không bị kẹt hay giật cục")

        view.sendPointerInput(PointerEventType.Release, 100f, 10f, uptimeMillis = t0 + 50L)
        view.dispose()
    }
}
