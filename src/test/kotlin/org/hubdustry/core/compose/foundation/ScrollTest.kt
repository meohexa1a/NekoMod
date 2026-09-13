package org.hubdustry.core.compose.foundation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.hubdustry.core.compose.CompositionManager
import org.hubdustry.core.compose.Modifier
import org.hubdustry.core.compose.input.Offset
import org.hubdustry.core.compose.input.PointerEventType
import org.hubdustry.core.compose.input.clickable
import org.hubdustry.core.compose.modifier.horizontalScroll
import org.hubdustry.core.compose.modifier.size
import org.hubdustry.core.compose.modifier.verticalScroll
import org.hubdustry.core.compose.modifier.ScrollModifier
import org.hubdustry.core.compose.primitive.Box
import org.hubdustry.core.compose.primitive.Column
import org.hubdustry.core.compose.primitive.Row
import org.hubdustry.core.compose.view.ComposeView
import org.hubdustry.core.layout.LayoutNode
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
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
        assertTrue(node.isScrollableVertical)
        assertFalse(node.isScrollableHorizontal)
        assertEquals(vState, node.verticalScrollState)
        assertEquals(15f, node.scrollY)

        val hMod = ScrollModifier(hState, isVertical = false, enabled = true)
        hMod.applyTo(node)

        assertTrue(node.isScrollableHorizontal)
        assertEquals(hState, node.horizontalScrollState)
        assertEquals(25f, node.scrollX)

        node.resetModifierState()
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
}
