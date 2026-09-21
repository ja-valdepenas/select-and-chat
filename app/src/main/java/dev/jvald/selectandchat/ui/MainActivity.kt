package dev.jvald.selectandchat.ui

import android.app.Activity
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.ComponentActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.jvald.selectandchat.R
import dev.jvald.selectandchat.core.AppLanguage
import dev.jvald.selectandchat.core.AppLocales
import dev.jvald.selectandchat.core.Contacts
import dev.jvald.selectandchat.core.HistoryEntry
import dev.jvald.selectandchat.core.PhoneCandidate
import dev.jvald.selectandchat.core.PhoneNumberExtractor
import dev.jvald.selectandchat.core.QuickMessage
import dev.jvald.selectandchat.core.SettingsRepository
import dev.jvald.selectandchat.core.ThemePreference
import dev.jvald.selectandchat.core.WhatsAppFlavor
import dev.jvald.selectandchat.core.WhatsAppLauncher
import dev.jvald.selectandchat.ui.theme.SelectAndChatTheme
import kotlinx.coroutines.launch

private enum class Destination { NEW, RECENTS }

class MainActivity : ComponentActivity() {

    /** A number spotted in the clipboard, refreshed only when the app comes to the front. */
    private var clipboardChecked = false
    private var pendingClipboardNumber by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val viewModel: MainViewModel = viewModel()
            val state by viewModel.uiState.collectAsStateWithLifecycle()

            LaunchedEffect(pendingClipboardNumber) {
                pendingClipboardNumber?.let(viewModel::onClipboardNumberFound)
            }

            SelectAndChatTheme(theme = state.theme) {
                SyncSystemBars(state.theme)
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    AppScaffold(state = state, viewModel = viewModel)
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
        pendingClipboardNumber = readNumberFromClipboard()
    }

    private fun readNumberFromClipboard(): String? {
        val manager = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return null
        val text = runCatching {
            manager.primaryClip?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.text?.toString()
        }.getOrNull() ?: return null
        val region = SettingsRepository(this).blockingSnapshot().region
        val extraction = PhoneNumberExtractor.extract(text, region)
        return extraction.candidates.firstOrNull { it.confident }?.e164
    }
}

@Composable
private fun AppScaffold(state: MainUiState, viewModel: MainViewModel) {
    val context = LocalContext.current
    val locale = currentLocale()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val pagerState = rememberPagerState(pageCount = { Destination.entries.size })
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current

    // The language is not ours to store, so it is read back from the platform. Keying on
    // the configuration is what re-reads it after a change takes effect.
    val configuration = LocalConfiguration.current
    val language = remember(configuration) { AppLocales.current(context) }

    var showingSettings by remember { mutableStateOf(false) }
    var pickingCountry by remember { mutableStateOf(false) }
    var editingMessage by remember { mutableStateOf<QuickMessage?>(null) }
    var addingMessage by remember { mutableStateOf(false) }
    var managingMessages by remember { mutableStateOf(false) }
    var managingHistory by remember { mutableStateOf(false) }

    // Both pager pages stay composed, so the number field keeps focus — and with it the
    // dial pad — after a swipe to Recents. Watching targetPage rather than currentPage
    // starts the IME sliding away as the swipe begins, instead of leaving it stranded
    // over whatever arrives.
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.targetPage }
            .collect { page ->
                if (Destination.entries[page] != Destination.NEW) dropFocus(focusManager, keyboard)
            }
    }

    // Anything that opens over the page has the same problem: the field underneath still
    // holds focus, so the IME stays up beneath the sheet and the sheet lays itself out
    // above it. That is what makes Settings and the country list open at the wrong height.
    val overlayOpen = showingSettings || pickingCountry || managingMessages ||
        managingHistory || addingMessage || editingMessage != null
    LaunchedEffect(overlayOpen) {
        if (overlayOpen) dropFocus(focusManager, keyboard)
    }

    // Every string a callback can show is resolved here, in composition: read from the
    // context inside the callback instead and it would keep the language the app started
    // in after a per-app language change.
    val deletedLabel = stringResource(R.string.message_deleted)
    val undoLabel = stringResource(R.string.undo)
    val notInstalledMessage = stringResource(R.string.whatsapp_not_installed)
    val noContactsAppMessage = stringResource(R.string.no_contacts_app)
    val openedElsewhereTemplate = stringResource(R.string.opened_in_other_app)
    val flavorNames = mapOf(
        WhatsAppFlavor.STANDARD to stringResource(R.string.whatsapp_standard),
        WhatsAppFlavor.BUSINESS to stringResource(R.string.whatsapp_business),
    )

    // Android convention: back from a secondary tab returns to the first one.
    BackHandler(enabled = pagerState.currentPage != 0) {
        scope.launch { pagerState.animateScrollToPage(0) }
    }

    // A WhatsApp flavor can be installed while this screen is open, so the list of what
    // is available is re-read every time the app comes back to the front.
    LifecycleResumeEffect(Unit) {
        viewModel.refreshInstalledApps()
        onPauseOrDispose { }
    }

    fun openChat(candidate: PhoneCandidate, message: String?) {
        val opened = WhatsAppLauncher.openChat(
            context = context,
            e164 = candidate.e164,
            preferred = state.flavor,
            message = message,
        )
        if (opened != null) {
            viewModel.recordOpenedChat(candidate.e164)
            if (opened != state.flavor) {
                // The preference is a preference, not a requirement: say which one it
                // actually reached rather than failing because the other one is missing.
                val name = flavorNames.getValue(opened)
                scope.launch {
                    snackbarHostState.showSnackbar(openedElsewhereTemplate.format(name))
                }
            }
        } else {
            scope.launch {
                snackbarHostState.showSnackbar(notInstalledMessage)
            }
        }
    }

    fun deleteQuickMessage(message: QuickMessage) {
        val deleted = viewModel.deleteQuickMessage(message.id) ?: return
        scope.launch {
            val result = snackbarHostState.showSnackbar(
                message = deletedLabel,
                actionLabel = undoLabel,
                duration = SnackbarDuration.Long,
            )
            if (result == SnackbarResult.ActionPerformed) viewModel.restoreQuickMessage(deleted)
        }
    }

    fun saveContact(entry: HistoryEntry) {
        if (!Contacts.saveNumber(context, entry.e164, entry.label)) {
            scope.launch {
                snackbarHostState.showSnackbar(noContactsAppMessage)
            }
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .consumeWindowInsets(padding),
        ) {
            AppHeader(onSettingsClick = { showingSettings = true })

            PrimaryTabRow(
                selectedTabIndex = pagerState.currentPage,
                containerColor = Color.Transparent,
            ) {
                Destination.entries.forEachIndexed { index, item ->
                    Tab(
                        selected = pagerState.currentPage == index,
                        onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                        text = { Text(stringResource(item.labelRes())) },
                    )
                }
            }

            // Both pages stay composed, so a half-typed number survives a peek at Recents.
            HorizontalPager(
                state = pagerState,
                beyondViewportPageCount = 1,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) { page ->
                when (Destination.entries[page]) {
                    Destination.NEW -> NewChatScreen(
                        state = state,
                        locale = locale,
                        onPhoneInputChange = viewModel::onPhoneInputChange,
                        onCountryClick = { pickingCountry = true },
                        onQuickMessageToggle = { viewModel.onQuickMessageToggled(it.id) },
                        onAddQuickMessage = { addingMessage = true },
                        onEditQuickMessage = { editingMessage = it },
                        onDeleteQuickMessage = ::deleteQuickMessage,
                        onUseClipboardNumber = viewModel::useClipboardNumber,
                        onDismissClipboardNumber = viewModel::dismissClipboardNumber,
                        onDismissHowItWorks = viewModel::dismissHowItWorks,
                        onOpenChat = ::openChat,
                        onFlavorChange = viewModel::setFlavor,
                    )

                    Destination.RECENTS -> RecentsScreen(
                        entries = state.recents,
                        historyEnabled = state.historyEnabled,
                        locale = locale,
                        onOpen = { entry ->
                            PhoneNumberExtractor.parseManual(entry.e164, null)?.let {
                                openChat(it, null)
                            }
                        },
                        onSetLabel = { entry, label -> viewModel.setRecentLabel(entry.e164, label) },
                        onSaveContact = ::saveContact,
                        onRemove = { viewModel.removeRecent(it.e164) },
                        onHistoryEnabledChange = viewModel::setHistoryEnabled,
                        onManageHistory = { managingHistory = true },
                    )
                }
            }
        }
    }

    if (showingSettings) {
        SettingsSheet(
            language = language,
            theme = state.theme,
            // Writing the locale restarts the activity, so this is the last thing the
            // current composition does with it.
            onLanguageChange = { AppLocales.apply(context, it) },
            onThemeChange = viewModel::setTheme,
            onManageQuickMessages = { managingMessages = true },
            onDismiss = { showingSettings = false },
        )
    }

    if (pickingCountry) {
        CountrySheet(
            selectedIso = state.region,
            locale = locale,
            onSelect = {
                pickingCountry = false
                viewModel.setRegion(it.iso)
            },
            onDismiss = { pickingCountry = false },
        )
    }

    if (addingMessage) {
        QuickMessageDialog(
            initialText = "",
            onDismiss = { addingMessage = false },
            onSave = {
                viewModel.addQuickMessage(it)
                addingMessage = false
            },
        )
    }

    editingMessage?.let { message ->
        QuickMessageDialog(
            initialText = message.text,
            onDismiss = { editingMessage = null },
            onSave = {
                viewModel.editQuickMessage(message.id, it)
                editingMessage = null
            },
        )
    }

    if (managingMessages) {
        ManageQuickMessagesSheet(
            messages = state.quickMessages,
            onAdd = { addingMessage = true },
            onEdit = { editingMessage = it },
            onDelete = ::deleteQuickMessage,
            onDismiss = { managingMessages = false },
        )
    }

    if (managingHistory) {
        ManageHistorySheet(
            entries = state.recents,
            onRemove = { viewModel.removeRecent(it.e164) },
            onClearAll = viewModel::clearRecents,
            onDismiss = { managingHistory = false },
        )
    }
}

/**
 * Take the caret out of whatever holds it, and the keyboard with it.
 *
 * Clearing focus is what actually ends the text input session; hiding the keyboard as
 * well covers the case where nothing was focused but the IME is still on screen, which
 * happens when a field is removed from composition while it has focus.
 */
private fun dropFocus(focusManager: FocusManager, keyboard: SoftwareKeyboardController?) {
    focusManager.clearFocus()
    keyboard?.hide()
}

/** Title on one line, with the only settings entry point at the far end of the same row. */
@Composable
private fun AppHeader(onSettingsClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 12.dp, top = 12.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            stringResource(R.string.app_title),
            style = MaterialTheme.typography.headlineLargeEmphasized,
            modifier = Modifier.weight(1f),
        )
        SettingsButton(onClick = onSettingsClick)
    }
}

/**
 * Without this the status bar icons keep their launch-time colour, so they vanish against
 * the dark scheme or wash out on the light one.
 */
@Composable
private fun SyncSystemBars(theme: ThemePreference) {
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
    Destination.NEW -> R.string.tab_new_chat
    Destination.RECENTS -> R.string.tab_recents
}
