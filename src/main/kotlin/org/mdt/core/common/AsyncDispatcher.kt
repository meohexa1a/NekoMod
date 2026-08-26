package org.mdt.core.common

import kotlinx.coroutines.*
import org.mdt.core.engine.EngineContext
import kotlin.coroutines.CoroutineContext

/**
 * ## AsyncDispatcher
 *
 * Centralized coroutine dispatcher and background execution manager.
 * Safely bridges asynchronous background tasks back onto the host platform's
 * Main / Render Thread via [org.mdt.core.engine.PlatformHost].
 *
 * See: docs/core-subsystems/core_subsystems_en.md
 */
object AsyncDispatcher {

    /** Supervisor job managing all background subsystem tasks without cascading failures. */
    private val supervisorJob = SupervisorJob()

    /** Background I/O and computation coroutine scope bound to [Dispatchers.IO]. */
    val scope = CoroutineScope(Dispatchers.IO + supervisorJob)

    // =========================================================================
    // I. Coroutine Dispatching & Execution
    // =========================================================================

    /**
     * Launches a background asynchronous coroutine task on [Dispatchers.IO].
     *
     * @param block Asynchronous coroutine body.
     * @return Active coroutine [Job].
     */
    fun launch(block: suspend CoroutineScope.() -> Unit): Job = scope.launch(block = block)

    /**
     * Posts a callback block onto the host platform's Main/Render thread.
     * Inlined to eliminate heap lambda allocations (Zero-GC invariant).
     *
     * @param block Action to execute on the main thread.
     */
    inline fun onMainThread(crossinline block: () -> Unit) {
        EngineContext.default.host.postToMainThread { block() }
    }

    /**
     * Custom [CoroutineDispatcher] that posts execution blocks onto the host platform's Main/Render Thread.
     */
    val Main: CoroutineDispatcher = object : CoroutineDispatcher() {
        override fun dispatch(context: CoroutineContext, block: Runnable) = onMainThread(block::run)
    }

    // =========================================================================
    // II. Lifecycle Cleanup
    // =========================================================================

    /**
     * Cancels all active background coroutine jobs without terminating the dispatcher scope.
     */
    fun dispose() = supervisorJob.cancelChildren()
}
