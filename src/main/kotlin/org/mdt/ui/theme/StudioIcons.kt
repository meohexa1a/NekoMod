package org.mdt.ui.theme

/**
 * ## StudioIcons
 *
 * Semantic icon registry for Neko Studio using concise URI schemes.
 * Supports multiple CDN backends: `material:`, `lucide:`, `sf:`, `fluent:`, `tabler:`, `ph:`.
 *
 * Backed by [org.mdt.core.engine.image.ImageService] with automatic disk caching and L1 VRAM retention.
 *
 * See: docs/design-system/design_system_en.md
 */
object StudioIcons {
    // Brand & Main Navigation
    const val STUDIO_LOGO = "material:sparkling"
    const val LAYERS = "material:layers"
    const val COMPONENTS = "material:package"
    const val I18N = "material:globe"
    const val CODE = "material:code"

    // Primary Canvas Tools
    const val SELECT = "material:cursor"
    const val FRAME = "material:grid-2"
    const val RECT = "material:rounded-rectangle"
    const val TEXT = "material:type"
    const val PEN = "material:pen"
    const val COMPONENT = "material:puzzle"
    const val COMMENT = "material:speech-bubble"

    // Tree & Hierarchy Node Types
    const val SCENE = "material:window"
    const val CONTAINER = "material:template"
    const val IMAGE = "material:image"
    const val CARD = "material:layers"
    const val BUTTON = "material:cursor-in-window"
    const val SETTINGS = "material:settings"
    const val FILE = "material:file"
    const val EYE = "material:visible"
    const val PLUS = "material:plus-math"
    const val CHEVRON_LEFT = "material:chevron-left"
    const val CHEVRON_RIGHT = "material:chevron-right"
    const val CHEVRON_DOWN = "material:chevron-down"
    const val REFRESH = "material:synchronize"
    const val PLAY = "material:play"
    const val APPLE = "material:mac-os"
}
