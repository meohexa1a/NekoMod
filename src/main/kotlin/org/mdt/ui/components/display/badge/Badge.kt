package org.mdt.ui.components.display.badge

import androidx.compose.runtime.Composable
import arc.graphics.Color
import org.mdt.core.ui.compose.*
import org.mdt.core.ui.layout.Alignment
import org.mdt.ui.components.layout.Box
import org.mdt.ui.components.text.Text
import org.mdt.ui.theme.ColorTokens
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

    val backgroundColor = computeBadgeBackground(variant, colors)
    val textColor = computeBadgeText(variant, colors)
    val borderColor = computeBadgeBorder(variant, colors)

    Box(
        modifier = Modifier
            .radius(shapes.pill)
            .background(backgroundColor)
            .border(1f, borderColor)
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

// =========================================================================
// Pure Style Resolution Helpers (Zero-GC)
// =========================================================================

private fun computeBadgeBackground(variant: BadgeVariant, colors: ColorTokens): Color = when (variant) {
    BadgeVariant.DEFAULT -> colors.glassThin
    BadgeVariant.PRIMARY -> colors.systemBlue.cpy().apply { a = 0.20f }
    BadgeVariant.SUCCESS -> colors.systemGreen.cpy().apply { a = 0.20f }
    BadgeVariant.WARNING -> colors.systemOrange.cpy().apply { a = 0.20f }
    BadgeVariant.ERROR -> colors.systemRed.cpy().apply { a = 0.20f }
}

private fun computeBadgeText(variant: BadgeVariant, colors: ColorTokens): Color = when (variant) {
    BadgeVariant.DEFAULT -> colors.textSecondary
    BadgeVariant.PRIMARY -> colors.systemBlue
    BadgeVariant.SUCCESS -> colors.systemGreen
    BadgeVariant.WARNING -> colors.systemOrange
    BadgeVariant.ERROR -> colors.systemRed
}

private fun computeBadgeBorder(variant: BadgeVariant, colors: ColorTokens): Color = when (variant) {
    BadgeVariant.DEFAULT -> colors.glassBorderSubtle
    BadgeVariant.PRIMARY -> colors.systemBlue.cpy().apply { a = 0.40f }
    BadgeVariant.SUCCESS -> colors.systemGreen.cpy().apply { a = 0.40f }
    BadgeVariant.WARNING -> colors.systemOrange.cpy().apply { a = 0.40f }
    BadgeVariant.ERROR -> colors.systemRed.cpy().apply { a = 0.40f }
}
