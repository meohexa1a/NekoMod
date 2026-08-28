package org.mdt.ui

import androidx.compose.runtime.*
import org.mdt.core.ui.compose.*
import org.mdt.core.ui.graphics.Color
import org.mdt.core.ui.layout.Alignment
import org.mdt.core.ui.layout.Arrangement
import org.mdt.core.ui.render.GameBlurService
import org.mdt.ui.components.layout.Box
import org.mdt.ui.components.layout.Column
import org.mdt.ui.components.layout.Row
import org.mdt.ui.components.surface.Button
import org.mdt.ui.components.surface.ButtonVariant
import org.mdt.ui.components.surface.Card
import org.mdt.ui.components.text.Text

/**
 * ## NekoApp
 *
 * Root UI composable bootstrapping NekoMod's 1-Draw-Call pure GPU rendering pipeline.
 *
 * See: docs/architecture/architecture_en.md
 */
@Composable
fun NekoApp() {
    var clickCount by remember { mutableStateOf(0) }
    var blurActive by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Card(
            modifier = Modifier
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
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "[#85c1dc]NekoMod[] v2.0",
                    color = Color.White
                )

                Text(
                    text = "He thong UI 1-Draw-Call sieu muot tren nen tang Uber Shader Batcher.",
                    color = Color(0.85f, 0.85f, 0.90f, 1.0f),
                    wrap = true
                )

                Row(
                    arrangement = Arrangement.spacedBy(10.0f),
                    alignment = Alignment.CenterStart
                ) {
                    Button(
                        text = if (blurActive) "Tat Blur" else "Bat Blur",
                        onClick = {
                            blurActive = !blurActive
                            GameBlurService.isEnabled = blurActive
                        },
                        variant = if (blurActive) ButtonVariant.FILLED else ButtonVariant.TINTED
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
