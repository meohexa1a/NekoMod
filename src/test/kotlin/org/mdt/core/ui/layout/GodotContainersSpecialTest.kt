package org.mdt.core.ui.layout

import org.mdt.core.ui.node.LayoutNode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * ## GodotContainersSpecialTest [Pillar 4: Exhaustive Flow & Scroll Algorithms Unit Tests]
 *
 * Exhaustively validates FlowRow multi-line wrapping thresholds, line-height calculations,
 * horizontal/vertical line spacing, and native dual-axis scrolling on [LayoutNode].
 */
class GodotContainersSpecialTest {

    // --- FLOW ROW MULTI-LINE WRAPPING ---

    @Test
    fun `FlowRow with all items fitting remains single-line`() {
        val flow = LayoutNode().apply {
            width = 400.0f
            height = 50.0f
            measurePolicy = FlowRowMeasurePolicy(horizontalGap = 10.0f, verticalGap = 10.0f)
        }
        val c1 = createNode(width = 80.0f, height = 30.0f)
        val c2 = createNode(width = 80.0f, height = 30.0f)
        val c3 = createNode(width = 80.0f, height = 30.0f)
        flow.children.addAll(listOf(c1, c2, c3))
        flow.layout()

        c1.assertBounds(expectedX = 0.0f, expectedY = 20.0f, expectedW = 80.0f, expectedH = 30.0f)
        c2.assertBounds(expectedX = 90.0f, expectedY = 20.0f, expectedW = 80.0f, expectedH = 30.0f)
        c3.assertBounds(expectedX = 180.0f, expectedY = 20.0f, expectedW = 80.0f, expectedH = 30.0f)
    }

    @Test
    fun `FlowRow wraps onto 3 lines with varying heights and gaps`() {
        val flow = LayoutNode().apply {
            width = 150.0f
            height = 200.0f
            measurePolicy = FlowRowMeasurePolicy(horizontalGap = 10.0f, verticalGap = 10.0f, alignment = Alignment.TopStart)
        }

        // Line 1: c1(60x30), c2(60x40) -> Total W = 130 <= 150. Line 1 H = max(30, 40) = 40.
        // Line 2: c3(80x25), c4(50x35) -> Total W = 140 <= 150. Line 2 H = max(25, 35) = 35.
        // Line 3: c5(70x20) -> Total W = 70 <= 150. Line 3 H = 20.
        val c1 = createNode(width = 60.0f, height = 30.0f)
        val c2 = createNode(width = 60.0f, height = 40.0f)
        val c3 = createNode(width = 80.0f, height = 25.0f)
        val c4 = createNode(width = 50.0f, height = 35.0f)
        val c5 = createNode(width = 70.0f, height = 20.0f)
        flow.children.addAll(listOf(c1, c2, c3, c4, c5))
        flow.layout()

        // Container H = 200.
        // Line 1 (H=40): Top = 200, Slot Y = 160.
        // Line 2 (H=35): Top = 160 - 10 = 150, Slot Y = 150 - 35 = 115.
        // Line 3 (H=20): Top = 115 - 10 = 105, Slot Y = 105 - 20 = 85.
        c1.assertBounds(expectedX = 0.0f, expectedY = 170.0f, expectedW = 60.0f, expectedH = 30.0f)
        c2.assertBounds(expectedX = 70.0f, expectedY = 160.0f, expectedW = 60.0f, expectedH = 40.0f)

        c3.assertBounds(expectedX = 0.0f, expectedY = 125.0f, expectedW = 80.0f, expectedH = 25.0f)
        c4.assertBounds(expectedX = 90.0f, expectedY = 115.0f, expectedW = 50.0f, expectedH = 35.0f)

        c5.assertBounds(expectedX = 0.0f, expectedY = 85.0f, expectedW = 70.0f, expectedH = 20.0f)
    }

    @Test
    fun `FlowRow line alignment - CENTER centers items horizontally per line`() {
        val flow = LayoutNode().apply {
            width = 200.0f
            height = 100.0f
            measurePolicy = FlowRowMeasurePolicy(
                horizontalGap = 10.0f,
                verticalGap = 10.0f,
                arrangement = Arrangement.Center,
                alignment = Alignment.TopStart
            )
        }
        // Line 1: c1(60), c2(60) -> W = 130. Free = 70. Offset = 35.
        // Line 2: c3(100) -> W = 100. Free = 100. Offset = 50.
        val c1 = createNode(width = 60.0f, height = 30.0f)
        val c2 = createNode(width = 60.0f, height = 30.0f)
        val c3 = createNode(width = 100.0f, height = 30.0f)
        flow.children.addAll(listOf(c1, c2, c3))
        flow.layout()

        c1.assertBounds(expectedX = 35.0f, expectedY = 70.0f, expectedW = 60.0f, expectedH = 30.0f)
        c2.assertBounds(expectedX = 105.0f, expectedY = 70.0f, expectedW = 60.0f, expectedH = 30.0f)
        c3.assertBounds(expectedX = 50.0f, expectedY = 30.0f, expectedW = 100.0f, expectedH = 30.0f)
    }

    @Test
    fun `FlowRow with hidden items skips hidden nodes in wrapping calculations`() {
        val flow = LayoutNode().apply {
            width = 150.0f
            height = 80.0f
            measurePolicy = FlowRowMeasurePolicy(horizontalGap = 10.0f, verticalGap = 10.0f, alignment = Alignment.TopStart)
        }
        val c1 = createNode(width = 60.0f, height = 30.0f)
        val hiddenHuge = createNode(width = 200.0f, height = 100.0f, visible = false)
        val c2 = createNode(width = 60.0f, height = 30.0f)
        flow.children.addAll(listOf(c1, hiddenHuge, c2))
        flow.layout()

        // Only c1 and c2 are visible -> 60 + 10 + 60 = 130 <= 150 (Both stay on Line 1!)
        c1.assertBounds(expectedX = 0.0f, expectedY = 50.0f, expectedW = 60.0f, expectedH = 30.0f)
        c2.assertBounds(expectedX = 70.0f, expectedY = 50.0f, expectedW = 60.0f, expectedH = 30.0f)
    }

    // --- NATIVE SCROLL RANGE (DUAL-AXIS) ---

    @Test
    fun `Scrollable LayoutNode calculates horizontal and vertical scroll range`() {
        val scrollBox = LayoutNode().apply {
            width = 200.0f
            height = 150.0f
            scrollable = true
            enableHorizontalScroll = true
            enableVerticalScroll = true
            measurePolicy = ColumnMeasurePolicy(gap = 10.0f)
        }

        // Add 5 items with width 300 (exceeds 200) and height 50 (5*50 + 4*10 = 290, exceeds 150)
        repeat(5) { idx ->
            scrollBox.children.add(createNode(id = "item$idx", width = 300.0f, height = 50.0f, minWidth = 300.0f, minHeight = 50.0f))
        }

        scrollBox.layout()

        // Content width = 300, Available W = 200 -> maxScrollX = 100
        // Content height = 290, Available H = 150 -> maxScrollY = 140
        assertEquals(100.0f, scrollBox.maxScrollX, 0.01f)
        assertEquals(140.0f, scrollBox.maxScrollY, 0.01f)
    }

    @Test
    fun `Scrollable LayoutNode clamps scroll positions within limits`() {
        val scrollBox = createColumn(width = 200.0f, height = 100.0f).apply {
            scrollable = true
            scrollX = 500.0f
            scrollY = 500.0f
        }
        val child = createNode(width = 200.0f, height = 250.0f, minHeight = 250.0f)
        scrollBox.children.add(child)

        scrollBox.layout()

        // Content H = 250, Container H = 100 -> maxScrollY = 150.
        // scrollY was initialized to 500 -> must clamp to 150!
        assertEquals(150.0f, scrollBox.maxScrollY, 0.01f)
        assertEquals(150.0f, scrollBox.scrollY, 0.01f)
    }
}
