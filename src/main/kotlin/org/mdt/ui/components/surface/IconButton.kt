@file:Suppress("FunctionName", "unused")

package org.mdt.ui.components.surface

import androidx.compose.runtime.*
import arc.graphics.Color
import org.mdt.core.engine.image.ImageSource
import org.mdt.core.ui.compose.*
import org.mdt.core.ui.layout.Alignment
import org.mdt.ui.components.display.image.Image
import org.mdt.ui.components.layout.Box
import org.mdt.ui.theme.Theme

/**
 * ## IconButton
 *
 * Circular or square Frosted Glass action button.
 *
 * @param icon Image source for the button icon.
 * @param onClick Action callback invoked on click.
 * @param size Button dimension in pixels (default 36f).
 * @param tint Icon tint color.
 * @param modifier Chainable [UIModifier].
 */
@Composable
fun IconButton(
    icon: Any,
    onClick: () -> Unit,
    size: Float = 36f,
    tint: Color = Theme.colors.textPrimary,
    modifier: UIModifier = UIModifier
) {
    val colors = Theme.colors
    val shapes = Theme.shapes

    var isHovered by remember { mutableStateOf(false) }
    var isPressed by remember { mutableStateOf(false) }

    val bg = when {
        isPressed -> colors.glassActive
        isHovered -> colors.glassThick
        else -> colors.glassThin
    }

    val border = when {
        isHovered -> colors.glassBorderActive
        else -> colors.glassBorderSubtle
    }

    Box(
        modifier = Modifier
            .size(size)
            .radius(shapes.pill)
            .background(bg)
            .border(width = 1f, color = border)
            .hoverable { isHovered = it }
            .clickable(onPressStateChanged = { isPressed = it }) { onClick() }
            .then(modifier)
    ) {
        Image(
            source = ImageSource.from(icon),
            tint = tint,
            modifier = Modifier
                .size(size * 0.55f)
                .align(Alignment.Center)
        )
    }
}
