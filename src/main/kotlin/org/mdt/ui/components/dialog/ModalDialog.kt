package org.mdt.ui.components.dialog

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import org.mdt.core.ui.compose.*
import org.mdt.core.ui.graphics.Color
import org.mdt.core.ui.layout.Alignment
import org.mdt.ui.components.layout.Box
import org.mdt.ui.components.surface.Card
import org.mdt.ui.components.surface.CardVariant
import org.mdt.ui.theme.Theme

/**
 * ## ModalDialog
 *
 * Highly flexible, unopinionated Apple iOS/macOS-style Frosted Glass Modal Dialog.
 * Provides a smooth animated backdrop scrim and pop-in scale presentation without
 * enforcing rigid UI scaffolds, allowing developers to craft completely custom layouts.
 *
 * @param visible Whether the modal dialog is currently active and visible.
 * @param onDismiss Callback invoked when clicking on the outside scrim backdrop.
 * @param modifier Chainable [UIModifier] applied to the dialog popover card.
 * @param dismissOnBackdropClick Whether clicking outside the dialog dismisses it (default true).
 * @param content Declarative custom dialog body and action controls.
 *
 * See: docs/design-system/design_system_en.md
 */
@Composable
fun ModalDialog(
    visible: Boolean,
    onDismiss: () -> Unit,
    modifier: UIModifier = UIModifier,
    dismissOnBackdropClick: Boolean = true,
    content: @Composable () -> Unit
) {
    val progress by animateFloatAsState(
        targetValue = if (visible) 1.0f else 0.0f,
        animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing)
    )

    if (progress <= 0.001f && !visible) return

    val colors = Theme.colors
    val shapes = Theme.shapes

    val scrimColor = colors.scrim.withAlpha(colors.scrim.a * progress)

    // 1. Smooth animated backdrop scrim
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(scrimColor)
            .then(
                if (dismissOnBackdropClick) {
                    Modifier.clickable { onDismiss() }
                } else Modifier
            )
    ) {
        // 2. Centered Frosted Glass Popover Card with Pop-in scale
        Card(
            variant = CardVariant.GLASS,
            modifier = Modifier
                .radius(shapes.xl)
                .align(Alignment.Center)
                .clickable { /* Absorb clicks inside dialog to prevent dismissal */ }
                .shadow(
                    color = Color(0f, 0f, 0f, 0.50f * progress),
                    blur = 24f * progress,
                    spread = 4f
                )
                .then(modifier)
        ) {
            content()
        }
    }
}
