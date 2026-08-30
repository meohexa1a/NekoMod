@file:Suppress("FunctionName", "unused")

package org.mdt.ui.components.input

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeNode
import arc.graphics.g2d.Font
import org.mdt.core.ui.compose.NodeApplier
import org.mdt.core.ui.compose.UIModifier
import org.mdt.core.ui.compose.align
import org.mdt.core.ui.compose.background
import org.mdt.core.ui.compose.border
import org.mdt.core.ui.compose.fillMaxWidth
import org.mdt.core.ui.compose.pad
import org.mdt.core.ui.compose.radius
import org.mdt.core.ui.layout.Alignment
import org.mdt.core.ui.node.InputNode
import org.mdt.core.ui.unit.Color
import org.mdt.ui.components.layout.Box

/**
 * ## TextFieldColors
 *
 * Visual color palette state for declarative [TextField].
 */
data class TextFieldColors(
    val background: Color = Color(0.08f, 0.08f, 0.12f, 0.70f),
    val text: Color = Color.White,
    val placeholder: Color = Color(1.0f, 1.0f, 1.0f, 0.40f),
    val border: Color = Color(1.0f, 1.0f, 1.0f, 0.15f),
    val focusBorder: Color = Color(0.52f, 0.75f, 0.86f, 0.90f),
    val cursor: Color = Color(0.52f, 0.75f, 0.86f, 1.0f),
    val selection: Color = Color(0.52f, 0.75f, 0.86f, 0.35f)
)

/**
 * ## BasicInput
 *
 * Primitive, unstyled raw text input composable wrapping [InputNode].
 * Provides core focus, horizontal scrolling, IME bridging, and typing state.
 *
 * See: docs/components-guide/components_guide_en.md
 */
@Composable
fun BasicInput(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: UIModifier = UIModifier,
    placeholder: String = "",
    placeholderColor: Color = Color(1.0f, 1.0f, 1.0f, 0.40f),
    textColor: Color = Color.White,
    cursorColor: Color = Color(0.52f, 0.75f, 0.86f, 1.0f),
    selectionColor: Color = Color(0.52f, 0.75f, 0.86f, 0.35f),
    font: Font? = null,
    enabled: Boolean = true,
    isMultiline: Boolean = false
) {
    ComposeNode<InputNode, NodeApplier>(
        factory = {
            val node = InputNode()
            node.editState.setText(value)
            node.onValueChange = onValueChange
            node.placeholder = placeholder
            node.placeholderColor = placeholderColor
            node.touchable = enabled
            node.isFocusable = enabled
            node.isMultiline = isMultiline
            node.textColor = textColor
            node.cursorColor = cursorColor
            node.selectionColor = selectionColor
            node.font = font
            modifier.applyTo(node)
            node
        },
        update = {
            set(value) {
                if (this.editState.text != it) {
                    this.editState.setText(it)
                    invalidateLayout()
                }
            }
            set(placeholder) {
                this.placeholder = it
                invalidateLayout()
            }
            set(placeholderColor) { this.placeholderColor = it }
            set(onValueChange) { this.onValueChange = it }
            set(enabled) {
                this.touchable = it
                this.isFocusable = it
            }
            set(isMultiline) {
                this.isMultiline = it
                invalidateLayout()
            }
            set(textColor) { this.textColor = it }
            set(cursorColor) { this.cursorColor = it }
            set(selectionColor) { this.selectionColor = it }
            set(font) { this.font = it }
            set(modifier) {
                it.applyTo(this)
                invalidateLayout()
            }
        }
    )
}

/**
 * ## TextField
 *
 * Styled interactive text input field composed with container background,
 * border, padding, horizontal scrolling, and IME-safe placeholder handling.
 *
 * @param value Current string value.
 * @param onValueChange Callback invoked when the user types or modifies text.
 * @param modifier Chainable [UIModifier].
 * @param placeholder Hint text displayed when [value] and IME buffer are empty.
 * @param enabled Whether this text input accepts focus and typing.
 * @param isMultiline Whether multiple lines and Enter keybreaks are supported.
 * @param radius Corner radius.
 * @param colors Color palette.
 *
 * See: docs/components-guide/components_guide_en.md
 */
@Composable
fun TextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: UIModifier = UIModifier,
    placeholder: String = "",
    enabled: Boolean = true,
    isMultiline: Boolean = false,
    radius: Float = 8.0f,
    font: Font? = null,
    colors: TextFieldColors = TextFieldColors()
) {
    Box(
        modifier = UIModifier
            .background(colors.background)
            .radius(radius)
            .border(1.0f, colors.border)
            .pad(left = 12.0f, right = 12.0f, top = 8.0f, bottom = 8.0f)
            .then(modifier)
    ) {
        BasicInput(
            value = value,
            onValueChange = onValueChange,
            placeholder = placeholder,
            placeholderColor = colors.placeholder,
            textColor = colors.text,
            cursorColor = colors.cursor,
            selectionColor = colors.selection,
            font = font,
            enabled = enabled,
            isMultiline = isMultiline,
            modifier = UIModifier.fillMaxWidth().align(Alignment.CenterStart)
        )
    }
}
