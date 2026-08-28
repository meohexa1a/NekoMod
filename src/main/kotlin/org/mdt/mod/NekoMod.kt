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
 * ## NekoMod
 *
 * Mindustry mod entry point bootstrapping the 1-Draw-Call Declarative UI Engine.
 *
 * See: docs/architecture/architecture_en.md
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
