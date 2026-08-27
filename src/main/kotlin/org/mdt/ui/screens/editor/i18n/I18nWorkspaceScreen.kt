package org.mdt.ui.screens.editor.i18n

import androidx.compose.runtime.*
import arc.Core
import org.mdt.core.engine.EngineContext
import org.mdt.core.engine.i18n.LocaleMetadata
import org.mdt.core.ui.compose.*
import org.mdt.core.ui.graphics.Color
import org.mdt.core.ui.layout.Alignment
import org.mdt.core.ui.layout.Arrangement
import org.mdt.core.ui.layout.LayoutPreset
import org.mdt.ui.components.display.Image
import org.mdt.ui.components.input.SegmentedControl
import org.mdt.ui.components.input.textfield.TextField
import org.mdt.ui.components.layout.Box
import org.mdt.ui.components.layout.Column
import org.mdt.ui.components.layout.Row
import org.mdt.ui.components.layout.Spacer
import org.mdt.ui.components.scroll.ScrollView
import org.mdt.ui.components.surface.Button
import org.mdt.ui.components.surface.ButtonVariant
import org.mdt.ui.components.surface.Divider
import org.mdt.ui.components.text.MonoText
import org.mdt.ui.components.text.Text
import org.mdt.ui.screens.editor.components.EditorSplitPane
import org.mdt.ui.theme.StudioIcons
import org.mdt.ui.theme.Theme

/**
 * ## I18nWorkspaceScreen
 *
 * Professional 2-Column Full-Screen Internationalization & Localization Studio:
 * - Left: Language selector deck with live translation coverage progress bars and filter pills.
 * - Right: High-performance interactive translation matrix with inline textfields, missing key tracking,
 *   `.properties` import/export, and one-click AI translation assistant.
 *
 * See: docs/design-system/design_system_en.md
 */
@Composable
fun I18nWorkspaceScreen(
    modifier: UIModifier = UIModifier
) {
    val colors = Theme.colors
    val shapes = Theme.shapes
    val spacing = Theme.spacing
    val typography = Theme.typography

    val i18n = EngineContext.default.i18n
    val locales = remember { LocaleMetadata.DEFAULT_LOCALES }
    var selectedLocale by remember { mutableStateOf(locales.find { it.code == "vi" } ?: locales.first()) }
    var selectedFilterIndex by remember { mutableStateOf(0) } // 0: All, 1: Missing, 2: Auto-Tracked
    var searchQuery by remember { mutableStateOf("") }
    var showAiModal by remember { mutableStateOf(false) }
    var refreshTrigger by remember { mutableStateOf(0) }

    var sidebarWidth by remember { mutableStateOf(280f) }
    var isHoveringHandle by remember { mutableStateOf(false) }
    var isDraggingHandle by remember { mutableStateOf(false) }

    // Re-query keys & bundles when refreshed
    val allKeys = remember(refreshTrigger) { i18n.getAllKeys().sorted() }
    val currentBundle = remember(selectedLocale, refreshTrigger) { i18n.getBundle(selectedLocale.code) }
    val enBundle = remember(refreshTrigger) { i18n.getBundle("en") }
    val currentStats = remember(selectedLocale, refreshTrigger) { i18n.getLocaleStats(selectedLocale.code) }

    // Filter keys according to search and category
    val displayedKeys = remember(allKeys, currentBundle, searchQuery, selectedFilterIndex) {
        allKeys.filter { key ->
            val enVal = enBundle[key] ?: ""
            val locVal = currentBundle[key] ?: ""
            val matchesSearch = searchQuery.isBlank() ||
                key.contains(searchQuery, ignoreCase = true) ||
                enVal.contains(searchQuery, ignoreCase = true) ||
                locVal.contains(searchQuery, ignoreCase = true)

            if (!matchesSearch) return@filter false

            when (selectedFilterIndex) {
                1 -> locVal.isBlank() // Missing keys only
                2 -> i18n.getMissingKeys(selectedLocale.code).any { it.key == key }
                else -> true
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.canvasVoid)
    ) {
        EditorSplitPane(
            initialSplit = 280f,
            minSplit = 200f,
            maxSplit = 600f,
            startPanel = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(colors.surfacePrimary)
                    .border(1f, colors.borderHairline)
                    .pad(spacing.md)
            ) {
                Column(
                    arrangement = Arrangement.spacedBy(spacing.sm),
                    alignment = Alignment.TopStart,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Header
                    Row(
                        arrangement = Arrangement.spacedBy(spacing.sm),
                        alignment = Alignment.CenterStart,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Image(
                            source = StudioIcons.I18N,
                            modifier = Modifier.size(18f),
                            tint = colors.blue
                        )
                        Text(
                            text = "Languages",
                            font = typography.title,
                            color = colors.textPrimary
                        )
                        Spacer(modifier = Modifier.weight(1.0f))
                        MonoText(
                            text = "${locales.size}",
                            color = colors.textSecondary
                        )
                    }

                    Divider(modifier = Modifier.fillMaxWidth().height(1f))

                    // Language List
                    ScrollView(
                        modifier = Modifier.weight(1.0f).fillMaxWidth(),
                        enableVertical = true
                    ) {
                        Column(
                            arrangement = Arrangement.spacedBy(spacing.xs + 2f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            for (locale in locales) {
                                val isSelected = locale.code == selectedLocale.code
                                val stats = i18n.getLocaleStats(locale.code)
                                val isComplete = stats.missingKeys == 0

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .radius(shapes.sm)
                                        .background(if (isSelected) colors.surfaceElevated else colors.surfaceSecondary)
                                        .border(1f, if (isSelected) colors.blue else colors.borderHairline)
                                        .clickable { selectedLocale = locale }
                                        .pad(spacing.sm)
                                ) {
                                    Column(
                                        arrangement = Arrangement.spacedBy(4f),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            arrangement = Arrangement.spacedBy(spacing.xs),
                                            alignment = Alignment.CenterStart,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = locale.displayName,
                                                font = typography.body,
                                                color = if (isSelected) colors.textPrimary else colors.textSecondary
                                            )
                                            MonoText(
                                                text = "(${locale.code})",
                                                color = colors.textTertiary
                                            )
                                            Spacer(modifier = Modifier.weight(1.0f))
                                            MonoText(
                                                text = "${stats.coveragePercent}%",
                                                color = if (isComplete) colors.green else if (stats.coveragePercent > 50) colors.blue else colors.orange
                                            )
                                        }

                                        // Progress Bar
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(4f)
                                                .radius(2f)
                                                .background(colors.surfaceTertiary)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxHeight()
                                                    .width(220f * stats.coverageRatio)
                                                    .radius(2f)
                                                    .background(if (isComplete) colors.green else if (stats.coveragePercent > 50) colors.blue else colors.orange)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        endPanel = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pad(spacing.lg)
            ) {
                Column(
                    arrangement = Arrangement.spacedBy(spacing.md),
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Header Actions Row
                    Row(
                        arrangement = Arrangement.spacedBy(spacing.md),
                        alignment = Alignment.CenterStart,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(arrangement = Arrangement.spacedBy(2f)) {
                            Text(
                                text = "${selectedLocale.displayName} (${selectedLocale.nativeName})",
                                font = typography.title,
                                color = colors.textPrimary
                            )
                            Row(arrangement = Arrangement.spacedBy(spacing.xs), alignment = Alignment.CenterStart) {
                                MonoText(
                                    text = "${currentStats.translatedKeys}/${currentStats.totalKeys} translated",
                                    color = colors.textSecondary
                                )
                                MonoText(
                                    text = "- ${currentStats.coveragePercent}% complete",
                                    color = if (currentStats.missingKeys == 0) colors.green else colors.orange
                                )
                            }
                        }

                        Spacer(modifier = Modifier.weight(1.0f))

                        // AI Assistant Button
                        Button(
                            text = "AI Translation Assistant",
                            icon = StudioIcons.STUDIO_LOGO,
                            variant = ButtonVariant.FILLED,
                            onClick = { showAiModal = true }
                        )

                        // Export .properties Button
                        Button(
                            text = "Export .properties",
                            icon = StudioIcons.FILE,
                            variant = ButtonVariant.PLAIN,
                            onClick = {
                                val content = i18n.exportProperties(selectedLocale.code)
                                try {
                                    Core.app?.setClipboardText(content)
                                } catch (_: Throwable) {}
                            }
                        )
                    }

                    Divider(modifier = Modifier.fillMaxWidth().height(1f))

                    // Filters & Search Row
                    Row(
                        arrangement = Arrangement.spacedBy(spacing.md),
                        alignment = Alignment.CenterStart,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        SegmentedControl(
                            items = listOf("All Keys (${allKeys.size})", "Missing (${currentStats.missingKeys})", "Auto-Tracked"),
                            selectedIndex = selectedFilterIndex,
                            onSelect = { selectedFilterIndex = it }
                        )

                        Spacer(modifier = Modifier.weight(1.0f))

                        // Search Input
                        Box(
                            modifier = Modifier
                                .width(280f)
                                .radius(shapes.xs)
                                .background(colors.surfacePrimary)
                                .border(1f, colors.borderHairline)
                                .pad(horizontal = spacing.sm, vertical = 2f)
                        ) {
                            TextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = "Search keys or text...",
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    // Translation Table Container
                    Box(
                        modifier = Modifier
                            .weight(1.0f)
                            .fillMaxWidth()
                            .radius(shapes.md)
                            .background(colors.surfacePrimary)
                            .border(1f, colors.borderHairline)
                            .pad(spacing.md)
                    ) {
                        Column(modifier = Modifier.fillMaxSize()) {
                            // Table Column Headers
                            Row(
                                arrangement = Arrangement.spacedBy(spacing.md),
                                alignment = Alignment.CenterStart,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .pad(horizontal = spacing.sm, vertical = spacing.xs)
                            ) {
                                MonoText(text = "KEY IDENTIFIER", color = colors.textTertiary, modifier = Modifier.width(220f))
                                Text(text = "ENGLISH REFERENCE", color = colors.textTertiary, modifier = Modifier.width(240f))
                                Text(text = "TRANSLATION VALUE (${selectedLocale.code.uppercase()})", color = colors.textTertiary, modifier = Modifier.weight(1.0f))
                            }

                            Divider(modifier = Modifier.fillMaxWidth().height(1f).margin(vertical = spacing.xs))

                            // Scrollable Matrix Rows
                            ScrollView(
                                modifier = Modifier.weight(1.0f).fillMaxWidth(),
                                enableVertical = true
                            ) {
                                Column(
                                    arrangement = Arrangement.spacedBy(spacing.xs),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    if (displayedKeys.isEmpty()) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .pad(spacing.xl)
                                        ) {
                                            Text(
                                                text = "No translation keys match the active filters.",
                                                color = colors.textTertiary,
                                                modifier = Modifier.align(Alignment.Center)
                                            )
                                        }
                                    }

                                    for (key in displayedKeys) {
                                        val enVal = enBundle[key] ?: key
                                        val rawTranslation = currentBundle[key] ?: ""
                                        val isMissing = rawTranslation.isBlank()

                                        Row(
                                            arrangement = Arrangement.spacedBy(spacing.md),
                                            alignment = Alignment.CenterStart,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .radius(shapes.xs)
                                                .background(if (isMissing) colors.surfaceElevated else colors.surfaceSecondary)
                                                .border(1f, if (isMissing) colors.orange.copy(a = 0.3f) else colors.borderHairline)
                                                .pad(horizontal = spacing.sm, vertical = 6f)
                                        ) {
                                            // 1. Key
                                            MonoText(
                                                text = key,
                                                color = colors.teal,
                                                modifier = Modifier.width(220f)
                                            )

                                            // 2. English Reference
                                            Text(
                                                text = enVal,
                                                color = colors.textSecondary,
                                                modifier = Modifier.width(240f)
                                            )

                                            // 3. Target Translation Inline TextField
                                            Box(
                                                modifier = Modifier
                                                    .weight(1.0f)
                                                    .radius(shapes.xs)
                                                    .background(colors.surfacePrimary)
                                                    .border(1f, if (isMissing) colors.orange else colors.borderHairline)
                                                    .pad(horizontal = spacing.xs, vertical = 2f)
                                            ) {
                                                TextField(
                                                    value = rawTranslation,
                                                    onValueChange = { newVal ->
                                                        i18n[selectedLocale.code, key] = newVal
                                                        refreshTrigger++
                                                    },
                                                    placeholder = "Type ${selectedLocale.displayName} translation...",
                                                    modifier = Modifier.fillMaxWidth()
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    )

        // =====================================================================
        // 3. AI Translation Assistant Modal Overlay
        // =====================================================================
        if (showAiModal) {
            AiTranslationModal(
                targetLocale = selectedLocale,
                onClose = { showAiModal = false },
                onApply = { map ->
                    i18n.register(selectedLocale.code, map)
                    refreshTrigger++
                }
            )
        }
    }
}
