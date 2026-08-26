@file:Suppress("FunctionName", "unused")

package org.mdt.ui.components.layout

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeNode
import org.mdt.core.ui.compose.GridScope
import org.mdt.core.ui.compose.NodeApplier
import org.mdt.core.ui.compose.UIModifier
import org.mdt.core.ui.layout.policy.GridMeasurePolicy

/**
 * ## Grid
 *
 * Multi-column grid container layout.
 */
@Composable
fun Grid(
    columns: Int = 2,
    hGap: Float = 0f,
    vGap: Float = 0f,
    modifier: UIModifier = UIModifier,
    content: @Composable GridScope.() -> Unit
) {
    ComposeNode<LayoutNode, NodeApplier>(
        factory = {
            val node = LayoutNode()
            node.measurePolicy = GridMeasurePolicy(columns = columns, hGap = hGap, vGap = vGap)
            modifier.applyTo(node)
            node
        },
        update = {
            set(columns) {
                this.measurePolicy = GridMeasurePolicy(columns = it, hGap = hGap, vGap = vGap)
                invalidateLayout()
            }
            set(hGap) {
                this.measurePolicy = GridMeasurePolicy(columns = columns, hGap = it, vGap = vGap)
                invalidateLayout()
            }
            set(vGap) {
                this.measurePolicy = GridMeasurePolicy(columns = columns, hGap = hGap, vGap = it)
                invalidateLayout()
            }
            set(modifier) {
                it.applyTo(this)
                invalidateLayout()
            }
        },
        content = {
            GridScope.content()
        }
    )
}
