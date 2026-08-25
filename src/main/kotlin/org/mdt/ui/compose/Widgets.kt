@file:Suppress("FunctionName")

package org.mdt.ui.compose

import androidx.compose.runtime.Composable
import arc.graphics.Color
import arc.util.Align

/**
 * ## ButtonColors
 *
 * Color palette state configuration for interactive buttons.
 */
data class ButtonColors(
    val fill: Color = Color.valueOf("2a2a3a"),
    val hover: Color = Color.valueOf("45475a"),
    val pressed: Color = Color.valueOf("585b70"),
    val content: Color = Color.white,
    val disabled: Color = Color.valueOf("181825")
) {
    companion object {
        val Default = ButtonColors()
        val Primary = ButtonColors(
            fill = Color.valueOf("89b4fa"),
            hover = Color.valueOf("b4befe"),
            pressed = Color.valueOf("74c7ec"),
            content = Color.valueOf("11111b")
        )
        val Danger = ButtonColors(
            fill = Color.valueOf("f38ba8"),
            hover = Color.valueOf("eba0ac"),
            pressed = Color.valueOf("e78284"),
            content = Color.valueOf("11111b")
        )
        val Success = ButtonColors(
            fill = Color.valueOf("a6e3a1"),
            hover = Color.valueOf("94e2d5"),
            pressed = Color.valueOf("81c8be"),
            content = Color.valueOf("11111b")
        )
    }
}

/**
 * ## Button
 *
 * Pre-styled interactive button component with automated hover and click state styling.
 *
 * @param text Button label text.
 * @param onClick Callback invoked when button is clicked.
 * @param modifier Chainable [UIModifier].
 * @param enabled Whether the button is active and interactive.
 * @param colors Color state palette ([ButtonColors]).
 * @param radius Corner radius in pixels.
 * @param minWidth Minimum button width in pixels.
 * @param minHeight Minimum button height in pixels.
 * @param onDoubleClick Optional double-click listener.
 */
@Composable
fun Button(
    text: String,
    onClick: () -> Unit,
    modifier: UIModifier = UIModifier,
    enabled: Boolean = true,
    colors: ButtonColors = ButtonColors.Default,
    radius: Double = 6.0,
    minWidth: Float = 80f,
    minHeight: Float = 36f,
    onDoubleClick: (() -> Unit)? = null
) {
    val buttonModifier = UIModifier
        .pad(horizontal = 16f, vertical = 8f)
        .minSize(minWidth, minHeight)
        .radius(radius)
        .background(if (enabled) colors.fill else colors.disabled)
        .opacity(if (enabled) 1.0 else 0.4)
        .touchable(enabled)
        .onClick { if (enabled) onClick() }
        .then(if (onDoubleClick != null) UIModifier.onDoubleClick(onDoubleClick) else UIModifier)
        .custom { node ->
            node.onPointerEnter = {
                if (enabled) (node as? org.mdt.ui.widgets.BoxNode)?.visuals?.fillColor?.set(colors.hover)
            }
            node.onPointerExit = {
                if (enabled) (node as? org.mdt.ui.widgets.BoxNode)?.visuals?.fillColor?.set(colors.fill)
            }
        }
        .then(modifier)

    Box(modifier = buttonModifier) {
        Text(
            text = text,
            color = if (enabled) colors.content else Color.gray,
            align = Align.center
        )
    }
}

/**
 * ## Toggle
 *
 * Binary state toggle switch component.
 *
 * @param checked Current boolean state.
 * @param onToggle Callback invoked when toggled.
 * @param modifier Chainable [UIModifier].
 * @param size Box dimension size in pixels.
 * @param radius Corner radius in pixels.
 * @param onColor Color when checked is true.
 * @param offColor Color when checked is false.
 */
@Composable
fun Toggle(
    checked: Boolean,
    onToggle: () -> Unit,
    modifier: UIModifier = UIModifier,
    size: Float = 36f,
    radius: Double = 6.0,
    onColor: Color = Color.valueOf("7ED321"),
    offColor: Color = Color.valueOf("2a2a3a")
) {
    val toggleModifier = UIModifier
        .fixed(size, size)
        .pad(4f)
        .radius(radius)
        .background(if (checked) onColor else offColor)
        .onClick(onToggle)
        .then(modifier)

    Box(modifier = toggleModifier)
}

/**
 * ## Card
 *
 * Pre-styled surface card container with border outline, background fill, and padding.
 *
 * @param modifier Chainable [UIModifier].
 * @param backgroundColor Card fill color.
 * @param borderColor Card stroke outline color.
 * @param borderWidth Card border stroke width in pixels.
 * @param radius Corner radius in pixels.
 * @param padding Inner content padding in pixels.
 * @param content Scoped child composable within [BoxScope].
 */
@Composable
fun Card(
    modifier: UIModifier = UIModifier,
    backgroundColor: Color = Color.valueOf("1e1e2e"),
    borderColor: Color = Color.valueOf("89b4fa"),
    borderWidth: Double = 1.8,
    radius: Double = 12.0,
    padding: Float = 20f,
    content: @Composable BoxScope.() -> Unit
) {
    val cardModifier = UIModifier
        .pad(padding)
        .radius(radius)
        .background(backgroundColor)
        .border(borderWidth, borderColor)
        .then(modifier)

    Box(modifier = cardModifier, content = content)
}

/**
 * ## Divider
 *
 * Thin horizontal separator line bar.
 *
 * @param modifier Chainable [UIModifier].
 * @param color Stroke line color.
 * @param thickness Line thickness in pixels.
 */
@Composable
fun Divider(
    modifier: UIModifier = UIModifier,
    color: Color = Color.valueOf("45475a"),
    thickness: Float = 1.5f
) {
    val dividerModifier = UIModifier
        .fillMaxWidth()
        .height(thickness)
        .background(color)
        .then(modifier)

    Box(modifier = dividerModifier)
}
