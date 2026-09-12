package org.hubdustry.libs.layout

import kotlin.test.Test
import kotlin.test.assertEquals

class AnchorResolutionTest {

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

    @Test
    fun testDirectResolveAnchorsCall() {
        val node = LayoutNode().apply {
            minWidth = 100f
            minHeight = 50f
            anchor.setPreset(AnchorPreset.CENTER)
        }

        // Direct call to node.resolveAnchors
        node.resolveAnchors(innerX = 50f, innerY = 50f, innerWidth = 500f, innerHeight = 300f)

        // center: innerX + 500 * 0.5 - 100 * 0.5 = 50 + 250 - 50 = 250
        // center: innerY + 300 * 0.5 - 50 * 0.5 = 50 + 150 - 25 = 175
        assertEquals(250f, node.x, 0.001f)
        assertEquals(175f, node.y, 0.001f)
        assertEquals(100f, node.width, 0.001f)
        assertEquals(50f, node.height, 0.001f)
    }

    @Test
    fun testResolveAnchorsClampingToMinMax() {
        val node = LayoutNode().apply {
            minWidth = 120f
            maxWidth = 200f
            minHeight = 60f
            maxHeight = 100f
            anchor.setPreset(AnchorPreset.FULL_RECT)
        }

        // In a very small container: inner width 50, height 30 -> clamped to min (120, 60)
        node.resolveAnchors(innerX = 0f, innerY = 0f, innerWidth = 50f, innerHeight = 30f)
        assertEquals(120f, node.width, 0.001f)
        assertEquals(60f, node.height, 0.001f)

        // In a huge container: inner width 500, height 400 -> clamped to max (200, 100)
        node.resolveAnchors(innerX = 0f, innerY = 0f, innerWidth = 500f, innerHeight = 400f)
        assertEquals(200f, node.width, 0.001f)
        assertEquals(100f, node.height, 0.001f)
    }

    @Test
    fun testResolveAnchorsCascadesToChildren() {
        val parent = LayoutNode().apply {
            anchor.setPreset(AnchorPreset.FULL_RECT)
            paddingLeft = 10f
            paddingTop = 15f
            paddingRight = 10f
            paddingBottom = 15f
        }

        val child = LayoutNode().apply {
            anchor.setPreset(AnchorPreset.FULL_RECT)
        }
        parent.addChild(child)

        // Resolve anchors on parent: 400x300
        parent.resolveAnchors(innerX = 0f, innerY = 0f, innerWidth = 400f, innerHeight = 300f)

        // Parent should be at 0, 0, 400, 300
        assertEquals(0f, parent.x, 0.001f)
        assertEquals(0f, parent.y, 0.001f)
        assertEquals(400f, parent.width, 0.001f)
        assertEquals(300f, parent.height, 0.001f)

        // Child should have been arranged within parent's inner bounds: innerX=10, innerY=15, innerW=380, innerH=270
        assertEquals(10f, child.x, 0.001f)
        assertEquals(15f, child.y, 0.001f)
        assertEquals(380f, child.width, 0.001f)
        assertEquals(270f, child.height, 0.001f)
    }
}
