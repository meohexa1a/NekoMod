// [AGENT INVARIANT] Synchronously update @property, @param, and @see KDocs when modifying this file.

package org.mdt.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import org.mdt.core.platform.LocalPlatformHost
import org.mdt.core.ui.modifier.UIModifier
import org.mdt.core.ui.modifier.align
import org.mdt.core.ui.modifier.fillMaxSize
import org.mdt.core.ui.modifier.fillMaxWidth
import org.mdt.core.ui.modifier.width
import org.mdt.core.ui.unit.Alignment
import org.mdt.core.ui.unit.Arrangement
import org.mdt.core.ui.unit.Color
import org.mdt.ui.components.input.TextField
import org.mdt.ui.components.layout.Box
import org.mdt.ui.components.layout.Column
import org.mdt.ui.components.layout.Row
import org.mdt.ui.components.surface.Button
import org.mdt.ui.components.surface.ButtonVariant
import org.mdt.ui.components.surface.Card
import org.mdt.ui.components.text.Text

/**
 * ## NekoApp [Root UI Application Entry]
 *
 * Root UI composable bootstrapping NekoMod's 1-Draw-Call pure GPU rendering pipeline.
 *
 * See: docs/architecture/architecture_en.md
 */
@Composable
fun NekoApp() {
    var clickCount by remember { mutableStateOf(0) }
    var blurActive by remember { mutableStateOf(false) }
    var inputText by remember { mutableStateOf("") }
    val host = LocalPlatformHost.current

    Box(
        modifier = UIModifier.fillMaxSize()
    ) {
        Card(
            modifier = UIModifier
                .align(Alignment.Center)
                .width(480.0f),
            color = Color(0.10f, 0.10f, 0.16f, 0.55f),
            radius = 16.0f,
            borderWidth = 1.0f,
            borderColor = Color(1.0f, 1.0f, 1.0f, 0.20f),
            isGlass = blurActive
        ) {
            Column(
                arrangement = Arrangement.spacedBy(14.0f),
                alignment = Alignment.TopStart,
                modifier = UIModifier.fillMaxWidth()
            ) {
                Text(
                    text = "[#85c1dc]NekoMod[] [#e5c890]v2.0[]",
                    color = Color.White
                )

                Text(
                    text = "He thong UI [#a6d189]1-Draw-Call[] sieu muot tren nen tang [#ca9ee6]Uber Shader Batcher[] & Dual-Kawase Frosted Glass.",
                    color = Color(0.85f, 0.85f, 0.90f, 1.0f),
                    wrap = true
                )

                TextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = "Nhap lenh hoac van ban o day...",
                    modifier = UIModifier.fillMaxWidth()
                )

                Row(
                    arrangement = Arrangement.spacedBy(10.0f),
                    alignment = Alignment.CenterStart
                ) {
                    Button(
                        text = when {
                            blurActive -> "Tat Blur"
                            else -> "Bat Blur"
                        },
                        onClick = {
                            blurActive = !blurActive
                            host.render.blur.isEnabled = blurActive
                        },
                        variant = when {
                            blurActive -> ButtonVariant.FILLED
                            else -> ButtonVariant.TINTED
                        }
                    )

                    Button(
                        text = "So lan bam: $clickCount",
                        onClick = { clickCount++ },
                        variant = ButtonVariant.GLASS
                    )
                }
            }
        }
    }
}
