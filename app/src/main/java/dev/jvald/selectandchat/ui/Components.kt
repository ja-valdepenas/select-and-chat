package dev.jvald.selectandchat.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import dev.jvald.selectandchat.R
import dev.jvald.selectandchat.core.Countries
import dev.jvald.selectandchat.core.Country
import dev.jvald.selectandchat.core.LengthCheck
import dev.jvald.selectandchat.core.NumberFeedback
import dev.jvald.selectandchat.core.PhoneCandidate
import dev.jvald.selectandchat.core.PhoneNumberExtractor

/**
 * Warning shown when the digits typed cannot dial in the selected country. Silent while
 * the number is merely incomplete, so it never nags mid-typing.
 */
@Composable
fun LengthWarning(feedback: NumberFeedback, region: String?) {
    if (!feedback.shouldWarn) return
    val country = Countries.byIso(region)?.displayName ?: return
    val expected = feedback.expectedDigits
    Text(
        when {
            expected != null && feedback.check == LengthCheck.TOO_LONG ->
                stringResource(R.string.warning_too_long, country, expected)

            expected != null -> stringResource(R.string.warning_wrong_length_hint, country, expected)
            else -> stringResource(R.string.warning_wrong_length, country)
        },
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.error,
    )
}

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
fun CountryDialog(onDismiss: () -> Unit, onSelect: (Country) -> Unit) {
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

/** Country + number entry used by the fallback sheet, where space is tight. */
@Composable
fun ManualEntry(
    initialNumber: String,
    region: String?,
    onRegionChange: (String) -> Unit,
    onSubmit: (PhoneCandidate) -> Unit,
) {
    var input by remember { mutableStateOf(initialNumber) }
    val parsed = remember(input, region) { PhoneNumberExtractor.parseManual(input, region) }
    val feedback = remember(input, region) { PhoneNumberExtractor.checkLength(input, region) }

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
            isError = feedback.shouldWarn,
            supportingText = parsed?.let { { Text(it.international) } },
            modifier = Modifier.fillMaxWidth(),
        )
        LengthWarning(feedback, region)
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

/** The explainer card, dismissible once the user has the idea. */
@Composable
fun HowItWorksCard(onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = RoundedCornerShape(28.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(start = 20.dp, top = 16.dp, end = 8.dp, bottom = 20.dp)) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        stringResource(R.string.how_it_works),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.how_it_works_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            IconButton(onClick = onDismiss) {
                Icon(
                    Icons.Default.Clear,
                    contentDescription = stringResource(R.string.dismiss),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
    }
}

/**
 * The number is the hero: country code sits inline and opens the picker on tap, so the
 * screen carries one country control rather than two.
 */
@Composable
fun NumberEntryCard(
    region: String?,
    templates: List<String>,
    prefill: String?,
    onRegionClick: () -> Unit,
    onAddTemplate: () -> Unit,
    onSubmit: (PhoneCandidate, String?) -> Unit,
) {
    var input by remember { mutableStateOf("") }
    var chosenTemplate by remember { mutableStateOf<String?>(null) }
    val parsed = remember(input, region) { PhoneNumberExtractor.parseManual(input, region) }
    val feedback = remember(input, region) { PhoneNumberExtractor.checkLength(input, region) }
    val country = Countries.byIso(region)

    LaunchedEffect(prefill) {
        if (!prefill.isNullOrBlank()) input = prefill
    }

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(28.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(20.dp)) {
            Text(
                stringResource(R.string.message_any_number),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    country?.let { "+${it.callingCode}" } ?: "+?",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(onClick = onRegionClick)
                        .padding(vertical = 8.dp, horizontal = 4.dp),
                )
                TextField(
                    value = input,
                    onValueChange = { input = it },
                    placeholder = {
                        Text(
                            stringResource(R.string.number_placeholder),
                            style = MaterialTheme.typography.headlineSmall,
                        )
                    },
                    textStyle = MaterialTheme.typography.headlineSmall,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                    ),
                    modifier = Modifier.weight(1f),
                )
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(Modifier.height(8.dp))
            LengthWarning(feedback, region)

            Spacer(Modifier.height(12.dp))
            Text(
                stringResource(R.string.start_with),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(6.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                templates.forEach { template ->
                    FilterChip(
                        selected = template == chosenTemplate,
                        onClick = {
                            chosenTemplate = if (template == chosenTemplate) null else template
                        },
                        label = { Text(template, maxLines = 1) },
                    )
                }
                AssistChip(
                    onClick = onAddTemplate,
                    label = { Text(stringResource(R.string.add_message)) },
                    leadingIcon = {
                        Icon(Icons.Default.Add, contentDescription = null, Modifier.size(18.dp))
                    },
                )
            }

            Spacer(Modifier.height(16.dp))
            Button(
                onClick = { parsed?.let { onSubmit(it, chosenTemplate) } },
                enabled = parsed != null,
                shape = RoundedCornerShape(50),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
            ) {
                Text(
                    // The one place emphasis is spent on this screen: the primary action.
                    stringResource(R.string.open_chat),
                    style = MaterialTheme.typography.titleMediumEmphasized,
                )
            }
        }
    }
}

/** Tonal settings tile. */
@Composable
fun SettingTile(
    label: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = RoundedCornerShape(24.dp),
        modifier = modifier,
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                value,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                maxLines = 2,
            )
        }
    }
}
