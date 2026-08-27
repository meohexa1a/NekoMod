@file:Suppress("FunctionName", "unused")

package org.mdt.ui.components.text

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeNode
import arc.graphics.Color
import arc.graphics.g2d.Font
import arc.util.Align
import mindustry.ui.Fonts
import org.mdt.core.ui.compose.NodeApplier
import org.mdt.core.ui.compose.UIModifier

/**
 * ## Text
 *
 * Declarative BMFont text rendering component.
 *
 * @param text Content string to display.
 * @param modifier Chainable [UIModifier].
 * @param color Text tint color.
 * @param font Font family ([Fonts.def], [Fonts.monospace], [Fonts.large], [Fonts.outline]).
 * @param scale Font scale multiplier (default 1.0f for pixel-perfect clarity).
 * @param align Horizontal text alignment ([Align.left], [Align.center], [Align.right]).
 * @param wrap Whether to enable multi-line text wrapping.
 * @param ellipsis Truncation string for single-line text (defaults to "..."). Set to null to disable truncation.
 */
@Composable
fun Text(
    text: String,
    modifier: UIModifier = UIModifier,
    color: Color = Color.white,
    font: Font = Fonts.def,
    scale: Float = 1.0f,
    align: Int = Align.left,
    wrap: Boolean = false,
    ellipsis: String? = "..."
) {
    ComposeNode<TextNode, NodeApplier>(
        factory = {
            val node = TextNode(text)
            node.textVisuals.font = font
            node.textVisuals.color.set(color)
            node.textVisuals.fontScale = scale
            node.textVisuals.align = align
            node.textVisuals.wrap = wrap
            node.textVisuals.ellipsis = ellipsis
            modifier.applyTo(node)
            node
        },
        update = {
            set(text) { this.text = it }
            set(font) {
                this.textVisuals.font = it
                invalidateLayout()
            }
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
            set(ellipsis) {
                this.textVisuals.ellipsis = it
                invalidateLayout()
            }
            set(modifier) {
                it.applyTo(this)
                invalidateLayout()
            }
        }
    )
}

/**
 * ## MonoText
 *
 * Declarative clean technical monospace text rendering component.
 * Uses [Fonts.monospace] if available, falling back to [Fonts.def] for crystal clear readability.
 *
 * @param text Content string to display.
 * @param modifier Chainable [UIModifier].
 * @param color Text tint color.
 * @param scale Font scale multiplier (default 1.0f).
 * @param align Horizontal text alignment ([Align.left], [Align.center], [Align.right]).
 * @param wrap Whether to enable multi-line text wrapping.
 * @param ellipsis Truncation string for single-line text (defaults to "..."). Set to null to disable truncation.
 */
@Composable
fun MonoText(
    text: String,
    modifier: UIModifier = UIModifier,
    color: Color = Color.white,
    scale: Float = 1.0f,
    align: Int = Align.left,
    wrap: Boolean = false,
    ellipsis: String? = "..."
) {
    val cleanFont = Fonts.monospace ?: Fonts.def
    Text(
        text = text,
        modifier = modifier,
        color = color,
        font = cleanFont,
        scale = scale,
        align = align,
        wrap = wrap,
        ellipsis = ellipsis
    )
}
