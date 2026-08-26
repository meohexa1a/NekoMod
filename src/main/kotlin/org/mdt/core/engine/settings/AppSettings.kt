package org.mdt.core.engine.settings

import kotlinx.serialization.Serializable
import kotlinx.serialization.serializer
import org.mdt.core.engine.EngineContext
import org.mdt.core.ui.render.BoxRenderer

/**
 * ## AppSettings
 *
 * Immutable, fully typed domain configuration data class for the Mindustry Mod environment.
 * Contains core engine settings alongside graphics and audio preferences.
 *
 * See: docs/core-subsystems/core_subsystems_en.md
 */
@Serializable
data class AppSettings(
    val core: CoreSettings = CoreSettings(),
    val graphics: GraphicsSettings = GraphicsSettings(),
    val audio: AudioSettings = AudioSettings()
)

/** Graphics, shaders, and post-processing visual settings. */
@Serializable
data class GraphicsSettings(
    val blurEnabled: Boolean = true,
    val bloomEnabled: Boolean = true,
    val maxUploadsPerFrame: Int = 4
)

/** Sound effects, music, and ambient audio settings. */
@Serializable
data class AudioSettings(
    val sfxVolume: Float = 1.0f,
    val musicVolume: Float = 1.0f,
    val ambientEnabled: Boolean = true
)

/**
 * ## AppSettingsService
 *
 * Application domain configuration service for Mindustry mod settings.
 * Specializes [CoreSettingsService] to eliminate downcasting across the engine.
 *
 * See: docs/core-subsystems/core_subsystems_en.md
 */
open class AppSettingsService(
    context: EngineContext,
    fileName: String = "settings.json"
) : CoreSettingsService(context, fileName) {

    /** Typed configuration store for the complete application domain settings. */
    val appStore: ConfigStore<AppSettings> =
        ConfigStore(context.storage.resolve(fileName), AppSettings(), serializer())

    /** Current snapshot value of domain settings. */
    val current: AppSettings
        get() = appStore.value

    override val locale: String
        get() = appStore.value.core.locale

    override val darkTheme: Boolean
        get() = appStore.value.core.darkTheme

    init {
        // Synchronize BoxRenderer blur status with settings registry
        BoxRenderer.blurEnabled = current.graphics.blurEnabled
        appStore.addListener { updated ->
            BoxRenderer.blurEnabled = updated.graphics.blurEnabled
        }
    }

    // =========================================================================
    // I. State Mutators
    // =========================================================================

    override fun updateLocale(locale: String) {
        appStore.update { it.copy(core = it.core.copy(locale = locale)) }
    }

    override fun updateTheme(darkTheme: Boolean) {
        appStore.update { it.copy(core = it.core.copy(darkTheme = darkTheme)) }
    }

    /** Atomically updates core engine configuration settings. */
    fun updateCore(transform: (CoreSettings) -> CoreSettings) {
        appStore.update { it.copy(core = transform(it.core)) }
    }

    /** Atomically updates graphics configuration settings. */
    fun updateGraphics(transform: (GraphicsSettings) -> GraphicsSettings) {
        appStore.update { it.copy(graphics = transform(it.graphics)) }
    }

    /** Atomically updates audio configuration settings. */
    fun updateAudio(transform: (AudioSettings) -> AudioSettings) {
        appStore.update { it.copy(audio = transform(it.audio)) }
    }
}
