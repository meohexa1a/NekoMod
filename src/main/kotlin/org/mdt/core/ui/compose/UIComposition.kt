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
import org.mdt.core.ui.node.UINode

// --- COMPOSITION MANAGER & FRAME CLOCK ---

/**
 * ## CompositionManager [Compose Runtime & Frame Clock Coordinator]
 *
 * ### 1. 📖 Feature Specification & Core Architecture:
 * - Orchestrates the Jetpack Compose Runtime [Recomposer] and [BroadcastFrameClock].
 * - Registers global [Snapshot] write observers to dispatch state mutation apply notifications immediately.
 * - Drives frame ticks on-demand via `frame()` during `EngineRuntime.draw()`.
 *
 * ### 2. ⚡ Invariants & Non-Negotiable Rules:
 * - **Rule 1 (Thread Safety & Coroutines):** Recomposer runs under `Dispatchers.Unconfined + SupervisorJob() + clock`.
 * - **Rule 2 (Idempotent Lifecycle):** `start()` and `stop()` can be called safely without leaking coroutines.
 *
 * ### 3. 🔗 Related Files & Subsystem Map:
 * - ⚙️ **Runtime Host:** `src/main/kotlin/org/mdt/core/ui/EngineRuntime.kt`
 * - 🔄 **Tree Composition:** `src/main/kotlin/org/mdt/core/ui/compose/UIComposition.kt`
 *
 * ### 4. ✅ Behavioral Verification Checklist:
 * - [x] `start()` launches undispatched `recomposer.runRecomposeAndApplyChanges()`.
 * - [x] `frame()` sends apply notifications and nano-timestamp to `clock`.
 * - [x] `stop()` cancels coroutine scope and recomposer.
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
 * ## UIComposition [Compose Tree Mount Host]
 *
 * ### 1. 📖 Feature Specification & Core Architecture:
 * - Mounts a declarative Compose UI tree onto a root [UINode] via [NodeApplier].
 * - Injects ambient [LocalPlatformHost] CompositionLocal provider.
 *
 * ### 2. ⚡ Invariants & Non-Negotiable Rules:
 * - **Rule 1 (Clean Disposal):** Calling `dispose()` frees the Compose slot table and applier tree.
 *
 * ### 3. 🔗 Related Files & Subsystem Map:
 * - 🌲 **Node Applier:** `src/main/kotlin/org/mdt/core/ui/compose/NodeApplier.kt`
 * - 🌲 **Root Virtual Node:** `src/main/kotlin/org/mdt/core/ui/node/CanvasNode.kt`
 * - 🔌 **Platform Host:** `src/main/kotlin/org/mdt/core/engine/PlatformHost.kt`
 *
 * ### 4. ✅ Behavioral Verification Checklist:
 * - [x] Automatically provides [LocalPlatformHost].
 * - [x] `dispose()` cleanly tears down underlying [Composition].
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
                CompositionLocalProvider(
                    LocalPlatformHost provides EngineRuntime.host
                ) {
                    content()
                }
            }
        }
    }

    fun dispose() = composition.dispose()
}
