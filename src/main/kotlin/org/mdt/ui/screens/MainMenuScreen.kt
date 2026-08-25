@file:Suppress("FunctionName", "unused")

package org.mdt.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import arc.Core
import arc.graphics.Color
import mindustry.Vars
import mindustry.core.Version
import mindustry.gen.Sounds
import mindustry.ui.Fonts
import org.mdt.ui.components.display.image.Image
import org.mdt.ui.components.display.tooltip.tooltip
import org.mdt.ui.components.input.slider.Slider
import org.mdt.ui.components.layout.Box
import org.mdt.ui.components.layout.Column
import org.mdt.ui.components.layout.Row
import org.mdt.ui.components.layout.Spacer
import org.mdt.ui.components.surface.Button
import org.mdt.ui.components.surface.ButtonColors
import org.mdt.ui.components.surface.Card
import org.mdt.ui.components.surface.Divider
import org.mdt.ui.components.surface.Toggle
import org.mdt.ui.components.text.Text
import org.mdt.ui.compose.Modifier
import org.mdt.ui.compose.anchor
import org.mdt.ui.compose.fillMaxWidth
import org.mdt.ui.compose.height
import org.mdt.ui.compose.margin
import org.mdt.ui.compose.minWidth
import org.mdt.ui.compose.size
import org.mdt.ui.compose.weight
import org.mdt.ui.layout.Arrangement
import org.mdt.ui.layout.LayoutPreset

/**
 * ## MainMenuScreen
 *
 * Rich Bento Dashboard Main Menu screen featuring dynamic Hug Content sizing,
 * expansive navigation drawer, and reactive audio telemetry controls.
 */
@Composable
fun MainMenuScreen() {
    var sfxVolume by remember { mutableStateOf(Core.settings?.getInt("sfxvol", 100)?.toFloat()?.div(100f) ?: 1.0f) }
    var musicVolume by remember { mutableStateOf(Core.settings?.getInt("musicvol", 100)?.toFloat()?.div(100f) ?: 1.0f) }
    var ambientToggled by remember { mutableStateOf(true) }

    Box(modifier = Modifier.anchor(LayoutPreset.FULL_RECT)) {

        // 1. LEFT MAIN NAVIGATION DRAWER (Hugs content dynamically with minWidth protection)
        Card(
            modifier = Modifier
                .anchor(LayoutPreset.LEFT_WIDE)
                .margin(left = 32f, top = 28f, bottom = 28f)
                .minWidth(280f),
            backgroundColor = Color.valueOf("10111a").a(0.94f),
            borderColor = Color.valueOf("25283d"),
            radius = 18f,
            padding = 20f
        ) {
            Column(gap = 8f) {

                // BRAND HEADER (Hugs icon + text)
                Row(arrangement = Arrangement.spacedBy(12f)) {
                    Image(
                        source = "https://raw.githubusercontent.com/Anuken/Mindustry/master/core/assets-raw/sprites/blocks/distribution/router.png",
                        modifier = Modifier.size(38f, 38f).tooltip("Mindustry Engine Core")
                    )
                    Column(gap = 2f) {
                        Text(
                            text = "MINDUSTRY",
                            font = Fonts.def,
                            color = Color.white,
                            scale = 1.0f
                        )
                        Text(
                            text = "NekoMod Pure KMP • ${Version.buildString()}",
                            font = Fonts.def,
                            color = Color.valueOf("9399b2"),
                            scale = 1.0f
                        )
                    }
                }

                Divider(modifier = Modifier.margin(vertical = 4f))

                // SECTION: GAME MODES
                Text(
                    text = "GAME MODES",
                    font = Fonts.def,
                    color = Color.valueOf("89b4fa"),
                    scale = 1.0f,
                    modifier = Modifier.margin(top = 2f)
                )

                Button(
                    text = "▶  PLANETARY CAMPAIGN",
                    colors = ButtonColors.Primary,
                    paddingV = 10f,
                    paddingH = 16f,
                    radius = 10f,
                    modifier = Modifier.fillMaxWidth().tooltip("Launch the planetary sector campaign map"),
                    onClick = {
                        Sounds.uiButton.play()
                        Vars.ui?.planet?.show()
                    }
                )

                Button(
                    text = "⚡  CUSTOM SKIRMISH",
                    paddingV = 9f,
                    paddingH = 16f,
                    radius = 10f,
                    modifier = Modifier.fillMaxWidth().tooltip("Play custom sandbox or attack game on local maps"),
                    onClick = {
                        Sounds.uiButton.play()
                        Vars.ui?.custom?.show()
                    }
                )

                Button(
                    text = "🌐  MULTIPLAYER SERVERS",
                    paddingV = 9f,
                    paddingH = 16f,
                    radius = 10f,
                    modifier = Modifier.fillMaxWidth().tooltip("Browse and connect to multiplayer game servers"),
                    onClick = {
                        Sounds.uiButton.play()
                        Vars.ui?.join?.show()
                    }
                )

                Divider(modifier = Modifier.margin(vertical = 4f))

                // SECTION: CONTENT & TOOLS
                Text(
                    text = "CONTENT & TOOLS",
                    font = Fonts.def,
                    color = Color.valueOf("89b4fa"),
                    scale = 1.0f,
                    modifier = Modifier.margin(top = 2f)
                )

                Button(
                    text = "📖  DATABASE & TECH TREE",
                    paddingV = 8f,
                    paddingH = 14f,
                    radius = 9f,
                    modifier = Modifier.fillMaxWidth().tooltip("Inspect blocks, units, and tech research tree"),
                    onClick = {
                        Sounds.uiButton.play()
                        Vars.ui?.database?.show()
                    }
                )

                Button(
                    text = "🛠️  MAP EDITOR",
                    paddingV = 8f,
                    paddingH = 14f,
                    radius = 9f,
                    modifier = Modifier.fillMaxWidth().tooltip("Create, edit, and script custom maps"),
                    onClick = {
                        Sounds.uiButton.play()
                        Vars.ui?.maps?.show()
                    }
                )

                Button(
                    text = "📋  SCHEMATICS",
                    paddingV = 8f,
                    paddingH = 14f,
                    radius = 9f,
                    modifier = Modifier.fillMaxWidth().tooltip("Manage and preview factory schematics"),
                    onClick = {
                        Sounds.uiButton.play()
                        Vars.ui?.schematics?.show()
                    }
                )

                Button(
                    text = "📦  MODS MANAGER",
                    paddingV = 8f,
                    paddingH = 14f,
                    radius = 9f,
                    modifier = Modifier.fillMaxWidth().tooltip("Browse installed mods and GitHub community mods"),
                    onClick = {
                        Sounds.uiButton.play()
                        Vars.ui?.mods?.show()
                    }
                )

                // FLEXIBLE GROWTH PUSHING SYSTEM CONTROLS TO THE BOTTOM
                Spacer(modifier = Modifier.weight(1f))

                Divider(modifier = Modifier.margin(vertical = 4f))

                // SYSTEM ACTIONS ROW
                Row(arrangement = Arrangement.spacedBy(8f)) {
                    Button(
                        text = "⚙️ SETTINGS",
                        paddingV = 9f,
                        paddingH = 12f,
                        radius = 10f,
                        modifier = Modifier.weight(1f).tooltip("Configure graphics, sound, and keybinds"),
                        onClick = {
                            Sounds.uiButton.play()
                            Vars.ui?.settings?.show()
                        }
                    )
                    Button(
                        text = "🚪 EXIT",
                        colors = ButtonColors.Danger,
                        paddingV = 9f,
                        paddingH = 12f,
                        radius = 10f,
                        modifier = Modifier.weight(1f).tooltip("Exit game to desktop"),
                        onClick = {
                            Sounds.uiButton.play()
                            Core.app.exit()
                        }
                    )
                }
            }
        }

        // 2. RIGHT BENTO CONTAINER (Hugs content dynamically)
        Card(
            modifier = Modifier
                .anchor(LayoutPreset.RIGHT_WIDE)
                .margin(right = 32f, top = 28f, bottom = 28f)
                .minWidth(360f),
            backgroundColor = Color.valueOf("10111a").a(0.92f),
            borderColor = Color.valueOf("25283d"),
            radius = 18f,
            padding = 20f
        ) {
            Column(gap = 12f) {

                // TOP HERO EXPEDITION CARD
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = Color.valueOf("181a2b").a(0.95f),
                    borderColor = Color.valueOf("3b82f6").a(0.4f),
                    radius = 14f,
                    padding = 16f
                ) {
                    Column(gap = 8f) {
                        Row(arrangement = Arrangement.spacedBy(8f)) {
                            Text(
                                text = "🪐 PLANETARY EXPEDITION",
                                font = Fonts.def,
                                color = Color.valueOf("93c5fd"),
                                scale = 1.0f
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            Text(
                                text = "● ACTIVE",
                                font = Fonts.def,
                                color = Color.valueOf("a6e3a1"),
                                scale = 1.0f
                            )
                        }
                        Text(
                            text = "Serpulo campaign in progress. Conquer hostile sectors and build launch networks.",
                            font = Fonts.def,
                            color = Color.valueOf("9399b2"),
                            scale = 1.0f,
                            wrap = true
                        )
                        Row(arrangement = Arrangement.spacedBy(8f), modifier = Modifier.fillMaxWidth().margin(top = 2f)) {
                            Button(
                                text = "▶  RESUME",
                                colors = ButtonColors.Primary,
                                paddingV = 9f,
                                paddingH = 14f,
                                radius = 8f,
                                modifier = Modifier.weight(1f).tooltip("Jump directly into active planetary campaign map"),
                                onClick = {
                                    Sounds.uiButton.play()
                                    Vars.ui?.planet?.show()
                                }
                            )
                            Button(
                                text = "⚡  SKIRMISH",
                                paddingV = 9f,
                                paddingH = 14f,
                                radius = 8f,
                                modifier = Modifier.weight(1f).tooltip("Launch custom game skirmish"),
                                onClick = {
                                    Sounds.uiButton.play()
                                    Vars.ui?.custom?.show()
                                }
                            )
                        }
                    }
                }

                // TELEMETRY & SYSTEM STATS BENTO ROW (Both cards hug text and expand equally)
                Row(arrangement = Arrangement.spacedBy(10f), modifier = Modifier.fillMaxWidth()) {
                    Card(
                        modifier = Modifier.weight(1f),
                        backgroundColor = Color.valueOf("161826").a(0.9f),
                        borderColor = Color.valueOf("25283d"),
                        radius = 12f,
                        padding = 12f
                    ) {
                        Column(gap = 2f) {
                            Text(text = "FRAME RATE", font = Fonts.def, color = Color.valueOf("7c829e"), scale = 1.0f)
                            Text(text = "144 FPS", font = Fonts.def, color = Color.valueOf("a6e3a1"), scale = 1.0f)
                            Text(text = "GPU SDF Batch", font = Fonts.def, color = Color.valueOf("9399b2"), scale = 1.0f)
                        }
                    }

                    Card(
                        modifier = Modifier.weight(1f),
                        backgroundColor = Color.valueOf("161826").a(0.9f),
                        borderColor = Color.valueOf("25283d"),
                        radius = 12f,
                        padding = 12f
                    ) {
                        Column(gap = 2f) {
                            Text(text = "ENGINE CORE", font = Fonts.def, color = Color.valueOf("7c829e"), scale = 1.0f)
                            Text(text = "Pure KMP", font = Fonts.def, color = Color.valueOf("89b4fa"), scale = 1.0f)
                            Text(text = "Build ${Version.build}", font = Fonts.def, color = Color.valueOf("9399b2"), scale = 1.0f)
                        }
                    }
                }

                // FLEXIBLE GROWTH
                Spacer(modifier = Modifier.weight(1f))

                Divider(modifier = Modifier.margin(vertical = 2f))

                // AUDIO & ENVIRONMENT CONTROLS
                Text(
                    text = "QUICK AUDIO & ENVIRONMENT",
                    font = Fonts.def,
                    color = Color.valueOf("89b4fa"),
                    scale = 1.0f
                )

                Slider(
                    value = sfxVolume,
                    onValueChange = {
                        sfxVolume = it
                        Core.settings?.put("sfxvol", (it * 100f).toInt())
                    },
                    label = "SFX Volume",
                    modifier = Modifier.fillMaxWidth().height(28f)
                )

                Slider(
                    value = musicVolume,
                    onValueChange = {
                        musicVolume = it
                        Core.settings?.put("musicvol", (it * 100f).toInt())
                    },
                    label = "Music Volume",
                    modifier = Modifier.fillMaxWidth().height(28f)
                )

                Row(arrangement = Arrangement.spacedBy(8f)) {
                    Text(
                        text = "Ambient Particles",
                        font = Fonts.def,
                        color = Color.valueOf("cad3f5"),
                        scale = 1.0f,
                        modifier = Modifier.weight(1f)
                    )
                    Toggle(
                        checked = ambientToggled,
                        onToggle = { ambientToggled = it }
                    )
                }
            }
        }
    }
}
