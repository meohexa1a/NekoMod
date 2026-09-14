package org.hubdustry.core.compose.modifier

import arc.graphics.Color
import org.hubdustry.core.compose.unit.dp
import org.hubdustry.core.layout.LayoutNode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class DpModifierTest {

    @Test
    fun `dp modifiers apply unboxed pixel values correctly to LayoutNode`() {
        val node = LayoutNode()
        val modifier = Modifier
            .size(100.dp, 50.dp)
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .margin(4.dp)
            .border(1.5f.dp, Color.royal)
            .offset(x = 10.dp, y = 20.dp)

        modifier.applyTo(node)

        assertEquals(100f, node.minWidth, 0.0001f)
        assertEquals(100f, node.maxWidth, 0.0001f)
        assertEquals(50f, node.minHeight, 0.0001f)
        assertEquals(50f, node.maxHeight, 0.0001f)

        assertEquals(12f, node.paddingLeft, 0.0001f)
        assertEquals(12f, node.paddingRight, 0.0001f)
        assertEquals(8f, node.paddingTop, 0.0001f)
        assertEquals(8f, node.paddingBottom, 0.0001f)

        assertEquals(4f, node.marginLeft, 0.0001f)
        assertEquals(4f, node.marginTop, 0.0001f)
        assertEquals(4f, node.marginRight, 0.0001f)
        assertEquals(4f, node.marginBottom, 0.0001f)

        assertEquals(1.5f, node.borderWidth, 0.0001f)
        assertEquals(Color.royal, node.borderColor)

        assertEquals(10f, node.offsetX, 0.0001f)
        assertEquals(20f, node.offsetY, 0.0001f)
    }

    @Test
    fun `size and constraint dp overloads set expected bounds`() {
        val squareNode = LayoutNode()
        Modifier.size(32.dp).applyTo(squareNode)
        assertEquals(32f, squareNode.minWidth, 0.0001f)
        assertEquals(32f, squareNode.maxWidth, 0.0001f)
        assertEquals(32f, squareNode.minHeight, 0.0001f)
        assertEquals(32f, squareNode.maxHeight, 0.0001f)

        val boundedNode = LayoutNode()
        Modifier.sizeIn(minWidth = 20.dp, minHeight = 10.dp, maxWidth = 80.dp, maxHeight = 60.dp).applyTo(boundedNode)
        assertEquals(20f, boundedNode.minWidth, 0.0001f)
        assertEquals(10f, boundedNode.minHeight, 0.0001f)
        assertEquals(80f, boundedNode.maxWidth, 0.0001f)
        assertEquals(60f, boundedNode.maxHeight, 0.0001f)

        val individualNode = LayoutNode()
        Modifier.width(48.dp).height(24.dp).applyTo(individualNode)
        assertEquals(48f, individualNode.minWidth, 0.0001f)
        assertEquals(48f, individualNode.maxWidth, 0.0001f)
        assertEquals(24f, individualNode.minHeight, 0.0001f)
        assertEquals(24f, individualNode.maxHeight, 0.0001f)

        val inNode = LayoutNode()
        Modifier.widthIn(min = 15.dp, max = 50.dp).heightIn(min = 25.dp, max = 75.dp).applyTo(inNode)
        assertEquals(15f, inNode.minWidth, 0.0001f)
        assertEquals(50f, inNode.maxWidth, 0.0001f)
        assertEquals(25f, inNode.minHeight, 0.0001f)
        assertEquals(75f, inNode.maxHeight, 0.0001f)
    }

    @Test
    fun `individual padding and margin dp overloads set expected insets`() {
        val node = LayoutNode()
        Modifier
            .padding(start = 1.dp, top = 2.dp, end = 3.dp, bottom = 4.dp)
            .margin(start = 5.dp, top = 6.dp, end = 7.dp, bottom = 8.dp)
            .applyTo(node)

        assertEquals(1f, node.paddingLeft, 0.0001f)
        assertEquals(2f, node.paddingTop, 0.0001f)
        assertEquals(3f, node.paddingRight, 0.0001f)
        assertEquals(4f, node.paddingBottom, 0.0001f)

        assertEquals(5f, node.marginLeft, 0.0001f)
        assertEquals(6f, node.marginTop, 0.0001f)
        assertEquals(7f, node.marginRight, 0.0001f)
        assertEquals(8f, node.marginBottom, 0.0001f)
    }

    @Test
    fun `negative dp offset shifts position backwards correctly without being clamped to zero`() {
        val node = LayoutNode()
        Modifier.offset(x = (-15).dp, y = (-25).dp).applyTo(node)

        assertEquals(-15f, node.offsetX, 0.0001f)
        assertEquals(-25f, node.offsetY, 0.0001f)
    }
}
