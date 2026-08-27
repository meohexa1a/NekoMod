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

    var isHoveringLeftHandle by remember { mutableStateOf(false) }
    var isDraggingLeftHandle by remember { mutableStateOf(false) }
    var isHoveringRightHandle by remember { mutableStateOf(false) }
    var isDraggingRightHandle by remember { mutableStateOf(false) }

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

            // 2. Main Workspace Body (Left Resizable Sidebar + Center Viewport + Right Inspector)
            Row(modifier = Modifier.weight(1.0f).fillMaxWidth().clip(true)) {

                // A. Left Resizable Sidebar (Zero-Gap Layout with Overlay Resize Seam)
                if (!session.isSidebarCollapsed && session.selectedMode != EditorMode.SETTINGS && session.selectedMode != EditorMode.I18N) {
                    Box(
                        modifier = Modifier
                            .width(session.sidebarWidth)
                            .fillMaxHeight()
                    ) {
                        // Sidebar Content
                        EditorLeftSidebar(
                            mode = session.selectedMode,
                            docState = docState,
                            selectedComponent = session.selectedComponent,
                            onSelectComponent = { session.selectedComponent = it },
                            onCollapse = { session.isSidebarCollapsed = true }
                        )

                        // Interactive Seamless Edge Drag Seam
                        Box(
                            modifier = Modifier
                                .anchor(LayoutPreset.RIGHT_WIDE)
                                .width(12f)
                                .cursor(arc.Graphics.Cursor.SystemCursor.horizontalResize)
                                .hoverable { isHoveringLeftHandle = it }
                                .onPointerDown { isDraggingLeftHandle = true }
                                .onPointerUp { isDraggingLeftHandle = false }
                                .onPointerDrag { event ->
                                    session.sidebarWidth = event.x.coerceIn(160f, 900f)
                                }
                        ) {
                            org.mdt.ui.components.surface.ResizeGripHandle(
                                isHovered = isHoveringLeftHandle,
                                isDragging = isDraggingLeftHandle
                            )
                        }
                    }
                }

                // B. Center Viewport (Settings in SETTINGS Mode, i18n in I18N Mode, ComponentCanvas in COMPONENTS Mode, EditorCenterCanvas in SCENE Mode)
                Box(
                    modifier = Modifier
                        .weight(1.0f)
                        .fillMaxHeight()
                ) {
                    if (session.selectedMode == EditorMode.SETTINGS) {
                        // Dedicated Full-Screen Studio Settings & Preferences
                        org.mdt.ui.screens.editor.settings.EditorSettingsScreen(
                            modifier = Modifier.fillMaxSize()
                        )
                    } else if (session.selectedMode == EditorMode.I18N) {
                        // Dedicated Full-Screen Internationalization & Localization Studio
                        org.mdt.ui.screens.editor.i18n.I18nWorkspaceScreen(
                            modifier = Modifier.fillMaxSize()
                        )
                    } else if (session.selectedMode == EditorMode.COMPONENTS) {
                        // Dedicated Isolated Component Workspace
                        ComponentCanvas(
                            componentName = session.selectedComponent,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        // Full Scene Canvas with Direct Virtual Node Manipulation & Transform Gizmo
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

                    // Floating Left Expand Button (Appears when left sidebar is collapsed)
                    if (session.isSidebarCollapsed && session.selectedMode != EditorMode.SETTINGS && session.selectedMode != EditorMode.I18N) {
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

                    // Floating Right Expand Button (Appears when right inspector is collapsed)
                    if (session.isInspectorCollapsed && session.selectedMode != EditorMode.SETTINGS && session.selectedMode != EditorMode.I18N) {
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

                // C. Right Inspector & Resource Panel (Zero-Gap Layout with Left Overlay Resize Seam)
                if (!session.isInspectorCollapsed && session.selectedMode != EditorMode.SETTINGS && session.selectedMode != EditorMode.I18N) {
                    Box(
                        modifier = Modifier
                            .width(session.inspectorWidth)
                            .fillMaxHeight()
                    ) {
                        // Right Inspector Content
                        EditorRightInspector(
                            docState = docState,
                            selectedComponent = session.selectedComponent,
                            isComponentMode = session.selectedMode == EditorMode.COMPONENTS,
                            onCollapse = { session.isInspectorCollapsed = true }
                        )

                        // Interactive Seamless Left Edge Drag Seam
                        Box(
                            modifier = Modifier
                                .anchor(LayoutPreset.LEFT_WIDE)
                                .width(12f)
                                .cursor(arc.Graphics.Cursor.SystemCursor.horizontalResize)
                                .hoverable { isHoveringRightHandle = it }
                                .onPointerDown { isDraggingRightHandle = true }
                                .onPointerUp { isDraggingRightHandle = false }
                                .onPointerDrag { event ->
                                    val screenWidth = Core.graphics.width.toFloat()
                                    session.inspectorWidth = (screenWidth - event.x).coerceIn(160f, 900f)
                                }
                        ) {
                            org.mdt.ui.components.surface.ResizeGripHandle(
                                isHovered = isHoveringRightHandle,
                                isDragging = isDraggingRightHandle
                            )
                        }
                    }
                }
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
