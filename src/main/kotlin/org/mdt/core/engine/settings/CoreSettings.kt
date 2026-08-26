package org.mdt.core.engine.settings

import kotlinx.serialization.Serializable
import kotlinx.serialization.serializer
import org.mdt.core.engine.EngineContext

/**
 * ## CoreSettings
 *
 * Universal configuration state present in every [EngineContext].
 * Manages persistent locale code and theme mode across all environments (Game, Editor, Launcher).
 *
 * See: docs/core-subsystems/core_subsystems_en.md
 */
@Serializable
data class CoreSettings(
    val locale: String = "en",
    val darkTheme: Boolean = true
)

/**
 * ## CoreSettingsService
 *
 * Base configuration service for [EngineContext] providing strongly-typed access
 * to universal engine properties (locale, theme) without dynamic downcasting.
 *
 * See: docs/core-subsystems/core_subsystems_en.md
 */
open class CoreSettingsService(
    protected val context: EngineContext,
    fileName: String = "core-settings.json"
) {
    /** Underlying reactive configuration store. */
    open val store: ConfigStore<CoreSettings> =
        ConfigStore(context.storage.resolve(fileName), CoreSettings(), serializer())

    /** Current persistent locale code. */
    open val locale: String
        get() = store.value.locale

    /** Current persistent theme mode. */
    open val darkTheme: Boolean
        get() = store.value.darkTheme

    /** Atomically updates the active locale in settings. */
    open fun updateLocale(locale: String) {
        store.update { it.copy(locale = locale) }
    }

    /** Atomically updates the theme mode in settings. */
    open fun updateTheme(darkTheme: Boolean) {
        store.update { it.copy(darkTheme = darkTheme) }
    }
}
