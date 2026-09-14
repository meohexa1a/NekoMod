package org.hubdustry.sample

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import arc.graphics.Color
import kotlinx.coroutines.launch
import mindustry.ui.dialogs.BaseDialog
import org.hubdustry.core.compose.modifier.Modifier
import org.hubdustry.core.compose.modifier.anchor
import org.hubdustry.core.compose.modifier.background
import org.hubdustry.core.compose.modifier.border
import org.hubdustry.core.compose.modifier.clip
import org.hubdustry.core.compose.modifier.fillMaxHeight
import org.hubdustry.core.compose.modifier.fillMaxWidth
import org.hubdustry.core.compose.modifier.heightIn
import org.hubdustry.core.compose.modifier.padding
import org.hubdustry.core.compose.modifier.size
import org.hubdustry.core.compose.modifier.verticalScroll
import org.hubdustry.core.compose.modifier.widthIn
import org.hubdustry.core.compose.primitive.FlowRow
import org.hubdustry.core.compose.foundation.rememberScrollState
import org.hubdustry.core.compose.primitive.Box
import org.hubdustry.core.compose.primitive.Column
import org.hubdustry.core.compose.primitive.Row
import org.hubdustry.core.compose.primitive.Text
import org.hubdustry.core.compose.view.compose
import org.hubdustry.core.graphics.RoundedCorners
import org.hubdustry.core.layout.Alignment
import org.hubdustry.core.layout.AnchorPreset
import org.hubdustry.ui.components.Button
import org.hubdustry.ui.components.Checkbox
import org.hubdustry.ui.components.TextField

/**
 * Dialog trình diễn các tính năng của NekoMod v3 Engine:
 * 1. Nhúng DSL tự nhiên qua Table.compose(...) không cần boilerplate.
 * 2. Phản ứng Recomposition theo Reactive State.
 * 3. Bố cục Flexbox (Row, Column) với Gap, Padding và Stretch Ratio qua Modifier.
 * 4. Định vị neo Godot-style (AnchorPreset overlay / ghost badge).
 * 5. Dựng hình SDF Shader cho Box: Bo góc độc lập 4 đỉnh và viền nổi (Inner Border).
 * 6. Điều khiển tương tác: Checkbox.
 */
class SampleComposeDialog : BaseDialog("NekoMod v3 Engine Showcase") {

    private val countState = mutableStateOf(0)

    init {
        // Nhúng Compose DSL trực tiếp vào Table, để Cell quản lý kích thước và padding
        cont.compose {
            val count by remember { countState }
            val scrollState = rememberScrollState()
            val coroutineScope = rememberCoroutineScope()
            var autoMiningEnabled by remember { mutableStateOf(false) }
            var inputText by remember { mutableStateOf("") }
            val dynamicColor = when (count % 4) {
                0 -> Color.royal
                1 -> Color.forest
                2 -> Color.coral
                else -> Color.gold
            }

            // Root Column lấp đầy ComposeView với bo góc SDF và cuộn dọc mượt mà (Smooth Vertical Scroll)
            Column(
                modifier = Modifier
                    .background(Color.darkGray, RoundedCorners(12f))
                    .clip(12f)
                    .padding(12f)
                    .verticalScroll(scrollState),
                gap = 10f
            ) {
                // 0. Live Scroll Telemetry Bar: Theo dõi trạng thái cuộn trực quan
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 36f)
                        .background(Color.black, RoundedCorners(8f))
                        .padding(horizontal = 10f, vertical = 6f),
                    gap = 8f
                ) {
                    Text(
                        text = "Offset: ${scrollState.value.toInt()} / ${scrollState.maxValue.toInt()} px",
                        textColor = Color.coral,
                        modifier = Modifier.weight(1f).align(Alignment.CENTER)
                    )
                    Button(
                        onClick = { coroutineScope.launch { scrollState.animateScrollTo(0f) } },
                        modifier = Modifier.padding(horizontal = 2f),
                        corners = RoundedCorners(6f),
                        backgroundColor = Color.slate
                    ) {
                        Text(text = "Top ⬆", textColor = Color.white)
                    }
                    Button(
                        onClick = { coroutineScope.launch { scrollState.animateScrollTo(scrollState.maxValue) } },
                        modifier = Modifier.padding(horizontal = 2f),
                        corners = RoundedCorners(6f),
                        backgroundColor = Color.slate
                    ) {
                        Text(text = "Bottom ⬇", textColor = Color.white)
                    }
                }

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

                // 4. Showcase: Interactive Selection Controls (Checkbox)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 40f)
                        .background(Color.slate, RoundedCorners(8f))
                        .padding(8f),
                    gap = 16f
                ) {
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
                }

                // 5. Interactive TextField với IME Support (Tiếng Việt Telex / VNI)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 36f),
                    gap = 8f
                ) {
                    TextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = "Nhập tiếng Việt Telex/VNI...",
                        modifier = Modifier.weight(1f)
                    )
                }

                // 6. Showcase: FlowRow Tags Wrap (Tự động xuống dòng khi tràn ngang)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.valueOf("1e1e2e"), RoundedCorners(8f))
                        .padding(8f)
                ) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalGap = 6f,
                        verticalGap = 6f
                    ) {
                        val tags = remember { listOf("FlowRow", "Auto-Wrap", "Zero-GC", "SDF Shaders", "Gestures", "Fling", "Scissor Clip", "Mindustry v3") }
                        for (tag in tags) {
                            Box(
                                modifier = Modifier
                                    .background(Color.valueOf("313244"), RoundedCorners(4f))
                                    .border(1f, Color.valueOf("45475a"), RoundedCorners(4f))
                                    .padding(horizontal = 8f, vertical = 4f)
                            ) {
                                Text(text = tag, textColor = Color.valueOf("cdd6f4"))
                            }
                        }
                    }
                }

                // 7. Danh sách các thẻ tràn viền để test cuộn chuột, kéo trượt và tương tác
                for (i in 1..8) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 42f)
                            .background(if (i % 2 == 0) Color.valueOf("252739") else Color.valueOf("1b1c28"), RoundedCorners(8f))
                            .border(1f, Color.valueOf("363a4f"), RoundedCorners(8f))
                            .padding(horizontal = 12f, vertical = 8f),
                        gap = 8f
                    ) {
                        Text(
                            text = "Item #$i: Scroll & Fling Test Card",
                            textColor = Color.lightGray,
                            modifier = Modifier.weight(1f).align(Alignment.CENTER)
                        )
                        Button(
                            onClick = { countState.value += i },
                            modifier = Modifier.widthIn(min = 52f),
                            corners = RoundedCorners(6f),
                            backgroundColor = Color.royal
                        ) {
                            Text(text = "+$i", textColor = Color.white)
                        }
                    }
                }

                // 8. Footer Info
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    Text(
                        text = "Zero-GC Engine * SDF Shader * Berlin Wall Y-Down * IME Reflection",
                        textColor = Color.gray,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }.size(560f, 380f).pad(10f).row()

        addCloseButton()
    }
}
