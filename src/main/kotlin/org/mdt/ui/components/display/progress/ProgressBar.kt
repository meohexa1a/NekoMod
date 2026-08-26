@file:Suppress("FunctionName", "unused")

package org.mdt.ui.components.display.progress

import androidx.compose.runtime.Composable
import arc.graphics.Color
import org.mdt.core.ui.compose.*
import org.mdt.ui.components.layout.Box
import org.mdt.ui.theme.Theme

/**
 * ## ProgressBar
 *
 * Apple iOS-style capsule progress bar rendered via hardware SDF shaders.
 *
 * @param progress Progress ratio (clamped to 0.0f..1.0f).
 * @param modifier Chainable [UIModifier] for the outer track container.
 * @param barHeight Thickness of the progress bar in pixels (defaults to 6f).
 * @param activeColor Fill progress color (defaults to [Theme.colors.systemBlue]).
 * @param trackColor Background track color (defaults to [Theme.colors.surfaceTertiary]).
 */
@Composable
fun ProgressBar(
    progress: Float,
    modifier: UIModifier = UIModifier,
    barHeight: Float = 6f,
    activeColor: Color = Theme.colors.systemBlue,
    trackColor: Color = Theme.colors.surfaceTertiary
) {
    val clamped = progress.coerceIn(0.0f, 1.0f)

    Box(
        modifier = Modifier
            .height(barHeight)
            .minWidth(60f)
            .radius(Theme.shapes.pill)
            .background(trackColor)
            .progress(clamped, activeColor)
            .then(modifier)
    )
}
