package org.hubdustry.libs.layout

import kotlin.test.Test
import kotlin.test.assertEquals

class AnchorMathTest {

    @Test
    fun testCenterPreset() {
        val parent = LayoutNode().apply {
            width = 800f
            height = 600f
        }

        val child = LayoutNode().apply {
            minWidth = 200f
            minHeight = 100f
            anchor.setPreset(AnchorPreset.CENTER)
        }
        parent.addChild(child)

        // Bố cục với kích thước khả dụng của cha là 800x600
        parent.layout(800f, 600f)

        assertEquals(300f, child.x, 0.001f)
        assertEquals(250f, child.y, 0.001f)
        assertEquals(200f, child.width, 0.001f)
        assertEquals(100f, child.height, 0.001f)
    }

    @Test
    fun testFullRectWithMargins() {
        val parent = LayoutNode().apply {
            width = 500f
            height = 400f
        }

        val child = LayoutNode().apply {
            anchor.setPreset(AnchorPreset.FULL_RECT)
            anchor.setOffsets(left = 10f, top = 20f, right = -10f, bottom = -20f)
        }
        parent.addChild(child)

        parent.layout(500f, 400f)

        assertEquals(10f, child.x, 0.001f)
        assertEquals(20f, child.y, 0.001f)
        assertEquals(480f, child.width, 0.001f)
        assertEquals(360f, child.height, 0.001f)
    }

    @Test
    fun testTopRightCorner() {
        val parent = LayoutNode().apply {
            width = 1000f
            height = 500f
        }

        val child = LayoutNode().apply {
            minWidth = 150f
            minHeight = 50f
            anchor.setPreset(AnchorPreset.TOP_RIGHT)
            anchor.setOffsets(left = -170f, top = 20f, right = -20f, bottom = 70f)
        }
        parent.addChild(child)

        parent.layout(1000f, 500f)

        assertEquals(830f, child.x, 0.001f)
        assertEquals(20f, child.y, 0.001f)
        assertEquals(150f, child.width, 0.001f)
        assertEquals(50f, child.height, 0.001f)
    }
}
