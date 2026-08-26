package org.mdt.core.engine.settings

import kotlinx.serialization.Serializable
import kotlinx.serialization.serializer
import org.mdt.core.engine.EngineContext
import org.mdt.core.ui.render.BoxRenderer

/**
 * ## AppSettings
 *
 * Fully typed, immutable application configuration data class containing all domain settings.
 *
 * See: docs/core-subsystems/core_subsystems_en.md
 */
@Serializable
data class AppSettings(
    val graphics: GraphicsSettings = GraphicsSettings(),
    val audio: AudioSettings = AudioSettings(),
    val ui: UISettings = UISettings()
)

/** Graphics, shaders, and visual effects settings. */
@Serializable
data class GraphicsSettings(
    val blurEnabled: Boolean = true,
    val bloomEnabled: Boolean = true
)

/** Sound effects, music, and ambient audio settings. */
@Serializable
data class AudioSettings(
    val sfxVolume: Float = 1.0f,
    val musicVolume: Float = 1.0f,
    val ambientEnabled: Boolean = true
)

/** User interface, localization, and theme settings. */
@Serializable
data class UISettings(
    val language: String = "en",
    val darkTheme: Boolean = true
)

/**
 * ## AppSettingsService
 *
 * Application domain configuration service for Mindustry mod settings.
 * Automatically synchronizes [BoxRenderer] blur and [EngineContext.i18n] locale on changes.
 *
 * See: docs/core-subsystems/core_subsystems_en.md
 */
open class AppSettingsService(
    context: EngineContext,
    fileName: String = "settings.json"
) : SettingsService<AppSettings>(context, AppSettings(), serializer(), fileName) {

    init {
        // Synchronize BoxRenderer blur and i18n locale with settings store
        BoxRenderer.blurEnabled = current.graphics.blurEnabled
        context.i18n.currentLocale = current.ui.language

        store.addListener { updated ->
            BoxRenderer.blurEnabled = updated.graphics.blurEnabled
            context.i18n.currentLocale = updated.ui.language
        }
    }

    /** Atomically updates graphics configuration settings. */
    fun updateGraphics(transform: (GraphicsSettings) -> GraphicsSettings) {
        store.update { it.copy(graphics = transform(it.graphics)) }
    }

    /** Atomically updates audio configuration settings. */
    fun updateAudio(transform: (AudioSettings) -> AudioSettings) {
        store.update { it.copy(audio = transform(it.audio)) }
    }

    /** Atomically updates user interface configuration settings. */
    fun updateUI(transform: (UISettings) -> UISettings) {
        store.update { it.copy(ui = transform(it.ui)) }
    }
}

/**
 * Global convenience facade delegating to the default mod context's [AppSettingsService].
 */
val Settings: AppSettingsService
    inline get() = EngineContext.default.settings
