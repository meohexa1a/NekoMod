package org.mdt.ui.screens.editor.i18n

import androidx.compose.runtime.*
import arc.Core
import org.mdt.core.engine.EngineContext
import org.mdt.core.engine.i18n.AiTranslationEntry
import org.mdt.core.engine.i18n.AiTranslationHelper
import org.mdt.core.engine.i18n.LocaleMetadata
import org.mdt.core.ui.compose.*
import org.mdt.core.ui.graphics.Color
import org.mdt.core.ui.layout.Alignment
import org.mdt.core.ui.layout.Arrangement
import org.mdt.core.ui.layout.LayoutPreset
import org.mdt.ui.components.display.Image
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
import org.mdt.ui.theme.StudioIcons
import org.mdt.ui.theme.Theme

/**
 * ## AiTranslationModal
 *
 * Multi-step interactive dialog facilitating AI-assisted translation:
 * 1. Exports context-rich prompt template with untranslated key-value pairs.
 * 2. Parses pasted AI JSON/Properties output.
 * 3. Previews interactive translation diffs.
 * 4. Applies verified strings directly into [org.mdt.core.engine.i18n.I18nService].
 *
 * See: docs/design-system/design_system_en.md
 */
@Composable
fun AiTranslationModal(
    targetLocale: LocaleMetadata,
    onClose: () -> Unit,
    onApply: (Map<String, String>) -> Unit
) {
    val colors = Theme.colors
    val shapes = Theme.shapes
    val spacing = Theme.spacing
    val typography = Theme.typography

    val i18n = EngineContext.default.i18n
    val allKeys = remember { i18n.getAllKeys().sorted() }
    val bundle = remember(targetLocale) { i18n.getBundle(targetLocale.code) }
    val enBundle = remember { i18n.getBundle("en") }

    // Keys that need translation
    val missingEntries = remember(targetLocale, allKeys) {
        allKeys.filter { key -> bundle[key].isNullOrBlank() }
            .map { key -> key to (enBundle[key] ?: key) }
    }

    var pastedResponse by remember { mutableStateOf("") }
    var copiedFeedback by remember { mutableStateOf(false) }
    var proposedEntries by remember { mutableStateOf<List<AiTranslationEntry>>(emptyList()) }
    var step by remember { mutableStateOf(1) } // 1: Prompt & Paste, 2: Review Diff

    // Scrim overlay
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0f, 0f, 0f, 0.65f))
            .clickable { /* Block clicks to background */ }
    ) {
        // Modal Container
        Box(
            modifier = Modifier
                .anchor(LayoutPreset.CENTER)
                .width(680f)
                .height(540f)
                .radius(shapes.lg)
                .background(colors.surfaceElevated)
                .border(1f, colors.borderHairline)
                .pad(spacing.lg)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {

                // =============================================================
                // 1. Header & Steps Tracker
                // =============================================================
                Row(
                    arrangement = Arrangement.spacedBy(spacing.sm),
                    alignment = Alignment.CenterStart,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Image(
                        source = StudioIcons.STUDIO_LOGO,
                        modifier = Modifier.size(20f),
                        tint = colors.blue
                    )
                    Text(
                        text = "AI Translation Assistant",
                        font = typography.title,
                        color = colors.textPrimary
                    )
                    MonoText(
                        text = "-> ${targetLocale.displayName} (${targetLocale.code})",
                        color = colors.teal
                    )

                    Spacer(modifier = Modifier.weight(1.0f))

                    // Close Button
                    Box(
                        modifier = Modifier
                            .size(26f, 26f)
                            .radius(shapes.xs)
                            .background(colors.surfaceSecondary)
                            .clickable { onClose() }
                            .pad(6f)
                    ) {
                        Image(
                            source = StudioIcons.COMMENT,
                            modifier = Modifier.fillMaxSize(),
                            tint = colors.textSecondary
                        )
                    }
                }

                Divider(modifier = Modifier.fillMaxWidth().height(1f).margin(vertical = spacing.md))

                // =============================================================
                // 2. Step Body Content
                // =============================================================
                if (step == 1) {
                    // STEP 1: Prompt Generation & Paste Input
                    Column(
                        arrangement = Arrangement.spacedBy(spacing.sm),
                        modifier = Modifier.weight(1.0f).fillMaxWidth()
                    ) {
                        Row(
                            arrangement = Arrangement.spacedBy(spacing.sm),
                            alignment = Alignment.CenterStart
                        ) {
                            Text(
                                text = "1. Export Prompt to Clipboard",
                                font = typography.body,
                                color = colors.textPrimary
                            )
                            MonoText(
                                text = "(${missingEntries.size} keys to translate)",
                                color = colors.textSecondary
                            )
                            Spacer(modifier = Modifier.weight(1.0f))

                            Button(
                                text = if (copiedFeedback) "Copied to Clipboard!" else "Copy AI Prompt",
                                icon = StudioIcons.FILE,
                                variant = if (copiedFeedback) ButtonVariant.TINTED else ButtonVariant.FILLED,
                                onClick = {
                                    val prompt = AiTranslationHelper.generatePrompt(targetLocale, missingEntries)
                                    try {
                                        Core.app?.setClipboardText(prompt)
                                    } catch (_: Throwable) {}
                                    copiedFeedback = true
                                }
                            )
                        }

                        Text(
                            text = "Paste prompt into ChatGPT / Claude / Gemini, then paste the AI's JSON output below:",
                            font = typography.caption,
                            color = colors.textSecondary
                        )

                        // AI Output Text Input Area
                        Box(
                            modifier = Modifier
                                .weight(1.0f)
                                .fillMaxWidth()
                                .radius(shapes.sm)
                                .background(colors.surfacePrimary)
                                .border(1f, colors.borderHairline)
                                .pad(spacing.sm)
                        ) {
                            TextField(
                                value = pastedResponse,
                                onValueChange = { pastedResponse = it },
                                placeholder = "Paste AI JSON response here (e.g. {\"ui.btn.save\": \"Lưu\"})...",
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }

                    Divider(modifier = Modifier.fillMaxWidth().height(1f).margin(vertical = spacing.sm))

                    // Step 1 Footer Action Bar
                    Row(
                        arrangement = Arrangement.spacedBy(spacing.sm),
                        alignment = Alignment.CenterStart,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            text = "Cancel",
                            variant = ButtonVariant.PLAIN,
                            onClick = { onClose() }
                        )

                        Spacer(modifier = Modifier.weight(1.0f))

                        Button(
                            text = "Parse & Review Diffs ->",
                            variant = ButtonVariant.FILLED,
                            enabled = pastedResponse.isNotBlank(),
                            onClick = {
                                val parsed = AiTranslationHelper.parseAiResponse(pastedResponse)
                                proposedEntries = parsed.map { (k, v) ->
                                    AiTranslationEntry(
                                        key = k,
                                        sourceEnglish = enBundle[k] ?: k,
                                        proposedTranslation = v,
                                        isSelected = true
                                    )
                                }
                                if (proposedEntries.isNotEmpty()) {
                                    step = 2
                                }
                            }
                        )
                    }

                } else {
                    // STEP 2: Diff Review & Confirmation Table
                    Column(
                        arrangement = Arrangement.spacedBy(spacing.sm),
                        modifier = Modifier.weight(1.0f).fillMaxWidth()
                    ) {
                        Row(
                            arrangement = Arrangement.spacedBy(spacing.sm),
                            alignment = Alignment.CenterStart
                        ) {
                            Text(
                                text = "2. Review Proposed Translations (${proposedEntries.size} entries)",
                                font = typography.body,
                                color = colors.textPrimary
                            )
                            Spacer(modifier = Modifier.weight(1.0f))

                            Button(
                                text = "<- Back to Prompt",
                                variant = ButtonVariant.PLAIN,
                                onClick = { step = 1 }
                            )
                        }

                        // Diff Table
                        Box(
                            modifier = Modifier
                                .weight(1.0f)
                                .fillMaxWidth()
                                .radius(shapes.sm)
                                .background(colors.surfacePrimary)
                                .border(1f, colors.borderHairline)
                                .pad(spacing.sm)
                        ) {
                            ScrollView(
                                modifier = Modifier.fillMaxSize(),
                                enableVertical = true
                            ) {
                                Column(
                                    arrangement = Arrangement.spacedBy(spacing.xs),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    for (entry in proposedEntries) {
                                        Row(
                                            arrangement = Arrangement.spacedBy(spacing.sm),
                                            alignment = Alignment.CenterStart,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .radius(shapes.xs)
                                                .background(colors.surfaceSecondary)
                                                .pad(horizontal = spacing.sm, vertical = 4f)
                                        ) {
                                            MonoText(
                                                text = entry.key,
                                                color = colors.teal,
                                                modifier = Modifier.width(180f)
                                            )
                                            Text(
                                                text = entry.sourceEnglish,
                                                color = colors.textSecondary,
                                                modifier = Modifier.width(180f)
                                            )
                                            MonoText(
                                                text = "->",
                                                color = colors.textTertiary
                                            )
                                            Text(
                                                text = entry.proposedTranslation,
                                                color = colors.green,
                                                modifier = Modifier.weight(1.0f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Divider(modifier = Modifier.fillMaxWidth().height(1f).margin(vertical = spacing.sm))

                    // Step 2 Footer
                    Row(
                        arrangement = Arrangement.spacedBy(spacing.sm),
                        alignment = Alignment.CenterStart,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            text = "Cancel",
                            variant = ButtonVariant.PLAIN,
                            onClick = { onClose() }
                        )

                        Spacer(modifier = Modifier.weight(1.0f))

                        Button(
                            text = "Apply All Translations (${proposedEntries.size})",
                            variant = ButtonVariant.FILLED,
                            onClick = {
                                val map = proposedEntries
                                    .filter { it.isSelected }
                                    .associate { it.key to it.proposedTranslation }
                                onApply(map)
                                onClose()
                            }
                        )
                    }
                }
            }
        }
    }
}
