package org.hubdustry.core.layout

import org.hubdustry.core.layout.policies.ColumnPolicy
import org.hubdustry.core.layout.policies.RowPolicy
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ExtremeBoundaryTest {

    @Test
    fun testUnconstrainedInfinityRow() {
        val row = LayoutNode().apply {
            policy = RowPolicy(gap = 10f)
        }
        val c1 = LayoutNode().apply {
            minWidth = 30f
            minHeight = 20f
            sizeFlagHorizontal = SizeFlag.FILL
        }
        val c2 = LayoutNode().apply {
            minWidth = 40f
            minHeight = 25f
            sizeFlagHorizontal = SizeFlag.FILL
        }
        row.addChild(c1)
        row.addChild(c2)

        // Truyền Float.MAX_VALUE vào unconstrained layout
        row.layout(Float.MAX_VALUE, Float.MAX_VALUE)

        assertFalse(row.width.isNaN())
        assertFalse(row.width.isInfinite())
        assertFalse(row.height.isNaN())
        assertFalse(row.height.isInfinite())

        // Fallback an toàn về minSize: 30 + 40 + gap 10 = 80
        assertEquals(80f, row.width, 0.001f)
        assertEquals(25f, row.height, 0.001f)

        assertFalse(c1.width.isNaN())
        assertFalse(c1.width.isInfinite())
        assertEquals(30f, c1.width, 0.001f)

        assertFalse(c2.width.isNaN())
        assertFalse(c2.width.isInfinite())
        assertEquals(40f, c2.width, 0.001f)
    }

    @Test
    fun testUnconstrainedInfinityColumn() {
        val col = LayoutNode().apply {
            policy = ColumnPolicy(gap = 15f)
        }
        val c1 = LayoutNode().apply {
            minWidth = 20f
            minHeight = 50f
            sizeFlagVertical = SizeFlag.FILL
        }
        val c2 = LayoutNode().apply {
            minWidth = 25f
            minHeight = 60f
            sizeFlagVertical = SizeFlag.FILL
        }
        col.addChild(c1)
        col.addChild(c2)

        col.layout(Float.MAX_VALUE, Float.MAX_VALUE)

        assertFalse(col.width.isNaN())
        assertFalse(col.width.isInfinite())
        assertFalse(col.height.isNaN())
        assertFalse(col.height.isInfinite())

        // Fallback an toàn về minSize: 50 + 60 + gap 15 = 125
        assertEquals(25f, col.width, 0.001f)
        assertEquals(125f, col.height, 0.001f)

        assertEquals(50f, c1.height, 0.001f)
        assertEquals(60f, c2.height, 0.001f)
    }

    @Test
    fun testContainerSmallerThanPadding() {
        val row = LayoutNode().apply {
            policy = RowPolicy(gap = 10f)
            setPadding(left = 50f, top = 50f, right = 50f, bottom = 50f)
        }
        val child = LayoutNode().apply {
            minWidth = 30f
            minHeight = 30f
        }
        row.addChild(child)

        // Container kích thước 40x40 nhưng padding là 100x100
        row.layout(40f, 40f)

        // node.width sẽ ít nhất là minWidth (30 + 100 = 130)
        assertTrue(row.width >= 130f)
        assertTrue(row.height >= 130f)
        assertTrue(child.width >= 30f)
        assertTrue(child.height >= 30f)
        assertTrue(child.x >= 0f)
        assertTrue(child.y >= 0f)
    }

    @Test
    fun testZeroVisibleChildren() {
        val row = LayoutNode().apply {
            policy = RowPolicy(gap = 20f)
        }
        for (i in 0 until 5) {
            val hiddenChild = LayoutNode().apply {
                visible = false
                minWidth = 100f
                minHeight = 100f
            }
            row.addChild(hiddenChild)
        }

        row.layout(400f, 400f)

        // Không có con nào hiển thị -> minWidth/minHeight bằng 0
        assertEquals(0f, row.minWidth, 0.001f)
        assertEquals(0f, row.minHeight, 0.001f)
    }

    @Test
    fun testNegativeAndNaNInputs() {
        val row = LayoutNode().apply {
            policy = RowPolicy(gap = 0f)
        }
        val child = LayoutNode().apply {
            minWidth = 50f
            minHeight = 50f
            stretchRatio = Float.NaN
        }
        row.addChild(child)

        // Truyền giá trị âm
        row.layout(-100f, -50f)
        assertTrue(row.width >= 50f)
        assertTrue(row.height >= 50f)
        assertFalse(row.width.isNaN())
        assertFalse(row.height.isNaN())

        // Truyền NaN
        row.layout(Float.NaN, Float.NaN)
        assertTrue(row.width >= 50f)
        assertTrue(row.height >= 50f)
        assertFalse(row.width.isNaN())
        assertFalse(row.height.isNaN())
    }

    @Test
    fun testNegativePaddingAndGapDefensive() {
        val row = LayoutNode().apply {
            policy = RowPolicy(gap = -10f)
            paddingLeft = -20f
            paddingTop = -20f
            paddingRight = -20f
            paddingBottom = -20f
        }
        val child = LayoutNode().apply {
            minWidth = 40f
            minHeight = 40f
            stretchRatio = -5f
        }
        row.addChild(child)

        row.layout(200f, 100f)

        assertTrue(row.width >= 0f)
        assertTrue(row.height >= 0f)
        assertTrue(child.width >= 40f)
        assertTrue(child.height >= 40f)
        assertFalse(child.x.isNaN())
        assertFalse(child.y.isNaN())
    }
}
