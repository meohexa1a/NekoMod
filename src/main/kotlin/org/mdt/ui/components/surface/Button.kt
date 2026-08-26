@file:Suppress("FunctionName", "unused")

package org.mdt.ui.components.surface

import androidx.compose.runtime.*
import arc.graphics.Color
import arc.graphics.g2d.Font
import arc.util.Align
import mindustry.ui.Fonts
import org.mdt.core.engine.image.ImageSource
import org.mdt.core.ui.compose.*
import org.mdt.core.ui.layout.Alignment
import org.mdt.core.ui.layout.Arrangement
import org.mdt.ui.components.display.image.Image
import org.mdt.ui.components.layout.Box
import org.mdt.ui.components.layout.Row
import org.mdt.ui.components.text.Text
import org.mdt.ui.theme.ColorTokens
import org.mdt.ui.theme.Theme

/**
 * ## ButtonVariant
 *
 * Apple iOS visual style variants for buttons.
 */
enum class ButtonVariant {
    /** Prominent solid accent button (System Blue). */
    FILLED,

    /** Translucent tinted background with accent text. */
    TINTED,

    /** Frosted glass button with subtle specular border. */
    GLASS,

    /** Clean hairline border with transparent background. */
    OUTLINED,

    /** Minimal borderless text button. */
    PLAIN,

    /** Warning/destructive action button (System Red). */
    DESTRUCTIVE
}

/**
 * ## Button (Slot-based)
 *
 * Apple iOS-style clickable button supporting arbitrary custom composable content.
 *
 * @param onClick Action callback invoked on click.
 * @param modifier Chainable [UIModifier].
 * @param variant Visual style variant ([ButtonVariant.FILLED], [ButtonVariant.GLASS], etc.).
 * @param radius Corner radius in pixels (defaults to [Theme.shapes.pill]).
 * @param enabled Whether the button is interactive.
 * @param content Declarative button interior content.
 */
@Composable
fun Button(
    onClick: () -> Unit,
    modifier: UIModifier = UIModifier,
    variant: ButtonVariant = ButtonVariant.FILLED,
    radius: Float = Theme.shapes.pill,
    enabled: Boolean = true,
    content: @Composable BoxScope.() -> Unit
) {
    val colors = Theme.colors

    var isHovered by remember { mutableStateOf(false) }
    var isPressed by remember { mutableStateOf(false) }

    val backgroundColor = computeButtonBackground(variant, enabled, isPressed, isHovered, colors)
    val borderColor = computeButtonBorder(variant, isHovered, colors)
    val shadowColor = computeButtonShadow(variant, isHovered, colors)

    Box(
        modifier = Modifier
            .radius(radius)
            .background(backgroundColor)
            .border(1f, borderColor)
            .shadow(shadowColor, blur = 8f, spread = 1f)
            .pad(horizontal = 16f, vertical = 8f)
            .hoverable { if (enabled) isHovered = it }
            .clickable(
                onClick = { if (enabled) onClick() },
                onPressStateChanged = { if (enabled) isPressed = it }
            )
            .then(modifier),
        content = content
    )
}

/**
 * ## Button (Text & Icon overload)
 *
 * Convenient declarative text button with optional Mindustry vector icon.
 */
@Composable
fun Button(
    text: String,
    onClick: () -> Unit,
    modifier: UIModifier = UIModifier,
    icon: Any? = null,
    iconTint: Color? = null,
    variant: ButtonVariant = ButtonVariant.FILLED,
    radius: Float = Theme.shapes.pill,
    paddingH: Float = 16f,
    paddingV: Float = 8f,
    font: Font = Fonts.def,
    textAlign: Int = Align.center,
    enabled: Boolean = true,
    textModifier: UIModifier = UIModifier
) {
    val colors = Theme.colors

    val textColor = when (variant) {
        ButtonVariant.FILLED, ButtonVariant.DESTRUCTIVE -> colors.textOnAccent
        ButtonVariant.TINTED -> colors.systemBlue
        ButtonVariant.GLASS, ButtonVariant.OUTLINED, ButtonVariant.PLAIN -> colors.textPrimary
    }

    Button(
        onClick = onClick,
        modifier = Modifier.pad(horizontal = paddingH, vertical = paddingV).then(modifier),
        variant = variant,
        radius = radius,
        enabled = enabled
    ) {
        if (icon != null) {
            Row(
                arrangement = Arrangement.spacedBy(8f),
                alignment = Alignment.Center,
                modifier = Modifier.align(Alignment.Center)
            ) {
                Image(
                    source = ImageSource.from(icon),
                    tint = iconTint ?: textColor,
                    modifier = Modifier.size(16f)
                )
                Text(
                    text = text,
                    color = if (enabled) textColor else colors.textQuaternary,
                    font = font,
                    scale = 1.0f,
                    align = textAlign,
                    modifier = textModifier
                )
            }
        } else {
            Text(
                text = text,
                color = if (enabled) textColor else colors.textQuaternary,
                font = font,
                scale = 1.0f,
                align = textAlign,
                modifier = Modifier.align(Alignment.Center).then(textModifier)
            )
        }
    }
}

// =========================================================================
// Pure Style Resolution Helpers (Zero-GC)
// =========================================================================

private fun computeButtonBackground(
    variant: ButtonVariant,
    enabled: Boolean,
    isPressed: Boolean,
    isHovered: Boolean,
    colors: ColorTokens
): Color = when (variant) {
    ButtonVariant.FILLED -> {
        val base = colors.systemBlue
        when {
            !enabled -> colors.glassThin
            isPressed -> base.cpy().mul(0.80f)
            isHovered -> base.cpy().mul(1.15f)
            else -> base
        }
    }
    ButtonVariant.TINTED -> {
        val base = colors.systemBlue
        when {
            !enabled -> colors.glassUltraThin
            isPressed -> base.cpy().apply { a = 0.35f }
            isHovered -> base.cpy().apply { a = 0.25f }
            else -> base.cpy().apply { a = 0.15f }
        }
    }
    ButtonVariant.GLASS -> when {
        !enabled -> colors.glassUltraThin
        isPressed -> colors.glassActive
        isHovered -> colors.glassThick
        else -> colors.glassRegular
    }
    ButtonVariant.OUTLINED, ButtonVariant.PLAIN -> when {
        !enabled -> Color.clear
        isPressed -> colors.glassThin
        isHovered -> colors.glassUltraThin
        else -> Color.clear
    }
    ButtonVariant.DESTRUCTIVE -> {
        val base = colors.systemRed
        when {
            !enabled -> colors.glassThin
            isPressed -> base.cpy().mul(0.80f)
            isHovered -> base.cpy().mul(1.15f)
            else -> base
        }
    }
}

private fun computeButtonBorder(variant: ButtonVariant, isHovered: Boolean, colors: ColorTokens): Color = when (variant) {
    ButtonVariant.GLASS -> if (isHovered) colors.glassBorderSubtle else Color.clear
    ButtonVariant.OUTLINED -> if (isHovered) colors.glassBorderActive else colors.glassBorderSubtle
    else -> Color.clear
}

private fun computeButtonShadow(variant: ButtonVariant, isHovered: Boolean, colors: ColorTokens): Color = when (variant) {
    ButtonVariant.FILLED -> if (isHovered) colors.glowAccent else Color.clear
    ButtonVariant.GLASS -> if (isHovered) colors.shadowAmbient else Color.clear
    ButtonVariant.DESTRUCTIVE -> if (isHovered) colors.systemRed.cpy().apply { a = 0.4f } else Color.clear
    else -> Color.clear
}
