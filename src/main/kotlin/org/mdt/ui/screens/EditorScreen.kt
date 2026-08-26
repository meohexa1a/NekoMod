package org.mdt.ui.screens

import androidx.compose.runtime.*
import arc.graphics.Color
import mindustry.gen.Icon
import org.mdt.core.nxml.NxmlCanvas
import org.mdt.core.ui.compose.*
import org.mdt.core.ui.layout.LayoutPreset
import org.mdt.ui.components.display.image.Image
import org.mdt.ui.components.layout.Box
import org.mdt.ui.components.layout.Column
import org.mdt.ui.components.layout.Row
import org.mdt.ui.screens.editor.*

/**
 * ## EditorScreen
 *
 * Highly modular, clean-architecture Figma Studio Workspace.
 * Features:
 * - Dynamic [EditorMode] tab switching from the top navbar.
 * - Collapsible & Draggable Resizable Left Sidebar ([isSidebarCollapsed], [sidebarWidth]).
 * - Live [NxmlCanvas] mounting dynamic NXML DOM trees parsed by dom4j.
 * - Dedicated Top-Level Docked [EditorToolDock] floating at the master workspace bottom.
 * - Visual Hover & Active Drag Handle with glowing accent feedback (Rule 11).
 *
 * See: docs/architecture/architecture_en.md
 */
@Composable
fun EditorScreen() {
    var selectedTool by remember { mutableStateOf("select") }
    var selectedMode by remember { mutableStateOf(EditorMode.LAYERS) }
    var selectedItem by remember { mutableStateOf("App Root Scene") }

    // Dynamic Live NXML Content String
    var activeNxml by remember {
        mutableStateOf(
            """
            <Scene name="MainScene">
              <Box anchor="center" width="380" pad="20" background="#1e1e1e" radius="16" borderwidth="1" bordercolor="#333333">
                <Column gap="14">
                  <Row gap="10" align="center_start">
                    <Box width="36" height="36" radius="8" background="#0d99ff" pad="8">
                      <Image src="icon:tree" tint="#ffffff" />
                    </Box>
                    <Column gap="2">
                      <Text text="NXML Runtime Engine" color="#ffffff" font="mono" />
                      <Text text="Active Live DOM Parser (dom4j)" color="#8f8f8f" />
                    </Column>
                  </Row>
                  
                  <Box height="1" background="#333333" fill="true" />
                  
                  <Text text="Giao dien nay dang duoc render 100% tu chuoi NXML dong thong qua dom4j va NxmlNodeBuilder!" color="#d4d4d4" wrap="true" />
                  
                  <Row gap="8">
                    <Button text="Kham pha ngay" radius="8" background="#0d99ff" />
                    <Button text="NXML Code" radius="8" background="#2c3e55" />
                  </Row>
                </Column>
              </Box>
            </Scene>
            """.trimIndent()
        )
    }

    // Sidebar Resizing & Collapse States
    var sidebarWidth by remember { mutableStateOf(280f) }
    var isSidebarCollapsed by remember { mutableStateOf(false) }
    var isHoveringHandle by remember { mutableStateOf(false) }
    var isDraggingHandle by remember { mutableStateOf(false) }

    val figmaCanvasBg = Color.valueOf("141414")
    val figmaBorder = Color.valueOf("333333")
    val figmaAccent = Color.valueOf("0d99ff")

    // Full-Screen Master Auto-Layout Container
    Box(modifier = Modifier.anchor(LayoutPreset.FULL_RECT).background(figmaCanvasBg)) {
        // =====================================================================
        // I. Master Structural Layout (TopBar + 3-Panel Body)
        // =====================================================================
        Column(modifier = Modifier.fillMaxSize()) {

            // 1. Top Navigation Bar (Segmented Mode Switcher + Actions)
            EditorTopBar(
                currentMode = selectedMode,
                onSelectMode = {
                    selectedMode = it
                    if (isSidebarCollapsed) isSidebarCollapsed = false
                },
                onPreview = {},
                onReload = {}
            )

            // 2. Main Workspace Body (Left Resizable Sidebar + Center Canvas + Right Inspector)
            Row(modifier = Modifier.weight(1.0f).fillMaxWidth()) {

                // A. Left Resizable Sidebar
                if (!isSidebarCollapsed) {
                    Box(
                        modifier = Modifier
                            .width(sidebarWidth)
                            .fillMaxHeight()
                    ) {
                        EditorLeftSidebar(
                            mode = selectedMode,
                            selectedItem = selectedItem,
                            onSelectItem = { selectedItem = it },
                            onCollapse = { isSidebarCollapsed = true }
                        )
                    }

                    // Interactive Resize Drag Handle (Hitbox Width: 8px, Visual Line: 1px-2px)
                    Box(
                        modifier = Modifier
                            .width(8f)
                            .fillMaxHeight()
                            .cursor(arc.Graphics.Cursor.SystemCursor.horizontalResize)
                            .hoverable { isHoveringHandle = it }
                            .onPointerDown { isDraggingHandle = true }
                            .onPointerUp { isDraggingHandle = false }
                            .onPointerDrag { event ->
                                sidebarWidth = event.x.coerceIn(180f, 600f)
                            }
                    ) {
                        val isHighlighted = isHoveringHandle || isDraggingHandle

                        // Visible Center Divider Line (1px dark idle -> 2px glowing blue on hover/drag)
                        Box(
                            modifier = Modifier
                                .anchor(LayoutPreset.CENTER)
                                .width(if (isHighlighted) 2f else 1f)
                                .fillMaxHeight()
                                .background(if (isHighlighted) figmaAccent else figmaBorder)
                                .then(
                                    if (isHighlighted) Modifier.glow(figmaAccent, blur = 4f) else UIModifier
                                )
                        )
                    }
                }

                // B. Center Canvas Viewport (Infinite Pan & 25px Checkerboard Grid)
                Box(
                    modifier = Modifier
                        .weight(1.0f)
                        .fillMaxHeight()
                ) {
                    EditorCenterCanvas(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // Live NXML Runtime Canvas Slot
                        NxmlCanvas(
                            nxmlContent = activeNxml,
                            modifier = Modifier.anchor(LayoutPreset.FULL_RECT)
                        )
                    }

                    // Floating Expand Button (Appears when sidebar is collapsed)
                    if (isSidebarCollapsed) {
                        Box(
                            modifier = Modifier
                                .anchor(LayoutPreset.TOP_LEFT)
                                .margin(left = 12f, top = 12f)
                                .size(36f, 36f)
                                .radius(8f)
                                .background(Color.valueOf("1e1e1e"))
                                .border(1f, figmaBorder)
                                .shadow(Color(0f, 0f, 0f, 0.4f), blur = 12f)
                                .clickable { isSidebarCollapsed = false }
                                .pad(8f)
                        ) {
                            Image(
                                region = Icon.right.region,
                                modifier = Modifier.fillMaxSize(),
                                tint = figmaAccent
                            )
                        }
                    }
                }

                // C. Right Inspector Panel
                EditorRightInspector(
                    selectedNode = selectedItem
                )
            }
        }

        // =====================================================================
        // II. Master Top-Level Floating Tool Dock (Docked at EditorScreen Bottom)
        // =====================================================================
        Box(
            modifier = Modifier
                .anchor(LayoutPreset.CENTER_BOTTOM)
                .margin(bottom = 20f)
        ) {
            EditorToolDock(
                selectedTool = selectedTool,
                onSelectTool = { selectedTool = it }
            )
        }
    }
}
