// [AGENT INVARIANT] Synchronously update @property, @param, and @see KDocs when modifying this file.

package org.mdt.ui.components.surface

import androidx.compose.runtime.Composable
import org.mdt.core.ui.layout.BoxScope
import org.mdt.core.ui.modifier.UIModifier
import org.mdt.core.ui.unit.Color
import org.mdt.core.ui.unit.Insets
import org.mdt.ui.theme.CardDefaults
import org.mdt.ui.theme.ThemeTokens

/**
 * ## Card
 *
 * Visual group container surface built on [Surface] with customizable padding, radius, and glassmorphism.
 * Intercepts pointer events (`opaque`) to prevent clicks passing through to underlying gameplay.
 *
 * @param modifier Chainable [UIModifier].
 * @param color Card surface fill color.
 * @param contentColor Foreground text/icon color passed to [org.mdt.ui.theme.LocalContentColor].
 * @param radius Corner radius in pixels.
 * @param borderWidth Outline border stroke thickness in pixels.
 * @param borderColor Outline border stroke color.
 * @param isGlass Whether background frosted glass is active.
 * @param contentPadding Inward padding insets for card contents.
 * @param content Composable slot receiving [BoxScope].
 *
 * @see Surface
 * @see CardDefaults
 */
@Composable
fun Card(
    modifier: UIModifier = UIModifier,
    color: Color = CardDefaults.color,
    contentColor: Color = CardDefaults.contentColor,
    radius: Float = CardDefaults.radius,
    borderWidth: Float = CardDefaults.borderWidth,
    borderColor: Color = CardDefaults.borderColor,
    isGlass: Boolean = true,
    contentPadding: Insets = CardDefaults.contentPadding,
    content: @Composable BoxScope.() -> Unit
) {
    Surface(
        modifier = modifier,
        color = color,
        contentColor = contentColor,
        radius = radius,
        borderWidth = borderWidth,
        borderColor = borderColor,
        isGlass = isGlass,
        contentPadding = contentPadding,
        content = content
    )
}
