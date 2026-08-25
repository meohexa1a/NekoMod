@file:Suppress("FunctionName")

package org.mdt.ui.compose

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
    radius: Float = 6f,
    minWidth: Float = 72f,
    minHeight: Float = 32f,
    onDoubleClick: (() -> Unit)? = null
) {
    val buttonModifier = UIModifier
        .pad(horizontal = 14f, vertical = 6f)
        .minSize(minWidth, minHeight)
        .radius(radius)
        .background(if (enabled) colors.fill else colors.disabled)
        .border(1f, if (enabled) colors.border else Color.valueOf("24273a"))
        .opacity(if (enabled) 1.0f else 0.4f)
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
            scale = 0.9f,
            align = Align.center
        )
    }
}

/**
 * ## Toggle
 *
 * Smoothly animated capsule pill toggle switch (300ms transition) with sliding knob indicator
 * and color interpolation.
 *
 * @param checked Current boolean state.
 * @param onToggle Callback invoked when toggled.
 * @param modifier Chainable [UIModifier].
 * @param width Pill width in pixels.
 * @param height Pill height in pixels.
 * @param onColor Track color when checked is true.
 * @param offColor Track color when checked is false.
 * @param durationMillis Animation transition duration in milliseconds (default: 300ms).
 */
@Composable
fun Toggle(
    checked: Boolean,
    onToggle: () -> Unit,
    modifier: UIModifier = UIModifier,
    width: Float = 46f,
    height: Float = 24f,
    onColor: Color = Color.valueOf("2563eb"),
    offColor: Color = Color.valueOf("24273a"),
    durationMillis: Int = 300
) {
    val progress by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = tween(durationMillis = durationMillis, easing = FastOutSlowInEasing)
    )

    val currentTrackColor = Color(offColor).lerp(onColor, progress)
    val currentBorderColor = Color(Color.valueOf("363a4f")).lerp(Color.valueOf("60a5fa"), progress)

    val pillModifier = UIModifier
        .fixed(width, height)
        .radius(height * 0.5f)
        .background(currentTrackColor)
        .border(1f, currentBorderColor)
        .onClick(onToggle)
        .then(modifier)

    val knobSize = height - 6f
    val minMargin = 3f
    val maxMargin = width - knobSize - 3f
    val currentMarginLeft = minMargin + (maxMargin - minMargin) * progress

    val currentKnobColor = Color(Color.valueOf("a6adc8")).lerp(Color.white, progress)

    val knobModifier = UIModifier
        .anchor(LayoutPreset.CENTER_LEFT)
        .margin(left = currentMarginLeft)
        .fixed(knobSize, knobSize)
        .radius(knobSize * 0.5f)
        .background(currentKnobColor)
        .shadow(Color.black.a(0.35f), spread = 1f, blur = 4f)

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
    borderWidth: Float = 1.0f,
    radius: Float = 10.0f,
    padding: Float = 18f,
    content: @Composable BoxScope.() -> Unit
) {
    val cardModifier = UIModifier
        .pad(padding)
        .radius(radius)
        .background(backgroundColor)
        .backdrop(blur = true, radius = 4f, weight = 0.85f, tint = Color.valueOf("181926"))
        .border(borderWidth, borderColor)
        .shadow(Color.black.a(0.4f), spread = 2f, blur = 10f)
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
