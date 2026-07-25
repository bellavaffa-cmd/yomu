package com.yomu.reader.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Accent = Color(0xFF5B8DEF)
private val AccentDark = Color(0xFF4A76D4)

private val DarkColors = darkColorScheme(
    primary = Accent,
    onPrimary = Color.White,
    secondary = Color(0xFF9CB4E0),
    background = Color(0xFF0F1115),
    onBackground = Color(0xFFE6E8EC),
    surface = Color(0xFF171A21),
    onSurface = Color(0xFFE6E8EC),
    surfaceVariant = Color(0xFF232733),
    onSurfaceVariant = Color(0xFFAEB4C0),
    outline = Color(0xFF333846),
)

private val LightColors = lightColorScheme(
    primary = AccentDark,
    onPrimary = Color.White,
    secondary = Color(0xFF3D5A99),
    background = Color(0xFFF7F8FA),
    onBackground = Color(0xFF1A1C1E),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1A1C1E),
    surfaceVariant = Color(0xFFE6E9F0),
    onSurfaceVariant = Color(0xFF44474E),
    outline = Color(0xFFC4C7CF),
)

@Composable
fun YomuTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
