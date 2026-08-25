@file:Suppress("FunctionName", "unused")

package org.mdt.ui.components.surface

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import arc.graphics.Color
import org.mdt.ui.components.layout.Box
import org.mdt.ui.compose.*
import org.mdt.ui.layout.LayoutPreset

/**
 * ## Toggle
 *
 * Highly refined animated switch toggle component with smooth sliding knob,
 * dynamic color interpolation, and realistic 3D depth shadows.
 *
 * See: docs/compose-dsl/compose_dsl_en.md
 *
 * @param checked Current binary state of the toggle.
 * @param onToggle Callback triggered on click with the new toggled state.
 * @param modifier Chainable [UIModifier] to customize layout/events.
 * @param activeColor Background track color when ON.
 * @param inactiveColor Background track color when OFF.
 * @param activeThumbColor Knob/thumb color when ON.
 * @param inactiveThumbColor Knob/thumb color when OFF.
 * @param borderColor Subtle outline stroke color when OFF.
 */
@Composable
fun Toggle(
    checked: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: UIModifier = UIModifier,
    activeColor: Color = Color.valueOf("10b981"),
    inactiveColor: Color = Color.valueOf("24273a"),
    activeThumbColor: Color = Color.white,
    inactiveThumbColor: Color = Color.valueOf("9399b2"),
    borderColor: Color = Color.valueOf("45475a")
) {
    val progress by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing)
    )

    val currentTrackColor = inactiveColor.cpy().lerp(activeColor, progress)
    val currentBorderColor = borderColor.cpy().lerp(activeColor.cpy().mul(1.15f), progress)
    val currentThumbColor = inactiveThumbColor.cpy().lerp(activeThumbColor, progress)

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
            .shadow(activeColor.cpy().a(0.35f * progress), blur = 6f * progress, spread = 1f)
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
        )
    }
}
