package dev.jvald.selectandchat.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import dev.jvald.selectandchat.R
import dev.jvald.selectandchat.core.AppSettings
import dev.jvald.selectandchat.core.Extraction
import dev.jvald.selectandchat.core.PhoneCandidate
import dev.jvald.selectandchat.core.PhoneNumberExtractor
import dev.jvald.selectandchat.core.SettingsRepository
import dev.jvald.selectandchat.core.WhatsAppLauncher
import dev.jvald.selectandchat.ui.theme.SelectAndChatTheme
import kotlinx.coroutines.launch

/**
 * The toolbar item, and the share-sheet target.
 *
 * In the common case this activity renders nothing at all: it resolves the number, fires
 * the intent at WhatsApp and finishes. Any UI here would be a flash on the way to the
 * chat, so the window is translucent and un-animated and `setContent` is only ever
 * reached when the selection genuinely needs a decision. That is also why the settings
 * are read synchronously here — see [SettingsRepository.blockingSnapshot].
 */
class ProcessTextActivity : ComponentActivity() {

    private lateinit var repository: SettingsRepository
    private lateinit var settings: AppSettings

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repository = SettingsRepository(this)
        val region = repository.blockingSeedRegionIfUnset()
        settings = repository.blockingSnapshot().copy(region = region)

        val selection = intent.selectedText()
        val extraction = PhoneNumberExtractor.extract(selection, settings.region)

        // Skip straight to WhatsApp only for a single *confident* match. An unconfident one
        // came from the fallback pass, which can turn an order number into a phone number —
        // acting on that silently would open a chat the user never asked for.
        val single = extraction.candidates.singleOrNull()
        if (single != null && single.confident) {
            openChat(single)
            return
        }
        showSheet(selection, extraction)
    }

    /** PROCESS_TEXT carries the selection; ACTION_SEND carries shared text. */
    private fun Intent.selectedText(): CharSequence? =
        getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT) ?: getCharSequenceExtra(Intent.EXTRA_TEXT)

    private fun openChat(candidate: PhoneCandidate) {
        val opened = WhatsAppLauncher.openChat(this, candidate.e164, settings.flavor)
        if (opened != null) {
            // Recorded here too, so numbers reached from the selection toolbar - the main
            // way this app is used - show up in Recents alongside typed ones.
            repository.blockingRecordChat(candidate.e164)
        } else {
            Toast.makeText(this, R.string.whatsapp_not_installed, Toast.LENGTH_LONG).show()
        }
        dismiss()
    }

    private fun showSheet(selection: CharSequence?, initial: Extraction) {
        setContent {
            SelectAndChatTheme(theme = settings.theme) {
                var region by remember { mutableStateOf(settings.region) }
                var extraction by remember { mutableStateOf(initial) }

                NumberPickerSheet(
                    extraction = extraction,
                    region = region,
                    onRegionChange = { iso ->
                        region = iso
                        lifecycleScope.launch { repository.setRegion(iso) }
                        // Re-run extraction: a local number that resolved to nothing a
                        // moment ago usually resolves now that the country is known.
                        extraction = PhoneNumberExtractor.extract(selection, iso)
                    },
                    onPick = ::openChat,
                    onDismiss = ::dismiss,
                )
            }
        }
    }

    @Suppress("DEPRECATION") // No non-deprecated equivalent below API 34.
    private fun dismiss() {
        finish()
        overridePendingTransition(0, 0)
    }
}
