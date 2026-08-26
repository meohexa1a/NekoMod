package org.mdt.ui.screens.editor

import androidx.compose.runtime.*
import arc.graphics.Color
import mindustry.gen.Icon
import org.mdt.core.ui.compose.*
import org.mdt.core.ui.layout.Alignment
import org.mdt.core.ui.layout.Arrangement
import org.mdt.ui.components.display.image.Image
import org.mdt.ui.components.layout.*
import org.mdt.ui.components.scroll.ScrollView
import org.mdt.ui.components.surface.Divider
import org.mdt.ui.components.text.MonoText
import org.mdt.ui.components.text.Text

/**
 * ## EditorRightInspector
 *
 * Right panel displaying node properties, layout constraints, position coordinates,
 * typography, and appearance styling using [MonoText] for technical accuracy.
 */
@Composable
fun EditorRightInspector(
    selectedNode: String = "App Root Scene",
    posX: Float = 0f,
    posY: Float = 0f,
    width: Float = 1920f,
    height: Float = 1080f
) {
    val figmaBg = Color.valueOf("1e1e1e")
    val figmaBorder = Color.valueOf("333333")
    val figmaText = Color.valueOf("ffffff")
    val figmaMuted = Color.valueOf("949494")
    val figmaAccent = Color.valueOf("0d99ff")

    var selectedTab by remember { mutableStateOf("Design") }

    Box(
        modifier = Modifier
            .width(280f)
            .fillMaxHeight()
            .background(figmaBg)
            .border(1f, figmaBorder)
    ) {
        ScrollView(
            modifier = Modifier.fillMaxSize(),
            enableVertical = true
        ) {
            Column(
                arrangement = Arrangement.spacedBy(14f),
                alignment = Alignment.CenterStart,
                modifier = Modifier.fillMaxWidth().pad(14f)
            ) {
                // -------------------------------------------------------------
                // 1. Inspector Tabs (Design | Prototype)
                // -------------------------------------------------------------
                Row(
                    arrangement = Arrangement.spacedBy(20f),
                    alignment = Alignment.CenterStart,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Design",
                        color = if (selectedTab == "Design") figmaText else figmaMuted,
                        modifier = Modifier.clickable { selectedTab = "Design" }
                    )
                    Text(
                        text = "Prototype",
                        color = if (selectedTab == "Prototype") figmaText else figmaMuted,
                        modifier = Modifier.clickable { selectedTab = "Prototype" }
                    )
                }

                Divider(modifier = Modifier.fillMaxWidth().height(1f))

                // -------------------------------------------------------------
                // 2. Selection Info
                // -------------------------------------------------------------
                Row(
                    arrangement = Arrangement.spacedBy(8f),
                    alignment = Alignment.CenterStart,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = "Node", color = figmaMuted)
                    MonoText(text = selectedNode, color = figmaText)
                    Spacer(modifier = Modifier.weight(1.0f))
                    Image(
                        region = Icon.settings.region,
                        modifier = Modifier.size(16f),
                        tint = figmaMuted
                    )
                }

                // -------------------------------------------------------------
                // 3. Position & Geometry
                // -------------------------------------------------------------
                Column(
                    arrangement = Arrangement.spacedBy(6f),
                    alignment = Alignment.CenterStart,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = "Position", color = figmaMuted)
                    Row(
                        arrangement = Arrangement.spacedBy(12f),
                        alignment = Alignment.CenterStart
                    ) {
                        MonoText(text = "X: ${posX.toInt()}", color = figmaText)
                        MonoText(text = "Y: ${posY.toInt()}", color = figmaText)
                        MonoText(text = "∠: 0°", color = figmaText)
                    }
                }

                Divider(modifier = Modifier.fillMaxWidth().height(1f))

                // -------------------------------------------------------------
                // 4. Layout Dimensions
                // -------------------------------------------------------------
                Column(
                    arrangement = Arrangement.spacedBy(6f),
                    alignment = Alignment.CenterStart,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = "Dimensions", color = figmaMuted)
                    Row(
                        arrangement = Arrangement.spacedBy(12f),
                        alignment = Alignment.CenterStart
                    ) {
                        MonoText(text = "W: ${width.toInt()}", color = figmaText)
                        MonoText(text = "H: ${height.toInt()}", color = figmaText)
                    }
                }

                Divider(modifier = Modifier.fillMaxWidth().height(1f))

                // -------------------------------------------------------------
                // 5. Appearance & Styling
                // -------------------------------------------------------------
                Column(
                    arrangement = Arrangement.spacedBy(8f),
                    alignment = Alignment.CenterStart,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = "Appearance", color = figmaMuted)
                    Row(
                        arrangement = Arrangement.spacedBy(12f),
                        alignment = Alignment.CenterStart
                    ) {
                        MonoText(text = "Opacity: 100%", color = figmaText)
                        MonoText(text = "Radius: 16px", color = figmaText)
                    }

                    // Material / Fill Token Chip
                    Row(
                        arrangement = Arrangement.spacedBy(8f),
                        alignment = Alignment.CenterStart,
                        modifier = Modifier
                            .fillMaxWidth()
                            .radius(6f)
                            .background(Color.valueOf("25272e"))
                            .pad(horizontal = 8f, vertical = 6f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(14f, 14f)
                                .radius(4f)
                                .background(figmaAccent)
                        )
                        MonoText(text = "#0d99ff", color = figmaText)
                        Spacer(modifier = Modifier.weight(1.0f))
                        MonoText(text = "100%", color = figmaMuted)
                    }
                }
            }
        }
    }
}
