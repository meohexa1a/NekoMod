@file:Suppress("FunctionName", "unused")

package org.mdt.ui.components.layout

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeNode
import org.mdt.core.ui.compose.BoxScope
import org.mdt.core.ui.compose.NodeApplier
import org.mdt.core.ui.compose.UIModifier
import org.mdt.core.ui.layout.BoxMeasurePolicy

/**
 * ## Box
 *
 * Fundamental container layout positioning children relative to anchor points.
 *
 * See: docs/ui-components/ui_components_en.md
 */
@Composable
fun Box(
    modifier: UIModifier = UIModifier,
    content: @Composable BoxScope.() -> Unit = {}
) {
    ComposeNode<LayoutNode, NodeApplier>(
        factory = {
            val node = LayoutNode()
            node.measurePolicy = BoxMeasurePolicy
            modifier.applyTo(node)
            node
        },
        update = {
            set(modifier) {
                it.applyTo(this)
                invalidateLayout()
            }
        },
        content = {
            BoxScope.content()
        }
    )
}
