package org.mdt.core.engine.i18n

import org.mdt.core.engine.EngineContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class I18nServiceTest {

    @Test
    fun testMissingKeyAutoRecording() {
        val i18n = I18nService(EngineContext.default)
        i18n.currentLocale = "vi"

        // 1. Query a key that does not exist
        val result = i18n["game.over.subtitle", "score" to 100]

        // Should return the key itself since neither 'vi' nor 'en' has it
        assertEquals("game.over.subtitle", result)

        // 2. Check if the key was automatically recorded in missing keys
        val missing = i18n.getMissingKeys("vi")
        assertTrue(missing.any { it.key == "game.over.subtitle" })
    }

    @Test
    fun testLocaleCoverageStats() {
        val i18n = I18nService(EngineContext.default)
        val allKeys = i18n.getAllKeys()
        assertTrue(allKeys.isNotEmpty())

        val statsEn = i18n.getLocaleStats("en")
        assertEquals(1.0f, statsEn.coverageRatio)
        assertEquals(0, statsEn.missingKeys)

        val statsVi = i18n.getLocaleStats("vi")
        assertTrue(statsVi.translatedKeys > 0)
    }

    @Test
    fun testAiPromptGenerationAndParsing() {
        val locale = LocaleMetadata("vi", "Vietnamese", "Tiếng Việt")
        val entries = listOf(
            "ui.btn.save" to "Save",
            "ui.msg.welcome" to "Welcome, {name}!"
        )

        // 1. Generate prompt
        val prompt = AiTranslationHelper.generatePrompt(locale, entries)
        assertTrue(prompt.contains("Vietnamese"))
        assertTrue(prompt.contains("ui.btn.save"))
        assertTrue(prompt.contains("ui.msg.welcome"))

        // 2. Parse mock AI response
        val mockAiResponse = """
            ```json
            {
              "ui.btn.save": "Lưu",
              "ui.msg.welcome": "Chào mừng, {name}!"
            }
            ```
        """.trimIndent()

        val parsed = AiTranslationHelper.parseAiResponse(mockAiResponse)
        assertEquals("Lưu", parsed["ui.btn.save"])
        assertEquals("Chào mừng, {name}!", parsed["ui.msg.welcome"])
    }

    @Test
    fun testPropertiesExportAndImport() {
        val i18n = I18nService(EngineContext.default)
        i18n["ja", "menu.start"] = "スタート"
        i18n["ja", "menu.exit"] = "終了"

        val exported = i18n.exportProperties("ja")
        assertTrue(exported.contains("menu.start = スタート"))
        assertTrue(exported.contains("menu.exit = 終了"))

        // Create a new instance and import
        val freshI18n = I18nService(EngineContext.default)
        freshI18n.loadProperties("ja", exported)

        assertEquals("スタート", freshI18n.getRaw("menu.start", "ja"))
        assertEquals("終了", freshI18n.getRaw("menu.exit", "ja"))
    }
}
