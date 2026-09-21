package dev.jvald.selectandchat.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WhatsAppLauncherTest {

    @Test
    fun `a number with no message is a bare wa_me link`() {
        assertEquals(
            "https://wa.me/50362041006",
            WhatsAppLauncher.chatUrl("+503 6204 1006"),
        )
    }

    @Test
    fun `the selected quick message rides along url-encoded`() {
        val url = WhatsAppLauncher.chatUrl("+50362041006", "Hola, ¿cómo estás?")!!
        assertTrue(url.startsWith("https://wa.me/50362041006?text="))
        assertTrue(url.contains("%C2%BF")) // the inverted question mark
        assertTrue(url.contains("+")) // the space
    }

    @Test
    fun `characters that would break the query string are escaped, not dropped`() {
        val url = WhatsAppLauncher.chatUrl("+50362041006", "A&B #1 100%")!!
        assertTrue(url.contains("%26"))
        assertTrue(url.contains("%23"))
        assertTrue(url.contains("%25"))
    }

    @Test
    fun `a blank message adds no query string at all`() {
        assertEquals(
            "https://wa.me/50362041006",
            WhatsAppLauncher.chatUrl("+50362041006", "   "),
        )
    }

    @Test
    fun `a number with no digits has no link, so nothing is launched`() {
        assertNull(WhatsAppLauncher.chatUrl("not a number"))
        assertNull(WhatsAppLauncher.chatUrl(""))
    }

    @Test
    fun `an unknown stored flavor falls back to standard WhatsApp`() {
        assertEquals(WhatsAppFlavor.STANDARD, WhatsAppFlavor.from(null))
        assertEquals(WhatsAppFlavor.STANDARD, WhatsAppFlavor.from("TELEGRAM"))
        assertEquals(WhatsAppFlavor.BUSINESS, WhatsAppFlavor.from("BUSINESS"))
    }
}
