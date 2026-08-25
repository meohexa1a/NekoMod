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
 * Color styling palette for [Slider].
 */
data class SliderColors(
    val track: Color = Color.valueOf("181926"),
    val activeTrack: Color = Color.valueOf("2563eb"),
    val thumb: Color = Color.white
) {
    companion object {
        val Default = SliderColors()
        val Primary = SliderColors(track = Color.valueOf("181926"), activeTrack = Color.valueOf("2563eb"), thumb = Color.white)
        val Success = SliderColors(track = Color.valueOf("181926"), activeTrack = Color.valueOf("059669"), thumb = Color.white)
        val Danger = SliderColors(track = Color.valueOf("181926"), activeTrack = Color.valueOf("dc2626"), thumb = Color.white)
    }
}

/**
 * ## Slider
 *
 * Interactive draggable slider for selecting continuous or discrete numeric values.
 *
 * @param value Current slider value.
 * @param onValueChange Callback invoked continuously as slider is dragged.
 * @param modifier Chainable [UIModifier].
 * @param valueRange Value range interval ([ClosedFloatingPointRange]).
 * @param step Optional snap step increment (0.0f = continuous).
 * @param colors Color styling palette ([SliderColors]).
 */
@Composable
fun Slider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: UIModifier = UIModifier,
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
            node.onValueChange = onValueChange
            node.trackColor.set(colors.track)
            node.activeTrackColor.set(colors.activeTrack)
            node.thumbColor.set(colors.thumb)
            modifier.applyTo(node)
            node
        },
        update = {
            set(value) { this.value = it }
            set(onValueChange) { this.onValueChange = it }
            set(valueRange) { this.valueRange = it; invalidateLayout() }
            set(step) { this.step = it }
            set(colors) {
                this.trackColor.set(it.track)
                this.activeTrackColor.set(it.activeTrack)
                this.thumbColor.set(it.thumb)
            }
            set(modifier) {
                it.applyTo(this)
                invalidateLayout()
            }
        }
    )
}
