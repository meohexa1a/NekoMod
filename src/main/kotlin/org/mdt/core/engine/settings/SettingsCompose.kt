package org.mdt.core.engine.settings

import androidx.compose.runtime.*
import org.mdt.core.engine.EngineContext

/**
 * ## collectAsState
 *
 * Seamless reactive bridge between a [ConfigStore] and Compose [State].
 * Updates Compose recomposition scopes automatically whenever the underlying store changes.
 *
 * See: docs/core-subsystems/core_subsystems_en.md
 */
@Composable
fun <T : Any> ConfigStore<T>.collectAsState(): State<T> {
    val state = remember(this) { mutableStateOf(value) }

    DisposableEffect(this) {
        val listener: (T) -> Unit = { state.value = it }
        addListener(listener)
        onDispose { removeListener(listener) }
    }

    return state
}

/**
 * Observes an [AppSettingsService] as reactive Compose [State].
 * Defaults to the active game context settings ([EngineContext.default.settings]).
 */
@Composable
fun rememberSettings(service: AppSettingsService = EngineContext.default.settings): State<AppSettings> =
    service.appStore.collectAsState()
