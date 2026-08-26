@file:Suppress("FunctionName", "unused")

package org.mdt.ui.components.scroll

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeNode
import org.mdt.core.ui.compose.NodeApplier
import org.mdt.core.ui.compose.UIModifier
import org.mdt.core.ui.layout.ColumnMeasurePolicy
import org.mdt.core.ui.layout.MeasurePolicy

/**
 * ## ScrollView
 *
 * Declarative scrollable viewport container for long content lists.
 *
 * @param modifier Chainable [UIModifier].
 * @param enableVertical Enables vertical scrolling.
 * @param enableHorizontal Enables horizontal scrolling.
 * @param measurePolicy Internal layout measurement strategy.
 * @param content Composable children block.
 */
@Composable
fun ScrollView(
    modifier: UIModifier = UIModifier,
    enableVertical: Boolean = true,
    enableHorizontal: Boolean = false,
    measurePolicy: MeasurePolicy = ColumnMeasurePolicy(gap = 0f),
    content: @Composable () -> Unit
) {
    ComposeNode<ScrollContainerNode, NodeApplier>(
        factory = {
            val node = ScrollContainerNode()
            node.enableVertical = enableVertical
            node.enableHorizontal = enableHorizontal
            node.measurePolicy = measurePolicy
            modifier.applyTo(node)
            node
        },
        update = {
            set(enableVertical) { this.enableVertical = it; invalidateLayout() }
            set(enableHorizontal) { this.enableHorizontal = it; invalidateLayout() }
            set(measurePolicy) { this.measurePolicy = it; invalidateLayout() }
            set(modifier) {
                it.applyTo(this)
                invalidateLayout()
            }
        },
        content = content
    )
}
