package dev.jvald.selectandchat.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import dev.jvald.selectandchat.R
import dev.jvald.selectandchat.core.Countries
import dev.jvald.selectandchat.core.Country
import dev.jvald.selectandchat.core.PhoneCandidate
import dev.jvald.selectandchat.core.PhoneNumberExtractor

/** One resolved number, tappable. */
@Composable
fun CandidateRow(candidate: PhoneCandidate, onClick: () -> Unit) {
    OutlinedCard(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text(candidate.international, style = MaterialTheme.typography.titleMedium)
            Countries.byIso(candidate.regionCode)?.let { country ->
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

/** Tappable country field that opens a searchable dialog. */
@Composable
fun CountryField(
    selected: Country?,
    onSelect: (Country) -> Unit,
    modifier: Modifier = Modifier,
) {
    var open by remember { mutableStateOf(false) }

    OutlinedCard(onClick = { open = true }, modifier = modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(
                    stringResource(R.string.country),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    selected?.label ?: stringResource(R.string.choose_country),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
            Text("▾", style = MaterialTheme.typography.titleLarge)
        }
    }

    if (open) {
        CountryDialog(
            onDismiss = { open = false },
            onSelect = {
                open = false
                onSelect(it)
            },
        )
    }
}

@Composable
private fun CountryDialog(onDismiss: () -> Unit, onSelect: (Country) -> Unit) {
    var query by remember { mutableStateOf("") }
    val results = remember(query) { Countries.search(query) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
        title = { Text(stringResource(R.string.choose_country)) },
        text = {
            Column {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text(stringResource(R.string.search_countries)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                LazyColumn(Modifier.heightIn(max = 360.dp)) {
                    items(results, key = { it.iso }) { country ->
                        Text(
                            country.label,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelect(country) }
                                .padding(vertical = 14.dp),
                        )
                    }
                }
            }
        },
    )
}

/**
 * Country + number entry, used both in the fallback sheet and on the main screen.
 * The resolved international form is echoed under the field so it is obvious which
 * number is actually about to be opened.
 */
@Composable
fun ManualEntry(
    initialNumber: String,
    region: String?,
    onRegionChange: (String) -> Unit,
    onSubmit: (PhoneCandidate) -> Unit,
) {
    var input by remember { mutableStateOf(initialNumber) }
    val parsed = remember(input, region) { PhoneNumberExtractor.parseManual(input, region) }

    Column {
        CountryField(
            selected = Countries.byIso(region),
            onSelect = { onRegionChange(it.iso) },
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = input,
            onValueChange = { input = it },
            label = { Text(stringResource(R.string.enter_a_number)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            supportingText = parsed?.let { { Text(it.international) } },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = { parsed?.let(onSubmit) },
            enabled = parsed != null,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.open_chat))
        }
    }
}
