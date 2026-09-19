package dev.jvald.selectandchat.core

import android.content.Context
import com.google.i18n.phonenumbers.PhoneNumberUtil

/**
 * Two settings, read on the hot path.
 *
 * SharedPreferences rather than DataStore on purpose: this is read synchronously inside
 * [dev.jvald.selectandchat.ui.ProcessTextActivity.onCreate] before deciding whether to
 * show any UI at all. DataStore's suspend API would force a coroutine hop first and
 * reintroduce exactly the visible flash this design avoids.
 */
class Prefs(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("select_and_chat", Context.MODE_PRIVATE)

    /** ISO 3166-1 alpha-2, or null if never set and the device locale gave us nothing. */
    var region: String?
        get() = prefs.getString(KEY_REGION, null)
        set(value) = prefs.edit().putString(KEY_REGION, value).apply()

    var preferredFlavor: WhatsAppFlavor
        get() = prefs.getString(KEY_FLAVOR, null)
            ?.let { name -> WhatsAppFlavor.entries.firstOrNull { it.name == name } }
            ?: WhatsAppFlavor.STANDARD
        set(value) = prefs.edit().putString(KEY_FLAVOR, value.name).apply()

    /**
     * Seeds the country once from the device locale. The country is a manual setting: after
     * this first guess it only ever changes because the user changed it, so travelling or
     * swapping SIMs never silently rewrites numbers.
     */
    fun seedRegionIfUnset(context: Context) {
        if (region != null) return
        val supported = PhoneNumberUtil.getInstance().supportedRegions
        val locales = context.resources.configuration.locales
        for (i in 0 until locales.size()) {
            val country = locales[i].country.uppercase()
            if (country.length == 2 && country in supported) {
                region = country
                return
            }
        }
    }

    private companion object {
        const val KEY_REGION = "region"
        const val KEY_FLAVOR = "preferred_flavor"
    }
}
