package org.mdt.core.engine.i18n

import org.mdt.core.engine.EngineContext
import java.util.concurrent.ConcurrentHashMap

/**
 * ## I18nService
 *
 * In-memory, high-performance internationalization registry.
 * Manages locale dictionary bundles, dynamic parameter interpolation ({name}),
 * and reactive locale change notifications without filesystem I/O overhead.
 *
 * Designed specifically for NXML component-defined localization blocks (<i18n>)
 * and runtime in-memory dictionary injection.
 *
 * See: docs/core-subsystems/core_subsystems_en.md
 */
open class I18nService(val context: EngineContext) {

    /** Active locale language code (e.g. 'en', 'vi'). Setting a new value triggers [onLocaleChanged]. */
    var currentLocale: String = "en"
        set(value) {
            if (field != value) {
                field = value
                onLocaleChanged?.invoke()
            }
        }

    /** Optional listener callback invoked when the active locale changes. */
    var onLocaleChanged: (() -> Unit)? = null

    private val translations = ConcurrentHashMap<String, MutableMap<String, String>>()

    init {
        // Register default fallback strings
        register("en", mapOf(
            "app.title" to "NEKOMOD ENGINE",
            "app.subtitle" to "Declarative KMP Virtual DOM • 60 FPS GPU Render",
            "btn.count" to "Count: {count}",
            "btn.reset" to "Reset",
            "status.online" to "Online",
            "status.offline" to "Offline"
        ))

        register("vi", mapOf(
            "app.title" to "NEKOMOD ENGINE",
            "app.subtitle" to "Virtual DOM Khai báo KMP • Dựng hình GPU 60 FPS",
            "btn.count" to "Đếm: {count}",
            "btn.reset" to "Đặt lại",
            "status.online" to "Trực tuyến",
            "status.offline" to "Ngoại tuyến"
        ))
    }

    /**
     * Registers a bundle of translation key-value pairs for a specific locale code.
     */
    fun register(locale: String, bundle: Map<String, String>) {
        val map = translations.computeIfAbsent(locale) { ConcurrentHashMap() }
        map.putAll(bundle)
    }

    /**
     * Clears all registered translation bundles from memory.
     */
    fun clear() {
        translations.clear()
    }

    /**
     * Resolves a localized string template with dynamic parameter interpolation ({paramName}).
     */
    operator fun get(key: String, vararg params: Pair<String, Any>): String {
        val localeMap = translations[currentLocale] ?: translations["en"]
        var template = localeMap?.get(key) ?: translations["en"]?.get(key) ?: key

        for ((name, value) in params) {
            template = template.replace("{$name}", value.toString())
        }

        return template
    }
}

/**
 * Convenient global shorthand for [EngineContext.current.i18n.get].
 */
fun i18n(key: String, vararg params: Pair<String, Any>): String =
    EngineContext.current.i18n.get(key, *params)
