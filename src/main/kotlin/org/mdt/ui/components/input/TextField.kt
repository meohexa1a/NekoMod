// [AGENT INVARIANT] Synchronously update @property, @param, and @see KDocs when modifying this file.

@file:Suppress("FunctionName", "unused")

package org.mdt.ui.components.input

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeNode
import arc.graphics.g2d.Font
import org.mdt.core.platform.LocalPlatformHost
import org.mdt.core.ui.compose.NodeApplier
import org.mdt.core.ui.modifier.UIModifier
import org.mdt.core.ui.modifier.align
import org.mdt.core.ui.modifier.background
import org.mdt.core.ui.modifier.border
import org.mdt.core.ui.modifier.fillMaxWidth
import org.mdt.core.ui.modifier.pad
import org.mdt.core.ui.modifier.radius
import org.mdt.core.ui.node.InputNode
import org.mdt.core.ui.unit.Alignment
import org.mdt.core.ui.unit.Color
import org.mdt.ui.components.layout.Box
import org.mdt.ui.theme.InputStyle
import org.mdt.ui.theme.LocalInputStyle
import org.mdt.ui.theme.TextFieldDefaults

/**
 * ## BasicInput
 *
 * Primitive, unstyled raw text input composable wrapping [InputNode].
 * Provides core focus, horizontal scrolling, IME bridging, and typing state.
 *
 * @param value Current text value string.
 * @param onValueChange Callback invoked on user text entry.
 * @param modifier Chainable [UIModifier].
 * @param placeholder Placeholder string shown when text is empty.
 * @param style Native input visual configuration ([InputStyle]). Defaults to [LocalInputStyle.current].
 * @param font Optional font override.
 * @param enabled Whether the text input accepts user focus and input.
 * @param isMultiline Whether multiple lines and Enter keybreaks are supported.
 *
 * @see InputStyle
 * @see LocalInputStyle
 * @see InputNode
 */
@Composable
fun BasicInput(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: UIModifier = UIModifier,
    placeholder: String = "",
    style: InputStyle? = null,
    font: Font? = null,
    enabled: Boolean = true,
    isMultiline: Boolean = false
) {
    val host = LocalPlatformHost.current
    val activeStyle = style ?: LocalInputStyle.current

    ComposeNode<InputNode, NodeApplier>(
        factory = {
            val node = InputNode(hostProvider = { host })
            node.editState.setText(value)
            node.onValueChange = onValueChange
            node.placeholder = placeholder
            node.style = activeStyle
            node.touchable = enabled
            node.isFocusable = enabled
            node.isMultiline = isMultiline
            node.font = font
            node.modifier = modifier
            node
        },
        update = {
            set(modifier) { this.modifier = it }
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
            set(activeStyle) { this.style = it }
            set(onValueChange) { this.onValueChange = it }
            set(enabled) {
                this.touchable = it
                this.isFocusable = it
            }
            set(isMultiline) {
                this.isMultiline = it
                invalidateLayout()
            }
            set(font) { this.font = it }
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
 * @param inputStyle Native input visual configuration ([InputStyle]). Defaults to [LocalInputStyle.current].
 * @param backgroundColor Fill color for the input box container.
 * @param borderColor Outline border color for the input box container.
 * @param borderWidth Outline border stroke thickness in pixels.
 * @param radius Corner radius in pixels.
 * @param font Optional font override.
 *
 * @see BasicInput
 * @see InputStyle
 */
@Composable
fun TextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: UIModifier = UIModifier,
    placeholder: String = "",
    enabled: Boolean = true,
    isMultiline: Boolean = false,
    inputStyle: InputStyle? = null,
    backgroundColor: Color = TextFieldDefaults.backgroundColor,
    borderColor: Color = TextFieldDefaults.borderColor,
    borderWidth: Float = TextFieldDefaults.borderWidth,
    radius: Float = TextFieldDefaults.radius,
    font: Font? = null
) {
    Box(
        modifier = UIModifier
            .background(backgroundColor)
            .radius(radius)
            .border(borderWidth, borderColor)
            .pad(left = 12.0f, right = 12.0f, top = 8.0f, bottom = 8.0f)
            .then(modifier)
    ) {
        BasicInput(
            value = value,
            onValueChange = onValueChange,
            placeholder = placeholder,
            style = inputStyle,
            font = font,
            enabled = enabled,
            isMultiline = isMultiline,
            modifier = UIModifier.fillMaxWidth().align(Alignment.CenterStart)
        )
    }
}


