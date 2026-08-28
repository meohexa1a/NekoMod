@file:Suppress("FunctionName", "unused")

package org.mdt.ui.components.text

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeNode
import arc.graphics.g2d.Font
import arc.util.Align
import org.mdt.core.ui.compose.NodeApplier
import org.mdt.core.ui.compose.UIModifier
import org.mdt.core.ui.compose.fillMaxWidth
import org.mdt.core.ui.graphics.Color

/**
 * ## Text
 *
 * Declarative BMFont text composable.
 *
 * @param text The string to display.
 * @param modifier Chainable [UIModifier].
 * @param color Text color.
 * @param font Custom font instance (defaults to [mindustry.ui.Fonts.def]).
 * @param align Alignment mode ([Align.left], [Align.center], [Align.right]).
 * @param wrap Whether long text should wrap onto multiple lines.
 *
 * See: docs/components-guide/components_guide_en.md
 */
@Composable
fun Text(
    text: String,
    modifier: UIModifier = UIModifier,
    color: Color = Color.White,
    font: Font? = null,
    align: Int = Align.left,
    wrap: Boolean = false
) {
    val effectiveModifier = if (wrap) UIModifier.fillMaxWidth().then(modifier) else modifier
    ComposeNode<TextNode, NodeApplier>(
        factory = {
            val node = TextNode(text)
            node.textColor = color
            node.font = font
            node.align = align
            node.wrap = wrap
            effectiveModifier.applyTo(node)
            node
        },
        update = {
            set(text) { this.text = it }
            set(color) { this.textColor = it }
            set(font) { this.font = it }
            set(align) { this.align = it }
            set(wrap) { this.wrap = it }
            set(effectiveModifier) {
                it.applyTo(this)
                invalidateLayout()
            }
        }
    )
}
