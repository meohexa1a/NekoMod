package org.mdt.mod

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import arc.Core
import arc.Events
import arc.graphics.Color
import mindustry.game.EventType.ClientLoadEvent
import mindustry.mod.Mod
import org.mdt.ui.EngineRuntime
import org.mdt.ui.compose.*
import org.mdt.ui.layout.Arrangement
import org.mdt.ui.layout.LayoutPreset

class NekoMod : Mod() {

    override fun init() {
        Events.on(ClientLoadEvent::class.java) {
            Core.app.post { setup() }
        }
    }

    private fun setup() {
        EngineRuntime.setContent {
            var counter by remember { mutableStateOf(0) }
            var toggled by remember { mutableStateOf(true) }

            Card(
                modifier = Modifier.anchor(LayoutPreset.CENTER),
                backgroundColor = Color.valueOf("1e1e2e"),
                borderColor = Color.valueOf("89b4fa"),
                borderWidth = 1.8,
                radius = 12.0,
                padding = 20f
            ) {
                Column(gap = 12f) {
                    Text(
                        text = "Pure KMP UI Engine",
                        color = Color.valueOf("cdd6f4"),
                        scale = 1.3
                    )
                    Text(
                        text = "Standardized Scoped DSL & Godot Layout",
                        color = Color.valueOf("a6adc8"),
                        scale = 0.95
                    )
                    Divider(modifier = Modifier.margin(vertical = 4f))
                    Row(
                        arrangement = Arrangement.spacedBy(10f)
                    ) {
                        Button(
                            text = "Count: $counter",
                            colors = ButtonColors.Primary,
                            onClick = { counter++ }
                        )
                        Button(
                            text = "Reset",
                            colors = ButtonColors.Danger,
                            onClick = { counter = 0 }
                        )
                        Toggle(
                            checked = toggled,
                            onToggle = { toggled = !toggled }
                        )
                    }
                }
            }
        }
    }
}
