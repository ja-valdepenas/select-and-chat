package dev.jvald.selectandchat.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.jvald.selectandchat.R
import dev.jvald.selectandchat.core.Countries
import dev.jvald.selectandchat.core.PhoneCandidate
import dev.jvald.selectandchat.core.PhoneNumberExtractor
import dev.jvald.selectandchat.core.QuickMessage
import dev.jvald.selectandchat.core.WhatsAppFlavor
import dev.jvald.selectandchat.ui.theme.AppIcons
import java.util.Locale

/**
 * The first screen, in the order the task is actually done: pick an opening line, type the
 * number, open the chat. The action sits directly under the field rather than pinned to
 * the bottom of the window, so it stays next to what it acts on at any text size.
 */
@Composable
fun NewChatScreen(
    state: MainUiState,
    locale: Locale,
    onPhoneInputChange: (String) -> Unit,
    onCountryClick: () -> Unit,
    onQuickMessageToggle: (QuickMessage) -> Unit,
    onAddQuickMessage: () -> Unit,
    onEditQuickMessage: (QuickMessage) -> Unit,
    onDeleteQuickMessage: (QuickMessage) -> Unit,
    onUseClipboardNumber: (String) -> Unit,
    onDismissClipboardNumber: () -> Unit,
    onDismissHowItWorks: () -> Unit,
    onOpenChat: (PhoneCandidate, String?) -> Unit,
    onFlavorChange: (WhatsAppFlavor) -> Unit,
    modifier: Modifier = Modifier,
) {
    val input = state.phoneInput
    val region = state.region
    val country = remember(region, locale) { Countries.byIso(region, locale) }
    val parsed = remember(input, region) { PhoneNumberExtractor.parseManual(input, region) }
    val feedback = remember(input, region) { PhoneNumberExtractor.checkLength(input, region) }

    Column(
        modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
    ) {
        Spacer(Modifier.height(8.dp))

        QuickMessagesSection(
            messages = state.quickMessages,
            selectedId = state.selectedQuickMessageId,
            onToggle = onQuickMessageToggle,
            onAdd = onAddQuickMessage,
            onEdit = onEditQuickMessage,
            onDelete = onDeleteQuickMessage,
        )

        Spacer(Modifier.height(24.dp))

        // A number spotted in the clipboard belongs next to the field it would fill, so it
        // sits here rather than at the top where it would push the whole page down.
        state.clipboardNumber?.let { number ->
            ClipboardStrip(
                number = number,
                onUse = { onUseClipboardNumber(number) },
                onDismiss = onDismissClipboardNumber,
            )
            Spacer(Modifier.height(12.dp))
        }

        PhoneNumberField(
            input = input,
            onInputChange = onPhoneInputChange,
            country = country,
            feedback = feedback,
            onCountryClick = onCountryClick,
        )

        Spacer(Modifier.height(16.dp))

        WhatsAppSplitButton(
            flavor = state.flavor,
            enabled = parsed != null,
            installedFlavors = state.installedFlavors,
            onOpen = { parsed?.let { onOpenChat(it, state.selectedMessage) } },
            onFlavorChange = onFlavorChange,
        )

        if (!state.howItWorksDismissed) {
            Spacer(Modifier.height(32.dp))
            HowItWorksCard(onDismiss = onDismissHowItWorks)
        }

        Spacer(Modifier.height(32.dp))
        Text(
            stringResource(R.string.not_affiliated),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun ClipboardStrip(number: String, onUse: () -> Unit, onDismiss: () -> Unit) {
    val formatted = remember(number) {
        PhoneNumberExtractor.parseManual(number, null)?.international ?: number
    }
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier.padding(start = 16.dp, top = 8.dp, end = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                AppIcons.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.found_in_clipboard),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(formatted, style = MaterialTheme.typography.bodyLarge)
            }
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.dismiss)) }
            TextButton(onClick = onUse) { Text(stringResource(R.string.use_it)) }
        }
    }
}
