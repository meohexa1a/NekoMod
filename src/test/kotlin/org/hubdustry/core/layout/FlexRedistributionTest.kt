package org.hubdustry.core.layout

import org.hubdustry.core.layout.policies.ColumnPolicy
import org.hubdustry.core.layout.policies.RowPolicy
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FlexRedistributionTest {

    @Test
    fun testClampingRedistributionSingleChildHitMax() {
        val row = LayoutNode().apply {
            policy = RowPolicy(gap = 0f)
        }
        val nodeA = LayoutNode().apply {
            minWidth = 0f
            maxWidth = 50f
            sizeFlagHorizontal = SizeFlag.EXPAND
            stretchRatio = 1f
        }
        val nodeB = LayoutNode().apply {
            minWidth = 0f
            maxWidth = Float.MAX_VALUE
            sizeFlagHorizontal = SizeFlag.EXPAND
            stretchRatio = 1f
        }

        row.addChild(nodeA)
        row.addChild(nodeB)

        // Phân bổ trên 200px: Mỗi con ban đầu nhận 100px.
        // Node A bị kẹp trần ở 50px -> 50px thừa được tái phân bổ trọn vẹn cho Node B (100 + 50 = 150px)
        row.layout(200f, 100f)

        assertEquals(50f, nodeA.width, 0.001f)
        assertEquals(150f, nodeB.width, 0.001f)

        assertEquals(0f, nodeA.x, 0.001f)
        assertEquals(50f, nodeB.x, 0.001f)
        assertEquals(200f, nodeA.width + nodeB.width, 0.001f)
    }

    @Test
    fun testClampingRedistributionCascadingHits() {
        val row = LayoutNode().apply {
            policy = RowPolicy(gap = 0f)
        }
        val nodeA = LayoutNode().apply {
            minWidth = 0f
            maxWidth = 40f
            sizeFlagHorizontal = SizeFlag.EXPAND
            stretchRatio = 1f
        }
        val nodeB = LayoutNode().apply {
            minWidth = 0f
            maxWidth = 90f
            sizeFlagHorizontal = SizeFlag.EXPAND
            stretchRatio = 1f
        }
        val nodeC = LayoutNode().apply {
            minWidth = 0f
            maxWidth = Float.MAX_VALUE
            sizeFlagHorizontal = SizeFlag.EXPAND
            stretchRatio = 1f
        }

        row.addChild(nodeA)
        row.addChild(nodeB)
        row.addChild(nodeC)

        // Tổng 300px, 3 con:
        // Pass 1: Mỗi con 100px. A kẹp 40px -> Dư 60px dồn cho B và C (mỗi con 100 + 30 = 130px)
        // Pass 2: B (130px) kẹp 90px -> Dư 40px dồn tiếp cho C (130 + 40 = 170px)
        // Final: A = 40px, B = 90px, C = 170px. Tổng = 300px
        row.layout(300f, 100f)

        assertEquals(40f, nodeA.width, 0.001f)
        assertEquals(90f, nodeB.width, 0.001f)
        assertEquals(170f, nodeC.width, 0.001f)

        assertEquals(0f, nodeA.x, 0.001f)
        assertEquals(40f, nodeB.x, 0.001f)
        assertEquals(130f, nodeC.x, 0.001f)
        assertEquals(300f, nodeA.width + nodeB.width + nodeC.width, 0.001f)
    }

    @Test
    fun testClampingRedistributionColumnMaxHeight() {
        val col = LayoutNode().apply {
            policy = ColumnPolicy(gap = 0f)
        }
        val nodeA = LayoutNode().apply {
            minHeight = 0f
            maxHeight = 60f
            sizeFlagVertical = SizeFlag.EXPAND
            stretchRatio = 1f
        }
        val nodeB = LayoutNode().apply {
            minHeight = 0f
            maxHeight = Float.MAX_VALUE
            sizeFlagVertical = SizeFlag.EXPAND
            stretchRatio = 1f
        }

        col.addChild(nodeA)
        col.addChild(nodeB)

        col.layout(100f, 240f)

        assertEquals(60f, nodeA.height, 0.001f)
        assertEquals(180f, nodeB.height, 0.001f)
        assertEquals(0f, nodeA.y, 0.001f)
        assertEquals(60f, nodeB.y, 0.001f)
    }

    @Test
    fun testAllChildrenHitMaxCeiling() {
        val row = LayoutNode().apply {
            policy = RowPolicy(gap = 0f)
        }
        val nodeA = LayoutNode().apply {
            minWidth = 0f
            maxWidth = 50f
            sizeFlagHorizontal = SizeFlag.EXPAND
            stretchRatio = 1f
        }
        val nodeB = LayoutNode().apply {
            minWidth = 0f
            maxWidth = 50f
            sizeFlagHorizontal = SizeFlag.EXPAND
            stretchRatio = 1f
        }

        row.addChild(nodeA)
        row.addChild(nodeB)

        // Cả 2 con đều kẹp trần 50px trên container 300px
        row.layout(300f, 100f)

        assertEquals(50f, nodeA.width, 0.001f)
        assertEquals(50f, nodeB.width, 0.001f)
        assertEquals(0f, nodeA.x, 0.001f)
        assertEquals(50f, nodeB.x, 0.001f)
    }

    @Test
    fun testClampingWithNonZeroMinWidthAndGap() {
        val row = LayoutNode().apply {
            policy = RowPolicy(gap = 10f)
        }
        val nodeA = LayoutNode().apply {
            minWidth = 20f
            maxWidth = 60f
            sizeFlagHorizontal = SizeFlag.EXPAND
            stretchRatio = 1f
        }
        val nodeB = LayoutNode().apply {
            minWidth = 30f
            maxWidth = Float.MAX_VALUE
            sizeFlagHorizontal = SizeFlag.EXPAND
            stretchRatio = 1f
        }

        row.addChild(nodeA)
        row.addChild(nodeB)

        // Container 250px. Gap 10px -> Available = 240px.
        // Total min = 50. Free space = 190.
        // Node A kẹp ở 60px (dùng 40px free space).
        // Node B nhận trọn 150px free space còn lại -> width = 30 + 150 = 180px.
        row.layout(250f, 100f)

        assertEquals(60f, nodeA.width, 0.001f)
        assertEquals(180f, nodeB.width, 0.001f)
        assertEquals(0f, nodeA.x, 0.001f)
        assertEquals(70f, nodeB.x, 0.001f) // 60 + gap 10 = 70
    }

    @Test
    fun testExtremeStretchRatios() {
        val row = LayoutNode().apply {
            policy = RowPolicy(gap = 0f)
        }
        val hugeRatio = LayoutNode().apply {
            minWidth = 0f
            sizeFlagHorizontal = SizeFlag.EXPAND
            stretchRatio = 1_000_000f
        }
        val tinyRatio = LayoutNode().apply {
            minWidth = 0f
            sizeFlagHorizontal = SizeFlag.EXPAND
            stretchRatio = 0.0001f
        }

        row.addChild(hugeRatio)
        row.addChild(tinyRatio)

        row.layout(1000f, 100f)

        assertTrue(hugeRatio.width > 999.9f)
        assertTrue(tinyRatio.width >= 0f)
        assertEquals(1000f, hugeRatio.width + tinyRatio.width, 0.01f)
    }
}
