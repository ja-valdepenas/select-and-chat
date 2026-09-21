package dev.jvald.selectandchat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Guards the two localization rules that are easy to break and invisible until someone
 * runs the app in the other language: every English string has a Spanish one, and no
 * user-visible text is written in a composable.
 */
class ResourcesTest {

    private val moduleDir = listOf(File("."), File("app"))
        .first { File(it, "src/main/res/values/strings.xml").exists() }

    private val english = File(moduleDir, "src/main/res/values/strings.xml")
    private val spanish = File(moduleDir, "src/main/res/values-es/strings.xml")
    private val uiSources = File(moduleDir, "src/main/java/dev/jvald/selectandchat/ui")

    @Test
    fun `every translatable English string has a Spanish translation`() {
        val missing = translatableNames(english) - names(spanish)
        assertEquals("Missing from values-es/strings.xml: $missing", emptySet<String>(), missing)
    }

    @Test
    fun `the Spanish file has no strings the English one does not`() {
        val extra = names(spanish) - names(english)
        assertEquals("Not in values/strings.xml: $extra", emptySet<String>(), extra)
    }

    @Test
    fun `strings marked untranslatable are not translated`() {
        val untranslatable = names(english) - translatableNames(english)
        val translated = untranslatable intersect names(spanish)
        assertEquals(
            "Marked translatable=false but present in values-es: $translated",
            emptySet<String>(),
            translated,
        )
    }

    @Test
    fun `the strings the design calls for all exist`() {
        val required = listOf(
            "app_title", "tab_new_chat", "tab_recents", "more_options", "language", "theme",
            "system_default", "light_theme", "dark_theme", "app_default_theme", "amoled_theme",
            "settings", "manage_quick_messages",
            "quick_messages", "quick_messages_helper", "add_quick_message", "quick_message",
            "edit", "delete", "message_deleted", "undo", "selected_message_helper",
            "phone_number", "select_country", "search_country_or_code", "open_in_whatsapp",
            "open_in_whatsapp_business", "save_recent_chats", "stored_on_device",
            "recent_chats", "just_now", "manage_history", "manage_history_helper",
            "clear_history", "cancel", "save", "not_affiliated",
        )
        val present = names(english)
        assertEquals(
            "Missing from values/strings.xml: ${required.filterNot { it in present }}",
            emptyList<String>(),
            required.filterNot { it in present },
        )
    }

    @Test
    fun `no user-visible text is hardcoded in a composable`() {
        val prose = Regex("""[A-Za-z]{3}""")
        val literal = Regex("""(?:Text\(|contentDescription\s*=\s*)"([^"$\\]*)"""")

        val offenders = uiSources.walkTopDown()
            .filter { it.extension == "kt" }
            .flatMap { file ->
                literal.findAll(file.readText())
                    .map { it.groupValues[1] }
                    .filter { prose.containsMatchIn(it) }
                    .map { "${file.name}: \"$it\"" }
            }
            .toList()

        assertEquals("Hardcoded user-visible text: $offenders", emptyList<String>(), offenders)
    }

    @Test
    fun `the unqualified resource locale is declared, which generateLocaleConfig requires`() {
        val properties = File(moduleDir, "src/main/res/resources.properties")
        assertTrue("src/main/res/resources.properties is missing", properties.exists())
        assertTrue(properties.readText().contains("unqualifiedResLocale="))
    }

    /** Plurals count too: a missing quantity string is just as invisible as a missing one. */
    private fun names(file: File): Set<String> =
        Regex("""<(?:string|plurals)\s+name="([^"]+)"""").findAll(file.readText())
            .map { it.groupValues[1] }
            .toSet()

    private fun translatableNames(file: File): Set<String> =
        Regex("""<(?:string|plurals)\s+name="([^"]+)"([^>]*)>""").findAll(file.readText())
            .filterNot { it.groupValues[2].contains("translatable=\"false\"") }
            .map { it.groupValues[1] }
            .toSet()
}
