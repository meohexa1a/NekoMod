package org.mdt.core.ui.compose

import androidx.compose.runtime.*
import org.mdt.core.engine.EngineContext

/**
 * ## stringResource
 *
 * Declarative Compose helper resolving localized translation strings from the active [EngineContext.i18n].
 * Automatically observes active locale mutations to trigger seamless UI recomposition.
 *
 * @param key Translation key identifier.
 * @param params Optional key-value interpolation tokens.
 * @return Resolved localized string.
 *
 * See: docs/compose-dsl/compose_dsl_en.md
 */
@Composable
fun stringResource(key: String, vararg params: Pair<String, Any>): String {
    val i18n = EngineContext.default.i18n
    var activeLocale by remember { mutableStateOf(i18n.currentLocale) }

    DisposableEffect(i18n) {
        val prevListener = i18n.onLocaleChanged
        i18n.onLocaleChanged = {
            activeLocale = i18n.currentLocale
            prevListener?.invoke()
        }
        onDispose {
            i18n.onLocaleChanged = prevListener
        }
    }

    // Force recomposition dependency on activeLocale
    @Suppress("UNUSED_VARIABLE")
    val unused = activeLocale

    return i18n.get(key, *params)
}
