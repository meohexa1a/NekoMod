package org.mdt.ui.screens.editor

import androidx.compose.runtime.*
import arc.Core
import arc.graphics.Color
import arc.graphics.g2d.Draw
import arc.graphics.g2d.Fill
import arc.graphics.g2d.Lines
import mindustry.ui.Fonts
import org.mdt.core.ui.Rect
import org.mdt.core.ui.UINode
import org.mdt.core.ui.compose.*
import org.mdt.core.ui.layout.LayoutPreset
import org.mdt.ui.components.display.canvas.Canvas
import org.mdt.ui.components.layout.Box
import org.mdt.ui.components.layout.LayoutNode
import org.mdt.ui.components.layout.SceneNode
import org.mdt.ui.components.text.TextNode
import org.mdt.ui.screens.editor.undo.NodeTransformStep
import org.mdt.ui.theme.Theme
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * ## GizmoHandle
 *
 * 8 interactive resize handles + body handle for 2D transform manipulation.
 */
enum class GizmoHandle {
    NONE, BODY,
    TOP_LEFT, TOP_CENTER, TOP_RIGHT,
    LEFT_CENTER, RIGHT_CENTER,
    BOTTOM_LEFT, BOTTOM_CENTER, BOTTOM_RIGHT
}

/**
 * ## EditorCenterCanvas
 *
 * Interactive 2D Studio Viewport featuring:
 * 1. Infinite Canvas Zoom Engine (25% - 400%) with cursor-anchored focal math.
 * 2. 100% Direct Virtual Node Scene Graph manipulation (0-GC, 60 FPS).
 * 3. Artboard Scene Frame with header pill, custom dimensions, and device presets.
 * 4. 8-Point Transform Gizmo with live resize handles (W, H) and drag-to-move (X, Y).
 * 5. Direct Reverse-DFS Hit-Testing on live in-memory [UINode] tree.
 * 6. Multi-tool state machine (`select`, `rect`, `frame`, `text`).
 * 7. Event-driven 2D pan tracking with mouse drag ([onPointerDrag], Rule 11).
 * 8. In-Memory Drag Coalescing with [org.mdt.ui.screens.editor.undo.UndoRedoManager].
 *
 * See: docs/design-system/design_system_en.md
 */
@Composable
fun EditorCenterCanvas(
    panX: Float = 0f,
    panY: Float = 0f,
    zoomScale: Float = 1.0f,
    selectedTool: String = "select",
    docState: EditorDocumentState? = null,
    onPanChange: (Float, Float) -> Unit = { _, _ -> },
    onZoomChange: (Float) -> Unit = {},
    onCreateRect: (x: Float, y: Float, width: Float, height: Float, isCard: Boolean) -> Unit = { _, _, _, _, _ -> },
    onCreateText: (x: Float, y: Float) -> Unit = { _, _ -> },
    modifier: UIModifier = UIModifier
) {
    var isDraggingCanvas by remember { mutableStateOf(false) }
    var isDrawingRect by remember { mutableStateOf(false) }
    var activeGizmoHandle by remember { mutableStateOf(GizmoHandle.NONE) }

    var lastTouchX by remember { mutableStateOf(0f) }
    var lastTouchY by remember { mutableStateOf(0f) }

    var dragStartX by remember { mutableStateOf(0f) }
    var dragStartY by remember { mutableStateOf(0f) }
    var dragCurrentX by remember { mutableStateOf(0f) }
    var dragCurrentY by remember { mutableStateOf(0f) }

    var elementStartX by remember { mutableStateOf(0f) }
    var elementStartY by remember { mutableStateOf(0f) }

    // Drag Coalescing: captures transform values before interaction starts
    var dragBeforeTransform by remember { mutableStateOf<FloatArray?>(null) }

    val startBounds = remember { Rect() }

    val colors = Theme.colors

    val rootScene = docState?.rootScene
    val selectedNode = docState?.selectedNode

    // Determine cursor based on tool and active gizmo handle
    val activeCursor = when (selectedTool) {
        "rect", "frame" -> arc.Graphics.Cursor.SystemCursor.crosshair
        "text" -> arc.Graphics.Cursor.SystemCursor.ibeam
        else -> when (activeGizmoHandle) {
            GizmoHandle.TOP_LEFT, GizmoHandle.BOTTOM_RIGHT,
            GizmoHandle.TOP_RIGHT, GizmoHandle.BOTTOM_LEFT -> arc.Graphics.Cursor.SystemCursor.crosshair
            GizmoHandle.LEFT_CENTER, GizmoHandle.RIGHT_CENTER -> arc.Graphics.Cursor.SystemCursor.horizontalResize
            GizmoHandle.TOP_CENTER, GizmoHandle.BOTTOM_CENTER -> arc.Graphics.Cursor.SystemCursor.verticalResize
            GizmoHandle.BODY -> arc.Graphics.Cursor.SystemCursor.hand
            else -> if (isDraggingCanvas) arc.Graphics.Cursor.SystemCursor.hand else arc.Graphics.Cursor.SystemCursor.arrow
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(true)
            .cursor(activeCursor)
            .onScroll { event ->
                // Cursor-Anchored Viewport Zoom Math (25% - 400%)
                val oldScale = zoomScale
                val scrollFactor = if (event.amountY > 0f) 0.85f else 1.15f
                val newScale = (zoomScale * scrollFactor).coerceIn(0.25f, 4.0f)

                if (newScale != oldScale) {
                    val mouseX = if (Core.input != null) Core.input.mouseX().toFloat() else lastTouchX
                    val mouseY = if (Core.input != null) Core.input.mouseY().toFloat() else lastTouchY

                    val worldMouseX = (mouseX - panX) / oldScale
                    val worldMouseY = (mouseY - panY) / oldScale

                    val newPanX = mouseX - worldMouseX * newScale
                    val newPanY = mouseY - worldMouseY * newScale

                    onZoomChange(newScale)
                    onPanChange(newPanX, newPanY)
                    event.isConsumed = true
                }
            }
            .onPointerDown { event ->
                lastTouchX = event.x
                lastTouchY = event.y

                val sceneOriginX = rootScene?.bounds?.x ?: 0f
                val sceneHeight = rootScene?.bounds?.height ?: 0f
                val sceneTopY = (rootScene?.bounds?.y ?: 0f) + sceneHeight

                when (selectedTool) {
                    "rect", "frame" -> {
                        isDrawingRect = true
                        dragStartX = event.x
                        dragStartY = event.y
                        dragCurrentX = event.x
                        dragCurrentY = event.y
                    }
                    "text" -> {
                        // Click to place text at exact scene coordinates (Top-Left Origin)
                        val worldX = (event.x - sceneOriginX) / zoomScale
                        val worldY = (sceneTopY - event.y) / zoomScale
                        onCreateText(worldX, worldY)
                    }
                    else -> {
                        // 1. Check if clicking specifically on one of the 8 resize handles
                        val bounds = selectedNode?.bounds
                        val isRootScene = selectedNode === rootScene
                        val resizeHandle = if (bounds != null && !isRootScene) {
                            testResizeHandle(event.x, event.y, bounds)
                        } else {
                            GizmoHandle.NONE
                        }

                        if (resizeHandle != GizmoHandle.NONE && selectedNode != null) {
                            activeGizmoHandle = resizeHandle
                            val safeBounds = bounds ?: Rect()
                            startBounds.set(safeBounds)
                            dragStartX = event.x
                            dragStartY = event.y
                            elementStartX = selectedNode.anchorData.offsetLeft
                            elementStartY = selectedNode.anchorData.offsetTop
                            dragBeforeTransform = floatArrayOf(elementStartX, elementStartY, selectedNode.width, selectedNode.height)
                        } else {
                            // 2. Perform Reverse-DFS Hit-Testing directly on live Virtual Node tree
                            val hit = rootScene?.hitTest(event.x, event.y)
                            val clickedNode = findActionableNode(hit, rootScene)

                            if (clickedNode != null && clickedNode !== rootScene) {
                                if (clickedNode === selectedNode) {
                                    // Clicking the already-selected element's body -> Start Drag-to-Move
                                    activeGizmoHandle = GizmoHandle.BODY
                                    val safeBounds = bounds ?: Rect()
                                    startBounds.set(safeBounds)
                                    dragStartX = event.x
                                    dragStartY = event.y
                                    elementStartX = selectedNode.anchorData.offsetLeft
                                    elementStartY = selectedNode.anchorData.offsetTop
                                    dragBeforeTransform = floatArrayOf(elementStartX, elementStartY, selectedNode.width, selectedNode.height)
                                } else {
                                    // Clicked on a child or different element -> Select it!
                                    docState?.selectedNodeId = clickedNode.id
                                    activeGizmoHandle = GizmoHandle.NONE
                                }
                            } else {
                                // Clicked on empty background -> Deselect and start Canvas Pan
                                docState?.selectedNodeId = null
                                activeGizmoHandle = GizmoHandle.NONE
                                isDraggingCanvas = true
                            }
                        }
                    }
                }
            }
            .onPointerUp { event ->
                val sceneOriginX = rootScene?.bounds?.x ?: 0f
                val sceneHeight = rootScene?.bounds?.height ?: 0f
                val sceneTopY = (rootScene?.bounds?.y ?: 0f) + sceneHeight

                when (selectedTool) {
                    "rect", "frame" -> {
                        if (isDrawingRect) {
                            isDrawingRect = false
                            val rawW = abs(dragCurrentX - dragStartX) / zoomScale
                            val rawH = abs(dragCurrentY - dragStartY) / zoomScale
                            val finalW = if (rawW < 10f) 240f else rawW
                            val finalH = if (rawH < 10f) 140f else rawH

                            val minScreenX = min(dragStartX, dragCurrentX)
                            val maxScreenY = max(dragStartY, dragCurrentY)

                            // Convert to Scene Top-Left Coordinates
                            val worldX = (minScreenX - sceneOriginX) / zoomScale
                            val worldY = (sceneTopY - maxScreenY) / zoomScale

                            onCreateRect(worldX, worldY, finalW, finalH, selectedTool == "frame")
                        }
                    }
                    "text" -> {}
                    else -> {
                        // Drag Coalescing: Push 1 discrete mutation step to Undo/Redo history
                        if (dragBeforeTransform != null && selectedNode != null) {
                            val before = dragBeforeTransform!!
                            val newX = selectedNode.anchorData.offsetLeft
                            val newY = selectedNode.anchorData.offsetTop
                            val newW = selectedNode.width
                            val newH = selectedNode.height

                            if (before[0] != newX || before[1] != newY || before[2] != newW || before[3] != newH) {
                                docState?.undoRedoManager?.record(
                                    NodeTransformStep(
                                        targetId = selectedNode.id,
                                        oldX = before[0], oldY = before[1], oldW = before[2], oldH = before[3],
                                        newX = newX, newY = newY, newW = newW, newH = newH
                                    )
                                )
                            }
                            dragBeforeTransform = null
                        }

                        isDraggingCanvas = false
                        activeGizmoHandle = GizmoHandle.NONE
                    }
                }
            }
            .onPointerDrag { event ->
                when (selectedTool) {
                    "rect", "frame" -> {
                        dragCurrentX = event.x
                        dragCurrentY = event.y
                    }
                    else -> {
                        if (activeGizmoHandle != GizmoHandle.NONE && activeGizmoHandle != GizmoHandle.BODY && selectedNode != null) {
                            // --- RESIZING W, H & PINNING OPPOSITE EDGES (Top-Left Origin Math) ---
                            val deltaX = (event.x - dragStartX) / zoomScale
                            val deltaY = -((event.y - dragStartY) / zoomScale)

                            var newW = startBounds.width / zoomScale
                            var newH = startBounds.height / zoomScale
                            var newX = elementStartX
                            var newY = elementStartY

                            when (activeGizmoHandle) {
                                GizmoHandle.RIGHT_CENTER, GizmoHandle.TOP_RIGHT, GizmoHandle.BOTTOM_RIGHT -> {
                                    newW = max(20f, (startBounds.width / zoomScale) + deltaX)
                                }
                                GizmoHandle.LEFT_CENTER, GizmoHandle.TOP_LEFT, GizmoHandle.BOTTOM_LEFT -> {
                                    newW = max(20f, (startBounds.width / zoomScale) - deltaX)
                                    newX = elementStartX + deltaX
                                }
                                else -> {}
                            }

                            when (activeGizmoHandle) {
                                GizmoHandle.BOTTOM_CENTER, GizmoHandle.BOTTOM_LEFT, GizmoHandle.BOTTOM_RIGHT -> {
                                    newH = max(20f, (startBounds.height / zoomScale) + deltaY)
                                }
                                GizmoHandle.TOP_CENTER, GizmoHandle.TOP_LEFT, GizmoHandle.TOP_RIGHT -> {
                                    newH = max(20f, (startBounds.height / zoomScale) - deltaY)
                                    newY = elementStartY + deltaY
                                }
                                else -> {}
                            }

                            selectedNode.width = newW
                            selectedNode.height = newH
                            if (newX != elementStartX) selectedNode.anchorData.offsetLeft = newX
                            if (newY != elementStartY) selectedNode.anchorData.offsetTop = newY
                            selectedNode.invalidateLayout()
                        } else if (activeGizmoHandle == GizmoHandle.BODY && selectedNode != null) {
                            // --- MOVING X, Y (Top-Left Origin Math) ---
                            val deltaX = (event.x - dragStartX) / zoomScale
                            val deltaY = (dragStartY - event.y) / zoomScale

                            selectedNode.anchorData.offsetLeft = elementStartX + deltaX
                            selectedNode.anchorData.offsetTop = elementStartY + deltaY
                            selectedNode.invalidateLayout()
                        } else if (isDraggingCanvas) {
                            // --- PANNING THE CANVAS VIEWPORT ---
                            val deltaX = event.x - lastTouchX
                            val deltaY = event.y - lastTouchY
                            lastTouchX = event.x
                            lastTouchY = event.y

                            onPanChange(panX + deltaX, panY + deltaY)
                        }
                    }
                }
            }
    ) {
        // =====================================================================
        // 1. Procedural Scaled Checkerboard Background
        // =====================================================================
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cellSize = 25f * zoomScale
            val screenW = Core.graphics?.width?.toFloat() ?: 1920f
            val screenH = Core.graphics?.height?.toFloat() ?: 1080f

            val startCol = (-panX / cellSize).toInt() - 2
            val endCol = ((screenW - panX) / cellSize).toInt() + 2
            val startRow = (-panY / cellSize).toInt() - 2
            val endRow = ((screenH - panY) / cellSize).toInt() + 2

            for (row in startRow..endRow) {
                val cellY = row * cellSize + panY
                for (col in startCol..endCol) {
                    val cellX = col * cellSize + panX
                    val isEven = ((row + col) % 2 + 2) % 2 == 0
                    Draw.color(if (isEven) colors.canvasGridDark else colors.canvasGridLight)
                    Fill.rect(cellX + cellSize * 0.5f, cellY + cellSize * 0.5f, cellSize, cellSize)
                }
            }
            Draw.color(Color.white)
        }

        // =====================================================================
        // 2. Direct Virtual Node Scene Render Pass (Drawing live scene in-memory)
        // =====================================================================
        if (rootScene != null) {
            Canvas(modifier = Modifier.fillMaxSize()) { renderer ->
                val screenW = Core.graphics?.width?.toFloat() ?: 1920f
                val screenH = Core.graphics?.height?.toFloat() ?: 1080f

                val artW = rootScene.artboardWidth * zoomScale
                val artH = rootScene.artboardHeight * zoomScale

                val sceneX = (screenW - artW) * 0.5f + panX
                val sceneY = (screenH - artH) * 0.5f + panY

                // Layout and positioning for Scene Artboard
                rootScene.setBounds(sceneX, sceneY, artW, artH)
                rootScene.layout()

                // Draw Artboard Frame Background & Shadow
                Draw.color(Color(0f, 0f, 0f, 0.40f))
                Fill.rect(sceneX + artW * 0.5f, sceneY + artH * 0.5f - 4f, artW + 8f, artH + 8f)

                // Render the complete in-memory Virtual Node tree!
                rootScene.draw(renderer)

                // Artboard Header Label Pill
                val font = Fonts.def
                val headerText = "${rootScene.name} • ${rootScene.artboardWidth.toInt()} × ${rootScene.artboardHeight.toInt()} px"
                val badgeX = sceneX + 60f
                val badgeY = sceneY + artH + 16f

                Draw.color(Color(0.08f, 0.08f, 0.12f, 0.85f))
                Fill.rect(badgeX, badgeY, 140f, 20f)
                Draw.color(Color(0.2f, 0.5f, 1.0f, 0.4f))
                Lines.stroke(1f)
                Lines.rect(badgeX - 70f, badgeY - 10f, 140f, 20f)

                Draw.color(Color.white)
                font.draw(headerText, badgeX - 64f, badgeY + 4f)
                Draw.color(Color.white)
            }
        }

        // =====================================================================
        // 3. Interactive Transform Gizmo & Dimension Overlay Pass
        // =====================================================================
        if (selectedTool == "select" && selectedNode != null && selectedNode !== rootScene) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val bounds = selectedNode.bounds
                val bx = bounds.x
                val by = bounds.y
                val bw = bounds.width
                val bh = bounds.height

                if (bw > 0f && bh > 0f) {
                    val blue = Color.valueOf("0a84ff")
                    val handleSize = 8f

                    // 1. Bounding Outline
                    Draw.color(blue)
                    Lines.stroke(2f)
                    Lines.rect(bx, by, bw, bh)

                    // 2. 8-Point Transform Handles
                    val handles = listOf(
                        bx to by + bh,                 // Top-Left
                        bx + bw * 0.5f to by + bh,     // Top-Center
                        bx + bw to by + bh,            // Top-Right
                        bx to by + bh * 0.5f,          // Left-Center
                        bx + bw to by + bh * 0.5f,     // Right-Center
                        bx to by,                      // Bottom-Left
                        bx + bw * 0.5f to by,          // Bottom-Center
                        bx + bw to by                  // Bottom-Right
                    )

                    for ((hx, hy) in handles) {
                        Draw.color(Color.white)
                        Fill.rect(hx, hy, handleSize, handleSize)

                        Draw.color(blue)
                        Lines.stroke(1.5f)
                        Lines.rect(hx - handleSize * 0.5f, hy - handleSize * 0.5f, handleSize, handleSize)
                    }

                    // 3. Dimension & Position Tooltip Badge
                    val curX = selectedNode.anchorData.offsetLeft.toInt()
                    val curY = selectedNode.anchorData.offsetTop.toInt()
                    val tagLabel = "${selectedNode.name.ifEmpty { selectedNode.javaClass.simpleName }}  ${(bw / zoomScale).toInt()} × ${(bh / zoomScale).toInt()} px  (x:$curX, y:$curY)"
                    val font = Fonts.def
                    val badgeW = 240f
                    val badgeH = 22f
                    val badgeX = bx + bw * 0.5f
                    val badgeY = by - 16f

                    Draw.color(Color(0.08f, 0.08f, 0.10f, 0.85f))
                    Fill.rect(badgeX, badgeY, badgeW, badgeH)

                    Draw.color(blue)
                    Lines.stroke(1f)
                    Lines.rect(badgeX - badgeW * 0.5f, badgeY - badgeH * 0.5f, badgeW, badgeH)

                    Draw.color(Color.white)
                    font.draw(tagLabel, badgeX - badgeW * 0.46f, badgeY + 5f)
                }
                Draw.color(Color.white)
            }
        }

        // =====================================================================
        // 4. Real-time Live Bounding Box Overlay Pass (Draws creation preview)
        // =====================================================================
        if (isDrawingRect) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val left = min(dragStartX, dragCurrentX)
                val bottom = min(dragStartY, dragCurrentY)
                val w = max(4f, abs(dragCurrentX - dragStartX))
                val h = max(4f, abs(dragCurrentY - dragStartY))

                val strokeColor = if (selectedTool == "frame") Color.valueOf("bf5af2") else Color.valueOf("0a84ff")
                val fillColor = Color(strokeColor.r, strokeColor.g, strokeColor.b, 0.12f)

                Draw.color(fillColor)
                Fill.rect(left + w * 0.5f, bottom + h * 0.5f, w, h)

                Draw.color(strokeColor)
                Lines.stroke(2f)
                Lines.rect(left, bottom, w, h)
                Draw.color(Color.white)
            }
        }
    }
}

// =============================================================================
// Helper Functions for Gizmo Hit-Testing and Node Search
// =============================================================================

private fun testResizeHandle(px: Float, py: Float, bounds: Rect, hitTolerance: Float = 10f): GizmoHandle {
    val bx = bounds.x
    val by = bounds.y
    val bw = bounds.width
    val bh = bounds.height

    fun hit(hx: Float, hy: Float): Boolean =
        px in (hx - hitTolerance)..(hx + hitTolerance) && py in (hy - hitTolerance)..(hy + hitTolerance)

    return when {
        hit(bx, by + bh) -> GizmoHandle.TOP_LEFT
        hit(bx + bw * 0.5f, by + bh) -> GizmoHandle.TOP_CENTER
        hit(bx + bw, by + bh) -> GizmoHandle.TOP_RIGHT
        hit(bx, by + bh * 0.5f) -> GizmoHandle.LEFT_CENTER
        hit(bx + bw, by + bh * 0.5f) -> GizmoHandle.RIGHT_CENTER
        hit(bx, by) -> GizmoHandle.BOTTOM_LEFT
        hit(bx + bw * 0.5f, by) -> GizmoHandle.BOTTOM_CENTER
        hit(bx + bw, by) -> GizmoHandle.BOTTOM_RIGHT
        else -> GizmoHandle.NONE
    }
}

private fun findActionableNode(hitNode: UINode?, rootScene: SceneNode?): UINode? {
    var current = hitNode
    while (current != null && current !== rootScene) {
        if (current.parent === rootScene || current is LayoutNode || current is TextNode) {
            return current
        }
        current = current.parent
    }
    return (hitNode ?: rootScene)
}
