@file:Suppress("FunctionName", "unused")

package org.mdt.ui.components.display.progress

import androidx.compose.runtime.Composable
import arc.graphics.Color
import org.mdt.ui.components.layout.Box
import org.mdt.core.ui.compose.*

/**
 * ## ProgressColors
 *
 * Color palette state for [ProgressBar].
 */
data class ProgressColors(
    val track: Color = Color.valueOf("181926"),
    val fill: Color = Color.valueOf("2563eb")
) {
    companion object {
        val Default = ProgressColors()
        val Primary = ProgressColors(track = Color.valueOf("181926"), fill = Color.valueOf("2563eb"))
        val Success = ProgressColors(track = Color.valueOf("181926"), fill = Color.valueOf("059669"))
        val Danger = ProgressColors(track = Color.valueOf("181926"), fill = Color.valueOf("dc2626"))
        val Warning = ProgressColors(track = Color.valueOf("181926"), fill = Color.valueOf("d97706"))
    }
}

/**
 * ## ProgressBar
 *
 * Pure declarative progress bar component composed of primitive [Box] nodes.
 * Automatically leverages GPU SDF rounded corners and supports targeted [fillModifier].
 *
 * @param progress Progress ratio (clamped to 0.0f..1.0f).
 * @param modifier Chainable [UIModifier] for the outer track container.
 * @param barHeight Thickness of the progress bar in pixels.
 * @param colors Color styling palette ([ProgressColors]).
 * @param fillModifier Optional modifier applied directly to the active progress fill bar.
 */
@Composable
fun ProgressBar(
    progress: Float,
    modifier: UIModifier = UIModifier,
    barHeight: Float = 6f,
    colors: ProgressColors = ProgressColors.Default,
    fillModifier: UIModifier = UIModifier
) {
    val clamped = progress.coerceIn(0.0f, 1.0f)
    val r = barHeight * 0.5f

    Box(
        modifier = UIModifier
            .height(barHeight)
            .minWidth(60f)
            .background(colors.track)
            .radius(r)
            .clip(true)
            .then(modifier)
    ) {
        if (clamped > 0.001f) {
            Box(
                modifier = UIModifier
                    .anchorFillWidth(clamped)
                    .background(colors.fill)
                    .radius(r)
                    .then(fillModifier)
            )
        }
    }
}
