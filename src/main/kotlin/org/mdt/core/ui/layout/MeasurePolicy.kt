package org.mdt.core.ui.layout

import org.mdt.ui.components.layout.LayoutNode

/**
 * ## MeasurePolicy
 *
 * Defines the intrinsic measurement and positioning strategy for a [LayoutNode] and its children.
 *
 * See: docs/layout-engine/layout_engine_en.md
 */
interface MeasurePolicy {

    /** Computes preferred width of the [node] factoring in inward padding and children constraints. */
    fun measureWidth(node: LayoutNode): Float

    /** Computes preferred height of the [node] factoring in inward padding and children constraints. */
    fun measureHeight(node: LayoutNode): Float

    /** Computes and applies spatial layouts onto [node] children inside the available inner box. */
    fun layout(node: LayoutNode, innerX: Float, innerY: Float, availW: Float, availH: Float)
}

/**
 * ## BoxMeasurePolicy
 *
 * Layout policy supporting Hug Content (intrinsic sizing), Godot anchors,
 * and content alignment.
 */
object BoxMeasurePolicy : MeasurePolicy {
    override fun measureWidth(node: LayoutNode): Float {
        val visibleChildren = node.children.filter { it.visible }
        var maxChildW = 0f
        for (child in visibleChildren) {
            maxChildW = maxOf(maxChildW, GodotLayout.getChildMinWidth(child))
        }
        return if (node.minWidth >= 0f) maxOf(maxChildW, node.minWidth) else maxChildW
    }

    override fun measureHeight(node: LayoutNode): Float {
        val visibleChildren = node.children.filter { it.visible }
        var maxChildH = 0f
        for (child in visibleChildren) {
            maxChildH = maxOf(maxChildH, GodotLayout.getChildMinHeight(child))
        }
        return if (node.minHeight >= 0f) maxOf(maxChildH, node.minHeight) else maxChildH
    }

    override fun layout(node: LayoutNode, innerX: Float, innerY: Float, availW: Float, availH: Float) {
        for (child in node.children) {
            if (!child.visible) continue
            val a = child.anchorData
            val hasExplicitAnchor = a.anchorLeft != 0f || a.anchorRight != 0f || a.anchorTop != 0f || a.anchorBottom != 0f ||
                    a.offsetLeft != 0f || a.offsetRight != 0f || a.offsetTop != 0f || a.offsetBottom != 0f

            if (hasExplicitAnchor) {
                GodotLayout.layoutSingleAnchor(child, innerX, innerY, availW, availH)
            } else {
                // Standard unanchored box child: fit inside box with alignment and size flags
                GodotLayout.fitChildInRect(
                    child = child,
                    rx = innerX, ry = innerY, rw = availW, rh = availH,
                    hFlags = child.sizeFlagsHorizontal,
                    vFlags = child.sizeFlagsVertical
                )
            }
        }
    }
}

/**
 * ## ColumnMeasurePolicy
 *
 * Vertical linear layout policy arranging children top-to-bottom with Hug Content and Weight distribution.
 */
data class ColumnMeasurePolicy(
    val gap: Float = 0f,
    val arrangement: Arrangement = Arrangement.Start,
    val alignment: Alignment = Alignment.TopStart
) : MeasurePolicy {

    private val effectiveArrangement: Arrangement =
        if (gap > 0f && arrangement == Arrangement.Start) Arrangement.spacedBy(gap) else arrangement

    override fun measureWidth(node: LayoutNode): Float {
        val visibleChildren = node.children.filter { it.visible }
        if (visibleChildren.isEmpty()) return 0f
        var maxW = 0f
        for (child in visibleChildren) {
            maxW = maxOf(maxW, GodotLayout.getChildMinWidth(child))
        }
        return maxW
    }

    override fun measureHeight(node: LayoutNode): Float {
        val visibleChildren = node.children.filter { it.visible }
        if (visibleChildren.isEmpty()) return 0f
        var sum = 0f
        for (child in visibleChildren) {
            sum += GodotLayout.getChildMinHeight(child)
        }
        if (visibleChildren.size > 1) sum += (visibleChildren.size - 1) * effectiveArrangement.spacing
        return sum
    }

    override fun layout(node: LayoutNode, innerX: Float, innerY: Float, availW: Float, availH: Float) {
        GodotLayout.layoutBox(
            children = node.children,
            parentX = node.bounds.x, parentY = node.bounds.y,
            parentW = node.bounds.width, parentH = node.bounds.height,
            padL = node.padL, padT = node.padT, padR = node.padR, padB = node.padB,
            isVertical = true,
            arrangement = effectiveArrangement
        )
    }
}

/**
 * ## RowMeasurePolicy
 *
 * Horizontal linear layout policy arranging children left-to-right with Hug Content and Weight distribution.
 */
data class RowMeasurePolicy(
    val gap: Float = 0f,
    val arrangement: Arrangement = Arrangement.Start,
    val alignment: Alignment = Alignment.CenterStart
) : MeasurePolicy {

    private val effectiveArrangement: Arrangement =
        if (gap > 0f && arrangement == Arrangement.Start) Arrangement.spacedBy(gap) else arrangement

    override fun measureWidth(node: LayoutNode): Float {
        val visibleChildren = node.children.filter { it.visible }
        if (visibleChildren.isEmpty()) return 0f
        var sum = 0f
        for (child in visibleChildren) {
            sum += GodotLayout.getChildMinWidth(child)
        }
        if (visibleChildren.size > 1) sum += (visibleChildren.size - 1) * effectiveArrangement.spacing
        return sum
    }

    override fun layout(node: LayoutNode, innerX: Float, innerY: Float, availW: Float, availH: Float) {
        GodotLayout.layoutBox(
            children = node.children,
            parentX = node.bounds.x, parentY = node.bounds.y,
            parentW = node.bounds.width, parentH = node.bounds.height,
            padL = node.padL, padT = node.padT, padR = node.padR, padB = node.padB,
            isVertical = false,
            arrangement = effectiveArrangement
        )
    }

    override fun measureHeight(node: LayoutNode): Float {
        val visibleChildren = node.children.filter { it.visible }
        if (visibleChildren.isEmpty()) return 0f
        var maxH = 0f
        for (child in visibleChildren) {
            maxH = maxOf(maxH, GodotLayout.getChildMinHeight(child))
        }
        return maxH
    }
}

/**
 * ## GridMeasurePolicy
 *
 * Grid layout policy arranging children in uniform/flexible columns and rows.
 */
data class GridMeasurePolicy(
    val columns: Int = 1,
    val hGap: Float = 0f,
    val vGap: Float = 0f
) : MeasurePolicy {
    override fun measureWidth(node: LayoutNode): Float {
        if (columns <= 0) return 0f
        val visibleChildren = node.children.filter { it.visible }
        if (visibleChildren.isEmpty()) return 0f
        val colWidths = FloatArray(columns)
        for (i in visibleChildren.indices) {
            val c = i % columns
            colWidths[c] = maxOf(colWidths[c], GodotLayout.getChildMinWidth(visibleChildren[i]))
        }
        val totalGaps = if (columns > 1) (columns - 1) * hGap else 0f
        return colWidths.sum() + totalGaps
    }

    override fun measureHeight(node: LayoutNode): Float {
        if (columns <= 0) return 0f
        val visibleChildren = node.children.filter { it.visible }
        if (visibleChildren.isEmpty()) return 0f
        val rows = (visibleChildren.size + columns - 1) / columns
        val rowHeights = FloatArray(rows)
        for (i in visibleChildren.indices) {
            val r = i / columns
            rowHeights[r] = maxOf(rowHeights[r], GodotLayout.getChildMinHeight(visibleChildren[i]))
        }
        val totalGaps = if (rows > 1) (rows - 1) * vGap else 0f
        return rowHeights.sum() + totalGaps
    }

    override fun layout(node: LayoutNode, innerX: Float, innerY: Float, availW: Float, availH: Float) {
        GodotLayout.layoutGrid(
            children = node.children,
            parentX = node.bounds.x, parentY = node.bounds.y,
            parentW = node.bounds.width, parentH = node.bounds.height,
            padL = node.padL, padT = node.padT, padR = node.padR, padB = node.padB,
            columns = columns,
            hSeparation = hGap,
            vSeparation = vGap
        )
    }
}
