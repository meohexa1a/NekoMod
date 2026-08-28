@file:Suppress("FunctionName", "unused")

package org.mdt.ui.components.layout

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeNode
import org.mdt.core.ui.compose.ColumnScope
import org.mdt.core.ui.compose.NodeApplier
import org.mdt.core.ui.compose.RowScope
import org.mdt.core.ui.compose.UIModifier
import org.mdt.core.ui.layout.Alignment
import org.mdt.core.ui.layout.Arrangement
import org.mdt.core.ui.layout.ColumnMeasurePolicy
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
    content: @Composable RowScope.() -> Unit
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
                (measurePolicy as? RowMeasurePolicy)?.let {
                    measurePolicy = it.copy(arrangement = arrangement)
                } ?: run {
                    measurePolicy = RowMeasurePolicy(arrangement = arrangement, alignment = alignment)
                }
                invalidateLayout()
            }
            set(alignment) {
                (measurePolicy as? RowMeasurePolicy)?.let {
                    measurePolicy = it.copy(alignment = alignment)
                } ?: run {
                    measurePolicy = RowMeasurePolicy(arrangement = arrangement, alignment = alignment)
                }
                invalidateLayout()
            }
            set(modifier) {
                it.applyTo(this)
                invalidateLayout()
            }
        },
        content = {
            RowScope.content()
        }
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
    content: @Composable ColumnScope.() -> Unit
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
                (measurePolicy as? ColumnMeasurePolicy)?.let {
                    measurePolicy = it.copy(arrangement = arrangement)
                } ?: run {
                    measurePolicy = ColumnMeasurePolicy(arrangement = arrangement, alignment = alignment)
                }
                invalidateLayout()
            }
            set(alignment) {
                (measurePolicy as? ColumnMeasurePolicy)?.let {
                    measurePolicy = it.copy(alignment = alignment)
                } ?: run {
                    measurePolicy = ColumnMeasurePolicy(arrangement = arrangement, alignment = alignment)
                }
                invalidateLayout()
            }
            set(modifier) {
                it.applyTo(this)
                invalidateLayout()
            }
        },
        content = {
            ColumnScope.content()
        }
    )
}
