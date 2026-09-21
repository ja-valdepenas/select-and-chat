package dev.jvald.selectandchat.core

import android.app.LocaleManager
import android.content.Context
import android.os.LocaleList

/**
 * The language options offered in Settings.
 *
 * This is *not* stored by the app. The selection lives in the platform's own per-app
 * locale store, so the value this menu writes is the same one System settings > Apps >
 * Select & Chat > Language shows and edits. [tag] is the BCP-47 tag handed to it;
 * [SYSTEM] clears the override instead of writing one, which is what keeps a first launch
 * following the device.
 */
enum class AppLanguage(val tag: String?) {
    SYSTEM(null),
    ENGLISH("en"),
    SPANISH("es"),
    ;

    companion object {
        /**
         * Maps whatever the platform reports back onto one of the three options. Matching
         * on the language subtag alone is deliberate: the stored tag can come back as
         * "es-419" or "en-US" depending on the device and how it was set.
         */
        fun fromTag(tag: String?): AppLanguage {
            val language = tag?.takeIf { it.isNotBlank() }?.substringBefore('-')?.lowercase()
                ?: return SYSTEM
            return entries.firstOrNull { it.tag == language } ?: SYSTEM
        }
    }
}

/**
 * Reads and writes the per-app language through [LocaleManager], which exists from
 * Android 13 — the minimum this app supports, and the reason it needs no AppCompat.
 * Setting it restarts the activity, so nothing here has to re-resolve resources by hand.
 */
object AppLocales {

    fun current(context: Context): AppLanguage =
        AppLanguage.fromTag(localeManager(context)?.applicationLocales?.toLanguageTags())

    fun apply(context: Context, language: AppLanguage) {
        localeManager(context)?.applicationLocales = language.tag
            ?.let { LocaleList.forLanguageTags(it) }
            ?: LocaleList.getEmptyLocaleList()
    }

    private fun localeManager(context: Context): LocaleManager? =
        context.getSystemService(LocaleManager::class.java)
}
