package org.hubdustry.sample

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import arc.graphics.Color
import mindustry.ui.dialogs.BaseDialog
import org.hubdustry.ui.*
import org.hubdustry.core.compose.modifier.*

/**
 * Dialog trình diễn các tính năng của NekoMod v3 Engine:
 * 1. Nhúng DSL tự nhiên qua Table.compose(...) không cần boilerplate.
 * 2. Phản ứng Recomposition theo Reactive State.
 * 3. Bố cục Flexbox (Row, Column) với Gap, Padding và Stretch Ratio qua Modifier.
 * 4. Định vị neo Godot-style (AnchorPreset overlay / ghost badge).
 * 5. Dựng hình SDF Shader cho Box: Bo góc độc lập 4 đỉnh và viền nổi (Inner Border).
 * 6. Bộ đôi điều khiển tương tác: Checkbox & Switch.
 */
class SampleComposeDialog : BaseDialog("NekoMod v3 Engine Showcase") {

    private val countState = mutableStateOf(0)

    init {
        // Nhúng Compose DSL trực tiếp vào Table, để Cell quản lý kích thước và padding
        cont.compose {
            val count by remember { countState }
            var autoMiningEnabled by remember { mutableStateOf(false) }
            var soundFxEnabled by remember { mutableStateOf(true) }
            val dynamicColor = when (count % 4) {
                0 -> Color.royal
                1 -> Color.forest
                2 -> Color.coral
                else -> Color.gold
            }

            // Root Column lấp đầy ComposeView một cách tự nhiên với bo góc SDF 12px
            Column(
                modifier = Modifier
                    .background(Color.darkGray, RoundedCorners(12f))
                    .padding(12f),
                gap = 10f
            ) {

                // 1. Header Card với Title & Ghost Anchor Badge (Bo góc 8px)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 36f)
                        .background(Color.black, RoundedCorners(8f))
                        .padding(8f)
                ) {
                    Text(
                        text = "NekoMod v3 Layout & SDF Shader",
                        textColor = Color.sky
                    )

                    // Ghost Anchor Badge gắn góc trên-phải của Header (Bo góc 4px)
                    Box(
                        modifier = Modifier
                            .anchor(AnchorPreset.TOP_RIGHT)
                            .size(46f, 20f)
                            .background(Color.scarlet, RoundedCorners(4f))
                            .padding(2f)
                    ) {
                        Text(text = "LIVE", textColor = Color.white)
                    }
                }

                // 2. Reactive Counter Card với Composable Button tương tác trực tiếp & Viền SDF
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 44f),
                    gap = 8f
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(dynamicColor, RoundedCorners(8f))
                            .border(1.5f, Color.white, RoundedCorners(8f))
                            .padding(10f)
                    ) {
                        Text(
                            text = "Count: $count",
                            textColor = Color.white
                        )
                    }

                    // Nút bấm tương tác trực tiếp bên trong Compose Tree (AOSP Pointer Input + SDF Shader)
                    Button(
                        onClick = { countState.value++ },
                        modifier = Modifier
                            .fillMaxHeight()
                            .padding(horizontal = 4f),
                        corners = RoundedCorners(8f),
                        backgroundColor = Color.forest,
                        pressedColor = Color.green
                    ) {
                        Text(text = "Tap (+1)", textColor = Color.white)
                    }

                    Button(
                        onClick = { countState.value = 0 },
                        modifier = Modifier
                            .fillMaxHeight()
                            .widthIn(min = 72f)
                            .padding(horizontal = 4f)
                            .align(Alignment.CENTER),

                        corners = RoundedCorners(8f),
                        backgroundColor = Color.scarlet,
                        pressedColor = Color.crimson
                    ) {
                        Text(text = "Reset", textColor = Color.white)
                    }
                }

                // 3. Showcase: Flexbox Stretch Ratio (Phân bổ không gian tỷ lệ 1x vs 2x)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 40f),
                    gap = 8f
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(Color.slate, RoundedCorners(6f))
                            .padding(6f)
                    ) {
                        Text(text = "Flex: 1x", textColor = Color.white)
                    }
                    Box(
                        modifier = Modifier
                            .weight(2f)
                            .background(Color.navy, RoundedCorners(6f))
                            .padding(6f)
                    ) {
                        Text(text = "Flex: 2x (Double Width)", textColor = Color.white)
                    }
                }

                // 4. Showcase: Interactive Selection Controls (Checkbox & Switch)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 40f)
                        .background(Color.slate, RoundedCorners(8f))
                        .padding(8f),
                    gap = 16f
                ) {
                    // Checkbox Item
                    Row(
                        gap = 8f,
                        modifier = Modifier.weight(1f).align(Alignment.CENTER)
                    ) {
                        Checkbox(
                            checked = autoMiningEnabled,
                            onCheckedChange = { autoMiningEnabled = it }
                        )
                        Text(
                            text = if (autoMiningEnabled) "Auto Mining: ON" else "Auto Mining: OFF",
                            textColor = if (autoMiningEnabled) Color.sky else Color.white,
                            modifier = Modifier.align(Alignment.CENTER)
                        )
                    }

                    // Switch Item
                    Row(
                        gap = 8f,
                        modifier = Modifier.weight(1f).align(Alignment.CENTER)
                    ) {
                        Switch(
                            checked = soundFxEnabled,
                            onCheckedChange = { soundFxEnabled = it }
                        )
                        Text(
                            text = if (soundFxEnabled) "Sound FX: ON" else "Sound FX: OFF",
                            textColor = if (soundFxEnabled) Color.sky else Color.white,
                            modifier = Modifier.align(Alignment.CENTER)
                        )
                    }
                }

                // 5. Footer Info
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.CENTER),
                    gap = 8f
                ) {
                    Text(
                        text = "Zero-GC Engine * SDF Shader * Berlin Wall Y-Down * AOSP Pointer Input",
                        textColor = Color.gray
                    )
                }
            }
        }.size(560f, 320f).pad(10f).row()

        addCloseButton()
    }
}
