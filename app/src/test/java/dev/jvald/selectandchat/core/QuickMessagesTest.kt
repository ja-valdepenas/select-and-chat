package dev.jvald.selectandchat.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QuickMessageCodecTest {

    @Test
    fun `a saved list comes back exactly as it was written`() {
        val messages = listOf(
            QuickMessage("a", "Hola, ¿cómo estás?"),
            QuickMessage("b", "On my way"),
        )
        assertEquals(messages, QuickMessageCodec.decode(QuickMessageCodec.encode(messages)))
    }

    @Test
    fun `user text is stored verbatim, including characters a URL would eat`() {
        val text = "Cost & time? 100% sure — #1 🙂"
        val decoded = QuickMessageCodec.decode(
            QuickMessageCodec.encode(listOf(QuickMessage("a", text))),
        )
        assertEquals(text, decoded.single().text)
    }

    @Test
    fun `the old bare-string format is still readable, so nobody loses their messages`() {
        val decoded = QuickMessageCodec.decode("""["Hola","On my way"]""")
        assertEquals(listOf("Hola", "On my way"), decoded.map { it.text })
        assertTrue(decoded.all { it.id.isNotBlank() })
    }

    @Test
    fun `junk on disk decodes to nothing rather than crashing`() {
        assertEquals(emptyList<QuickMessage>(), QuickMessageCodec.decode("not json at all"))
        assertEquals(emptyList<QuickMessage>(), QuickMessageCodec.decode(null))
        assertEquals(emptyList<QuickMessage>(), QuickMessageCodec.decode(""))
    }
}

class QuickMessageRulesTest {

    private val hello = QuickMessage("a", "Hola")
    private val onMyWay = QuickMessage("b", "On my way")

    @Test
    fun `adding trims but does not otherwise rewrite what was typed`() {
        val result = QuickMessageRules.add(emptyList(), "  ¿Podemos hablar?  ")
        assertEquals("¿Podemos hablar?", result.single().text)
    }

    @Test
    fun `blank text adds nothing`() {
        assertEquals(emptyList<QuickMessage>(), QuickMessageRules.add(emptyList(), "   "))
    }

    @Test
    fun `the same message is not added twice`() {
        val once = QuickMessageRules.add(emptyList(), "Hola")
        assertEquals(1, QuickMessageRules.add(once, "Hola").size)
    }

    @Test
    fun `editing keeps the id, so the selection survives a rewording`() {
        val edited = QuickMessageRules.edit(listOf(hello, onMyWay), "a", "Hola de nuevo")
        assertEquals("a", edited.first().id)
        assertEquals("Hola de nuevo", edited.first().text)
        assertEquals(onMyWay, edited.last())
    }

    @Test
    fun `undo puts the message back where it was, not at the end`() {
        val current = listOf(hello, onMyWay)
        val afterDelete = QuickMessageRules.remove(current, "a")
        assertEquals(listOf(onMyWay), afterDelete)
        assertEquals(current, QuickMessageRules.restore(afterDelete, hello, index = 0))
    }

    @Test
    fun `undo twice does not duplicate the message`() {
        val restored = QuickMessageRules.restore(listOf(onMyWay), hello, 0)
        assertEquals(restored, QuickMessageRules.restore(restored, hello, 0))
    }

    @Test
    fun `the list stops growing at the ceiling`() {
        var list = emptyList<QuickMessage>()
        repeat(QuickMessageCodec.MAX_MESSAGES + 5) { list = QuickMessageRules.add(list, "m$it") }
        assertEquals(QuickMessageCodec.MAX_MESSAGES, list.size)
    }
}
