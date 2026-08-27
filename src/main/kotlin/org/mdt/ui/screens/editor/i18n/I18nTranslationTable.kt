package org.mdt.ui.screens.editor.i18n

import androidx.compose.runtime.Composable
import org.mdt.core.engine.i18n.I18nService
import org.mdt.core.engine.i18n.LocaleMetadata
import org.mdt.core.ui.compose.*
import org.mdt.core.ui.layout.Alignment
import org.mdt.core.ui.layout.Arrangement
import org.mdt.ui.components.input.textfield.TextField
import org.mdt.ui.components.layout.Box
import org.mdt.ui.components.layout.Column
import org.mdt.ui.components.layout.Row
import org.mdt.ui.components.scroll.ScrollView
import org.mdt.ui.components.surface.Divider
import org.mdt.ui.components.text.MonoText
import org.mdt.ui.components.text.Text
import org.mdt.ui.theme.Theme

/**
 * ## I18nTranslationTable
 *
 * Dedicated translation matrix table rendering key identifiers,
 * English reference values, and inline reactive translation textfields.
 *
 * See: docs/design-system/design_system_en.md
 */
@Composable
fun I18nTranslationTable(
    displayedKeys: List<String>,
    selectedLocale: LocaleMetadata,
    currentBundle: Map<String, String>,
    enBundle: Map<String, String>,
    i18n: I18nService,
    onValueUpdated: () -> Unit,
    modifier: UIModifier = UIModifier
) {
    val colors = Theme.colors
    val shapes = Theme.shapes
    val spacing = Theme.spacing

    Box(
        modifier = Modifier
            .fillMaxSize()
            .radius(shapes.md)
            .background(colors.surfacePrimary)
            .border(1f, colors.borderHairline)
            .pad(spacing.md)
            .then(modifier)
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
                        val englishValue = enBundle[key] ?: key
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
                                text = englishValue,
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
                                    onValueChange = { newValue ->
                                        i18n[selectedLocale.code, key] = newValue
                                        onValueUpdated()
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
