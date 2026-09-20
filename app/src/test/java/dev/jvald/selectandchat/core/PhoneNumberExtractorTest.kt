package dev.jvald.selectandchat.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PhoneNumberExtractorTest {

    private fun e164s(text: String, region: String?) =
        PhoneNumberExtractor.extract(text, region).candidates.map { it.e164 }

    @Test
    fun `international number needs no region`() {
        assertEquals(listOf("+34612345678"), e164s("+34 612 345 678", null))
    }

    @Test
    fun `national number uses the configured region`() {
        assertEquals(listOf("+34612345678"), e164s("612 345 678", "ES"))
    }

    @Test
    fun `number embedded in prose is extracted`() {
        assertEquals(listOf("+34612345678"), e164s("call me at 612345678 tomorrow", "ES"))
    }

    @Test
    fun `several numbers in one selection are all returned in order`() {
        val found = e164s("Ana 612345678 or Luis 698765432", "ES")
        assertEquals(listOf("+34612345678", "+34698765432"), found)
    }

    @Test
    fun `the same number written twice is returned once`() {
        assertEquals(listOf("+34612345678"), e164s("612345678 / +34 612 345 678", "ES"))
    }

    @Test
    fun `prose with no number yields nothing`() {
        val result = PhoneNumberExtractor.extract("hello there, see you soon", "ES")
        assertTrue(result.candidates.isEmpty())
        assertFalse(result.needsRegion)
    }

    @Test
    fun `local number with no region set asks for a region`() {
        val result = PhoneNumberExtractor.extract("612345678", null)
        assertTrue(result.candidates.isEmpty())
        assertTrue(result.needsRegion)
        assertEquals("612345678", result.digitsHint)
    }

    @Test
    fun `international number never asks for a region`() {
        assertFalse(PhoneNumberExtractor.extract("+34612345678", null).needsRegion)
    }

    @Test
    fun `digits hint survives punctuation for manual prefill`() {
        assertEquals("612345678", PhoneNumberExtractor.extract("(612) 345-678", "ES").digitsHint)
    }

    @Test
    fun `candidates carry display formats and a region`() {
        val candidate = PhoneNumberExtractor.extract("+34612345678", null).candidates.single()
        assertEquals("ES", candidate.regionCode)
        assertTrue(candidate.international.startsWith("+34"))
        assertTrue(candidate.national.isNotBlank())
    }

    @Test
    fun `us number resolves`() {
        assertEquals(listOf("+16502530000"), e164s("+1 650-253-0000", null))
    }

    @Test
    fun `empty and blank selections are safe`() {
        assertTrue(PhoneNumberExtractor.extract("", "ES").candidates.isEmpty())
        assertTrue(PhoneNumberExtractor.extract("   ", "ES").candidates.isEmpty())
        assertTrue(PhoneNumberExtractor.extract(null, "ES").candidates.isEmpty())
    }

    @Test
    fun `too few digits is not a number`() {
        assertTrue(PhoneNumberExtractor.extract("123", "ES").candidates.isEmpty())
    }

    @Test
    fun `manual entry parses what the user typed`() {
        assertNotNull(PhoneNumberExtractor.parseManual("612345678", "ES"))
        assertNotNull(PhoneNumberExtractor.parseManual("+34 612 345 678", null))
        assertNull(PhoneNumberExtractor.parseManual("12", "ES"))
        assertNull(PhoneNumberExtractor.parseManual("", "ES"))
    }

    @Test
    fun `non-numbers do not resolve`() {
        val notNumbers = listOf(
            "12/03/2024",
            "Total: 1.234,56",
            "2024-09-19",
            "IBAN ES9121000418450200051332",
            "version 8.13.2",
            "1234567890123456",
        )
        for (text in notNumbers) {
            assertTrue(text, PhoneNumberExtractor.extract(text, "ES").candidates.isEmpty())
        }
    }

    /**
     * An order number is the one false positive the forgiving fallback pass really does
     * produce: nine digits is a valid Spanish length. It must come back unconfident so the
     * caller shows it instead of opening a chat with it.
     */
    @Test
    fun `order number resolves but is not confident`() {
        val candidate = PhoneNumberExtractor
            .extract("Order #100045678 shipped", "ES")
            .candidates
            .single()
        assertEquals("+34100045678", candidate.e164)
        assertFalse(candidate.confident)
    }

    @Test
    fun `real numbers are confident`() {
        assertTrue(PhoneNumberExtractor.extract("+34 612 345 678", null).candidates.single().confident)
        assertTrue(PhoneNumberExtractor.extract("612 345 678", "ES").candidates.single().confident)
        assertTrue(
            PhoneNumberExtractor.extract("call me at 612345678 tomorrow", "ES")
                .candidates.single().confident,
        )
        assertTrue(PhoneNumberExtractor.parseManual("612345678", "ES")!!.confident)
    }

    // --- per-country length feedback -------------------------------------------------

    @Test
    fun `el salvador numbers are eight digits`() {
        assertEquals(8, PhoneNumberExtractor.checkLength("7777", "SV").expectedDigits)
    }

    @Test
    fun `length feedback tracks digits typed for SV`() {
        fun fb(n: String) = PhoneNumberExtractor.checkLength(n, "SV")
        assertEquals(LengthCheck.EMPTY, fb("").check)
        assertEquals(LengthCheck.TOO_SHORT, fb("7777").check)
        assertEquals(LengthCheck.OK, fb("77778888").check)

        // Nine digits is not merely "over the maximum" for SV, which has more than one
        // valid length, so libphonenumber reports INVALID_LENGTH. Either way the user
        // must be warned.
        assertEquals(LengthCheck.WRONG_LENGTH, fb("777788889").check)
        assertEquals(LengthCheck.TOO_LONG, fb("7777888899999").check)
    }

    @Test
    fun `only unusable lengths raise a warning`() {
        fun warns(n: String, region: String) =
            PhoneNumberExtractor.checkLength(n, region).shouldWarn

        // Still typing must stay silent.
        assertFalse(warns("", "SV"))
        assertFalse(warns("7777", "SV"))
        assertFalse(warns("77778888", "SV"))

        // Anything that cannot dial must warn, whichever reason libphonenumber gives.
        assertTrue(warns("777788889", "SV"))
        assertTrue(warns("7777888899999", "SV"))
        assertTrue(warns("65025300001", "US"))
        assertFalse(warns("6502530000", "US"))
    }

    @Test
    fun `length feedback adapts to a different country`() {
        fun check(n: String) = PhoneNumberExtractor.checkLength(n, "US").check
        // US numbers are ten digits, so eight is short here but fine in SV.
        assertEquals(LengthCheck.TOO_SHORT, check("77778888"))
        assertEquals(LengthCheck.OK, check("6502530000"))
        assertEquals(LengthCheck.TOO_LONG, check("65025300001"))
        assertEquals(10, PhoneNumberExtractor.checkLength("650", "US").expectedDigits)
    }

    @Test
    fun `length feedback is unknown without a country`() {
        assertEquals(LengthCheck.UNKNOWN, PhoneNumberExtractor.checkLength("77778888", null).check)
    }

    /** Prints real expected lengths across regions, so the hint text can be sanity checked. */
    @Test
    fun `probe expected digits across regions`() {
        println("--- expected digits ---")
        for (iso in listOf("SV", "US", "ES", "GB", "MX", "DE", "IN", "BR", "JP", "AU")) {
            val fb = PhoneNumberExtractor.checkLength("1", iso)
            println("%s -> %s".format(iso, fb.expectedDigits))
        }
        println("--- end ---")
    }
}
