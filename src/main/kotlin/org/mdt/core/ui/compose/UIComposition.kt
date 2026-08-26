package org.mdt.core.ui.compose

import androidx.compose.runtime.BroadcastFrameClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Composition
import androidx.compose.runtime.Recomposer
import androidx.compose.runtime.snapshots.Snapshot
import arc.util.Log
import kotlinx.coroutines.*
import org.mdt.core.ui.UINode

object CompositionManager {
    val clock = BroadcastFrameClock()
    private val scope = CoroutineScope(Dispatchers.Unconfined + SupervisorJob() + clock)
    val recomposer = Recomposer(scope.coroutineContext)

    private var started = false

    fun start() {
        if (started) return
        started = true

        // Register Global Write Observer so mutations to mutableStateOf trigger immediate apply notifications
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
            setContent { content() }
        }
    }

    fun dispose() = composition.dispose()
}
