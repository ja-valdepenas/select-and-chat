package dev.jvald.selectandchat.core

import android.content.Context
import android.telephony.TelephonyManager
import com.google.i18n.phonenumbers.PhoneNumberUtil

/**
 * Three settings, read on the hot path.
 *
 * SharedPreferences rather than DataStore on purpose: this is read synchronously inside
 * [dev.jvald.selectandchat.ui.ProcessTextActivity.onCreate] before deciding whether to
 * show any UI at all. DataStore's suspend API would force a coroutine hop first and
 * reintroduce exactly the visible flash this design avoids.
 */
class Prefs(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("select_and_chat", Context.MODE_PRIVATE)

    /** ISO 3166-1 alpha-2, or null if never set and detection found nothing. */
    var region: String?
        get() = prefs.getString(KEY_REGION, null)
        set(value) = prefs.edit().putString(KEY_REGION, value).apply()

    var preferredFlavor: WhatsAppFlavor
        get() = prefs.getString(KEY_FLAVOR, null)
            ?.let { name -> WhatsAppFlavor.entries.firstOrNull { it.name == name } }
            ?: WhatsAppFlavor.STANDARD
        set(value) = prefs.edit().putString(KEY_FLAVOR, value.name).apply()

    /** Appearance. Defaults to following the system, using the app's green palette. */
    var theme: AppTheme
        get() = AppTheme.from(prefs.getString(KEY_THEME, null))
        set(value) = prefs.edit().putString(KEY_THEME, value.name).apply()

    /**
     * Detects the country once, on first run. After this the country is a manual setting:
     * it only ever changes because the user changed it, so travelling or roaming never
     * silently rewrites the numbers you're about to message.
     */
    fun seedRegionIfUnset(context: Context) {
        if (region != null) return
        val supported = PhoneNumberUtil.getInstance().supportedRegions
        region = detectRegions(context).firstOrNull { it in supported }
    }

    /** Best guess first: the SIM, then the network, then the device locale. */
    private fun detectRegions(context: Context): List<String> {
        val candidates = mutableListOf<String?>()

        // Neither getter is permission-guarded, but OEM builds have been known to throw
        // here; a bad guess must never cost us the locale fallback.
        runCatching {
            val telephony = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
            candidates += telephony?.simCountryIso
            candidates += telephony?.networkCountryIso
        }

        val locales = context.resources.configuration.locales
        for (i in 0 until locales.size()) candidates += locales[i].country

        return candidates
            .filterNotNull()
            .map { it.uppercase() }
            .filter { it.length == 2 }
    }

    private companion object {
        const val KEY_REGION = "region"
        const val KEY_FLAVOR = "preferred_flavor"
        const val KEY_THEME = "theme"
    }
}
