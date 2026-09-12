package org.hubdustry.sample

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import arc.graphics.Color
import mindustry.ui.dialogs.BaseDialog
import org.hubdustry.libs.compose.Box
import org.hubdustry.libs.compose.Button
import org.hubdustry.libs.compose.Column
import org.hubdustry.libs.compose.Modifier
import org.hubdustry.libs.compose.Row
import org.hubdustry.libs.compose.Text
import org.hubdustry.libs.compose.compose
import org.hubdustry.libs.compose.modifier.anchor
import org.hubdustry.libs.compose.modifier.background
import org.hubdustry.libs.compose.modifier.fillMaxHeight
import org.hubdustry.libs.compose.modifier.fillMaxWidth
import org.hubdustry.libs.compose.modifier.heightIn
import org.hubdustry.libs.compose.modifier.padding
import org.hubdustry.libs.compose.modifier.size
import org.hubdustry.libs.compose.modifier.widthIn
import org.hubdustry.libs.layout.Alignment
import org.hubdustry.libs.layout.AnchorPreset

/**
 * Dialog trình diễn các tính năng của NekoMod v3 Compose Engine:
 * 1. Nhúng DSL tự nhiên qua Table.compose(...) không cần boilerplate.
 * 2. Phản ứng Recomposition theo Reactive State.
 * 3. Bố cục Flexbox (Row, Column) với Gap, Padding và Stretch Ratio qua Modifier.
 * 4. Định vị neo Godot-style (AnchorPreset overlay / ghost badge).
 */
class SampleComposeDialog : BaseDialog("NekoMod v3 Engine Showcase") {

    private val countState = mutableStateOf(0)

    init {
        // Nhúng Compose DSL trực tiếp vào Table, để Cell quản lý kích thước và padding
        cont.compose {
            val count by remember { countState }
            val dynamicColor = when (count % 4) {
                0 -> Color.royal
                1 -> Color.forest
                2 -> Color.coral
                else -> Color.gold
            }

            // Root Column lấp đầy ComposeView một cách tự nhiên
            Column(
                modifier = Modifier
                    .background(Color.darkGray)
                    .padding(12f),
                gap = 10f
            ) {

                // 1. Header Card với Title & Ghost Anchor Badge
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 36f)
                        .background(Color.black)
                        .padding(8f)
                ) {
                    Text(
                        text = "NekoMod v3 Layout Engine",
                        textColor = Color.sky
                    )

                    // Ghost Anchor Badge gắn góc trên-phải của Header
                    Box(
                        modifier = Modifier
                            .anchor(AnchorPreset.TOP_RIGHT)
                            .size(46f, 20f)
                            .background(Color.scarlet)
                            .padding(2f)
                    ) {
                        Text(text = "LIVE", textColor = Color.white)
                    }
                }

                // 2. Reactive Counter Card với Composable Button tương tác trực tiếp
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 44f),
                    gap = 8f
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(dynamicColor)
                            .padding(10f)
                    ) {
                        Text(
                            text = "Count: $count",
                            textColor = Color.white
                        )
                    }

                    // Nút bấm tương tác trực tiếp bên trong Compose Tree (AOSP Pointer Input)
                    Button(
                        onClick = { countState.value++ },
                        modifier = Modifier
                            .fillMaxHeight()
                            .padding(horizontal = 4f),
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
                            .padding(horizontal = 4f),
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
                            .background(Color.slate)
                            .padding(6f)
                    ) {
                        Text(text = "Flex: 1x", textColor = Color.white)
                    }
                    Box(
                        modifier = Modifier
                            .weight(2f)
                            .background(Color.navy)
                            .padding(6f)
                    ) {
                        Text(text = "Flex: 2x (Double Width)", textColor = Color.white)
                    }
                }

                // 4. Footer Info
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.CENTER),
                    gap = 8f
                ) {
                    Text(
                        text = "Zero-GC Engine * [gold]Berlin Wall Y-Down[] * AOSP Pointer Input",
                        textColor = Color.gray
                    )
                }
            }
        }.size(560f, 260f).pad(10f).row()

        addCloseButton()
    }
}
