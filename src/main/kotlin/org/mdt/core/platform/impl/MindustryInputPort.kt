package org.mdt.core.platform.impl

import arc.Core
import arc.input.InputMultiplexer
import arc.input.InputProcessor
import arc.util.Log
import org.mdt.core.platform.port.InputPort

/**
 * ## MindustryInputPort [Mindustry / Arc Input Implementation]
 *
 * > 🤖 **AGENT DIRECTIVE:** When modifying this class or related logic, synchronously update this KDoc:
 * > 1) Keep Purpose accurate. 2) Update Key Rules & Checklist [x]/[ ]. 3) Maintain Related Files map.
 *
 * ### 1. Purpose
 * - Implements [InputPort] by bridging to Arc's `Core.input` and injecting a local [InputMultiplexer].
 *
 * ### 2. Key Rules & Checklist
 * - [x] Injects [localMultiplexer] at index 0 of `Core.input.inputProcessors`.
 * - [x] Returns safe fallback values if `Core.input` is uninitialized.
 * - [x] Keep initialization flat and explicit without excessive inline scope functions.
 *
 * ### 3. Related Files
 * - Input Port: `src/main/kotlin/org/mdt/core/platform/port/InputPort.kt`
 * - Platform Host: `src/main/kotlin/org/mdt/core/platform/PlatformHost.kt`
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
