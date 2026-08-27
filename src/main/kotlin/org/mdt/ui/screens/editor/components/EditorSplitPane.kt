package org.mdt.ui.screens.editor.components

import androidx.compose.runtime.*
import arc.Graphics.Cursor.SystemCursor
import org.mdt.core.ui.compose.*
import org.mdt.core.ui.layout.LayoutPreset
import org.mdt.ui.components.layout.Box
import org.mdt.ui.components.layout.Row
import org.mdt.ui.components.surface.ResizeGripHandle

/**
 * ## EditorSplitPane
 *
 * Professional 2-panel horizontal resizable split container for Studio workspaces.
 * Encapsulates interactive pointer drag delta calculation, system cursor styling,
 * boundary clamping, and seamless glass grip handle rendering.
 *
 * @param initialSplit Initial pixel width of the left start panel.
 * @param minSplit Minimum permitted width bound.
 * @param maxSplit Maximum permitted width bound.
 * @param modifier Chainable [UIModifier].
 * @param startPanel Composable content rendered inside the resizable left panel.
 * @param endPanel Composable content rendered inside the flexible right panel.
 *
 * See: docs/design-system/design_system_en.md
 */
@Composable
fun EditorSplitPane(
    initialSplit: Float = 280f,
    minSplit: Float = 160f,
    maxSplit: Float = 600f,
    modifier: UIModifier = UIModifier,
    startPanel: @Composable () -> Unit,
    endPanel: @Composable () -> Unit
) {
    var splitWidth by remember { mutableStateOf(initialSplit) }
    var isHoveringHandle by remember { mutableStateOf(false) }
    var isDraggingHandle by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        Row(modifier = Modifier.fillMaxSize()) {
            // 1. Left Resizable Panel
            Box(
                modifier = Modifier
                    .width(splitWidth)
                    .fillMaxHeight()
                    .clip(true)
            ) {
                startPanel()

                // Resizing Grip Handle on Right Edge
                Box(
                    modifier = Modifier
                        .anchor(LayoutPreset.RIGHT_WIDE)
                        .width(12f)
                        .cursor(SystemCursor.horizontalResize)
                        .hoverable { isHoveringHandle = it }
                        .onPointerDown { isDraggingHandle = true }
                        .onPointerUp { isDraggingHandle = false }
                        .onPointerDrag { event ->
                            splitWidth = event.x.coerceIn(minSplit, maxSplit)
                        }
                ) {
                    ResizeGripHandle(
                        isHovered = isHoveringHandle,
                        isDragging = isDraggingHandle
                    )
                }
            }

            // 2. Right Flexible Panel
            Box(
                modifier = Modifier
                    .weight(1.0f)
                    .fillMaxHeight()
            ) {
                endPanel()
            }
        }
    }
}
