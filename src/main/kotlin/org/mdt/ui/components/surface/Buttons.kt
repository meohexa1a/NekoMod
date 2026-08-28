@file:Suppress("FunctionName", "unused")

package org.mdt.ui.components.surface

import androidx.compose.runtime.*
import arc.graphics.g2d.TextureRegion
import org.mdt.core.ui.compose.*
import org.mdt.core.ui.graphics.Color
import org.mdt.core.ui.layout.Alignment
import org.mdt.ui.components.display.Image
import org.mdt.ui.components.layout.Box
import org.mdt.ui.components.text.Text

// --- BUTTON VARIANTS ---

/**
 * ## ButtonVariant
 *
 * Visual styling variants for interactive buttons.
 *
 * See: docs/components-guide/components_guide_en.md
 */
enum class ButtonVariant {
    FILLED,
    TINTED,
    GLASS,
    OUTLINED
}

// --- BUTTON COMPOSABLE ---

/**
 * ## Button
 *
 * Clickable interactive container supporting filled, tinted, glassmorphic, and outlined styles.
 *
 * See: docs/components-guide/components_guide_en.md
 */
@Composable
fun Button(
    onClick: () -> Unit,
    modifier: UIModifier = UIModifier,
    variant: ButtonVariant = ButtonVariant.FILLED,
    enabled: Boolean = true,
    content: @Composable BoxScope.() -> Unit
) {
    var isHovered by remember { mutableStateOf(false) }
    var isPressed by remember { mutableStateOf(false) }

    val accentBlue = Color.valueOf("0a84ff")
    val glassBg = Color(0.25f, 0.25f, 0.35f, 0.35f)

    val currentBg = when (variant) {
        ButtonVariant.FILLED -> {
            if (isPressed) accentBlue.mul(0.8f)
            else if (isHovered) accentBlue.mul(1.15f)
            else accentBlue
        }
        ButtonVariant.TINTED -> {
            if (isPressed) accentBlue.withAlpha(0.35f)
            else if (isHovered) accentBlue.withAlpha(0.25f)
            else accentBlue.withAlpha(0.15f)
        }
        ButtonVariant.GLASS -> {
            if (isPressed) glassBg.withAlpha(0.65f)
            else if (isHovered) glassBg.withAlpha(0.50f)
            else glassBg
        }
        ButtonVariant.OUTLINED -> {
            if (isPressed) Color(1.0f, 1.0f, 1.0f, 0.15f)
            else if (isHovered) Color(1.0f, 1.0f, 1.0f, 0.08f)
            else Color.Clear
        }
    }

    val currentBorder = when (variant) {
        ButtonVariant.GLASS -> {
            if (isHovered) Color(1.0f, 1.0f, 1.0f, 0.45f)
            else Color(1.0f, 1.0f, 1.0f, 0.25f)
        }
        ButtonVariant.OUTLINED -> {
            if (isHovered) accentBlue
            else Color(1.0f, 1.0f, 1.0f, 0.3f)
        }
        else -> Color.Clear
    }

    Box(
        modifier = UIModifier
            .radius(8.0f)
            .background(currentBg)
            .border(1.0f, currentBorder)
            .glass(variant == ButtonVariant.GLASS)
            .pad(horizontal = 16.0f, vertical = 8.0f)
            .hoverable { if (enabled) isHovered = it }
            .clickable(
                onClick = { if (enabled) onClick() },
                onPressStateChanged = { if (enabled) isPressed = it }
            )
            .then(modifier),
        content = content
    )
}

// --- CONVENIENCE OVERLOADS ---

/**
 * ## Button (Convenience Text Overload)
 *
 * Clickable button with centered text label.
 *
 * See: docs/components-guide/components_guide_en.md
 */
@Composable
fun Button(
    text: String,
    onClick: () -> Unit,
    modifier: UIModifier = UIModifier,
    variant: ButtonVariant = ButtonVariant.FILLED,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        variant = variant,
        enabled = enabled
    ) {
        Text(
            text = text,
            color = Color.White,
            modifier = UIModifier.align(Alignment.Center)
        )
    }
}

/**
 * ## IconButton
 *
 * Compact action button with centered icon.
 *
 * See: docs/components-guide/components_guide_en.md
 */
@Composable
fun IconButton(
    region: TextureRegion,
    onClick: () -> Unit,
    modifier: UIModifier = UIModifier,
    variant: ButtonVariant = ButtonVariant.GLASS,
    enabled: Boolean = true,
    tint: Color = Color.White
) {
    Button(
        onClick = onClick,
        modifier = UIModifier.size(36.0f).pad(0.0f).then(modifier),
        variant = variant,
        enabled = enabled
    ) {
        Image(
            region = region,
            tint = tint,
            modifier = UIModifier.size(18.0f).align(Alignment.Center)
        )
    }
}
