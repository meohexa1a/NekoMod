@file:Suppress("FunctionName")

package org.mdt.ui.compose

import androidx.compose.runtime.Composable
import arc.graphics.Color
import arc.util.Align
import org.mdt.ui.layout.LayoutPreset

/**
 * ## ButtonColors
 *
 * Professional color palette state configuration for interactive buttons.
 */
data class ButtonColors(
    val fill: Color = Color.valueOf("24273a"),
    val hover: Color = Color.valueOf("363a4f"),
    val pressed: Color = Color.valueOf("494d64"),
    val content: Color = Color.valueOf("cad3f5"),
    val border: Color = Color.valueOf("494d64"),
    val disabled: Color = Color.valueOf("181926")
) {
    companion object {
        val Default = ButtonColors()

        val Primary = ButtonColors(
            fill = Color.valueOf("2563eb"),
            hover = Color.valueOf("3b82f6"),
            pressed = Color.valueOf("1d4ed8"),
            content = Color.white,
            border = Color.valueOf("60a5fa")
        )

        val Danger = ButtonColors(
            fill = Color.valueOf("dc2626"),
            hover = Color.valueOf("ef4444"),
            pressed = Color.valueOf("b91c1c"),
            content = Color.white,
            border = Color.valueOf("f87171")
        )

        val Success = ButtonColors(
            fill = Color.valueOf("059669"),
            hover = Color.valueOf("10b981"),
            pressed = Color.valueOf("047857"),
            content = Color.white,
            border = Color.valueOf("34d399")
        )
    }
}

/**
 * ## Button
 *
 * Sleek, professional interactive button with automated hover, press feedback,
 * clean typography, and subtle border strokes.
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
    minWidth: Float = 72f,
    minHeight: Float = 32f,
    onDoubleClick: (() -> Unit)? = null
) {
    val buttonModifier = UIModifier
        .pad(horizontal = 14f, vertical = 6f)
        .minSize(minWidth, minHeight)
        .radius(radius)
        .background(if (enabled) colors.fill else colors.disabled)
        .border(1.0, if (enabled) colors.border else Color.valueOf("24273a"))
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
            scale = 0.9,
            align = Align.center
        )
    }
}

/**
 * ## Toggle
 *
 * Sleek capsule pill toggle switch with sliding knob indicator.
 *
 * @param checked Current boolean state.
 * @param onToggle Callback invoked when toggled.
 * @param modifier Chainable [UIModifier].
 * @param width Pill width in pixels.
 * @param height Pill height in pixels.
 * @param onColor Track color when checked is true.
 * @param offColor Track color when checked is false.
 */
@Composable
fun Toggle(
    checked: Boolean,
    onToggle: () -> Unit,
    modifier: UIModifier = UIModifier,
    width: Float = 44f,
    height: Float = 24f,
    onColor: Color = Color.valueOf("2563eb"),
    offColor: Color = Color.valueOf("24273a")
) {
    val pillModifier = UIModifier
        .fixed(width, height)
        .radius(height * 0.5)
        .background(if (checked) onColor else offColor)
        .border(1.0, if (checked) Color.valueOf("60a5fa") else Color.valueOf("363a4f"))
        .onClick(onToggle)
        .then(modifier)

    val knobSize = height - 6f
    val knobModifier = if (checked) {
        UIModifier
            .anchor(LayoutPreset.CENTER_RIGHT)
            .margin(right = 3f)
            .fixed(knobSize, knobSize)
            .radius(knobSize * 0.5)
            .background(Color.white)
            .shadow(Color.black.a(0.3f), spread = 1.0, blur = 3.0)
    } else {
        UIModifier
            .anchor(LayoutPreset.CENTER_LEFT)
            .margin(left = 3f)
            .fixed(knobSize, knobSize)
            .radius(knobSize * 0.5)
            .background(Color.valueOf("9399b2"))
            .shadow(Color.black.a(0.3f), spread = 1.0, blur = 3.0)
    }

    Box(modifier = pillModifier) {
        Box(modifier = knobModifier)
    }
}

/**
 * ## Card
 *
 * Sleek glassmorphism surface card container with subtle borders and backdrop blur.
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
    backgroundColor: Color = Color.valueOf("14151f").a(0.92f),
    borderColor: Color = Color.valueOf("363a4f"),
    borderWidth: Double = 1.0,
    radius: Double = 10.0,
    padding: Float = 18f,
    content: @Composable BoxScope.() -> Unit
) {
    val cardModifier = UIModifier
        .pad(padding)
        .radius(radius)
        .background(backgroundColor)
        .backdrop(blur = true, radius = 4f, weight = 0.85, tint = Color.valueOf("181926"))
        .border(borderWidth, borderColor)
        .shadow(Color.black.a(0.4f), spread = 2.0, blur = 10.0)
        .then(modifier)

    Box(modifier = cardModifier, content = content)
}

/**
 * ## Divider
 *
 * Subtle, modern separator line.
 *
 * @param modifier Chainable [UIModifier].
 * @param color Stroke line color.
 * @param thickness Line thickness in pixels.
 */
@Composable
fun Divider(
    modifier: UIModifier = UIModifier,
    color: Color = Color.valueOf("2a2d3f"),
    thickness: Float = 1.0f
) {
    val dividerModifier = UIModifier
        .fillMaxWidth()
        .height(thickness)
        .background(color)
        .then(modifier)

    Box(modifier = dividerModifier)
}
