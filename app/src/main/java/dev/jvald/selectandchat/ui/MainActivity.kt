package dev.jvald.selectandchat.ui

import android.app.Activity
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ShortNavigationBar
import androidx.compose.material3.ShortNavigationBarItem
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
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import dev.jvald.selectandchat.R
import dev.jvald.selectandchat.core.AppTheme
import dev.jvald.selectandchat.core.Contacts
import dev.jvald.selectandchat.core.Countries
import dev.jvald.selectandchat.core.HistoryEntry
import dev.jvald.selectandchat.core.HistoryStore
import dev.jvald.selectandchat.core.PhoneCandidate
import dev.jvald.selectandchat.core.PhoneNumberExtractor
import dev.jvald.selectandchat.core.Prefs
import dev.jvald.selectandchat.core.TemplateStore
import dev.jvald.selectandchat.core.WhatsAppFlavor
import dev.jvald.selectandchat.core.WhatsAppLauncher
import dev.jvald.selectandchat.ui.theme.SelectAndChatTheme

private enum class Destination { NEW, RECENTS, SETTINGS }

class MainActivity : ComponentActivity() {

    private lateinit var prefs: Prefs
    private lateinit var history: HistoryStore
    private lateinit var templates: TemplateStore

    /** A number spotted in the clipboard, refreshed only when the app comes to the front. */
    private var clipboardNumber by mutableStateOf<String?>(null)
    private var clipboardChecked = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        prefs = Prefs(this)
        history = HistoryStore(this)
        templates = TemplateStore(this)
        prefs.seedRegionIfUnset(this)

        setContent {
            var theme by remember { mutableStateOf(prefs.theme) }
            SelectAndChatTheme(theme = theme) {
                SyncSystemBars(theme)
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    AppScaffold(
                        prefs = prefs,
                        history = history,
                        templates = templates,
                        theme = theme,
                        clipboardNumber = clipboardNumber,
                        onClipboardUsed = { clipboardNumber = null },
                        onThemeChange = {
                            prefs.theme = it
                            theme = it
                        },
                        onOpenChat = ::openChat,
                        onSaveContact = { entry ->
                            if (!Contacts.saveNumber(this, entry.e164, entry.label)) {
                                toast(R.string.no_contacts_app)
                            }
                        },
                    )
                }
            }
        }
    }

    /**
     * Read the clipboard only once the window actually has focus, and only per launch.
     * Android shows the user a toast every time an app reads the clipboard, so polling it
     * would be both noisy and indistinguishable from spyware.
     */
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (!hasFocus || clipboardChecked) return
        clipboardChecked = true
        clipboardNumber = readNumberFromClipboard()
    }

    private fun readNumberFromClipboard(): String? {
        val manager = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return null
        val text = runCatching {
            manager.primaryClip?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.text?.toString()
        }.getOrNull() ?: return null
        val extraction = PhoneNumberExtractor.extract(text, prefs.region)
        return extraction.candidates.firstOrNull { it.confident }?.e164
    }

    private fun openChat(candidate: PhoneCandidate, message: String?) {
        val opened = WhatsAppLauncher.openChat(
            context = this,
            e164 = candidate.e164,
            preferred = prefs.preferredFlavor,
            message = message,
        )
        if (opened) history.record(candidate.e164) else toast(R.string.whatsapp_not_installed)
    }

    private fun toast(resId: Int) = Toast.makeText(this, resId, Toast.LENGTH_LONG).show()
}

@Composable
private fun AppScaffold(
    prefs: Prefs,
    history: HistoryStore,
    templates: TemplateStore,
    theme: AppTheme,
    clipboardNumber: String?,
    onClipboardUsed: () -> Unit,
    onThemeChange: (AppTheme) -> Unit,
    onOpenChat: (PhoneCandidate, String?) -> Unit,
    onSaveContact: (HistoryEntry) -> Unit,
) {
    var destination by remember { mutableStateOf(Destination.NEW) }
    var region by remember { mutableStateOf(prefs.region) }
    var flavor by remember { mutableStateOf(prefs.preferredFlavor) }
    var historyEnabled by remember { mutableStateOf(history.enabled) }
    var entries by remember { mutableStateOf(history.all()) }
    var templateList by remember { mutableStateOf(templates.all()) }
    var howItWorksDismissed by remember { mutableStateOf(prefs.howItWorksDismissed) }

    var countryOpen by remember { mutableStateOf(false) }
    var flavorOpen by remember { mutableStateOf(false) }
    var themeOpen by remember { mutableStateOf(false) }
    var addTemplateOpen by remember { mutableStateOf(false) }
    var prefill by remember { mutableStateOf<String?>(null) }

    // Android convention: back from a secondary tab returns to the first one.
    BackHandler(enabled = destination != Destination.NEW) { destination = Destination.NEW }

    Scaffold(
        bottomBar = {
            ShortNavigationBar {
                Destination.entries.forEach { item ->
                    ShortNavigationBarItem(
                        selected = destination == item,
                        onClick = {
                            if (item == Destination.RECENTS) entries = history.all()
                            destination = item
                        },
                        icon = { Icon(item.icon(), contentDescription = null) },
                        label = { Text(stringResource(item.labelRes())) },
                    )
                }
            }
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            when (destination) {
                Destination.NEW -> NewChatScreen(
                    region = region,
                    templates = templateList,
                    clipboardNumber = clipboardNumber,
                    prefill = prefill,
                    howItWorksDismissed = howItWorksDismissed,
                    onDismissHowItWorks = {
                        prefs.howItWorksDismissed = true
                        howItWorksDismissed = true
                    },
                    onUseClipboard = {
                        prefill = it
                        onClipboardUsed()
                    },
                    onRegionClick = { countryOpen = true },
                    onAddTemplate = { addTemplateOpen = true },
                    onSubmit = { candidate, message ->
                        onOpenChat(candidate, message)
                        entries = history.all()
                    },
                )

                Destination.RECENTS -> RecentsScreen(
                    entries = entries,
                    historyEnabled = historyEnabled,
                    onOpen = { entry ->
                        PhoneNumberExtractor.parseManual(entry.e164, null)?.let {
                            onOpenChat(it, null)
                        }
                        entries = history.all()
                    },
                    onSetLabel = { entry, label ->
                        history.setLabel(entry.e164, label)
                        entries = history.all()
                    },
                    onSaveContact = onSaveContact,
                    onRemove = { entry ->
                        history.remove(entry.e164)
                        entries = history.all()
                    },
                    onClearAll = {
                        history.clear()
                        entries = emptyList()
                    },
                )

                Destination.SETTINGS -> SettingsScreen(
                    region = region,
                    flavor = flavor,
                    theme = theme,
                    historyEnabled = historyEnabled,
                    templates = templateList,
                    onCountryClick = { countryOpen = true },
                    onFlavorClick = { flavorOpen = true },
                    onThemeClick = { themeOpen = true },
                    onHistoryEnabledChange = {
                        history.enabled = it
                        historyEnabled = it
                        entries = history.all()
                    },
                    onRemoveTemplate = {
                        templates.remove(it)
                        templateList = templates.all()
                    },
                    onAddTemplate = { addTemplateOpen = true },
                )
            }
        }
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
        FlavorDialog(
            selected = flavor,
            onSelect = {
                prefs.preferredFlavor = it
                flavor = it
                flavorOpen = false
            },
            onDismiss = { flavorOpen = false },
        )
    }

    if (addTemplateOpen) {
        AddTemplateDialog(
            onDismiss = { addTemplateOpen = false },
            onConfirm = {
                templates.add(it)
                templateList = templates.all()
                addTemplateOpen = false
            },
        )
    }
}

@Composable
private fun NewChatScreen(
    region: String?,
    templates: List<String>,
    clipboardNumber: String?,
    prefill: String?,
    howItWorksDismissed: Boolean,
    onDismissHowItWorks: () -> Unit,
    onUseClipboard: (String) -> Unit,
    onRegionClick: () -> Unit,
    onAddTemplate: () -> Unit,
    onSubmit: (PhoneCandidate, String?) -> Unit,
) {
    Column(Modifier.padding(horizontal = 24.dp)) {
        Spacer(Modifier.height(24.dp))
        Text(
            stringResource(R.string.headline_title),
            style = MaterialTheme.typography.displaySmall,
        )

        if (!howItWorksDismissed) {
            Spacer(Modifier.height(20.dp))
            HowItWorksCard(onDismiss = onDismissHowItWorks)
        }

        if (clipboardNumber != null) {
            Spacer(Modifier.height(12.dp))
            ClipboardStrip(number = clipboardNumber, onUse = { onUseClipboard(clipboardNumber) })
        }

        Spacer(Modifier.height(16.dp))
        NumberEntryCard(
            region = region,
            templates = templates,
            prefill = prefill,
            onRegionClick = onRegionClick,
            onAddTemplate = onAddTemplate,
            onSubmit = onSubmit,
        )
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun ClipboardStrip(number: String, onUse: () -> Unit) {
    val formatted = remember(number) {
        PhoneNumberExtractor.parseManual(number, null)?.international ?: number
    }
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(22.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier.padding(start = 16.dp, top = 12.dp, end = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Default.Info,
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
            TextButton(onClick = onUse) { Text(stringResource(R.string.use_it)) }
        }
    }
}

@Composable
private fun AddTemplateDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_message)) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text(stringResource(R.string.message_hint)) },
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(text) },
                enabled = text.isNotBlank(),
            ) { Text(stringResource(R.string.save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
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
                    ChoiceRow(
                        label = stringResource(option.labelRes()),
                        selected = option == selected,
                        onClick = { onSelect(option) },
                    )
                }
            }
        },
    )
}

@Composable
private fun FlavorDialog(
    selected: WhatsAppFlavor,
    onSelect: (WhatsAppFlavor) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val installed = remember { WhatsAppLauncher.installedFlavors(context) }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
        title = { Text(stringResource(R.string.open_with)) },
        text = {
            Column {
                WhatsAppFlavor.entries.forEach { option ->
                    val name = stringResource(option.labelRes())
                    ChoiceRow(
                        // The preference still applies when the app is missing: openChat
                        // falls back to whichever flavor is actually installed.
                        label = if (option in installed) {
                            name
                        } else {
                            "$name ${stringResource(R.string.not_installed_suffix)}"
                        },
                        selected = option == selected,
                        onClick = { onSelect(option) },
                    )
                }
            }
        },
    )
}

@Composable
private fun ChoiceRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Spacer(Modifier.width(8.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge)
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

private fun Destination.labelRes() = when (this) {
    Destination.NEW -> R.string.tab_new
    Destination.RECENTS -> R.string.tab_recents
    Destination.SETTINGS -> R.string.tab_settings
}

private fun Destination.icon() = when (this) {
    Destination.NEW -> Icons.Default.Create
    Destination.RECENTS -> Icons.Default.List
    Destination.SETTINGS -> Icons.Default.Settings
}

internal fun AppTheme.labelRes() = when (this) {
    AppTheme.SYSTEM -> R.string.theme_system
    AppTheme.LIGHT -> R.string.theme_light
    AppTheme.DARK -> R.string.theme_dark
    AppTheme.AMOLED -> R.string.theme_amoled
    AppTheme.GREEN -> R.string.theme_green
    AppTheme.DYNAMIC -> R.string.theme_dynamic
}

internal fun WhatsAppFlavor.labelRes() = when (this) {
    WhatsAppFlavor.STANDARD -> R.string.whatsapp_standard
    WhatsAppFlavor.BUSINESS -> R.string.whatsapp_business
}
