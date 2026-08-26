@file:Suppress("FunctionName", "unused")

package org.mdt.ui.components.dialog

import androidx.compose.runtime.Composable
import org.mdt.core.ui.compose.*
import org.mdt.core.ui.layout.Alignment
import org.mdt.core.ui.layout.Arrangement
import org.mdt.ui.components.layout.Box
import org.mdt.ui.components.layout.Column
import org.mdt.ui.components.surface.Card
import org.mdt.ui.components.surface.CardVariant
import org.mdt.ui.components.text.Text
import org.mdt.ui.theme.Theme

/**
 * ## ModalDialog
 *
 * Centered Apple iOS-style Frosted Glass Modal Dialog with a dimmed background backdrop.
 *
 * @param visible Whether the modal dialog is currently active and visible.
 * @param onDismiss Callback invoked when clicking on the outside scrim backdrop.
 * @param title Modal title header.
 * @param description Optional secondary description.
 * @param modifier Chainable [UIModifier].
 * @param content Declarative body and actions inside the dialog.
 */
@Composable
fun ModalDialog(
    visible: Boolean,
    onDismiss: () -> Unit,
    title: String,
    description: String? = null,
    modifier: UIModifier = UIModifier,
    content: @Composable () -> Unit
) {
    if (!visible) return

    val colors = Theme.colors
    val shapes = Theme.shapes

    // 1. Dimmed background scrim capturing outside clicks
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.scrim)
            .clickable { onDismiss() }
    ) {
        // 2. Centered Frosted Glass Popover Card
        Card(
            variant = CardVariant.GLASS,
            radius = shapes.xLarge,
            modifier = Modifier
                .width(360f)
                .align(Alignment.Center)
                .clickable { /* Absorb clicks inside dialog */ }
                .then(modifier)
        ) {
            Column(
                arrangement = Arrangement.spacedBy(12f),
                alignment = Alignment.TopStart,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = title,
                    color = colors.textPrimary
                )

                if (description != null) {
                    Text(
                        text = description,
                        color = colors.textSecondary
                    )
                }

                content()
            }
        }
    }
}
