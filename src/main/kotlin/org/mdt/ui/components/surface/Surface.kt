// [AGENT INVARIANT] Synchronously update @property, @param, and @see KDocs when modifying this file.

package org.mdt.ui.components.surface

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import org.mdt.core.ui.layout.BoxScope
import org.mdt.core.ui.modifier.UIModifier
import org.mdt.core.ui.modifier.background
import org.mdt.core.ui.modifier.border
import org.mdt.core.ui.modifier.clickable
import org.mdt.core.ui.modifier.glass
import org.mdt.core.ui.modifier.hoverable
import org.mdt.core.ui.modifier.opaque
import org.mdt.core.ui.modifier.pad
import org.mdt.core.ui.modifier.radius
import org.mdt.core.ui.unit.Color
import org.mdt.core.ui.unit.Insets
import org.mdt.ui.components.layout.Box
import org.mdt.ui.theme.LocalContentColor
import org.mdt.ui.theme.ThemeTokens

/**
 * ## Surface
 *
 * Foundational container surface providing unified background shape, corner radius,
 * border outline, frosted glassmorphism, pointer interception, and ambient content color.
 *
 * @param modifier Chainable [UIModifier].
 * @param color Surface background fill color.
 * @param contentColor Foreground content color pushed to [LocalContentColor].
 * @param radius Corner radius in pixels.
 * @param borderWidth Outline border stroke thickness in pixels.
 * @param borderColor Outline border stroke color.
 * @param isGlass Whether frosted background blur is enabled.
 * @param contentPadding Inward padding insets for child content.
 * @param onClick Optional click handler making this surface interactive.
 * @param onPressStateChanged Optional callback receiving press/release interaction states.
 * @param onHoverStateChanged Optional callback receiving hover interaction states.
 * @param content Composable slot receiving [BoxScope].
 *
 * @see LocalContentColor
 * @see Card
 * @see Button
 */
@Composable
fun Surface(
    modifier: UIModifier = UIModifier,
    color: Color = ThemeTokens.surface,
    contentColor: Color = ThemeTokens.textPrimary,
    radius: Float = 0.0f,
    borderWidth: Float = 0.0f,
    borderColor: Color = Color.Clear,
    isGlass: Boolean = false,
    contentPadding: Insets = Insets.Zero,
    onClick: (() -> Unit)? = null,
    onPressStateChanged: ((Boolean) -> Unit)? = null,
    onHoverStateChanged: ((Boolean) -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    var surfaceModifier = modifier
        .opaque()
        .radius(radius)
        .background(color)
        .border(borderWidth, borderColor)
        .glass(isGlass)
        .pad(
            left = contentPadding.left,
            top = contentPadding.top,
            right = contentPadding.right,
            bottom = contentPadding.bottom
        )

    if (onHoverStateChanged != null) {
        surfaceModifier = surfaceModifier.hoverable(onHoverStateChanged)
    }

    if (onClick != null) {
        surfaceModifier = surfaceModifier.clickable(
            onClick = onClick,
            onPressStateChanged = onPressStateChanged
        )
    }

    CompositionLocalProvider(LocalContentColor provides contentColor) {
        Box(
            modifier = surfaceModifier,
            content = content
        )
    }
}
