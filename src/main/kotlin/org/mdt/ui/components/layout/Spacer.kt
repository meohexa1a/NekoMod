// [AGENT INVARIANT] Synchronously update @property, @param, and @see KDocs when modifying this file.

@file:Suppress("FunctionName", "unused")

package org.mdt.ui.components.layout

import androidx.compose.runtime.Composable
import org.mdt.core.ui.modifier.UIModifier
import org.mdt.core.ui.modifier.size
import org.mdt.core.ui.modifier.weight

/**
 * ## Spacer
 *
 * Empty layout node reserving spatial gaps in Row or Column containers.
 *
 * @param modifier Chainable [UIModifier].
 */
@Composable
fun Spacer(modifier: UIModifier = UIModifier) {
    Box(modifier = modifier)
}

/**
 * ## Spacer
 *
 * Fixed-size spacer reserving equal horizontal and vertical bounds.
 *
 * @param size Fixed spatial width and height in pixels.
 * @param modifier Chainable [UIModifier].
 */
@Composable
fun Spacer(
    size: Float,
    modifier: UIModifier = UIModifier
) {
    Box(modifier = modifier.size(size))
}

/**
 * ## FlexSpacer
 *
 * Flexible weighted spacer expanding to consume available flex space in Row or Column containers.
 *
 * @param weight Relative weight fraction for proportional space distribution.
 * @param modifier Chainable [UIModifier].
 */
@Composable
fun FlexSpacer(
    weight: Float = 1.0f,
    modifier: UIModifier = UIModifier
) {
    Box(modifier = modifier.weight(weight))
}
