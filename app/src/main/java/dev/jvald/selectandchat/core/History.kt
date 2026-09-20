package dev.jvald.selectandchat.core

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * A number this app has opened a chat with.
 *
 * The label is the point of the whole feature: every number here is one the user chose
 * *not* to save as a contact, so the digits alone are unidentifiable a few days later.
 */
data class HistoryEntry(
    val e164: String,
    val label: String?,
    val lastOpenedAt: Long,
)

/** Pure encode/decode, split out so it can be tested without an Android context. */
object HistoryCodec {

    fun encode(entries: List<HistoryEntry>): String {
        val array = JSONArray()
        for (entry in entries) {
            array.put(
                JSONObject()
                    .put(KEY_NUMBER, entry.e164)
                    .put(KEY_LABEL, entry.label ?: JSONObject.NULL)
                    .put(KEY_AT, entry.lastOpenedAt),
            )
        }
        return array.toString()
    }

    fun decode(raw: String?): List<HistoryEntry> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            (0 until array.length()).mapNotNull { i ->
                val item = array.optJSONObject(i) ?: return@mapNotNull null
                val number = item.optString(KEY_NUMBER).takeIf { it.isNotBlank() }
                    ?: return@mapNotNull null
                HistoryEntry(
                    e164 = number,
                    label = if (item.isNull(KEY_LABEL)) {
                        null
                    } else {
                        item.optString(KEY_LABEL).takeIf { it.isNotBlank() }
                    },
                    lastOpenedAt = item.optLong(KEY_AT),
                )
            }
        }.getOrDefault(emptyList())
    }

    private const val KEY_NUMBER = "n"
    private const val KEY_LABEL = "l"
    private const val KEY_AT = "t"
}

class HistoryStore(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("select_and_chat_history", Context.MODE_PRIVATE)

    /**
     * History is the first thing this app retains about the user, so it can be switched
     * off outright. Turning it off also discards what was already collected: a toggle
     * that left the old data behind would be a lie.
     */
    var enabled: Boolean
        get() = prefs.getBoolean(KEY_ENABLED, true)
        set(value) {
            prefs.edit().putBoolean(KEY_ENABLED, value).apply()
            if (!value) clear()
        }

    fun all(): List<HistoryEntry> =
        HistoryCodec.decode(prefs.getString(KEY_ENTRIES, null))
            .sortedByDescending { it.lastOpenedAt }

    /** Records an open, keeping any label already attached to that number. */
    fun record(e164: String) {
        if (!enabled) return
        val byNumber = all().associateBy { it.e164 }.toMutableMap()
        byNumber[e164] = HistoryEntry(
            e164 = e164,
            label = byNumber[e164]?.label,
            lastOpenedAt = System.currentTimeMillis(),
        )
        write(byNumber.values.sortedByDescending { it.lastOpenedAt }.take(MAX_ENTRIES))
    }

    fun setLabel(e164: String, label: String?) {
        val cleaned = label?.trim()?.takeIf { it.isNotEmpty() }
        write(all().map { if (it.e164 == e164) it.copy(label = cleaned) else it })
    }

    fun remove(e164: String) = write(all().filterNot { it.e164 == e164 })

    fun clear() = prefs.edit().remove(KEY_ENTRIES).apply()

    private fun write(entries: List<HistoryEntry>) =
        prefs.edit().putString(KEY_ENTRIES, HistoryCodec.encode(entries)).apply()

    private companion object {
        const val KEY_ENTRIES = "entries"
        const val KEY_ENABLED = "enabled"
        const val MAX_ENTRIES = 50
    }
}
