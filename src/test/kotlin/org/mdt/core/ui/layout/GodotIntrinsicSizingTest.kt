package org.mdt.core.ui.layout

import org.mdt.core.ui.node.CanvasNode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * ## GodotIntrinsicSizingTest [Pillar 3: Exhaustive 2-Pass Intrinsic Sizing Unit Tests]
 *
 * Exhaustively validates bottom-up intrinsic sizing (Hug Content) for Box, Row, Column,
 * asymmetric inward padding and outward margins, rigid min-dimension preservation,
 * deep 4-level nested hierarchies, and layout invalidation.
 */
class GodotIntrinsicSizingTest {

    // --- BOX HUG CONTENT ---

    @Test
    fun `Box with 0 children hugs padding dimensions`() {
        val box = createBox(padL = 10.0f, padT = 15.0f, padR = 20.0f, padB = 25.0f)
        assertEquals(30.0f, box.getPrefWidth())
        assertEquals(40.0f, box.getPrefHeight())
    }

    @Test
    fun `Box with 3 children hugs the bounding box of largest child plus padding`() {
        val box = createBox(padL = 12.0f, padT = 16.0f, padR = 14.0f, padB = 18.0f)
        val c1 = createNode(minWidth = 100.0f, minHeight = 40.0f)
        val c2 = createNode(minWidth = 250.0f, minHeight = 60.0f)
        val c3 = createNode(minWidth = 180.0f, minHeight = 120.0f)
        box.children.addAll(listOf(c1, c2, c3))

        // Max width = 250 + 12 + 14 = 276
        // Max height = 120 + 16 + 18 = 154
        assertEquals(276.0f, box.getPrefWidth())
        assertEquals(154.0f, box.getPrefHeight())
    }

    // --- ROW & COLUMN HUG CONTENT ---

    @Test
    fun `Row with 4 children hugs sum of widths, 3 gaps, and asymmetric padding`() {
        val row = createRow(gap = 15.0f, padL = 10.0f, padR = 20.0f, padT = 5.0f, padB = 15.0f)
        val c1 = createNode(minWidth = 40.0f, minHeight = 20.0f)
        val c2 = createNode(minWidth = 60.0f, minHeight = 45.0f)
        val c3 = createNode(minWidth = 80.0f, minHeight = 30.0f)
        val c4 = createNode(minWidth = 50.0f, minHeight = 15.0f)
        row.children.addAll(listOf(c1, c2, c3, c4))

        // Sum widths = 40 + 60 + 80 + 50 = 230. 3 gaps of 15 = 45. Pad = 10 + 20 = 30.
        // Total W = 230 + 45 + 30 = 305.
        // Max height = 45. Pad = 5 + 15 = 20. Total H = 65.
        assertEquals(305.0f, row.getPrefWidth())
        assertEquals(65.0f, row.getPrefHeight())
    }

    @Test
    fun `Column with 4 children hugs sum of heights, 3 gaps, and asymmetric padding`() {
        val col = createColumn(gap = 12.0f, padL = 8.0f, padR = 12.0f, padT = 14.0f, padB = 16.0f)
        val c1 = createNode(minWidth = 100.0f, minHeight = 30.0f)
        val c2 = createNode(minWidth = 140.0f, minHeight = 50.0f)
        val c3 = createNode(minWidth = 120.0f, minHeight = 40.0f)
        val c4 = createNode(minWidth = 90.0f, minHeight = 25.0f)
        col.children.addAll(listOf(c1, c2, c3, c4))

        // Max width = 140 + 8 + 12 = 160.
        // Sum heights = 30 + 50 + 40 + 25 = 145. 3 gaps of 12 = 36. Pad = 14 + 16 = 30.
        // Total H = 145 + 36 + 30 = 211.
        assertEquals(160.0f, col.getPrefWidth())
        assertEquals(211.0f, col.getPrefHeight())
    }

    // --- MARGINS & PADDING INTERACTION ---

    @Test
    fun `Child with outward margins expands container intrinsic size demand`() {
        val col = createColumn()
        val child = createNode(minWidth = 100.0f, minHeight = 50.0f, marginL = 10.0f, marginR = 15.0f, marginT = 8.0f, marginB = 12.0f)
        col.children.add(child)

        // Child minWidth = 100 + 10 + 15 = 125.
        // Child minHeight = 50 + 8 + 12 = 70.
        assertEquals(125.0f, col.getPrefWidth())
        assertEquals(70.0f, col.getPrefHeight())
    }

    // --- DEEPLY NESTED 4-LEVEL HIERARCHY ---

    @Test
    fun `Deeply nested 4-level tree calculates cumulative coordinates without precision loss`() {
        // Level 1: Canvas (1000x800)
        val canvas = CanvasNode().apply { resize(1000.0f, 800.0f) }

        // Level 2: Dialog Modal Box (Center anchor, 600x400, pad=20)
        val dialog = createBox(width = 600.0f, height = 400.0f, pad = 20.0f).apply {
            anchorData.setPreset(LayoutPreset.CENTER)
        }

        // Level 3: Main Content Column (gap=10, pad=10)
        val contentColumn = createColumn(width = 560.0f, height = 360.0f, gap = 10.0f, pad = 10.0f)

        // Level 4: Header Row (gap=10) & Footer Action Row (gap=10)
        val headerRow = createRow(width = 540.0f, height = 40.0f, gap = 10.0f)
        val titleIcon = createNode(width = 30.0f, height = 30.0f)
        val titleText = createNode(width = 200.0f, height = 30.0f)
        headerRow.children.addAll(listOf(titleIcon, titleText))

        val footerRow = createRow(width = 540.0f, height = 40.0f, gap = 10.0f, arrangement = Arrangement.End)
        val cancelBtn = createNode(width = 80.0f, height = 35.0f)
        val confirmBtn = createNode(width = 100.0f, height = 35.0f)
        footerRow.children.addAll(listOf(cancelBtn, confirmBtn))

        contentColumn.children.addAll(listOf(headerRow, footerRow))
        dialog.children.add(contentColumn)
        canvas.children.add(dialog)

        canvas.layout()

        // Dialog: Center in 1000x800 -> X = (1000-600)/2 = 200, Y = (800-400)/2 = 200
        dialog.assertBounds(expectedX = 200.0f, expectedY = 200.0f, expectedW = 600.0f, expectedH = 400.0f)

        // Content Column inside Dialog (pad=20): X = 200 + 20 = 220, Y = 200 + 20 = 220
        contentColumn.assertBounds(expectedX = 220.0f, expectedY = 220.0f, expectedW = 560.0f, expectedH = 360.0f)

        // Header Row inside Column (pad=10, top of 360):
        // Inner X = 220 + 10 = 230
        // Top Y of column = 220 + 360 - 10 = 570. Slot Y = 570 - 40 = 530.
        headerRow.assertBounds(expectedX = 230.0f, expectedY = 530.0f, expectedW = 540.0f, expectedH = 40.0f)

        // Header items inside Header Row (Y centered in 40: 530 + (40-30)/2 = 535)
        titleIcon.assertBounds(expectedX = 230.0f, expectedY = 535.0f, expectedW = 30.0f, expectedH = 30.0f)
        titleText.assertBounds(expectedX = 230.0f + 30.0f + 10.0f, expectedY = 535.0f, expectedW = 200.0f, expectedH = 30.0f)

        // Footer Row inside Column (below header 40 + gap 10): Slot Y = 530 - 10 - 40 = 480.
        footerRow.assertBounds(expectedX = 230.0f, expectedY = 480.0f, expectedW = 540.0f, expectedH = 40.0f)

        // Footer items in Arrangement.End (Total = 80 + 10 + 100 = 190. Free = 540 - 190 = 350):
        // cancelBtn: X = 230 + 350 = 580, Y = 480 + (40-35)/2 = 482.5
        // confirmBtn: X = 580 + 80 + 10 = 670, Y = 482.5
        cancelBtn.assertBounds(expectedX = 580.0f, expectedY = 482.5f, expectedW = 80.0f, expectedH = 35.0f)
        confirmBtn.assertBounds(expectedX = 670.0f, expectedY = 482.5f, expectedW = 100.0f, expectedH = 35.0f)
    }

    // --- INVALIDATION PROPAGATION ---

    @Test
    fun `Dynamic tree mutation triggers full layout recomputation`() {
        val parent = createRow(width = 400.0f, height = 60.0f, gap = 10.0f)
        val c1 = createNode(width = 100.0f, height = 40.0f)
        val c2 = createNode(width = 100.0f, height = 40.0f)
        parent.children.addAll(listOf(c1, c2))
        parent.layout()

        c1.assertBounds(expectedX = 0.0f, expectedY = 10.0f, expectedW = 100.0f, expectedH = 40.0f)
        c2.assertBounds(expectedX = 110.0f, expectedY = 10.0f, expectedW = 100.0f, expectedH = 40.0f)

        // Modify c1 width to 150px
        c1.width = 150.0f
        c1.invalidateLayout()
        parent.layout()

        // c2 shifts right by +50px
        c1.assertBounds(expectedX = 0.0f, expectedY = 10.0f, expectedW = 150.0f, expectedH = 40.0f)
        c2.assertBounds(expectedX = 160.0f, expectedY = 10.0f, expectedW = 100.0f, expectedH = 40.0f)
    }
}
