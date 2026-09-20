package dev.jvald.selectandchat.ui

import android.app.Activity
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import dev.jvald.selectandchat.R
import dev.jvald.selectandchat.core.AppTheme
import dev.jvald.selectandchat.core.Countries
import dev.jvald.selectandchat.core.PhoneCandidate
import dev.jvald.selectandchat.core.Prefs
import dev.jvald.selectandchat.core.WhatsAppFlavor
import dev.jvald.selectandchat.core.WhatsAppLauncher
import dev.jvald.selectandchat.ui.theme.SelectAndChatTheme

/** Launcher screen: headline, explainer, number entry, and the three settings. */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val prefs = Prefs(this)
        prefs.seedRegionIfUnset(this)

        setContent {
            var theme by remember { mutableStateOf(prefs.theme) }
            SelectAndChatTheme(theme = theme) {
                SyncSystemBars(theme)
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                MainScreen(
                    prefs = prefs,
                    theme = theme,
                    onThemeChange = {
                        prefs.theme = it
                        theme = it
                    },
                    onSubmit = { candidate ->
                        val opened =
                            WhatsAppLauncher.openChat(this, candidate.e164, prefs.preferredFlavor)
                        if (!opened) {
                            Toast.makeText(this, R.string.whatsapp_not_installed, Toast.LENGTH_LONG)
                                .show()
                        }
                    },
                )
                }
            }
        }
    }
}

/**
 * Without this the status bar icons keep their launch-time colour, so they vanish against
 * AMOLED black or wash out on the light theme.
 */
@Composable
private fun SyncSystemBars(theme: AppTheme) {
    val dark = theme.isDark(isSystemInDarkTheme())
    val view = LocalView.current
    SideEffect {
        val window = (view.context as Activity).window
        WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = !dark
            isAppearanceLightNavigationBars = !dark
        }
    }
}

@Composable
private fun MainScreen(
    prefs: Prefs,
    theme: AppTheme,
    onThemeChange: (AppTheme) -> Unit,
    onSubmit: (PhoneCandidate) -> Unit,
) {
    val context = LocalContext.current
    var region by remember { mutableStateOf(prefs.region) }
    var flavor by remember { mutableStateOf(prefs.preferredFlavor) }
    var countryOpen by remember { mutableStateOf(false) }
    var flavorOpen by remember { mutableStateOf(false) }
    var themeOpen by remember { mutableStateOf(false) }
    val installed = remember { WhatsAppLauncher.installedFlavors(context) }

    Column(
        Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
    ) {
        Spacer(Modifier.height(24.dp))
        Text(
            stringResource(R.string.headline_title),
            style = MaterialTheme.typography.displaySmall,
        )

        Spacer(Modifier.height(24.dp))
        HowItWorksCard()

        Spacer(Modifier.height(16.dp))
        NumberEntryCard(
            region = region,
            onRegionClick = { countryOpen = true },
            onSubmit = onSubmit,
        )

        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SettingTile(
                label = stringResource(R.string.tile_country),
                value = Countries.byIso(region)
                    ?.let { "${it.flag} ${it.iso} +${it.callingCode}" }
                    ?: stringResource(R.string.choose_country),
                onClick = { countryOpen = true },
                modifier = Modifier.weight(1f),
            )
            SettingTile(
                label = stringResource(R.string.tile_opens_in),
                value = stringResource(flavor.labelRes()),
                onClick = { flavorOpen = true },
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(Modifier.height(12.dp))
        SettingTile(
            label = stringResource(R.string.theme),
            value = stringResource(theme.labelRes()),
            onClick = { themeOpen = true },
            modifier = Modifier.fillMaxWidth(),
        )

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

    if (countryOpen) {
        CountryDialog(
            onDismiss = { countryOpen = false },
            onSelect = {
                prefs.region = it.iso
                region = it.iso
                countryOpen = false
            },
        )
    }

    if (themeOpen) {
        ThemeDialog(
            selected = theme,
            onSelect = {
                onThemeChange(it)
                themeOpen = false
            },
            onDismiss = { themeOpen = false },
        )
    }

    if (flavorOpen) {
        AlertDialog(
            onDismissRequest = { flavorOpen = false },
            confirmButton = {
                TextButton(onClick = { flavorOpen = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
            title = { Text(stringResource(R.string.open_with)) },
            text = {
                Column {
                    WhatsAppFlavor.entries.forEach { option ->
                        val name = stringResource(option.labelRes())
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = option == flavor,
                                onClick = {
                                    prefs.preferredFlavor = option
                                    flavor = option
                                    flavorOpen = false
                                },
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                // The preference still applies when the app is missing:
                                // openChat falls back to whichever flavor is present.
                                if (option in installed) {
                                    name
                                } else {
                                    "$name ${stringResource(R.string.not_installed_suffix)}"
                                },
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        }
                    }
                }
            },
        )
    }
}

@Composable
private fun ThemeDialog(
    selected: AppTheme,
    onSelect: (AppTheme) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
        title = { Text(stringResource(R.string.theme)) },
        text = {
            Column {
                AppTheme.entries.forEach { option ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = option == selected, onClick = { onSelect(option) })
                        Spacer(Modifier.width(8.dp))
                        Text(
                            stringResource(option.labelRes()),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
            }
        },
    )
}

private fun AppTheme.labelRes() = when (this) {
    AppTheme.SYSTEM -> R.string.theme_system
    AppTheme.LIGHT -> R.string.theme_light
    AppTheme.DARK -> R.string.theme_dark
    AppTheme.AMOLED -> R.string.theme_amoled
    AppTheme.GREEN -> R.string.theme_green
    AppTheme.DYNAMIC -> R.string.theme_dynamic
}

private fun WhatsAppFlavor.labelRes() = when (this) {
    WhatsAppFlavor.STANDARD -> R.string.whatsapp_standard
    WhatsAppFlavor.BUSINESS -> R.string.whatsapp_business
}
