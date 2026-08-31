// [AGENT INVARIANT] Synchronously update @property, @param, and @see KDocs when modifying this file.

@file:Suppress("FunctionName", "unused")

package org.mdt.ui.components.text

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeNode
import arc.graphics.g2d.Font
import arc.util.Align
import org.mdt.core.platform.LocalPlatformHost
import org.mdt.core.ui.compose.NodeApplier
import org.mdt.core.ui.modifier.UIModifier
import org.mdt.core.ui.node.TextNode
import org.mdt.core.ui.unit.Color

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
    wrap: Boolean = false,
    ellipsis: Boolean = false
) {
    val host = LocalPlatformHost.current
    ComposeNode<TextNode, NodeApplier>(
        factory = {
            val node = TextNode(text, hostProvider = { host })
            node.textColor = color
            node.font = font
            node.align = align
            node.wrap = wrap
            node.ellipsis = ellipsis
            node.modifier = modifier
            node
        },
        update = {
            set(modifier) { this.modifier = it }
            set(text) { this.text = it }
            set(color) { this.textColor = it }
            set(font) { this.font = it }
            set(align) { this.align = it }
            set(wrap) { this.wrap = it }
            set(ellipsis) { this.ellipsis = it }
        }
    )
}
