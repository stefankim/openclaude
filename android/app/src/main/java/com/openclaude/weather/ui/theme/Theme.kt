package com.openclaude.weather.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColors = darkColorScheme(
    primary = Sky500,
    secondary = Indigo600,
    tertiary = Amber400,
    background = Slate900,
    surface = Slate800,
    onPrimary = Color.White,
    onBackground = Slate100,
    onSurface = Slate100
)

private val LightColors = lightColorScheme(
    primary = Sky700,
    secondary = Indigo600,
    tertiary = Amber400,
    background = Slate100,
    surface = Color.White,
    onPrimary = Color.White,
    onBackground = Slate900,
    onSurface = Slate900
)

@Composable
fun WeatherTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = Typography(),
        content = content
    )
}
