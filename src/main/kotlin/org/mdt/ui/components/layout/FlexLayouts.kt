// [AGENT INVARIANT] Synchronously update @property, @param, and @see KDocs when modifying this file.

package org.mdt.ui.components.layout

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeNode
import org.mdt.core.ui.compose.NodeApplier
import org.mdt.core.ui.compose.UIModifier
import org.mdt.core.ui.layout.Alignment
import org.mdt.core.ui.layout.Arrangement
import org.mdt.core.ui.layout.ColumnMeasurePolicy
import org.mdt.core.ui.layout.FlowRowMeasurePolicy
import org.mdt.core.ui.layout.RowMeasurePolicy
import org.mdt.core.ui.node.LayoutNode

// --- ROW COMPOSABLE ---

/**
 * ## Row
 *
 * Horizontal linear layout arranging child elements left-to-right.
 *
 * See: docs/components-guide/components_guide_en.md
 */
@Composable
fun Row(
    modifier: UIModifier = UIModifier,
    arrangement: Arrangement = Arrangement.Start,
    alignment: Alignment = Alignment.CenterStart,
    content: @Composable () -> Unit
) {
    ComposeNode<LayoutNode, NodeApplier>(
        factory = {
            val node = LayoutNode()
            node.measurePolicy = RowMeasurePolicy(arrangement = arrangement, alignment = alignment)
            modifier.applyTo(node)
            node
        },
        update = {
            set(arrangement) {
                val currentPolicy = measurePolicy
                measurePolicy = when (currentPolicy) {
                    is RowMeasurePolicy -> currentPolicy.copy(arrangement = arrangement)
                    else -> RowMeasurePolicy(arrangement = arrangement, alignment = alignment)
                }
                invalidateLayout()
            }
            set(alignment) {
                val currentPolicy = measurePolicy
                measurePolicy = when (currentPolicy) {
                    is RowMeasurePolicy -> currentPolicy.copy(alignment = alignment)
                    else -> RowMeasurePolicy(arrangement = arrangement, alignment = alignment)
                }
                invalidateLayout()
            }
            set(modifier) {
                it.applyTo(this)
                invalidateLayout()
            }
        },
        content = content
    )
}

// --- COLUMN COMPOSABLE ---

/**
 * ## Column
 *
 * Vertical linear layout arranging child elements top-to-bottom.
 *
 * See: docs/components-guide/components_guide_en.md
 */
@Composable
fun Column(
    modifier: UIModifier = UIModifier,
    arrangement: Arrangement = Arrangement.Start,
    alignment: Alignment = Alignment.TopStart,
    content: @Composable () -> Unit
) {
    ComposeNode<LayoutNode, NodeApplier>(
        factory = {
            val node = LayoutNode()
            node.measurePolicy = ColumnMeasurePolicy(arrangement = arrangement, alignment = alignment)
            modifier.applyTo(node)
            node
        },
        update = {
            set(arrangement) {
                val currentPolicy = measurePolicy
                measurePolicy = when (currentPolicy) {
                    is ColumnMeasurePolicy -> currentPolicy.copy(arrangement = arrangement)
                    else -> ColumnMeasurePolicy(arrangement = arrangement, alignment = alignment)
                }
                invalidateLayout()
            }
            set(alignment) {
                val currentPolicy = measurePolicy
                measurePolicy = when (currentPolicy) {
                    is ColumnMeasurePolicy -> currentPolicy.copy(alignment = alignment)
                    else -> ColumnMeasurePolicy(arrangement = arrangement, alignment = alignment)
                }
                invalidateLayout()
            }
            set(modifier) {
                it.applyTo(this)
                invalidateLayout()
            }
        },
        content = content
    )
}

// --- FLOW ROW COMPOSABLE (FLEX WRAP) ---

/**
 * ## FlowRow
 *
 * Multi-line wrapping horizontal flex layout that wraps children onto new rows when container width is exceeded.
 */
@Composable
fun FlowRow(
    modifier: UIModifier = UIModifier,
    horizontalGap: Float = 8.0f,
    verticalGap: Float = 8.0f,
    arrangement: Arrangement = Arrangement.Start,
    alignment: Alignment = Alignment.CenterStart,
    content: @Composable () -> Unit
) {
    ComposeNode<LayoutNode, NodeApplier>(
        factory = {
            val node = LayoutNode()
            node.measurePolicy = FlowRowMeasurePolicy(
                horizontalGap = horizontalGap,
                verticalGap = verticalGap,
                arrangement = arrangement,
                alignment = alignment
            )
            modifier.applyTo(node)
            node
        },
        update = {
            set(horizontalGap) {
                val currentPolicy = measurePolicy
                measurePolicy = when (currentPolicy) {
                    is FlowRowMeasurePolicy -> currentPolicy.copy(horizontalGap = horizontalGap)
                    else -> FlowRowMeasurePolicy(horizontalGap, verticalGap, arrangement, alignment)
                }
                invalidateLayout()
            }
            set(verticalGap) {
                val currentPolicy = measurePolicy
                measurePolicy = when (currentPolicy) {
                    is FlowRowMeasurePolicy -> currentPolicy.copy(verticalGap = verticalGap)
                    else -> FlowRowMeasurePolicy(horizontalGap, verticalGap, arrangement, alignment)
                }
                invalidateLayout()
            }
            set(arrangement) {
                val currentPolicy = measurePolicy
                measurePolicy = when (currentPolicy) {
                    is FlowRowMeasurePolicy -> currentPolicy.copy(arrangement = arrangement)
                    else -> FlowRowMeasurePolicy(horizontalGap, verticalGap, arrangement, alignment)
                }
                invalidateLayout()
            }
            set(alignment) {
                val currentPolicy = measurePolicy
                measurePolicy = when (currentPolicy) {
                    is FlowRowMeasurePolicy -> currentPolicy.copy(alignment = alignment)
                    else -> FlowRowMeasurePolicy(horizontalGap, verticalGap, arrangement, alignment)
                }
                invalidateLayout()
            }
            set(modifier) {
                it.applyTo(this)
                invalidateLayout()
            }
        },
        content = content
    )
}


