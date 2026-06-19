package com.openclaude.weather.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Foggy
import androidx.compose.material.icons.filled.Grain
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Thunderstorm
import androidx.compose.material.icons.filled.WbCloudy
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

/** Maps the condition icon name to a Material symbol. */
@Composable
fun WeatherIcon(name: String, modifier: Modifier = Modifier, tint: Color = Color.White) {
    val vector: ImageVector = when (name) {
        "sunny" -> Icons.Filled.WbSunny
        "clear_night" -> Icons.Filled.NightsStay
        "partly_cloudy" -> Icons.Filled.WbCloudy
        "cloudy" -> Icons.Filled.Cloud
        "foggy" -> Icons.Filled.Foggy
        "rainy" -> Icons.Filled.Grain
        "snowy" -> Icons.Filled.AcUnit
        "thunderstorm" -> Icons.Filled.Thunderstorm
        else -> Icons.Filled.Cloud
    }
    Icon(imageVector = vector, contentDescription = name, modifier = modifier, tint = tint)
}
