package dev.elainedb.ytdash_android_codex.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = SlateBlue,
    secondary = Accent,
    tertiary = Success,
    background = DeepNavy,
    surface = ColorTokens.darkSurface,
    onPrimary = Mist,
    onSecondary = Mist,
    onTertiary = Mist,
    onBackground = Mist,
    onSurface = Mist
)

private val LightColorScheme = lightColorScheme(
    primary = SlateBlue,
    secondary = Accent,
    tertiary = Success,
    background = Mist,
    surface = ColorTokens.lightSurface,
    onPrimary = Mist,
    onSecondary = Mist,
    onTertiary = Mist,
    onBackground = Ink,
    onSurface = Ink
)

private object ColorTokens {
    val lightSurface = Cloud
    val darkSurface = Color(0xFF162948)
}

@Composable
fun YTDashACodexTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
