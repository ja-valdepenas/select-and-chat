package dev.jvald.selectandchat.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import dev.jvald.selectandchat.R
import dev.jvald.selectandchat.core.HistoryEntry
import dev.jvald.selectandchat.core.PhoneNumberExtractor
import dev.jvald.selectandchat.ui.theme.SelectAndChatTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.util.Locale

class RecentsUiTest {

    @get:Rule
    val rule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private val entries = listOf(
        HistoryEntry("+50362041006", null, System.currentTimeMillis()),
        HistoryEntry("+50370000000", "Plumber", System.currentTimeMillis() - 90_000),
    )

    private fun setContent(
        historyEnabled: Boolean = true,
        list: List<HistoryEntry> = entries,
        onManageHistory: () -> Unit = {},
        onHistoryEnabledChange: (Boolean) -> Unit = {},
    ) {
        rule.setContent {
            SelectAndChatTheme {
                RecentsScreen(
                    entries = list,
                    historyEnabled = historyEnabled,
                    locale = Locale.ENGLISH,
                    onOpen = {},
                    onSetLabel = { _, _ -> },
                    onSaveContact = {},
                    onRemove = {},
                    onHistoryEnabledChange = onHistoryEnabledChange,
                    onManageHistory = onManageHistory,
                )
            }
        }
    }

    @Test
    fun clearHistoryIsNotAButtonOnThisScreen() {
        setContent()
        rule.onNodeWithText(context.getString(R.string.clear_history)).assertDoesNotExist()
    }

    @Test
    fun clearingIsReachedThroughManageHistory() {
        var managed = false
        setContent(onManageHistory = { managed = true })
        rule.onNodeWithText(context.getString(R.string.manage_history)).performClick()
        rule.runOnIdle { assertEquals(true, managed) }
    }

    @Test
    fun eachOverflowButtonNamesItsOwnNumber() {
        setContent()
        entries.forEach { entry ->
            val formatted = PhoneNumberExtractor.parseManual(entry.e164, null)!!.international
            rule.onNodeWithContentDescription(
                context.getString(R.string.more_options_for, formatted),
            ).assertIsDisplayed()
        }
    }

    @Test
    fun turningTheSwitchOffKeepsWhatIsAlreadySaved() {
        setContent(historyEnabled = false)
        rule.onNodeWithText(context.getString(R.string.save_recent_chats)).assertIsDisplayed()
        // The entries are still listed; only new ones stop being recorded.
        rule.onNodeWithText("Plumber").assertIsDisplayed()
        rule.onNodeWithText(context.getString(R.string.history_off_note)).assertIsDisplayed()
    }

    @Test
    fun theSwitchReflectsWhetherRecordingIsOn() {
        setContent(historyEnabled = true)
        rule.onNodeWithText(context.getString(R.string.save_recent_chats)).assertIsDisplayed()
    }

    @Test
    fun clearingEverythingAsksFirst() {
        var cleared = false
        rule.setContent {
            SelectAndChatTheme {
                ManageHistorySheet(
                    entries = entries,
                    onRemove = {},
                    onClearAll = { cleared = true },
                    onDismiss = {},
                )
            }
        }

        rule.onNodeWithText(context.getString(R.string.clear_history)).performClick()
        rule.onNodeWithText(context.getString(R.string.clear_history_body)).assertIsDisplayed()
        rule.runOnIdle { assertEquals(false, cleared) }

        rule.onNodeWithText(context.getString(R.string.clear)).performClick()
        rule.runOnIdle { assertEquals(true, cleared) }
    }
}
