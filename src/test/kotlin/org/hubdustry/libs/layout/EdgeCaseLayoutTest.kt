package org.hubdustry.libs.layout

import org.hubdustry.libs.layout.policies.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EdgeCaseLayoutTest {

    @Test
    fun testAncestorCyclePrevention() {
        val nodeA = LayoutNode()
        val nodeB = LayoutNode()
        val nodeC = LayoutNode()

        nodeA.addChild(nodeB)
        nodeB.addChild(nodeC)

        // Thử tạo chu trình lặp: nodeC.addChild(nodeA)
        // Nếu không có guard, A -> B -> C -> A sẽ gây StackOverflowError khi gọi layout()
        nodeC.addChild(nodeA)

        // Xác nhận nodeA KHÔNG được phép thêm vào nodeC
        assertFalse(nodeC.children.contains(nodeA))
        assertEquals(nodeA, nodeB.parent)
        assertEquals(nodeB, nodeC.parent)

        // Thử layout để chứng minh không bị StackOverflow
        nodeA.layout(500f, 500f)
    }

    @Test
    fun testInvertedAnchorsDefensive() {
        val parent = LayoutNode().apply { width = 400f; height = 400f }
        val child = LayoutNode().apply {
            minWidth = 50f
            minHeight = 50f
            // Neo bị đảo ngược: Left > Right
            anchor.anchorLeft = 1f
            anchor.anchorRight = 0f
            anchor.anchorTop = 1f
            anchor.anchorBottom = 0f
            anchor.isEnabled = true
        }
        parent.addChild(child)

        // Không crash và kích thước không bị âm
        parent.layout(400f, 400f)
        assertTrue(child.width >= 50f)
        assertTrue(child.height >= 50f)
    }

    @Test
    fun testAllHiddenChildrenInPolicies() {
        val row = LayoutNode().apply { policy = RowPolicy(gap = 10f) }
        val col = LayoutNode().apply { policy = ColumnPolicy(gap = 10f) }

        val h1 = LayoutNode().apply { visible = false; minWidth = 100f; minHeight = 100f }
        val h2 = LayoutNode().apply { visible = false; minWidth = 100f; minHeight = 100f }

        row.addChild(h1)
        col.addChild(h2)

        row.layout(200f, 200f)
        col.layout(200f, 200f)

        assertEquals(0f, row.minWidth)
        assertEquals(0f, col.minHeight)
    }

    @Test
    fun testPaddingExceedingContainer() {
        val node = LayoutNode().apply {
            policy = RowPolicy(gap = 5f)
            setPadding(left = 40f, top = 40f, right = 40f, bottom = 40f)
        }
        val child = LayoutNode().apply { minWidth = 20f; minHeight = 20f }
        node.addChild(child)

        // Container kích thước 50x50 nhưng tổng padding là 80x80 -> innerWidth, innerHeight = 0
        node.layout(50f, 50f)

        // Không bị crash hoặc kích thước âm
        assertTrue(node.width >= 50f)
        assertTrue(node.height >= 50f)
        assertTrue(child.width >= 20f)
    }

    @Test
    fun testExtremeFlexStretchRatio() {
        val row = LayoutNode().apply { policy = RowPolicy(gap = 0f) }
        val hugeRatio = LayoutNode().apply {
            minWidth = 0f
            sizeFlagHorizontal = SizeFlag.EXPAND
            stretchRatio = 1_000_000f
        }
        val tinyRatio = LayoutNode().apply {
            minWidth = 0f
            sizeFlagHorizontal = SizeFlag.EXPAND
            stretchRatio = 0.00001f
        }
        row.addChild(hugeRatio)
        row.addChild(tinyRatio)

        row.layout(1000f, 100f)

        // hugeRatio chiếm gần như toàn bộ 1000px, tinyRatio chiếm xấp xỉ 0px
        assertTrue(hugeRatio.width > 999f)
        assertTrue(tinyRatio.width >= 0f)
        assertTrue(hugeRatio.width + tinyRatio.width <= 1000.01f)
    }
}

