package org.mdt.ui.screens.editor.model

import org.mdt.ui.theme.StudioIcons

/**
 * ## EditorMode
 *
 * Workspace mode determining active sidebar tabs, viewport canvas renderers,
 * and top-level studio tool configurations.
 *
 * See: docs/design-system/design_system_en.md
 */
enum class EditorMode(val title: String, val iconUrl: String) {
    SCENE("Scene", StudioIcons.SCENE),
    COMPONENTS("Components", StudioIcons.COMPONENTS),
    ASSETS("Assets", StudioIcons.IMAGE),
    I18N("i18n", StudioIcons.I18N),
    CODE("Code", StudioIcons.CODE),
    SETTINGS("Settings", StudioIcons.SETTINGS)
}
