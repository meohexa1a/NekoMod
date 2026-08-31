// [AGENT INVARIANT] Synchronously update @property, @param, and @see KDocs when modifying this file.

package org.mdt.ui.theme

import androidx.compose.runtime.compositionLocalOf
import org.mdt.core.ui.unit.Color

/**
 * ## InputStyle
 *
 * Native text editing visual configuration for [org.mdt.core.ui.node.InputNode].
 * Controls text, placeholder, cursor, selection, and IME composition colors rendered natively by the engine.
 *
 * @property textColor Primary text rendering color.
 * @property placeholderColor Placeholder hint string color.
 * @property cursorColor Blinking caret indicator color.
 * @property selectionColor Highlight selection box color.
 * @property compositionColor IME candidate composition background box color.
 * @property compositionUnderlineColor IME candidate composition underline bar color.
 *
 * @see LocalInputStyle
 * @see org.mdt.core.ui.node.InputNode
 * @see org.mdt.ui.components.input.BasicInput
 */
data class InputStyle(
    val textColor: Color = Color.White,
    val placeholderColor: Color = Color(1.0f, 1.0f, 1.0f, 0.40f),
    val cursorColor: Color = Color(0.52f, 0.75f, 0.86f, 1.0f),
    val selectionColor: Color = Color(0.52f, 0.75f, 0.86f, 0.35f),
    val compositionColor: Color = Color(0.52f, 0.75f, 0.86f, 0.20f),
    val compositionUnderlineColor: Color = Color(0.52f, 0.75f, 0.86f, 0.90f)
) {
    companion object {
        val Default = InputStyle()
    }
}

/**
 * CompositionLocal distributing the ambient [InputStyle] across the UI tree.
 */
val LocalInputStyle = compositionLocalOf { InputStyle.Default }
