package org.mdt.core.ui.compose

import androidx.compose.runtime.BroadcastFrameClock
import androidx.compose.runtime.Recomposer
import androidx.compose.runtime.snapshots.Snapshot
import arc.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * ## ComposePipeline [Compose Runtime Host]
 *
 * > 🤖 **AGENT DIRECTIVE:** When modifying this class or related logic, synchronously update this KDoc:
 * > 1) Keep Purpose accurate. 2) Update Key Rules & Checklist [x]/[ ]. 3) Maintain Related Files map.
 *
 * ### 1. Purpose
 * - Manages the Compose runtime lifecycle, including [Recomposer], [BroadcastFrameClock], and [CoroutineScope].
 * - Dispatches frame ticks to trigger UI recomposition during the engine render loop.
 *
 * ### 2. Key Rules & Checklist
 * - [x] Each pipeline instance owns an isolated coroutine scope that cancels cleanly on `dispose()`.
 * - [x] `frame()` dispatches pending snapshot state changes and sends frame times to the clock.
 * - [x] Snapshot write observers are registered safely only once per process.
 *
 * ### 3. Related Files
 * - Runtime Orchestrator: `src/main/kotlin/org/mdt/core/ui/EngineRuntime.kt`
 * - UI Composition: `src/main/kotlin/org/mdt/core/ui/compose/UIComposition.kt`
 */
class ComposePipeline {
    val clock = BroadcastFrameClock()
    private val scope = CoroutineScope(Dispatchers.Unconfined + SupervisorJob() + clock)
    val recomposer = Recomposer(scope.coroutineContext)

    init {
        ensureSnapshotObserver()
        scope.launch(start = CoroutineStart.UNDISPATCHED) { recomposer.runRecomposeAndApplyChanges() }
        Log.info("[NekoMod] ComposePipeline initialized with fresh BroadcastFrameClock.")
    }

    fun frame() {
        Snapshot.sendApplyNotifications()
        clock.sendFrame(System.nanoTime())
    }

    fun dispose() {
        recomposer.cancel()
        scope.cancel()
    }

    companion object {
        private var observerRegistered = false

        private fun ensureSnapshotObserver() {
            if (observerRegistered) return
            observerRegistered = true
            Snapshot.registerGlobalWriteObserver { Snapshot.sendApplyNotifications() }
        }
    }
}
