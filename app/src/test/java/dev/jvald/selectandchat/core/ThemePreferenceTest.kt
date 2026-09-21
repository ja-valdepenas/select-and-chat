package dev.jvald.selectandchat.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemePreferenceTest {

    @Test
    fun `nothing stored means follow the system`() {
        assertEquals(ThemePreference.SYSTEM, ThemePreference.from(null))
    }

    @Test
    fun `system follows the device both ways`() {
        assertTrue(ThemePreference.SYSTEM.isDark(systemDark = true))
        assertFalse(ThemePreference.SYSTEM.isDark(systemDark = false))
    }

    @Test
    fun `an explicit choice ignores the device`() {
        assertFalse(ThemePreference.LIGHT.isDark(systemDark = true))
        assertTrue(ThemePreference.DARK.isDark(systemDark = false))
    }

    @Test
    fun `all three dark options are dark, whatever the device is doing`() {
        val dark = listOf(ThemePreference.DARK, ThemePreference.APP_DEFAULT, ThemePreference.AMOLED)
        for (theme in dark) {
            assertTrue(theme.name, theme.isDark(systemDark = false))
            assertTrue(theme.name, theme.isDark(systemDark = true))
        }
    }

    @Test
    fun `an explicit choice survives a round trip through storage`() {
        for (theme in ThemePreference.entries) {
            assertEquals(theme, ThemePreference.from(theme.name))
        }
    }

    @Test
    fun `the retired wallpaper theme goes back to following the system`() {
        assertEquals(ThemePreference.SYSTEM, ThemePreference.from("DYNAMIC"))
        assertEquals(ThemePreference.SYSTEM, ThemePreference.from("something else entirely"))
    }

    @Test
    fun `the name GREEN carried in an older build no longer resolves`() {
        // It is now APP_DEFAULT. Anyone who had it stored lands on SYSTEM, which in the
        // dark paints the same scheme, so the fallback is invisible rather than wrong.
        assertEquals(ThemePreference.SYSTEM, ThemePreference.from("GREEN"))
    }
}

class AppLanguageTest {

    @Test
    fun `no stored tag means follow the device`() {
        assertEquals(AppLanguage.SYSTEM, AppLanguage.fromTag(null))
        assertEquals(AppLanguage.SYSTEM, AppLanguage.fromTag(""))
    }

    @Test
    fun `a region-qualified tag still resolves to its language`() {
        assertEquals(AppLanguage.SPANISH, AppLanguage.fromTag("es-419"))
        assertEquals(AppLanguage.SPANISH, AppLanguage.fromTag("es"))
        assertEquals(AppLanguage.ENGLISH, AppLanguage.fromTag("en-US"))
    }

    @Test
    fun `a language this app does not ship falls back to the device`() {
        assertEquals(AppLanguage.SYSTEM, AppLanguage.fromTag("fr-FR"))
    }

    @Test
    fun `system default carries no tag, which is what clears the override`() {
        assertEquals(null, AppLanguage.SYSTEM.tag)
        assertEquals("en", AppLanguage.ENGLISH.tag)
        assertEquals("es", AppLanguage.SPANISH.tag)
    }
}
