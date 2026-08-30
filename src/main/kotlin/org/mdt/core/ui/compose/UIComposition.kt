package org.mdt.core.ui.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Composition
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Recomposer
import org.mdt.core.platform.LocalPlatformHost
import org.mdt.core.ui.EngineRuntime
import org.mdt.core.ui.node.UINode

// --- UI COMPOSITION ROOT HOST ---

/**
 * ## UIComposition [Compose Tree Mount Host]
 *
 * > 🤖 **AGENT DIRECTIVE:** When modifying this class or related logic, synchronously update this KDoc:
 * > 1) Keep Purpose accurate. 2) Update Key Rules & Checklist [x]/[ ]. 3) Maintain Related Files map.
 *
 * ### 1. Purpose
 * - Mounts a declarative Compose UI hierarchy onto a root [UINode] tree via [NodeApplier].
 * - Injects default environment providers like [LocalPlatformHost].
 *
 * ### 2. Key Rules & Checklist
 * - [x] Automatically injects [LocalPlatformHost] at the root of the composition.
 * - [x] Calling `dispose()` completely tears down the underlying [Composition] slot table.
 *
 * ### 3. Related Files
 * - Node Applier: `src/main/kotlin/org/mdt/core/ui/compose/NodeApplier.kt`
 * - Root Screen Node: `src/main/kotlin/org/mdt/core/ui/node/CanvasNode.kt`
 * - Platform Host: `src/main/kotlin/org/mdt/core/platform/PlatformHost.kt`
 */
class UIComposition(
    root: UINode,
    parentRecomposer: Recomposer,
    content: @Composable () -> Unit
) {
    private val composition: Composition = Composition(
        applier = NodeApplier(root),
        parent = parentRecomposer
    ).apply {
        setContent {
            CompositionLocalProvider(
                LocalPlatformHost provides EngineRuntime.host
            ) {
                content()
            }
        }
    }

    fun dispose() = composition.dispose()
}


