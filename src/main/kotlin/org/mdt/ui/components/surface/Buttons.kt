// [AGENT INVARIANT] Synchronously update @property, @param, and @see KDocs when modifying this file.

@file:Suppress("FunctionName", "unused")

package org.mdt.ui.components.surface

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import arc.graphics.g2d.TextureRegion
import org.mdt.core.ui.layout.RowScope
import org.mdt.ui.components.layout.Row
import org.mdt.core.ui.modifier.UIModifier
import org.mdt.core.ui.modifier.size
import org.mdt.core.ui.unit.Alignment
import org.mdt.core.ui.unit.Arrangement
import org.mdt.core.ui.unit.Color
import org.mdt.core.ui.unit.Insets
import org.mdt.ui.components.display.Image
import org.mdt.ui.components.text.Text
import org.mdt.ui.theme.ButtonColors
import org.mdt.ui.theme.ButtonDefaults

/**
 * ## Button
 *
 * Primary interactive button built on [Surface] with a slot-based [RowScope] layout.
 * Supports custom [ButtonColors] data palettes, stateful hover/press color shifts,
 * and ambient foreground [org.mdt.ui.theme.LocalContentColor].
 *
 * @param onClick Action invoked when the button is clicked.
 * @param modifier Chainable [UIModifier].
 * @param enabled Whether this button responds to user interaction.
 * @param colors Interactive container and content color palette ([ButtonColors]).
 * @param radius Corner radius in pixels.
 * @param borderWidth Outline border stroke thickness in pixels.
 * @param isGlass Whether frosted background glassmorphism is enabled.
 * @param contentPadding Inward padding insets for button content.
 * @param content Slot receiving [RowScope] for placing text, icons, and badges.
 *
 * @see Surface
 * @see ButtonColors
 * @see ButtonDefaults
 */
@Composable
fun Button(
    onClick: () -> Unit,
    modifier: UIModifier = UIModifier,
    enabled: Boolean = true,
    colors: ButtonColors = ButtonDefaults.filled(),
    radius: Float = ButtonDefaults.radius,
    borderWidth: Float = ButtonDefaults.borderWidth,
    isGlass: Boolean = false,
    contentPadding: Insets = ButtonDefaults.contentPadding,
    content: @Composable RowScope.() -> Unit
) {
    var isHovered by remember { mutableStateOf(false) }
    var isPressed by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier,
        color = colors.currentContainer(isHovered, isPressed),
        contentColor = colors.contentColor,
        radius = radius,
        borderWidth = borderWidth,
        borderColor = colors.currentBorder(isHovered),
        isGlass = isGlass,
        contentPadding = contentPadding,
        onClick = { if (enabled) onClick() },
        onPressStateChanged = { if (enabled) isPressed = it },
        onHoverStateChanged = { if (enabled) isHovered = it }
    ) {
        Row(
            arrangement = Arrangement.Center,
            alignment = Alignment.Center,
            content = content
        )
    }
}

// --- CONVENIENCE OVERLOADS ---

/**
 * ## Button (Convenience Text Label Overload)
 *
 * Interactive button rendering a single text label with automatic ambient contrast color.
 *
 * @param text Button label string.
 * @param onClick Action invoked on click.
 * @param modifier Chainable [UIModifier].
 * @param colors Interactive color palette ([ButtonColors]).
 * @param enabled Whether interaction is enabled.
 * @param isGlass Whether background frosted glass is enabled.
 */
@Composable
fun Button(
    text: String,
    onClick: () -> Unit,
    modifier: UIModifier = UIModifier,
    colors: ButtonColors = ButtonDefaults.filled(),
    enabled: Boolean = true,
    isGlass: Boolean = false
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        colors = colors,
        enabled = enabled,
        isGlass = isGlass
    ) {
        Text(text = text)
    }
}

/**
 * ## IconButton
 *
 * Compact icon-centric action button with centered graphic region.
 *
 * @param region Texture region icon to display.
 * @param onClick Action invoked on click.
 * @param modifier Chainable [UIModifier].
 * @param colors Interactive color palette ([ButtonColors]).
 * @param enabled Whether interaction is enabled.
 * @param tint Tint color override (defaults to [org.mdt.ui.theme.LocalContentColor]).
 * @param isGlass Whether background frosted glass is enabled.
 */
@Composable
fun IconButton(
    region: TextureRegion,
    onClick: () -> Unit,
    modifier: UIModifier = UIModifier,
    colors: ButtonColors = ButtonDefaults.glass(),
    enabled: Boolean = true,
    tint: Color = Color.Unspecified,
    isGlass: Boolean = true
) {
    Button(
        onClick = onClick,
        modifier = modifier.size(36.0f),
        colors = colors,
        enabled = enabled,
        isGlass = isGlass,
        contentPadding = Insets.Zero
    ) {
        Image(
            region = region,
            tint = tint,
            modifier = UIModifier.size(18.0f)
        )
    }
}
