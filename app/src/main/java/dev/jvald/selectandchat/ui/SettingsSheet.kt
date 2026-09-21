package dev.jvald.selectandchat.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.jvald.selectandchat.R
import dev.jvald.selectandchat.core.AppLanguage
import dev.jvald.selectandchat.core.ThemePreference
import dev.jvald.selectandchat.ui.theme.AppIcons

/** The gear in the header. Opens [SettingsSheet]; holds no state of its own. */
@Composable
fun SettingsButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    IconButton(onClick = onClick, modifier = modifier.size(48.dp)) {
        Icon(
            AppIcons.Settings,
            contentDescription = stringResource(R.string.settings),
        )
    }
}

/**
 * The only settings surface in the app.
 *
 * A sheet rather than a dropdown, and rows that expand in place rather than submenus: the
 * two choices here are one-of-five and one-of-three, and a menu that has to be reopened to
 * see what is currently picked is worse at both than a list that shows it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(
    language: AppLanguage,
    theme: ThemePreference,
    onLanguageChange: (AppLanguage) -> Unit,
    onThemeChange: (ThemePreference) -> Unit,
    onManageQuickMessages: () -> Unit,
    onDismiss: () -> Unit,
) {
    var expanded by remember { mutableStateOf(Section.NONE) }

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
                stringResource(R.string.settings),
                style = MaterialTheme.typography.headlineSmall,
            )
            Spacer(Modifier.height(16.dp))

            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column {
                    ExpandableRow(
                        icon = AppIcons.Language,
                        label = stringResource(R.string.language),
                        value = stringResource(language.labelRes()),
                        expanded = expanded == Section.LANGUAGE,
                        onClick = {
                            expanded =
                                if (expanded == Section.LANGUAGE) Section.NONE else Section.LANGUAGE
                        },
                    ) {
                        AppLanguage.entries.forEach { option ->
                            ChoiceRow(
                                label = stringResource(option.labelRes()),
                                selected = option == language,
                                onClick = { onLanguageChange(option) },
                            )
                        }
                    }

                    HorizontalDivider(
                        Modifier.padding(horizontal = 20.dp),
                        color = MaterialTheme.colorScheme.outlineVariant,
                    )

                    ExpandableRow(
                        icon = AppIcons.Palette,
                        label = stringResource(R.string.theme),
                        value = stringResource(theme.labelRes()),
                        expanded = expanded == Section.THEME,
                        onClick = {
                            expanded =
                                if (expanded == Section.THEME) Section.NONE else Section.THEME
                        },
                    ) {
                        ThemePreference.entries.forEach { option ->
                            ChoiceRow(
                                label = stringResource(option.labelRes()),
                                selected = option == theme,
                                onClick = { onThemeChange(option) },
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 64.dp)
                        .clickable {
                            onDismiss()
                            onManageQuickMessages()
                        }
                        .padding(start = 20.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
                ) {
                    Icon(
                        AppIcons.ChatBubble,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.width(16.dp))
                    Text(
                        stringResource(R.string.manage_quick_messages),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f),
                    )
                    Icon(
                        AppIcons.Chevron,
                        contentDescription = null,
                    )
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

private enum class Section { NONE, LANGUAGE, THEME }

/**
 * A setting that shows its current value and opens underneath itself. The chevron turns
 * so the state is not carried by position alone.
 */
@Composable
private fun ExpandableRow(
    icon: Painter,
    label: String,
    value: String,
    expanded: Boolean,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 64.dp)
                .clickable(onClick = onClick)
                .padding(start = 20.dp, end = 16.dp, top = 12.dp, bottom = 12.dp)
                .semantics { role = Role.Button },
        ) {
            // The row already says what it is in words; the glyph is there to be found
            // at a glance, so it announces nothing of its own.
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(16.dp))
            Text(
                label,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            Text(
                value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.width(8.dp))
            Icon(
                AppIcons.ExpandMore,
                contentDescription = null,
                modifier = Modifier.rotate(if (expanded) 180f else 0f),
            )
        }
        AnimatedVisibility(visible = expanded) {
            Column(Modifier.padding(bottom = 8.dp)) { content() }
        }
    }
}

/** One of a set. The checkmark is the state; the colour only agrees with it. */
@Composable
private fun ChoiceRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        color = Color.Transparent,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable(onClick = onClick)
            .semantics {
                role = Role.RadioButton
                this.selected = selected
            },
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 60.dp, end = 20.dp, top = 8.dp, bottom = 8.dp),
        ) {
            Box(Modifier.size(24.dp), contentAlignment = Alignment.Center) {
                if (selected) {
                    Icon(
                        AppIcons.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            Spacer(Modifier.width(16.dp))
            Text(
                label,
                style = MaterialTheme.typography.bodyLarge,
                color = if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            )
        }
    }
}

/** Settings rows show the current value, so both enums need a label of their own. */
internal fun ThemePreference.labelRes() = when (this) {
    ThemePreference.SYSTEM -> R.string.system_default
    ThemePreference.LIGHT -> R.string.light_theme
    ThemePreference.DARK -> R.string.dark_theme
    ThemePreference.APP_DEFAULT -> R.string.app_default_theme
    ThemePreference.AMOLED -> R.string.amoled_theme
}

internal fun AppLanguage.labelRes() = when (this) {
    AppLanguage.SYSTEM -> R.string.system_default
    AppLanguage.ENGLISH -> R.string.language_english
    AppLanguage.SPANISH -> R.string.language_spanish
}
