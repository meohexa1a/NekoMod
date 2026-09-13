package org.hubdustry.core.layout

import org.hubdustry.core.layout.policies.BoxLayoutPolicy
import org.hubdustry.core.layout.policies.ColumnPolicy
import kotlin.test.Test
import kotlin.test.assertEquals

class NestedLayoutTest {

    @Test
    fun testHeaderContentFooterStructure() {
        // Gốc là một Box container kích thước 800x600
        val rootBox = LayoutNode().apply {
            policy = BoxLayoutPolicy
        }

        // Column phủ kín toàn bộ diện tích của Box (FULL_RECT)
        val column = LayoutNode().apply {
            policy = ColumnPolicy(gap = 0f)
            anchor.setPreset(AnchorPreset.FULL_RECT)
        }
        rootBox.addChild(column)

        // 1. Thanh Header (height/minHeight = 60, SHRINK)
        val header = LayoutNode().apply {
            minHeight = 60f
            sizeFlagVertical = SizeFlag.SHRINK
        }

        // 2. Vùng Content (EXPAND, stretchRatio = 1f)
        val content = LayoutNode().apply {
            minHeight = 0f
            sizeFlagVertical = SizeFlag.EXPAND
            stretchRatio = 1f
        }

        // 3. Thanh Footer (height/minHeight = 40, SHRINK)
        val footer = LayoutNode().apply {
            minHeight = 40f
            sizeFlagVertical = SizeFlag.SHRINK
        }

        column.addChild(header)
        column.addChild(content)
        column.addChild(footer)

        // Bắt đầu layout từ node gốc
        rootBox.layout(800f, 600f)

        // Kiểm tra Column đã phủ kín Box 800x600
        assertEquals(0f, column.x, 0.001f)
        assertEquals(0f, column.y, 0.001f)
        assertEquals(800f, column.width, 0.001f)
        assertEquals(600f, column.height, 0.001f)

        // Kỳ vọng: Header y=0, h=60. Content y=60, h=500. Footer y=560, h=40. Không lệch Y.
        assertEquals(0f, header.y, 0.001f)
        assertEquals(60f, header.height, 0.001f)

        assertEquals(60f, content.y, 0.001f)
        assertEquals(500f, content.height, 0.001f)

        assertEquals(560f, footer.y, 0.001f)
        assertEquals(40f, footer.height, 0.001f)
    }
}
