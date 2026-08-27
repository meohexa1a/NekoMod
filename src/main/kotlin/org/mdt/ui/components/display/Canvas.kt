package org.mdt.ui.components.display

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeNode
import org.mdt.core.ui.compose.NodeApplier
import org.mdt.core.ui.compose.UIModifier
import org.mdt.core.ui.render.EngineRenderer
import org.mdt.ui.components.layout.LayoutNode

/**
 * ## CustomDrawNode
 *
 * Virtual DOM primitive node executing a direct GPU draw callback during the render pass.
 *
 * See: docs/design-system/design_system_en.md
 */
open class CustomDrawNode : LayoutNode() {
    var onDraw: ((EngineRenderer) -> Unit)? = null

    override fun drawSelf(renderer: EngineRenderer) {
        super.drawSelf(renderer)
        onDraw?.invoke(renderer)
    }
}

/**
 * ## Canvas
 *
 * Declarative custom drawing surface component (standard Compose [Canvas] parity).
 * Allows screens and widgets to render custom OpenGL shaders, 3D backgrounds,
 * particle systems, or native game engine renderers directly onto the screen.
 *
 * @param modifier Chainable [UIModifier] to configure size, anchors, and bounds.
 * @param onDraw Callback invoked during the render pass with the active [EngineRenderer].
 *
 * See: docs/design-system/design_system_en.md
 */
@Composable
fun Canvas(
    modifier: UIModifier = UIModifier,
    onDraw: (EngineRenderer) -> Unit
) {
    ComposeNode<CustomDrawNode, NodeApplier>(
        factory = {
            val node = CustomDrawNode()
            node.onDraw = onDraw
            modifier.applyTo(node)
            node
        },
        update = {
            set(onDraw) { this.onDraw = it }
            set(modifier) {
                it.applyTo(this)
                invalidateLayout()
            }
        }
    )
}
