package org.mdt.core.nxml

import arc.util.Log
import org.dom4j.Document
import org.dom4j.DocumentHelper
import org.dom4j.Element
import org.dom4j.io.OutputFormat
import org.dom4j.io.SAXReader
import org.dom4j.io.XMLWriter
import java.io.File
import java.io.StringReader
import java.io.StringWriter

/**
 * ## NxmlDocument
 *
 * Core reading, parsing, manipulating, and pretty-print writing abstraction for NXML documents.
 * Backed by [org.dom4j.Document] with lenient error handling and structured formatting.
 *
 * See: docs/nxml-schema/nxml_specification_en.md
 */
class NxmlDocument(
    val document: Document
) {
    /** The root [Element] of the NXML document tree. */
    val rootElement: Element
        get() = document.rootElement

    // =========================================================================
    // I. Serialization & Writing (Write API)
    // =========================================================================

    /**
     * Serializes this NXML document tree into a clean, pretty-printed XML string with standard 2-space indentation.
     *
     * @return Formatted NXML string.
     */
    fun toPrettyXml(): String {
        val stringWriter = StringWriter()
        val format = OutputFormat.createPrettyPrint().apply {
            indent = "  "
            isNewlines = true
            isTrimText = true
            isPadText = false
            encoding = "UTF-8"
        }

        val xmlWriter = XMLWriter(stringWriter, format)
        xmlWriter.write(document)
        xmlWriter.flush()
        return stringWriter.toString()
    }

    /**
     * Writes this NXML document tree directly to a target destination [File].
     *
     * @param target Destination file on disk.
     */
    fun writeToFile(target: File) {
        val prettyContent = toPrettyXml()
        target.parentFile?.mkdirs()
        target.writeText(prettyContent, Charsets.UTF_8)
    }

    companion object {
        // =====================================================================
        // II. Deserialization & Parsing (Read API)
        // =====================================================================

        /**
         * Parses an NXML raw string into a structured [NxmlDocument].
         * Safely catches parsing errors and returns a fallback empty document on failure.
         *
         * @param xmlString Raw XML content string.
         * @return Parsed [NxmlDocument], or fallback container if malformed.
         */
        fun parse(xmlString: String): NxmlDocument {
            if (xmlString.isBlank()) {
                return createDefaultEmpty()
            }

            return try {
                val reader = SAXReader().apply {
                    // Disable external entity resolution for security and speed
                    setFeature("http://apache.org/xml/features/disallow-doctype-decl", false)
                    setFeature("http://xml.org/sax/features/external-general-entities", false)
                    setFeature("http://xml.org/sax/features/external-parameter-entities", false)
                }

                val doc = reader.read(StringReader(xmlString))
                NxmlDocument(doc)
            } catch (error: Throwable) {
                Log.warn("[NXML] Failed to parse XML document: ${error.message}")
                createFallbackErrorDocument(error.message ?: "Unknown XML syntax error", xmlString)
            }
        }

        /**
         * Reads and parses an NXML file from disk.
         *
         * @param file Target NXML file on disk.
         * @return Parsed [NxmlDocument].
         */
        fun parseFile(file: File): NxmlDocument {
            if (!file.exists()) return createDefaultEmpty()
            return parse(file.readText(Charsets.UTF_8))
        }

        /**
         * Creates a clean, empty default NXML scene document.
         */
        fun createDefaultEmpty(): NxmlDocument {
            val doc = DocumentHelper.createDocument()
            doc.addElement("Scene").apply {
                addAttribute("name", "DefaultScene")
            }
            return NxmlDocument(doc)
        }

        private fun createFallbackErrorDocument(errorMessage: String, rawContent: String): NxmlDocument {
            val doc = DocumentHelper.createDocument()
            val scene = doc.addElement("Scene").apply {
                addAttribute("name", "ErrorFallback")
            }

            val box = scene.addElement("Box").apply {
                addAttribute("fill", "true")
                addAttribute("background", "#2a1215")
                addAttribute("pad", "16")
            }

            val col = box.addElement("Column").apply {
                addAttribute("gap", "8")
            }

            col.addElement("Text").apply {
                addAttribute("text", "NXML Syntax Error: $errorMessage")
                addAttribute("color", "#ef4444")
            }

            return NxmlDocument(doc)
        }
    }
}
