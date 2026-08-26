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
import org.mdt.core.ui.layout.policy.ColumnMeasurePolicy
import org.mdt.core.ui.layout.policy.RowMeasurePolicy

/**
 * ## Row
 *
 * Horizontal linear layout positioning children left-to-right.
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
            node.measurePolicy = RowMeasurePolicy(
                gap = arrangement.spacing,
                arrangement = arrangement,
                alignment = alignment
            )
            modifier.applyTo(node)
            node
        },
        update = {
            set(arrangement) {
                this.measurePolicy = RowMeasurePolicy(
                    gap = it.spacing,
                    arrangement = it,
                    alignment = alignment
                )
                invalidateLayout()
            }
            set(alignment) {
                this.measurePolicy = RowMeasurePolicy(
                    gap = arrangement.spacing,
                    arrangement = arrangement,
                    alignment = it
                )
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

/**
 * ## Column
 *
 * Vertical linear layout positioning children top-to-bottom.
 */
@Composable
fun Column(
    modifier: UIModifier = UIModifier,
    gap: Float = 0f,
    arrangement: Arrangement = Arrangement.Start,
    alignment: Alignment = Alignment.TopStart,
    content: @Composable ColumnScope.() -> Unit
) {
    val actualArrangement = if (gap > 0f && arrangement == Arrangement.Start) {
        Arrangement.spacedBy(gap)
    } else arrangement

    ComposeNode<LayoutNode, NodeApplier>(
        factory = {
            val node = LayoutNode()
            node.measurePolicy = ColumnMeasurePolicy(
                gap = actualArrangement.spacing,
                arrangement = actualArrangement,
                alignment = alignment
            )
            modifier.applyTo(node)
            node
        },
        update = {
            set(actualArrangement) {
                this.measurePolicy = ColumnMeasurePolicy(
                    gap = it.spacing,
                    arrangement = it,
                    alignment = alignment
                )
                invalidateLayout()
            }
            set(alignment) {
                this.measurePolicy = ColumnMeasurePolicy(
                    gap = actualArrangement.spacing,
                    arrangement = actualArrangement,
                    alignment = it
                )
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
