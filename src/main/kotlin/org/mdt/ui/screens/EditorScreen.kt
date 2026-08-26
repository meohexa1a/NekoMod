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
import org.mdt.ui.theme.Theme

/**
 * ## EditorScreen
 *
 * Highly modular, clean-architecture Figma Studio Workspace styled with Apple Design tokens.
 * Features:
 * - Dynamic [EditorMode] tab switching from the top navbar.
 * - Collapsible & Draggable Resizable Left Sidebar ([isSidebarCollapsed], [sidebarWidth]).
 * - Live [NxmlCanvas] mounting dynamic NXML DOM trees parsed by dom4j.
 * - Dedicated Top-Level Docked [EditorToolDock] floating at the master workspace bottom.
 * - Seamless zero-gap edge resize handle with luminous highlight feedback (Rule 11).
 *
 * See: docs/design-system/design_system_en.md
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
              <Card anchor="center" width="400" pad="22" radius="16" blur="24">
                <Column gap="14">
                  <Row gap="12" align="center_start">
                    <Box width="36" height="36" radius="8" background="#0a84ff" pad="8">
                      <Image src="icon:tree" tint="#ffffff" />
                    </Box>
                    <Column gap="4">
                      <Text text="Apple HIG NXML Studio" color="#ffffff" font="mono" />
                      <Text text="Real-time GPU Gaussian Blur &amp; SDF" color="#8f8f8f" />
                    </Column>
                  </Row>
                  
                  <Box height="1" background="#ffffff14" />
                  
                  <Text text="Toan bo studio va NXML hien tai da duoc quy chuan hoa theo Apple Design System (8pt Grid, Acrylic Materials, Continuous Radii)!" color="#ebebf5b2" wrap="true" />
                  
                  <Row gap="8">
                    <Button text="Kham pha ngay" radius="8" background="#0a84ff" />
                    <Button text="NXML Code" radius="8" background="#2c3e55" />
                  </Row>
                </Column>
              </Card>
            </Scene>
            """.trimIndent()
        )
    }

    // Sidebar Resizing & Collapse States
    var sidebarWidth by remember { mutableStateOf(280f) }
    var isSidebarCollapsed by remember { mutableStateOf(false) }
    var isHoveringHandle by remember { mutableStateOf(false) }
    var isDraggingHandle by remember { mutableStateOf(false) }

    val colors = Theme.colors
    val shapes = Theme.shapes
    val spacing = Theme.spacing

    // Full-Screen Master Auto-Layout Container
    Box(modifier = Modifier.anchor(LayoutPreset.FULL_RECT).background(colors.canvasVoid)) {
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
                            selectedItem = selectedItem,
                            onSelectItem = { selectedItem = it },
                            onCollapse = { isSidebarCollapsed = true }
                        )

                        // Interactive Seamless Edge Drag Seam (Overlay on right border, width: 6px)
                        Box(
                            modifier = Modifier
                                .anchor(LayoutPreset.RIGHT_WIDE)
                                .width(6f)
                                .cursor(arc.Graphics.Cursor.SystemCursor.horizontalResize)
                                .hoverable { isHoveringHandle = it }
                                .onPointerDown { isDraggingHandle = true }
                                .onPointerUp { isDraggingHandle = false }
                                .onPointerDrag { event ->
                                    sidebarWidth = event.x.coerceIn(180f, 600f)
                                }
                        ) {
                            val isHighlighted = isHoveringHandle || isDraggingHandle

                            // Precision 1px Border (Expands to 2px luminous Apple Blue on hover/drag)
                            Box(
                                modifier = Modifier
                                    .anchor(LayoutPreset.RIGHT_WIDE)
                                    .width(if (isHighlighted) 2f else 1f)
                                    .background(if (isHighlighted) colors.blue else colors.borderHairline)
                                    .then(
                                        if (isHighlighted) Modifier.glow(colors.blue, blur = 4f) else UIModifier
                                    )
                            )
                        }
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
                                .margin(left = spacing.md, top = spacing.md)
                                .size(36f, 36f)
                                .radius(shapes.md)
                                .background(colors.surfaceElevated)
                                .border(1f, colors.borderHairline)
                                .shadow(colors.shadowKey, blur = 12f)
                                .clickable { isSidebarCollapsed = false }
                                .pad(spacing.sm)
                        ) {
                            Image(
                                region = Icon.right.region,
                                modifier = Modifier.fillMaxSize(),
                                tint = colors.blue
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
                .margin(bottom = spacing.xl)
        ) {
            EditorToolDock(
                selectedTool = selectedTool,
                onSelectTool = { selectedTool = it }
            )
        }
    }
}
