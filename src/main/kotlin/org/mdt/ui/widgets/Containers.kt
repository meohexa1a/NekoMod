package org.mdt.ui.widgets

import org.mdt.ui.layout.Arrangement
import org.mdt.ui.layout.GodotLayout
import org.mdt.ui.layout.SizeFlags

/**
 * ## BoxContainerNode
 *
 * Base container for linear layouts ([RowNode] and [ColumnNode]).
 * Implements Godot's 2-pass box layout algorithm supporting flexible stretch ratios,
 * arrangement spacing ([Arrangement]), and margin/padding insets.
 *
 * See: docs/layout-engine/layout_engine_en.md
 */
open class BoxContainerNode(val isVertical: Boolean = false) : BoxNode() {
    /** Content distribution and spacing mode. */
    var arrangement: Arrangement = Arrangement.Start
        set(value) {
            if (field != value) {
                field = value
                invalidateLayout()
            }
        }

    /** Inter-item gap spacing (convenience alias for [Arrangement.spacing]). */
    var gap: Float
        get() = arrangement.spacing
        set(value) {
            if (arrangement.spacing != value) {
                arrangement = arrangement.copy(spacing = value)
                invalidateLayout()
            }
        }

    override fun getPrefWidth(): Float {
        if (width >= 0f) return width
        val visibleChildren = children.filter { it.visible }
        if (visibleChildren.isEmpty()) return if (minWidth >= 0f) minWidth else padL + padR

        val content = if (isVertical) {
            var maxW = 0f
            for (child in visibleChildren) {
                maxW = maxOf(maxW, GodotLayout.getChildMinWidth(child))
            }
            maxW
        } else {
            var sum = 0f
            for (child in visibleChildren) {
                sum += GodotLayout.getChildMinWidth(child)
            }
            if (visibleChildren.size > 1) sum += (visibleChildren.size - 1) * arrangement.spacing
            sum
        }
        val total = content + padL + padR
        return if (minWidth >= 0f) maxOf(total, minWidth) else total
    }

    override fun getPrefHeight(): Float {
        if (height >= 0f) return height
        val visibleChildren = children.filter { it.visible }
        if (visibleChildren.isEmpty()) return if (minHeight >= 0f) minHeight else padT + padB

        val content = if (isVertical) {
            var sum = 0f
            for (child in visibleChildren) {
                sum += GodotLayout.getChildMinHeight(child)
            }
            if (visibleChildren.size > 1) sum += (visibleChildren.size - 1) * arrangement.spacing
            sum
        } else {
            var maxH = 0f
            for (child in visibleChildren) {
                maxH = maxOf(maxH, GodotLayout.getChildMinHeight(child))
            }
            maxH
        }
        val total = content + padT + padB
        return if (minHeight >= 0f) maxOf(total, minHeight) else total
    }

    override fun layout() {
        val w = if (bounds.width > 0f) bounds.width else getPrefWidth()
        val h = if (bounds.height > 0f) bounds.height else getPrefHeight()
        if (bounds.width != w || bounds.height != h) {
            setSize(w, h)
        }

        GodotLayout.layoutBox(
            children = children,
            parentX = bounds.x, parentY = bounds.y,
            parentW = bounds.width, parentH = bounds.height,
            padL = padL, padT = padT, padR = padR, padB = padB,
            isVertical = isVertical,
            arrangement = arrangement
        )

        isLayoutDirty = false
        for (child in children) {
            if (child.visible) {
                child.layout()
            }
        }
    }
}

/**
 * ## RowNode
 *
 * Horizontal linear container node stacking children from Left to Right.
 */
class RowNode(gap: Float = 0f) : BoxContainerNode(isVertical = false) {
    init {
        this.arrangement = Arrangement.spacedBy(gap)
        sizeFlagsHorizontal = SizeFlags.EXPAND_FILL
        sizeFlagsVertical = SizeFlags.SHRINK_CENTER
    }
}

/**
 * ## ColumnNode
 *
 * Vertical linear container node stacking children from Top to Bottom.
 */
class ColumnNode(gap: Float = 0f) : BoxContainerNode(isVertical = true) {
    init {
        this.arrangement = Arrangement.spacedBy(gap)
        sizeFlagsHorizontal = SizeFlags.EXPAND_FILL
        sizeFlagsVertical = SizeFlags.SHRINK_BEGIN
    }
}

/**
 * ## GridContainerNode
 *
 * Multi-column and multi-row layout container arranging children in an organized grid table.
 */
class GridContainerNode(var columns: Int = 2, var gap: Float = 4f) : BoxNode() {
    override fun layout() {
        val w = if (bounds.width > 0f) bounds.width else getPrefWidth()
        val h = if (bounds.height > 0f) bounds.height else getPrefHeight()
        if (bounds.width != w || bounds.height != h) {
            setSize(w, h)
        }

        GodotLayout.layoutGrid(
            children = children,
            parentX = bounds.x, parentY = bounds.y,
            parentW = bounds.width, parentH = bounds.height,
            padL = padL, padT = padT, padR = padR, padB = padB,
            columns = columns,
            hSeparation = gap,
            vSeparation = gap
        )

        isLayoutDirty = false
        for (child in children) {
            if (child.visible) {
                child.layout()
            }
        }
    }
}

/**
 * ## SpacerNode
 *
 * Flexible or fixed-size empty spacer node for adjusting layout spacing.
 */
class SpacerNode(w: Float = 0f, h: Float = 0f, stretch: Float = 1f) : BoxNode() {
    init {
        visuals.opacity = 0.0f
        if (w > 0f || h > 0f) {
            width = w; minWidth = w; maxWidth = w
            height = h; minHeight = h; maxHeight = h
            sizeFlagsHorizontal = SizeFlags.SHRINK_BEGIN
            sizeFlagsVertical = SizeFlags.SHRINK_BEGIN
        } else {
            sizeFlagsHorizontal = SizeFlags.EXPAND_FILL
            sizeFlagsVertical = SizeFlags.EXPAND_FILL
            stretchRatio = stretch
        }
    }
}
