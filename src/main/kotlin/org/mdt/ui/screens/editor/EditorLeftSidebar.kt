package org.mdt.ui.screens.editor

import androidx.compose.runtime.Composable
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
import org.mdt.ui.theme.Theme

/**
 * ## TreeItemData
 *
 * Data model for hierarchical tree items in the editor sidebar.
 */
data class TreeItemData(
    val id: String,
    val name: String,
    val type: String,
    val icon: arc.scene.style.TextureRegionDrawable,
    val iconTint: Color,
    val depth: Int = 0,
    val isExpandable: Boolean = false,
    val isExpanded: Boolean = true
)

/**
 * ## EditorLeftSidebar
 *
 * Left panel managing Pages, Hierarchical Layers, Templates, i18n, and NXML code.
 * Styled with Apple Human Interface Guidelines and acrylic materials.
 *
 * See: docs/design-system/design_system_en.md
 */
@Composable
fun EditorLeftSidebar(
    mode: EditorMode = EditorMode.LAYERS,
    selectedItem: String = "App Root Scene",
    onSelectItem: (String) -> Unit = {},
    onCollapse: () -> Unit = {}
) {
    val colors = Theme.colors
    val shapes = Theme.shapes
    val spacing = Theme.spacing
    val typography = Theme.typography

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.surfacePrimary)
    ) {
        ScrollView(
            modifier = Modifier.fillMaxSize(),
            enableVertical = true
        ) {
            Column(
                arrangement = Arrangement.spacedBy(spacing.sm),
                alignment = Alignment.CenterStart,
                modifier = Modifier.fillMaxWidth().pad(horizontal = spacing.md, vertical = spacing.sm)
            ) {
                when (mode) {
                    EditorMode.LAYERS -> {
                        // =====================================================
                        // 1. SCENES & LAYERS HIERARCHICAL TREE VIEW
                        // =====================================================
                        Row(
                            arrangement = Arrangement.spacedBy(spacing.sm),
                            alignment = Alignment.CenterStart,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(text = "Layers", color = colors.textPrimary, font = typography.title)
                            Spacer(modifier = Modifier.weight(1.0f))

                            // Add Node Button
                            Box(
                                modifier = Modifier
                                    .size(24f, 24f)
                                    .radius(shapes.xs)
                                    .clickable { }
                                    .pad(4f)
                            ) {
                                Image(
                                    region = Icon.add.region,
                                    modifier = Modifier.fillMaxSize(),
                                    tint = colors.textSecondary
                                )
                            }

                            // Collapse Sidebar Button
                            Box(
                                modifier = Modifier
                                    .size(24f, 24f)
                                    .radius(shapes.xs)
                                    .clickable { onCollapse() }
                                    .pad(4f)
                            ) {
                                Image(
                                    region = Icon.left.region,
                                    modifier = Modifier.fillMaxSize(),
                                    tint = colors.textSecondary
                                )
                            }
                        }

                        // Scene / Page Chip Selector
                        Row(
                            arrangement = Arrangement.spacedBy(spacing.xs + 2f),
                            alignment = Alignment.CenterStart,
                            modifier = Modifier
                                .fillMaxWidth()
                                .radius(shapes.sm)
                                .background(colors.surfaceSecondary)
                                .border(1f, colors.borderHairline)
                                .pad(horizontal = spacing.sm, vertical = spacing.xs + 2f)
                        ) {
                            Image(
                                region = Icon.file.region,
                                modifier = Modifier.size(14f),
                                tint = colors.blue
                            )
                            Text(text = "app.nxml : MainScene", color = colors.textPrimary, font = typography.body)
                            Spacer(modifier = Modifier.weight(1.0f))
                            Text(text = "▾", color = colors.textTertiary)
                        }

                        Divider(modifier = Modifier.fillMaxWidth().height(1f))

                        // Hierarchical Node Tree
                        val treeItems = listOf(
                            TreeItemData("root", "App Root Scene", "Scene", Icon.tree, colors.blue, depth = 0, isExpandable = true),
                            TreeItemData("header", "Header Container", "Box", Icon.box, colors.teal, depth = 1, isExpandable = true),
                            TreeItemData("logo", "Logo Brand Image", "Image", Icon.planet, colors.yellow, depth = 2),
                            TreeItemData("title", "Title Text", "Text", Icon.fileText, colors.green, depth = 2),
                            TreeItemData("body", "Main Viewport Body", "Row", Icon.box, colors.teal, depth = 1, isExpandable = true),
                            TreeItemData("card", "Hero Glass Card", "Card", Icon.layers, colors.purple, depth = 2, isExpandable = true),
                            TreeItemData("card_txt", "Hero Subtext", "Text", Icon.fileText, colors.green, depth = 3),
                            TreeItemData("btn_start", "Button (Primary)", "Button", Icon.play, colors.purple, depth = 3),
                            TreeItemData("btn_opt", "Button (Options)", "Button", Icon.settings, colors.purple, depth = 3),
                            TreeItemData("footer", "Status Dock Bar", "Box", Icon.box, colors.teal, depth = 1)
                        )

                        Column(
                            arrangement = Arrangement.spacedBy(spacing.xxs),
                            alignment = Alignment.CenterStart,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            for (item in treeItems) {
                                val isSelected = item.name == selectedItem
                                val indentPx = item.depth * 14f

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .radius(shapes.sm)
                                        .background(if (isSelected) colors.surfaceHighlight else Color.clear)
                                        .border(if (isSelected) 1f else 0f, if (isSelected) colors.blue else Color.clear)
                                        .clickable { onSelectItem(item.name) }
                                        .pad(horizontal = spacing.xs + 2f, vertical = spacing.xs + 1f)
                                ) {
                                    Row(
                                        arrangement = Arrangement.spacedBy(spacing.xs + 2f),
                                        alignment = Alignment.CenterStart,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        // Indentation spacer
                                        if (indentPx > 0f) {
                                            Spacer(modifier = Modifier.width(indentPx))
                                        }

                                        // Expand chevron or leaf bullet
                                        if (item.isExpandable) {
                                            Text(text = "▾", color = colors.textTertiary)
                                        } else {
                                            Text(text = "•", color = colors.textQuaternary)
                                        }

                                        // Node Type Icon
                                        Image(
                                            region = item.icon.region,
                                            modifier = Modifier.size(14f),
                                            tint = if (isSelected) colors.blue else item.iconTint
                                        )

                                        // Node Name
                                        Text(
                                            text = item.name,
                                            color = if (isSelected) colors.textPrimary else colors.textSecondary,
                                            font = typography.body
                                        )

                                        Spacer(modifier = Modifier.weight(1.0f))

                                        // Hover/Action: Visibility Icon
                                        Image(
                                            region = Icon.eye.region,
                                            modifier = Modifier.size(12f),
                                            tint = if (isSelected) colors.textPrimary else colors.textQuaternary
                                        )
                                    }
                                }
                            }
                        }
                    }

                    EditorMode.COMPONENTS -> {
                        // =====================================================
                        // 2. REUSABLE TEMPLATES VIEW
                        // =====================================================
                        Row(
                            arrangement = Arrangement.spacedBy(spacing.sm),
                            alignment = Alignment.CenterStart,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(text = "Templates Library", color = colors.textPrimary, font = typography.title)
                            Spacer(modifier = Modifier.weight(1.0f))

                            Box(
                                modifier = Modifier
                                    .size(24f, 24f)
                                    .radius(shapes.xs)
                                    .clickable { onCollapse() }
                                    .pad(4f)
                            ) {
                                Image(
                                    region = Icon.left.region,
                                    modifier = Modifier.fillMaxSize(),
                                    tint = colors.textSecondary
                                )
                            }
                        }

                        Divider(modifier = Modifier.fillMaxWidth().height(1f))

                        val components = listOf(
                            "Card" to "Apple Frosted Glass Container",
                            "Button" to "Interactive Action Button",
                            "Toggle" to "Smooth Sliding Boolean Switch",
                            "Slider" to "Numeric Continuous Progress Bar",
                            "Badge" to "Sleek Accent Status Pill",
                            "Tooltip" to "Top-Layer Floating Context Tip"
                        )

                        for ((name, desc) in components) {
                            val isSelected = name == selectedItem
                            Column(
                                arrangement = Arrangement.spacedBy(3f),
                                alignment = Alignment.CenterStart,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .radius(shapes.sm)
                                    .background(if (isSelected) colors.surfaceHighlight else colors.surfaceSecondary)
                                    .border(1f, if (isSelected) colors.blue else colors.borderHairline)
                                    .clickable { onSelectItem(name) }
                                    .pad(spacing.sm)
                            ) {
                                Row(
                                    arrangement = Arrangement.spacedBy(spacing.xs + 2f),
                                    alignment = Alignment.CenterStart
                                ) {
                                    Image(
                                        region = Icon.box.region,
                                        modifier = Modifier.size(14f),
                                        tint = colors.blue
                                    )
                                    MonoText(
                                        text = "<$name>",
                                        color = colors.textPrimary
                                    )
                                }
                                Text(
                                    text = desc,
                                    color = colors.textSecondary
                                )
                            }
                        }
                    }

                    EditorMode.I18N -> {
                        // =====================================================
                        // 3. I18N LOCALIZATION DICTIONARY
                        // =====================================================
                        Row(
                            arrangement = Arrangement.spacedBy(spacing.sm),
                            alignment = Alignment.CenterStart,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(text = "i18n Dictionary", color = colors.textPrimary, font = typography.title)
                            Spacer(modifier = Modifier.weight(1.0f))

                            Box(
                                modifier = Modifier
                                    .size(24f, 24f)
                                    .radius(shapes.xs)
                                    .clickable { onCollapse() }
                                    .pad(4f)
                            ) {
                                Image(
                                    region = Icon.left.region,
                                    modifier = Modifier.fillMaxSize(),
                                    tint = colors.textSecondary
                                )
                            }
                        }

                        Divider(modifier = Modifier.fillMaxWidth().height(1f))

                        val strings = listOf(
                            "app.title" to "Neko Studio",
                            "btn.preview" to "Xem trước",
                            "btn.save" to "Lưu NXML",
                            "label.zoom" to "Tỉ lệ hiển thị",
                            "dialog.confirm" to "Xác nhận"
                        )

                        for ((key, value) in strings) {
                            Column(
                                arrangement = Arrangement.spacedBy(2f),
                                alignment = Alignment.CenterStart,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .radius(shapes.sm)
                                    .background(colors.surfaceSecondary)
                                    .border(1f, colors.borderHairline)
                                    .pad(spacing.sm)
                            ) {
                                MonoText(text = "\$t($key)", color = colors.blue)
                                Text(text = value, color = colors.textPrimary, font = typography.body)
                            }
                        }
                    }

                    EditorMode.CODE -> {
                        // =====================================================
                        // 4. LIVE NXML CODE VIEW
                        // =====================================================
                        Row(
                            arrangement = Arrangement.spacedBy(spacing.sm),
                            alignment = Alignment.CenterStart,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(text = "NXML Schema", color = colors.textPrimary, font = typography.title)
                            Spacer(modifier = Modifier.weight(1.0f))

                            Box(
                                modifier = Modifier
                                    .size(24f, 24f)
                                    .radius(shapes.xs)
                                    .clickable { onCollapse() }
                                    .pad(4f)
                            ) {
                                Image(
                                    region = Icon.left.region,
                                    modifier = Modifier.fillMaxSize(),
                                    tint = colors.textSecondary
                                )
                            }
                        }

                        Divider(modifier = Modifier.fillMaxWidth().height(1f))

                        val nxmlLines = listOf(
                            "<Scene name=\"MainView\">",
                            "  <Box fill=\"true\">",
                            "    <Column gap=\"12\">",
                            "      <Text text=\"\$t(app.title)\" />",
                            "      <Button text=\"Start\" />",
                            "    </Column>",
                            "  </Box>",
                            "</Scene>"
                        )

                        for (line in nxmlLines) {
                            MonoText(text = line, color = colors.teal)
                        }
                    }
                }
            }
        }
    }
}
