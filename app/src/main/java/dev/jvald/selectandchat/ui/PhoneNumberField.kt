package dev.jvald.selectandchat.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.jvald.selectandchat.R
import dev.jvald.selectandchat.core.Countries
import dev.jvald.selectandchat.core.Country
import dev.jvald.selectandchat.core.NumberFeedback
import dev.jvald.selectandchat.ui.theme.AppIcons
import java.util.Locale

/**
 * One field, not two: the country lives inside it as a tappable calling-code pill, so
 * there is no second card repeating what the pill already says.
 *
 * The number itself is set several type roles larger than anything around it. It is the
 * one value the whole app exists to collect, and at body size it read like a form field
 * among other form fields.
 */
@Composable
fun PhoneNumberField(
    input: String,
    onInputChange: (String) -> Unit,
    country: Country?,
    feedback: NumberFeedback,
    onCountryClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val countryName = country?.displayName.orEmpty()
    val dialCode = country?.dialCode ?: "+?"

    TextField(
        value = input,
        onValueChange = onInputChange,
        label = { Text(stringResource(R.string.phone_number)) },
        placeholder = {
            Text(
                stringResource(R.string.number_placeholder),
                style = MaterialTheme.typography.headlineSmall,
            )
        },
        textStyle = MaterialTheme.typography.headlineSmallEmphasized,
        singleLine = true,
        isError = feedback.shouldWarn,
        supportingText = if (feedback.shouldWarn) {
            { LengthWarning(feedback, country) }
        } else {
            null
        },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
        leadingIcon = {
            CountryPill(
                flag = country?.flag,
                dialCode = dialCode,
                // One spoken label for the whole pill: "El Salvador, +503".
                label = stringResource(R.string.country_calling_code, countryName, dialCode),
                onClick = onCountryClick,
            )
        },
        trailingIcon = {
            if (input.isNotEmpty()) {
                IconButton(onClick = { onInputChange("") }) {
                    Icon(
                        AppIcons.Close,
                        contentDescription = stringResource(R.string.clear_number),
                    )
                }
            }
        },
        shape = RoundedCornerShape(20.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
            errorContainerColor = MaterialTheme.colorScheme.surfaceContainer,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            errorIndicatorColor = Color.Transparent,
        ),
        modifier = modifier.fillMaxWidth(),
    )
}

@Composable
private fun CountryPill(
    flag: String?,
    dialCode: String,
    label: String,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .padding(start = 8.dp, end = 8.dp)
            .heightIn(min = 52.dp)
            .semantics {
                role = Role.Button
                contentDescription = label
            },
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp),
        ) {
            // The flag and the code are both spoken by the pill's own label above, so
            // neither should announce itself again.
            Text(flag ?: "🏳", modifier = Modifier.clearAndSetSemantics { })
            Spacer(Modifier.width(8.dp))
            Text(
                dialCode,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.clearAndSetSemantics { },
            )
            Icon(
                AppIcons.ExpandMore,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

/**
 * A full modal sheet rather than a dropdown: there are around 240 countries, and the list
 * is only usable with a search field and rows big enough to hit.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CountrySheet(
    selectedIso: String?,
    locale: Locale,
    onSelect: (Country) -> Unit,
    onDismiss: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val results = remember(query, locale) { Countries.search(query, locale) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(Modifier.padding(horizontal = 20.dp)) {
            Text(
                stringResource(R.string.select_country),
                style = MaterialTheme.typography.headlineSmall,
            )
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text(stringResource(R.string.search_country_or_code)) },
                leadingIcon = { Icon(AppIcons.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))

            if (results.isEmpty()) {
                Text(
                    stringResource(R.string.no_countries_found),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 24.dp),
                )
            }

            // Bounded by the sheet, and no taller than its own content when the search
            // has narrowed the list to a handful of rows.
            LazyColumn(
                Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false),
            ) {
                items(results, key = { it.iso }) { country ->
                    CountryRow(
                        country = country,
                        selected = country.iso.equals(selectedIso, ignoreCase = true),
                        onClick = { onSelect(country) },
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }
    }
}

@Composable
private fun CountryRow(country: Country, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = if (selected) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            Color.Transparent
        },
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .semantics { this.selected = selected },
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.size(44.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(country.flag, style = MaterialTheme.typography.titleLarge)
                }
            }
            Column(Modifier.weight(1f)) {
                Text(
                    country.displayName,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    country.dialCode,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (selected) {
                Icon(
                    AppIcons.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}
