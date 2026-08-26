@file:Suppress("FunctionName", "unused")

package org.mdt.ui.components.layout

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeNode
import org.mdt.core.ui.compose.NodeApplier
import org.mdt.core.ui.compose.UIModifier
import org.mdt.core.ui.compose.height
import org.mdt.core.ui.compose.width

/**
 * ## Spacer
 *
 * Empty layout space provider for adding fixed gaps or flexible expansions.
 */
@Composable
fun Spacer(
    modifier: UIModifier = UIModifier
) {
    ComposeNode<LayoutNode, NodeApplier>(
        factory = {
            val node = LayoutNode()
            modifier.applyTo(node)
            node
        },
        update = {
            set(modifier) {
                it.applyTo(this)
                invalidateLayout()
            }
        }
    )
}

@Composable
fun Spacer(width: Float = 0f, height: Float = 0f) {
    var mod: UIModifier = UIModifier
    if (width > 0f) mod = mod.width(width)
    if (height > 0f) mod = mod.height(height)
    Spacer(mod)
}
