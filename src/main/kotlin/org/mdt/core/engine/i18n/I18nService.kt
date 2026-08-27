package org.mdt.core.engine.i18n

import org.mdt.core.engine.EngineContext
import java.util.concurrent.ConcurrentHashMap

/**
 * ## I18nService
 *
 * In-memory, high-performance internationalization and localization registry.
 * Manages locale dictionary bundles, dynamic parameter interpolation (`{name}`),
 * runtime missing-key tracking, coverage analysis, and standard `.properties` import/export.
 *
 * See: docs/core-subsystems/core_subsystems_en.md
 */
open class I18nService(val context: EngineContext) {

    private var inMemoryLocale: String = "en"

    /**
     * Active locale language code (e.g. 'en', 'vi', 'ja').
     * Strongly typed and synchronized with persistent [EngineContext.settings].
     */
    var currentLocale: String
        get() = try {
            context.settings.locale
        } catch (_: Throwable) {
            inMemoryLocale
        }
        set(value) {
            inMemoryLocale = value
            try {
                context.settings.updateLocale(value)
            } catch (_: Throwable) {}
            onLocaleChanged?.invoke()
        }

    /** Optional listener callback invoked whenever the active locale changes. */
    var onLocaleChanged: (() -> Unit)? = null

    /** Thread-safe dictionary registry mapping locale codes to key-value translation maps. */
    private val translations = ConcurrentHashMap<String, MutableMap<String, String>>()

    /** Thread-safe registry tracking keys queried at runtime that were missing in the active bundle. */
    private val missingKeysRegistry = ConcurrentHashMap<String, MissingKeyEntry>()

    init {
        // Register standard default English core translations
        register("en", mapOf(
            "studio.title" to "Neko Studio",
            "studio.mode.scene" to "Scene",
            "studio.mode.components" to "Components",
            "studio.mode.assets" to "Assets",
            "studio.mode.i18n" to "i18n",
            "studio.mode.settings" to "Settings",
            "common.save" to "Save",
            "common.cancel" to "Cancel",
            "common.delete" to "Delete",
            "common.search" to "Search...",
            "common.reload" to "Reload",
            "common.preview" to "Preview",
            "settings.general" to "General",
            "settings.viewport" to "Viewport & Grid",
            "settings.gpu" to "GPU & Shaders",
            "settings.components" to "UI Components",
            "settings.shortcuts" to "Shortcuts",
            "settings.about" to "About Studio",
            "i18n.all_keys" to "All Keys",
            "i18n.missing_keys" to "Missing Keys",
            "i18n.ai_assistant" to "AI Assistant",
            "i18n.import_bundle" to "Import Bundle",
            "i18n.export_bundle" to "Export Bundle"
        ))

        // Pre-seed common Vietnamese translations
        register("vi", mapOf(
            "studio.title" to "Neko Studio",
            "studio.mode.scene" to "Phân cảnh",
            "studio.mode.components" to "Thành phần",
            "studio.mode.assets" to "Tài nguyên",
            "studio.mode.i18n" to "Đa ngôn ngữ",
            "studio.mode.settings" to "Cài đặt",
            "common.save" to "Lưu",
            "common.cancel" to "Hủy",
            "common.delete" to "Xóa",
            "common.search" to "Tìm kiếm...",
            "common.reload" to "Tải lại",
            "common.preview" to "Xem trước",
            "settings.general" to "Chung",
            "settings.viewport" to "Khung nhìn & Lưới",
            "settings.gpu" to "GPU & Hiệu ứng",
            "settings.components" to "Thư viện UI",
            "settings.shortcuts" to "Phím tắt",
            "settings.about" to "Thông tin Studio",
            "i18n.all_keys" to "Tất cả các khóa",
            "i18n.missing_keys" to "Chưa dịch",
            "i18n.ai_assistant" to "Trợ lý AI",
            "i18n.import_bundle" to "Nhập gói ngôn ngữ",
            "i18n.export_bundle" to "Xuất gói ngôn ngữ"
        ))
    }

    // =========================================================================
    // I. Translation Query & Resolution (Read API)
    // =========================================================================

    /**
     * Resolves a localized string template with dynamic parameter interpolation (`{paramName}`).
     * Automatically tracks missing keys into [missingKeysRegistry] for studio diagnostics.
     *
     * @param key Translation key identifier.
     * @param params Key-value pairs substituted into `{paramName}` placeholders.
     * @return Interpolated localized string.
     */
    operator fun get(key: String, vararg params: Pair<String, Any>): String {
        val locale = currentLocale
        val localeMap = translations[locale]
        val enMap = translations["en"]

        val rawTemplate = localeMap?.get(key)
        val template = if (rawTemplate != null) {
            rawTemplate
        } else {
            // Track key as missing in the current locale
            if (!missingKeysRegistry.containsKey("$locale:$key")) {
                missingKeysRegistry["$locale:$key"] = MissingKeyEntry(
                    key = key,
                    locale = locale,
                    sampleParams = params.map { it.first }
                )
            }
            enMap?.get(key) ?: key
        }

        var result = template
        for ((name, value) in params) {
            result = result.replace("{$name}", value.toString())
        }

        return result
    }

    /** Checks whether a translation exists for the given [key] in [locale]. */
    fun has(key: String, locale: String = currentLocale): Boolean =
        translations[locale]?.containsKey(key) == true

    /** Returns all unique keys known across all registered bundles and recorded missing keys. */
    fun getAllKeys(): Set<String> {
        val keys = mutableSetOf<String>()
        translations.values.forEach { keys.addAll(it.keys) }
        missingKeysRegistry.values.forEach { keys.add(it.key) }
        return keys
    }

    /** Returns the raw string value for [key] in [locale] without fallback substitution. */
    fun getRaw(key: String, locale: String = currentLocale): String? =
        translations[locale]?.get(key)

    /** Returns an immutable snapshot of all translations for a given [locale]. */
    fun getBundle(locale: String): Map<String, String> =
        translations[locale]?.toMap() ?: emptyMap()

    /** Returns all missing keys recorded for a specific [locale]. */
    fun getMissingKeys(locale: String = currentLocale): List<MissingKeyEntry> =
        missingKeysRegistry.values.filter { it.locale == locale }

    /** Computes translation completion statistics for [locale]. */
    fun getLocaleStats(locale: String): LocaleStats {
        val allKeys = getAllKeys()
        if (allKeys.isEmpty()) {
            return LocaleStats(locale, 0, 0, 0, 1.0f)
        }

        val bundle = translations[locale] ?: emptyMap()
        var translatedCount = 0
        for (key in allKeys) {
            if (bundle[key]?.isNotBlank() == true) {
                translatedCount++
            }
        }
        val missingCount = allKeys.size - translatedCount
        val ratio = translatedCount.toFloat() / allKeys.size.toFloat()
        return LocaleStats(locale, allKeys.size, translatedCount, missingCount, ratio)
    }

    // =========================================================================
    // II. Bundle Registration & Mutation (Write API)
    // =========================================================================

    /** Registers a map of translation key-value pairs for a specific locale code. */
    fun register(locale: String, bundle: Map<String, String>) {
        val map = translations.computeIfAbsent(locale) { ConcurrentHashMap() }
        map.putAll(bundle)
        // Clear missing key records that are now satisfied
        bundle.keys.forEach { missingKeysRegistry.remove("$locale:$it") }
    }

    /** Operator shorthand to set a translation key-value pair for [currentLocale]. */
    operator fun set(key: String, value: String) {
        set(currentLocale, key, value)
    }

    /** Operator shorthand to set a translation key-value pair for a specific [locale]. */
    operator fun set(locale: String, key: String, value: String) {
        val map = translations.computeIfAbsent(locale) { ConcurrentHashMap() }
        map[key] = value
        missingKeysRegistry.remove("$locale:$key")
    }

    // =========================================================================
    // III. Standard Mindustry Properties Import & Export
    // =========================================================================

    /** Parses standard Java / Mindustry `.properties` file format into [locale]. */
    fun loadProperties(locale: String, propertiesContent: String) {
        val map = mutableMapOf<String, String>()
        for (rawLine in propertiesContent.lines()) {
            val line = rawLine.trim()
            if (line.isEmpty() || line.startsWith("#") || line.startsWith("!")) continue

            val separatorIdx = line.indexOf('=').let { if (it == -1) line.indexOf(':') else it }
            if (separatorIdx > 0) {
                val key = line.substring(0, separatorIdx).trim()
                val value = line.substring(separatorIdx + 1).trim()
                map[key] = unescapePropertiesValue(value)
            }
        }
        register(locale, map)
    }

    /** Exports all key-values of [locale] to a standard Mindustry `.properties` string format. */
    fun exportProperties(locale: String): String {
        val bundle = translations[locale] ?: emptyMap()
        val allKeys = getAllKeys().sorted()
        val sb = StringBuilder()
        sb.appendLine("# NekoMod Localization Bundle: $locale")
        sb.appendLine("# Generated on ${java.time.Instant.now()}")
        sb.appendLine()

        for (key in allKeys) {
            val value = bundle[key] ?: ""
            sb.appendLine("$key = ${escapePropertiesValue(value)}")
        }
        return sb.toString()
    }

    private fun escapePropertiesValue(value: String): String =
        value.replace("\n", "\\n").replace("\r", "\\r")

    private fun unescapePropertiesValue(value: String): String =
        value.replace("\\n", "\n").replace("\\r", "\r")

    // =========================================================================
    // IV. Removal & Cleanup
    // =========================================================================

    fun remove(key: String, locale: String = currentLocale): String? =
        translations[locale]?.remove(key)

    fun removeLocale(locale: String): MutableMap<String, String>? =
        translations.remove(locale)

    operator fun minusAssign(key: String) {
        remove(key)
    }

    fun clear() {
        translations.clear()
        missingKeysRegistry.clear()
    }
}

