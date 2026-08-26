package org.mdt.core.engine.settings

import kotlinx.serialization.KSerializer
import org.mdt.core.engine.EngineContext

/**
 * ## SettingsService
 *
 * Generic, domain-agnostic configuration service wrapping a typed [ConfigStore].
 * Bound to a specific [EngineContext] instance and storage file.
 *
 * See: docs/core-subsystems/core_subsystems_en.md
 */
open class SettingsService<T : Any>(
    protected val context: EngineContext,
    val default: T,
    val serializer: KSerializer<T>,
    val fileName: String = "settings.json"
) {
    /** Underlying reactive configuration store. */
    val store: ConfigStore<T> = ConfigStore(context.storage.resolve(fileName), default, serializer)

    /** Current snapshot value of the configuration. */
    val current: T
        get() = store.value

    /** Atomically updates configuration state. */
    fun update(transform: (T) -> T) {
        store.update(transform)
    }
}
