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
import org.mdt.core.i18n.i18n
import org.mdt.core.store.KVStore
import org.mdt.ui.EngineRuntime
import org.mdt.ui.components.display.image.*
import org.mdt.ui.components.display.progress.*
import org.mdt.ui.components.display.tooltip.*
import org.mdt.ui.components.input.slider.*
import org.mdt.ui.components.input.textfield.*
import org.mdt.ui.components.layout.*
import org.mdt.ui.components.surface.*
import org.mdt.ui.components.text.*
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
            // Restore persistent state from KVStore
            var counter by remember { mutableStateOf(KVStore.default.getInt("demo_counter", 0)) }
            var toggled by remember { mutableStateOf(KVStore.default.getBoolean("demo_toggled", true)) }
            var inputText by remember { mutableStateOf(KVStore.default.getString("demo_input", "NekoMod Pure KMP Engine")) }
            var sliderVal by remember { mutableStateOf(KVStore.default.getFloat("demo_slider", 0.65f)) }

            Card(
                modifier = Modifier.anchor(LayoutPreset.CENTER),
                padding = 18f
            ) {
                Column(gap = 10f) {
                    Row(arrangement = Arrangement.spacedBy(10f)) {
                        Image(
                            source = "https://raw.githubusercontent.com/Anuken/Mindustry/master/core/assets-raw/sprites/blocks/distribution/router.png",
                            modifier = Modifier.size(32f, 32f).tooltip("Mindustry Router Block")
                        )
                        Column(gap = 2f) {
                            Text(
                                text = i18n("app.title"),
                                color = Color.white,
                                scale = 1.1f
                            )
                            Text(
                                text = i18n("app.subtitle"),
                                color = Color.valueOf("9399b2"),
                                scale = 0.8f
                            )
                        }
                    }

                    Divider(modifier = Modifier.margin(vertical = 2f))

                    TextField(
                        value = inputText,
                        onValueChange = {
                            inputText = it
                            KVStore.default.putString("demo_input", it)
                        },
                        placeholder = "Type message or command...",
                        modifier = Modifier.fillMaxWidth().height(32f).tooltip("Interactive Text Input")
                    )

                    Row(arrangement = Arrangement.spacedBy(10f)) {
                        Text(
                            text = "Power: ${(sliderVal * 100f).toInt()}%",
                            color = Color.valueOf("cad3f5"),
                            scale = 0.85f
                        )
                        Slider(
                            value = sliderVal,
                            onValueChange = {
                                sliderVal = it
                                KVStore.default.putFloat("demo_slider", it)
                            },
                            modifier = Modifier.width(130f).tooltip("Drag to adjust power output")
                        )
                    }

                    ProgressBar(
                        progress = sliderVal,
                        barHeight = 4f,
                        modifier = Modifier.fillMaxWidth(),
                        colors = if (sliderVal > 0.8f) ProgressColors.Danger else ProgressColors.Primary
                    )

                    Divider(modifier = Modifier.margin(vertical = 2f))

                    Row(
                        arrangement = Arrangement.spacedBy(10f)
                    ) {
                        Button(
                            text = i18n("btn.count", "count" to counter),
                            colors = ButtonColors.Primary,
                            modifier = Modifier.tooltip("Increment counter"),
                            onClick = {
                                counter++
                                KVStore.default.putInt("demo_counter", counter)
                            }
                        )
                        Button(
                            text = i18n("btn.reset"),
                            colors = ButtonColors.Danger,
                            modifier = Modifier.tooltip("Reset counter to zero"),
                            onClick = {
                                counter = 0
                                KVStore.default.putInt("demo_counter", 0)
                            }
                        )
                        Toggle(
                            checked = toggled,
                            modifier = Modifier.tooltip("Toggle feature switch"),
                            onToggle = {
                                toggled = !toggled
                                KVStore.default.putBoolean("demo_toggled", toggled)
                            }
                        )
                    }
                }
            }
        }
    }
}
