@file:Suppress("FunctionName", "unused")

package org.mdt.ui.components.input.slider

import androidx.compose.runtime.Composable
import arc.graphics.Color
import arc.math.Mathf
import mindustry.ui.Fonts
import org.mdt.ui.components.layout.Box
import org.mdt.ui.components.layout.Row
import org.mdt.ui.components.layout.Spacer
import org.mdt.ui.components.text.Text
import org.mdt.core.ui.compose.*
import org.mdt.core.ui.layout.Alignment

/**
 * ## SliderColors
 *
 * Color styling palette for Capsule [Slider].
 */
data class SliderColors(
    val track: Color = Color.valueOf("181926"),
    val activeTrack: Color = Color.valueOf("2563eb"),
    val border: Color = Color.valueOf("363a4f"),
    val labelText: Color = Color.white,
    val valueText: Color = Color.valueOf("cad3f5")
) {
    companion object {
        val Default = SliderColors()
        val Primary = SliderColors(
            track = Color.valueOf("181926"),
            activeTrack = Color.valueOf("2563eb"),
            border = Color.valueOf("363a4f"),
            labelText = Color.white,
            valueText = Color.valueOf("cad3f5")
        )
        val Success = SliderColors(
            track = Color.valueOf("181926"),
            activeTrack = Color.valueOf("059669"),
            border = Color.valueOf("363a4f"),
            labelText = Color.white,
            valueText = Color.valueOf("a7f3d0")
        )
        val Danger = SliderColors(
            track = Color.valueOf("181926"),
            activeTrack = Color.valueOf("dc2626"),
            border = Color.valueOf("363a4f"),
            labelText = Color.white,
            valueText = Color.valueOf("fecaca")
        )
    }
}

/**
 * ## Slider
 *
 * Modern interactive Capsule Pill Slider (iOS Control Center & Material 3 inspired).
 * Pure declarative component composed from primitive [Box], [Row], and [Text] nodes,
 * supporting targeted sub-node modifiers ([trackModifier], [labelModifier], [valueModifier]).
 *
 * @param value Current slider value.
 * @param onValueChange Callback invoked continuously as slider is dragged.
 * @param modifier Chainable [UIModifier] for outer container.
 * @param label Optional title text rendered inside the left side of the slider.
 * @param valueText Optional formatted value text rendered on the right (defaults to "X%").
 * @param valueRange Value range interval ([ClosedFloatingPointRange]).
 * @param step Optional snap step increment (0.0f = continuous).
 * @param colors Color styling palette ([SliderColors]).
 * @param trackModifier Targeted modifier for the active progress track.
 * @param labelModifier Targeted modifier for the left label text.
 * @param valueModifier Targeted modifier for the right value text.
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
    colors: SliderColors = SliderColors.Default,
    trackModifier: UIModifier = UIModifier,
    labelModifier: UIModifier = UIModifier,
    valueModifier: UIModifier = UIModifier
) {
    val clamped = value.coerceIn(valueRange.start, valueRange.endInclusive)
    val span = valueRange.endInclusive - valueRange.start
    val fraction = if (span > 0.0001f) ((clamped - valueRange.start) / span).coerceIn(0f, 1f) else 0f
    val displayValue = valueText ?: "${(fraction * 100f).toInt()}%"

    val dragModifier = SliderDragModifier(valueRange, span, step, onValueChange)

    Box(
        modifier = UIModifier
            .height(36f)
            .minWidth(120f)
            .background(colors.track)
            .border(1f, colors.border)
            .radius(18f)
            .clip(true)
            .then(dragModifier)
            .then(modifier)
    ) {
        // Active filled capsule bar
        if (fraction > 0.001f) {
            Box(
                modifier = UIModifier
                    .anchorFillWidth(fraction)
                    .background(colors.activeTrack)
                    .radius(18f)
                    .then(trackModifier)
            )
        }

        // Inner label and value readout row
        Row(
            modifier = UIModifier
                .fillMaxSize()
                .pad(horizontal = 14f),
            alignment = Alignment.CenterStart
        ) {
            if (label != null) {
                Text(
                    text = label,
                    color = colors.labelText,
                    font = Fonts.def,
                    modifier = labelModifier
                )
            }
            Spacer(UIModifier.weight(1f))
            Text(
                text = displayValue,
                color = colors.valueText,
                font = Fonts.def,
                modifier = valueModifier
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
    override fun applyTo(node: org.mdt.core.ui.UINode) {
        val update = { event: org.mdt.core.ui.PointerEvent ->
            val w = node.bounds.width - node.padL - node.padR
            if (w > 0f) {
                val localX = event.x - (node.bounds.x + node.padL)
                val frac = (localX / w).coerceIn(0f, 1f)
                var rawVal = valueRange.start + frac * span
                if (step > 0f) {
                    rawVal = Mathf.round(rawVal / step) * step
                }
                val finalVal = rawVal.coerceIn(valueRange.start, valueRange.endInclusive)
                onValueChange(finalVal)
                event.isConsumed = true
            }
        }
        node.onPointerDown = update
        node.onPointerDrag = update
    }
}

