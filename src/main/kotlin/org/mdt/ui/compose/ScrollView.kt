@file:Suppress("FunctionName")

package org.mdt.ui.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeNode
import org.mdt.ui.widgets.ScrollContainerNode

/**
 * ## ScrollView
 *
 * Declarative scrollable viewport container with automatic hardware clipping
 * and smooth mouse-wheel interaction.
 *
 * @param modifier Chainable [UIModifier].
 * @param enableVertical Whether vertical scrolling is enabled.
 * @param enableHorizontal Whether horizontal scrolling is enabled.
 * @param content Scoped child composable content within [BoxScope].
 */
@Composable
fun ScrollView(
    modifier: UIModifier = UIModifier,
    enableVertical: Boolean = true,
    enableHorizontal: Boolean = false,
    content: @Composable BoxScope.() -> Unit
) {
    ComposeNode<ScrollContainerNode, NodeApplier>(
        factory = {
            val node = ScrollContainerNode()
            node.enableVertical = enableVertical
            node.enableHorizontal = enableHorizontal
            modifier.applyTo(node)
            node
        },
        update = {
            set(enableVertical) { this.enableVertical = it; invalidateLayout() }
            set(enableHorizontal) { this.enableHorizontal = it; invalidateLayout() }
            set(modifier) {
                it.applyTo(this)
                invalidateLayout()
            }
        },
        content = { BoxScope.Instance.content() }
    )
}
