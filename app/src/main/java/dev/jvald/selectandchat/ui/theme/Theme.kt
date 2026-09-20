package dev.jvald.selectandchat.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import dev.jvald.selectandchat.core.AppTheme

@Composable
fun SelectAndChatTheme(
    theme: AppTheme = AppTheme.SYSTEM,
    content: @Composable () -> Unit,
) {
    val systemDark = isSystemInDarkTheme()
    val context = LocalContext.current

    val colors = when (theme) {
        AppTheme.LIGHT -> LightColors
        AppTheme.DARK -> NeutralDarkColors
        AppTheme.AMOLED -> AmoledColors
        AppTheme.GREEN -> GreenDarkColors
        AppTheme.SYSTEM -> if (systemDark) GreenDarkColors else LightColors
        AppTheme.DYNAMIC ->
            // Dynamic colour needs Android 12; fall back rather than showing a dead option.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (systemDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            } else {
                if (systemDark) GreenDarkColors else LightColors
            }
    }
    MaterialTheme(
        colorScheme = colors,
        shapes = ExpressiveShapes,
        typography = ExpressiveTypography,
        content = content,
    )
}
