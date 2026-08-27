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
    fun layout(node: LayoutNode, innerX: Float, innerY: Float, availableWidth: Float, availableHeight: Float)
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
        var maxChildWidth = 0f
        for (child in visibleChildren) {
            maxChildWidth = maxOf(maxChildWidth, GodotLayout.getChildMinWidth(child))
        }
        return if (node.minWidth >= 0f) maxOf(maxChildWidth, node.minWidth) else maxChildWidth
    }

    override fun measureHeight(node: LayoutNode): Float {
        val visibleChildren = node.children.filter { it.visible }
        var maxChildHeight = 0f
        for (child in visibleChildren) {
            maxChildHeight = maxOf(maxChildHeight, GodotLayout.getChildMinHeight(child))
        }
        return if (node.minHeight >= 0f) maxOf(maxChildHeight, node.minHeight) else maxChildHeight
    }

    override fun layout(node: LayoutNode, innerX: Float, innerY: Float, availableWidth: Float, availableHeight: Float) {
        for (child in node.children) {
            if (!child.visible) continue

            if (child.anchorData.isEnabled) {
                GodotLayout.layoutSingleAnchor(child, innerX, innerY, availableWidth, availableHeight)
            } else {
                // Standard unanchored box child: fit inside box with alignment and size flags
                GodotLayout.fitChildInRect(
                    child = child,
                    rectX = innerX,
                    rectY = innerY,
                    rectWidth = availableWidth,
                    rectHeight = availableHeight,
                    horizontalFlags = child.sizeFlagsHorizontal,
                    verticalFlags = child.sizeFlagsVertical
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

        var maxWidth = 0f
        for (child in visibleChildren) {
            maxWidth = maxOf(maxWidth, GodotLayout.getChildMinWidth(child))
        }
        return maxWidth
    }

    override fun measureHeight(node: LayoutNode): Float {
        val visibleChildren = node.children.filter { it.visible }
        if (visibleChildren.isEmpty()) return 0f

        var sumHeight = 0f
        for (child in visibleChildren) {
            sumHeight += GodotLayout.getChildMinHeight(child)
        }
        if (visibleChildren.size > 1) sumHeight += (visibleChildren.size - 1) * effectiveArrangement.spacing
        return sumHeight
    }

    override fun layout(node: LayoutNode, innerX: Float, innerY: Float, availableWidth: Float, availableHeight: Float) {
        GodotLayout.layoutBox(
            children = node.children,
            parentX = innerX - node.padL,
            parentY = innerY - node.padB,
            parentWidth = availableWidth + node.padL + node.padR,
            parentHeight = availableHeight + node.padT + node.padB,
            padLeft = node.padL,
            padTop = node.padT,
            padRight = node.padR,
            padBottom = node.padB,
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

        var sumWidth = 0f
        for (child in visibleChildren) {
            sumWidth += GodotLayout.getChildMinWidth(child)
        }
        if (visibleChildren.size > 1) sumWidth += (visibleChildren.size - 1) * effectiveArrangement.spacing
        return sumWidth
    }

    override fun layout(node: LayoutNode, innerX: Float, innerY: Float, availableWidth: Float, availableHeight: Float) {
        GodotLayout.layoutBox(
            children = node.children,
            parentX = innerX - node.padL,
            parentY = innerY - node.padB,
            parentWidth = availableWidth + node.padL + node.padR,
            parentHeight = availableHeight + node.padT + node.padB,
            padLeft = node.padL,
            padTop = node.padT,
            padRight = node.padR,
            padBottom = node.padB,
            isVertical = false,
            arrangement = effectiveArrangement
        )
    }

    override fun measureHeight(node: LayoutNode): Float {
        val visibleChildren = node.children.filter { it.visible }
        if (visibleChildren.isEmpty()) return 0f

        var maxHeight = 0f
        for (child in visibleChildren) {
            maxHeight = maxOf(maxHeight, GodotLayout.getChildMinHeight(child))
        }
        return maxHeight
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

        val columnWidths = FloatArray(columns)
        for (i in visibleChildren.indices) {
            val c = i % columns
            columnWidths[c] = maxOf(columnWidths[c], GodotLayout.getChildMinWidth(visibleChildren[i]))
        }
        val totalGaps = if (columns > 1) (columns - 1) * hGap else 0f
        return columnWidths.sum() + totalGaps
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

    override fun layout(node: LayoutNode, innerX: Float, innerY: Float, availableWidth: Float, availableHeight: Float) {
        GodotLayout.layoutGrid(
            children = node.children,
            parentX = innerX - node.padL,
            parentY = innerY - node.padB,
            parentWidth = availableWidth + node.padL + node.padR,
            parentHeight = availableHeight + node.padT + node.padB,
            padLeft = node.padL,
            padTop = node.padT,
            padRight = node.padR,
            padBottom = node.padB,
            columns = columns,
            hSeparation = hGap,
            vSeparation = vGap
        )
    }
}
