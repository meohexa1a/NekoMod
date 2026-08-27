package org.mdt.core.engine.i18n

/**
 * ## AiTranslationHelper
 *
 * Utility engine generating structured translation prompts for Large Language Models
 * (Gemini, Claude, GPT) and parsing structured responses back into dictionary entries.
 *
 * See: docs/core-subsystems/core_subsystems_en.md
 */
object AiTranslationHelper {

    /**
     * Generates a context-rich prompt template formatted for direct input to AI assistants.
     *
     * @param targetLocale Metadata for the destination language.
     * @param entries List of key-value pairs (Key to English Source String) to translate.
     * @return Formatted Markdown prompt string ready to copy to clipboard.
     */
    fun generatePrompt(
        targetLocale: LocaleMetadata,
        entries: List<Pair<String, String>>
    ): String {
        val sb = StringBuilder()
        sb.appendLine("You are an expert game and UI localizer translating strings for a Mindustry mod and studio tool (NekoMod).")
        sb.appendLine("Translate the following JSON key-value pairs from English into **${targetLocale.displayName} (${targetLocale.nativeName})**.")
        sb.appendLine()
        sb.appendLine("### STRICT LOCALIZATION RULES:")
        sb.appendLine("1. **Preserve Placeholders:** Keep all `{name}`, `{count}`, `{value}`, and `%s` parameter tokens EXACTLY as they appear.")
        sb.appendLine("2. **Preserve Color & Markup Tags:** Do NOT translate or remove Mindustry bracketed color/markup codes like `[accent]`, `[stat]`, `[white]`, `[]`.")
        sb.appendLine("3. **Contextual Natural Phrasing:** Use natural, modern gaming/software terminology appropriate for UI buttons, settings, tooltips, and labels.")
        sb.appendLine("4. **Output Format:** Return ONLY a valid, parseable JSON object mapping each key to its translated string. Do not add markdown commentary outside the JSON block.")
        sb.appendLine()
        sb.appendLine("```json")
        sb.appendLine("{")
        entries.forEachIndexed { index, (key, value) ->
            val escapedKey = escapeJson(key)
            val escapedValue = escapeJson(value)
            val comma = if (index < entries.size - 1) "," else ""
            sb.appendLine("  \"$escapedKey\": \"$escapedValue\"$comma")
        }
        sb.appendLine("}")
        sb.appendLine("```")
        return sb.toString()
    }

    /**
     * Parses an AI response (which may be fenced inside markdown ```json ... ``` or plain key-value lines)
     * into a clean map of translation pairs.
     */
    fun parseAiResponse(rawResponse: String): Map<String, String> {
        val result = mutableMapOf<String, String>()
        val trimmed = rawResponse.trim()

        // 1. Extract content inside markdown code block if present
        val content = if (trimmed.contains("```")) {
            val startIdx = trimmed.indexOf("```")
            val nextLineIdx = trimmed.indexOf('\n', startIdx)
            val endIdx = trimmed.lastIndexOf("```")
            if (nextLineIdx in 0 until endIdx) {
                trimmed.substring(nextLineIdx + 1, endIdx).trim()
            } else {
                trimmed
            }
        } else {
            trimmed
        }

        // 2. Parse line by line (supports JSON `"key": "value"` or properties `key=value`)
        val lines = content.lines()
        for (line in lines) {
            val cleanLine = line.trim().removeSuffix(",")
            if (cleanLine.startsWith("{") || cleanLine.startsWith("}") || cleanLine.startsWith("#")) continue

            // JSON format: "key": "value"
            if (cleanLine.startsWith("\"") && cleanLine.contains("\":")) {
                val colonIdx = cleanLine.indexOf("\":")
                val key = cleanLine.substring(1, colonIdx)
                val valPart = cleanLine.substring(colonIdx + 2).trim()
                if (valPart.startsWith("\"")) {
                    val endQuote = valPart.lastIndexOf('"')
                    if (endQuote > 0) {
                        val value = valPart.substring(1, endQuote)
                        result[unescapeJson(key)] = unescapeJson(value)
                    }
                }
            } else if (cleanLine.contains("=")) {
                // Properties format: key=value
                val eqIdx = cleanLine.indexOf('=')
                val key = cleanLine.substring(0, eqIdx).trim()
                val value = cleanLine.substring(eqIdx + 1).trim()
                if (key.isNotEmpty()) {
                    result[key] = value
                }
            }
        }

        return result
    }

    private fun escapeJson(str: String): String {
        return str
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }

    private fun unescapeJson(str: String): String {
        return str
            .replace("\\n", "\n")
            .replace("\\r", "\r")
            .replace("\\t", "\t")
            .replace("\\\"", "\"")
            .replace("\\\\", "\\")
    }
}
