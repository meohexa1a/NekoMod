@file:Suppress("FunctionName", "unused")

package org.mdt.ui.components.surface

import androidx.compose.runtime.Composable
import arc.graphics.Color
import org.mdt.ui.components.layout.Box
import org.mdt.ui.compose.UIModifier
import org.mdt.ui.compose.background
import org.mdt.ui.compose.fillMaxWidth
import org.mdt.ui.compose.height

/**
 * ## Divider
 *
 * Thin horizontal visual divider line.
 */
@Composable
fun Divider(
    modifier: UIModifier = UIModifier,
    color: Color = Color.valueOf("363a4f"),
    thickness: Float = 1f
) {
    Box(
        modifier = UIModifier
            .fillMaxWidth()
            .height(thickness)
            .background(color)
            .then(modifier)
    )
}
