package com.example.musicplayer.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.example.musicplayer.data.settings.ThemeMode

private val LightColors = lightColorScheme(
    primary = Color(0xFF1DB954),
    onPrimary = Color(0xFF00210A),
    primaryContainer = Color(0xFFC8F7D9),
    onPrimaryContainer = Color(0xFF00210A),
    secondary = Color(0xFF4F6352),
    onSecondary = Color(0xFFFFFFFF),
    background = Color(0xFFF7FBF6),
    onBackground = Color(0xFF171D18),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF171D18),
    surfaceVariant = Color(0xFFDDE5DB),
    onSurfaceVariant = Color(0xFF414941),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF1ED760),
    onPrimary = Color(0xFF04130A),
    primaryContainer = Color(0xFF169C46),
    onPrimaryContainer = Color(0xFFE9FFEF),
    secondary = Color(0xFF191414),
    onSecondary = Color(0xFFEDEDED),
    background = Color(0xFF000000),
    onBackground = Color(0xFFF2F2F2),
    surface = Color(0xFF121212),
    onSurface = Color(0xFFF2F2F2),
    surfaceVariant = Color(0xFF1F1F1F),
    onSurfaceVariant = Color(0xFFB3B3B3),
    error = Color(0xFFFF7B7B),
    onError = Color(0xFF2B0000),
    errorContainer = Color(0xFF4A1C1C),
    onErrorContainer = Color(0xFFFFDAD7)
)

private val AppTypography = Typography()

@Composable
fun MusicPlayerTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    MaterialTheme(
        colorScheme = if (dark) DarkColors else LightColors,
        typography = AppTypography,
        content = content
    )
}
