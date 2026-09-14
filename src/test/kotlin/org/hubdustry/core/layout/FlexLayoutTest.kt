package org.hubdustry.core.layout

import org.hubdustry.core.layout.policies.ColumnPolicy
import org.hubdustry.core.layout.policies.RowPolicy
import kotlin.test.Test
import kotlin.test.assertEquals

class FlexLayoutTest {

    @Test
    fun testRowEqualDistribution1to1to1() {
        val row = LayoutNode().apply {
            policy = RowPolicy(gap = 0f)
        }
        val c1 = LayoutNode().apply { minWidth = 10f; sizeFlagHorizontal = SizeFlag.FILL; stretchRatio = 1f }
        val c2 = LayoutNode().apply { minWidth = 10f; sizeFlagHorizontal = SizeFlag.FILL; stretchRatio = 1f }
        val c3 = LayoutNode().apply { minWidth = 10f; sizeFlagHorizontal = SizeFlag.FILL; stretchRatio = 1f }

        row.addChild(c1)
        row.addChild(c2)
        row.addChild(c3)

        row.layout(300f, 100f)

        assertEquals(100f, c1.width, 0.001f)
        assertEquals(100f, c2.width, 0.001f)
        assertEquals(100f, c3.width, 0.001f)

        assertEquals(0f, c1.x, 0.001f)
        assertEquals(100f, c2.x, 0.001f)
        assertEquals(200f, c3.x, 0.001f)
    }

    @Test
    fun testRowUnequalDistribution1to2to3() {
        val row = LayoutNode().apply {
            policy = RowPolicy(gap = 0f)
        }
        // Total ratio = 1 + 2 + 3 = 6. Total width = 600. MinWidth = 0.
        // Expected: c1 = 100, c2 = 200, c3 = 300
        val c1 = LayoutNode().apply { minWidth = 0f; sizeFlagHorizontal = SizeFlag.FILL; stretchRatio = 1f }
        val c2 = LayoutNode().apply { minWidth = 0f; sizeFlagHorizontal = SizeFlag.FILL; stretchRatio = 2f }
        val c3 = LayoutNode().apply { minWidth = 0f; sizeFlagHorizontal = SizeFlag.FILL; stretchRatio = 3f }

        row.addChild(c1)
        row.addChild(c2)
        row.addChild(c3)

        row.layout(600f, 100f)

        assertEquals(100f, c1.width, 0.001f)
        assertEquals(200f, c2.width, 0.001f)
        assertEquals(300f, c3.width, 0.001f)

        assertEquals(0f, c1.x, 0.001f)
        assertEquals(100f, c2.x, 0.001f)
        assertEquals(300f, c3.x, 0.001f)
    }

    @Test
    fun testRowWithGap() {
        val row = LayoutNode().apply {
            policy = RowPolicy(gap = 20f)
        }
        // 3 children, 2 gaps = 40px. Total = 340px.
        // Available for expand = 340 - 40 = 300px -> each gets 100px.
        val c1 = LayoutNode().apply { minWidth = 0f; sizeFlagHorizontal = SizeFlag.FILL; stretchRatio = 1f }
        val c2 = LayoutNode().apply { minWidth = 0f; sizeFlagHorizontal = SizeFlag.FILL; stretchRatio = 1f }
        val c3 = LayoutNode().apply { minWidth = 0f; sizeFlagHorizontal = SizeFlag.FILL; stretchRatio = 1f }

        row.addChild(c1)
        row.addChild(c2)
        row.addChild(c3)

        row.layout(340f, 100f)

        assertEquals(100f, c1.width, 0.001f)
        assertEquals(100f, c2.width, 0.001f)
        assertEquals(100f, c3.width, 0.001f)

        assertEquals(0f, c1.x, 0.001f)
        assertEquals(120f, c2.x, 0.001f)
        assertEquals(240f, c3.x, 0.001f)
    }

    @Test
    fun testColumnEqualDistribution1to1to1() {
        val column = LayoutNode().apply {
            policy = ColumnPolicy(gap = 0f)
        }
        val c1 = LayoutNode().apply { minHeight = 10f; sizeFlagVertical = SizeFlag.FILL; stretchRatio = 1f }
        val c2 = LayoutNode().apply { minHeight = 10f; sizeFlagVertical = SizeFlag.FILL; stretchRatio = 1f }
        val c3 = LayoutNode().apply { minHeight = 10f; sizeFlagVertical = SizeFlag.FILL; stretchRatio = 1f }

        column.addChild(c1)
        column.addChild(c2)
        column.addChild(c3)

        column.layout(100f, 300f)

        assertEquals(100f, c1.height, 0.001f)
        assertEquals(100f, c2.height, 0.001f)
        assertEquals(100f, c3.height, 0.001f)

        assertEquals(0f, c1.y, 0.001f)
        assertEquals(100f, c2.y, 0.001f)
        assertEquals(200f, c3.y, 0.001f)
    }

    @Test
    fun testColumnUnequalDistribution1to2to3() {
        val column = LayoutNode().apply {
            policy = ColumnPolicy(gap = 0f)
        }
        val c1 = LayoutNode().apply { minHeight = 0f; sizeFlagVertical = SizeFlag.FILL; stretchRatio = 1f }
        val c2 = LayoutNode().apply { minHeight = 0f; sizeFlagVertical = SizeFlag.FILL; stretchRatio = 2f }
        val c3 = LayoutNode().apply { minHeight = 0f; sizeFlagVertical = SizeFlag.FILL; stretchRatio = 3f }

        column.addChild(c1)
        column.addChild(c2)
        column.addChild(c3)

        column.layout(100f, 600f)

        assertEquals(100f, c1.height, 0.001f)
        assertEquals(200f, c2.height, 0.001f)
        assertEquals(300f, c3.height, 0.001f)

        assertEquals(0f, c1.y, 0.001f)
        assertEquals(100f, c2.y, 0.001f)
        assertEquals(300f, c3.y, 0.001f)
    }

    @Test
    fun testColumnWithGap() {
        val column = LayoutNode().apply {
            policy = ColumnPolicy(gap = 25f)
        }
        val c1 = LayoutNode().apply { minHeight = 0f; sizeFlagVertical = SizeFlag.FILL; stretchRatio = 1f }
        val c2 = LayoutNode().apply { minHeight = 0f; sizeFlagVertical = SizeFlag.FILL; stretchRatio = 1f }
        val c3 = LayoutNode().apply { minHeight = 0f; sizeFlagVertical = SizeFlag.FILL; stretchRatio = 1f }

        column.addChild(c1)
        column.addChild(c2)
        column.addChild(c3)

        column.layout(100f, 350f)

        assertEquals(100f, c1.height, 0.001f)
        assertEquals(100f, c2.height, 0.001f)
        assertEquals(100f, c3.height, 0.001f)

        assertEquals(0f, c1.y, 0.001f)
        assertEquals(125f, c2.y, 0.001f)
        assertEquals(250f, c3.y, 0.001f)
    }

    @Test
    fun testCrossAxisAlignmentInRow() {
        val row = LayoutNode().apply {
            policy = RowPolicy(gap = 0f)
        }
        val topChild = LayoutNode().apply {
            minWidth = 50f; minHeight = 40f
            sizeFlagHorizontal = SizeFlag.SHRINK
            sizeFlagVertical = SizeFlag.SHRINK
            alignVertical = Alignment.START
        }
        val centerChild = LayoutNode().apply {
            minWidth = 50f; minHeight = 40f
            sizeFlagHorizontal = SizeFlag.SHRINK
            sizeFlagVertical = SizeFlag.SHRINK
            alignVertical = Alignment.CENTER
        }
        val bottomChild = LayoutNode().apply {
            minWidth = 50f; minHeight = 40f
            sizeFlagHorizontal = SizeFlag.SHRINK
            sizeFlagVertical = SizeFlag.SHRINK
            alignVertical = Alignment.END
        }

        row.addChild(topChild)
        row.addChild(centerChild)
        row.addChild(bottomChild)

        // Row height = 100
        row.layout(300f, 100f)

        assertEquals(0f, topChild.y, 0.001f)
        assertEquals(30f, centerChild.y, 0.001f) // (100 - 40) * 0.5 = 30
        assertEquals(60f, bottomChild.y, 0.001f) // 100 - 40 = 60
    }

    @Test
    fun testCrossAxisAlignmentInColumn() {
        val col = LayoutNode().apply {
            policy = ColumnPolicy(gap = 0f)
        }
        val leftChild = LayoutNode().apply {
            minWidth = 40f; minHeight = 50f
            sizeFlagHorizontal = SizeFlag.SHRINK
            sizeFlagVertical = SizeFlag.SHRINK
            alignHorizontal = Alignment.START
        }
        val centerChild = LayoutNode().apply {
            minWidth = 40f; minHeight = 50f
            sizeFlagHorizontal = SizeFlag.SHRINK
            sizeFlagVertical = SizeFlag.SHRINK
            alignHorizontal = Alignment.CENTER
        }
        val rightChild = LayoutNode().apply {
            minWidth = 40f; minHeight = 50f
            sizeFlagHorizontal = SizeFlag.SHRINK
            sizeFlagVertical = SizeFlag.SHRINK
            alignHorizontal = Alignment.END
        }

        col.addChild(leftChild)
        col.addChild(centerChild)
        col.addChild(rightChild)

        // Col width = 100
        col.layout(100f, 300f)

        assertEquals(0f, leftChild.x, 0.001f)
        assertEquals(30f, centerChild.x, 0.001f) // (100 - 40) * 0.5 = 30
        assertEquals(60f, rightChild.x, 0.001f) // 100 - 40 = 60
    }

    @Test
    fun testComputeMinSizeBottomUp() {
        val row = LayoutNode().apply {
            policy = RowPolicy(gap = 10f)
            setPadding(10f, 15f, 20f, 25f)
        }
        val c1 = LayoutNode().apply { minWidth = 40f; minHeight = 30f }
        val c2 = LayoutNode().apply { minWidth = 60f; minHeight = 50f }

        row.addChild(c1)
        row.addChild(c2)

        row.policy.computeMinSize(row)

        // MinWidth = 40 + 60 + gap 10 + padLeft 10 + padRight 20 = 140
        // MinHeight = max(30, 50) + padTop 15 + padBottom 25 = 90
        assertEquals(140f, row.minWidth, 0.001f)
        assertEquals(90f, row.minHeight, 0.001f)
    }

    @Test
    fun testGhostNodeWithAnchorInRow() {
        val row = LayoutNode().apply {
            policy = RowPolicy(gap = 0f)
        }
        val buttonA = LayoutNode().apply {
            minWidth = 50f
            sizeFlagHorizontal = SizeFlag.SHRINK
        }
        val ghostBadge = LayoutNode().apply {
            minWidth = 20f
            minHeight = 20f
            anchor.setPreset(AnchorPreset.TOP_RIGHT)
        }
        val buttonB = LayoutNode().apply {
            minWidth = 50f
            sizeFlagHorizontal = SizeFlag.SHRINK
        }

        row.addChild(buttonA)
        row.addChild(ghostBadge)
        row.addChild(buttonB)

        row.layout(300f, 100f)

        // Ghost badge không làm dời vị trí của buttonB
        assertEquals(0f, buttonA.x, 0.001f)
        assertEquals(50f, buttonB.x, 0.001f)

        // Neo chính xác vào góc trên-phải của Row: x = 300 - 20 = 280, y = 0
        assertEquals(280f, ghostBadge.x, 0.001f)
        assertEquals(0f, ghostBadge.y, 0.001f)
        assertEquals(20f, ghostBadge.width, 0.001f)
        assertEquals(20f, ghostBadge.height, 0.001f)

        // Min size không bị inflate bởi ghostBadge
        assertEquals(100f, row.minWidth, 0.001f)
    }

    @Test
    fun testGhostNodeInColumn() {
        val column = LayoutNode().apply {
            policy = ColumnPolicy(gap = 0f)
        }
        val buttonA = LayoutNode().apply {
            minHeight = 50f
            sizeFlagVertical = SizeFlag.SHRINK
        }
        val ghostBadge = LayoutNode().apply {
            minWidth = 20f
            minHeight = 20f
            anchor.setPreset(AnchorPreset.BOTTOM_RIGHT)
        }
        val buttonB = LayoutNode().apply {
            minHeight = 50f
            sizeFlagVertical = SizeFlag.SHRINK
        }

        column.addChild(buttonA)
        column.addChild(ghostBadge)
        column.addChild(buttonB)

        column.layout(100f, 300f)

        // Ghost badge không làm dời vị trí của buttonB
        assertEquals(0f, buttonA.y, 0.001f)
        assertEquals(50f, buttonB.y, 0.001f)

        // Neo chính xác vào góc dưới-phải của Column: x = 100 - 20 = 80, y = 300 - 20 = 280
        assertEquals(80f, ghostBadge.x, 0.001f)
        assertEquals(280f, ghostBadge.y, 0.001f)
        assertEquals(20f, ghostBadge.width, 0.001f)
        assertEquals(20f, ghostBadge.height, 0.001f)

        // Min size không bị inflate bởi ghostBadge
        assertEquals(100f, column.minHeight, 0.001f)
    }
}
