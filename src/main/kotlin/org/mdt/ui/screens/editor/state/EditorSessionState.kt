package org.mdt.ui.screens.editor.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.mdt.ui.screens.editor.model.EditorMode

/**
 * ## EditorSessionState
 *
 * Consolidated reactive state model for the active Studio session.
 * Eliminates fragmented state proliferation across toolbars, sidebars, and canvases.
 *
 * See: docs/design-system/design_system_en.md
 */
class EditorSessionState {
    /** Active top bar workflow mode (SCENE, COMPONENTS, I18N, SETTINGS). */
    var selectedMode by mutableStateOf(EditorMode.SCENE)

    /** Active canvas editing tool (select, rect, text, frame). */
    var selectedTool by mutableStateOf("select")

    /** Active component template identifier in COMPONENTS mode. */
    var selectedComponent by mutableStateOf("PrimaryButton")

    // Viewport Transform
    var panX by mutableStateOf(0f)
    var panY by mutableStateOf(0f)
    var zoomScale by mutableStateOf(1.0f)

    // Left Sidebar
    var sidebarWidth by mutableStateOf(280f)
    var isSidebarCollapsed by mutableStateOf(false)

    // Right Inspector
    var inspectorWidth by mutableStateOf(280f)
    var isInspectorCollapsed by mutableStateOf(false)

    /** Reset viewport translation and scale back to origin. */
    fun resetViewport() {
        panX = 0f
        panY = 0f
        zoomScale = 1.0f
    }
}
