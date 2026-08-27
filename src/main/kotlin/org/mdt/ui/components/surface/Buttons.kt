package org.mdt.ui.components.surface

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.*
import org.mdt.core.engine.image.ImageSource
import org.mdt.core.ui.compose.*
import org.mdt.core.ui.graphics.Color
import org.mdt.core.ui.layout.Alignment
import org.mdt.core.ui.layout.Arrangement
import org.mdt.ui.components.display.Image
import org.mdt.ui.components.layout.Box
import org.mdt.ui.components.layout.Row
import org.mdt.ui.components.text.Text
import org.mdt.ui.theme.ColorTokens
import org.mdt.ui.theme.Theme

/**
 * ## ButtonVariant
 *
 * Apple iOS visual style variants for button surfaces.
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
 * ## Button (Slot-based Container)
 *
 * Apple iOS/macOS-style clickable button primitive supporting arbitrary custom composables,
 * featuring smooth color interpolation transitions on hover and press states.
 *
 * @param onClick Action callback invoked on click.
 * @param modifier Chainable [UIModifier].
 * @param variant Visual style variant ([ButtonVariant.FILLED], [ButtonVariant.GLASS], etc.).
 * @param enabled Whether the button is interactive.
 * @param content Declarative button interior content slot.
 *
 * See: docs/design-system/design_system_en.md
 */
@Composable
fun Button(
    onClick: () -> Unit,
    modifier: UIModifier = UIModifier,
    variant: ButtonVariant = ButtonVariant.FILLED,
    enabled: Boolean = true,
    content: @Composable BoxScope.() -> Unit
) {
    val colors = Theme.colors
    val shapes = Theme.shapes

    var isHovered by remember { mutableStateOf(false) }
    var isPressed by remember { mutableStateOf(false) }

    val hoverAnim by animateFloatAsState(
        targetValue = if (isHovered && enabled) 1f else 0f,
        animationSpec = tween(durationMillis = 150, easing = FastOutSlowInEasing)
    )
    val pressAnim by animateFloatAsState(
        targetValue = if (isPressed && enabled) 1f else 0f,
        animationSpec = tween(durationMillis = 80, easing = FastOutSlowInEasing)
    )

    val idleBg = computeButtonBackground(variant, enabled, isPressed = false, isHovered = false, colors)
    val hoverBg = computeButtonBackground(variant, enabled, isPressed = false, isHovered = true, colors)
    val pressBg = computeButtonBackground(variant, enabled, isPressed = true, isHovered = true, colors)

    val idleBorder = computeButtonBorder(variant, isHovered = false, colors)
    val hoverBorder = computeButtonBorder(variant, isHovered = true, colors)

    val idleShadow = computeButtonShadow(variant, isHovered = false, colors)
    val hoverShadow = computeButtonShadow(variant, isHovered = true, colors)

    val currentBg = idleBg.lerp(hoverBg, hoverAnim).lerp(pressBg, pressAnim)
    val currentBorder = idleBorder.lerp(hoverBorder, hoverAnim)
    val currentShadow = idleShadow.lerp(hoverShadow, hoverAnim)

    Box(
        modifier = Modifier
            .radius(shapes.pill)
            .background(currentBg)
            .border(1f, currentBorder)
            .shadow(currentShadow, blur = 6f + 2f * hoverAnim, spread = 0.5f)
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
 * ## TextButton
 *
 * Dedicated text button composable with optional leading vector icon.
 *
 * @param text Label string displayed inside the button.
 * @param onClick Action callback invoked on click.
 * @param modifier Chainable [UIModifier].
 * @param icon Optional leading icon resource or Mindustry icon.
 * @param iconTint Optional custom tint for the leading icon.
 * @param variant Visual style variant ([ButtonVariant]).
 * @param enabled Whether the button accepts clicks.
 *
 * See: docs/design-system/design_system_en.md
 */
@Composable
fun TextButton(
    text: String,
    onClick: () -> Unit,
    modifier: UIModifier = UIModifier,
    icon: Any? = null,
    iconTint: Color? = null,
    variant: ButtonVariant = ButtonVariant.FILLED,
    enabled: Boolean = true
) {
    val colors = Theme.colors

    val textColor = when (variant) {
        ButtonVariant.FILLED, ButtonVariant.DESTRUCTIVE -> colors.textOnAccent
        ButtonVariant.TINTED -> colors.blue
        ButtonVariant.GLASS, ButtonVariant.OUTLINED, ButtonVariant.PLAIN -> colors.textPrimary
    }

    Button(
        onClick = onClick,
        modifier = modifier,
        variant = variant,
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
                    color = if (enabled) textColor else colors.textQuaternary
                )
            }
        } else {
            Text(
                text = text,
                color = if (enabled) textColor else colors.textQuaternary,
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}

/**
 * ## Button (Convenience Text Overload)
 *
 * Alias routing to [TextButton] for clean DX.
 */
@Composable
fun Button(
    text: String,
    onClick: () -> Unit,
    modifier: UIModifier = UIModifier,
    icon: Any? = null,
    iconTint: Color? = null,
    variant: ButtonVariant = ButtonVariant.FILLED,
    enabled: Boolean = true
) = TextButton(
    text = text,
    onClick = onClick,
    modifier = modifier,
    icon = icon,
    iconTint = iconTint,
    variant = variant,
    enabled = enabled
)

/**
 * ## IconButton
 *
 * Compact circular or square Frosted Glass action button with smooth hover animation.
 *
 * @param icon Image source for the button icon.
 * @param onClick Action callback invoked on click.
 * @param modifier Chainable [UIModifier]. Use `Modifier.size(...)` to customize dimensions.
 * @param variant Visual style variant ([ButtonVariant.GLASS] by default).
 * @param enabled Whether button accepts click interactions.
 * @param tint Icon tint color.
 *
 * See: docs/design-system/design_system_en.md
 */
@Composable
fun IconButton(
    icon: Any,
    onClick: () -> Unit,
    modifier: UIModifier = UIModifier,
    variant: ButtonVariant = ButtonVariant.GLASS,
    enabled: Boolean = true,
    tint: Color = Theme.colors.textPrimary
) {
    val colors = Theme.colors
    val shapes = Theme.shapes

    var isHovered by remember { mutableStateOf(false) }
    var isPressed by remember { mutableStateOf(false) }

    val hoverAnim by animateFloatAsState(
        targetValue = if (isHovered && enabled) 1f else 0f,
        animationSpec = tween(durationMillis = 140, easing = FastOutSlowInEasing)
    )
    val pressAnim by animateFloatAsState(
        targetValue = if (isPressed && enabled) 1f else 0f,
        animationSpec = tween(durationMillis = 80, easing = FastOutSlowInEasing)
    )

    val idleBg = computeButtonBackground(variant, enabled, isPressed = false, isHovered = false, colors)
    val hoverBg = computeButtonBackground(variant, enabled, isPressed = false, isHovered = true, colors)
    val pressBg = computeButtonBackground(variant, enabled, isPressed = true, isHovered = true, colors)

    val idleBorder = computeButtonBorder(variant, isHovered = false, colors)
    val hoverBorder = computeButtonBorder(variant, isHovered = true, colors)

    val currentBg = idleBg.lerp(hoverBg, hoverAnim).lerp(pressBg, pressAnim)
    val currentBorder = idleBorder.lerp(hoverBorder, hoverAnim)

    Box(
        modifier = Modifier
            .size(36f)
            .radius(shapes.pill)
            .background(currentBg)
            .border(width = 1f, color = currentBorder)
            .shadow(
                color = colors.shadowAmbient.withAlpha(colors.shadowAmbient.a * hoverAnim),
                blur = 4f * hoverAnim,
                spread = 0.5f
            )
            .hoverable { if (enabled) isHovered = it }
            .clickable(
                onClick = { if (enabled) onClick() },
                onPressStateChanged = { if (enabled) isPressed = it }
            )
            .then(modifier)
    ) {
        Image(
            source = ImageSource.from(icon),
            tint = if (enabled) tint else colors.textQuaternary,
            modifier = Modifier
                .fillMaxSize()
                .pad(8f)
                .align(Alignment.Center)
        )
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
        val base = colors.blue
        when {
            !enabled -> colors.glassThin
            isPressed -> base.mul(0.80f)
            isHovered -> base.mul(1.15f)
            else -> base
        }
    }
    ButtonVariant.TINTED -> {
        val base = colors.blue
        when {
            !enabled -> colors.glassUltraThin
            isPressed -> base.withAlpha(0.35f)
            isHovered -> base.withAlpha(0.25f)
            else -> base.withAlpha(0.15f)
        }
    }
    ButtonVariant.GLASS -> when {
        !enabled -> colors.glassUltraThin
        isPressed -> colors.glassActive
        isHovered -> colors.glassThick
        else -> colors.glassRegular
    }
    ButtonVariant.OUTLINED, ButtonVariant.PLAIN -> when {
        !enabled -> Color.Clear
        isPressed -> colors.glassThin
        isHovered -> colors.glassUltraThin
        else -> Color.Clear
    }
    ButtonVariant.DESTRUCTIVE -> {
        val base = colors.red
        when {
            !enabled -> colors.glassThin
            isPressed -> base.mul(0.80f)
            isHovered -> base.mul(1.15f)
            else -> base
        }
    }
}

private fun computeButtonBorder(variant: ButtonVariant, isHovered: Boolean, colors: ColorTokens): Color = when (variant) {
    ButtonVariant.GLASS -> if (isHovered) colors.borderHairline else Color.Clear
    ButtonVariant.OUTLINED -> if (isHovered) colors.borderActive else colors.borderHairline
    else -> Color.Clear
}

private fun computeButtonShadow(variant: ButtonVariant, isHovered: Boolean, colors: ColorTokens): Color = when (variant) {
    ButtonVariant.FILLED -> if (isHovered) colors.glowAccent else Color.Clear
    ButtonVariant.GLASS -> if (isHovered) colors.shadowAmbient else Color.Clear
    ButtonVariant.DESTRUCTIVE -> if (isHovered) colors.red.withAlpha(0.4f) else Color.Clear
    else -> Color.Clear
}
