package dev.jvald.selectandchat.core

import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * A canned opening message. WhatsApp accepts these through the wa.me `text` parameter, so
 * one costs a URL parameter and no extra permission or storage beyond the text itself.
 *
 * The [id] exists so a message can be edited in place: the text is the thing the user
 * typed and is never normalised, translated or deduplicated against a localized list.
 */
data class QuickMessage(
    val id: String,
    val text: String,
) {
    companion object {
        fun of(text: String) = QuickMessage(UUID.randomUUID().toString(), text.trim())
    }
}

/** Pure encode/decode, split out so it can be tested without an Android context. */
object QuickMessageCodec {

    const val MAX_MESSAGES = 30

    fun encode(messages: List<QuickMessage>): String {
        val array = JSONArray()
        for (message in messages) {
            array.put(JSONObject().put(KEY_ID, message.id).put(KEY_TEXT, message.text))
        }
        return array.toString()
    }

    /**
     * Also reads the shape this app shipped first, a bare `["hello", …]` array of strings,
     * so upgrading does not silently drop the messages someone already wrote.
     */
    fun decode(raw: String?): List<QuickMessage> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            (0 until array.length()).mapNotNull { i ->
                val item = array.opt(i)
                when (item) {
                    is JSONObject -> {
                        val text = item.optString(KEY_TEXT).takeIf { it.isNotBlank() }
                            ?: return@mapNotNull null
                        val id = item.optString(KEY_ID).takeIf { it.isNotBlank() }
                            ?: UUID.randomUUID().toString()
                        QuickMessage(id, text)
                    }

                    is String -> item.takeIf { it.isNotBlank() }?.let(QuickMessage::of)
                    else -> null
                }
            }
        }.getOrDefault(emptyList())
    }

    private const val KEY_ID = "i"
    private const val KEY_TEXT = "t"
}

/**
 * The list operations, kept pure so the rules — no blanks, no duplicate text, a ceiling on
 * how many are kept — are testable on their own and the repository only has to persist.
 */
object QuickMessageRules {

    fun add(current: List<QuickMessage>, text: String): List<QuickMessage> {
        val cleaned = text.trim()
        if (cleaned.isEmpty()) return current
        if (current.any { it.text == cleaned }) return current
        return (current + QuickMessage.of(cleaned)).take(QuickMessageCodec.MAX_MESSAGES)
    }

    fun edit(current: List<QuickMessage>, id: String, text: String): List<QuickMessage> {
        val cleaned = text.trim()
        if (cleaned.isEmpty()) return current
        return current.map { if (it.id == id) it.copy(text = cleaned) else it }
    }

    fun remove(current: List<QuickMessage>, id: String): List<QuickMessage> =
        current.filterNot { it.id == id }

    /** Puts a deleted message back where it was, which is what makes Undo truthful. */
    fun restore(
        current: List<QuickMessage>,
        message: QuickMessage,
        index: Int,
    ): List<QuickMessage> {
        if (current.any { it.id == message.id }) return current
        val at = index.coerceIn(0, current.size)
        return current.toMutableList().apply { add(at, message) }
    }
}
