package dev.jvald.selectandchat.core

import android.content.Context
import android.telephony.TelephonyManager
import androidx.datastore.core.DataMigration
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.i18n.phonenumbers.PhoneNumberUtil
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

/**
 * Everything this app remembers, in one immutable snapshot.
 *
 * The language is deliberately absent: it lives in the platform locale store instead, so
 * that Android 13+ System settings and this app's menu are reading and writing the same
 * value rather than two that can disagree. See [AppLanguage].
 */
data class AppSettings(
    val theme: ThemePreference = ThemePreference.SYSTEM,
    /** ISO 3166-1 alpha-2, or null if never set and detection found nothing. */
    val region: String? = null,
    val flavor: WhatsAppFlavor = WhatsAppFlavor.STANDARD,
    val historyEnabled: Boolean = true,
    val howItWorksDismissed: Boolean = false,
    val quickMessages: List<QuickMessage> = emptyList(),
    val recents: List<HistoryEntry> = emptyList(),
)

private val Context.dataStore by preferencesDataStore(
    name = "settings",
    produceMigrations = { context -> listOf(LegacySharedPreferencesMigration(context)) },
)

class SettingsRepository(context: Context) {

    private val appContext = context.applicationContext
    private val store = appContext.dataStore

    val settings: Flow<AppSettings> = store.data.map(::toSettings)

    /**
     * A synchronous read, for [dev.jvald.selectandchat.ui.ProcessTextActivity] only.
     *
     * That activity decides whether to show any UI at all inside `onCreate`, and a
     * coroutine hop first would reintroduce exactly the visible flash the translucent
     * window exists to avoid. This is one file read on a cold start, which is what the
     * SharedPreferences it replaced was doing anyway.
     */
    fun blockingSnapshot(): AppSettings = runBlocking { settings.first() }

    suspend fun setTheme(theme: ThemePreference) = put { it[KEY_THEME] = theme.name }

    suspend fun setRegion(iso: String) = put { it[KEY_REGION] = iso }

    suspend fun setFlavor(flavor: WhatsAppFlavor) = put { it[KEY_FLAVOR] = flavor.name }

    suspend fun dismissHowItWorks() = put { it[KEY_HOW_IT_WORKS_DISMISSED] = true }

    /**
     * Stops new chats being recorded. It deliberately leaves what is already saved alone:
     * turning a switch off is not a request to delete, and Manage history is where
     * deleting lives.
     */
    suspend fun setHistoryEnabled(enabled: Boolean) = put { it[KEY_HISTORY_ENABLED] = enabled }

    suspend fun setQuickMessages(messages: List<QuickMessage>) =
        put { it[KEY_QUICK_MESSAGES] = QuickMessageCodec.encode(messages) }

    suspend fun recordChat(e164: String) = put { prefs ->
        if (prefs[KEY_HISTORY_ENABLED] != false) {
            prefs[KEY_RECENTS] = HistoryCodec.encode(
                HistoryRules.record(HistoryCodec.decode(prefs[KEY_RECENTS]), e164),
            )
        }
    }

    /**
     * The blocking equivalent, for the no-UI selection-toolbar path: that activity has
     * already fired the intent and is finishing, so a coroutine launched there would be
     * cancelled before it reached the disk.
     */
    fun blockingRecordChat(e164: String) = runBlocking { recordChat(e164) }

    suspend fun setRecentLabel(e164: String, label: String?) = updateRecents {
        HistoryRules.setLabel(it, e164, label)
    }

    suspend fun removeRecent(e164: String) = updateRecents { HistoryRules.remove(it, e164) }

    suspend fun clearRecents() = put { it.remove(KEY_RECENTS) }

    /**
     * Detects the country once, on first run. After this the country is a manual setting:
     * it only ever changes because the user changed it, so travelling or roaming never
     * silently rewrites the numbers you are about to message.
     */
    suspend fun seedRegionIfUnset() {
        if (settings.first().region != null) return
        detectRegion()?.let { setRegion(it) }
    }

    /** The blocking equivalent, for the no-UI selection-toolbar path. */
    fun blockingSeedRegionIfUnset(): String? = runBlocking {
        settings.first().region ?: detectRegion()?.also { setRegion(it) }
    }

    private fun detectRegion(): String? {
        val supported = PhoneNumberUtil.getInstance().supportedRegions
        return detectRegions().firstOrNull { it in supported }
    }

    /** Best guess first: the SIM, then the network, then the device locale. */
    private fun detectRegions(): List<String> {
        val candidates = mutableListOf<String?>()

        // Neither getter is permission-guarded, but OEM builds have been known to throw
        // here; a bad guess must never cost us the locale fallback.
        runCatching {
            val telephony =
                appContext.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
            candidates += telephony?.simCountryIso
            candidates += telephony?.networkCountryIso
        }

        val locales = appContext.resources.configuration.locales
        for (i in 0 until locales.size()) candidates += locales[i].country

        return candidates
            .filterNotNull()
            .map { it.uppercase() }
            .filter { it.length == 2 }
    }

    private suspend fun updateRecents(transform: (List<HistoryEntry>) -> List<HistoryEntry>) =
        put { prefs ->
            prefs[KEY_RECENTS] = HistoryCodec.encode(
                transform(HistoryCodec.decode(prefs[KEY_RECENTS])),
            )
        }

    private suspend fun put(block: (MutablePreferences) -> Unit) {
        store.edit(block)
    }

    private fun toSettings(prefs: Preferences) = AppSettings(
        theme = ThemePreference.from(prefs[KEY_THEME]),
        region = prefs[KEY_REGION],
        flavor = WhatsAppFlavor.from(prefs[KEY_FLAVOR]),
        historyEnabled = prefs[KEY_HISTORY_ENABLED] ?: true,
        howItWorksDismissed = prefs[KEY_HOW_IT_WORKS_DISMISSED] ?: false,
        quickMessages = QuickMessageCodec.decode(prefs[KEY_QUICK_MESSAGES]),
        recents = HistoryCodec.decode(prefs[KEY_RECENTS]).sortedByDescending { it.lastOpenedAt },
    )
}

internal val KEY_THEME = stringPreferencesKey("theme")
internal val KEY_REGION = stringPreferencesKey("region")
internal val KEY_FLAVOR = stringPreferencesKey("whatsapp_flavor")
internal val KEY_HISTORY_ENABLED = booleanPreferencesKey("history_enabled")
internal val KEY_HOW_IT_WORKS_DISMISSED = booleanPreferencesKey("how_it_works_dismissed")
internal val KEY_QUICK_MESSAGES = stringPreferencesKey("quick_messages")
internal val KEY_RECENTS = stringPreferencesKey("recents")

/**
 * Carries over the three SharedPreferences files this app used before DataStore, once.
 *
 * Both codecs already read their old on-disk shapes, so the values move across unchanged;
 * only the theme needs translating, because the six-option picker became three.
 */
private class LegacySharedPreferencesMigration(
    private val context: Context,
) : DataMigration<Preferences> {

    override suspend fun shouldMigrate(currentData: Preferences) =
        currentData[KEY_MIGRATED] != true

    override suspend fun migrate(currentData: Preferences): Preferences {
        val prefs = currentData.toMutablePreferences()
        prefs[KEY_MIGRATED] = true

        val main = context.getSharedPreferences("select_and_chat", Context.MODE_PRIVATE)
        main.getString("region", null)?.let { prefs[KEY_REGION] = it }
        main.getString("preferred_flavor", null)?.let { prefs[KEY_FLAVOR] = it }
        main.getString("theme", null)?.let { prefs[KEY_THEME] = ThemePreference.from(it).name }
        if (main.contains("how_it_works_dismissed")) {
            prefs[KEY_HOW_IT_WORKS_DISMISSED] = main.getBoolean("how_it_works_dismissed", false)
        }

        context.getSharedPreferences("select_and_chat_templates", Context.MODE_PRIVATE)
            .getString("templates", null)
            ?.let { prefs[KEY_QUICK_MESSAGES] = QuickMessageCodec.encode(QuickMessageCodec.decode(it)) }

        val history = context.getSharedPreferences("select_and_chat_history", Context.MODE_PRIVATE)
        history.getString("entries", null)?.let { prefs[KEY_RECENTS] = it }
        if (history.contains("enabled")) {
            prefs[KEY_HISTORY_ENABLED] = history.getBoolean("enabled", true)
        }

        return prefs.toPreferences()
    }

    override suspend fun cleanUp() = Unit

    private companion object {
        val KEY_MIGRATED = booleanPreferencesKey("migrated_from_shared_preferences")
    }
}
