package com.railsathi.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Railway blue + saffron accent. A fixed palette (no dynamic colour) keeps the brand consistent.
private val Blue700 = Color(0xFF1A4D8F)
private val Blue100 = Color(0xFFD6E3FF)
private val Blue900 = Color(0xFF001B3E)
private val Saffron600 = Color(0xFFB85F00)
private val Saffron100 = Color(0xFFFFDCBF)
private val Saffron900 = Color(0xFF2E1500)
private val Green700 = Color(0xFF1B6B3A)
private val Green100 = Color(0xFFB8F2C8)
private val Red700 = Color(0xFFBA1A1A)
private val Red100 = Color(0xFFFFDAD6)

private val LightColors = lightColorScheme(
    primary = Blue700,
    onPrimary = Color.White,
    primaryContainer = Blue100,
    onPrimaryContainer = Blue900,
    secondary = Saffron600,
    onSecondary = Color.White,
    secondaryContainer = Saffron100,
    onSecondaryContainer = Saffron900,
    tertiary = Green700,
    onTertiary = Color.White,
    tertiaryContainer = Green100,
    onTertiaryContainer = Color(0xFF00210E),
    error = Red700,
    onError = Color.White,
    errorContainer = Red100,
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFFDFBFF),
    onBackground = Color(0xFF1A1C1E),
    surface = Color(0xFFFDFBFF),
    onSurface = Color(0xFF1A1C1E),
    surfaceVariant = Color(0xFFE0E2EC),
    onSurfaceVariant = Color(0xFF44474E),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFAAC7FF),
    onPrimary = Color(0xFF002F65),
    primaryContainer = Color(0xFF00458D),
    onPrimaryContainer = Blue100,
    secondary = Color(0xFFFFB878),
    onSecondary = Color(0xFF4A2800),
    secondaryContainer = Color(0xFF6A3B00),
    onSecondaryContainer = Saffron100,
    tertiary = Color(0xFF9DD5AE),
    onTertiary = Color(0xFF003919),
    tertiaryContainer = Color(0xFF00522A),
    onTertiaryContainer = Green100,
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Red100,
    background = Color(0xFF1A1C1E),
    onBackground = Color(0xFFE3E2E6),
    surface = Color(0xFF1A1C1E),
    onSurface = Color(0xFFE3E2E6),
    surfaceVariant = Color(0xFF44474E),
    onSurfaceVariant = Color(0xFFC4C6D0),
)

@Composable
fun RailSathiTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
