package org.mdt.ui.screens.editor.settings

import androidx.compose.runtime.*
import org.mdt.core.ui.compose.*
import org.mdt.core.ui.graphics.Color
import org.mdt.core.ui.layout.Alignment
import org.mdt.core.ui.layout.Arrangement
import org.mdt.core.ui.layout.LayoutPreset
import org.mdt.ui.components.display.Image
import org.mdt.ui.components.layout.Box
import org.mdt.ui.components.layout.Column
import org.mdt.ui.components.layout.Row
import org.mdt.ui.components.layout.Spacer
import org.mdt.ui.components.scroll.ScrollView
import org.mdt.ui.components.surface.Divider
import org.mdt.ui.components.text.MonoText
import org.mdt.ui.components.text.Text
import org.mdt.ui.theme.StudioIcons
import org.mdt.ui.theme.Theme

/**
 * ## SettingsSection
 *
 * Navigation sections for the Full-Screen Studio Preferences workspace.
 */
enum class SettingsSection(val label: String, val iconUrl: String) {
    GENERAL("General", StudioIcons.SETTINGS),
    VIEWPORT("Viewport & Grid", StudioIcons.FRAME),
    GPU("GPU & Shaders", StudioIcons.CARD),
    COMPONENTS("UI Components", StudioIcons.COMPONENTS),
    SHORTCUTS("Shortcuts", StudioIcons.CODE),
    ABOUT("About Studio", StudioIcons.STUDIO_LOGO)
}

/**
 * ## EditorSettingsScreen
 *
 * Professional 2-Column Full-Screen Studio Preferences workspace:
 * - Left: Floating Settings Navigation Menu (General, Viewport, GPU, Shortcuts, About).
 * - Right: Smoothly scrollable, dedicated sub-screen content avoiding UI bloating.
 *
 * See: docs/design-system/design_system_en.md
 */
@Composable
fun EditorSettingsScreen(
    modifier: UIModifier = UIModifier
) {
    var activeSection by remember { mutableStateOf(SettingsSection.GENERAL) }
    var sidebarWidth by remember { mutableStateOf(220f) }
    var isHoveringHandle by remember { mutableStateOf(false) }
    var isDraggingHandle by remember { mutableStateOf(false) }

    val colors = Theme.colors
    val shapes = Theme.shapes
    val spacing = Theme.spacing
    val typography = Theme.typography

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.canvasVoid)
    ) {
        Row(modifier = Modifier.fillMaxSize()) {

            // =================================================================
            // 1. Left Resizable Navigation Menu Panel
            // =================================================================
            Box(
                modifier = Modifier
                    .width(sidebarWidth)
                    .fillMaxHeight()
            ) {
                // Sidebar Content
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(colors.surfacePrimary)
                        .border(1f, colors.borderHairline)
                        .pad(spacing.md)
                ) {
                    Column(
                        arrangement = Arrangement.spacedBy(spacing.sm),
                        alignment = Alignment.TopStart,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Preferences",
                            font = typography.title,
                            color = colors.textPrimary,
                            modifier = Modifier.pad(horizontal = spacing.xs, vertical = spacing.xs)
                        )

                        Divider(modifier = Modifier.fillMaxWidth().height(1f))

                        for (section in SettingsSection.values()) {
                            val isSelected = section == activeSection
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .radius(shapes.xs)
                                    .background(if (isSelected) colors.blue else Color.Clear)
                                    .clickable { activeSection = section }
                                    .pad(horizontal = spacing.sm, vertical = 6f)
                            ) {
                                Row(
                                    arrangement = Arrangement.spacedBy(spacing.sm),
                                    alignment = Alignment.CenterStart,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Image(
                                        source = section.iconUrl,
                                        modifier = Modifier.size(16f),
                                        tint = if (isSelected) Color.White else colors.textSecondary
                                    )
                                    MonoText(
                                        text = section.label,
                                        color = if (isSelected) Color.White else colors.textPrimary
                                    )
                                }
                            }
                        }
                    }
                }

                // Interactive Seamless Edge Drag Seam
                Box(
                    modifier = Modifier
                        .anchor(LayoutPreset.RIGHT_WIDE)
                        .width(12f)
                        .cursor(arc.Graphics.Cursor.SystemCursor.horizontalResize)
                        .hoverable { isHoveringHandle = it }
                        .onPointerDown { isDraggingHandle = true }
                        .onPointerUp { isDraggingHandle = false }
                        .onPointerDrag { event ->
                            sidebarWidth = event.x.coerceIn(160f, 600f)
                        }
                ) {
                    org.mdt.ui.components.surface.ResizeGripHandle(
                        isHovered = isHoveringHandle,
                        isDragging = isDraggingHandle
                    )
                }
            }

            // =================================================================
            // 2. Right Floating Content Area with Smooth Scroll
            // =================================================================
            Box(
                modifier = Modifier
                    .weight(1.0f)
                    .fillMaxHeight()
            ) {
                ScrollView(
                    modifier = Modifier
                        .fillMaxSize()
                        .pad(horizontal = 48f, vertical = spacing.lg),
                    enableVertical = true
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .pad(bottom = 60f)
                    ) {
                        when (activeSection) {
                            SettingsSection.GENERAL -> SettingsGeneralTab()
                            SettingsSection.VIEWPORT -> SettingsViewportTab()
                            SettingsSection.GPU -> SettingsGpuTab()
                            SettingsSection.COMPONENTS -> SettingsComponentsTab()
                            SettingsSection.SHORTCUTS -> SettingsShortcutsTab()
                            SettingsSection.ABOUT -> SettingsAboutTab()
                        }
                    }
                }
            }
        }
    }
}
