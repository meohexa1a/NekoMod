package org.mdt.core.settings

import kotlinx.serialization.Serializable
import org.mdt.core.engine.storage.ConfigStore
import org.mdt.ui.render.BoxRenderer

/**
 * ## AppSettings
 *
 * Fully typed, immutable application configuration data class.
 */
@Serializable
data class AppSettings(
    val graphics: GraphicsSettings = GraphicsSettings(),
    val audio: AudioSettings = AudioSettings(),
    val ui: UISettings = UISettings()
)

@Serializable
data class GraphicsSettings(
    val blurEnabled: Boolean = true,
    val bloomEnabled: Boolean = true
)

@Serializable
data class AudioSettings(
    val sfxVolume: Float = 1.0f,
    val musicVolume: Float = 1.0f,
    val ambientEnabled: Boolean = true
)

@Serializable
data class UISettings(
    val language: String = "en",
    val darkTheme: Boolean = true
)

/**
 * ## Settings
 *
 * Centralized, reactive application configuration manager backed by [ConfigStore].
 *
 * See: docs/core-subsystems/core_subsystems_en.md
 */
object Settings {

    val store: ConfigStore<AppSettings> = ConfigStore.create("settings", AppSettings())

    var current: AppSettings
        get() = store.value
        set(value) { store.value = value }

    init {
        // Synchronize BoxRenderer blur status with settings registry
        BoxRenderer.blurEnabled = current.graphics.blurEnabled
        store.addListener { updated ->
            BoxRenderer.blurEnabled = updated.graphics.blurEnabled
        }
    }

    fun updateGraphics(transform: (GraphicsSettings) -> GraphicsSettings) {
        store.update { it.copy(graphics = transform(it.graphics)) }
    }

    fun updateAudio(transform: (AudioSettings) -> AudioSettings) {
        store.update { it.copy(audio = transform(it.audio)) }
    }

    fun updateUI(transform: (UISettings) -> UISettings) {
        store.update { it.copy(ui = transform(it.ui)) }
    }
}
