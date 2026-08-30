// [AGENT INVARIANT] Synchronously update @property, @param, and @see KDocs when modifying this file.

@file:Suppress("FunctionName", "unused")

package org.mdt.ui.components.layout

import androidx.compose.runtime.Composable
import org.mdt.core.ui.compose.UIModifier
import org.mdt.core.ui.compose.size
import org.mdt.core.ui.compose.weight

/**
 * ## Spacer
 *
 * Empty layout node reserving spatial gaps in Row or Column containers.
 *
 * See: docs/components-guide/components_guide_en.md
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
 * See: docs/components-guide/components_guide_en.md
 */
@Composable
fun Spacer(
    size: Float,
    modifier: UIModifier = UIModifier
) {
    Box(modifier = UIModifier.size(size).then(modifier))
}

/**
 * ## FlexSpacer
 *
 * Flexible weighted spacer expanding to consume available flex space in Row or Column containers.
 *
 * See: docs/components-guide/components_guide_en.md
 */
@Composable
fun FlexSpacer(
    weight: Float = 1.0f,
    modifier: UIModifier = UIModifier
) {
    Box(modifier = UIModifier.weight(weight).then(modifier))
}
