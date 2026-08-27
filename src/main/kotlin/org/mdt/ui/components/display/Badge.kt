package org.mdt.ui.components.display

import androidx.compose.runtime.Composable
import org.mdt.core.ui.compose.*
import org.mdt.core.ui.graphics.Color
import org.mdt.core.ui.layout.Alignment
import org.mdt.ui.components.layout.Box
import org.mdt.ui.components.text.MonoText
import org.mdt.ui.theme.ColorTokens
import org.mdt.ui.theme.Theme

/**
 * ## BadgeVariant
 *
 * Visual style variants for status pills and indicator chips.
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
 *
 * See: docs/design-system/design_system_en.md
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
        MonoText(
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
    BadgeVariant.PRIMARY -> colors.blue.withAlpha(0.20f)
    BadgeVariant.SUCCESS -> colors.green.withAlpha(0.20f)
    BadgeVariant.WARNING -> colors.orange.withAlpha(0.20f)
    BadgeVariant.ERROR -> colors.red.withAlpha(0.20f)
}

private fun computeBadgeText(variant: BadgeVariant, colors: ColorTokens): Color = when (variant) {
    BadgeVariant.DEFAULT -> colors.textSecondary
    BadgeVariant.PRIMARY -> colors.blue
    BadgeVariant.SUCCESS -> colors.green
    BadgeVariant.WARNING -> colors.orange
    BadgeVariant.ERROR -> colors.red
}

private fun computeBadgeBorder(variant: BadgeVariant, colors: ColorTokens): Color = when (variant) {
    BadgeVariant.DEFAULT -> colors.borderHairline
    BadgeVariant.PRIMARY -> colors.blue.withAlpha(0.40f)
    BadgeVariant.SUCCESS -> colors.green.withAlpha(0.40f)
    BadgeVariant.WARNING -> colors.orange.withAlpha(0.40f)
    BadgeVariant.ERROR -> colors.red.withAlpha(0.40f)
}
