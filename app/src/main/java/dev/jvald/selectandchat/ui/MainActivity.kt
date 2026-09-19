package dev.jvald.selectandchat.ui

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.jvald.selectandchat.R
import dev.jvald.selectandchat.core.Countries
import dev.jvald.selectandchat.core.PhoneCandidate
import dev.jvald.selectandchat.core.Prefs
import dev.jvald.selectandchat.core.WhatsAppFlavor
import dev.jvald.selectandchat.core.WhatsAppLauncher
import dev.jvald.selectandchat.ui.theme.SelectAndChatTheme

/** Launcher screen: how it works, manual entry, and the two settings. */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = Prefs(this)
        prefs.seedRegionIfUnset(this)

        setContent {
            SelectAndChatTheme {
                MainScreen(
                    prefs = prefs,
                    onSubmit = { candidate ->
                        val opened =
                            WhatsAppLauncher.openChat(this, candidate.e164, prefs.preferredFlavor)
                        if (!opened) {
                            Toast.makeText(
                                this,
                                R.string.whatsapp_not_installed,
                                Toast.LENGTH_LONG,
                            ).show()
                        }
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainScreen(prefs: Prefs, onSubmit: (PhoneCandidate) -> Unit) {
    val context = LocalContext.current
    var region by remember { mutableStateOf(prefs.region) }
    var flavor by remember { mutableStateOf(prefs.preferredFlavor) }
    val installed = remember { WhatsAppLauncher.installedFlavors(context) }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.app_name)) }) },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
        ) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        stringResource(R.string.how_to_title),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        stringResource(R.string.how_to_body),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
            ManualEntry(
                initialNumber = "",
                region = region,
                onRegionChange = {
                    prefs.region = it
                    region = it
                },
                onSubmit = onSubmit,
            )

            Spacer(Modifier.height(32.dp))
            HorizontalDivider()
            Spacer(Modifier.height(24.dp))

            Text(stringResource(R.string.settings), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                stringResource(R.string.default_country_explainer),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            CountryField(
                selected = Countries.byIso(region),
                onSelect = {
                    prefs.region = it.iso
                    region = it.iso
                },
            )

            Spacer(Modifier.height(24.dp))
            Text(stringResource(R.string.open_with), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            WhatsAppFlavor.entries.forEach { option ->
                FlavorRow(
                    flavor = option,
                    selected = option == flavor,
                    installed = option in installed,
                    onSelect = {
                        prefs.preferredFlavor = option
                        flavor = option
                    },
                )
            }

            Spacer(Modifier.height(32.dp))
            Text(
                stringResource(R.string.disclaimer),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun FlavorRow(
    flavor: WhatsAppFlavor,
    selected: Boolean,
    installed: Boolean,
    onSelect: () -> Unit,
) {
    val name = stringResource(
        when (flavor) {
            WhatsAppFlavor.STANDARD -> R.string.whatsapp_standard
            WhatsAppFlavor.BUSINESS -> R.string.whatsapp_business
        },
    )
    Row(
        Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onSelect)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start,
    ) {
        RadioButton(selected = selected, onClick = onSelect)
        Spacer(Modifier.height(0.dp))
        Text(
            // The preference still applies when the app is missing: openChat falls back to
            // whichever flavor is actually present, so this is a hint, not an error.
            if (installed) name else "$name ${stringResource(R.string.not_installed_suffix)}",
            style = MaterialTheme.typography.bodyLarge,
            color = if (installed) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
    }
}
