package org.mdt.ui.layout.policy

import org.mdt.ui.components.layout.LayoutNode
import org.mdt.ui.layout.Alignment
import org.mdt.ui.layout.Arrangement
import org.mdt.ui.layout.GodotLayout

/**
 * ## MeasurePolicy
 *
 * Defines the measurement and positioning strategy for a [LayoutNode] and its children.
 *
 * See: docs/layout-engine/layout_engine_en.md
 */
interface MeasurePolicy {
    fun measureWidth(node: LayoutNode): Float
    fun measureHeight(node: LayoutNode): Float
    fun layout(node: LayoutNode, innerX: Float, innerY: Float, availW: Float, availH: Float)
}

/**
 * ## BoxMeasurePolicy
 *
 * Default layout policy arranging children via Godot-style anchors and bounds fitting.
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
        GodotLayout.layoutAnchors(node.children, availW, availH)
    }
}

/**
 * ## ColumnMeasurePolicy
 *
 * Vertical linear layout policy arranging children top-to-bottom.
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
 * Horizontal linear layout policy arranging children left-to-right.
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

    override fun measureHeight(node: LayoutNode): Float {
        val visibleChildren = node.children.filter { it.visible }
        if (visibleChildren.isEmpty()) return 0f
        var maxH = 0f
        for (child in visibleChildren) {
            maxH = maxOf(maxH, GodotLayout.getChildMinHeight(child))
        }
        return maxH
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
}

/**
 * ## GridMeasurePolicy
 *
 * Multi-column grid table layout policy.
 */
data class GridMeasurePolicy(
    val columns: Int = 2,
    val hGap: Float = 0f,
    val vGap: Float = 0f
) : MeasurePolicy {
    override fun measureWidth(node: LayoutNode): Float = 0f
    override fun measureHeight(node: LayoutNode): Float = 0f

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
