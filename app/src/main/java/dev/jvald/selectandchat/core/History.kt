package dev.jvald.selectandchat.core

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

/**
 * The list operations, pure so they can be tested directly. Persistence is
 * [SettingsRepository]'s job; whether recording is allowed at all is decided there too.
 */
object HistoryRules {

    const val MAX_ENTRIES = 50

    /** Records an open, keeping any label already attached to that number. */
    fun record(
        current: List<HistoryEntry>,
        e164: String,
        now: Long = System.currentTimeMillis(),
    ): List<HistoryEntry> {
        val byNumber = current.associateBy { it.e164 }.toMutableMap()
        byNumber[e164] = HistoryEntry(
            e164 = e164,
            label = byNumber[e164]?.label,
            lastOpenedAt = now,
        )
        return byNumber.values.sortedByDescending { it.lastOpenedAt }.take(MAX_ENTRIES)
    }

    fun setLabel(
        current: List<HistoryEntry>,
        e164: String,
        label: String?,
    ): List<HistoryEntry> {
        val cleaned = label?.trim()?.takeIf { it.isNotEmpty() }
        return current.map { if (it.e164 == e164) it.copy(label = cleaned) else it }
    }

    fun remove(current: List<HistoryEntry>, e164: String): List<HistoryEntry> =
        current.filterNot { it.e164 == e164 }
}
