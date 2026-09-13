package org.hubdustry.core.layout

import org.hubdustry.core.layout.policies.BoxLayoutPolicy
import org.hubdustry.core.layout.policies.ColumnPolicy
import org.hubdustry.core.layout.policies.RowPolicy
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ZeroGcStressTest {

    @Test
    fun testHighFrequencyLayoutStress() {
        val root = LayoutNode().apply {
            policy = BoxLayoutPolicy
        }

        val column = LayoutNode().apply {
            policy = ColumnPolicy(gap = 4f)
            anchor.setPreset(AnchorPreset.FULL_RECT)
        }
        root.addChild(column)

        val row = LayoutNode().apply {
            policy = RowPolicy(gap = 8f)
            minHeight = 40f
            sizeFlagVertical = SizeFlag.FILL
        }
        column.addChild(row)

        for (i in 0 until 10) {
            val child = LayoutNode().apply {
                minWidth = 20f
                sizeFlagHorizontal = SizeFlag.FILL
                stretchRatio = (i + 1).toFloat()
            }
            row.addChild(child)
        }

        // Thực hiện 50,000 lần layout liên tục để bảo đảm không bị rò rỉ, crash hoặc lỗi toán học
        for (step in 0 until 50_000) {
            val availableW = 1000f + (step % 100)
            val availableH = 600f + (step % 50)
            root.layout(availableW, availableH)
            assertTrue(row.width > 0f)
            assertTrue(row.height > 0f)
        }
    }

    @Test
    fun testNegativeAndZeroBoundsDefensive() {
        val row = LayoutNode().apply {
            policy = RowPolicy(gap = 10f)
        }

        val child1 = LayoutNode().apply {
            minWidth = 100f
            sizeFlagHorizontal = SizeFlag.FILL
        }
        val child2 = LayoutNode().apply {
            minWidth = 100f
            sizeFlagHorizontal = SizeFlag.FILL
        }
        row.addChild(child1)
        row.addChild(child2)

        // Container khả dụng chỉ có 50px (nhỏ hơn tổng minWidth 200px + gap 10px)
        row.layout(50f, 50f)

        // Không được phép ra số âm hoặc NaN
        assertTrue(child1.width >= 100f)
        assertTrue(child2.width >= 100f)
        assertTrue(child1.x >= 0f)
    }

    @Test
    fun testZeroVisibleChildren() {
        val column = LayoutNode().apply {
            policy = ColumnPolicy(gap = 10f)
        }

        val hiddenChild = LayoutNode().apply {
            visible = false
            minHeight = 100f
        }
        column.addChild(hiddenChild)

        column.layout(200f, 200f)
        assertEquals(0f, column.minHeight)
    }

    @Test
    fun testSelfAttachmentPrevention() {
        val node = LayoutNode()
        node.addChild(node) // Không được phép thêm chính mình làm con
        assertEquals(0, node.children.size)
    }
}
