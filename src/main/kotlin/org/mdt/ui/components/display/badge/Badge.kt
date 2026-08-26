@file:Suppress("FunctionName", "unused")

package org.mdt.ui.components.display.badge

import androidx.compose.runtime.Composable
import arc.graphics.Color
import org.mdt.core.ui.compose.*
import org.mdt.core.ui.layout.Alignment
import org.mdt.ui.components.layout.Box
import org.mdt.ui.components.text.Text
import org.mdt.ui.theme.Theme

/**
 * ## BadgeVariant
 *
 * Visual style variants for status pills and chips.
 */
enum class BadgeVariant {
    DEFAULT,
    PRIMARY,
    SUCCESS,
    WARNING,
    ERROR
}

/**
 * ## Badge
 *
 * Compact Apple iOS-style status tag and indicator chip.
 *
 * @param text Badge text label.
 * @param variant Visual color style (Default, Primary, Success, Warning, Error).
 * @param modifier Chainable [UIModifier].
 */
@Composable
fun Badge(
    text: String,
    variant: BadgeVariant = BadgeVariant.DEFAULT,
    modifier: UIModifier = UIModifier
) {
    val colors = Theme.colors
    val shapes = Theme.shapes

    val (bg, textColor, borderColor) = when (variant) {
        BadgeVariant.DEFAULT -> Triple(colors.glassThin, colors.textSecondary, colors.glassBorderSubtle)
        BadgeVariant.PRIMARY -> Triple(colors.systemBlue.cpy().apply { a = 0.20f }, colors.systemBlue, colors.systemBlue.cpy().apply { a = 0.40f })
        BadgeVariant.SUCCESS -> Triple(colors.systemGreen.cpy().apply { a = 0.20f }, colors.systemGreen, colors.systemGreen.cpy().apply { a = 0.40f })
        BadgeVariant.WARNING -> Triple(colors.systemOrange.cpy().apply { a = 0.20f }, colors.systemOrange, colors.systemOrange.cpy().apply { a = 0.40f })
        BadgeVariant.ERROR -> Triple(colors.systemRed.cpy().apply { a = 0.20f }, colors.systemRed, colors.systemRed.cpy().apply { a = 0.40f })
    }

    Box(
        modifier = Modifier
            .radius(shapes.pill)
            .background(bg)
            .pad(horizontal = 8f, vertical = 2f)
            .then(modifier)
    ) {
        Text(
            text = text,
            color = textColor,
            modifier = Modifier.align(Alignment.Center)
        )
    }
}
