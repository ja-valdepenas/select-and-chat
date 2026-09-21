package dev.jvald.selectandchat.ui

import android.content.Context
import android.text.format.DateUtils
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.jvald.selectandchat.R
import dev.jvald.selectandchat.core.Countries
import dev.jvald.selectandchat.core.HistoryEntry
import dev.jvald.selectandchat.core.PhoneNumberExtractor
import dev.jvald.selectandchat.ui.theme.AppIcons
import java.util.Locale

/**
 * Numbers previously opened. Every one of these is a number the user chose not to save as
 * a contact, so the label carries the row and the digits sit underneath it.
 */
@Composable
fun RecentsScreen(
    entries: List<HistoryEntry>,
    historyEnabled: Boolean,
    locale: Locale,
    onOpen: (HistoryEntry) -> Unit,
    onSetLabel: (HistoryEntry, String?) -> Unit,
    onSaveContact: (HistoryEntry) -> Unit,
    onRemove: (HistoryEntry) -> Unit,
    onHistoryEnabledChange: (Boolean) -> Unit,
    onManageHistory: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var editing by remember { mutableStateOf<HistoryEntry?>(null) }

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
    ) {
        Spacer(Modifier.height(8.dp))
        HistorySettingsCard(
            historyEnabled = historyEnabled,
            onHistoryEnabledChange = onHistoryEnabledChange,
            onManageHistory = onManageHistory,
        )

        Spacer(Modifier.height(24.dp))
        Text(
            stringResource(R.string.recent_chats),
            style = MaterialTheme.typography.titleLargeEmphasized,
        )
        Spacer(Modifier.height(12.dp))

        if (!historyEnabled && entries.isEmpty()) {
            EmptyNote(stringResource(R.string.history_off_note))
        } else if (entries.isEmpty()) {
            EmptyNote(stringResource(R.string.history_empty_note))
        } else {
            if (!historyEnabled) {
                Text(
                    stringResource(R.string.history_off_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
            }
            entries.forEach { entry ->
                RecentRow(
                    entry = entry,
                    locale = locale,
                    onOpen = { onOpen(entry) },
                    onEditLabel = { editing = entry },
                    onSaveContact = { onSaveContact(entry) },
                    onRemove = { onRemove(entry) },
                )
                Spacer(Modifier.height(8.dp))
            }
        }

        Spacer(Modifier.height(32.dp))
        Text(
            stringResource(R.string.stored_on_device),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(24.dp))
    }

    editing?.let { entry ->
        LabelDialog(
            entry = entry,
            onDismiss = { editing = null },
            onConfirm = { label ->
                onSetLabel(entry, label)
                editing = null
            },
        )
    }
}

/**
 * The two history controls in one raised surface.
 *
 * Clearing is deliberately not here: it removes many records at once and cannot be undone,
 * so it lives one level in, behind Manage history, rather than as a button the thumb can
 * find by accident.
 */
@Composable
private fun HistorySettingsCard(
    historyEnabled: Boolean,
    onHistoryEnabledChange: (Boolean) -> Unit,
    onManageHistory: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .heightIn(min = 64.dp)
                    .padding(start = 20.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.save_recent_chats),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        stringResource(R.string.stored_on_device),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.width(12.dp))
                Switch(checked = historyEnabled, onCheckedChange = onHistoryEnabledChange)
            }

            HorizontalDivider(
                Modifier.padding(horizontal = 20.dp),
                color = MaterialTheme.colorScheme.outlineVariant,
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 64.dp)
                    .clickable(onClick = onManageHistory)
                    .padding(start = 20.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.manage_history),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        stringResource(R.string.manage_history_helper),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Icon(AppIcons.Chevron, contentDescription = null)
            }
        }
    }
}

/**
 * One saved number. The row is one focus stop that reads out as a whole and opens the
 * chat; the overflow button is a second, with a label that names the number so several
 * rows of "More options" never sound alike.
 */
@Composable
private fun RecentRow(
    entry: HistoryEntry,
    locale: Locale,
    onOpen: () -> Unit,
    onEditLabel: () -> Unit,
    onSaveContact: () -> Unit,
    onRemove: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val country = remember(entry.e164, locale) { Countries.byIso(regionOf(entry), locale) }
    val formatted = remember(entry.e164) {
        PhoneNumberExtractor.parseManual(entry.e164, null)?.international ?: entry.e164
    }

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(end = 4.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 64.dp)
                    .clickable(onClick = onOpen)
                    .padding(horizontal = 12.dp, vertical = 10.dp)
                    .semantics(mergeDescendants = true) { role = Role.Button },
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.size(44.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            country?.iso ?: "?",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        entry.label ?: formatted,
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        if (entry.label == null) {
                            relativeTime(context, entry.lastOpenedAt)
                        } else {
                            "$formatted · ${relativeTime(context, entry.lastOpenedAt)}"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Box {
                // Named after the number, so several rows do not all announce
                // "More options".
                val overflowLabel = stringResource(R.string.more_options_for, formatted)
                IconButton(
                    onClick = { menuOpen = true },
                    modifier = Modifier
                        .size(48.dp)
                        .semantics { contentDescription = overflowLabel },
                ) {
                    Icon(AppIcons.MoreVert, contentDescription = null)
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.edit_label)) },
                        leadingIcon = { Icon(AppIcons.Edit, contentDescription = null) },
                        onClick = {
                            menuOpen = false
                            onEditLabel()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.save_contact)) },
                        leadingIcon = { Icon(AppIcons.PersonAdd, contentDescription = null) },
                        onClick = {
                            menuOpen = false
                            onSaveContact()
                        },
                    )
                    DropdownMenuItem(
                        text = {
                            Text(
                                stringResource(R.string.remove),
                                color = MaterialTheme.colorScheme.error,
                            )
                        },
                        leadingIcon = {
                            Icon(
                                AppIcons.Delete,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                            )
                        },
                        onClick = {
                            menuOpen = false
                            onRemove()
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun LabelDialog(
    entry: HistoryEntry,
    onDismiss: () -> Unit,
    onConfirm: (String?) -> Unit,
) {
    var text by remember { mutableStateOf(entry.label.orEmpty()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.edit_label)) },
        text = {
            Column {
                Text(
                    entry.e164,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text(stringResource(R.string.label_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(text) }) { Text(stringResource(R.string.save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}

@Composable
private fun EmptyNote(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        textAlign = TextAlign.Center,
    )
}

private fun regionOf(entry: HistoryEntry): String? =
    PhoneNumberExtractor.parseManual(entry.e164, null)?.regionCode

/**
 * "Just now" for the first minute, because `DateUtils` says "0 minutes ago" there. After
 * that the platform's own phrasing is used, which follows the active app locale.
 */
private fun relativeTime(context: Context, at: Long): String {
    if (at <= 0) return ""
    val elapsed = System.currentTimeMillis() - at
    if (elapsed < DateUtils.MINUTE_IN_MILLIS) return context.getString(R.string.just_now)
    return DateUtils.getRelativeTimeSpanString(
        at,
        System.currentTimeMillis(),
        DateUtils.MINUTE_IN_MILLIS,
        DateUtils.FORMAT_ABBREV_ALL,
    ).toString()
}
