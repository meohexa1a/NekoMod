// [AGENT INVARIANT] Synchronously update @property, @param, and @see KDocs when modifying this file.

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
 * ## ComposePipeline
 *
 * Manages the Compose runtime lifecycle, including [Recomposer], [BroadcastFrameClock], and [CoroutineScope].
 * Dispatches frame ticks to trigger UI recomposition during the engine render loop.
 *
 * @property clock Broadcast frame clock driving compose recomposition passes.
 * @property recomposer Compose recomposer instance executing recomposition jobs.
 *
 * @see org.mdt.core.ui.EngineRuntime
 * @see UIComposition
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
