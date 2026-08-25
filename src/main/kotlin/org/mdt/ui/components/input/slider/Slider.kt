@file:Suppress("FunctionName", "unused")

package org.mdt.ui.components.input.slider

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeNode
import arc.graphics.Color
import org.mdt.ui.compose.NodeApplier
import org.mdt.ui.compose.UIModifier

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
 * Features smooth drag-to-adjust, integrated left title label, and right numerical value display.
 *
 * @param value Current slider value.
 * @param onValueChange Callback invoked continuously as slider is dragged.
 * @param modifier Chainable [UIModifier].
 * @param label Optional title text rendered inside the left side of the slider.
 * @param valueText Optional formatted value text rendered on the right (defaults to "X%").
 * @param valueRange Value range interval ([ClosedFloatingPointRange]).
 * @param step Optional snap step increment (0.0f = continuous).
 * @param colors Color styling palette ([SliderColors]).
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
    colors: SliderColors = SliderColors.Default
) {
    ComposeNode<SliderNode, NodeApplier>(
        factory = {
            val node = SliderNode()
            node.valueRange = valueRange
            node.step = step
            node.value = value
            node.label = label
            node.valueText = valueText
            node.onValueChange = onValueChange
            node.trackColor.set(colors.track)
            node.activeTrackColor.set(colors.activeTrack)
            node.borderColor.set(colors.border)
            node.labelColor.set(colors.labelText)
            node.valueColor.set(colors.valueText)
            modifier.applyTo(node)
            node
        },
        update = {
            set(value) { this.value = it }
            set(onValueChange) { this.onValueChange = it }
            set(label) { this.label = it }
            set(valueText) { this.valueText = it }
            set(valueRange) { this.valueRange = it; invalidateLayout() }
            set(step) { this.step = it }
            set(colors) {
                this.trackColor.set(it.track)
                this.activeTrackColor.set(it.activeTrack)
                this.borderColor.set(it.border)
                this.labelColor.set(it.labelText)
                this.valueColor.set(it.valueText)
            }
            set(modifier) {
                it.applyTo(this)
                invalidateLayout()
            }
        }
    )
}
