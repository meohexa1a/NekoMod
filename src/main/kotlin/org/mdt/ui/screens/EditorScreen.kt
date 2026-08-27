package org.mdt.ui.screens

import androidx.compose.runtime.*
import arc.Core
import arc.input.KeyCode
import org.mdt.core.ui.compose.*
import org.mdt.core.ui.graphics.Color
import org.mdt.core.ui.layout.LayoutPreset
import org.mdt.ui.components.display.Image
import org.mdt.ui.components.layout.*
import org.mdt.ui.components.text.TextNode
import org.mdt.ui.screens.editor.*
import org.mdt.ui.screens.editor.canvas.ComponentCanvas
import org.mdt.ui.screens.editor.canvas.EditorCenterCanvas
import org.mdt.ui.screens.editor.components.EditorSplitPane
import org.mdt.ui.screens.editor.components.SplitSide
import org.mdt.ui.screens.editor.model.EditorMode
import org.mdt.ui.screens.editor.state.EditorDocumentState
import org.mdt.ui.screens.editor.state.EditorSessionState
import org.mdt.ui.screens.editor.state.SceneGraphFactory
import org.mdt.ui.theme.StudioIcons
import org.mdt.ui.theme.Theme

/**
 * ## EditorScreen
 *
 * Professional next-generation UI Visual Designer and Layout Composer for NekoMod.
 *
 * Driven 100% by pure In-Memory Virtual Node Scene Graph ([EditorDocumentState])
 * with zero-GC runtime performance, Artboard Frame presets, and instant 60 FPS Gizmo manipulation.
 *
 * See: docs/design-system/design_system_en.md
 */
@Composable
fun EditorScreen() {
    // Consolidated Reactive Studio Session State & In-Memory Scene Graph
    val session = remember { EditorSessionState() }
    val docState = remember { EditorDocumentState() }

    val colors = Theme.colors
    val shapes = Theme.shapes
    val spacing = Theme.spacing

    // Full-Screen Master Auto-Layout Container with Global Hotkeys
    Box(
        modifier = Modifier
            .anchor(LayoutPreset.FULL_RECT)
            .background(colors.canvasVoid)
            .focusable()
            .onKeyDown { key ->
                val isCtrl = Core.input != null && Core.input.ctrl()
                val isShift = Core.input != null && Core.input.shift()

                when {
                    isCtrl && key == KeyCode.z -> {
                        if (isShift) docState.redo() else docState.undo()
                        true
                    }
                    isCtrl && key == KeyCode.y -> {
                        docState.redo()
                        true
                    }
                    isCtrl && (key == KeyCode.num0 || key == KeyCode.numpad0) -> {
                        session.resetViewport()
                        true
                    }
                    key == KeyCode.del || key == KeyCode.forwardDel -> {
                        docState.removeSelectedNode()
                        true
                    }
                    !isCtrl && key == KeyCode.v -> {
                        session.selectedTool = "select"
                        true
                    }
                    !isCtrl && key == KeyCode.r -> {
                        session.selectedTool = "rect"
                        true
                    }
                    !isCtrl && key == KeyCode.t -> {
                        session.selectedTool = "text"
                        true
                    }
                    else -> false
                }
            }
    ) {
        // =====================================================================
        // I. Master Structural Layout (TopBar + 3-Panel Body + Bottom StatusBar)
        // =====================================================================
        Column(modifier = Modifier.fillMaxSize()) {

            // 1. Top Navigation Bar (Segmented Mode Switcher + Actions)
            EditorTopBar(
                currentMode = session.selectedMode,
                onSelectMode = {
                    session.selectedMode = it
                    if (session.isSidebarCollapsed) session.isSidebarCollapsed = false
                    if (session.isInspectorCollapsed) session.isInspectorCollapsed = false
                },
                onPreview = {},
                onReload = {
                    docState.rootScene = SceneGraphFactory.createDefaultScene()
                    docState.expandAll(docState.rootScene)
                    docState.selectedNodeId = null
                }
            )

            // 2. Main Workspace Body (Using 2-Way Bound EditorSplitPane)
            if (session.selectedMode == EditorMode.SETTINGS) {
                // Dedicated Full-Screen Studio Settings & Preferences
                org.mdt.ui.screens.editor.settings.EditorSettingsScreen(
                    modifier = Modifier.weight(1.0f).fillMaxWidth()
                )
            } else if (session.selectedMode == EditorMode.I18N) {
                // Dedicated Full-Screen Internationalization & Localization Studio
                org.mdt.ui.screens.editor.i18n.I18nWorkspaceScreen(
                    modifier = Modifier.weight(1.0f).fillMaxWidth()
                )
            } else {
                EditorSplitPane(
                    splitWidth = session.sidebarWidth,
                    onSplitWidthChange = { session.sidebarWidth = it },
                    isCollapsed = session.isSidebarCollapsed,
                    side = SplitSide.START,
                    modifier = Modifier.weight(1.0f).fillMaxWidth().clip(true),
                    startPanel = {
                        EditorLeftSidebar(
                            mode = session.selectedMode,
                            docState = docState,
                            selectedComponent = session.selectedComponent,
                            onSelectComponent = { session.selectedComponent = it },
                            onCollapse = { session.isSidebarCollapsed = true }
                        )
                    },
                    endPanel = {
                        EditorSplitPane(
                            splitWidth = session.inspectorWidth,
                            onSplitWidthChange = { session.inspectorWidth = it },
                            isCollapsed = session.isInspectorCollapsed,
                            side = SplitSide.END,
                            modifier = Modifier.fillMaxSize(),
                            startPanel = {
                                Box(modifier = Modifier.fillMaxSize()) {
                                    if (session.selectedMode == EditorMode.COMPONENTS) {
                                        ComponentCanvas(
                                            componentName = session.selectedComponent,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else {
                                        EditorCenterCanvas(
                                            panX = session.panX,
                                            panY = session.panY,
                                            zoomScale = session.zoomScale,
                                            selectedTool = session.selectedTool,
                                            docState = docState,
                                            onPanChange = { px, py ->
                                                session.panX = px
                                                session.panY = py
                                            },
                                            onZoomChange = { session.zoomScale = it },
                                            onCreateRect = { x, y, w, h, isCard ->
                                                val targetParent = (docState.selectedNode as? LayoutNode) ?: docState.rootScene
                                                val box = LayoutNode().apply {
                                                    id = "box_${System.currentTimeMillis() % 100000}"
                                                    name = if (isCard) "Frame" else "Box"
                                                    width = w
                                                    height = h
                                                    anchorData.offsetLeft = x
                                                    anchorData.offsetTop = y
                                                    val vis = ensureVisuals()
                                                    vis.background.mode = BackgroundFill.Mode.COLOR
                                                    vis.background.color = if (isCard) Color.valueOf("bf5af2") else Color.valueOf("0a84ff")
                                                    vis.radii.set(8f)
                                                }
                                                docState.addNode(targetParent, box)
                                                session.selectedTool = "select"
                                            },
                                            onCreateText = { x, y ->
                                                val targetParent = (docState.selectedNode as? LayoutNode) ?: docState.rootScene
                                                val text = TextNode(text = "New Text").apply {
                                                    id = "text_${System.currentTimeMillis() % 100000}"
                                                    name = "Text Node"
                                                    anchorData.offsetLeft = x
                                                    anchorData.offsetTop = y
                                                    textVisuals.color = Color.White
                                                }
                                                docState.addNode(targetParent, text)
                                                session.selectedTool = "select"
                                            },
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }

                                    // Floating Left Expand Button
                                    if (session.isSidebarCollapsed) {
                                        Box(
                                            modifier = Modifier
                                                .anchor(LayoutPreset.TOP_LEFT)
                                                .margin(left = spacing.md, top = spacing.md)
                                                .size(36f, 36f)
                                                .radius(shapes.md)
                                                .background(colors.surfaceElevated)
                                                .border(1f, colors.borderHairline)
                                                .clickable { session.isSidebarCollapsed = false }
                                                .pad(8f)
                                        ) {
                                            Image(
                                                source = StudioIcons.CHEVRON_RIGHT,
                                                modifier = Modifier.fillMaxSize(),
                                                tint = colors.blue
                                            )
                                        }
                                    }

                                    // Floating Right Expand Button
                                    if (session.isInspectorCollapsed) {
                                        Box(
                                            modifier = Modifier
                                                .anchor(LayoutPreset.TOP_RIGHT)
                                                .margin(right = spacing.md, top = spacing.md)
                                                .size(36f, 36f)
                                                .radius(shapes.md)
                                                .background(colors.surfaceElevated)
                                                .border(1f, colors.borderHairline)
                                                .clickable { session.isInspectorCollapsed = false }
                                                .pad(8f)
                                        ) {
                                            Image(
                                                source = StudioIcons.CHEVRON_LEFT,
                                                modifier = Modifier.fillMaxSize(),
                                                tint = colors.blue
                                            )
                                        }
                                    }
                                }
                            },
                            endPanel = {
                                EditorRightInspector(
                                    docState = docState,
                                    selectedComponent = session.selectedComponent,
                                    isComponentMode = session.selectedMode == EditorMode.COMPONENTS,
                                    onCollapse = { session.isInspectorCollapsed = true }
                                )
                            }
                        )
                    }
                )
            }

            // 3. Bottom Pro Status Bar (VSCode / Xcode Style, Full Width Edge-to-Edge)
            val activeItemName = docState.selectedNode?.name
                ?: docState.selectedNode?.javaClass?.simpleName
                ?: docState.rootScene.name

            EditorStatusBar(
                selectedItem = activeItemName,
                selectedComponent = session.selectedComponent,
                isComponentMode = session.selectedMode == EditorMode.COMPONENTS,
                panX = session.panX,
                panY = session.panY,
                zoom = session.zoomScale
            )
        }

        // =====================================================================
        // II. Master Top-Level Floating Tool Dock (Shown in Scene Assembly Mode)
        // =====================================================================
        if (session.selectedMode == EditorMode.SCENE) {
            Box(
                modifier = Modifier
                    .anchor(LayoutPreset.CENTER_BOTTOM)
                    .margin(bottom = 38f)
            ) {
                EditorToolDock(
                    selectedTool = session.selectedTool,
                    onSelectTool = { session.selectedTool = it }
                )
            }
        }
    }
}
