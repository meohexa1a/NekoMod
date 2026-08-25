package org.mdt.core.async

import arc.Core
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

/**
 * ## AsyncDispatcher
 *
 * Manages background asynchronous coroutine scopes and safe dispatching back onto
 * the Mindustry OpenGL Render/Main Thread.
 */
object AsyncDispatcher {

    /** Supervisor job for all background subsystem tasks. */
    private val supervisorJob = SupervisorJob()

    /** Background I/O and computation coroutine scope. */
    val scope = CoroutineScope(Dispatchers.IO + supervisorJob)

    /**
     * Custom CoroutineDispatcher that posts execution blocks onto Mindustry's Main/Render Thread.
     */
    val Main: CoroutineDispatcher = object : CoroutineDispatcher() {
        override fun dispatch(context: CoroutineContext, block: Runnable) {
            if (Core.app != null) {
                Core.app.post(block)
            } else {
                block.run()
            }
        }
    }

    /**
     * Launches a background asynchronous coroutine task on [Dispatchers.IO].
     */
    fun launch(block: suspend CoroutineScope.() -> Unit): Job {
        return scope.launch(block = block)
    }

    /**
     * Posts a callback block onto the Mindustry Main/Render thread.
     */
    fun onMainThread(block: () -> Unit) {
        if (Core.app != null) {
            Core.app.post(block)
        } else {
            block()
        }
    }

    /**
     * Disposes and cancels all active background coroutine jobs.
     */
    fun dispose() {
        supervisorJob.cancelChildren()
    }
}
