package dev.jvald.selectandchat.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import dev.jvald.selectandchat.R
import dev.jvald.selectandchat.core.QuickMessage
import dev.jvald.selectandchat.ui.theme.AppIcons

/**
 * The opening-message picker.
 *
 * Every saved message is on screen at once in a wrapping row — no horizontal scroller, so
 * nothing is hidden off the edge and nothing is truncated; the page scrolls instead when
 * the collection grows. The plus sits at the end of the same row, so adding one is in the
 * same place as choosing one.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QuickMessagesSection(
    messages: List<QuickMessage>,
    selectedId: String?,
    onToggle: (QuickMessage) -> Unit,
    onAdd: () -> Unit,
    onEdit: (QuickMessage) -> Unit,
    onDelete: (QuickMessage) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(28.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(20.dp)) {
            Text(
                stringResource(R.string.quick_messages),
                style = MaterialTheme.typography.titleLargeEmphasized,
            )
            Text(
                stringResource(R.string.quick_messages_helper),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                messages.forEach { message ->
                    QuickMessageChip(
                        message = message,
                        selected = message.id == selectedId,
                        onToggle = { onToggle(message) },
                        onEdit = { onEdit(message) },
                        onDelete = { onDelete(message) },
                    )
                }
                AddQuickMessageButton(onClick = onAdd)
            }

            if (messages.isEmpty()) {
                Spacer(Modifier.height(12.dp))
                Text(
                    stringResource(R.string.no_quick_messages),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (selectedId != null) {
                Spacer(Modifier.height(12.dp))
                Text(
                    stringResource(R.string.selected_message_helper),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/**
 * One message.
 *
 * Built on [Surface] rather than `FilterChip` for one reason: the chip has to answer to a
 * long press as well as a tap, and a Material chip's own click handler swallows the
 * gesture before a wrapper ever sees it. The semantics a chip would have given for free —
 * a selectable role, the selected state, a spoken state description — are therefore
 * declared here explicitly, along with the custom actions that make Edit and Delete
 * reachable without a long press at all.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun QuickMessageChip(
    message: QuickMessage,
    selected: Boolean,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    val haptics = LocalHapticFeedback.current

    val container = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        Color.Transparent
    }
    val content = if (selected) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    val stateLabel = stringResource(
        if (selected) R.string.quick_message_selected else R.string.quick_message_not_selected,
    )
    val editLabel = stringResource(R.string.edit)
    val deleteLabel = stringResource(R.string.delete)

    Box {
        Surface(
            color = container,
            contentColor = content,
            shape = CircleShape,
            border = if (selected) {
                null
            } else {
                BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outline,
                )
            },
            modifier = Modifier
                .heightIn(min = 48.dp)
                .clip(CircleShape)
                .combinedClickable(
                    role = Role.Button,
                    onLongClickLabel = stringResource(R.string.more_options),
                    onLongClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        menuOpen = true
                    },
                    onClick = onToggle,
                )
                .semantics {
                    this.selected = selected
                    stateDescription = stateLabel
                    customActions = listOf(
                        CustomAccessibilityAction(editLabel) { onEdit(); true },
                        CustomAccessibilityAction(deleteLabel) { onDelete(); true },
                    )
                },
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(
                    start = if (selected) 12.dp else 20.dp,
                    end = 20.dp,
                    top = 12.dp,
                    bottom = 12.dp,
                ),
            ) {
                if (selected) {
                    // The second signal alongside the fill, so selection is never colour alone.
                    Icon(
                        AppIcons.Check,
                        contentDescription = null,
                        modifier = Modifier
                            .size(20.dp)
                            .padding(end = 2.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Text(message.text, style = MaterialTheme.typography.labelLarge)
            }
        }

        ChipContextMenu(
            expanded = menuOpen,
            onDismiss = { menuOpen = false },
            onEdit = onEdit,
            onDelete = onDelete,
        )
    }
}

@Composable
private fun AddQuickMessageButton(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.primary,
        shape = CircleShape,
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline,
        ),
        modifier = Modifier.sizeIn(minWidth = 56.dp, minHeight = 48.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                AppIcons.Add,
                contentDescription = stringResource(R.string.add_quick_message),
            )
        }
    }
}

/** Add, and edit, are the same dialog; only the title and the starting text differ. */
@Composable
fun QuickMessageDialog(
    initialText: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    var text by remember { mutableStateOf(initialText) }
    val focusRequester = remember { FocusRequester() }

    // Moves accessibility and keyboard focus into the field as the dialog opens, rather
    // than leaving it on the title.
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(
                    if (initialText.isEmpty()) {
                        R.string.add_quick_message
                    } else {
                        R.string.edit_quick_message
                    },
                ),
            )
        },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text(stringResource(R.string.quick_message)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(text) },
                enabled = text.isNotBlank(),
            ) { Text(stringResource(R.string.save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}

/**
 * The management surface reached from the three-dot menu.
 *
 * It exists so that Edit and Delete are never only behind a long press: Switch Access and
 * a keyboard both reach these buttons the ordinary way.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageQuickMessagesSheet(
    messages: List<QuickMessage>,
    onAdd: () -> Unit,
    onEdit: (QuickMessage) -> Unit,
    onDelete: (QuickMessage) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            Modifier
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Text(
                stringResource(R.string.manage_quick_messages),
                style = MaterialTheme.typography.headlineSmall,
            )
            Spacer(Modifier.height(16.dp))

            if (messages.isEmpty()) {
                Text(
                    stringResource(R.string.no_quick_messages),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            messages.forEach { message ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp),
                ) {
                    Text(
                        message.text,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier
                            .weight(1f)
                            .padding(vertical = 8.dp),
                    )
                    IconButton(onClick = { onEdit(message) }) {
                        Icon(
                            AppIcons.Edit,
                            contentDescription = stringResource(R.string.edit),
                        )
                    }
                    IconButton(onClick = { onDelete(message) }) {
                        Icon(
                            AppIcons.Delete,
                            contentDescription = stringResource(R.string.delete),
                            tint = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onAdd) {
                Icon(AppIcons.Add, contentDescription = null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.add_quick_message))
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

/** Edit in the normal colour, Delete in the error colour, as the destructive one. */
@Composable
private fun ChipContextMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        DropdownMenuItem(
            text = { Text(stringResource(R.string.edit)) },
            leadingIcon = { Icon(AppIcons.Edit, contentDescription = null) },
            onClick = {
                onDismiss()
                onEdit()
            },
        )
        DropdownMenuItem(
            text = {
                Text(
                    stringResource(R.string.delete),
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
                onDismiss()
                onDelete()
            },
        )
    }
}
