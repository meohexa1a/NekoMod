package org.mdt.core.i18n

import java.util.concurrent.ConcurrentHashMap

/**
 * ## I18nEngine
 *
 * Modern hierarchical internationalization (i18n) engine supporting nested keys,
 * parameter interpolation ({name}), and live locale switching.
 */
object I18nEngine {

    var currentLocale: String = "en"
        set(value) {
            field = value
            onLocaleChanged?.invoke()
        }

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
     * Registers a map of translation key-value pairs for a specific locale.
     */
    fun register(locale: String, bundle: Map<String, String>) {
        val map = translations.computeIfAbsent(locale) { ConcurrentHashMap() }
        map.putAll(bundle)
    }

    /**
     * Resolves a localized string with parameter interpolation.
     */
    fun get(key: String, vararg params: Pair<String, Any>): String {
        val localeMap = translations[currentLocale] ?: translations["en"]
        var template = localeMap?.get(key) ?: translations["en"]?.get(key) ?: key

        for ((name, value) in params) {
            template = template.replace("{$name}", value.toString())
        }

        return template
    }
}

/**
 * Convenient global shorthand for [I18nEngine.get].
 */
fun i18n(key: String, vararg params: Pair<String, Any>): String {
    return I18nEngine.get(key, *params)
}
