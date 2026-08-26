@file:Suppress("FunctionName", "unused")

package org.mdt.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import arc.Core
import arc.graphics.Color
import mindustry.Vars
import mindustry.core.Version
import mindustry.gen.Sounds
import mindustry.graphics.MenuRenderer
import org.mdt.core.ui.compose.*
import org.mdt.core.ui.layout.Arrangement
import org.mdt.core.ui.layout.LayoutPreset
import org.mdt.ui.components.display.canvas.Canvas
import org.mdt.ui.components.display.image.Image
import org.mdt.ui.components.display.tooltip.tooltip
import org.mdt.ui.components.input.slider.Slider
import org.mdt.ui.components.layout.*
import org.mdt.ui.components.surface.*
import org.mdt.ui.components.text.Text

/**
 * ## MainMenuScreen
 *
 * Rich Bento Dashboard Main Menu screen built with clean, modern, modifier-driven
 * declarative composables, 3D animated space canvas background, and reactive telemetry controls.
 */
@Composable
fun MainMenuScreen() {
    var sfxVolume by remember { mutableStateOf(Core.settings?.getInt("sfxvol", 100)?.toFloat()?.div(100f) ?: 1.0f) }
    var musicVolume by remember { mutableStateOf(Core.settings?.getInt("musicvol", 100)?.toFloat()?.div(100f) ?: 1.0f) }
    var ambientToggled by remember { mutableStateOf(true) }

    val menuRenderer = remember { MenuRenderer() }
    DisposableEffect(Unit) {
        onDispose {
            try {
                menuRenderer.dispose()
            } catch (_: Throwable) {}
        }
    }

    Box(modifier = Modifier.anchor(LayoutPreset.FULL_RECT)) {

        // 0. NATIVE 3D ANIMATED SPACE MENU BACKGROUND
        Canvas(modifier = Modifier.fillMaxSize()) {
            try {
                menuRenderer.render()
            } catch (_: Throwable) {}
        }

        // 1. LEFT MAIN NAVIGATION DRAWER (Hugs content dynamically with minWidth protection)
        Card(
            modifier = Modifier
                .anchor(LayoutPreset.LEFT_WIDE)
                .margin(left = 32f, top = 28f, bottom = 28f)
                .minWidth(280f)
                .pad(20f)
                .radius(18f)
                .background(Color.valueOf("10111a").a(0.94f))
                .border(1f, Color.valueOf("25283d"))
        ) {
            Column(gap = 8f) {

                // BRAND HEADER (Hugs icon + text)
                Row(arrangement = Arrangement.spacedBy(12f)) {
                    Image(
                        source = "https://raw.githubusercontent.com/Anuken/Mindustry/master/core/assets-raw/sprites/blocks/distribution/router.png",
                        modifier = Modifier.size(38f).tooltip("Mindustry Engine Core")
                    )
                    Column(gap = 2f) {
                        Text(text = "MINDUSTRY", color = Color.white)
                        Text(text = "NekoMod Pure KMP • ${Version.buildString()}", color = Color.valueOf("9399b2"))
                    }
                }

                Divider(modifier = Modifier.margin(vertical = 4f))

                // SECTION: GAME MODES
                Text(
                    text = "GAME MODES",
                    color = Color.valueOf("89b4fa"),
                    modifier = Modifier.margin(top = 2f)
                )

                Button(
                    text = "▶  PLANETARY CAMPAIGN",
                    colors = ButtonColors.Primary,
                    modifier = Modifier.fillMaxWidth().radius(10f).tooltip("Launch the planetary sector campaign map"),
                    onClick = {
                        Sounds.uiButton.play()
                        Vars.ui?.planet?.show()
                    }
                )

                Button(
                    text = "⚡  CUSTOM SKIRMISH",
                    modifier = Modifier.fillMaxWidth().radius(10f).tooltip("Play custom sandbox or attack game on local maps"),
                    onClick = {
                        Sounds.uiButton.play()
                        Vars.ui?.custom?.show()
                    }
                )

                Button(
                    text = "🌐  MULTIPLAYER SERVERS",
                    modifier = Modifier.fillMaxWidth().radius(10f).tooltip("Browse and connect to multiplayer game servers"),
                    onClick = {
                        Sounds.uiButton.play()
                        Vars.ui?.join?.show()
                    }
                )

                Divider(modifier = Modifier.margin(vertical = 4f))

                // SECTION: CONTENT & TOOLS
                Text(
                    text = "CONTENT & TOOLS",
                    color = Color.valueOf("89b4fa"),
                    modifier = Modifier.margin(top = 2f)
                )

                Button(
                    text = "📖  DATABASE & TECH TREE",
                    modifier = Modifier.fillMaxWidth().tooltip("Inspect blocks, units, and tech research tree"),
                    onClick = {
                        Sounds.uiButton.play()
                        Vars.ui?.database?.show()
                    }
                )

                Button(
                    text = "🛠️  MAP EDITOR",
                    modifier = Modifier.fillMaxWidth().tooltip("Create, edit, and script custom maps"),
                    onClick = {
                        Sounds.uiButton.play()
                        Vars.ui?.maps?.show()
                    }
                )

                Button(
                    text = "📋  SCHEMATICS",
                    modifier = Modifier.fillMaxWidth().tooltip("Manage and preview factory schematics"),
                    onClick = {
                        Sounds.uiButton.play()
                        Vars.ui?.schematics?.show()
                    }
                )

                Button(
                    text = "📦  MODS MANAGER",
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
                        modifier = Modifier.weight(1f).tooltip("Configure graphics, sound, and keybinds"),
                        onClick = {
                            Sounds.uiButton.play()
                            Vars.ui?.settings?.show()
                        }
                    )
                    Button(
                        text = "🚪 EXIT",
                        colors = ButtonColors.Danger,
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
                .minWidth(360f)
                .pad(20f)
                .radius(18f)
                .background(Color.valueOf("10111a").a(0.92f))
                .border(1f, Color.valueOf("25283d"))
        ) {
            Column(gap = 12f) {

                // TOP HERO EXPEDITION CARD
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .pad(16f)
                        .radius(14f)
                        .background(Color.valueOf("181a2b").a(0.95f))
                        .border(1f, Color.valueOf("3b82f6").a(0.4f))
                ) {
                    Column(gap = 8f) {
                        Row(arrangement = Arrangement.spacedBy(8f)) {
                            Text(text = "🪐 PLANETARY EXPEDITION", color = Color.valueOf("93c5fd"))
                            Spacer(modifier = Modifier.weight(1f))
                            Text(text = "● ACTIVE", color = Color.valueOf("a6e3a1"))
                        }
                        Text(
                            text = "Serpulo campaign in progress. Conquer hostile sectors and build launch networks.",
                            color = Color.valueOf("9399b2"),
                            wrap = true
                        )
                        Row(
                            arrangement = Arrangement.spacedBy(8f),
                            modifier = Modifier.fillMaxWidth().margin(top = 2f)
                        ) {
                            Button(
                                text = "▶  RESUME",
                                colors = ButtonColors.Primary,
                                modifier = Modifier.weight(1f).tooltip("Jump directly into active planetary campaign map"),
                                onClick = {
                                    Sounds.uiButton.play()
                                    Vars.ui?.planet?.show()
                                }
                            )
                            Button(
                                text = "⚡  SKIRMISH",
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
                        modifier = Modifier
                            .weight(1f)
                            .pad(12f)
                            .radius(12f)
                            .background(Color.valueOf("161826").a(0.9f))
                            .border(1f, Color.valueOf("25283d"))
                    ) {
                        Column(gap = 2f) {
                            Text(text = "FRAME RATE", color = Color.valueOf("7c829e"))
                            Text(text = "144 FPS", color = Color.valueOf("a6e3a1"))
                            Text(text = "GPU SDF Batch", color = Color.valueOf("9399b2"))
                        }
                    }

                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .pad(12f)
                            .radius(12f)
                            .background(Color.valueOf("161826").a(0.9f))
                            .border(1f, Color.valueOf("25283d"))
                    ) {
                        Column(gap = 2f) {
                            Text(text = "ENGINE CORE", color = Color.valueOf("7c829e"))
                            Text(text = "Pure KMP", color = Color.valueOf("89b4fa"))
                            Text(text = "Build ${Version.build}", color = Color.valueOf("9399b2"))
                        }
                    }
                }

                // FLEXIBLE GROWTH
                Spacer(modifier = Modifier.weight(1f))

                Divider(modifier = Modifier.margin(vertical = 2f))

                // AUDIO & ENVIRONMENT CONTROLS
                Text(
                    text = "QUICK AUDIO & ENVIRONMENT",
                    color = Color.valueOf("89b4fa")
                )

                Slider(
                    value = sfxVolume,
                    onValueChange = {
                        sfxVolume = it
                        Core.settings?.put("sfxvol", (it * 100f).toInt())
                    },
                    label = "SFX Volume",
                    modifier = Modifier.fillMaxWidth()
                )

                Slider(
                    value = musicVolume,
                    onValueChange = {
                        musicVolume = it
                        Core.settings?.put("musicvol", (it * 100f).toInt())
                    },
                    label = "Music Volume",
                    modifier = Modifier.fillMaxWidth()
                )

                Row(arrangement = Arrangement.spacedBy(8f)) {
                    Text(
                        text = "Ambient Particles",
                        color = Color.valueOf("cad3f5"),
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
