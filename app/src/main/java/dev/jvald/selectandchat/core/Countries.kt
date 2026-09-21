package dev.jvald.selectandchat.core

import com.google.i18n.phonenumbers.PhoneNumberUtil
import java.util.Locale

data class Country(
    val iso: String,
    val callingCode: Int,
    val displayName: String,
    val flag: String,
) {
    val dialCode: String get() = "+$callingCode"
}

/**
 * The country list, derived at runtime rather than bundled.
 *
 * libphonenumber already knows every region and its dialing code, the JDK already has
 * localized country names, and flags are just the ISO code as regional-indicator
 * codepoints — so there is no countries.json and no flag images to ship or maintain.
 *
 * Everything is a function of a [Locale] rather than a lazy singleton: the app's language
 * can change while it is running, and a list built once at first use would keep showing
 * "Germany" after the user switched to Spanish. One locale's list is cached, because in
 * practice the screen asks for the same one many times per frame.
 */
object Countries {

    @Volatile
    private var cache: Pair<Locale, List<Country>>? = null

    fun all(locale: Locale = Locale.getDefault()): List<Country> {
        cache?.let { (cached, list) -> if (cached == locale) return list }
        val util = PhoneNumberUtil.getInstance()
        val list = util.supportedRegions
            .map { iso ->
                Country(
                    iso = iso,
                    callingCode = util.getCountryCodeForRegion(iso),
                    displayName = Locale("", iso).getDisplayCountry(locale).ifBlank { iso },
                    flag = flagEmoji(iso),
                )
            }
            .sortedBy { it.displayName.lowercase(locale) }
        cache = locale to list
        return list
    }

    fun byIso(iso: String?, locale: Locale = Locale.getDefault()): Country? {
        if (iso.isNullOrBlank()) return null
        return all(locale).firstOrNull { it.iso.equals(iso, ignoreCase = true) }
    }

    /** Matches the localized name, the ISO code and the calling code, with or without "+". */
    fun search(query: String, locale: Locale = Locale.getDefault()): List<Country> {
        val q = query.trim().lowercase(locale)
        if (q.isEmpty()) return all(locale)
        val digits = q.removePrefix("+")
        return all(locale).filter {
            it.displayName.lowercase(locale).contains(q) ||
                it.iso.lowercase().startsWith(q) ||
                (digits.isNotEmpty() && it.callingCode.toString().startsWith(digits))
        }
    }

    private fun flagEmoji(iso: String): String {
        if (iso.length != 2) return "🏳"
        val base = 0x1F1E6 // REGIONAL INDICATOR SYMBOL LETTER A
        return iso.uppercase()
            .map { base + (it - 'A') }
            .joinToString("") { String(Character.toChars(it)) }
    }
}
