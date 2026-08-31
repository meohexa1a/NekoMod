// [AGENT INVARIANT] Synchronously update @property, @param, and @see KDocs when modifying this file.

package org.mdt.core.platform.input

import arc.Core
import arc.input.InputMultiplexer
import arc.input.InputProcessor
import arc.util.Log

/**
 * ## MindustryInputPort
 *
 * Implements [InputPort] by bridging to Arc's `Core.input` and injecting a local [InputMultiplexer].
 *
 * @see InputPort
 * @see org.mdt.core.platform.PlatformHost
 */
class MindustryInputPort : InputPort {

    override val mouseX: Float
        get() = Core.input?.mouseX()?.toFloat() ?: 0.0f

    override val mouseY: Float
        get() = Core.input?.mouseY()?.toFloat() ?: 0.0f

    override val isCtrlPressed: Boolean
        get() = Core.input?.ctrl() ?: false

    override val isShiftPressed: Boolean
        get() = Core.input?.shift() ?: false

    override val isAltPressed: Boolean
        get() = Core.input?.alt() ?: false

    override fun addInputProcessor(processor: InputProcessor) {
        val processors = Core.input?.inputProcessors
        if (processors != null && !processors.contains(localMultiplexer)) {
            processors.insert(0, localMultiplexer)
        }

        if (!localMultiplexer.processors.contains(processor)) {
            localMultiplexer.addProcessor(0, processor)
        }
    }

    override fun removeInputProcessor(processor: InputProcessor) {
        localMultiplexer.removeProcessor(processor)
    }

    companion object {
        private val localMultiplexer by lazy {
            val inputMultiplexer = InputMultiplexer()
            val inputProcessors = Core.input?.inputProcessors

            when {
                inputProcessors != null && !inputProcessors.contains(inputMultiplexer) -> inputProcessors.insert(0, inputMultiplexer)
                inputProcessors == null -> Log.warn("[NekoMod] Core.input is uninitialized when creating InputMultiplexer")
            }

            inputMultiplexer
        }
    }
}
