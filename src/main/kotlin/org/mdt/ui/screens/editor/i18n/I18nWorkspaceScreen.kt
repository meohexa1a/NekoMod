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

    // Re-query keys & bundles when refreshed
    val allKeys = remember(refreshTrigger) { i18n.getAllKeys().sorted() }
    val currentBundle = remember(selectedLocale, refreshTrigger) { i18n.getBundle(selectedLocale.code) }
    val enBundle = remember(refreshTrigger) { i18n.getBundle("en") }
    val currentStats = remember(selectedLocale, refreshTrigger) { i18n.getLocaleStats(selectedLocale.code) }

    // Filter keys according to search and category
    val displayedKeys = remember(allKeys, currentBundle, searchQuery, selectedFilterIndex) {
        allKeys.filter { key ->
            val englishValue = enBundle[key] ?: ""
            val localeValue = currentBundle[key] ?: ""
            val matchesSearch = searchQuery.isBlank() ||
                key.contains(searchQuery, ignoreCase = true) ||
                englishValue.contains(searchQuery, ignoreCase = true) ||
                localeValue.contains(searchQuery, ignoreCase = true)

            if (!matchesSearch) return@filter false

            when (selectedFilterIndex) {
                1 -> localeValue.isBlank() // Missing keys only
                2 -> i18n.getMissingKeys(selectedLocale.code).any { it.key == key }
                else -> true
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.canvasVoid)
            .then(modifier)
    ) {
        EditorSplitPane(
            initialSplit = 280f,
            minSplit = 200f,
            maxSplit = 600f,
            startPanel = {
                I18nLanguageSidebar(
                    locales = locales,
                    selectedLocale = selectedLocale,
                    onSelectLocale = { selectedLocale = it },
                    i18n = i18n
                )
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
                        I18nTranslationTable(
                            displayedKeys = displayedKeys,
                            selectedLocale = selectedLocale,
                            currentBundle = currentBundle,
                            enBundle = enBundle,
                            i18n = i18n,
                            onValueUpdated = { refreshTrigger++ },
                            modifier = Modifier.weight(1.0f).fillMaxWidth()
                        )
                    }
                }
            }
        )

        // =====================================================================
        // AI Translation Assistant Modal Overlay
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
