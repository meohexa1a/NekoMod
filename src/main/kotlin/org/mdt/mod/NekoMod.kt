package org.mdt.mod

import arc.Core
import arc.Events
import arc.scene.event.Touchable
import mindustry.Vars
import mindustry.game.EventType.ClientLoadEvent
import mindustry.game.EventType.Trigger
import mindustry.mod.Mod
import org.mdt.core.ui.EngineRuntime
import org.mdt.ui.NekoApp

/**
 * ## NekoMod [Mod Entry Point]
 *
 * > 🤖 **AGENT DIRECTIVE:** When modifying this class or related logic, synchronously update this KDoc:
 * > 1) Keep Purpose accurate. 2) Update Key Rules & Checklist [x]/[ ]. 3) Maintain Related Files map.
 *
 * ### 1. Purpose
 * - Main Mindustry mod entry point that bootstraps the UI engine and mounts [NekoApp].
 *
 * ### 2. Key Rules & Checklist
 * - [x] Listens for `ClientLoadEvent` to set up UI on the main thread.
 * - [x] Mounts root app composable via `EngineRuntime.setContent`.
 *
 * ### 3. Related Files
 * - UI Runtime: `src/main/kotlin/org/mdt/core/ui/EngineRuntime.kt`
 * - Root UI App: `src/main/kotlin/org/mdt/ui/NekoApp.kt`
 */
class NekoMod : Mod() {

    override fun init() {
        Events.on(ClientLoadEvent::class.java) {
            Core.app.post { setup() }
        }
    }

    private fun setup() {
        EngineRuntime.setContent { NekoApp() }
    }
}
