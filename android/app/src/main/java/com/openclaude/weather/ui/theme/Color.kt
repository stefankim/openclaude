package com.openclaude.weather.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.openclaude.weather.domain.WeatherScene

// Brand palette
val Sky500 = Color(0xFF0EA5E9)
val Sky700 = Color(0xFF0369A1)
val Indigo600 = Color(0xFF4F46E5)
val Slate900 = Color(0xFF0F172A)
val Slate800 = Color(0xFF1E293B)
val Slate100 = Color(0xFFF1F5F9)
val Amber400 = Color(0xFFFBBF24)
val CardDark = Color(0x33FFFFFF)
val CardLight = Color(0x22FFFFFF)

/** A full-screen gradient that matches the current weather scene + time of day. */
fun sceneGradient(scene: WeatherScene, isDay: Boolean): Brush {
    val colors = when (scene) {
        WeatherScene.CLEAR_DAY -> listOf(Color(0xFF2A93D5), Color(0xFF5BC0EB), Color(0xFFA8DEF0))
        WeatherScene.CLEAR_NIGHT -> listOf(Color(0xFF0B1026), Color(0xFF1B2A4A), Color(0xFF2C3E6B))
        WeatherScene.PARTLY_CLOUDY ->
            if (isDay) listOf(Color(0xFF4A8FC0), Color(0xFF7FB2D6), Color(0xFFB9D4E6))
            else listOf(Color(0xFF15203A), Color(0xFF243352), Color(0xFF394B6E))
        WeatherScene.CLOUDY ->
            if (isDay) listOf(Color(0xFF5D6D7E), Color(0xFF7F8C99), Color(0xFFAEB9C4))
            else listOf(Color(0xFF1E2530), Color(0xFF2C3540), Color(0xFF3C4754))
        WeatherScene.FOG -> listOf(Color(0xFF6B7585), Color(0xFF919BA8), Color(0xFFBFC7D1))
        WeatherScene.RAIN ->
            if (isDay) listOf(Color(0xFF3A4A5C), Color(0xFF4F6478), Color(0xFF6E8295))
            else listOf(Color(0xFF12161F), Color(0xFF1E2733), Color(0xFF2C3A49))
        WeatherScene.SNOW ->
            if (isDay) listOf(Color(0xFF6E7E92), Color(0xFF9AAAC0), Color(0xFFD6E0EC))
            else listOf(Color(0xFF1A2230), Color(0xFF2A3447), Color(0xFF42506A))
        WeatherScene.THUNDERSTORM -> listOf(Color(0xFF14161F), Color(0xFF272A3A), Color(0xFF3A3F57))
    }
    return Brush.verticalGradient(colors)
}
