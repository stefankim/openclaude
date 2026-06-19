package com.openclaude.weather.ui.today

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.openclaude.weather.data.local.SavedLocation
import com.openclaude.weather.domain.Forecast
import com.openclaude.weather.domain.HourPoint
import com.openclaude.weather.ui.components.DetailItem
import com.openclaude.weather.ui.components.GlassCard
import com.openclaude.weather.ui.components.SectionTitle
import com.openclaude.weather.ui.components.WeatherIcon
import com.openclaude.weather.ui.components.WeatherSceneView
import com.openclaude.weather.util.Format
import com.openclaude.weather.util.WeatherCode

@Composable
fun TodayScreen(location: SavedLocation, forecast: Forecast) {
    val current = forecast.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        // Hero: animated scene with the headline numbers overlaid.
        Box(modifier = Modifier.fillMaxWidth().height(320.dp)) {
            WeatherSceneView(
                scene = current.condition.scene,
                isDay = current.isDay,
                modifier = Modifier.fillMaxSize()
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomStart)
                    .padding(bottom = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.LocationOn,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        location.displayName,
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                Text(
                    Format.tempPrecise(current.temperatureC),
                    color = Color.White,
                    fontSize = 72.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    current.condition.label,
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    "Feels like ${Format.tempPrecise(current.apparentTemperatureC)}",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 14.sp
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // Hourly forecast (next 24 hours from now).
        SectionTitle("Hourly forecast")
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            val nowSec = System.currentTimeMillis() / 1000
            val upcoming = forecast.hourly.filter { it.epochSeconds >= nowSec - 3600 }.take(24)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(upcoming) { hour -> HourCell(hour) }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Details grid.
        SectionTitle("Details")
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    DetailItem(
                        Icons.Filled.Air,
                        "Wind",
                        "${Format.wind(current.windSpeedKmh)} ${WeatherCode.windDirectionLabel(current.windDirectionDeg)}",
                        Modifier.weight(1f)
                    )
                    DetailItem(
                        Icons.Filled.WaterDrop, "Humidity", Format.percent(current.humidityPct),
                        Modifier.weight(1f)
                    )
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    DetailItem(
                        Icons.Filled.Compress, "Pressure", "${Math.round(current.pressureHpa)} hPa",
                        Modifier.weight(1f)
                    )
                    DetailItem(
                        Icons.Filled.WbSunny, "UV index", "${Math.round(current.uvIndex)}",
                        Modifier.weight(1f)
                    )
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    DetailItem(
                        Icons.Filled.WbTwilight,
                        "Sunrise",
                        current.sunriseEpoch?.let { Format.hour(it) } ?: "—",
                        Modifier.weight(1f)
                    )
                    DetailItem(
                        Icons.Filled.WbTwilight,
                        "Sunset",
                        current.sunsetEpoch?.let { Format.hour(it) } ?: "—",
                        Modifier.weight(1f)
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun HourCell(hour: HourPoint) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .width(64.dp)
            .padding(vertical = 8.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(Format.hour(hour.epochSeconds), color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp)
        WeatherIcon(hour.condition.icon, modifier = Modifier.size(26.dp))
        Text(Format.temp(hour.temperatureC), color = Color.White, fontWeight = FontWeight.SemiBold)
        if (hour.precipitationProbabilityPct > 0) {
            Text(
                "${hour.precipitationProbabilityPct}%",
                color = Color(0xFF9FD0FF),
                fontSize = 11.sp
            )
        }
    }
}
