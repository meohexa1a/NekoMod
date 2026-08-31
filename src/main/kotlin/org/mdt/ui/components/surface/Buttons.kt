// [AGENT INVARIANT] Synchronously update @property, @param, and @see KDocs when modifying this file.

@file:Suppress("FunctionName", "unused")

package org.mdt.ui.components.surface

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import arc.graphics.g2d.TextureRegion
import org.mdt.core.ui.layout.BoxScope
import org.mdt.core.ui.modifier.UIModifier
import org.mdt.core.ui.unit.Alignment
import org.mdt.core.ui.unit.Color
import org.mdt.core.ui.modifier.background
import org.mdt.core.ui.modifier.border
import org.mdt.core.ui.modifier.clickable
import org.mdt.core.ui.modifier.glass
import org.mdt.core.ui.modifier.hoverable
import org.mdt.core.ui.modifier.pad
import org.mdt.core.ui.modifier.radius
import org.mdt.core.ui.modifier.size
import org.mdt.core.ui.unit.Insets
import org.mdt.ui.components.display.Image
import org.mdt.ui.components.layout.Box
import org.mdt.ui.components.text.Text
import org.mdt.ui.theme.ButtonDefaults

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
    contentPadding: Insets = ButtonDefaults.contentPadding,
    content: @Composable BoxScope.() -> Unit
) {
    var isHovered by remember { mutableStateOf(false) }
    var isPressed by remember { mutableStateOf(false) }

    val accentBlue = Color.valueOf("0a84ff")
    val glassBg = Color(0.25f, 0.25f, 0.35f, 0.35f)

    val currentBg = when (variant) {
        ButtonVariant.FILLED -> when {
            isPressed -> accentBlue.mul(0.8f)
            isHovered -> accentBlue.mul(1.15f)
            else -> accentBlue
        }
        ButtonVariant.TINTED -> when {
            isPressed -> accentBlue.withAlpha(0.35f)
            isHovered -> accentBlue.withAlpha(0.25f)
            else -> accentBlue.withAlpha(0.15f)
        }
        ButtonVariant.GLASS -> when {
            isPressed -> glassBg.withAlpha(0.65f)
            isHovered -> glassBg.withAlpha(0.50f)
            else -> glassBg
        }
        ButtonVariant.OUTLINED -> when {
            isPressed -> Color(1.0f, 1.0f, 1.0f, 0.15f)
            isHovered -> Color(1.0f, 1.0f, 1.0f, 0.08f)
            else -> Color.Clear
        }
    }

    val currentBorder = when (variant) {
        ButtonVariant.GLASS -> when {
            isHovered -> Color(1.0f, 1.0f, 1.0f, 0.45f)
            else -> Color(1.0f, 1.0f, 1.0f, 0.25f)
        }
        ButtonVariant.OUTLINED -> when {
            isHovered -> accentBlue
            else -> Color(1.0f, 1.0f, 1.0f, 0.3f)
        }
        else -> Color.Clear
    }

    Box(
        modifier = UIModifier
            .radius(ButtonDefaults.radius)
            .background(currentBg)
            .border(ButtonDefaults.borderWidth, currentBorder)
            .glass(variant == ButtonVariant.GLASS)
            .pad(
                left = contentPadding.left,
                top = contentPadding.top,
                right = contentPadding.right,
                bottom = contentPadding.bottom
            )
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
        modifier = UIModifier.size(36.0f).then(modifier),
        variant = variant,
        enabled = enabled,
        contentPadding = Insets.Zero
    ) {
        Image(
            region = region,
            tint = tint,
            modifier = UIModifier.size(18.0f).align(Alignment.Center)
        )
    }
}
