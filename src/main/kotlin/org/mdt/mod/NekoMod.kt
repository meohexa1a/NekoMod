// [AGENT INVARIANT] Synchronously update @property, @param, and @see KDocs when modifying this file.

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
 * Main Mindustry mod entry point that bootstraps the UI engine and mounts [NekoApp].
 *
 * @see org.mdt.core.ui.EngineRuntime
 * @see NekoApp
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
