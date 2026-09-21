package dev.jvald.selectandchat.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.test.platform.app.InstrumentationRegistry
import dev.jvald.selectandchat.R
import dev.jvald.selectandchat.core.QuickMessage
import dev.jvald.selectandchat.ui.theme.SelectAndChatTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class QuickMessagesUiTest {

    @get:Rule
    val rule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private val messages = listOf(
        QuickMessage("a", "Hola, ¿cómo estás?"),
        QuickMessage("b", "On my way"),
        QuickMessage("c", "Can we talk?"),
        QuickMessage("d", "Thanks, I'll write later"),
    )

    private fun setContent(
        onEdit: (QuickMessage) -> Unit = {},
        onDelete: (QuickMessage) -> Unit = {},
    ) {
        rule.setContent {
            var selected by remember { mutableStateOf<String?>(null) }
            SelectAndChatTheme {
                QuickMessagesSection(
                    messages = messages,
                    selectedId = selected,
                    onToggle = { selected = if (selected == it.id) null else it.id },
                    onAdd = {},
                    onEdit = onEdit,
                    onDelete = onDelete,
                )
            }
        }
    }

    @Test
    fun everyMessageIsOnScreenWithoutScrollingSideways() {
        setContent()
        messages.forEach { rule.onNodeWithText(it.text).assertIsDisplayed() }
    }

    @Test
    fun theAddButtonIsReachableAndLabelled() {
        setContent()
        rule.onNodeWithContentDescription(context.getString(R.string.add_quick_message))
            .assertIsDisplayed()
    }

    @Test
    fun tappingAChipSelectsItAndSaysSoBeyondColour() {
        setContent()
        rule.onNodeWithText(messages[0].text).performClick()
        rule.onNodeWithText(messages[0].text).assertIsSelected()
        rule.onNodeWithText(messages[0].text).assert(
            SemanticsMatcher.expectValue(
                SemanticsProperties.StateDescription,
                context.getString(R.string.quick_message_selected),
            ),
        )
        rule.onNodeWithText(context.getString(R.string.selected_message_helper)).assertIsDisplayed()
    }

    @Test
    fun tappingTheSelectedChipAgainClearsIt() {
        setContent()
        rule.onNodeWithText(messages[0].text).performClick()
        rule.onNodeWithText(messages[0].text).performClick()
        rule.onNodeWithText(messages[0].text).assertIsNotSelected()
    }

    @Test
    fun onlyOneMessageIsSelectedAtATime() {
        setContent()
        rule.onNodeWithText(messages[0].text).performClick()
        rule.onNodeWithText(messages[1].text).performClick()
        rule.onNodeWithText(messages[0].text).assertIsNotSelected()
        rule.onNodeWithText(messages[1].text).assertIsSelected()
    }

    @Test
    fun longPressOffersEditAndDelete() {
        var edited: QuickMessage? = null
        setContent(onEdit = { edited = it })

        rule.onNodeWithText(messages[0].text).performTouchInput { longClick() }
        rule.onNodeWithText(context.getString(R.string.delete)).assertIsDisplayed()
        rule.onNodeWithText(context.getString(R.string.edit)).performClick()

        rule.runOnIdle { assertEquals(messages[0], edited) }
    }

    @Test
    fun editAndDeleteAreAlsoAvailableAsAccessibilityActions() {
        setContent()
        val actions = rule.onNodeWithText(messages[0].text)
            .fetchSemanticsNode()
            .config[SemanticsActions.CustomActions]
            .map { it.label }

        assertEquals(
            listOf(context.getString(R.string.edit), context.getString(R.string.delete)),
            actions,
        )
    }

    @Test
    fun theManagementSheetExposesEditAndDeleteAsOrdinaryButtons() {
        var deleted: QuickMessage? = null
        rule.setContent {
            SelectAndChatTheme {
                ManageQuickMessagesSheet(
                    messages = messages,
                    onAdd = {},
                    onEdit = {},
                    onDelete = { deleted = it },
                    onDismiss = {},
                )
            }
        }

        rule.onAllNodesWithContentDescription(context.getString(R.string.delete))[0].performClick()
        rule.runOnIdle { assertEquals(messages[0], deleted) }
    }
}
