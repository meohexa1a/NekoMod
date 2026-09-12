package org.hubdustry.libs.compose

import androidx.compose.runtime.mutableStateOf
import arc.graphics.Color
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.hubdustry.libs.compose.input.InteractionSource
import org.hubdustry.libs.compose.input.IntSize
import org.hubdustry.libs.compose.input.MutableInteractionSource
import org.hubdustry.libs.compose.input.Offset
import org.hubdustry.libs.compose.input.PointerEventType
import org.hubdustry.libs.compose.input.collectIsHoveredAsState
import org.hubdustry.libs.compose.input.collectIsPressedAsState
import org.hubdustry.libs.compose.modifier.*
import org.hubdustry.libs.layout.SizeFlag
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ArchitectureEnhancementsTest {

    @Test
    fun testOffsetAndIntSizeValueClasses() {
        val offset = Offset(15.5f, -20.25f)
        assertEquals(15.5f, offset.x, 0.001f)
        assertEquals(-20.25f, offset.y, 0.001f)
        assertTrue(offset.isSpecified)

        val offset2 = Offset(5f, 10f)
        val sum = offset + offset2
        assertEquals(20.5f, sum.x, 0.001f)
        assertEquals(-10.25f, sum.y, 0.001f)

        val diff = offset - offset2
        assertEquals(10.5f, diff.x, 0.001f)
        assertEquals(-30.25f, diff.y, 0.001f)

        val size = IntSize(320, 240)
        assertEquals(320, size.width)
        assertEquals(240, size.height)

        val negativeSize = IntSize(-1, -1)
        assertEquals(-1, negativeSize.width)
        assertEquals(-1, negativeSize.height)
    }

    @Test
    fun testChainedHoverAndClickOnButton() {
        val view = ComposeView()
        val interactionSource = MutableInteractionSource()
        var clicked = false

        view.setContent {
            Button(
                onClick = { clicked = true },
                modifier = Modifier.size(100f, 50f),
                interactionSource = interactionSource,
                backgroundColor = Color.blue
            ) {
                Text("TestButton")
            }
        }

        CompositionManager.frame()
        view.setSize(200f, 200f)
        view.layout()

        val buttonNode = view.rootLayoutNode.children[0]
        // Khẳng định node đã nhận cả 2 filter: hoverable và clickable!
        assertEquals(2, buttonNode.pointerInputFilters.size, "Button phải gắn đồng thời cả 2 filters: hoverable và clickable")

        // 1. Rê chuột vào giữa Button (50, 25)
        val hitMove = view.sendPointerInput(PointerEventType.Move, 50f, 25f)
        assertTrue(hitMove)
        CompositionManager.frame()

        // 2. Click nút (Press & Release)
        view.sendPointerInput(PointerEventType.Press, 50f, 25f)
        CompositionManager.frame()

        view.sendPointerInput(PointerEventType.Release, 50f, 25f)
        CompositionManager.frame()

        assertTrue(clicked, "Clickable filter phải kích hoạt onClick thành công dù đi sau hoverable")

        view.dispose()
    }

    @Test
    fun testContainerScopedWeights() {
        val view = ComposeView()
        view.setContent {
            Row {
                Box(modifier = Modifier.weight(1f)) {
                    Text("RowChild")
                }
            }
            Column {
                Box(modifier = Modifier.weight(2f)) {
                    Text("ColumnChild")
                }
            }
        }

        CompositionManager.frame()
        view.setSize(300f, 300f)
        view.layout()

        val rowNode = view.rootLayoutNode.children[0]
        val rowChild = rowNode.children[0]
        // RowScope.weight: chỉ mở EXPAND theo trục ngang (sizeFlagHorizontal), trục dọc không bị ép EXPAND
        assertEquals(SizeFlag.EXPAND, rowChild.sizeFlagHorizontal)
        assertEquals(SizeFlag.FILL, rowChild.sizeFlagVertical, "RowScope.weight không được phép ép trục dọc thành EXPAND")
        assertEquals(1f, rowChild.stretchRatio)

        val colNode = view.rootLayoutNode.children[1]
        val colChild = colNode.children[0]
        // ColumnScope.weight: chỉ mở EXPAND theo trục dọc (sizeFlagVertical), trục ngang không bị ép EXPAND
        assertEquals(SizeFlag.FILL, colChild.sizeFlagHorizontal, "ColumnScope.weight không được phép ép trục ngang thành EXPAND")
        assertEquals(SizeFlag.EXPAND, colChild.sizeFlagVertical)
        assertEquals(2f, colChild.stretchRatio)

        view.dispose()
    }

    @Test
    fun testTextMeasurerRespectsExplicitConstraints() {
        val node = org.hubdustry.libs.layout.LayoutNode()
        // Đặt kích thước chặt chẽ qua modifier: maxWidth = 80f
        node.minWidth = 0f
        node.maxWidth = 80f
        node.minHeight = 0f
        node.maxHeight = 30f

        // Đo chuỗi văn bản dài
        TextMeasurer.measure(node, "This is a very long text string that would naturally measure well over 200 pixels")

        // Khẳng định minWidth bị kẹp lại ở maxWidth = 80f, không làm phình maxWidth lên 200px+
        assertEquals(80f, node.minWidth, "minWidth từ TextMeasurer phải bị chặn lại ở maxWidth")
        assertEquals(80f, node.maxWidth, "maxWidth không được phép bị xé rách bởi TextMeasurer")
    }

    @Test
    fun testDeepRecursiveDisposal() {
        val view = ComposeView()
        view.setContent {
            Column {
                Box {
                    Button(onClick = {}, modifier = Modifier.size(50f)) {
                        Text("Btn")
                    }
                }
            }
        }

        CompositionManager.frame()
        view.setSize(100f, 100f)
        view.layout()

        val col = view.rootLayoutNode.children[0]
        val box = col.children[0]
        val btn = box.children[0]

        assertTrue(btn.pointerInputFilters.isNotEmpty(), "Button ban đầu phải có pointer filters")

        view.dispose()

        assertEquals(0, view.rootLayoutNode.children.size, "Cây ảo phải được xóa sạch sau khi dispose")
        assertEquals(0, btn.pointerInputFilters.size, "Toàn bộ filter của node con sâu bên trong phải được giải phóng sạch sẽ")
    }
}
