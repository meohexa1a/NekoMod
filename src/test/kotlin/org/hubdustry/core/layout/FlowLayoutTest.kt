package org.hubdustry.core.layout

import org.hubdustry.core.layout.policies.FlowColumnPolicy
import org.hubdustry.core.layout.policies.FlowRowPolicy
import kotlin.test.Test
import kotlin.test.assertEquals

class FlowLayoutTest {

    @Test
    fun testFlowRowBasicWrap() {
        val root = LayoutNode().apply {
            policy = FlowRowPolicy(horizontalGap = 0f, verticalGap = 0f)
        }
        val c1 = LayoutNode().apply { minWidth = 60f; minHeight = 20f; sizeFlagHorizontal = SizeFlag.SHRINK }
        val c2 = LayoutNode().apply { minWidth = 60f; minHeight = 20f; sizeFlagHorizontal = SizeFlag.SHRINK }
        val c3 = LayoutNode().apply { minWidth = 60f; minHeight = 20f; sizeFlagHorizontal = SizeFlag.SHRINK }

        root.addChild(c1)
        root.addChild(c2)
        root.addChild(c3)

        // Khung rộng 100, mỗi con rộng 60 -> Mỗi dòng chỉ chứa được 1 con
        root.layout(100f, 200f)

        assertEquals(0f, c1.x, 0.001f)
        assertEquals(0f, c1.y, 0.001f)
        assertEquals(60f, c1.width, 0.001f)
        assertEquals(20f, c1.height, 0.001f)

        assertEquals(0f, c2.x, 0.001f)
        assertEquals(20f, c2.y, 0.001f)
        assertEquals(60f, c2.width, 0.001f)
        assertEquals(20f, c2.height, 0.001f)

        assertEquals(0f, c3.x, 0.001f)
        assertEquals(40f, c3.y, 0.001f)
        assertEquals(60f, c3.width, 0.001f)
        assertEquals(20f, c3.height, 0.001f)
    }

    @Test
    fun testFlowRowSingleLineWhenFits() {
        val root = LayoutNode().apply {
            policy = FlowRowPolicy(horizontalGap = 0f, verticalGap = 0f)
        }
        val c1 = LayoutNode().apply { minWidth = 30f; minHeight = 20f; sizeFlagHorizontal = SizeFlag.SHRINK }
        val c2 = LayoutNode().apply { minWidth = 30f; minHeight = 20f; sizeFlagHorizontal = SizeFlag.SHRINK }
        val c3 = LayoutNode().apply { minWidth = 30f; minHeight = 20f; sizeFlagHorizontal = SizeFlag.SHRINK }

        root.addChild(c1)
        root.addChild(c2)
        root.addChild(c3)

        root.layout(100f, 200f)

        // Cả 3 con tổng rộng 90 <= 100 -> Nằm trên cùng 1 dòng
        assertEquals(0f, c1.x, 0.001f)
        assertEquals(0f, c1.y, 0.001f)

        assertEquals(30f, c2.x, 0.001f)
        assertEquals(0f, c2.y, 0.001f)

        assertEquals(60f, c3.x, 0.001f)
        assertEquals(0f, c3.y, 0.001f)
    }

    @Test
    fun testFlowRowWithHorizontalAndVerticalGaps() {
        val root = LayoutNode().apply {
            policy = FlowRowPolicy(horizontalGap = 10f, verticalGap = 15f)
        }
        val c1 = LayoutNode().apply { minWidth = 40f; minHeight = 20f; sizeFlagHorizontal = SizeFlag.SHRINK }
        val c2 = LayoutNode().apply { minWidth = 40f; minHeight = 20f; sizeFlagHorizontal = SizeFlag.SHRINK }
        val c3 = LayoutNode().apply { minWidth = 40f; minHeight = 20f; sizeFlagHorizontal = SizeFlag.SHRINK }

        root.addChild(c1)
        root.addChild(c2)
        root.addChild(c3)

        // c1 (40) + gap (10) + c2 (40) = 90 <= 100 -> c1 & c2 cùng dòng 1
        // c3 (40) không vừa 90 + 10 + 40 = 140 > 100 -> c3 xuống dòng 2
        root.layout(100f, 200f)

        assertEquals(0f, c1.x, 0.001f)
        assertEquals(0f, c1.y, 0.001f)

        assertEquals(50f, c2.x, 0.001f)
        assertEquals(0f, c2.y, 0.001f)

        assertEquals(0f, c3.x, 0.001f)
        assertEquals(35f, c3.y, 0.001f) // line1 height (20) + verticalGap (15) = 35
    }

    @Test
    fun testFlowRowPerLineCrossAlignment() {
        val root = LayoutNode().apply {
            policy = FlowRowPolicy(horizontalGap = 0f, verticalGap = 0f)
        }
        val c1 = LayoutNode().apply {
            minWidth = 50f
            minHeight = 20f
            sizeFlagHorizontal = SizeFlag.SHRINK
            sizeFlagVertical = SizeFlag.SHRINK
            alignVertical = Alignment.CENTER
        }
        val c2 = LayoutNode().apply {
            minWidth = 50f
            minHeight = 60f
            sizeFlagHorizontal = SizeFlag.SHRINK
            sizeFlagVertical = SizeFlag.SHRINK
        }

        root.addChild(c1)
        root.addChild(c2)

        root.layout(120f, 100f)

        // Cả 2 con nằm cùng 1 dòng, chiều cao dòng = max(20, 60) = 60.
        // c1 căn CENTER: y = (60 - 20) * 0.5 = 20.
        // c2 căn START (mặc định): y = 0.
        assertEquals(20f, c1.y, 0.001f)
        assertEquals(0f, c2.y, 0.001f)
        assertEquals(0f, c1.x, 0.001f)
        assertEquals(50f, c2.x, 0.001f)
    }

    @Test
    fun testFlowColumnBasicWrap() {
        val root = LayoutNode().apply {
            policy = FlowColumnPolicy(verticalGap = 5f, horizontalGap = 12f)
        }
        val c1 = LayoutNode().apply { minWidth = 30f; minHeight = 60f; sizeFlagVertical = SizeFlag.SHRINK }
        val c2 = LayoutNode().apply { minWidth = 40f; minHeight = 60f; sizeFlagVertical = SizeFlag.SHRINK }

        root.addChild(c1)
        root.addChild(c2)

        // Khung cao 100, c1 cao 60. c1 (60) + gap (5) + c2 (60) = 125 > 100 -> c2 ngắt sang cột 2
        root.layout(200f, 100f)

        assertEquals(0f, c1.x, 0.001f)
        assertEquals(0f, c1.y, 0.001f)

        // Cột 2: x = col1 width (30) + horizontalGap (12) = 42
        assertEquals(42f, c2.x, 0.001f)
        assertEquals(0f, c2.y, 0.001f)
    }

    @Test
    fun testFlowRowOversizedChildDoesNotInfiniteLoop() {
        val root = LayoutNode().apply {
            policy = FlowRowPolicy()
        }
        val c1 = LayoutNode().apply { minWidth = 150f; minHeight = 20f; sizeFlagHorizontal = SizeFlag.SHRINK }
        val c2 = LayoutNode().apply { minWidth = 30f; minHeight = 20f; sizeFlagHorizontal = SizeFlag.SHRINK }

        root.addChild(c1)
        root.addChild(c2)

        // Khung rộng 100, nhưng c1 rộng 150 > 100.
        // c1 vẫn chiếm 1 dòng đầu tiên, c2 xuống dòng thứ hai mà không bị treo lặp
        root.layout(100f, 200f)

        assertEquals(0f, c1.x, 0.001f)
        assertEquals(0f, c1.y, 0.001f)

        assertEquals(0f, c2.x, 0.001f)
        assertEquals(20f, c2.y, 0.001f)
    }

    @Test
    fun testFlowRowPaddingAndMargin() {
        val root = LayoutNode().apply {
            policy = FlowRowPolicy(horizontalGap = 10f, verticalGap = 10f)
            setPadding(10f)
        }
        val c1 = LayoutNode().apply {
            minWidth = 30f
            minHeight = 20f
            setMargin(5f)
            sizeFlagHorizontal = SizeFlag.SHRINK
        }

        root.addChild(c1)

        root.layout(100f, 100f)

        // paddingLeft (10) + marginLeft (5) = 15
        assertEquals(15f, c1.x, 0.001f)
        // paddingTop (10) + marginTop (5) = 15
        assertEquals(15f, c1.y, 0.001f)
    }

    @Test
    fun testFlowRowIntrinsicMinSize() {
        val root = LayoutNode().apply {
            policy = FlowRowPolicy()
            setPadding(10f)
        }
        val c1 = LayoutNode().apply { minWidth = 40f; minHeight = 30f }
        val c2 = LayoutNode().apply { minWidth = 70f; minHeight = 20f }

        root.addChild(c1)
        root.addChild(c2)

        root.policy.computeMinSize(root)

        // minWidth của FlowRow = max(child minWidth) + padding = 70 + 20 = 90
        assertEquals(90f, root.minWidth, 0.001f)
        // minHeight = max(child minHeight) + padding = 30 + 20 = 50
        assertEquals(50f, root.minHeight, 0.001f)
    }

    @Test
    fun testFlowRowWithAnchoredChildIgnoredInFlow() {
        val root = LayoutNode().apply {
            policy = FlowRowPolicy()
        }
        val flowChild = LayoutNode().apply { minWidth = 40f; minHeight = 20f; sizeFlagHorizontal = SizeFlag.SHRINK }
        val anchoredChild = LayoutNode().apply {
            minWidth = 10f
            minHeight = 10f
            anchor.setPreset(AnchorPreset.TOP_RIGHT)
        }

        root.addChild(flowChild)
        root.addChild(anchoredChild)

        root.layout(100f, 100f)

        // Flow child nằm ở (0, 0)
        assertEquals(0f, flowChild.x, 0.001f)
        assertEquals(0f, flowChild.y, 0.001f)

        // Anchored child được giải neo ở góc trên phải (100 - 10 = 90, 0)
        assertEquals(90f, anchoredChild.x, 0.001f)
        assertEquals(0f, anchoredChild.y, 0.001f)
    }
}
