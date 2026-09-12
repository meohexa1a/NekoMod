package org.hubdustry.libs.layout

import org.hubdustry.libs.layout.policies.ColumnPolicy
import org.hubdustry.libs.layout.policies.RowPolicy
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class ArchitectureInvariantsTest {

    // 1. Cấu trúc Cây & Đóng gói (Tree Integrity & Encapsulation)
    @Test
    fun testTreeEncapsulationAndCyclePrevention() {
        val root = LayoutNode()
        val child1 = LayoutNode()
        val child2 = LayoutNode()

        // Thêm node con bình thường
        root.addChild(child1)
        root.addChild(child2)

        assertEquals(2, root.children.size)
        assertSame(root, child1.parent)
        assertSame(root, child2.parent)

        // Chặn chu trình self-reference
        root.addChild(root)
        assertEquals(2, root.children.size)
        assertNull(root.parent)

        // Chặn thêm lại cùng một con (idempotent)
        root.addChild(child1)
        assertEquals(2, root.children.size)

        // Chặn chu trình lặp tổ tiên A -> B -> C -> A
        val grandChild = LayoutNode()
        child1.addChild(grandChild)
        assertSame(child1, grandChild.parent)
        assertTrue(child1.isAncestorOf(grandChild))
        assertTrue(root.isAncestorOf(grandChild))

        grandChild.addChild(root) // Thử tạo chu trình
        assertNull(root.parent) // root không bị gán parent là grandChild
        assertEquals(0, grandChild.children.size)

        // Tự động detach khỏi cha cũ khi sang cha mới
        val newParent = LayoutNode()
        newParent.addChild(child1)

        assertSame(newParent, child1.parent)
        assertTrue(newParent.children.contains(child1))
        assertFalse(root.children.contains(child1))
        assertEquals(1, root.children.size)

        // Gỡ con qua removeChild
        val removed = newParent.removeChild(child1)
        assertTrue(removed)
        assertNull(child1.parent)
        assertEquals(0, newParent.children.size)

        // clearChildren
        root.addChild(child1)
        assertEquals(2, root.children.size)
        root.clearChildren()
        assertEquals(0, root.children.size)
        assertNull(child1.parent)
        assertNull(child2.parent)
    }

    // 2. Gateway Sanitization
    @Test
    fun testGatewaySanitizationConstraints() {
        val node = LayoutNode()

        // Số âm và NaN trong minWidth / minHeight được ép về 0f
        node.minWidth = -50f
        assertEquals(0f, node.minWidth)
        node.minWidth = Float.NaN
        assertEquals(0f, node.minWidth)

        node.minHeight = -25f
        assertEquals(0f, node.minHeight)
        node.minHeight = Float.NaN
        assertEquals(0f, node.minHeight)

        // minWidth vượt quá maxWidth -> tự động đẩy maxWidth = minWidth
        node.maxWidth = 100f
        node.minWidth = 150f
        assertEquals(150f, node.minWidth)
        assertEquals(150f, node.maxWidth)

        // maxWidth nhỏ hơn minWidth -> tự động kẹp về minWidth
        node.maxWidth = 80f
        assertEquals(150f, node.maxWidth)

        node.maxWidth = Float.NaN
        assertEquals(150f, node.maxWidth)

        // Tương tự với minHeight / maxHeight
        node.maxHeight = 200f
        node.minHeight = 250f
        assertEquals(250f, node.minHeight)
        assertEquals(250f, node.maxHeight)

        node.maxHeight = 100f
        assertEquals(250f, node.maxHeight)

        // Padding không nhận số âm và NaN
        node.paddingLeft = -10f
        assertEquals(0f, node.paddingLeft)
        node.paddingLeft = Float.NaN
        assertEquals(0f, node.paddingLeft)

        node.paddingTop = -5f
        assertEquals(0f, node.paddingTop)

        node.paddingRight = Float.NaN
        assertEquals(0f, node.paddingRight)

        node.paddingBottom = -20f
        assertEquals(0f, node.paddingBottom)

        // stretchRatio không nhận số âm và NaN
        node.stretchRatio = -2f
        assertEquals(0f, node.stretchRatio)
        node.stretchRatio = Float.NaN
        assertEquals(0f, node.stretchRatio)

        // Tọa độ và kích thước
        node.x = Float.NaN
        assertEquals(0f, node.x)
        node.y = Float.NaN
        assertEquals(0f, node.y)

        node.width = -100f
        assertEquals(0f, node.width)
        node.width = Float.NaN
        assertEquals(0f, node.width)

        node.height = -50f
        assertEquals(0f, node.height)
        node.height = Float.NaN
        assertEquals(0f, node.height)
    }

    // 3. Trừu tượng hóa 1D Axis Projections
    @Test
    fun testAxisProjections() {
        val node = LayoutNode().apply {
            minWidth = 100f
            maxWidth = 300f
            minHeight = 50f
            maxHeight = 150f
            sizeFlagHorizontal = SizeFlag.EXPAND
            sizeFlagVertical = SizeFlag.SHRINK
            alignHorizontal = Alignment.CENTER
            alignVertical = Alignment.END
            paddingLeft = 10f
            paddingRight = 15f
            paddingTop = 20f
            paddingBottom = 25f
        }

        // HORIZONTAL: main là X, cross là Y
        assertEquals(100f, node.minSize(Orientation.HORIZONTAL))
        assertEquals(300f, node.maxSize(Orientation.HORIZONTAL))
        assertEquals(SizeFlag.EXPAND, node.sizeFlag(Orientation.HORIZONTAL))
        assertEquals(Alignment.CENTER, node.alignment(Orientation.HORIZONTAL))
        assertEquals(25f, node.paddingMain(Orientation.HORIZONTAL)) // 10 + 15
        assertEquals(45f, node.paddingCross(Orientation.HORIZONTAL)) // 20 + 25

        // VERTICAL: main là Y, cross là X
        assertEquals(50f, node.minSize(Orientation.VERTICAL))
        assertEquals(150f, node.maxSize(Orientation.VERTICAL))
        assertEquals(SizeFlag.SHRINK, node.sizeFlag(Orientation.VERTICAL))
        assertEquals(Alignment.END, node.alignment(Orientation.VERTICAL))
        assertEquals(45f, node.paddingMain(Orientation.VERTICAL)) // 20 + 25
        assertEquals(25f, node.paddingCross(Orientation.VERTICAL)) // 10 + 15

        // Orientation methods
        assertEquals(Orientation.VERTICAL, Orientation.HORIZONTAL.cross())
        assertEquals(Orientation.HORIZONTAL, Orientation.VERTICAL.cross())
        assertEquals(10f, Orientation.HORIZONTAL.main(10f, 20f))
        assertEquals(20f, Orientation.HORIZONTAL.cross(10f, 20f))
        assertEquals(20f, Orientation.VERTICAL.main(10f, 20f))
        assertEquals(10f, Orientation.VERTICAL.cross(10f, 20f))

        // setMinSizeByAxis
        val dynamicNode = LayoutNode()
        dynamicNode.setMinSizeByAxis(Orientation.HORIZONTAL, main = 70f, cross = 40f)
        assertEquals(70f, dynamicNode.minWidth)
        assertEquals(40f, dynamicNode.minHeight)

        dynamicNode.setMinSizeByAxis(Orientation.VERTICAL, main = 90f, cross = 80f)
        assertEquals(80f, dynamicNode.minWidth) // cross of vertical is width
        assertEquals(90f, dynamicNode.minHeight) // main of vertical is height
    }

    // 4. Tự trị Đệ quy (Self-Arranging Cascade)
    @Test
    fun testSelfArrangingCascadeThroughHierarchy() {
        val root = LayoutNode().apply {
            policy = RowPolicy(gap = 10f)
            paddingLeft = 5f
            paddingTop = 5f
            paddingRight = 5f
            paddingBottom = 5f
        }

        val child1 = LayoutNode().apply {
            policy = ColumnPolicy(gap = 5f)
            sizeFlagHorizontal = SizeFlag.EXPAND
            stretchRatio = 1f
        }
        val child2 = LayoutNode().apply {
            minWidth = 50f
            minHeight = 50f
            sizeFlagHorizontal = SizeFlag.SHRINK
        }
        root.addChild(child1)
        root.addChild(child2)

        val grandChild1 = LayoutNode().apply {
            minHeight = 30f
            sizeFlagVertical = SizeFlag.EXPAND
        }
        val grandChild2 = LayoutNode().apply {
            minHeight = 40f
            sizeFlagVertical = SizeFlag.SHRINK
        }
        child1.addChild(grandChild1)
        child1.addChild(grandChild2)

        // Gọi arrange trên root -> tự động cascade xuống child1, child2 và grandChild1, grandChild2
        root.arrange(newX = 0f, newY = 0f, newWidth = 200f, newHeight = 100f)

        // Root: innerWidth = 200 - 10 = 190, innerHeight = 100 - 10 = 90
        // Child2: minWidth = 50
        // Gap = 10
        // Child1: receives remaining main = 190 - 50 - 10 = 130
        assertEquals(5f, child1.x, 0.001f)
        assertEquals(5f, child1.y, 0.001f)
        assertEquals(130f, child1.width, 0.001f)
        assertEquals(90f, child1.height, 0.001f)

        assertEquals(145f, child2.x, 0.001f) // 5 + 130 + 10 = 145
        assertEquals(5f, child2.y, 0.001f)
        assertEquals(50f, child2.width, 0.001f)
        assertEquals(90f, child2.height, 0.001f)

        // Child1 inner bounds: innerX = 0, innerY = 0, innerW = 130, innerH = 90
        // GrandChild2 minHeight = 40, grandChild1 minHeight = 30
        // Gap = 5 -> Total min = 75. Free space = 90 - 75 = 15
        // GrandChild1 receives 30 + 15 = 45 height (local coords relative to child1)
        assertEquals(0f, grandChild1.x, 0.001f)
        assertEquals(0f, grandChild1.y, 0.001f)
        assertEquals(130f, grandChild1.width, 0.001f)
        assertEquals(45f, grandChild1.height, 0.001f)

        // GrandChild2 starts at 0 (start) + 45 (slot1) + 5 (gap) = 50
        assertEquals(0f, grandChild2.x, 0.001f)
        assertEquals(50f, grandChild2.y, 0.001f)
        assertEquals(130f, grandChild2.width, 0.001f)
        assertEquals(40f, grandChild2.height, 0.001f)
    }

    @Test
    fun testArrangeAxisHorizontalAndVertical() {
        val nodeH = LayoutNode()
        nodeH.arrangeAxis(Orientation.HORIZONTAL, mainPos = 10f, crossPos = 20f, mainSize = 100f, crossSize = 50f)
        assertEquals(10f, nodeH.x)
        assertEquals(20f, nodeH.y)
        assertEquals(100f, nodeH.width)
        assertEquals(50f, nodeH.height)

        val nodeV = LayoutNode()
        nodeV.arrangeAxis(Orientation.VERTICAL, mainPos = 30f, crossPos = 40f, mainSize = 120f, crossSize = 60f)
        assertEquals(40f, nodeV.x) // crossPos is X
        assertEquals(30f, nodeV.y) // mainPos is Y
        assertEquals(60f, nodeV.width) // crossSize is width
        assertEquals(120f, nodeV.height) // mainSize is height
    }
}
