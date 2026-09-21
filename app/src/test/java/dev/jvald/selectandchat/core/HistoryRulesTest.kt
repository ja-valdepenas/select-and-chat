package dev.jvald.selectandchat.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HistoryRulesTest {

    @Test
    fun `opening the same number again moves it to the top without duplicating it`() {
        var entries = HistoryRules.record(emptyList(), "+50362041006", now = 1_000)
        entries = HistoryRules.record(entries, "+50370000000", now = 2_000)
        entries = HistoryRules.record(entries, "+50362041006", now = 3_000)

        assertEquals(2, entries.size)
        assertEquals("+50362041006", entries.first().e164)
    }

    @Test
    fun `a label survives the number being opened again`() {
        val labelled = HistoryRules.setLabel(
            HistoryRules.record(emptyList(), "+50362041006", now = 1_000),
            "+50362041006",
            "Plumber",
        )
        val reopened = HistoryRules.record(labelled, "+50362041006", now = 2_000)
        assertEquals("Plumber", reopened.single().label)
    }

    @Test
    fun `a blank label clears the label rather than storing whitespace`() {
        val entries = HistoryRules.record(emptyList(), "+50362041006", now = 1_000)
        assertNull(HistoryRules.setLabel(entries, "+50362041006", "   ").single().label)
    }

    @Test
    fun `the list stops at the ceiling, keeping the most recent`() {
        var entries = emptyList<HistoryEntry>()
        repeat(HistoryRules.MAX_ENTRIES + 10) {
            entries = HistoryRules.record(entries, "+5036204$it", now = it.toLong())
        }
        assertEquals(HistoryRules.MAX_ENTRIES, entries.size)
    }

    @Test
    fun `removing one number leaves the rest alone`() {
        var entries = HistoryRules.record(emptyList(), "+50362041006", now = 1_000)
        entries = HistoryRules.record(entries, "+50370000000", now = 2_000)
        assertEquals(
            listOf("+50370000000"),
            HistoryRules.remove(entries, "+50362041006").map { it.e164 },
        )
    }
}
