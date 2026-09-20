package dev.jvald.selectandchat.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.jvald.selectandchat.R
import dev.jvald.selectandchat.core.AppTheme
import dev.jvald.selectandchat.core.Countries
import dev.jvald.selectandchat.core.WhatsAppFlavor

@Composable
fun SettingsScreen(
    region: String?,
    flavor: WhatsAppFlavor,
    theme: AppTheme,
    historyEnabled: Boolean,
    templates: List<String>,
    onCountryClick: () -> Unit,
    onFlavorClick: () -> Unit,
    onThemeClick: () -> Unit,
    onHistoryEnabledChange: (Boolean) -> Unit,
    onRemoveTemplate: (String) -> Unit,
    onAddTemplate: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
    ) {
        Spacer(Modifier.height(24.dp))
        Text(
            stringResource(R.string.tab_settings),
            style = MaterialTheme.typography.displaySmall,
        )

        Spacer(Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SettingTile(
                label = stringResource(R.string.tile_country),
                value = Countries.byIso(region)
                    ?.let { "${it.flag} ${it.iso} +${it.callingCode}" }
                    ?: stringResource(R.string.choose_country),
                onClick = onCountryClick,
                modifier = Modifier.weight(1f),
            )
            SettingTile(
                label = stringResource(R.string.tile_opens_in),
                value = stringResource(flavor.labelRes()),
                onClick = onFlavorClick,
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(Modifier.height(12.dp))
        SettingTile(
            label = stringResource(R.string.theme),
            value = stringResource(theme.labelRes()),
            onClick = onThemeClick,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(20.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.keep_history),
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    stringResource(R.string.keep_history_body),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(12.dp))
            Switch(checked = historyEnabled, onCheckedChange = onHistoryEnabledChange)
        }

        Spacer(Modifier.height(20.dp))
        Text(
            stringResource(R.string.quick_messages),
            style = MaterialTheme.typography.titleSmall,
        )
        Text(
            stringResource(R.string.quick_messages_body),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(10.dp))
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainer,
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(vertical = 4.dp)) {
                if (templates.isEmpty()) {
                    Text(
                        stringResource(R.string.no_templates),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    )
                }
                templates.forEach { template ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            template,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(onClick = { onRemoveTemplate(template) }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = stringResource(R.string.remove),
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        SettingTile(
            label = stringResource(R.string.quick_messages),
            value = stringResource(R.string.add_message),
            onClick = onAddTemplate,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(28.dp))
        Text(
            stringResource(R.string.disclaimer),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(24.dp))
    }
}
