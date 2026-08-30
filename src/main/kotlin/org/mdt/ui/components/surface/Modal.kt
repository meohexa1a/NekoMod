@file:Suppress("FunctionName", "unused")

package org.mdt.ui.components.surface

import androidx.compose.runtime.Composable
import org.mdt.core.ui.compose.UIModifier
import org.mdt.core.ui.compose.align
import org.mdt.core.ui.compose.anchor
import org.mdt.core.ui.compose.background
import org.mdt.core.ui.compose.glass
import org.mdt.core.ui.compose.onClick
import org.mdt.core.ui.compose.opaque
import org.mdt.core.ui.layout.Alignment
import org.mdt.core.ui.layout.LayoutPreset
import org.mdt.core.ui.unit.Color
import org.mdt.ui.components.layout.Box

/**
 * ## ModalDialog
 *
 * Full-screen modal overlay with a semi-transparent frosted backdrop scrim and click-outside dismiss handling.
 */
@Composable
fun ModalDialog(
    visible: Boolean,
    onDismiss: () -> Unit,
    modifier: UIModifier = UIModifier,
    scrimColor: Color = Color(0.0f, 0.0f, 0.0f, 0.55f),
    content: @Composable () -> Unit
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
        // Centered modal dialog card (intercepts clicks to prevent dismissing)
        Card(
            modifier = UIModifier
                .align(Alignment.Center)
                .onClick { /* absorb click inside modal card */ }
                .then(modifier),
            content = content
        )
    }
}
