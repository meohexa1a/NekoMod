@file:Suppress("unused")

package org.mdt.ui.components.layout

import org.mdt.ui.layout.policy.ColumnMeasurePolicy
import org.mdt.ui.layout.policy.GridMeasurePolicy
import org.mdt.ui.layout.policy.RowMeasurePolicy

/**
 * ## RowNode
 *
 * Horizontal linear container node (backward-compatible alias delegating to [RowMeasurePolicy]).
 */
open class RowNode(gap: Float = 0f) : LayoutNode() {
    init {
        measurePolicy = RowMeasurePolicy(gap = gap)
    }
}

/**
 * ## ColumnNode
 *
 * Vertical linear container node (backward-compatible alias delegating to [ColumnMeasurePolicy]).
 */
open class ColumnNode(gap: Float = 0f) : LayoutNode() {
    init {
        measurePolicy = ColumnMeasurePolicy(gap = gap)
    }
}

/**
 * ## GridContainerNode
 *
 * Multi-column grid container node (backward-compatible alias delegating to [GridMeasurePolicy]).
 */
open class GridContainerNode(columns: Int = 2, hGap: Float = 0f, vGap: Float = 0f) : LayoutNode() {
    init {
        measurePolicy = GridMeasurePolicy(columns = columns, hGap = hGap, vGap = vGap)
    }
}

/**
 * ## SpacerNode
 *
 * Spacer node (backward-compatible alias).
 */
open class SpacerNode(w: Float = 0f, h: Float = 0f, stretch: Float = 1f) : LayoutNode() {
    init {
        if (w > 0f) width = w
        if (h > 0f) height = h
        if (stretch > 0f) stretchRatio = stretch
    }
}
