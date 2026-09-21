package dev.jvald.selectandchat.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.runtime.Composable
import dev.jvald.selectandchat.core.ThemePreference

@Composable
fun SelectAndChatTheme(
    theme: ThemePreference = ThemePreference.SYSTEM,
    content: @Composable () -> Unit,
) {
    // isSystemInDarkTheme() reads the current configuration, so while the preference is
    // SYSTEM a device that flips to dark at sunset recomposes this straight away —
    // nothing is stored and nothing has to be re-read.
    val systemDark = isSystemInDarkTheme()

    val colors = when (theme) {
        ThemePreference.LIGHT -> LightColors
        ThemePreference.DARK -> GreyDarkColors
        ThemePreference.APP_DEFAULT -> AppDefaultDarkColors
        ThemePreference.AMOLED -> AmoledColors
        // Following the system means the app's own identity in the dark, not plain grey.
        ThemePreference.SYSTEM -> if (systemDark) AppDefaultDarkColors else LightColors
    }

    // material3 1.5 carries the Expressive shape scale and type roles in its defaults, so
    // only the motion scheme has to be opted into: springs with overshoot instead of
    // easing curves.
    MaterialTheme(
        colorScheme = colors,
        motionScheme = MotionScheme.expressive(),
        content = content,
    )
}
