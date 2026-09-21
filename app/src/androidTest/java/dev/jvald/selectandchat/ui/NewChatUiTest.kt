package dev.jvald.selectandchat.ui

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.platform.app.InstrumentationRegistry
import dev.jvald.selectandchat.R
import dev.jvald.selectandchat.core.Countries
import dev.jvald.selectandchat.core.Country
import dev.jvald.selectandchat.core.PhoneNumberExtractor
import dev.jvald.selectandchat.core.WhatsAppFlavor
import dev.jvald.selectandchat.ui.theme.SelectAndChatTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import java.util.Locale

class PhoneNumberFieldUiTest {

    @get:Rule
    val rule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val english = Locale.ENGLISH

    @Test
    fun theCountryPillReadsOutAsOneControlNamingTheCountryAndCode() {
        val sv = Countries.byIso("SV", english)!!
        rule.setContent {
            SelectAndChatTheme {
                PhoneNumberField(
                    input = "",
                    onInputChange = {},
                    country = sv,
                    feedback = PhoneNumberExtractor.checkLength("", "SV"),
                    onCountryClick = {},
                )
            }
        }

        rule.onNodeWithContentDescription(
            context.getString(R.string.country_calling_code, sv.displayName, sv.dialCode),
        ).assertIsDisplayed().assertHasClickAction()
    }

    @Test
    fun theClearButtonAppearsOnlyWhenThereIsSomethingToClear() {
        var value = ""
        rule.setContent {
            SelectAndChatTheme {
                PhoneNumberField(
                    input = value,
                    onInputChange = { value = it },
                    country = Countries.byIso("SV", english),
                    feedback = PhoneNumberExtractor.checkLength(value, "SV"),
                    onCountryClick = {},
                )
            }
        }

        rule.onNodeWithContentDescription(context.getString(R.string.clear_number))
            .assertDoesNotExist()
    }

    @Test
    fun searchingTheCountrySheetMatchesNamesAndCallingCodes() {
        var picked: Country? = null
        rule.setContent {
            SelectAndChatTheme {
                CountrySheet(
                    selectedIso = "SV",
                    locale = english,
                    onSelect = { picked = it },
                    onDismiss = {},
                )
            }
        }

        rule.onNodeWithText(context.getString(R.string.search_country_or_code))
            .performTextInput("+502")
        rule.onNodeWithText("Guatemala").assertIsDisplayed().performClick()
        rule.runOnIdle { assertEquals("GT", picked?.iso) }
    }
}

class WhatsAppSplitButtonUiTest {

    @get:Rule
    val rule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun theTwoHalvesAreIndependentlyReachable() {
        var opened = false
        var chosen: WhatsAppFlavor? = null
        rule.setContent {
            SelectAndChatTheme {
                WhatsAppSplitButton(
                    flavor = WhatsAppFlavor.STANDARD,
                    enabled = true,
                    installedFlavors = setOf(WhatsAppFlavor.STANDARD, WhatsAppFlavor.BUSINESS),
                    onOpen = { opened = true },
                    onFlavorChange = { chosen = it },
                )
            }
        }

        rule.onNodeWithContentDescription(context.getString(R.string.choose_whatsapp_app))
            .performClick()
        rule.runOnIdle { assertEquals(false, opened) }

        rule.onNodeWithText(context.getString(R.string.whatsapp_business)).performClick()
        rule.runOnIdle { assertEquals(WhatsAppFlavor.BUSINESS, chosen) }

        rule.onNodeWithText(context.getString(R.string.open_in_whatsapp)).performClick()
        rule.runOnIdle { assertEquals(true, opened) }
    }

    @Test
    fun theLabelNamesTheDestinationThatIsSelected() {
        rule.setContent {
            SelectAndChatTheme {
                WhatsAppSplitButton(
                    flavor = WhatsAppFlavor.BUSINESS,
                    enabled = true,
                    installedFlavors = setOf(WhatsAppFlavor.BUSINESS),
                    onOpen = {},
                    onFlavorChange = {},
                )
            }
        }
        rule.onNodeWithText(context.getString(R.string.open_in_whatsapp_business))
            .assertIsDisplayed()
    }

    @Test
    fun aDestinationThatIsNotInstalledSaysSoRatherThanFailingSilently() {
        rule.setContent {
            SelectAndChatTheme {
                WhatsAppSplitButton(
                    flavor = WhatsAppFlavor.STANDARD,
                    enabled = true,
                    installedFlavors = setOf(WhatsAppFlavor.STANDARD),
                    onOpen = {},
                    onFlavorChange = {},
                )
            }
        }

        rule.onNodeWithContentDescription(context.getString(R.string.choose_whatsapp_app))
            .performClick()
        rule.onNodeWithText(
            context.getString(R.string.whatsapp_business) + " " +
                context.getString(R.string.not_installed_suffix),
        ).assertIsDisplayed()
    }

    @Test
    fun anUnusableNumberLeavesBothHalvesDisabled() {
        rule.setContent {
            SelectAndChatTheme {
                WhatsAppSplitButton(
                    flavor = WhatsAppFlavor.STANDARD,
                    enabled = false,
                    installedFlavors = setOf(WhatsAppFlavor.STANDARD),
                    onOpen = {},
                    onFlavorChange = {},
                )
            }
        }
        rule.onNodeWithText(context.getString(R.string.open_in_whatsapp)).assertIsNotEnabled()
        rule.onNodeWithContentDescription(context.getString(R.string.choose_whatsapp_app))
            .assertIsNotEnabled()
    }
}
