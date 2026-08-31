// [AGENT INVARIANT] Synchronously update @property, @param, and @see KDocs when modifying this file.

package org.mdt.core.ui.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Composition
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Recomposer
import org.mdt.core.platform.LocalPlatformHost
import org.mdt.core.ui.node.UINode

// --- UI COMPOSITION ROOT HOST ---

/**
 * ## UIComposition
 *
 * Mounts a declarative Compose UI hierarchy onto a root [UINode] tree via [NodeApplier].
 * Injects default environment providers like [LocalPlatformHost].
 *
 * @param root Target root virtual DOM node.
 * @param parentRecomposer Parent recomposer orchestrating recomposition jobs.
 * @param content Declarative UI composable lambda.
 *
 * @see NodeApplier
 * @see LocalPlatformHost
 * @see org.mdt.core.ui.EngineRuntime
 */
class UIComposition(
    root: UINode,
    parentRecomposer: Recomposer,
) {
    private val composition: Composition = Composition(
        applier = NodeApplier(root),
        parent = parentRecomposer,
    )

    fun setContent(content: @Composable () -> Unit) {
        composition.setContent {
            CompositionLocalProvider(
                LocalPlatformHost provides org.mdt.core.ui.EngineRuntime.host,
            ) {
                content()
            }
        }
    }

    fun dispose() = composition.dispose()
}


