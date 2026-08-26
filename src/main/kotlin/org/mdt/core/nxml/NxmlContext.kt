package org.mdt.core.nxml

import org.dom4j.Element

/**
 * ## NxmlContext
 *
 * Runtime evaluation context maintaining i18n dictionaries, reactive state values,
 * and reusable `<template>` definitions for NXML scene graphs.
 *
 * See: docs/nxml-schema/nxml_specification_en.md
 */
class NxmlContext(
    val i18nDictionary: MutableMap<String, String> = HashMap(),
    val stateBindings: MutableMap<String, Any?> = HashMap(),
    val templates: MutableMap<String, Element> = HashMap()
) {
    // =========================================================================
    // I. i18n & Expression Resolution
    // =========================================================================

    /**
     * Resolves dynamic expressions such as `$t(key)` for localization or `@state.var` for state values.
     *
     * @param rawText Raw text string from XML attributes or node text.
     * @return Resolved string representation.
     */
    fun resolveText(rawText: String): String {
        if (rawText.isEmpty()) return ""

        // Handle i18n lookup: $t(key) or $t(key, fallback)
        if (rawText.startsWith("\$t(") && rawText.endsWith(")")) {
            val innerKey = rawText.substring(3, rawText.length - 1).trim()
            return i18nDictionary[innerKey] ?: innerKey
        }

        // Handle reactive state lookup: @key or @state.key
        if (rawText.startsWith("@")) {
            val stateKey = rawText.removePrefix("@").removePrefix("state.")
            return stateBindings[stateKey]?.toString() ?: rawText
        }

        return rawText
    }

    // =========================================================================
    // II. Reusable Template Management
    // =========================================================================

    /**
     * Registers a reusable component template by tag name.
     *
     * @param name Template tag name (e.g. "Card", "Button").
     * @param template Root element defining the template.
     */
    fun registerTemplate(name: String, template: Element) {
        templates[name.lowercase()] = template
    }

    /**
     * Finds a registered component template by tag name.
     *
     * @param name Template tag name.
     * @return Registered [Element], or `null` if absent.
     */
    fun findTemplate(name: String): Element? = templates[name.lowercase()]

    /**
     * Registers a localization key-value pair into the i18n dictionary.
     */
    fun setTranslation(key: String, translation: String) {
        i18nDictionary[key] = translation
    }

    companion object {
        /** Creates a default context pre-loaded with standard UI strings and components. */
        fun createDefault(): NxmlContext = NxmlContext()
    }
}
