package dev.jvald.selectandchat.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// Green primaries are shared by every non-dynamic scheme; only the neutrals change.
private val GreenPrimaryDark = Color(0xFF72DA9F)
private val GreenOnPrimaryDark = Color(0xFF00391F)
private val GreenPrimaryContainerDark = Color(0xFF005230)
private val GreenOnPrimaryContainerDark = Color(0xFF8FF7BC)

val LightColors = lightColorScheme(
    primary = Color(0xFF006D42),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF8FF7BC),
    onPrimaryContainer = Color(0xFF00210F),
    secondary = Color(0xFF4E6355),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD1E8D6),
    onSecondaryContainer = Color(0xFF0C1F14),
    tertiary = Color(0xFF3B6470),
    onTertiary = Color(0xFFFFFFFF),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    background = Color(0xFFF6FBF5),
    onBackground = Color(0xFF181D19),
    surface = Color(0xFFF6FBF5),
    onSurface = Color(0xFF181D19),
    surfaceVariant = Color(0xFFDCE5DC),
    onSurfaceVariant = Color(0xFF414942),
    surfaceContainer = Color(0xFFEAEFE9),
    surfaceContainerHigh = Color(0xFFE4EAE3),
    outline = Color(0xFF717971),
    outlineVariant = Color(0xFFC0C9C0),
)

/** The green-tinted dark the mockups were drawn in. */
val GreenDarkColors = darkColorScheme(
    primary = GreenPrimaryDark,
    onPrimary = GreenOnPrimaryDark,
    primaryContainer = GreenPrimaryContainerDark,
    onPrimaryContainer = GreenOnPrimaryContainerDark,
    secondary = Color(0xFFB5CCBA),
    onSecondary = Color(0xFF213528),
    secondaryContainer = Color(0xFF374B3E),
    onSecondaryContainer = Color(0xFFD1E8D6),
    tertiary = Color(0xFFA3CEDC),
    onTertiary = Color(0xFF033541),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    background = Color(0xFF101511),
    onBackground = Color(0xFFDFE4DE),
    surface = Color(0xFF101511),
    onSurface = Color(0xFFDFE4DE),
    surfaceVariant = Color(0xFF414942),
    onSurfaceVariant = Color(0xFFC0C9C0),
    surfaceContainer = Color(0xFF1C211D),
    surfaceContainerHigh = Color(0xFF262B27),
    outline = Color(0xFF8B938B),
    outlineVariant = Color(0xFF414942),
)

/** Neutral greys, green only on the accents. */
val NeutralDarkColors = GreenDarkColors.copy(
    secondaryContainer = Color(0xFF32362F),
    onSecondaryContainer = Color(0xFFE2E3DE),
    background = Color(0xFF121212),
    onBackground = Color(0xFFE3E3E3),
    surface = Color(0xFF121212),
    onSurface = Color(0xFFE3E3E3),
    surfaceVariant = Color(0xFF3F3F3F),
    onSurfaceVariant = Color(0xFFC6C6C6),
    surfaceContainer = Color(0xFF1E1E1E),
    surfaceContainerHigh = Color(0xFF282828),
    outline = Color(0xFF8F8F8F),
    outlineVariant = Color(0xFF3F3F3F),
)

/** True black so OLED pixels switch off entirely. */
val AmoledColors = NeutralDarkColors.copy(
    background = Color(0xFF000000),
    surface = Color(0xFF000000),
    surfaceContainer = Color(0xFF0A0A0A),
    surfaceContainerHigh = Color(0xFF161616),
    secondaryContainer = Color(0xFF1A1E1B),
    outlineVariant = Color(0xFF2A2A2A),
)
