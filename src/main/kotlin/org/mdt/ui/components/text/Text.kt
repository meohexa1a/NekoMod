@file:Suppress("FunctionName", "unused")

package org.mdt.ui.components.text

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeNode
import arc.graphics.Color
import arc.util.Align
import org.mdt.ui.compose.NodeApplier
import org.mdt.ui.compose.UIModifier

/**
 * ## Text
 *
 * Declarative BMFont text rendering component.
 *
 * @param text Content string to display.
 * @param modifier Chainable [UIModifier].
 * @param color Text tint color.
 * @param scale Font scale multiplier.
 * @param align Horizontal text alignment ([Align.left], [Align.center], [Align.right]).
 * @param wrap Whether to enable multi-line text wrapping.
 */
@Composable
fun Text(
    text: String,
    modifier: UIModifier = UIModifier,
    color: Color = Color.white,
    scale: Float = 1.0f,
    align: Int = Align.left,
    wrap: Boolean = false
) {
    ComposeNode<TextNode, NodeApplier>(
        factory = {
            val node = TextNode(text)
            node.textVisuals.color.set(color)
            node.textVisuals.fontScale = scale
            node.textVisuals.align = align
            node.textVisuals.wrap = wrap
            modifier.applyTo(node)
            node
        },
        update = {
            set(text) { this.text = it }
            set(color) { this.textVisuals.color.set(it) }
            set(scale) {
                this.textVisuals.fontScale = it
                invalidateLayout()
            }
            set(align) { this.textVisuals.align = it }
            set(wrap) {
                this.textVisuals.wrap = it
                invalidateLayout()
            }
            set(modifier) {
                it.applyTo(this)
                invalidateLayout()
            }
        }
    )
}
