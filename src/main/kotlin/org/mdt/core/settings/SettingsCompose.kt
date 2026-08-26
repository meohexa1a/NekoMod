package org.mdt.core.settings

import androidx.compose.runtime.*
import org.mdt.core.store.ConfigStore

/**
 * ## collectAsState
 *
 * Seamless reactive bridge between a [ConfigStore] and Compose [State].
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
 * Convenience helper to observe [Settings.store] directly as reactive Compose state.
 */
@Composable
fun rememberSettings(): State<AppSettings> = Settings.store.collectAsState()
