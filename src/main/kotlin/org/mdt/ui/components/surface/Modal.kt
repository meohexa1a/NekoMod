@file:Suppress("FunctionName", "unused")

package org.mdt.ui.components.surface

import androidx.compose.runtime.Composable
import org.mdt.core.ui.compose.*
import org.mdt.core.ui.layout.Alignment
import org.mdt.core.ui.layout.LayoutPreset
import org.mdt.core.ui.unit.Color
import org.mdt.ui.components.layout.Box

/**
 * ## ModalDialog [Declarative Full-Screen Modal Dialog]
 *
 * ### 1. 📖 Feature Specification & Core Architecture:
 * - Declarative full-screen overlay dialog with dimming scrim, frosted glass backdrop, and centered content card.
 * - Self-manages click-outside dismiss gesture without polluting core layout engine.
 * - Completely intercepts pointer events (opaque) to prevent click-through to underlying UI or gameplay.
 *
 * ### 2. ⚡ Invariants & Non-Negotiable Rules:
 * - **Rule 1 (Declarative Full-Screen Anchor):** Uses LayoutPreset.FULL_RECT to span the entire viewport.
 * - **Rule 2 (Opaque Scrim):** Consumes pointer down/up events over background scrim.
 *
 * ### 3. 🔗 Related Files & Subsystem Map:
 * - 🎨 **Composable Card:** src/main/kotlin/org/mdt/ui/components/surface/Card.kt
 * - 🎨 **Composable Box:** src/main/kotlin/org/mdt/ui/components/layout/Box.kt
 *
 * ### 4. ✅ Behavioral Verification Checklist:
 * - [x] When isible = false, renders nothing (Zero overhead).
 * - [x] Clicking the backdrop invokes onDismiss callback.
 * - [x] Clicking inside the modal container card is absorbed without triggering dismiss.
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
