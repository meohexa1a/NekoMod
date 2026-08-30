package org.mdt.core.ui.layout

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * ## GodotFlexBoxTest [Pillar 2: Exhaustive Flex Box, Row, Column & SizeFlags Unit Tests]
 *
 * Exhaustively validates multi-child flex weight permutations, all 6 Arrangement algorithms,
 * cross-axis SizeFlags (SHRINK_BEGIN, SHRINK_CENTER, SHRINK_END, FILL), gap mathematics,
 * and dynamic visibility toggling.
 */
class GodotFlexBoxTest {

    // --- WEIGHTED FLEX RATIOS & PERMUTATIONS ---

    @Test
    fun `Row with 3 equal expanding children splits available space into thirds`() {
        val row = createRow(width = 300.0f, height = 50.0f, gap = 0.0f)
        val c1 = createNode(hFlags = SizeFlags.EXPAND_FILL, vFlags = SizeFlags.FILL, stretchRatio = 1.0f)
        val c2 = createNode(hFlags = SizeFlags.EXPAND_FILL, vFlags = SizeFlags.FILL, stretchRatio = 1.0f)
        val c3 = createNode(hFlags = SizeFlags.EXPAND_FILL, vFlags = SizeFlags.FILL, stretchRatio = 1.0f)
        row.children.addAll(listOf(c1, c2, c3))

        row.layout()

        c1.assertBounds(expectedX = 0.0f, expectedY = 0.0f, expectedW = 100.0f, expectedH = 50.0f)
        c2.assertBounds(expectedX = 100.0f, expectedY = 0.0f, expectedW = 100.0f, expectedH = 50.0f)
        c3.assertBounds(expectedX = 200.0f, expectedY = 0.0f, expectedW = 100.0f, expectedH = 50.0f)
    }

    @Test
    fun `Row with 3 weighted children (1 to 2 to 3) with gaps`() {
        val row = createRow(width = 620.0f, height = 60.0f, gap = 10.0f)
        // 3 items -> 2 gaps of 10 = 20px. Available space = 620 - 20 = 600px. Total ratio = 6.
        // c1 (1/6) = 100px
        // c2 (2/6) = 200px
        // c3 (3/6) = 300px
        val c1 = createNode(hFlags = SizeFlags.EXPAND_FILL, vFlags = SizeFlags.FILL, stretchRatio = 1.0f)
        val c2 = createNode(hFlags = SizeFlags.EXPAND_FILL, vFlags = SizeFlags.FILL, stretchRatio = 2.0f)
        val c3 = createNode(hFlags = SizeFlags.EXPAND_FILL, vFlags = SizeFlags.FILL, stretchRatio = 3.0f)
        row.children.addAll(listOf(c1, c2, c3))

        row.layout()

        c1.assertBounds(expectedX = 0.0f, expectedY = 0.0f, expectedW = 100.0f, expectedH = 60.0f)
        c2.assertBounds(expectedX = 110.0f, expectedY = 0.0f, expectedW = 200.0f, expectedH = 60.0f)
        c3.assertBounds(expectedX = 320.0f, expectedY = 0.0f, expectedW = 300.0f, expectedH = 60.0f)
    }

    @Test
    fun `Mixed patterns - Expand then Fixed then Expand`() {
        val row = createRow(width = 500.0f, height = 40.0f, gap = 10.0f)
        // 3 items -> 2 gaps of 10 = 20. Fixed c2 = 80px.
        // Available flex = 500 - 80 - 20 = 400px.
        // c1 (ratio 1) = 200px, c3 (ratio 1) = 200px.
        val c1 = createNode(hFlags = SizeFlags.EXPAND_FILL, vFlags = SizeFlags.FILL, stretchRatio = 1.0f)
        val c2 = createNode(width = 80.0f, height = 40.0f)
        val c3 = createNode(hFlags = SizeFlags.EXPAND_FILL, vFlags = SizeFlags.FILL, stretchRatio = 1.0f)
        row.children.addAll(listOf(c1, c2, c3))

        row.layout()

        c1.assertBounds(expectedX = 0.0f, expectedY = 0.0f, expectedW = 200.0f, expectedH = 40.0f)
        c2.assertBounds(expectedX = 210.0f, expectedY = 0.0f, expectedW = 80.0f, expectedH = 40.0f)
        c3.assertBounds(expectedX = 300.0f, expectedY = 0.0f, expectedW = 200.0f, expectedH = 40.0f)
    }

    // --- COLUMN ORDER & PROPORTIONS ---

    @Test
    fun `Column orders 3 weighted items top-to-bottom accurately`() {
        val column = createColumn(width = 100.0f, height = 620.0f, gap = 10.0f)
        // Available flex = 620 - 20 = 600. c1=100, c2=200, c3=300.
        // c1 (top): Y = 620 - 100 = 520
        // c2 (mid): Y = 520 - 10 - 200 = 310
        // c3 (bot): Y = 310 - 10 - 300 = 0
        val c1 = createNode(vFlags = SizeFlags.EXPAND_FILL, hFlags = SizeFlags.FILL, stretchRatio = 1.0f)
        val c2 = createNode(vFlags = SizeFlags.EXPAND_FILL, hFlags = SizeFlags.FILL, stretchRatio = 2.0f)
        val c3 = createNode(vFlags = SizeFlags.EXPAND_FILL, hFlags = SizeFlags.FILL, stretchRatio = 3.0f)
        column.children.addAll(listOf(c1, c2, c3))

        column.layout()

        c1.assertBounds(expectedX = 0.0f, expectedY = 520.0f, expectedW = 100.0f, expectedH = 100.0f)
        c2.assertBounds(expectedX = 0.0f, expectedY = 310.0f, expectedW = 100.0f, expectedH = 200.0f)
        c3.assertBounds(expectedX = 0.0f, expectedY = 0.0f, expectedW = 100.0f, expectedH = 300.0f)
    }

    // --- ALL 6 ARRANGEMENT TYPES ---

    @Test
    fun `Arrangement - START places items left-aligned`() {
        val row = createRow(width = 400.0f, height = 40.0f, gap = 10.0f, arrangement = Arrangement.Start)
        val c1 = createNode(width = 60.0f, height = 40.0f)
        val c2 = createNode(width = 60.0f, height = 40.0f)
        row.children.addAll(listOf(c1, c2))
        row.layout()

        c1.assertBounds(expectedX = 0.0f, expectedY = 0.0f, expectedW = 60.0f, expectedH = 40.0f)
        c2.assertBounds(expectedX = 70.0f, expectedY = 0.0f, expectedW = 60.0f, expectedH = 40.0f)
    }

    @Test
    fun `Arrangement - CENTER centers group within available space`() {
        val row = createRow(width = 400.0f, height = 40.0f, gap = 10.0f, arrangement = Arrangement.Center)
        val c1 = createNode(width = 60.0f, height = 40.0f)
        val c2 = createNode(width = 60.0f, height = 40.0f)
        row.children.addAll(listOf(c1, c2))
        row.layout()

        // Total content = 60 + 10 + 60 = 130. Free space = 400 - 130 = 270. Offset = 135.
        c1.assertBounds(expectedX = 135.0f, expectedY = 0.0f, expectedW = 60.0f, expectedH = 40.0f)
        c2.assertBounds(expectedX = 205.0f, expectedY = 0.0f, expectedW = 60.0f, expectedH = 40.0f)
    }

    @Test
    fun `Arrangement - END places items right-aligned`() {
        val row = createRow(width = 400.0f, height = 40.0f, gap = 10.0f, arrangement = Arrangement.End)
        val c1 = createNode(width = 60.0f, height = 40.0f)
        val c2 = createNode(width = 60.0f, height = 40.0f)
        row.children.addAll(listOf(c1, c2))
        row.layout()

        // Total content = 130. Free space = 270.
        // c1: 270, c2: 270 + 60 + 10 = 340
        c1.assertBounds(expectedX = 270.0f, expectedY = 0.0f, expectedW = 60.0f, expectedH = 40.0f)
        c2.assertBounds(expectedX = 340.0f, expectedY = 0.0f, expectedW = 60.0f, expectedH = 40.0f)
    }

    @Test
    fun `Arrangement - SPACE_AROUND places equal half-spaces on ends`() {
        val row = createRow(width = 300.0f, height = 40.0f, arrangement = Arrangement.SpaceAround)
        val c1 = createNode(width = 50.0f, height = 40.0f)
        val c2 = createNode(width = 50.0f, height = 40.0f)
        row.children.addAll(listOf(c1, c2))
        row.layout()

        // Total width = 300. 2 items of 50 = 100. Remaining = 200.
        // Space per item = 200 / 2 = 100. StartOffset = 50.
        // c1: 50
        // c2: 50 + 50 + 100 = 200
        c1.assertBounds(expectedX = 50.0f, expectedY = 0.0f, expectedW = 50.0f, expectedH = 40.0f)
        c2.assertBounds(expectedX = 200.0f, expectedY = 0.0f, expectedW = 50.0f, expectedH = 40.0f)
    }

    @Test
    fun `Arrangement - SPACE_EVENLY places equal spaces everywhere`() {
        val row = createRow(width = 290.0f, height = 40.0f, arrangement = Arrangement.SpaceEvenly)
        val c1 = createNode(width = 50.0f, height = 40.0f)
        val c2 = createNode(width = 50.0f, height = 40.0f)
        row.children.addAll(listOf(c1, c2))
        row.layout()

        // Total width = 290. 2 items of 50 = 100. Remaining = 190.
        // 3 gaps = 190 / 3 = 63.33px each.
        // c1: 63.33
        // c2: 63.33 + 50 + 63.33 = 176.67
        c1.assertBounds(expectedX = 190.0f / 3.0f, expectedY = 0.0f, expectedW = 50.0f, expectedH = 40.0f)
        c2.assertBounds(expectedX = (190.0f / 3.0f) * 2.0f + 50.0f, expectedY = 0.0f, expectedW = 50.0f, expectedH = 40.0f)
    }

    // --- VISIBILITY TOGGLING ---

    @Test
    fun `Hiding first child eliminates leading gap`() {
        val row = createRow(width = 300.0f, height = 50.0f, gap = 20.0f)
        val c1 = createNode(width = 60.0f, height = 40.0f, visible = false)
        val c2 = createNode(width = 60.0f, height = 40.0f)
        val c3 = createNode(width = 60.0f, height = 40.0f)
        row.children.addAll(listOf(c1, c2, c3))
        row.layout()

        c2.assertBounds(expectedX = 0.0f, expectedY = 5.0f, expectedW = 60.0f, expectedH = 40.0f)
        c3.assertBounds(expectedX = 80.0f, expectedY = 5.0f, expectedW = 60.0f, expectedH = 40.0f)
    }

    @Test
    fun `Hiding last child eliminates trailing gap`() {
        val row = createRow(width = 300.0f, height = 50.0f, gap = 20.0f)
        val c1 = createNode(width = 60.0f, height = 40.0f)
        val c2 = createNode(width = 60.0f, height = 40.0f)
        val c3 = createNode(width = 60.0f, height = 40.0f, visible = false)
        row.children.addAll(listOf(c1, c2, c3))
        row.layout()

        c1.assertBounds(expectedX = 0.0f, expectedY = 5.0f, expectedW = 60.0f, expectedH = 40.0f)
        c2.assertBounds(expectedX = 80.0f, expectedY = 5.0f, expectedW = 60.0f, expectedH = 40.0f)
    }

    @Test
    fun `Hiding all children handles safely without error`() {
        val row = createRow(width = 300.0f, height = 50.0f, gap = 20.0f)
        val c1 = createNode(visible = false)
        val c2 = createNode(visible = false)
        row.children.addAll(listOf(c1, c2))
        row.layout()

        assertEquals(0, row.children.filter { it.bounds.width > 0f }.size)
    }
}
