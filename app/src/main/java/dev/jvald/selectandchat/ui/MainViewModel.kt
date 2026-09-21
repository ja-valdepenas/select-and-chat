package dev.jvald.selectandchat.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.jvald.selectandchat.core.AppSettings
import dev.jvald.selectandchat.core.HistoryEntry
import dev.jvald.selectandchat.core.QuickMessage
import dev.jvald.selectandchat.core.QuickMessageRules
import dev.jvald.selectandchat.core.SettingsRepository
import dev.jvald.selectandchat.core.ThemePreference
import dev.jvald.selectandchat.core.WhatsAppFlavor
import dev.jvald.selectandchat.core.WhatsAppLauncher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Everything the screen draws from, in one immutable value.
 *
 * Transient UI — which menu is open, which chip is pressed — is deliberately absent: that
 * belongs to the composable that owns it and must not survive a rotation as if it were a
 * setting.
 */
data class MainUiState(
    val theme: ThemePreference = ThemePreference.SYSTEM,
    val region: String? = null,
    val flavor: WhatsAppFlavor = WhatsAppFlavor.STANDARD,
    val installedFlavors: Set<WhatsAppFlavor> = emptySet(),
    val phoneInput: String = "",
    val quickMessages: List<QuickMessage> = emptyList(),
    val selectedQuickMessageId: String? = null,
    val historyEnabled: Boolean = true,
    val recents: List<HistoryEntry> = emptyList(),
    val howItWorksDismissed: Boolean = false,
    val clipboardNumber: String? = null,
) {
    /** The text that rides along to WhatsApp, or null when nothing is selected. */
    val selectedMessage: String?
        get() = quickMessages.firstOrNull { it.id == selectedQuickMessageId }?.text
}

/** A quick message taken out of the list, held just long enough for Undo to put it back. */
data class DeletedQuickMessage(val message: QuickMessage, val index: Int)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = SettingsRepository(application)

    /** State this app owns but never writes to disk: what has been typed, what is picked. */
    private val session = MutableStateFlow(SessionState())

    val uiState: StateFlow<MainUiState> =
        combine(repository.settings, session) { settings, session ->
            merge(settings, session)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = MainUiState(),
        )

    init {
        viewModelScope.launch { repository.seedRegionIfUnset() }
        refreshInstalledApps()
    }

    /**
     * Which WhatsApp flavors are present. Re-read on resume rather than cached for the
     * process lifetime, because one of them can be installed while this screen is open.
     */
    fun refreshInstalledApps() {
        val installed = WhatsAppLauncher.installedFlavors(getApplication())
        session.update { it.copy(installedFlavors = installed) }
    }

    fun onPhoneInputChange(value: String) = session.update { it.copy(phoneInput = value) }

    fun onClipboardNumberFound(number: String?) =
        session.update { it.copy(clipboardNumber = number) }

    fun useClipboardNumber(number: String) =
        session.update { it.copy(phoneInput = number, clipboardNumber = null) }

    fun dismissClipboardNumber() = session.update { it.copy(clipboardNumber = null) }

    /** Tapping the selected chip again clears it; tapping another replaces the selection. */
    fun onQuickMessageToggled(id: String) = session.update {
        it.copy(selectedQuickMessageId = if (it.selectedQuickMessageId == id) null else id)
    }

    fun setTheme(theme: ThemePreference) = launchWrite { repository.setTheme(theme) }

    fun setRegion(iso: String) = launchWrite { repository.setRegion(iso) }

    fun setFlavor(flavor: WhatsAppFlavor) = launchWrite { repository.setFlavor(flavor) }

    fun dismissHowItWorks() = launchWrite { repository.dismissHowItWorks() }

    fun addQuickMessage(text: String) = editQuickMessages { QuickMessageRules.add(it, text) }

    fun editQuickMessage(id: String, text: String) =
        editQuickMessages { QuickMessageRules.edit(it, id, text) }

    /**
     * Deletes immediately and hands back what was removed, so the snackbar can offer Undo
     * without the list having to stay in a half-deleted state meanwhile.
     */
    fun deleteQuickMessage(id: String): DeletedQuickMessage? {
        val current = uiState.value.quickMessages
        val index = current.indexOfFirst { it.id == id }
        if (index < 0) return null
        val removed = current[index]
        session.update {
            if (it.selectedQuickMessageId == id) it.copy(selectedQuickMessageId = null) else it
        }
        launchWrite { repository.setQuickMessages(QuickMessageRules.remove(current, id)) }
        return DeletedQuickMessage(removed, index)
    }

    fun restoreQuickMessage(deleted: DeletedQuickMessage) = editQuickMessages {
        QuickMessageRules.restore(it, deleted.message, deleted.index)
    }

    fun setHistoryEnabled(enabled: Boolean) = launchWrite {
        repository.setHistoryEnabled(enabled)
    }

    fun recordOpenedChat(e164: String) = launchWrite { repository.recordChat(e164) }

    fun setRecentLabel(e164: String, label: String?) = launchWrite {
        repository.setRecentLabel(e164, label)
    }

    fun removeRecent(e164: String) = launchWrite { repository.removeRecent(e164) }

    fun clearRecents() = launchWrite { repository.clearRecents() }

    private fun editQuickMessages(transform: (List<QuickMessage>) -> List<QuickMessage>) =
        launchWrite { repository.setQuickMessages(transform(uiState.value.quickMessages)) }

    private fun launchWrite(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }

    private fun merge(settings: AppSettings, session: SessionState) = MainUiState(
        theme = settings.theme,
        region = settings.region,
        flavor = settings.flavor,
        installedFlavors = session.installedFlavors,
        phoneInput = session.phoneInput,
        quickMessages = settings.quickMessages,
        // A message deleted from another surface must not ride along on the next chat.
        selectedQuickMessageId = session.selectedQuickMessageId
            ?.takeIf { id -> settings.quickMessages.any { it.id == id } },
        historyEnabled = settings.historyEnabled,
        recents = settings.recents,
        howItWorksDismissed = settings.howItWorksDismissed,
        clipboardNumber = session.clipboardNumber,
    )

    private data class SessionState(
        val phoneInput: String = "",
        val selectedQuickMessageId: String? = null,
        val clipboardNumber: String? = null,
        val installedFlavors: Set<WhatsAppFlavor> = emptySet(),
    )
}
