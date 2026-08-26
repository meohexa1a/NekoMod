@file:Suppress("FunctionName", "unused")

package org.mdt.ui.components.surface

import androidx.compose.runtime.Composable
import arc.graphics.Color
import org.mdt.core.ui.compose.*
import org.mdt.ui.components.layout.Box
import org.mdt.ui.theme.Theme

/**
 * ## Divider
 *
 * Subtle hairline glass separator line.
 *
 * @param modifier Chainable [UIModifier].
 * @param color Line color (defaults to [Theme.colors.divider]).
 * @param thickness Line thickness in pixels (defaults to 1f).
 */
@Composable
fun Divider(
    modifier: UIModifier = UIModifier,
    color: Color = Theme.colors.divider,
    thickness: Float = 1f
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(thickness)
            .background(color)
            .then(modifier)
    )
}
