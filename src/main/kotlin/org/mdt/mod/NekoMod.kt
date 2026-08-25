package org.mdt.mod

import arc.Core
import arc.Events
import arc.scene.event.Touchable
import mindustry.Vars
import mindustry.game.EventType.ClientLoadEvent
import mindustry.game.EventType.Trigger
import mindustry.mod.Mod
import org.mdt.ui.EngineRuntime
import org.mdt.ui.screens.MainMenuScreen

/**
 * ## NekoMod
 *
 * Mindustry mod entry point bootstrapping the Pure KMP UI Engine and modern Main Menu.
 *
 * See: docs/architecture/architecture_en.md
 */
class NekoMod : Mod() {

    override fun init() {
        Events.on(ClientLoadEvent::class.java) {
            Core.app.post { setup() }
        }

        // Completely turn off Mindustry's legacy Arc menuGroup
        Events.run(Trigger.update) {
            if (Vars.state == null || Vars.state.isMenu) {
                disableLegacyMenuGroup()
            }
        }
    }

    private fun setup() {
        disableLegacyMenuGroup()

        // Launch NekoMod Declarative UI Engine with MainMenuScreen
        EngineRuntime.setContent {
            MainMenuScreen()
        }
    }

    private fun disableLegacyMenuGroup() {
        val menuGroup = Vars.ui?.menuGroup ?: return
        menuGroup.visible = false
        menuGroup.touchable = Touchable.disabled
    }
}
