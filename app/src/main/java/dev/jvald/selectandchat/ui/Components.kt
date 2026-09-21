package dev.jvald.selectandchat.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.jvald.selectandchat.R
import dev.jvald.selectandchat.core.Countries
import dev.jvald.selectandchat.core.Country
import dev.jvald.selectandchat.core.LengthCheck
import dev.jvald.selectandchat.core.NumberFeedback
import dev.jvald.selectandchat.core.PhoneCandidate
import dev.jvald.selectandchat.core.PhoneNumberExtractor
import dev.jvald.selectandchat.ui.theme.AppIcons
import java.util.Locale

/**
 * The locale the app is actually rendering in.
 *
 * Read from the configuration rather than `Locale.getDefault()` so that a per-app language
 * change recomposes everything derived from it — country names most of all.
 */
@Composable
fun currentLocale(): Locale {
    val configuration = LocalConfiguration.current
    return remember(configuration) {
        configuration.locales.takeIf { it.size() > 0 }?.get(0) ?: Locale.getDefault()
    }
}

/**
 * Warning shown when the digits typed cannot dial in the selected country. Silent while
 * the number is merely incomplete, so it never nags mid-typing.
 */
@Composable
fun LengthWarning(feedback: NumberFeedback, country: Country?) {
    if (!feedback.shouldWarn) return
    val name = country?.displayName ?: return
    val expected = feedback.expectedDigits
    Text(
        when {
            expected != null && feedback.check == LengthCheck.TOO_LONG ->
                pluralStringResource(R.plurals.warning_too_long, expected, name, expected)

            expected != null ->
                pluralStringResource(R.plurals.warning_wrong_length_hint, expected, name, expected)
            else -> stringResource(R.string.warning_wrong_length, name)
        },
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.error,
    )
}

/** One resolved number, tappable. Used by the selection-toolbar sheet. */
@Composable
fun CandidateRow(candidate: PhoneCandidate, onClick: () -> Unit) {
    val locale = currentLocale()
    OutlinedCard(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text(candidate.international, style = MaterialTheme.typography.titleMedium)
            Countries.byIso(candidate.regionCode, locale)?.let { country ->
                Text(
                    "${country.flag}  ${country.displayName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (!candidate.confident) {
                Text(
                    stringResource(R.string.unsure_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

/**
 * Country + number entry for the selection-toolbar sheet, where the full screen's layout
 * does not apply but the same country picker does.
 */
@Composable
fun ManualEntry(
    initialNumber: String,
    region: String?,
    onRegionChange: (String) -> Unit,
    onSubmit: (PhoneCandidate) -> Unit,
) {
    val locale = currentLocale()
    var input by remember { mutableStateOf(initialNumber) }
    var pickingCountry by remember { mutableStateOf(false) }
    val parsed = remember(input, region) { PhoneNumberExtractor.parseManual(input, region) }
    val feedback = remember(input, region) { PhoneNumberExtractor.checkLength(input, region) }

    Column {
        PhoneNumberField(
            input = input,
            onInputChange = { input = it },
            country = Countries.byIso(region, locale),
            feedback = feedback,
            onCountryClick = { pickingCountry = true },
        )
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = { parsed?.let(onSubmit) },
            enabled = parsed != null,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.open_in_whatsapp))
        }
    }

    if (pickingCountry) {
        CountrySheet(
            selectedIso = region,
            locale = locale,
            onSelect = {
                pickingCountry = false
                onRegionChange(it.iso)
            },
            onDismiss = { pickingCountry = false },
        )
    }
}

/** The explainer card, dismissible once the user has the idea. */
@Composable
fun HowItWorksCard(onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(24.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(start = 20.dp, top = 16.dp, end = 8.dp, bottom = 20.dp)) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        AppIcons.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        stringResource(R.string.how_it_works),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.how_it_works_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onDismiss) {
                Icon(
                    AppIcons.Close,
                    contentDescription = stringResource(R.string.dismiss),
                )
            }
        }
    }
}
