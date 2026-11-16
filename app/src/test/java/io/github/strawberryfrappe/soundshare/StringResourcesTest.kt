package io.github.strawberryfrappe.soundshare

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element

/**
 * GATE-L10N: every English text has a Spanish one and the reverse, with the
 * same placeholders, so no screen falls back to English and no `%d` is fed a
 * string at run time (which would crash).
 */
class StringResourcesTest {
    private val english = read("src/main/res/values/strings.xml")
    private val spanish = read("src/main/res/values-es/strings.xml")

    @Test
    fun everyTextExistsInBothLanguages() {
        assertTrue("English strings not found", english.isNotEmpty())
        assertEquals("missing in Spanish", emptySet<String>(), english.keys - spanish.keys)
        assertEquals("missing in English", emptySet<String>(), spanish.keys - english.keys)
    }

    @Test
    fun untranslatableTextIsNotTranslated() {
        val document = DocumentBuilderFactory.newInstance().newDocumentBuilder()
            .parse(File("src/main/res/values/strings.xml"))
        val strings = document.getElementsByTagName("string")
        val fixed = (0 until strings.length).map { strings.item(it) as Element }
            .filter { it.getAttribute("translatable") == "false" }
            .map { it.getAttribute("name") }
        assertEquals("translatable=\"false\" texts must not be in Spanish", emptySet<String>(), fixed.toSet() intersect spanish.keys)
    }

    @Test
    fun placeholdersMatch() {
        for ((name, placeholders) in english) {
            val other = spanish[name] ?: continue
            assertEquals("placeholders of $name", placeholders, other)
        }
    }

    /** Text name to its sorted placeholders. Plurals contribute their "other" form. */
    private fun read(path: String): Map<String, List<String>> {
        val file = File(path)
        assertTrue("run from the app module: ${file.absolutePath}", file.exists())
        val document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
        val result = mutableMapOf<String, List<String>>()
        val strings = document.getElementsByTagName("string")
        for (i in 0 until strings.length) {
            val element = strings.item(i) as Element
            if (element.getAttribute("translatable") == "false") continue
            result[element.getAttribute("name")] = placeholders(element.textContent)
        }
        val plurals = document.getElementsByTagName("plurals")
        for (i in 0 until plurals.length) {
            val element = plurals.item(i) as Element
            val items = element.getElementsByTagName("item")
            val other = (0 until items.length).map { items.item(it) as Element }
                .firstOrNull { it.getAttribute("quantity") == "other" }
            result["plurals/" + element.getAttribute("name")] = placeholders(other?.textContent.orEmpty())
        }
        return result
    }

    private fun placeholders(text: String): List<String> =
        Regex("%(\\d+\\$)?[-#+ 0,(]*\\d*(\\.\\d+)?[a-zA-Z%]").findAll(text)
            .map { it.value }
            .filter { it != "%%" }
            .sorted()
            .toList()
}
