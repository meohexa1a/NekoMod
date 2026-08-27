package org.mdt.ui.screens.editor.canvas

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import arc.Core
import arc.Graphics.Cursor.SystemCursor
import org.mdt.core.ui.Rect
import org.mdt.core.ui.UINode
import org.mdt.core.ui.input.PointerEvent
import org.mdt.core.ui.input.ScrollEvent
import org.mdt.ui.components.layout.LayoutNode
import org.mdt.ui.components.layout.SceneNode
import org.mdt.ui.components.text.TextNode
import org.mdt.ui.screens.editor.model.GizmoHandle
import org.mdt.ui.screens.editor.state.EditorDocumentState
import org.mdt.ui.screens.editor.undo.NodeTransformStep
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * ## CanvasGizmoController
 *
 * Dedicated state machine and mathematical interaction engine for 2D Studio Viewport:
 * - Focal pan/zoom cursor-anchored transforms
 * - 8-point Gizmo handle detection, dragging, and directional resizing
 * - Real-time rectangle / frame shape drawing
 * - Node hit-testing and selection propagation
 * - Transform drag coalescing and [UndoRedoManager] step generation
 *
 * See: docs/design-system/design_system_en.md
 */
class CanvasGizmoController {
    var isDraggingCanvas by mutableStateOf(false)
    var isDrawingRect by mutableStateOf(false)
    var activeGizmoHandle by mutableStateOf(GizmoHandle.NONE)

    var lastTouchX = 0f
    var lastTouchY = 0f

    var dragStartX = 0f
    var dragStartY = 0f
    var dragCurrentX = 0f
    var dragCurrentY = 0f

    var elementStartX = 0f
    var elementStartY = 0f

    private var dragBeforeTransform: FloatArray? = null
    private val startBounds = Rect()

    fun getActiveCursor(selectedTool: String): SystemCursor {
        return when (selectedTool) {
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
    }

    fun handleScroll(
        event: ScrollEvent,
        panX: Float,
        panY: Float,
        zoomScale: Float,
        onZoomChange: (Float) -> Unit,
        onPanChange: (Float, Float) -> Unit
    ) {
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

    fun handlePointerDown(
        event: PointerEvent,
        selectedTool: String,
        zoomScale: Float,
        docState: EditorDocumentState?,
        onCreateText: (x: Float, y: Float) -> Unit
    ) {
        lastTouchX = event.x
        lastTouchY = event.y

        val rootScene = docState?.rootScene
        val selectedNode = docState?.selectedNode
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

    fun handlePointerUp(
        event: PointerEvent,
        selectedTool: String,
        zoomScale: Float,
        docState: EditorDocumentState?,
        onCreateRect: (x: Float, y: Float, width: Float, height: Float, isCard: Boolean) -> Unit
    ) {
        val rootScene = docState?.rootScene
        val selectedNode = docState?.selectedNode
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

    fun handlePointerDrag(
        event: PointerEvent,
        selectedTool: String,
        panX: Float,
        panY: Float,
        zoomScale: Float,
        docState: EditorDocumentState?,
        onPanChange: (Float, Float) -> Unit
    ) {
        val selectedNode = docState?.selectedNode

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
}
