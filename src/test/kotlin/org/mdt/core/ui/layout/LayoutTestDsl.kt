package org.mdt.core.ui.layout

import org.mdt.core.ui.node.LayoutNode
import org.mdt.core.ui.node.UINode
import kotlin.test.assertEquals

/**
 * Asserts that this node's bounding rectangle matches the expected geometry within [tolerance].
 */
fun UINode.assertBounds(
    expectedX: Float,
    expectedY: Float,
    expectedW: Float,
    expectedH: Float,
    tolerance: Float = 0.01f,
    label: String = id.ifEmpty { name.ifEmpty { "node" } }
) {
    assertEquals(expectedX, bounds.x, tolerance, "[$label] Mismatched X origin")
    assertEquals(expectedY, bounds.y, tolerance, "[$label] Mismatched Y origin")
    assertEquals(expectedW, bounds.width, tolerance, "[$label] Mismatched Width")
    assertEquals(expectedH, bounds.height, tolerance, "[$label] Mismatched Height")
}

/**
 * Creates a configured [LayoutNode] leaf or child container for testing.
 */
fun createNode(
    id: String = "",
    width: Float = -1.0f,
    height: Float = -1.0f,
    minWidth: Float = -1.0f,
    minHeight: Float = -1.0f,
    margin: Float = 0.0f,
    marginL: Float = margin,
    marginT: Float = margin,
    marginR: Float = margin,
    marginB: Float = margin,
    hFlags: Int = 0,
    vFlags: Int = 0,
    stretchRatio: Float = 1.0f,
    visible: Boolean = true
): LayoutNode {
    return LayoutNode().apply {
        this.id = id
        this.width = width
        this.height = height
        this.minWidth = minWidth
        this.minHeight = minHeight
        this.marginL = marginL
        this.marginT = marginT
        this.marginR = marginR
        this.marginB = marginB
        this.sizeFlagsHorizontal = hFlags
        this.sizeFlagsVertical = vFlags
        this.stretchRatio = stretchRatio
        this.visible = visible
    }
}

/**
 * Creates a Box layout container with optional bounds and inward padding.
 */
fun createBox(
    width: Float = -1.0f,
    height: Float = -1.0f,
    minWidth: Float = -1.0f,
    minHeight: Float = -1.0f,
    pad: Float = 0.0f,
    padL: Float = pad,
    padT: Float = pad,
    padR: Float = pad,
    padB: Float = pad
): LayoutNode {
    return LayoutNode().apply {
        this.width = width
        this.height = height
        this.minWidth = minWidth
        this.minHeight = minHeight
        this.padL = padL
        this.padT = padT
        this.padR = padR
        this.padB = padB
        this.measurePolicy = BoxMeasurePolicy
    }
}

/**
 * Creates a Row layout container with optional bounds, spacing, and inward padding.
 */
fun createRow(
    width: Float = -1.0f,
    height: Float = -1.0f,
    gap: Float = 0.0f,
    pad: Float = 0.0f,
    padL: Float = pad,
    padT: Float = pad,
    padR: Float = pad,
    padB: Float = pad,
    arrangement: Arrangement = Arrangement.Start,
    alignment: Alignment = Alignment.CenterStart
): LayoutNode {
    return LayoutNode().apply {
        this.width = width
        this.height = height
        this.padL = padL
        this.padT = padT
        this.padR = padR
        this.padB = padB
        this.measurePolicy = RowMeasurePolicy(gap = gap, arrangement = arrangement, alignment = alignment)
    }
}

/**
 * Creates a Column layout container with optional bounds, spacing, and inward padding.
 */
fun createColumn(
    width: Float = -1.0f,
    height: Float = -1.0f,
    gap: Float = 0.0f,
    pad: Float = 0.0f,
    padL: Float = pad,
    padT: Float = pad,
    padR: Float = pad,
    padB: Float = pad,
    arrangement: Arrangement = Arrangement.Start,
    alignment: Alignment = Alignment.TopStart
): LayoutNode {
    return LayoutNode().apply {
        this.width = width
        this.height = height
        this.padL = padL
        this.padT = padT
        this.padR = padR
        this.padB = padB
        this.measurePolicy = ColumnMeasurePolicy(gap = gap, arrangement = arrangement, alignment = alignment)
    }
}
