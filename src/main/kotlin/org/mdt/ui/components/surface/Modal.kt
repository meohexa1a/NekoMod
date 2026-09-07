// [AGENT INVARIANT] Synchronously update @property, @param, and @see KDocs when modifying this file.

@file:Suppress("FunctionName", "unused")

package org.mdt.ui.components.surface

import androidx.compose.runtime.Composable
import org.mdt.core.ui.layout.BoxScope
import org.mdt.core.ui.modifier.UIModifier
import org.mdt.core.ui.modifier.align
import org.mdt.core.ui.modifier.anchor
import org.mdt.core.ui.modifier.background
import org.mdt.core.ui.modifier.consumePointer
import org.mdt.core.ui.modifier.glass
import org.mdt.core.ui.modifier.onClick
import org.mdt.core.ui.modifier.opaque
import org.mdt.core.ui.unit.Alignment
import org.mdt.core.ui.unit.Color
import org.mdt.core.ui.unit.LayoutPreset
import org.mdt.ui.components.layout.Box
import org.mdt.ui.theme.ThemeTokens

/**
 * ## ModalDialog
 *
 * Full-screen modal overlay with a semi-transparent frosted backdrop scrim,
 * click-outside dismiss handling, and an elevated dialog [Surface].
 *
 * @param visible Whether the dialog is visible and mounted.
 * @param onDismiss Callback invoked when clicking on the backdrop scrim.
 * @param modifier Chainable [UIModifier] applied to the dialog card.
 * @param scrimColor Scrim fill color behind the dialog.
 * @param content Slot receiving [BoxScope] for dialog contents.
 *
 * @see Surface
 * @see Card
 */
@Composable
fun ModalDialog(
    visible: Boolean,
    onDismiss: () -> Unit,
    modifier: UIModifier = UIModifier,
    scrimColor: Color = Color(0.0f, 0.0f, 0.0f, 0.55f),
    content: @Composable BoxScope.() -> Unit
) {
    if (!visible) return

    // Full-screen backdrop scrim
    Box(
        modifier = UIModifier
            .anchor(LayoutPreset.FULL_RECT)
            .background(scrimColor)
            .glass(true)
            .opaque()
            .onClick { onDismiss() }
    ) {
        // Centered modal dialog surface (intercepts clicks to prevent dismissing)
        Surface(
            modifier = modifier
                .align(Alignment.Center)
                .consumePointer(),
            color = ThemeTokens.surfaceModal,
            radius = 16.0f,
            borderWidth = 1.0f,
            borderColor = ThemeTokens.border,
            isGlass = true,
            content = content
        )
    }
}
