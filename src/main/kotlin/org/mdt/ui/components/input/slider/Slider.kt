@file:Suppress("FunctionName", "unused")

package org.mdt.ui.components.input.slider

import androidx.compose.runtime.Composable
import arc.graphics.Color
import arc.math.Mathf
import mindustry.ui.Fonts
import org.mdt.core.ui.UINode
import org.mdt.core.ui.compose.*
import org.mdt.core.ui.input.PointerEvent
import org.mdt.core.ui.layout.Alignment
import org.mdt.ui.components.layout.Box
import org.mdt.ui.components.layout.Row
import org.mdt.ui.components.layout.Spacer
import org.mdt.ui.components.text.Text
import org.mdt.ui.theme.Theme

/**
 * ## Slider
 *
 * Modern interactive Apple iOS Control Center-style Capsule Pill Slider.
 * Built with pure declarative layout primitives and SDF capsule rendering.
 *
 * @param value Current slider value.
 * @param onValueChange Callback invoked continuously as slider is dragged.
 * @param modifier Chainable [UIModifier] for outer container.
 * @param label Optional title text rendered inside the left side of the slider.
 * @param valueText Optional formatted value text rendered on the right (defaults to "X%").
 * @param valueRange Value range interval ([ClosedFloatingPointRange]).
 * @param step Optional snap step increment (0.0f = continuous).
 * @param activeColor Active progress fill color (defaults to [Theme.colors.blue]).
 * @param trackColor Inactive track background color.
 */
@Composable
fun Slider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: UIModifier = UIModifier,
    label: String? = null,
    valueText: String? = null,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    step: Float = 0f,
    activeColor: Color = Theme.colors.blue,
    trackColor: Color = Theme.colors.surfaceTertiary
) {
    val colors = Theme.colors
    val shapes = Theme.shapes

    val clamped = value.coerceIn(valueRange.start, valueRange.endInclusive)
    val span = valueRange.endInclusive - valueRange.start
    val fraction = if (span > 0.0001f) ((clamped - valueRange.start) / span).coerceIn(0f, 1f) else 0f
    val displayValue = valueText ?: "${(fraction * 100f).toInt()}%"

    val dragModifier = SliderDragModifier(valueRange, span, step, onValueChange)

    Box(
        modifier = Modifier
            .height(34f)
            .minWidth(140f)
            .radius(shapes.pill)
            .background(trackColor)
            .progress(fraction, activeColor)
            .shadow(colors.shadowAmbient, blur = 4f, spread = 0.5f)
            .then(dragModifier)
            .then(modifier)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .pad(horizontal = 14f),
            alignment = Alignment.CenterStart
        ) {
            if (label != null) {
                Text(
                    text = label,
                    color = colors.textPrimary,
                    font = Fonts.def
                )
            }
            Spacer(Modifier.weight(1f))
            Text(
                text = displayValue,
                color = colors.textSecondary,
                font = Fonts.def
            )
        }
    }
}

/**
 * Typed modifier element for Slider dragging gestures (Zero-GC).
 */
data class SliderDragModifier(
    val valueRange: ClosedFloatingPointRange<Float>,
    val span: Float,
    val step: Float,
    val onValueChange: (Float) -> Unit
) : UIModifier.Element {
    override fun applyTo(node: UINode) {
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
        node.onPointerDown = update
        node.onPointerDrag = update
    }
}
