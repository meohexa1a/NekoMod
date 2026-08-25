@file:Suppress("FunctionName")

package org.mdt.ui.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeNode
import arc.graphics.Color
import org.mdt.ui.widgets.TextFieldNode

/**
 * ## TextFieldColors
 *
 * Color palette state configuration for [TextField].
 */
data class TextFieldColors(
    val background: Color = Color.valueOf("181926"),
    val text: Color = Color.white,
    val placeholder: Color = Color.valueOf("6e738d"),
    val border: Color = Color.valueOf("363a4f"),
    val focusBorder: Color = Color.valueOf("2563eb"),
    val cursor: Color = Color.valueOf("60a5fa"),
    val selection: Color = Color.valueOf("2563eb").a(0.45f)
) {
    companion object {
        val Default = TextFieldColors()
    }
}

/**
 * ## TextField
 *
 * Declarative, high-performance interactive text input field with animated focus glow,
 * SDF rounded borders, BMFont glyph cursor positioning, and selection highlighting.
 *
 * @param value Current input text string.
 * @param onValueChange Callback invoked whenever text content changes.
 * @param modifier Chainable [UIModifier].
 * @param placeholder Hint text displayed when [value] is empty.
 * @param enabled Whether input is active and interactable.
 * @param colors Color state palette ([TextFieldColors]).
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

            node.visuals.fillColor.set(colors.background)
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
            set(onValueChange) { this.onValueChange = it }
            set(placeholder) { this.placeholder = it; invalidateLayout() }
            set(enabled) {
                this.touchable = it
                this.isFocusable = it
            }
            set(modifier) {
                it.applyTo(this)
                invalidateLayout()
            }
        }
    )
}
