package org.mdt.core.ui.compose

import androidx.compose.runtime.BroadcastFrameClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Composition
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Recomposer
import androidx.compose.runtime.snapshots.Snapshot
import arc.util.Log
import kotlinx.coroutines.*
import org.mdt.core.engine.LocalPlatformHost
import org.mdt.core.ui.EngineRuntime
import org.mdt.core.ui.UINode
import org.mdt.core.ui.overlay.LocalOverlayHost
import org.mdt.core.ui.overlay.OverlayHost

// --- COMPOSITION MANAGER & FRAME CLOCK ---

/**
 * ## CompositionManager
 *
 * Master Compose Runtime orchestrator managing the recomposition frame clock and coroutine scopes.
 *
 * See: docs/compose-dsl/compose_dsl_en.md
 */
object CompositionManager {
    val clock = BroadcastFrameClock()
    private val scope = CoroutineScope(Dispatchers.Unconfined + SupervisorJob() + clock)
    val recomposer = Recomposer(scope.coroutineContext)

    private var started = false

    fun start() {
        if (started) return

        started = true

        Snapshot.registerGlobalWriteObserver {
            Snapshot.sendApplyNotifications()
        }

        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            recomposer.runRecomposeAndApplyChanges()
        }

        Log.info("[NekoMod] CompositionManager started with BroadcastFrameClock.")
    }

    fun frame() {
        if (!started) return

        Snapshot.sendApplyNotifications()
        clock.sendFrame(System.nanoTime())
    }

    fun stop() {
        recomposer.cancel()
        scope.cancel()
        started = false
    }
}

// --- UI COMPOSITION ROOT HOST ---

/**
 * ## UIComposition
 *
 * Mounts a declarative Compose UI tree onto a root [UINode].
 *
 * See: docs/compose-dsl/compose_dsl_en.md
 */
class UIComposition(
    root: UINode,
    content: @Composable () -> Unit
) {
    private val composition: Composition

    init {
        CompositionManager.start()
        composition = Composition(
            applier = NodeApplier(root),
            parent = CompositionManager.recomposer
        ).apply {
            setContent {
                val overlayHost = root as? OverlayHost
                if (overlayHost != null) {
                    CompositionLocalProvider(
                        LocalPlatformHost provides EngineRuntime.host,
                        LocalOverlayHost provides overlayHost
                    ) {
                        content()
                    }
                } else {
                    CompositionLocalProvider(
                        LocalPlatformHost provides EngineRuntime.host
                    ) {
                        content()
                    }
                }
            }
        }
    }

    fun dispose() = composition.dispose()
}
