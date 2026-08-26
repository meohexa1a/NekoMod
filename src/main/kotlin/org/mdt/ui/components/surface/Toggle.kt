@file:Suppress("FunctionName", "unused")

package org.mdt.ui.components.surface

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import arc.graphics.Color
import org.mdt.core.ui.compose.*
import org.mdt.core.ui.layout.LayoutPreset
import org.mdt.ui.components.layout.Box
import org.mdt.ui.theme.Theme

/**
 * ## ToggleColors
 *
 * Color palette configuration for Apple iOS [Toggle] switches.
 */
data class ToggleColors(
    val activeTrack: Color,
    val inactiveTrack: Color,
    val thumb: Color = Color.white,
    val border: Color = Color.clear
)

/**
 * ## Toggle
 *
 * Classic Apple iOS animated capsule switch toggle with sliding thumb knob,
 * smooth color interpolation, and tactile feedback.
 *
 * @param checked Current binary state of the toggle.
 * @param onToggle Callback triggered on click with the new toggled state.
 * @param modifier Chainable [UIModifier] to customize layout/events for the track container.
 * @param activeColor Custom active track color (defaults to [Theme.colors.green]).
 */
@Composable
fun Toggle(
    checked: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: UIModifier = UIModifier,
    activeColor: Color = Theme.colors.green
) {
    val colors = Theme.colors

    val progress by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing)
    )

    val currentTrackColor = colors.surfaceTertiary.cpy().lerp(activeColor, progress)
    val currentBorderColor = colors.borderHairline.cpy().lerp(activeColor.cpy().mul(1.15f), progress)

    val trackWidth = 44f
    val trackHeight = 24f
    val thumbSize = 18f
    val padding = 3f
    val travelDist = trackWidth - thumbSize - padding * 2f
    val thumbLeft = padding + travelDist * progress

    Box(
        modifier = Modifier
            .size(trackWidth, trackHeight)
            .radius(Theme.shapes.pill)
            .background(currentTrackColor)
            .shadow(activeColor.cpy().apply { a = 0.35f * progress }, blur = 6f * progress, spread = 1f)
            .clickable { onToggle(!checked) }
            .then(modifier)
    ) {
        Box(
            modifier = Modifier
                .size(thumbSize, thumbSize)
                .anchor(LayoutPreset.CENTER_LEFT)
                .margin(left = thumbLeft)
                .radius(Theme.shapes.pill)
                .background(Color.white)
                .shadow(Color(0f, 0f, 0f, 0.40f), offsetX = 0f, offsetY = -1f, blur = 3f, spread = 0.5f)
        )
    }
}
