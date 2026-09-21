package dev.jvald.selectandchat.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.jvald.selectandchat.R
import dev.jvald.selectandchat.core.WhatsAppFlavor
import dev.jvald.selectandchat.ui.theme.AppIcons

private val OuterCorner = 28.dp
private val InnerCorner = 8.dp

/**
 * The primary action, and the choice of which WhatsApp it opens.
 *
 * Two independent buttons rather than one button with a menu: the wide half does the
 * thing, the narrow half changes what the thing is, and each has its own touch target and
 * its own place in the focus order. The shapes are built here rather than taken from
 * `SplitButtonDefaults` because that API's shape types do not line up with what `Button`
 * accepts in material3 1.5.0-alpha18 — the geometry is the same: round on the outside,
 * tight on the seam.
 */
@Composable
fun WhatsAppSplitButton(
    flavor: WhatsAppFlavor,
    enabled: Boolean,
    installedFlavors: Set<WhatsAppFlavor>,
    onOpen: () -> Unit,
    onFlavorChange: (WhatsAppFlavor) -> Unit,
    modifier: Modifier = Modifier,
) {
    var menuOpen by remember { mutableStateOf(false) }
    val chevronRotation by animateFloatAsState(
        targetValue = if (menuOpen) 180f else 0f,
        label = "chevron",
    )

    Row(
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Button(
            onClick = onOpen,
            enabled = enabled,
            shape = RoundedCornerShape(
                topStart = OuterCorner,
                bottomStart = OuterCorner,
                topEnd = InnerCorner,
                bottomEnd = InnerCorner,
            ),
            modifier = Modifier
                .weight(1f)
                .height(60.dp),
        ) {
            Text(
                stringResource(flavor.openActionRes()),
                style = MaterialTheme.typography.titleMediumEmphasized,
            )
            Spacer(Modifier.width(10.dp))
            Icon(
                AppIcons.OpenInNew,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
            )
        }

        Box {
            Button(
                onClick = { menuOpen = true },
                enabled = enabled,
                contentPadding = PaddingValues(0.dp),
                shape = RoundedCornerShape(
                    topStart = InnerCorner,
                    bottomStart = InnerCorner,
                    topEnd = OuterCorner,
                    bottomEnd = OuterCorner,
                ),
                modifier = Modifier
                    .width(72.dp)
                    .height(60.dp)
                    .semantics {
                        role = Role.DropdownList
                    },
            ) {
                Icon(
                    AppIcons.ExpandMore,
                    contentDescription = stringResource(R.string.choose_whatsapp_app),
                    modifier = Modifier.rotate(chevronRotation),
                )
            }

            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                WhatsAppFlavor.entries.forEach { option ->
                    FlavorRow(
                        flavor = option,
                        selected = option == flavor,
                        installed = option in installedFlavors,
                        onClick = {
                            menuOpen = false
                            onFlavorChange(option)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun FlavorRow(
    flavor: WhatsAppFlavor,
    selected: Boolean,
    installed: Boolean,
    onClick: () -> Unit,
) {
    val name = stringResource(flavor.labelRes())
    // A flavor that is not installed is still offered, because it can be installed later
    // and the preference is the user's; saying so on the row is what stops the primary
    // button from looking broken when it is pressed.
    val label = if (installed) name else "$name ${stringResource(R.string.not_installed_suffix)}"

    DropdownMenuItem(
        text = { Text(label) },
        leadingIcon = {
            if (selected) {
                Icon(
                    AppIcons.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
            } else {
                Spacer(Modifier.width(24.dp))
            }
        },
        onClick = onClick,
        modifier = Modifier
            .defaultMinSize(minHeight = 48.dp)
            .semantics {
                role = Role.RadioButton
                this.selected = selected
                contentDescription = label
            },
    )
}

internal fun WhatsAppFlavor.labelRes() = when (this) {
    WhatsAppFlavor.STANDARD -> R.string.whatsapp_standard
    WhatsAppFlavor.BUSINESS -> R.string.whatsapp_business
}

internal fun WhatsAppFlavor.openActionRes() = when (this) {
    WhatsAppFlavor.STANDARD -> R.string.open_in_whatsapp
    WhatsAppFlavor.BUSINESS -> R.string.open_in_whatsapp_business
}
