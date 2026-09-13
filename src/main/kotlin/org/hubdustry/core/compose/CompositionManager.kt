package org.hubdustry.core.compose

import androidx.compose.runtime.BroadcastFrameClock
import androidx.compose.runtime.Recomposer
import androidx.compose.runtime.snapshots.Snapshot
import arc.util.Log
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Quản lý Singleton Recomposer và FrameClock toàn cục cho NekoMod.
 *
 * BẢO TỒN NGUYÊN TẮC V1/V2:
 * 1. Singleton sống vĩnh viễn, điều phối tất cả các ComposeView/Dialog.
 * 2. Sử dụng [Dispatchers.Unconfined] + [BroadcastFrameClock] để đồng bộ trực tiếp trên luồng Main GL.
 * 3. Bơm [Snapshot.sendApplyNotifications] và [clock.sendFrame] ở mỗi frame.
 * 4. Bọc phòng vệ [CoroutineExceptionHandler] chống crash ngầm.
 */
object CompositionManager {
    var clock = BroadcastFrameClock()
        private set

    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        Log.err("[CompositionManager] Uncaught exception in Recomposer", throwable)
    }

    private var _scope: CoroutineScope? = null
    val scope: CoroutineScope
        get() = _scope ?: CoroutineScope(Dispatchers.Unconfined + SupervisorJob() + clock + exceptionHandler).also { _scope = it }

    private var _recomposer: Recomposer? = null
    val recomposer: Recomposer
        get() = _recomposer ?: Recomposer(scope.coroutineContext).also { _recomposer = it }

    private var started = false
    private var writeObserverHandle: androidx.compose.runtime.snapshots.ObserverHandle? = null

    fun start() {
        if (started) return
        started = true

        val currentScope = _scope
        if (currentScope == null || currentScope.coroutineContext[kotlinx.coroutines.Job]?.isActive != true) {
            clock = BroadcastFrameClock()
            val newScope = CoroutineScope(Dispatchers.Unconfined + SupervisorJob() + clock + exceptionHandler)
            _scope = newScope
            _recomposer = Recomposer(newScope.coroutineContext)
        }

        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            try {
                recomposer.runRecomposeAndApplyChanges()
            } catch (t: Throwable) {
                Log.err("[CompositionManager] Recomposer runner exited with error", t)
            }
        }

        writeObserverHandle = Snapshot.registerGlobalWriteObserver {
            Snapshot.sendApplyNotifications()
        }

        Log.info("[NekoMod] CompositionManager started successfully.")
    }

    /**
     * Đập nhịp khung hình đồng bộ từ game loop của Arc.
     */
    fun frame() {
        if (!started) return
        Snapshot.sendApplyNotifications()
        clock.sendFrame(System.nanoTime())
    }

    fun stop() {
        if (!started) return
        writeObserverHandle?.dispose()
        writeObserverHandle = null
        _recomposer?.cancel()
        _recomposer?.close()
        _scope?.cancel()
        _recomposer = null
        _scope = null
        started = false
    }
}
