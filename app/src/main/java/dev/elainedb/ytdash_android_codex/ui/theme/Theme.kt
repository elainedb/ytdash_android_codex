package dev.elainedb.ytdash_android_codex.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Clay,
    secondary = Bronze,
    tertiary = Moss,
    background = Charcoal,
    surface = Color(0xFF2A2B28),
    onPrimary = Charcoal,
    onSecondary = Mist,
    onBackground = Mist,
    onSurface = Mist
)

private val LightColorScheme = lightColorScheme(
    primary = Moss,
    secondary = Bronze,
    tertiary = Clay,
    background = Sand,
    surface = Mist,
    onPrimary = Mist,
    onSecondary = Mist,
    onBackground = Charcoal,
    onSurface = Charcoal
)

@Composable
fun YTDashACodexTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = Typography,
        content = content
    )
}
