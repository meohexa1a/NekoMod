@file:Suppress("FunctionName", "unused")

package org.mdt.ui.components.surface

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import arc.graphics.Color
import org.mdt.ui.components.layout.Box
import org.mdt.core.ui.compose.*
import org.mdt.core.ui.layout.LayoutPreset

/**
 * ## ToggleColors
 *
 * Color palette state for [Toggle].
 */
data class ToggleColors(
    val activeTrack: Color = Color.valueOf("10b981"),
    val inactiveTrack: Color = Color.valueOf("24273a"),
    val activeThumb: Color = Color.white,
    val inactiveThumb: Color = Color.valueOf("9399b2"),
    val border: Color = Color.valueOf("45475a")
) {
    companion object {
        val Default = ToggleColors()
        val Primary = ToggleColors(activeTrack = Color.valueOf("2563eb"))
        val Success = ToggleColors(activeTrack = Color.valueOf("10b981"))
        val Danger = ToggleColors(activeTrack = Color.valueOf("dc2626"))
        val Warning = ToggleColors(activeTrack = Color.valueOf("f59e0b"))
    }
}

/**
 * ## Toggle
 *
 * Highly refined animated switch toggle component with smooth sliding knob,
 * dynamic color interpolation, and targeted [thumbModifier] support.
 *
 * See: docs/compose-dsl/compose_dsl_en.md
 *
 * @param checked Current binary state of the toggle.
 * @param onToggle Callback triggered on click with the new toggled state.
 * @param modifier Chainable [UIModifier] to customize layout/events for the track container.
 * @param colors Styling palette ([ToggleColors]).
 * @param thumbModifier Optional modifier to customize the inner sliding thumb knob.
 */
@Composable
fun Toggle(
    checked: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: UIModifier = UIModifier,
    colors: ToggleColors = ToggleColors.Default,
    thumbModifier: UIModifier = UIModifier
) {
    val progress by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing)
    )

    val currentTrackColor = colors.inactiveTrack.cpy().lerp(colors.activeTrack, progress)
    val currentBorderColor = colors.border.cpy().lerp(colors.activeTrack.cpy().mul(1.15f), progress)
    val currentThumbColor = colors.inactiveThumb.cpy().lerp(colors.activeThumb, progress)

    val trackWidth = 44f
    val trackHeight = 24f
    val thumbSize = 18f
    val padding = 3f
    val travelDist = trackWidth - thumbSize - padding * 2f
    val thumbLeft = padding + travelDist * progress

    Box(
        modifier = UIModifier
            .size(trackWidth, trackHeight)
            .cornerRadius(trackHeight * 0.5f)
            .background(currentTrackColor)
            .border(1f, currentBorderColor)
            .shadow(colors.activeTrack.cpy().a(0.35f * progress), blur = 6f * progress, spread = 1f)
            .clickable { onToggle(!checked) }
            .then(modifier)
    ) {
        Box(
            modifier = UIModifier
                .size(thumbSize, thumbSize)
                .anchor(LayoutPreset.CENTER_LEFT)
                .margin(left = thumbLeft)
                .cornerRadius(thumbSize * 0.5f)
                .background(currentThumbColor)
                .shadow(Color.black.a(0.35f), offsetX = 0f, offsetY = -1f, blur = 3f, spread = 0.5f)
                .then(thumbModifier)
        )
    }
}
