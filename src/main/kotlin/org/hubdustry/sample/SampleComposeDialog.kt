package org.hubdustry.sample

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import arc.graphics.Color
import kotlinx.coroutines.launch
import mindustry.ui.dialogs.BaseDialog
import org.hubdustry.core.compose.foundation.rememberScrollState
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
import org.hubdustry.core.compose.primitive.Box
import org.hubdustry.core.compose.primitive.Column
import org.hubdustry.core.compose.primitive.FlowRow
import org.hubdustry.core.compose.primitive.Row
import org.hubdustry.core.compose.primitive.Text
import org.hubdustry.core.compose.view.compose
import org.hubdustry.core.graphics.RoundedCorners
import org.hubdustry.core.layout.Alignment
import org.hubdustry.core.layout.AnchorPreset
import org.hubdustry.ui.components.Button
import org.hubdustry.ui.components.Checkbox
import org.hubdustry.ui.components.TextField

// ─── Dialog Host ─────────────────────────────────────────────────────────────

/**
 * Dialog trình diễn các tính năng của NekoMod v3 Engine:
 * 1. Nhúng DSL tự nhiên qua Table.compose(...) không cần boilerplate.
 * 2. Phản ứng Recomposition theo Reactive State.
 * 3. Bố cục Flexbox (Row, Column) với Gap, Padding và Stretch Ratio qua Modifier.
 * 4. Định vị neo Godot-style (AnchorPreset overlay / ghost badge).
 * 5. Dựng hình SDF Shader cho Box: Bo góc độc lập 4 đỉnh và viền nổi (Inner Border).
 * 6. Điều khiển tương tác: Checkbox, TextField, Button.
 */
class SampleComposeDialog : BaseDialog("NekoMod v3 Engine Showcase") {

    private val countState = mutableIntStateOf(0)

    init {
        cont.compose {
            SampleShowcaseScreen(countState = countState)
        }.size(560f, 380f).pad(10f).row()

        addCloseButton()
    }
}

// ─── Main Showcase Screen ────────────────────────────────────────────────────

@Composable
private fun SampleShowcaseScreen(countState: MutableState<Int>) {
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

    Column(
        modifier = Modifier
            .background(Color.darkGray, RoundedCorners(12f))
            .clip(12f)
            .padding(12f)
            .verticalScroll(scrollState),
        gap = 10f
    ) {
        ScrollTelemetryBar(
            scrollOffset = scrollState.value.toInt(),
            maxScrollOffset = scrollState.maxValue.toInt(),
            onScrollToTop = { coroutineScope.launch { scrollState.animateScrollTo(0f) } },
            onScrollToBottom = { coroutineScope.launch { scrollState.animateScrollTo(scrollState.maxValue) } }
        )

        HeaderCard()

        ReactiveCounterCard(
            count = count,
            cardColor = dynamicColor,
            onIncrement = { countState.value++ },
            onReset = { countState.value = 0 }
        )

        FlexboxStretchCard()

        InteractiveSelectionCard(
            autoMiningEnabled = autoMiningEnabled,
            onAutoMiningChanged = { autoMiningEnabled = it }
        )

        InteractiveTextFieldCard(
            text = inputText,
            onTextChange = { inputText = it }
        )

        FlowRowTagWrapCard()

        ScrollTestCardList(onIncrement = { step -> countState.value += step })

        FooterInfoCard()
    }
}

// ─── Section 0: Scroll Telemetry Bar ─────────────────────────────────────────

@Composable
private fun ScrollTelemetryBar(
    scrollOffset: Int,
    maxScrollOffset: Int,
    onScrollToTop: () -> Unit,
    onScrollToBottom: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 36f)
            .background(Color.black, RoundedCorners(8f))
            .padding(horizontal = 10f, vertical = 6f),
        gap = 8f
    ) {
        Text(
            text = "Offset: $scrollOffset / $maxScrollOffset px",
            textColor = Color.coral,
            modifier = Modifier.weight(1f).align(Alignment.CENTER)
        )
        Button(
            onClick = onScrollToTop,
            modifier = Modifier.padding(horizontal = 2f),
            corners = RoundedCorners(6f),
            backgroundColor = Color.slate
        ) {
            Text(text = "Top ⬆", textColor = Color.white)
        }
        Button(
            onClick = onScrollToBottom,
            modifier = Modifier.padding(horizontal = 2f),
            corners = RoundedCorners(6f),
            backgroundColor = Color.slate
        ) {
            Text(text = "Bottom ⬇", textColor = Color.white)
        }
    }
}

// ─── Section 1: Header Card & Anchor Badge ───────────────────────────────────

@Composable
private fun HeaderCard() {
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
}

// ─── Section 2: Reactive Counter Card ────────────────────────────────────────

@Composable
private fun ReactiveCounterCard(
    count: Int,
    cardColor: Color,
    onIncrement: () -> Unit,
    onReset: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 44f),
        gap = 8f
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .background(cardColor, RoundedCorners(8f))
                .border(1.5f, Color.white, RoundedCorners(8f))
                .padding(10f)
        ) {
            Text(
                text = "Count: $count",
                textColor = Color.white
            )
        }

        Button(
            onClick = onIncrement,
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
            onClick = onReset,
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
}

// ─── Section 3: Flexbox Stretch Card ─────────────────────────────────────────

@Composable
private fun FlexboxStretchCard() {
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
}

// ─── Section 4: Interactive Checkbox Card ────────────────────────────────────

@Composable
private fun InteractiveSelectionCard(
    autoMiningEnabled: Boolean,
    onAutoMiningChanged: (Boolean) -> Unit
) {
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
                onCheckedChange = onAutoMiningChanged
            )
            Text(
                text = if (autoMiningEnabled) "Auto Mining: ON" else "Auto Mining: OFF",
                textColor = if (autoMiningEnabled) Color.sky else Color.white,
                modifier = Modifier.align(Alignment.CENTER)
            )
        }
    }
}

// ─── Section 5: Interactive TextField Card ───────────────────────────────────

@Composable
private fun InteractiveTextFieldCard(
    text: String,
    onTextChange: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 36f),
        gap = 8f
    ) {
        TextField(
            value = text,
            onValueChange = onTextChange,
            placeholder = "Nhập tiếng Việt Telex/VNI...",
            modifier = Modifier.weight(1f)
        )
    }
}

// ─── Section 6: FlowRow Tags Wrap Card ───────────────────────────────────────

@Composable
private fun FlowRowTagWrapCard() {
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
            val tags = remember {
                listOf(
                    "FlowRow", "Auto-Wrap", "Zero-GC", "SDF Shaders",
                    "Gestures", "Fling", "Scissor Clip", "Mindustry v3"
                )
            }
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
}

// ─── Section 7: Scroll Test Item List ────────────────────────────────────────

@Composable
private fun ScrollTestCardList(onIncrement: (Int) -> Unit) {
    for (itemIndex in 1..8) {
        val backgroundColor = if (itemIndex % 2 == 0) Color.valueOf("252739") else Color.valueOf("1b1c28")
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 42f)
                .background(backgroundColor, RoundedCorners(8f))
                .border(1f, Color.valueOf("363a4f"), RoundedCorners(8f))
                .padding(horizontal = 12f, vertical = 8f),
            gap = 8f
        ) {
            Text(
                text = "Item #$itemIndex: Scroll & Fling Test Card",
                textColor = Color.lightGray,
                modifier = Modifier.weight(1f).align(Alignment.CENTER)
            )
            Button(
                onClick = { onIncrement(itemIndex) },
                modifier = Modifier.widthIn(min = 52f),
                corners = RoundedCorners(6f),
                backgroundColor = Color.royal
            ) {
                Text(text = "+$itemIndex", textColor = Color.white)
            }
        }
    }
}

// ─── Section 8: Footer Info ──────────────────────────────────────────────────

@Composable
private fun FooterInfoCard() {
    Box(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "Zero-GC Engine * SDF Shader * Berlin Wall Y-Down * IME Reflection",
            textColor = Color.gray,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

