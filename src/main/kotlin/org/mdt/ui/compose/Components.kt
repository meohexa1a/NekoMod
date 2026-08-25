@file:Suppress("FunctionName")

package org.mdt.ui.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeNode
import arc.graphics.Color
import arc.util.Align
import org.mdt.ui.core.UINode
import org.mdt.ui.layout.Arrangement
import org.mdt.ui.widgets.*

private fun UINode.applyToModifier(modifier: UIModifier) {
    modifier.applyTo(this)
}

/**
 * ## Box
 *
 * Fundamental container component. Wraps child composables inside a rectangular [BoxNode]
 * with customizable background, border, padding, and layout modifiers.
 *
 * @param modifier Chainable [UIModifier] configuring layout, box model, and styling.
 * @param content Scoped child composable content within [BoxScope].
 *
 * @sample
 * ```kotlin
 * Box(
 *     modifier = Modifier
 *         .pad(16f)
 *         .background(Color.darkGray)
 *         .radius(8.0)
 * ) {
 *     Text("Inside Box")
 * }
 * ```
 */
@Composable
fun Box(
    modifier: UIModifier = UIModifier,
    content: @Composable BoxScope.() -> Unit = {}
) {
    ComposeNode<BoxNode, NodeApplier>(
        factory = {
            val node = BoxNode()
            modifier.applyTo(node)
            node
        },
        update = {
            set(modifier) {
                this.applyToModifier(it)
                invalidateLayout()
            }
        },
        content = { BoxScope.Instance.content() }
    )
}

/**
 * ## Text
 *
 * Renders BMFont text with localization support, scaling, alignment, and auto text wrapping.
 *
 * @param text The string to display. Automatically resolves keys starting with `$` or `@`.
 * @param modifier Chainable [UIModifier] for padding, margin, or layout sizing.
 * @param color Text tint color.
 * @param scale Typography font scaling factor (e.g. 1.0 = default size).
 * @param align Arc [Align] alignment flag (e.g. [Align.left], [Align.center]).
 * @param wrap Whether to enable multi-line text wrapping.
 * @param ellipsis Optional suffix string when text overflows available width.
 */
@Composable
fun Text(
    text: String,
    modifier: UIModifier = UIModifier,
    color: Color = Color.white,
    scale: Float = 1.0f,
    align: Int = Align.left,
    wrap: Boolean = false,
    ellipsis: String? = null
) {
    ComposeNode<TextNode, NodeApplier>(
        factory = {
            val node = TextNode(text)
            node.visuals.color.set(color)
            node.visuals.scale(scale)
            node.visuals.align(align)
            node.visuals.wrap = wrap
            node.visuals.ellipsis = ellipsis
            node.renderer.invalidate()
            modifier.applyTo(node)
            node
        },
        update = {
            set(text) { this.text = it }
            set(color) { this.visuals.color.set(it); this.renderer.invalidate() }
            set(scale) { this.visuals.scale(it); this.renderer.invalidate(); invalidateLayout() }
            set(align) { this.visuals.align(it); this.renderer.invalidate() }
            set(wrap) { this.visuals.wrap = it; this.renderer.invalidate(); invalidateLayout() }
            set(ellipsis) { this.visuals.ellipsis = it; this.renderer.invalidate(); invalidateLayout() }
            set(modifier) {
                this.applyToModifier(it)
                invalidateLayout()
            }
        }
    )
}

/**
 * ## Row
 *
 * Horizontal linear container stacking children horizontally from Left to Right.
 *
 * @param modifier Chainable [UIModifier] for sizing, padding, and background styling.
 * @param arrangement Content distribution and spacing mode (e.g. [Arrangement.spacedBy]).
 * @param gap Inter-element horizontal gap in pixels.
 * @param content Scoped child composables within [RowScope].
 */
@Composable
fun Row(
    modifier: UIModifier = UIModifier,
    arrangement: Arrangement = Arrangement.Start,
    gap: Float = 0f,
    content: @Composable RowScope.() -> Unit = {}
) {
    val finalArrangement = if (gap > 0f && arrangement === Arrangement.Start) Arrangement.spacedBy(gap) else arrangement
    ComposeNode<RowNode, NodeApplier>(
        factory = {
            val node = RowNode()
            node.arrangement = finalArrangement
            modifier.applyTo(node)
            node
        },
        update = {
            set(finalArrangement) { this.arrangement = it }
            set(modifier) {
                this.applyToModifier(it)
                invalidateLayout()
            }
        },
        content = { RowScope.Instance.content() }
    )
}

/**
 * ## Column
 *
 * Vertical linear container stacking children vertically from Top to Bottom.
 *
 * @param modifier Chainable [UIModifier] for sizing, padding, and background styling.
 * @param arrangement Content distribution and spacing mode (e.g. [Arrangement.spacedBy]).
 * @param gap Inter-element vertical gap in pixels.
 * @param content Scoped child composables within [ColumnScope].
 */
@Composable
fun Column(
    modifier: UIModifier = UIModifier,
    arrangement: Arrangement = Arrangement.Start,
    gap: Float = 0f,
    content: @Composable ColumnScope.() -> Unit = {}
) {
    val finalArrangement = if (gap > 0f && arrangement === Arrangement.Start) Arrangement.spacedBy(gap) else arrangement
    ComposeNode<ColumnNode, NodeApplier>(
        factory = {
            val node = ColumnNode()
            node.arrangement = finalArrangement
            modifier.applyTo(node)
            node
        },
        update = {
            set(finalArrangement) { this.arrangement = it }
            set(modifier) {
                this.applyToModifier(it)
                invalidateLayout()
            }
        },
        content = { ColumnScope.Instance.content() }
    )
}

/**
 * ## Grid
 *
 * Multi-column and multi-row table grid layout.
 *
 * @param columns Number of columns in the grid.
 * @param gap Inter-cell separation gap in pixels.
 * @param modifier Chainable [UIModifier].
 * @param content Scoped child composables within [GridScope].
 */
@Composable
fun Grid(
    columns: Int = 2,
    gap: Float = 4f,
    modifier: UIModifier = UIModifier,
    content: @Composable GridScope.() -> Unit = {}
) {
    ComposeNode<GridContainerNode, NodeApplier>(
        factory = {
            val node = GridContainerNode(columns, gap)
            modifier.applyTo(node)
            node
        },
        update = {
            set(columns) { this.columns = it; invalidateLayout() }
            set(gap) { this.gap = it; invalidateLayout() }
            set(modifier) {
                this.applyToModifier(it)
                invalidateLayout()
            }
        },
        content = { GridScope.Instance.content() }
    )
}

/**
 * ## Spacer
 *
 * Empty spacer element used for creating fixed gaps or auto-expanding flexible space.
 *
 * @param modifier Chainable [UIModifier].
 * @param width Fixed width in pixels (0f for flexible).
 * @param height Fixed height in pixels (0f for flexible).
 * @param weight Expansion weight ratio when occupying free space.
 */
@Composable
fun Spacer(
    modifier: UIModifier = UIModifier,
    width: Float = 0f,
    height: Float = 0f,
    weight: Float = 0f
) {
    ComposeNode<SpacerNode, NodeApplier>(
        factory = {
            val node = SpacerNode(width, height, if (weight > 0f) weight else 1f)
            modifier.applyTo(node)
            node
        },
        update = {
            set(modifier) {
                this.applyToModifier(it)
                invalidateLayout()
            }
        }
    )
}
