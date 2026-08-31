// [AGENT INVARIANT] Synchronously update @property, @param, and @see KDocs when modifying this file.

@file:Suppress("FunctionName", "unused")

package org.mdt.ui.components.surface

import androidx.compose.runtime.Composable
import org.mdt.core.platform.unit.Color
import org.mdt.core.ui.layout.Alignment
import org.mdt.core.ui.layout.LayoutPreset
import org.mdt.core.ui.modifier.UIModifier
import org.mdt.core.ui.modifier.align
import org.mdt.core.ui.modifier.anchor
import org.mdt.core.ui.modifier.background
import org.mdt.core.ui.modifier.glass
import org.mdt.core.ui.modifier.onClick
import org.mdt.core.ui.modifier.opaque
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
