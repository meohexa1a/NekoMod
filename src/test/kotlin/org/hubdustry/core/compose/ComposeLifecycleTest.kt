package org.hubdustry.core.compose

import androidx.compose.runtime.mutableStateOf
import arc.graphics.Color
import org.hubdustry.core.compose.modifier.*
import org.hubdustry.core.compose.primitive.Box
import org.hubdustry.core.compose.primitive.Column
import org.hubdustry.core.compose.primitive.Row
import org.hubdustry.core.compose.primitive.Text
import org.hubdustry.core.compose.view.ComposeView
import org.hubdustry.core.layout.AnchorPreset
import org.hubdustry.core.layout.LayoutNode
import java.lang.ref.WeakReference
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.hubdustry.core.compose.input.clickable

class ComposeLifecycleTest {

    @Test
    fun testLayoutNodeApplierBasicOperations() {
        val root = LayoutNode()
        val applier = LayoutNodeApplier(root)

        val child1 = LayoutNode().apply { text = "1" }
        val child2 = LayoutNode().apply { text = "2" }

        // Test insertTopDown
        applier.insertTopDown(0, child1)
        applier.insertTopDown(1, child2)

        assertEquals(2, root.children.size)
        assertEquals("1", root.children[0].text)
        assertEquals("2", root.children[1].text)

        // Test move
        applier.move(0, 2, 1)
        assertEquals("2", root.children[0].text)
        assertEquals("1", root.children[1].text)

        // Test remove
        applier.remove(0, 1)
        assertEquals(1, root.children.size)
        assertEquals("1", root.children[0].text)

        // Test clear
        applier.clear()
        assertEquals(0, root.children.size)
    }

    @Test
    fun testComposeViewRecompositionAndDispose() {
        val view = ComposeView()
        val counterState = mutableStateOf(0)

        // Thiết lập nội dung Composable
        view.setContent {
            val count = counterState.value
            Box(
                modifier = Modifier
                    .background(Color.red)
                    .size(100f, 50f)
            ) {
                Text(text = "Count: $count")
            }
        }

        // Bơm 1 frame để Recomposer xử lý composition ban đầu
        CompositionManager.frame()

        // Kiểm tra cây node đã được tạo
        assertEquals(1, view.rootLayoutNode.children.size)
        val boxNode = view.rootLayoutNode.children[0]
        assertEquals(Color.red, boxNode.backgroundColor)
        assertEquals(1, boxNode.children.size)
        assertEquals("Count: 0", boxNode.children[0].text)

        // Thay đổi state và bơm frame
        counterState.value = 42
        CompositionManager.frame()

        // Kiểm tra Recomposition reactive
        assertEquals("Count: 42", boxNode.children[0].text)

        // Thử nghiệm dispose
        view.dispose()
        assertEquals(0, view.rootLayoutNode.children.size)
    }

    @Test
    fun testShowcaseHierarchyLayoutCalculation() {
        val view = ComposeView()
        view.setContent {
            Column(
                modifier = Modifier.padding(12f),
                gap = 10f
            ) {
                // Header (minHeight = 36f)
                Box(modifier = Modifier.heightIn(min = 36f).padding(8f)) {
                    Text(text = "Header")
                    Box(
                        modifier = Modifier
                            .anchor(AnchorPreset.TOP_RIGHT)
                            .size(46f, 20f)
                    ) {
                        Text(text = "LIVE")
                    }
                }

                // Counter (minHeight = 44f)
                Box(modifier = Modifier.heightIn(min = 44f).padding(10f)) {
                    Text(text = "Counter")
                }

                // Flex row 1x vs 2x (gap = 8f, minHeight = 40f)
                Row(modifier = Modifier.heightIn(min = 40f), gap = 8f) {
                    Box(modifier = Modifier.weight(1f)) {
                        Text(text = "1x")
                    }
                    Box(modifier = Modifier.weight(2f)) {
                        Text(text = "2x")
                    }
                }
            }
        }

        // Bơm frame để compose cây
        CompositionManager.frame()

        // Sizing view to 480 x 260
        view.setSize(480f, 260f)
        view.layout()

        val root = view.rootLayoutNode
        assertEquals(480f, root.width, 0.001f)
        assertEquals(260f, root.height, 0.001f)

        // Root Column lấp đầy view
        assertEquals(1, root.children.size)
        val column = root.children[0]
        assertEquals(480f, column.width, 0.001f)
        assertEquals(260f, column.height, 0.001f)

        // Column có 3 con trực tiếp
        assertEquals(3, column.children.size)
        val headerBox = column.children[0]
        val counterBox = column.children[1]
        val flexRow = column.children[2]

        // Header: x = 12, y = 12, w = 480 - 24 = 456, h = 36
        assertEquals(12f, headerBox.x, 0.001f)
        assertEquals(12f, headerBox.y, 0.001f)
        assertEquals(456f, headerBox.width, 0.001f)
        assertEquals(36f, headerBox.height, 0.001f)

        // LIVE badge bên trong Header (anchor TOP_RIGHT): x = 456 - 8 (padRight) - 46 = 402, y = 8 (padTop)
        val liveBadge = headerBox.children[1]
        assertEquals(456f - 8f - 46f, liveBadge.x, 0.001f)
        assertEquals(8f, liveBadge.y, 0.001f)
        assertEquals(46f, liveBadge.width, 0.001f)
        assertEquals(20f, liveBadge.height, 0.001f)

        // Counter: x = 12, y = 12 + 36 + 10 = 58, w = 456, h = 44
        assertEquals(12f, counterBox.x, 0.001f)
        assertEquals(58f, counterBox.y, 0.001f)
        assertEquals(456f, counterBox.width, 0.001f)
        assertEquals(44f, counterBox.height, 0.001f)

        // Flex row: x = 12, y = 58 + 44 + 10 = 112, w = 456, h = 40
        assertEquals(12f, flexRow.x, 0.001f)
        assertEquals(112f, flexRow.y, 0.001f)
        assertEquals(456f, flexRow.width, 0.001f)
        assertEquals(40f, flexRow.height, 0.001f)

        // Phân bổ 1x vs 2x: tổng w = 456, gap = 8, mỗi box có Text minWidth=16f (tổng min=32f)
        // free = 456 - 8 - 32 = 416f => 1x = 16 + 416/3 = 154.667f, 2x = 16 + 416*2/3 = 293.333f
        val box1x = flexRow.children[0]
        val box2x = flexRow.children[1]
        assertEquals(0f, box1x.x, 0.001f)
        assertEquals(16f + 416f / 3f, box1x.width, 0.01f)
        assertEquals(box1x.width + 8f, box2x.x, 0.01f)
        assertEquals(16f + 416f * 2f / 3f, box2x.width, 0.01f)

        view.dispose()
    }

    @Test
    fun testWeakReferenceCollectionAfterDispose() {
        // Tạo view trong một scope hàm riêng biệt để không còn con trỏ mạnh trên stack
        val weakRef = createViewAndDispose()

        // Ép JVM Garbage Collector chạy thu hồi
        repeat(5) {
            System.gc()
            Thread.sleep(20)
        }

        // Khẳng định đối tượng đã được giải phóng hoàn toàn
        assertNull(weakRef.get(), "ComposeView phải được GC thu hồi sạch sẽ sau khi dispose")
    }

    private fun createViewAndDispose(): WeakReference<ComposeView> {
        val view = ComposeView()
        val textState = mutableStateOf("Initial")

        view.setContent {
            Box(modifier = Modifier.size(50f, 50f)) {
                Text(text = textState.value)
            }
        }

        CompositionManager.frame()
        assertTrue(view.rootLayoutNode.children.isNotEmpty())

        val ref = WeakReference(view)
        view.dispose()
        return ref
    }

    @Test
    fun testGhostNodeInvalidationOnNodeRemoval() {
        val view = ComposeView()
        val showItem = mutableStateOf(true)
        var itemClicked = false

        view.setContent {
            Column(modifier = Modifier.size(200f, 200f)) {
                if (showItem.value) {
                    Box(
                        modifier = Modifier
                            .size(100f, 50f)
                            .clickable { itemClicked = true }
                    ) {
                        Text("Removable Item")
                    }
                }
            }
        }

        CompositionManager.frame()
        view.setSize(200f, 200f)
        view.layout()

        // 1. Nhn chutt xunng trAn item (Press) -> Bt u track pointer vA hover
        val hitDown = view.sendPointerInput(org.hubdustry.core.compose.input.PointerEventType.Press, 20f, 20f)
        assertTrue(hitDown, "Press must hit removable item")

        // 2. XA3a item kh?i cAy bng cAch thay Tic reactive state
        showItem.value = false
        CompositionManager.frame()
        view.layout()

        // XAc nhn item `A b< g- kh?i Virtual Tree
        val column = view.rootLayoutNode.children[0]
        assertEquals(0, column.children.size, "Removable item must be detached from tree")

        // 3. G-i s ki?n tip theo (Move hoc Release) - khAng `c nAm ngoi l? vA khAng cAn dA-nh ti node c
        view.sendPointerInput(org.hubdustry.core.compose.input.PointerEventType.Release, 20f, 20f)
        assertFalse(itemClicked, "Removed item must not trigger click after being detached")

        view.dispose()
    }
}
