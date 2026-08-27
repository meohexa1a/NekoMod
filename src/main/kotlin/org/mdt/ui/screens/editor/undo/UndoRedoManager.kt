package org.mdt.ui.screens.editor.undo

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.mdt.ui.screens.editor.EditorDocumentState

/**
 * ## UndoRedoManager
 *
 * Single Source of Truth for studio historical operations.
 * Manages discrete undo and redo stacks with bounded capacity to prevent memory leaks.
 *
 * See: docs/architecture/architecture_en.md
 */
class UndoRedoManager(
    private val maxSteps: Int = 50
) {
    private val undoStack = mutableListOf<HistoryStep>()
    private val redoStack = mutableListOf<HistoryStep>()

    var canUndo: Boolean by mutableStateOf(false)
        private set

    var canRedo: Boolean by mutableStateOf(false)
        private set

    /**
     * Records a new discrete operation and clears the redo stack.
     */
    fun record(step: HistoryStep) {
        undoStack.add(step)
        if (undoStack.size > maxSteps) {
            undoStack.removeAt(0)
        }
        redoStack.clear()
        updateState()
    }

    /**
     * Reverts the most recent operation.
     */
    fun undo(state: EditorDocumentState): Boolean {
        if (undoStack.isEmpty()) return false
        val step = undoStack.removeAt(undoStack.size - 1)
        step.undo(state)
        redoStack.add(step)
        updateState()
        return true
    }

    /**
     * Re-applies the most recently reverted operation.
     */
    fun redo(state: EditorDocumentState): Boolean {
        if (redoStack.isEmpty()) return false
        val step = redoStack.removeAt(redoStack.size - 1)
        step.redo(state)
        undoStack.add(step)
        updateState()
        return true
    }

    /**
     * Clears all recorded history.
     */
    fun clear() {
        undoStack.clear()
        redoStack.clear()
        updateState()
    }

    private fun updateState() {
        canUndo = undoStack.isNotEmpty()
        canRedo = redoStack.isNotEmpty()
    }
}
