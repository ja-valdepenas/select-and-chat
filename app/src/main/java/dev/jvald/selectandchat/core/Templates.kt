package dev.jvald.selectandchat.core

import android.content.Context
import org.json.JSONArray

/**
 * Canned opening messages. WhatsApp accepts these through the wa.me `text` parameter, so
 * a template costs one URL parameter and no extra permission or storage beyond the text.
 */
class TemplateStore(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("select_and_chat_templates", Context.MODE_PRIVATE)

    fun all(): List<String> = decode(prefs.getString(KEY_TEMPLATES, null))

    fun add(text: String) {
        val cleaned = text.trim()
        if (cleaned.isEmpty()) return
        val current = all()
        if (cleaned in current) return
        write((current + cleaned).take(MAX_TEMPLATES))
    }

    fun remove(text: String) = write(all().filterNot { it == text })

    private fun write(items: List<String>) =
        prefs.edit().putString(KEY_TEMPLATES, JSONArray(items).toString()).apply()

    private fun decode(raw: String?): List<String> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            (0 until array.length()).mapNotNull { array.optString(it).takeIf(String::isNotBlank) }
        }.getOrDefault(emptyList())
    }

    private companion object {
        const val KEY_TEMPLATES = "templates"
        const val MAX_TEMPLATES = 20
    }
}
