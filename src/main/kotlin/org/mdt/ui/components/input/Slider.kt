package org.mdt.ui.components.input

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.*
import arc.math.Mathf
import org.mdt.core.ui.UINode
import org.mdt.core.ui.compose.*
import org.mdt.core.ui.graphics.Color
import org.mdt.core.ui.input.PointerEvent
import org.mdt.core.ui.layout.Alignment
import org.mdt.core.ui.layout.Arrangement
import org.mdt.ui.components.layout.Box
import org.mdt.ui.components.layout.Column
import org.mdt.ui.components.layout.Row
import org.mdt.ui.components.layout.Spacer
import org.mdt.ui.components.text.MonoText
import org.mdt.ui.components.text.Text
import org.mdt.ui.theme.Theme

/**
 * ## Slider
 *
 * Pure Input Control Primitive for continuous or discrete range selection.
 * Focuses strictly on gesture tracking, value computation, and visual track progress.
 *
 * @param value Current slider value.
 * @param onValueChange Callback invoked continuously as slider is dragged.
 * @param modifier Chainable [UIModifier].
 * @param valueRange Value range interval ([ClosedFloatingPointRange]).
 * @param step Optional snap step increment (0.0f = continuous).
 * @param enabled Whether slider accepts touch and drag gestures.
 * @param activeColor Active progress fill color (defaults to [Theme.colors.blue]).
 * @param trackColor Inactive track background color.
 *
 * See: docs/design-system/design_system_en.md
 */
@Composable
fun Slider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: UIModifier = UIModifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    step: Float = 0f,
    enabled: Boolean = true,
    activeColor: Color = Theme.colors.blue,
    trackColor: Color = Theme.colors.surfaceTertiary
) {
    val colors = Theme.colors
    val shapes = Theme.shapes

    var isHovered by remember { mutableStateOf(false) }
    var isDragging by remember { mutableStateOf(false) }

    val clamped = value.coerceIn(valueRange.start, valueRange.endInclusive)
    val span = valueRange.endInclusive - valueRange.start
    val fraction = if (span > 0.0001f) ((clamped - valueRange.start) / span).coerceIn(0f, 1f) else 0f

    // Smooth hover and active drag animation
    val hoverAnim by animateFloatAsState(
        targetValue = if ((isDragging || isHovered) && enabled) 1f else 0f,
        animationSpec = tween(durationMillis = 140, easing = FastOutSlowInEasing)
    )

    val currentBorder = colors.borderHairline.lerp(activeColor.mul(1.25f), hoverAnim)
    val currentShadow = colors.shadowAmbient.lerp(activeColor.withAlpha(0.40f), hoverAnim)

    val dragModifier = SliderDragModifier(
        valueRange = valueRange,
        span = span,
        step = step,
        enabled = enabled,
        onValueChange = onValueChange,
        onDragStateChange = { isDragging = it }
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(24f)
            .minHeight(24f)
            .hoverable { if (enabled) isHovered = it }
            .then(dragModifier)
            .then(modifier)
    ) {
        // Inner Track Capsule (Stable 8f height with smooth glow transitions)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8f)
                .minHeight(8f)
                .radius(shapes.pill)
                .background(if (enabled) trackColor else colors.glassThin)
                .progress(fraction, if (enabled) activeColor else colors.textQuaternary)
                .border(1f, currentBorder)
                .shadow(
                    color = currentShadow,
                    blur = 4f + 4f * hoverAnim,
                    spread = 0.5f
                )
        )
    }
}

/**
 * ## LabeledSlider
 *
 * Convenient composite form-field pairing [Slider] with a leading label and trailing value readout.
 */
@Composable
fun LabeledSlider(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: UIModifier = UIModifier,
    valueText: String? = null,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    step: Float = 0f,
    enabled: Boolean = true,
    activeColor: Color = Theme.colors.blue,
    trackColor: Color = Theme.colors.surfaceTertiary
) {
    val colors = Theme.colors
    val clamped = value.coerceIn(valueRange.start, valueRange.endInclusive)
    val span = valueRange.endInclusive - valueRange.start
    val fraction = if (span > 0.0001f) ((clamped - valueRange.start) / span).coerceIn(0f, 1f) else 0f
    val displayValue = valueText ?: "${(fraction * 100f).toInt()}%"

    Column(
        arrangement = Arrangement.spacedBy(4f),
        alignment = Alignment.TopStart,
        modifier = Modifier.fillMaxWidth().then(modifier)
    ) {
        Row(
            arrangement = Arrangement.spacedBy(8f),
            alignment = Alignment.CenterStart,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = label, color = if (enabled) colors.textPrimary else colors.textQuaternary)
            Spacer(modifier = Modifier.weight(1f))
            MonoText(text = displayValue, color = if (enabled) colors.blue else colors.textTertiary)
        }

        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            step = step,
            enabled = enabled,
            activeColor = activeColor,
            trackColor = trackColor
        )
    }
}

/**
 * Typed modifier element for Slider dragging gestures (Zero-GC).
 */
data class SliderDragModifier(
    val valueRange: ClosedFloatingPointRange<Float>,
    val span: Float,
    val step: Float,
    val enabled: Boolean,
    val onValueChange: (Float) -> Unit,
    val onDragStateChange: (Boolean) -> Unit
) : UIModifier.Element {
    override fun applyTo(node: UINode) {
        if (!enabled) return

        val update = { event: PointerEvent ->
            val trackWidth = node.bounds.width - node.padL - node.padR
            if (trackWidth > 0f) {
                val localX = event.x - (node.bounds.x + node.padL)
                val fraction = (localX / trackWidth).coerceIn(0f, 1f)
                var rawValue = valueRange.start + fraction * span

                if (step > 0f) {
                    rawValue = Mathf.round(rawValue / step) * step
                }

                val finalValue = rawValue.coerceIn(valueRange.start, valueRange.endInclusive)
                onValueChange(finalValue)
                event.isConsumed = true
            }
        }
        node.onPointerDown = {
            onDragStateChange(true)
            update(it)
        }
        node.onPointerDrag = update
        node.onPointerUp = {
            onDragStateChange(false)
        }
    }
}
