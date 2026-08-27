package org.mdt.ui.components.input.textfield

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeNode
import org.mdt.core.ui.compose.NodeApplier
import org.mdt.core.ui.compose.UIModifier
import org.mdt.core.ui.graphics.Color
import org.mdt.ui.theme.Theme

/**
 * ## TextFieldColors
 *
 * Visual color palette state for Apple iOS-style [TextField].
 */
data class TextFieldColors(
    val background: Color,
    val text: Color,
    val placeholder: Color,
    val border: Color,
    val focusBorder: Color,
    val cursor: Color,
    val selection: Color
)

/**
 * ## TextField
 *
 * Apple iOS-style inset Frosted Glass text field with cursor animation,
 * selection bounding box, placeholder label, and active focus glow.
 *
 * @param value Current string value.
 * @param onValueChange Callback invoked when the user types or modifies text.
 * @param modifier Chainable [UIModifier].
 * @param placeholder Hint text displayed when [value] is empty.
 * @param enabled Whether this text input accepts focus and typing.
 * @param colors Optional custom styling palette (defaults to [Theme] tokens).
 */
@Composable
fun TextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: UIModifier = UIModifier,
    placeholder: String = "",
    enabled: Boolean = true,
    colors: TextFieldColors = TextFieldColors(
        background = Theme.colors.surfacePrimary,
        text = Theme.colors.textPrimary,
        placeholder = Theme.colors.textTertiary,
        border = Theme.colors.borderHairline,
        focusBorder = Theme.colors.blue,
        cursor = Theme.colors.blue,
        selection = Theme.colors.blue.withAlpha(0.35f)
    )
) {
    ComposeNode<TextFieldNode, NodeApplier>(
        factory = {
            val node = TextFieldNode()
            node.editState.setText(value)
            node.placeholder = placeholder
            node.onValueChange = onValueChange
            node.touchable = enabled
            node.isFocusable = enabled

            node.boxVisuals.background.color = colors.background
            node.textColor = colors.text
            node.placeholderColor = colors.placeholder
            node.normalBorderColor = colors.border
            node.focusBorderColor = colors.focusBorder
            node.cursorColor = colors.cursor
            node.selectionColor = colors.selection

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
                this.boxVisuals.background.color = it.background
                this.textColor = it.text
                this.placeholderColor = it.placeholder
                this.normalBorderColor = it.border
                this.focusBorderColor = it.focusBorder
                this.cursorColor = it.cursor
                this.selectionColor = it.selection
            }
            set(modifier) {
                it.applyTo(this)
                invalidateLayout()
            }
        }
    )
}
