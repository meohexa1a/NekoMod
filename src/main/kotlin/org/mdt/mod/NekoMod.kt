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

            Card(
                modifier = Modifier.anchor(LayoutPreset.CENTER),
                padding = 18f
            ) {
                Column(gap = 10f) {
                    Row(arrangement = Arrangement.spacedBy(10f)) {
                        Image(
                            source = "https://raw.githubusercontent.com/Anuken/Mindustry/master/icon.png",
                            modifier = Modifier.size(32f, 32f)
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
                        modifier = Modifier.fillMaxWidth().height(32f)
                    )

                    Divider(modifier = Modifier.margin(vertical = 2f))

                    Row(
                        arrangement = Arrangement.spacedBy(10f)
                    ) {
                        Button(
                            text = i18n("btn.count", "count" to counter),
                            colors = ButtonColors.Primary,
                            onClick = {
                                counter++
                                KVStore.default.putInt("demo_counter", counter)
                            }
                        )
                        Button(
                            text = i18n("btn.reset"),
                            colors = ButtonColors.Danger,
                            onClick = {
                                counter = 0
                                KVStore.default.putInt("demo_counter", 0)
                            }
                        )
                        Toggle(
                            checked = toggled,
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
