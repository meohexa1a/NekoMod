package org.mdt.ui.screens.editor.i18n

import androidx.compose.runtime.Composable
import org.mdt.core.engine.i18n.I18nService
import org.mdt.core.engine.i18n.LocaleMetadata
import org.mdt.core.ui.compose.*
import org.mdt.core.ui.layout.Alignment
import org.mdt.core.ui.layout.Arrangement
import org.mdt.ui.components.display.Image
import org.mdt.ui.components.layout.Box
import org.mdt.ui.components.layout.Column
import org.mdt.ui.components.layout.Row
import org.mdt.ui.components.layout.Spacer
import org.mdt.ui.components.scroll.ScrollView
import org.mdt.ui.components.surface.Divider
import org.mdt.ui.components.text.MonoText
import org.mdt.ui.components.text.Text
import org.mdt.ui.theme.StudioIcons
import org.mdt.ui.theme.Theme

/**
 * ## I18nLanguageSidebar
 *
 * Dedicated sidebar component displaying all registered system locales,
 * translation completion ratios, and dynamic progress bars.
 *
 * See: docs/design-system/design_system_en.md
 */
@Composable
fun I18nLanguageSidebar(
    locales: List<LocaleMetadata>,
    selectedLocale: LocaleMetadata,
    onSelectLocale: (LocaleMetadata) -> Unit,
    i18n: I18nService,
    modifier: UIModifier = UIModifier
) {
    val colors = Theme.colors
    val shapes = Theme.shapes
    val spacing = Theme.spacing
    val typography = Theme.typography

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.surfacePrimary)
            .border(1f, colors.borderHairline)
            .pad(spacing.md)
            .then(modifier)
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
                                .clickable { onSelectLocale(locale) }
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
}
