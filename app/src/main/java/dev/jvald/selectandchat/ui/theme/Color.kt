package dev.jvald.selectandchat.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * The colour system, built the way Material 3 builds one.
 *
 * https://m3.material.io/styles/color/system/how-the-system-works
 *
 * One source colour — #68A500, the yellow-green the app is known by — is expanded into
 * tonal palettes through the *2025* colour spec: ramps of one hue where the number is the
 * tone, 0 being black and 100 white. Nothing in the app names a colour; every composable
 * asks for a *role* (`primary`, `surfaceContainer`, `onSurfaceVariant`), and the spec
 * decides which tone of which ramp each role lands on. Contrast therefore comes out of
 * tone arithmetic rather than out of someone eyeballing two hex values.
 *
 * Every value below was generated from that seed with material-color-utilities at spec
 * version 2025, variant Tonal Spot, standard contrast — the same arithmetic
 * materialkolor.com runs. They are written out rather than computed at runtime because
 * the seed never changes, and a table is something you can diff.
 *
 * The four schemes differ only in the neutral ramp their surfaces are drawn from. That is
 * the whole difference between App default, Dark and AMOLED: the accents, and every
 * contrast pairing against them, are identical across all three.
 */

// ---------------------------------------------------------------------------------------
// Accents. Shared by all three dark schemes; the light scheme takes the same palettes at
// the tones a light surface needs.
// ---------------------------------------------------------------------------------------

private val DarkPrimary = Color(0xFFB9CE9B)
private val DarkOnPrimary = Color(0xFF35451F)
private val DarkPrimaryContainer = Color(0xFF465830)
private val DarkOnPrimaryContainer = Color(0xFFD5EBB6)
private val DarkInversePrimary = Color(0xFF53653B)

private val DarkSecondary = Color(0xFFC0CBAC)
private val DarkOnSecondary = Color(0xFF3A432D)
private val DarkSecondaryContainer = Color(0xFF353F28)
private val DarkOnSecondaryContainer = Color(0xFFB8C3A6)

private val DarkTertiary = Color(0xFFFFF6DC)
private val DarkOnTertiary = Color(0xFF695D26)
private val DarkTertiaryContainer = Color(0xFFF9E8A2)
private val DarkOnTertiaryContainer = Color(0xFF60551E)

private val DarkError = Color(0xFFF97758)
private val DarkOnError = Color(0xFF450900)
private val DarkErrorContainer = Color(0xFF85230A)
private val DarkOnErrorContainer = Color(0xFFFF9B82)

// ---------------------------------------------------------------------------------------
// Neutrals. One of these per dark scheme, and the only thing that varies between them.
// ---------------------------------------------------------------------------------------

/**
 * The surface half of a dark scheme: the page, the five container elevations stacked on
 * top of it, and the outlines and text that have to stay legible against all six.
 */
private class DarkSurfaces(
    val surface: Color,
    val surfaceDim: Color,
    val surfaceBright: Color,
    val containerLowest: Color,
    val containerLow: Color,
    val container: Color,
    val containerHigh: Color,
    val containerHighest: Color,
    val onSurface: Color,
    val inverseOnSurface: Color,
    val surfaceVariant: Color,
    val onSurfaceVariant: Color,
    val outline: Color,
    val outlineVariant: Color,
)

/**
 * Green-tinted neutrals — the app's own identity in the dark, and what [SYSTEM] resolves
 * to. The hue is the seed's, held at chroma 12: enough for the page to read as green
 * rather than as grey that happens to sit under a green button.
 */
private val AppDefaultSurfaces = DarkSurfaces(
    surface = Color(0xFF1B2213),
    surfaceDim = Color(0xFF171E0F),
    surfaceBright = Color(0xFF39402F),
    containerLowest = Color(0xFF141A0C),
    containerLow = Color(0xFF1F2617),
    container = Color(0xFF242A1B),
    containerHigh = Color(0xFF282E1F),
    containerHighest = Color(0xFF303727),
    onSurface = Color(0xFFDEE5CE),
    inverseOnSurface = Color(0xFF2C3323),
    surfaceVariant = Color(0xFF2E3227),
    onSurfaceVariant = Color(0xFFA9AD9E),
    outline = Color(0xFF75796B),
    outlineVariant = Color(0xFF44483D),
)

/**
 * The same ramp with the tint taken out, for people who want plain dark grey.
 *
 * It sits at tone 12 rather than down near the bottom of the ramp: a dark grey that is
 * visibly grey, not a near-black that is indistinguishable from [AmoledSurfaces] on an
 * OLED panel. The whole point of offering both is that they look different.
 */
private val GreySurfaces = DarkSurfaces(
    surface = Color(0xFF1F1F1F),
    surfaceDim = Color(0xFF1B1B1B),
    surfaceBright = Color(0xFF474747),
    containerLowest = Color(0xFF181818),
    containerLow = Color(0xFF242424),
    container = Color(0xFF282828),
    containerHigh = Color(0xFF303030),
    containerHighest = Color(0xFF393939),
    onSurface = Color(0xFFE2E2E2),
    inverseOnSurface = Color(0xFF303030),
    surfaceVariant = Color(0xFF303030),
    onSurfaceVariant = Color(0xFFABABAB),
    outline = Color(0xFF777777),
    outlineVariant = Color(0xFF474747),
)

/**
 * The page pulled to #000000 so OLED pixels switch off entirely.
 *
 * Only the roles a dark scheme paints the page with go to black; the containers keep
 * enough separation from each other that an elevated card is still visible, which a scheme
 * that took everything to black would lose. They are drawn from a *more* saturated green
 * ramp than [AppDefaultSurfaces] — against pure black a low-chroma tint reads as grey, so
 * it takes more chroma to look like the same theme.
 */
private val AmoledSurfaces = DarkSurfaces(
    surface = Color(0xFF000000),
    surfaceDim = Color(0xFF000000),
    surfaceBright = Color(0xFF313D21),
    containerLowest = Color(0xFF000000),
    containerLow = Color(0xFF0C1602),
    container = Color(0xFF141F06),
    containerHigh = Color(0xFF1C270D),
    containerHighest = Color(0xFF243015),
    onSurface = Color(0xFFD9E8C0),
    inverseOnSurface = Color(0xFF283419),
    surfaceVariant = Color(0xFF2E3227),
    onSurfaceVariant = Color(0xFFA9AD9E),
    outline = Color(0xFF75796B),
    outlineVariant = Color(0xFF44483D),
)

/**
 * Role assignment for a dark scheme, per the Material 3 mapping. The only argument is the
 * neutral ramp, which is what the three dark options vary.
 */
private fun darkSchemeOf(surfaces: DarkSurfaces): ColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = DarkOnPrimaryContainer,
    inversePrimary = DarkInversePrimary,
    secondary = DarkSecondary,
    onSecondary = DarkOnSecondary,
    secondaryContainer = DarkSecondaryContainer,
    onSecondaryContainer = DarkOnSecondaryContainer,
    tertiary = DarkTertiary,
    onTertiary = DarkOnTertiary,
    tertiaryContainer = DarkTertiaryContainer,
    onTertiaryContainer = DarkOnTertiaryContainer,
    error = DarkError,
    onError = DarkOnError,
    errorContainer = DarkErrorContainer,
    onErrorContainer = DarkOnErrorContainer,
    background = surfaces.surface,
    onBackground = surfaces.onSurface,
    surface = surfaces.surface,
    onSurface = surfaces.onSurface,
    surfaceDim = surfaces.surfaceDim,
    surfaceBright = surfaces.surfaceBright,
    surfaceVariant = surfaces.surfaceVariant,
    onSurfaceVariant = surfaces.onSurfaceVariant,
    surfaceContainerLowest = surfaces.containerLowest,
    surfaceContainerLow = surfaces.containerLow,
    surfaceContainer = surfaces.container,
    surfaceContainerHigh = surfaces.containerHigh,
    surfaceContainerHighest = surfaces.containerHighest,
    inverseSurface = surfaces.onSurface,
    inverseOnSurface = surfaces.inverseOnSurface,
    outline = surfaces.outline,
    outlineVariant = surfaces.outlineVariant,
    scrim = Color(0xFF000000),
)

val AppDefaultDarkColors: ColorScheme = darkSchemeOf(AppDefaultSurfaces)
val GreyDarkColors: ColorScheme = darkSchemeOf(GreySurfaces)
val AmoledColors: ColorScheme = darkSchemeOf(AmoledSurfaces)

/**
 * The light scheme, straight out of the 2025 spec.
 *
 * Its neutrals carry the seed's hue too, so the page is a warm off-white rather than #FFF
 * and the containers step through it in green. A light theme built on pure grey would
 * throw away the one thing the source colour is there to do.
 */
val LightColors: ColorScheme = lightColorScheme(
    primary = Color(0xFF4F6632),
    onPrimary = Color(0xFFEFFFD4),
    primaryContainer = Color(0xFFD0ECAA),
    onPrimaryContainer = Color(0xFF425826),
    inversePrimary = Color(0xFFE1FDBA),
    secondary = Color(0xFF58634A),
    onSecondary = Color(0xFFF2FDDD),
    secondaryContainer = Color(0xFFDCE7C7),
    onSecondaryContainer = Color(0xFF4B553D),
    tertiary = Color(0xFF6A5F27),
    onTertiary = Color(0xFFFFF8EA),
    tertiaryContainer = Color(0xFFF9E8A2),
    onTertiaryContainer = Color(0xFF60551E),
    error = Color(0xFFA73B21),
    onError = Color(0xFFFFF7F6),
    errorContainer = Color(0xFFFD795A),
    onErrorContainer = Color(0xFF6E1400),
    background = Color(0xFFFAFAF0),
    onBackground = Color(0xFF303429),
    surface = Color(0xFFFAFAF0),
    onSurface = Color(0xFF303429),
    surfaceDim = Color(0xFFD8DCCC),
    surfaceBright = Color(0xFFFAFAF0),
    surfaceVariant = Color(0xFFE1E4D4),
    onSurfaceVariant = Color(0xFF5C6154),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF3F5E9),
    surfaceContainer = Color(0xFFEDEFE2),
    surfaceContainerHigh = Color(0xFFE7EADB),
    surfaceContainerHighest = Color(0xFFE1E4D4),
    inverseSurface = Color(0xFF0D0F0A),
    inverseOnSurface = Color(0xFF9D9E95),
    outline = Color(0xFF787C6F),
    outlineVariant = Color(0xFFB0B4A5),
    scrim = Color(0xFF000000),
)
