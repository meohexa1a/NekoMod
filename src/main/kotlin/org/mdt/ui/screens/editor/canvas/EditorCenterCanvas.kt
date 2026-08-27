package org.mdt.ui.screens.editor.canvas

import androidx.compose.runtime.*
import arc.Core
import arc.Graphics.Cursor.SystemCursor
import org.mdt.core.ui.Rect
import org.mdt.core.ui.UINode
import org.mdt.core.ui.compose.*
import org.mdt.ui.components.display.Canvas
import org.mdt.ui.components.layout.Box
import org.mdt.ui.components.layout.LayoutNode
import org.mdt.ui.components.layout.SceneNode
import org.mdt.ui.components.text.TextNode
import org.mdt.ui.screens.editor.model.GizmoHandle
import org.mdt.ui.screens.editor.state.EditorDocumentState
import org.mdt.ui.screens.editor.undo.NodeTransformStep
import org.mdt.ui.theme.Theme
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * ## EditorCenterCanvas
 *
 * Interactive 2D Studio Viewport coordinating tool interactions, focal pan/zoom math,
 * and delegating rendering to modular sub-systems:
 * - [CanvasCheckerboard] for infinite void grid
 * - [CanvasArtboardFrame] for Scene Artboard bounds and headers
 * - [CanvasGizmoOverlay] for 8-point transform handles and badges
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
        "rect", "frame" -> SystemCursor.crosshair
        "text" -> SystemCursor.ibeam
        else -> when (activeGizmoHandle) {
            GizmoHandle.TOP_LEFT, GizmoHandle.BOTTOM_RIGHT,
            GizmoHandle.TOP_RIGHT, GizmoHandle.BOTTOM_LEFT -> SystemCursor.crosshair
            GizmoHandle.LEFT_CENTER, GizmoHandle.RIGHT_CENTER -> SystemCursor.horizontalResize
            GizmoHandle.TOP_CENTER, GizmoHandle.BOTTOM_CENTER -> SystemCursor.verticalResize
            GizmoHandle.BODY -> SystemCursor.hand
            else -> if (isDraggingCanvas) SystemCursor.hand else SystemCursor.arrow
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
                        val worldX = (event.x - sceneOriginX) / zoomScale
                        val worldY = (sceneTopY - event.y) / zoomScale
                        onCreateText(worldX, worldY)
                    }
                    else -> {
                        val bounds = selectedNode?.bounds
                        val isRootScene = selectedNode === rootScene
                        val resizeHandle = if (bounds != null && !isRootScene) {
                            GizmoHandle.testHit(event.x, event.y, bounds)
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
                            val hit = rootScene?.hitTest(event.x, event.y)
                            val clickedNode = findActionableNode(hit, rootScene)

                            if (clickedNode != null && clickedNode !== rootScene) {
                                if (clickedNode === selectedNode) {
                                    activeGizmoHandle = GizmoHandle.BODY
                                    val safeBounds = bounds ?: Rect()
                                    startBounds.set(safeBounds)
                                    dragStartX = event.x
                                    dragStartY = event.y
                                    elementStartX = selectedNode.anchorData.offsetLeft
                                    elementStartY = selectedNode.anchorData.offsetTop
                                    dragBeforeTransform = floatArrayOf(elementStartX, elementStartY, selectedNode.width, selectedNode.height)
                                } else {
                                    docState?.selectedNodeId = clickedNode.id
                                    activeGizmoHandle = GizmoHandle.NONE
                                }
                            } else {
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

                            val worldX = (minScreenX - sceneOriginX) / zoomScale
                            val worldY = (sceneTopY - maxScreenY) / zoomScale

                            onCreateRect(worldX, worldY, finalW, finalH, selectedTool == "frame")
                        }
                    }
                    "text" -> {}
                    else -> {
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
                            val deltaX = (event.x - dragStartX) / zoomScale
                            val deltaY = (dragStartY - event.y) / zoomScale

                            selectedNode.anchorData.offsetLeft = elementStartX + deltaX
                            selectedNode.anchorData.offsetTop = elementStartY + deltaY
                            selectedNode.invalidateLayout()
                        } else if (isDraggingCanvas) {
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
        // 1. Scaled Checkerboard Grid
        Canvas(modifier = Modifier.fillMaxSize()) {
            CanvasCheckerboard.draw(panX, panY, zoomScale, colors)
        }

        // 2. Scene Artboard Frame & Virtual Node Rendering
        if (rootScene != null) {
            Canvas(modifier = Modifier.fillMaxSize()) { renderer ->
                CanvasArtboardFrame.draw(rootScene, renderer, panX, panY, zoomScale)
            }
        }

        // 3. Transform Gizmo Overlay
        if (selectedTool == "select" && selectedNode != null && selectedNode !== rootScene) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                CanvasGizmoOverlay.drawSelectionGizmo(selectedNode, zoomScale)
            }
        }

        // 4. Creation Preview Overlay
        if (isDrawingRect) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                CanvasGizmoOverlay.drawCreationPreview(dragStartX, dragStartY, dragCurrentX, dragCurrentY, selectedTool == "frame")
            }
        }
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
