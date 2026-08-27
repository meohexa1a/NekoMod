package org.mdt.ui.screens

import androidx.compose.runtime.*
import arc.Core
import arc.graphics.Color
import arc.input.KeyCode
import mindustry.gen.Icon
import org.mdt.core.ui.compose.*
import org.mdt.core.ui.layout.LayoutPreset
import org.mdt.ui.components.display.image.Image
import org.mdt.ui.components.layout.*
import org.mdt.ui.components.text.TextNode
import org.mdt.ui.screens.editor.*
import org.mdt.ui.screens.editor.canvas.ComponentCanvas
import org.mdt.ui.screens.editor.canvas.EditorCenterCanvas
import org.mdt.ui.screens.editor.model.EditorMode
import org.mdt.ui.screens.editor.state.EditorDocumentState
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
    // Top-Level Studio Mode
    var selectedMode by remember { mutableStateOf(EditorMode.SCENE) }

    // Canvas Tools & Viewport States
    var selectedTool by remember { mutableStateOf("select") }
    var panX by remember { mutableStateOf(0f) }
    var panY by remember { mutableStateOf(0f) }
    var zoomScale by remember { mutableStateOf(1.0f) }
    var selectedComponent by remember { mutableStateOf("PrimaryButton") }

    // Single Source of Truth: In-Memory Virtual Node Scene Graph
    val docState = remember { EditorDocumentState() }

    // Sidebar Resizing & Collapse States
    var sidebarWidth by remember { mutableStateOf(280f) }
    var isSidebarCollapsed by remember { mutableStateOf(false) }
    var isHoveringHandle by remember { mutableStateOf(false) }
    var isDraggingHandle by remember { mutableStateOf(false) }

    // Right Inspector Resizing & Collapse States
    var inspectorWidth by remember { mutableStateOf(280f) }
    var isInspectorCollapsed by remember { mutableStateOf(false) }
    var isHoveringInspectorHandle by remember { mutableStateOf(false) }
    var isDraggingInspectorHandle by remember { mutableStateOf(false) }

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
                        zoomScale = 1.0f
                        panX = 0f
                        panY = 0f
                        true
                    }
                    key == KeyCode.del || key == KeyCode.forwardDel -> {
                        docState.removeSelectedNode()
                        true
                    }
                    !isCtrl && key == KeyCode.v -> {
                        selectedTool = "select"
                        true
                    }
                    !isCtrl && key == KeyCode.r -> {
                        selectedTool = "rect"
                        true
                    }
                    !isCtrl && key == KeyCode.t -> {
                        selectedTool = "text"
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
                currentMode = selectedMode,
                onSelectMode = {
                    selectedMode = it
                    if (isSidebarCollapsed) isSidebarCollapsed = false
                    if (isInspectorCollapsed) isInspectorCollapsed = false
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
                if (!isSidebarCollapsed) {
                    Box(
                        modifier = Modifier
                            .width(sidebarWidth)
                            .fillMaxHeight()
                    ) {
                        // Sidebar Content
                        EditorLeftSidebar(
                            mode = selectedMode,
                            docState = docState,
                            selectedComponent = selectedComponent,
                            onSelectComponent = { selectedComponent = it },
                            onCollapse = { isSidebarCollapsed = true }
                        )

                        // Interactive Seamless Edge Drag Seam
                        Box(
                            modifier = Modifier
                                .anchor(LayoutPreset.RIGHT_WIDE)
                                .width(10f)
                                .cursor(arc.Graphics.Cursor.SystemCursor.horizontalResize)
                                .hoverable { isHoveringHandle = it }
                                .onPointerDown { isDraggingHandle = true }
                                .onPointerUp { isDraggingHandle = false }
                                .onPointerDrag { event ->
                                    sidebarWidth = event.x.coerceIn(180f, 600f)
                                }
                        ) {
                            if (isHoveringHandle || isDraggingHandle) {
                                Box(
                                    modifier = Modifier
                                        .anchor(LayoutPreset.FULL_RECT)
                                        .width(4f)
                                        .background(colors.blue)
                                )
                            }
                        }
                    }
                }

                // B. Center Viewport (ComponentCanvas in COMPONENTS Mode, EditorCenterCanvas in SCENE Mode)
                Box(
                    modifier = Modifier
                        .weight(1.0f)
                        .fillMaxHeight()
                ) {
                    if (selectedMode == EditorMode.COMPONENTS) {
                        // Dedicated Isolated Component Workspace
                        ComponentCanvas(
                            componentName = selectedComponent,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        // Full Scene Canvas with Direct Virtual Node Manipulation & Transform Gizmo
                        EditorCenterCanvas(
                            panX = panX,
                            panY = panY,
                            zoomScale = zoomScale,
                            selectedTool = selectedTool,
                            docState = docState,
                            onPanChange = { px, py ->
                                panX = px
                                panY = py
                            },
                            onZoomChange = { zoomScale = it },
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
                                    vis.background.color.set(if (isCard) Color.valueOf("bf5af2") else Color.valueOf("0a84ff"))
                                    vis.radii.set(8f)
                                }
                                docState.addNode(targetParent, box)
                                selectedTool = "select"
                            },
                            onCreateText = { x, y ->
                                val targetParent = (docState.selectedNode as? LayoutNode) ?: docState.rootScene
                                val text = TextNode(text = "New Text").apply {
                                    id = "text_${System.currentTimeMillis() % 100000}"
                                    name = "Text Node"
                                    anchorData.offsetLeft = x
                                    anchorData.offsetTop = y
                                    textVisuals.color = Color.white
                                }
                                docState.addNode(targetParent, text)
                                selectedTool = "select"
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    // Floating Left Expand Button (Appears when left sidebar is collapsed)
                    if (isSidebarCollapsed) {
                        Box(
                            modifier = Modifier
                                .anchor(LayoutPreset.TOP_LEFT)
                                .margin(left = spacing.md, top = spacing.md)
                                .size(36f, 36f)
                                .radius(shapes.md)
                                .background(colors.surfaceElevated)
                                .border(1f, colors.borderHairline)
                                .clickable { isSidebarCollapsed = false }
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
                    if (isInspectorCollapsed) {
                        Box(
                            modifier = Modifier
                                .anchor(LayoutPreset.TOP_RIGHT)
                                .margin(right = spacing.md, top = spacing.md)
                                .size(36f, 36f)
                                .radius(shapes.md)
                                .background(colors.surfaceElevated)
                                .border(1f, colors.borderHairline)
                                .clickable { isInspectorCollapsed = false }
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
                if (!isInspectorCollapsed) {
                    Box(
                        modifier = Modifier
                            .width(inspectorWidth)
                            .fillMaxHeight()
                    ) {
                        // Right Inspector Content
                        EditorRightInspector(
                            docState = docState,
                            selectedComponent = selectedComponent,
                            isComponentMode = selectedMode == EditorMode.COMPONENTS,
                            onCollapse = { isInspectorCollapsed = true }
                        )

                        // Interactive Seamless Left Edge Drag Seam
                        Box(
                            modifier = Modifier
                                .anchor(LayoutPreset.LEFT_WIDE)
                                .width(10f)
                                .cursor(arc.Graphics.Cursor.SystemCursor.horizontalResize)
                                .hoverable { isHoveringInspectorHandle = it }
                                .onPointerDown { isDraggingInspectorHandle = true }
                                .onPointerUp { isDraggingInspectorHandle = false }
                                .onPointerDrag { event ->
                                    val screenWidth = Core.graphics.width.toFloat()
                                    inspectorWidth = (screenWidth - event.x).coerceIn(180f, 600f)
                                }
                        ) {
                            if (isHoveringInspectorHandle || isDraggingInspectorHandle) {
                                Box(
                                    modifier = Modifier
                                        .anchor(LayoutPreset.FULL_RECT)
                                        .width(4f)
                                        .background(colors.blue)
                                )
                            }
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
                selectedComponent = selectedComponent,
                isComponentMode = selectedMode == EditorMode.COMPONENTS,
                panX = panX,
                panY = panY,
                zoom = zoomScale
            )
        }

        // =====================================================================
        // II. Master Top-Level Floating Tool Dock (Shown in Scene Assembly Mode)
        // =====================================================================
        if (selectedMode == EditorMode.SCENE) {
            Box(
                modifier = Modifier
                    .anchor(LayoutPreset.CENTER_BOTTOM)
                    .margin(bottom = 38f)
            ) {
                EditorToolDock(
                    selectedTool = selectedTool,
                    onSelectTool = { selectedTool = it }
                )
            }
        }
    }
}
