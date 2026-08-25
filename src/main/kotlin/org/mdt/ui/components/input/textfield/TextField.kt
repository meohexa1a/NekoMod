@file:Suppress("FunctionName", "unused")

package org.mdt.ui.components.input.textfield

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeNode
import arc.graphics.Color
import org.mdt.ui.compose.NodeApplier
import org.mdt.ui.compose.UIModifier

/**
 * ## TextFieldColors
 *
 * Visual color palette state for [TextField].
 */
data class TextFieldColors(
    val background: Color = Color.valueOf("181926"),
    val text: Color = Color.valueOf("cad3f5"),
    val placeholder: Color = Color.valueOf("5b6078"),
    val border: Color = Color.valueOf("363a4f"),
    val focusBorder: Color = Color.valueOf("2563eb"),
    val cursor: Color = Color.valueOf("85c1dc"),
    val selection: Color = Color.valueOf("363a4f").a(0.8f)
) {
    companion object {
        val Default = TextFieldColors()
    }
}

/**
 * ## TextField
 *
 * Declarative single-line text input field supporting real-time editing,
 * cursor positioning, selection, and OS clipboard integration.
 *
 * @param value Current string value.
 * @param onValueChange Callback invoked when the user types or modifies text.
 * @param modifier Chainable [UIModifier].
 * @param placeholder Hint text displayed when [value] is empty.
 * @param enabled Whether this text input accepts focus and typing.
 * @param colors Styling palette ([TextFieldColors]).
 */
@Composable
fun TextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: UIModifier = UIModifier,
    placeholder: String = "",
    enabled: Boolean = true,
    colors: TextFieldColors = TextFieldColors.Default
) {
    ComposeNode<TextFieldNode, NodeApplier>(
        factory = {
            val node = TextFieldNode()
            node.editState.setText(value)
            node.placeholder = placeholder
            node.onValueChange = onValueChange
            node.touchable = enabled
            node.isFocusable = enabled

            node.boxVisuals.fillColor.set(colors.background)
            node.textColor.set(colors.text)
            node.placeholderColor.set(colors.placeholder)
            node.normalBorderColor.set(colors.border)
            node.focusBorderColor.set(colors.focusBorder)
            node.cursorColor.set(colors.cursor)
            node.selectionColor.set(colors.selection)

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
            set(placeholder) { this.placeholder = it; invalidateLayout() }
            set(onValueChange) { this.onValueChange = it }
            set(enabled) {
                this.touchable = it
                this.isFocusable = it
            }
            set(colors) {
                this.boxVisuals.fillColor.set(it.background)
                this.textColor.set(it.text)
                this.placeholderColor.set(it.placeholder)
                this.normalBorderColor.set(it.border)
                this.focusBorderColor.set(it.focusBorder)
                this.cursorColor.set(it.cursor)
                this.selectionColor.set(it.selection)
            }
            set(modifier) {
                it.applyTo(this)
                invalidateLayout()
            }
        }
    )
}
