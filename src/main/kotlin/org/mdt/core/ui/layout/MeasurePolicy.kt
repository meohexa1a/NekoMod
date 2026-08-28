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

    /** Computes preferred height of the [node] factoring in inward padding, children constraints, and optional [availableWidth]. */
    fun measureHeight(node: LayoutNode, availableWidth: Float = -1.0f): Float

    /** Computes and applies spatial layouts onto [node] children inside the available inner box. */
    fun layout(node: LayoutNode, innerX: Float, innerY: Float, availableWidth: Float, availableHeight: Float)
}

// --- BOX MEASURE POLICY ---

/**
 * ## BoxMeasurePolicy
 *
 * Layout policy supporting Hug Content (intrinsic sizing), Godot anchors,
 * and content alignment.
 *
 * See: docs/layout-engine/layout_engine_en.md
 */
object BoxMeasurePolicy : MeasurePolicy {

    override fun measureWidth(node: LayoutNode): Float {
        val visibleChildren = node.children.filter { it.visible }
        var maxChildWidth = 0.0f
        for (i in 0 until visibleChildren.size) {
            maxChildWidth = maxOf(maxChildWidth, GodotLayout.getChildMinWidth(visibleChildren[i]))
        }
        return if (node.minWidth >= 0.0f) maxOf(maxChildWidth, node.minWidth) else maxChildWidth
    }

    override fun measureHeight(node: LayoutNode, availableWidth: Float): Float {
        val visibleChildren = node.children.filter { it.visible }
        var maxChildHeight = 0.0f
        val availableInnerWidth = if (availableWidth >= 0.0f) availableWidth
                                  else if (node.width >= 0.0f) maxOf(0.0f, node.width - node.padL - node.padR)
                                  else -1.0f

        for (i in 0 until visibleChildren.size) {
            maxChildHeight = maxOf(maxChildHeight, GodotLayout.getChildMinHeight(visibleChildren[i], availableInnerWidth))
        }
        return if (node.minHeight >= 0.0f) maxOf(maxChildHeight, node.minHeight) else maxChildHeight
    }

    override fun layout(node: LayoutNode, innerX: Float, innerY: Float, availableWidth: Float, availableHeight: Float) {
        for (i in 0 until node.children.size) {
            val child = node.children[i]
            if (!child.visible) continue

            if (child.anchorData.isEnabled) {
                GodotLayout.layoutSingleAnchor(child, innerX, innerY, availableWidth, availableHeight)
            } else {
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

// --- COLUMN MEASURE POLICY ---

/**
 * ## ColumnMeasurePolicy
 *
 * Vertical linear layout policy arranging children top-to-bottom with Hug Content and Weight distribution.
 *
 * See: docs/layout-engine/layout_engine_en.md
 */
data class ColumnMeasurePolicy(
    val gap: Float = 0.0f,
    val arrangement: Arrangement = Arrangement.Start,
    val alignment: Alignment = Alignment.TopStart
) : MeasurePolicy {

    private val effectiveArrangement: Arrangement =
        if (gap > 0.0f && arrangement == Arrangement.Start) Arrangement.spacedBy(gap) else arrangement

    override fun measureWidth(node: LayoutNode): Float {
        val visibleChildren = node.children.filter { it.visible }
        if (visibleChildren.isEmpty()) return 0.0f

        var maxWidth = 0.0f
        for (i in 0 until visibleChildren.size) {
            maxWidth = maxOf(maxWidth, GodotLayout.getChildMinWidth(visibleChildren[i]))
        }
        return maxWidth
    }

    override fun measureHeight(node: LayoutNode, availableWidth: Float): Float {
        val visibleChildren = node.children.filter { it.visible }
        if (visibleChildren.isEmpty()) return 0.0f

        var sumHeight = 0.0f
        val availableInnerWidth = if (availableWidth >= 0.0f) availableWidth
                                  else if (node.width >= 0.0f) maxOf(0.0f, node.width - node.padL - node.padR)
                                  else -1.0f

        for (i in 0 until visibleChildren.size) {
            sumHeight += GodotLayout.getChildMinHeight(visibleChildren[i], availableInnerWidth)
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
            arrangement = effectiveArrangement,
            alignment = alignment
        )
    }
}

// --- ROW MEASURE POLICY ---

/**
 * ## RowMeasurePolicy
 *
 * Horizontal linear layout policy arranging children left-to-right with Hug Content and Weight distribution.
 *
 * See: docs/layout-engine/layout_engine_en.md
 */
data class RowMeasurePolicy(
    val gap: Float = 0.0f,
    val arrangement: Arrangement = Arrangement.Start,
    val alignment: Alignment = Alignment.CenterStart
) : MeasurePolicy {

    private val effectiveArrangement: Arrangement =
        if (gap > 0.0f && arrangement == Arrangement.Start) Arrangement.spacedBy(gap) else arrangement

    override fun measureWidth(node: LayoutNode): Float {
        val visibleChildren = node.children.filter { it.visible }
        if (visibleChildren.isEmpty()) return 0.0f

        var sumWidth = 0.0f
        for (i in 0 until visibleChildren.size) {
            sumWidth += GodotLayout.getChildMinWidth(visibleChildren[i])
        }
        if (visibleChildren.size > 1) sumWidth += (visibleChildren.size - 1) * effectiveArrangement.spacing
        return sumWidth
    }

    override fun measureHeight(node: LayoutNode, availableWidth: Float): Float {
        val visibleChildren = node.children.filter { it.visible }
        if (visibleChildren.isEmpty()) return 0.0f

        var maxHeight = 0.0f
        for (i in 0 until visibleChildren.size) {
            maxHeight = maxOf(maxHeight, GodotLayout.getChildMinHeight(visibleChildren[i]))
        }
        return maxHeight
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
            arrangement = effectiveArrangement,
            alignment = alignment
        )
    }
}

// --- GRID MEASURE POLICY ---

/**
 * ## GridMeasurePolicy
 *
 * Grid layout policy arranging children in uniform/flexible columns and rows.
 *
 * See: docs/layout-engine/layout_engine_en.md
 */
data class GridMeasurePolicy(
    val columns: Int = 1,
    val horizontalGap: Float = 0.0f,
    val verticalGap: Float = 0.0f
) : MeasurePolicy {

    override fun measureWidth(node: LayoutNode): Float {
        if (columns <= 0) return 0.0f

        val visibleChildren = node.children.filter { it.visible }
        if (visibleChildren.isEmpty()) return 0.0f

        val columnWidths = FloatArray(columns)
        for (i in visibleChildren.indices) {
            val columnIndex = i % columns
            columnWidths[columnIndex] = maxOf(columnWidths[columnIndex], GodotLayout.getChildMinWidth(visibleChildren[i]))
        }
        val totalGaps = if (columns > 1) (columns - 1) * horizontalGap else 0.0f
        return columnWidths.sum() + totalGaps
    }

    override fun measureHeight(node: LayoutNode, availableWidth: Float): Float {
        if (columns <= 0) return 0.0f

        val visibleChildren = node.children.filter { it.visible }
        if (visibleChildren.isEmpty()) return 0.0f

        val rows = (visibleChildren.size + columns - 1) / columns
        val rowHeights = FloatArray(rows)
        for (i in visibleChildren.indices) {
            val rowIndex = i / columns
            rowHeights[rowIndex] = maxOf(rowHeights[rowIndex], GodotLayout.getChildMinHeight(visibleChildren[i]))
        }
        val totalGaps = if (rows > 1) (rows - 1) * verticalGap else 0.0f
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
            horizontalSeparation = horizontalGap,
            verticalSeparation = verticalGap
        )
    }
}
