package org.hubdustry

import arc.Core
import arc.Events
import mindustry.game.EventType.ClientLoadEvent
import mindustry.mod.Mod
import org.hubdustry.sample.SampleComposeDialog

class NekoMod : Mod() {

    override fun init() {
        Events.on(ClientLoadEvent::class.java) { Core.app.post(this::loadMod) }
    }

    fun loadMod() {
        SampleComposeDialog().show()
    }
}
