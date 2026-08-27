package org.mdt.ui.screens.editor.canvas

import androidx.compose.runtime.*
import org.mdt.core.ui.compose.*
import org.mdt.ui.components.display.Canvas
import org.mdt.ui.components.layout.Box
import org.mdt.ui.screens.editor.state.EditorDocumentState
import org.mdt.ui.theme.Theme

/**
 * ## EditorCenterCanvas
 *
 * Interactive 2D Studio Viewport coordinating tool interactions, focal pan/zoom math,
 * and delegating rendering to modular sub-systems:
 * - [CanvasGizmoController] for gesture math, transform logic, and undo recording
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
    val controller = remember { CanvasGizmoController() }
    val colors = Theme.colors
    val rootScene = docState?.rootScene
    val selectedNode = docState?.selectedNode

    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(true)
            .cursor(controller.getActiveCursor(selectedTool))
            .onScroll { event ->
                controller.handleScroll(event, panX, panY, zoomScale, onZoomChange, onPanChange)
            }
            .onPointerDown { event ->
                controller.handlePointerDown(event, selectedTool, zoomScale, docState, onCreateText)
            }
            .onPointerUp { event ->
                controller.handlePointerUp(event, selectedTool, zoomScale, docState, onCreateRect)
            }
            .onPointerDrag { event ->
                controller.handlePointerDrag(event, selectedTool, panX, panY, zoomScale, docState, onPanChange)
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
        if (controller.isDrawingRect) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                CanvasGizmoOverlay.drawCreationPreview(
                    controller.dragStartX,
                    controller.dragStartY,
                    controller.dragCurrentX,
                    controller.dragCurrentY,
                    selectedTool == "frame"
                )
            }
        }
    }
}
