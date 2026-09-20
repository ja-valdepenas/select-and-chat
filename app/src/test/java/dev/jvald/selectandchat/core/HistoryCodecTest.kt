package dev.jvald.selectandchat.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HistoryCodecTest {

    @Test
    fun `entries survive a round trip`() {
        val entries = listOf(
            HistoryEntry("+50377778888", "Plumber", 1_700_000_000_000),
            HistoryEntry("+16502530000", null, 1_600_000_000_000),
        )
        assertEquals(entries, HistoryCodec.decode(HistoryCodec.encode(entries)))
    }

    @Test
    fun `a missing label stays null rather than becoming the text null`() {
        val decoded = HistoryCodec
            .decode(HistoryCodec.encode(listOf(HistoryEntry("+50377778888", null, 1L))))
            .single()
        assertNull(decoded.label)
    }

    @Test
    fun `corrupt or empty storage degrades to an empty list`() {
        assertTrue(HistoryCodec.decode(null).isEmpty())
        assertTrue(HistoryCodec.decode("").isEmpty())
        assertTrue(HistoryCodec.decode("not json at all").isEmpty())
        assertTrue(HistoryCodec.decode("{\"unexpected\":true}").isEmpty())
    }

    @Test
    fun `entries without a number are dropped`() {
        assertTrue(HistoryCodec.decode("[{\"l\":\"Plumber\",\"t\":1}]").isEmpty())
    }
}
