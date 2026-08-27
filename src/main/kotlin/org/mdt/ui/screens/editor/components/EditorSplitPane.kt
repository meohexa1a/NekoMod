package org.mdt.ui.screens.editor.components

import androidx.compose.runtime.*
import arc.Core
import arc.Graphics.Cursor.SystemCursor
import org.mdt.core.ui.compose.*
import org.mdt.core.ui.layout.LayoutPreset
import org.mdt.ui.components.layout.Box
import org.mdt.ui.components.layout.Row
import org.mdt.ui.components.surface.ResizeGripHandle

/**
 * ## SplitSide
 *
 * Denotes which panel in [EditorSplitPane] has a fixed/resizable width.
 */
enum class SplitSide {
    /** The start (left) panel is fixed/resizable; the end (right) panel takes remaining space. */
    START,
    /** The end (right) panel is fixed/resizable; the start (left) panel takes remaining space. */
    END
}

/**
 * ## EditorSplitPane
 *
 * Professional 2-panel horizontal resizable split container for Studio workspaces.
 * Encapsulates interactive pointer drag delta calculation, system cursor styling,
 * boundary clamping, collapse toggling, and seamless glass grip handle rendering.
 *
 * Supports both controlled 2-way state (via [splitWidth] + [onSplitWidthChange])
 * and autonomous uncontrolled internal state.
 *
 * @param splitWidth Current pixel width of the resizable panel (controlled).
 * @param onSplitWidthChange Callback when the resizable panel is dragged.
 * @param initialSplit Default width when running in uncontrolled mode.
 * @param minSplit Minimum permitted width bound.
 * @param maxSplit Maximum permitted width bound.
 * @param isCollapsed When true, hides the resizable panel entirely.
 * @param side Which panel is resizable ([SplitSide.START] for Left Sidebar, [SplitSide.END] for Right Inspector).
 * @param modifier Chainable [UIModifier].
 * @param startPanel Composable content for the start (left) panel.
 * @param endPanel Composable content for the end (right) panel.
 *
 * See: docs/design-system/design_system_en.md
 */
@Composable
fun EditorSplitPane(
    splitWidth: Float? = null,
    onSplitWidthChange: ((Float) -> Unit)? = null,
    initialSplit: Float = 280f,
    minSplit: Float = 160f,
    maxSplit: Float = 900f,
    isCollapsed: Boolean = false,
    side: SplitSide = SplitSide.START,
    modifier: UIModifier = UIModifier,
    startPanel: @Composable () -> Unit,
    endPanel: @Composable () -> Unit
) {
    var internalWidth by remember { mutableStateOf(initialSplit) }
    val currentWidth = splitWidth ?: internalWidth

    fun updateWidth(newW: Float) {
        val clamped = newW.coerceIn(minSplit, maxSplit)
        if (onSplitWidthChange != null) {
            onSplitWidthChange(clamped)
        } else {
            internalWidth = clamped
        }
    }

    var isHoveringHandle by remember { mutableStateOf(false) }
    var isDraggingHandle by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize().then(modifier)) {
        Row(modifier = Modifier.fillMaxSize()) {
            when (side) {
                SplitSide.START -> {
                    // 1. Left Resizable Panel
                    if (!isCollapsed) {
                        Box(
                            modifier = Modifier
                                .width(currentWidth)
                                .fillMaxHeight()
                                .clip(true)
                        ) {
                            startPanel()

                            // Grip Handle on Right Edge
                            Box(
                                modifier = Modifier
                                    .anchor(LayoutPreset.RIGHT_WIDE)
                                    .width(12f)
                                    .cursor(SystemCursor.horizontalResize)
                                    .hoverable { isHoveringHandle = it }
                                    .onPointerDown { isDraggingHandle = true }
                                    .onPointerUp { isDraggingHandle = false }
                                    .onPointerDrag { event ->
                                        updateWidth(event.x)
                                    }
                            ) {
                                ResizeGripHandle(
                                    isHovered = isHoveringHandle,
                                    isDragging = isDraggingHandle
                                )
                            }
                        }
                    }

                    // 2. Right Flexible Main Viewport
                    Box(
                        modifier = Modifier
                            .weight(1.0f)
                            .fillMaxHeight()
                    ) {
                        endPanel()
                    }
                }

                SplitSide.END -> {
                    // 1. Left Flexible Main Viewport
                    Box(
                        modifier = Modifier
                            .weight(1.0f)
                            .fillMaxHeight()
                    ) {
                        startPanel()
                    }

                    // 2. Right Resizable Panel
                    if (!isCollapsed) {
                        Box(
                            modifier = Modifier
                                .width(currentWidth)
                                .fillMaxHeight()
                                .clip(true)
                        ) {
                            endPanel()

                            // Grip Handle on Left Edge
                            Box(
                                modifier = Modifier
                                    .anchor(LayoutPreset.LEFT_WIDE)
                                    .width(12f)
                                    .cursor(SystemCursor.horizontalResize)
                                    .hoverable { isHoveringHandle = it }
                                    .onPointerDown { isDraggingHandle = true }
                                    .onPointerUp { isDraggingHandle = false }
                                    .onPointerDrag { event ->
                                        val screenW = Core.graphics?.width?.toFloat() ?: 1920f
                                        updateWidth(screenW - event.x)
                                    }
                            ) {
                                ResizeGripHandle(
                                    isHovered = isHoveringHandle,
                                    isDragging = isDraggingHandle
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

