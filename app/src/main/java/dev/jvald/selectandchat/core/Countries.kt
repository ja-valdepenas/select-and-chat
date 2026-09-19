package dev.jvald.selectandchat.core

import com.google.i18n.phonenumbers.PhoneNumberUtil
import java.util.Locale

data class Country(
    val iso: String,
    val callingCode: Int,
    val displayName: String,
    val flag: String,
) {
    val label: String get() = "$flag  $displayName  +$callingCode"
}

/**
 * The country list, derived at runtime rather than bundled.
 *
 * libphonenumber already knows every region and its dialing code, the JDK already has
 * localized country names, and flags are just the ISO code as regional-indicator
 * codepoints — so there is no countries.json and no flag images to ship or maintain.
 */
object Countries {

    val all: List<Country> by lazy {
        val util = PhoneNumberUtil.getInstance()
        util.supportedRegions
            .map { iso ->
                Country(
                    iso = iso,
                    callingCode = util.getCountryCodeForRegion(iso),
                    displayName = Locale("", iso).displayCountry.ifBlank { iso },
                    flag = flagEmoji(iso),
                )
            }
            .sortedBy { it.displayName.lowercase() }
    }

    fun byIso(iso: String?): Country? {
        if (iso.isNullOrBlank()) return null
        return all.firstOrNull { it.iso.equals(iso, ignoreCase = true) }
    }

    fun search(query: String): List<Country> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return all
        return all.filter {
            it.displayName.lowercase().contains(q) ||
                it.iso.lowercase().startsWith(q) ||
                "+${it.callingCode}".startsWith(q) ||
                it.callingCode.toString().startsWith(q)
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
