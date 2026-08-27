@file:Suppress("FunctionName", "unused")

package org.mdt.ui.screens

import androidx.compose.runtime.*
import arc.Core
import mindustry.Vars
import mindustry.core.Version
import mindustry.gen.Icon
import mindustry.gen.Sounds
import mindustry.graphics.MenuRenderer
import org.mdt.core.engine.EngineContext
import org.mdt.core.engine.settings.rememberSettings
import org.mdt.core.ui.compose.*
import org.mdt.core.ui.layout.Alignment
import org.mdt.core.ui.layout.Arrangement
import org.mdt.core.ui.layout.LayoutPreset
import org.mdt.ui.components.display.*
import org.mdt.ui.components.input.Slider
import org.mdt.ui.components.layout.*
import org.mdt.ui.components.surface.*
import org.mdt.ui.components.text.Text
import org.mdt.ui.theme.NekoTheme
import org.mdt.ui.theme.Theme

/**
 * ## MainMenuScreen
 *
 * Minimalist Apple iOS Frosted Glass Main Menu with native Mindustry [Icon] integration.
 * Zero unicode emojis to guarantee perfect BMFont baseline alignment.
 *
 * See: docs/design-system/design_system_en.md
 */
@Composable
fun MainMenuScreen() {
    NekoTheme {
        val colors = Theme.colors
        val shapes = Theme.shapes

        val settings by rememberSettings()
        val sfxVolume = settings.audio.sfxVolume
        val musicVolume = settings.audio.musicVolume
        val ambientToggled = settings.audio.ambientEnabled

        val menuRenderer = remember { MenuRenderer() }
        DisposableEffect(Unit) {
            onDispose {
                try {
                    menuRenderer.dispose()
                } catch (_: Throwable) {}
            }
        }

        Box(modifier = Modifier.anchor(LayoutPreset.FULL_RECT)) {

            // 0. FULLSCREEN 3D ANIMATED SPACE CANVAS
            Canvas(modifier = Modifier.fillMaxSize()) {
                try {
                    menuRenderer.render()
                } catch (_: Throwable) {}
            }

            // 1. TOP FLOATING ISLAND (Hug content with generous minWidth)
            Box(
                modifier = Modifier
                    .anchor(LayoutPreset.CENTER_TOP)
                    .margin(top = 28f)
            ) {
                Card(
                    variant = CardVariant.GLASS,
                    modifier = Modifier.radius(shapes.pill).pad(8f).minWidth(460f)
                ) {
                    Row(
                        arrangement = Arrangement.spacedBy(14f),
                        alignment = Alignment.CenterStart,
                        modifier = Modifier.pad(horizontal = 14f, vertical = 2f)
                    ) {
                        Image(
                            source = "https://raw.githubusercontent.com/Anuken/Mindustry/master/core/assets-raw/sprites/blocks/distribution/router.png",
                            modifier = Modifier.size(24f).tooltip("Mindustry Pure KMP Engine")
                        )

                        Text(
                            text = "MINDUSTRY",
                            color = colors.textPrimary
                        )

                        Badge(
                            text = "v${Version.build}",
                            variant = BadgeVariant.SUCCESS
                        )

                        Spacer(modifier = Modifier.weight(1f))

                        Divider(
                            modifier = Modifier
                                .width(1f)
                                .height(16f)
                        )

                        Button(
                            text = "Settings",
                            icon = Icon.settingsSmall,
                            variant = ButtonVariant.PLAIN,
                            modifier = Modifier.pad(horizontal = 8f, vertical = 4f),
                            onClick = {
                                Sounds.uiButton.play()
                                Vars.ui?.settings?.show()
                            }
                        )

                        Button(
                            text = "Mods",
                            icon = Icon.bookSmall,
                            variant = ButtonVariant.PLAIN,
                            modifier = Modifier.pad(horizontal = 8f, vertical = 4f),
                            onClick = {
                                Sounds.uiButton.play()
                                Vars.ui?.mods?.show()
                            }
                        )

                        Button(
                            text = "Exit",
                            icon = Icon.exitSmall,
                            variant = ButtonVariant.DESTRUCTIVE,
                            modifier = Modifier.radius(shapes.pill).pad(horizontal = 12f, vertical = 4f),
                            onClick = {
                                Sounds.uiButton.play()
                                Core.app.exit()
                            }
                        )
                    }
                }
            }

            // 2. CENTERED FOCUSED ACTION DECK (Hug content with generous minWidth)
            Box(
                modifier = Modifier
                    .anchor(LayoutPreset.CENTER)
            ) {
                Card(
                    variant = CardVariant.GLASS,
                    modifier = Modifier.radius(shapes.xl).pad(28f).minWidth(450f)
                ) {
                    Column(gap = 14f, modifier = Modifier.fillMaxWidth()) {

                        // Header Tagline
                        Column(gap = 4f, modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "EXPEDITION",
                                color = colors.blue
                            )
                            Text(
                                text = "Select an operation mode to begin.",
                                color = colors.textSecondary
                            )
                        }

                        // Primary Play Button
                        Button(
                            text = "PLAY CAMPAIGN",
                            icon = Icon.play,
                            variant = ButtonVariant.FILLED,
                            modifier = Modifier.fillMaxWidth().pad(vertical = 12f).tooltip("Launch planetary campaign map"),
                            onClick = {
                                Sounds.uiButton.play()
                                Vars.ui?.planet?.show()
                            }
                        )

                        // Secondary Modes
                        Button(
                            text = "CUSTOM SKIRMISH",
                            icon = Icon.hammer,
                            variant = ButtonVariant.GLASS,
                            modifier = Modifier.fillMaxWidth().pad(vertical = 10f).tooltip("Play custom sandbox or skirmish game"),
                            onClick = {
                                Sounds.uiButton.play()
                                Vars.ui?.custom?.show()
                            }
                        )

                        Button(
                            text = "JOIN MULTIPLAYER",
                            icon = Icon.host,
                            variant = ButtonVariant.GLASS,
                            modifier = Modifier.fillMaxWidth().pad(vertical = 10f).tooltip("Browse public & community servers"),
                            onClick = {
                                Sounds.uiButton.play()
                                Vars.ui?.join?.show()
                            }
                        )

                        Divider(modifier = Modifier.margin(vertical = 4f))

                        // Secondary Quick Tools Row (Generous spacing and distinct tinted buttons with Mindustry Icons)
                        Row(
                            arrangement = Arrangement.spacedBy(10f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(
                                text = "Tech Tree",
                                icon = Icon.treeSmall,
                                variant = ButtonVariant.TINTED,
                                modifier = Modifier.weight(1f).radius(shapes.md).tooltip("Inspect technology research"),
                                onClick = {
                                    Sounds.uiButton.play()
                                    Vars.ui?.database?.show()
                                }
                            )

                            Button(
                                text = "Map Editor",
                                icon = Icon.terrain,
                                variant = ButtonVariant.TINTED,
                                modifier = Modifier.weight(1f).radius(shapes.md).tooltip("Create & edit maps"),
                                onClick = {
                                    Sounds.uiButton.play()
                                    Vars.ui?.maps?.show()
                                }
                            )

                            Button(
                                text = "Schematics",
                                icon = Icon.pasteSmall,
                                variant = ButtonVariant.TINTED,
                                modifier = Modifier.weight(1f).radius(shapes.md).tooltip("Factory blueprints"),
                                onClick = {
                                    Sounds.uiButton.play()
                                    Vars.ui?.schematics?.show()
                                }
                            )
                        }
                    }
                }
            }

            // 3. BOTTOM FLOATING CONTROL DOCK (Hug content with generous minWidth)
            Box(
                modifier = Modifier
                    .anchor(LayoutPreset.CENTER_BOTTOM)
                    .margin(bottom = 28f)
            ) {
                Card(
                    variant = CardVariant.GLASS,
                    modifier = Modifier.radius(shapes.pill).pad(10f).minWidth(520f)
                ) {
                    Row(
                        arrangement = Arrangement.spacedBy(16f),
                        alignment = Alignment.CenterStart,
                        modifier = Modifier.pad(horizontal = 14f, vertical = 2f)
                    ) {
                        // SFX Slider with inner label and % readout
                        org.mdt.ui.components.input.LabeledSlider(
                            label = "SFX",
                            value = sfxVolume,
                            onValueChange = { v ->
                                EngineContext.default.settings.updateAudio { it.copy(sfxVolume = v) }
                            },
                            modifier = Modifier.minWidth(155f)
                        )

                        Divider(
                            modifier = Modifier
                                .width(1f)
                                .height(20f)
                        )

                        // Music Slider with inner label and % readout
                        org.mdt.ui.components.input.LabeledSlider(
                            label = "Music",
                            value = musicVolume,
                            onValueChange = { v ->
                                EngineContext.default.settings.updateAudio { it.copy(musicVolume = v) }
                            },
                            modifier = Modifier.minWidth(155f)
                        )

                        Divider(
                            modifier = Modifier
                                .width(1f)
                                .height(20f)
                        )

                        // Particles Toggle
                        Row(
                            arrangement = Arrangement.spacedBy(10f),
                            alignment = Alignment.CenterStart
                        ) {
                            Text(text = "Particles", color = colors.textSecondary)
                            Toggle(
                                checked = ambientToggled,
                                onToggle = { t ->
                                    EngineContext.default.settings.updateAudio { it.copy(ambientEnabled = t) }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
