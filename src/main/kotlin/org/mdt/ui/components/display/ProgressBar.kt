package org.mdt.ui.components.display

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import org.mdt.core.ui.compose.*
import org.mdt.core.ui.graphics.Color
import org.mdt.ui.components.layout.Box
import org.mdt.ui.theme.Theme

/**
 * ## ProgressBar
 *
 * Apple iOS-style capsule progress bar rendered via hardware SDF shaders,
 * featuring smooth interpolation animations on progress state mutations.
 *
 * @param progress Progress ratio (clamped to 0.0f..1.0f).
 * @param modifier Chainable [UIModifier]. Use `Modifier.height(...)` to adjust thickness.
 * @param activeColor Fill progress color (defaults to [Theme.colors.blue]).
 * @param trackColor Background track color (defaults to [Theme.colors.surfaceTertiary]).
 *
 * See: docs/design-system/design_system_en.md
 */
@Composable
fun ProgressBar(
    progress: Float,
    modifier: UIModifier = UIModifier,
    activeColor: Color = Theme.colors.blue,
    trackColor: Color = Theme.colors.surfaceTertiary
) {
    val clamped = progress.coerceIn(0.0f, 1.0f)

    // Smooth progress fill interpolation by default
    val animatedProgress by animateFloatAsState(
        targetValue = clamped,
        animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing)
    )

    Box(
        modifier = Modifier
            .height(6f)
            .minWidth(60f)
            .radius(Theme.shapes.pill)
            .background(trackColor)
            .progress(animatedProgress, activeColor)
            .then(modifier)
    )
}
