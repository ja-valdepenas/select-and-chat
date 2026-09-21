package dev.jvald.selectandchat.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class CountriesTest {

    private val english = Locale.ENGLISH
    private val spanish = Locale("es")

    @Test
    fun `searching by localized name finds the country`() {
        val results = Countries.search("El Salvador", english)
        assertEquals("SV", results.first().iso)
    }

    @Test
    fun `the Spanish name is what matches in Spanish`() {
        assertTrue(Countries.search("Alemania", spanish).any { it.iso == "DE" })
        assertTrue(Countries.search("Germany", english).any { it.iso == "DE" })
    }

    @Test
    fun `searching by calling code works with and without the plus`() {
        assertTrue(Countries.search("+503", english).any { it.iso == "SV" })
        assertTrue(Countries.search("503", english).any { it.iso == "SV" })
    }

    @Test
    fun `searching by ISO code works`() {
        assertTrue(Countries.search("sv", english).any { it.iso == "SV" })
    }

    @Test
    fun `an empty query lists everything`() {
        assertEquals(Countries.all(english).size, Countries.search("   ", english).size)
    }

    @Test
    fun `nonsense matches nothing rather than everything`() {
        assertTrue(Countries.search("zzzzqq", english).isEmpty())
    }

    @Test
    fun `country names follow the locale asked for, not the one asked for first`() {
        val inEnglish = Countries.byIso("DE", english)!!.displayName
        val inSpanish = Countries.byIso("DE", spanish)!!.displayName
        assertNotEquals(inEnglish, inSpanish)
        // Asking again in the first locale must not return the cached second one.
        assertEquals(inEnglish, Countries.byIso("DE", english)!!.displayName)
    }

    @Test
    fun `the calling code is what the pill shows`() {
        assertEquals("+503", Countries.byIso("SV", english)!!.dialCode)
    }

    @Test
    fun `an unknown or missing ISO code resolves to nothing`() {
        assertNull(Countries.byIso(null, english))
        assertNull(Countries.byIso("", english))
        assertNull(Countries.byIso("XX", english))
    }
}
