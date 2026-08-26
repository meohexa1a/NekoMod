package org.mdt.core.engine.i18n

import org.mdt.core.engine.EngineContext
import java.util.concurrent.ConcurrentHashMap

/**
 * ## I18nService
 *
 * In-memory, high-performance internationalization and localization registry.
 * Manages locale dictionary bundles and dynamic parameter interpolation (`{name}`),
 * with the active locale strongly bound to persistent [EngineContext.settings].
 *
 * Designed specifically for NXML component-defined localization blocks (`<i18n>`)
 * and zero-latency in-memory dictionary lookup.
 *
 * See: docs/core-subsystems/core_subsystems_en.md
 */
open class I18nService(val context: EngineContext) {

    /**
     * Active locale language code (e.g. 'en', 'vi').
     * Strongly typed and synchronized with persistent [EngineContext.settings].
     */
    var currentLocale: String
        get() = context.settings.locale
        set(value) {
            context.settings.updateLocale(value)
            onLocaleChanged?.invoke()
        }

    /** Optional listener callback invoked whenever the active locale changes. */
    var onLocaleChanged: (() -> Unit)? = null

    /** Thread-safe dictionary registry mapping locale codes to key-value translation maps. */
    private val translations = ConcurrentHashMap<String, MutableMap<String, String>>()

    // =========================================================================
    // I. Translation Query & Resolution (Read API)
    // =========================================================================

    /**
     * Resolves a localized string template with dynamic parameter interpolation (`{paramName}`).
     * Falls back to the `"en"` locale bundle, or returns the raw [key] if absent.
     *
     * @param key Translation key identifier.
     * @param params Key-value pairs substituted into `{paramName}` placeholders.
     * @return Interpolated localized string.
     */
    operator fun get(key: String, vararg params: Pair<String, Any>): String {
        val locale = currentLocale
        val localeMap = translations[locale] ?: translations["en"]
        var template = localeMap?.get(key) ?: translations["en"]?.get(key) ?: key

        for ((name, value) in params) {
            template = template.replace("{$name}", value.toString())
        }

        return template
    }

    /**
     * Checks whether a translation exists for the given [key] in the specified [locale].
     *
     * @param key Translation key identifier.
     * @param locale Target locale code (defaults to [currentLocale]).
     * @return `true` if the key exists in the locale bundle.
     */
    fun has(key: String, locale: String = currentLocale): Boolean =
        translations[locale]?.containsKey(key) == true

    // =========================================================================
    // II. Bundle Registration & Mutation (Write API)
    // =========================================================================

    /**
     * Registers a map of translation key-value pairs for a specific locale code.
     *
     * @param locale Target locale language code (e.g. 'en', 'vi').
     * @param bundle Map of translation key-value pairs.
     */
    fun register(locale: String, bundle: Map<String, String>) {
        val map = translations.computeIfAbsent(locale) { ConcurrentHashMap() }
        map.putAll(bundle)
    }

    /**
     * Operator shorthand to set a translation key-value pair for [currentLocale] (`i18n["btn.save"] = "Save"`).
     *
     * @param key Translation key identifier.
     * @param value Localized text string.
     */
    operator fun set(key: String, value: String) {
        val map = translations.computeIfAbsent(currentLocale) { ConcurrentHashMap() }
        map[key] = value
    }

    /**
     * Operator shorthand to set a translation key-value pair for a specific [locale] (`i18n["vi", "btn.save"] = "Lưu"`).
     *
     * @param locale Target locale language code.
     * @param key Translation key identifier.
     * @param value Localized text string.
     */
    operator fun set(locale: String, key: String, value: String) {
        val map = translations.computeIfAbsent(locale) { ConcurrentHashMap() }
        map[key] = value
    }

    // =========================================================================
    // III. Removal & Lifecycle Cleanup (Deletion API)
    // =========================================================================

    /**
     * Removes a specific translation key from a given locale (defaults to [currentLocale]).
     *
     * @param key Translation key identifier to remove.
     * @param locale Target locale code (defaults to [currentLocale]).
     * @return The previous localized string, or `null` if absent.
     */
    fun remove(key: String, locale: String = currentLocale): String? =
        translations[locale]?.remove(key)

    /**
     * Removes an entire locale translation bundle from memory.
     *
     * @param locale Target locale language code to remove.
     * @return The removed bundle map, or `null` if absent.
     */
    fun removeLocale(locale: String): MutableMap<String, String>? =
        translations.remove(locale)

    /**
     * Operator shorthand to remove a translation key from the [currentLocale] (`i18n -= "key"`).
     *
     * @param key Translation key identifier to remove.
     */
    operator fun minusAssign(key: String) {
        remove(key)
    }

    /**
     * Clears all registered translation bundles from memory.
     */
    fun clear() {
        translations.clear()
    }
}
