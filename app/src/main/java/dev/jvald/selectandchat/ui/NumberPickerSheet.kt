package dev.jvald.selectandchat.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.jvald.selectandchat.R
import dev.jvald.selectandchat.core.Extraction
import dev.jvald.selectandchat.core.PhoneCandidate

/**
 * Shown only when the selection did not resolve to exactly one number: several matches,
 * none at all, or a local number with no country set yet. The single-match case never
 * reaches this — it opens WhatsApp with no UI.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NumberPickerSheet(
    extraction: Extraction,
    region: String?,
    onRegionChange: (String) -> Unit,
    onPick: (PhoneCandidate) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            Modifier.padding(start = 24.dp, end = 24.dp, bottom = 32.dp),
        ) {
            if (extraction.candidates.isNotEmpty()) {
                Text(
                    stringResource(R.string.pick_a_number),
                    style = MaterialTheme.typography.titleLarge,
                )
                Spacer(Modifier.height(12.dp))
                extraction.candidates.forEach { candidate ->
                    CandidateRow(candidate) { onPick(candidate) }
                }
                Spacer(Modifier.height(20.dp))
                HorizontalDivider()
                Spacer(Modifier.height(20.dp))
                Text(
                    stringResource(R.string.or_type_one),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Text(
                    stringResource(R.string.no_number_found),
                    style = MaterialTheme.typography.titleLarge,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    stringResource(
                        if (extraction.needsRegion) {
                            R.string.needs_region_explainer
                        } else {
                            R.string.no_number_explainer
                        },
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(Modifier.height(16.dp))
            ManualEntry(
                initialNumber = extraction.digitsHint,
                region = region,
                onRegionChange = onRegionChange,
                onSubmit = onPick,
            )
        }
    }
}
