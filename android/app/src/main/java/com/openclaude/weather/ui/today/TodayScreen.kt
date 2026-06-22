package com.openclaude.weather.ui.today

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Grain
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.openclaude.weather.data.local.SavedLocation
import com.openclaude.weather.domain.Forecast
import com.openclaude.weather.domain.HourPoint
import com.openclaude.weather.ui.components.DetailItem
import com.openclaude.weather.ui.components.GlassCard
import com.openclaude.weather.ui.components.SectionTitle
import com.openclaude.weather.ui.components.WeatherGlyph
import com.openclaude.weather.ui.components.WeatherSceneView
import com.openclaude.weather.util.Format
import com.openclaude.weather.util.WeatherCode

@Composable
fun TodayScreen(location: SavedLocation, forecast: Forecast) {
    val current = forecast.current
    val tzOffset = forecast.utcOffsetSeconds

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(8.dp))

        // ---- Hero card: temperature on the left, contained animation on the right. ----
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.LocationOn, contentDescription = null,
                        tint = Color.White, modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        location.displayName,
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(Modifier.weight(1f))
                    Text("now", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                }

                Spacer(Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "${Math.round(current.temperatureC)}°",
                            color = Color.White,
                            fontSize = 68.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            current.condition.label,
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            "Feels like ${Math.round(current.apparentTemperatureC)}°",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 13.sp
                        )
                    }
                    WeatherSceneView(
                        scene = current.condition.scene,
                        isDay = current.isDay,
                        modifier = Modifier.size(132.dp)
                    )
                }

                Spacer(Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Color.White.copy(alpha = 0.18f))
                )
                Spacer(Modifier.height(8.dp))

                // Quick stats
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    QuickStat(
                        Icons.Filled.Air, "Wind",
                        "${Format.wind(current.windSpeedKmh)} ${WeatherCode.windDirectionLabel(current.windDirectionDeg)}"
                    )
                    QuickStat(Icons.Filled.WaterDrop, "Humidity", Format.percent(current.humidityPct))
                    QuickStat(Icons.Filled.WbSunny, "UV", "${Math.round(current.uvIndex)}")
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // ---- "Today" divider with sunrise/sunset, like the reference. ----
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Today", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            SunTime(current.sunriseEpoch, tzOffset, rising = true)
            Spacer(Modifier.width(14.dp))
            SunTime(current.sunsetEpoch, tzOffset, rising = false)
        }

        Spacer(Modifier.height(8.dp))

        // Hourly forecast (next 24 hours).
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            val nowSec = System.currentTimeMillis() / 1000
            val upcoming = forecast.hourly.filter { it.epochSeconds >= nowSec - 3600 }.take(24)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                items(upcoming) { hour -> HourCell(hour, tzOffset) }
            }
        }

        Spacer(Modifier.height(16.dp))

        // ---- Details ----
        SectionTitle("Details")
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    DetailItem(
                        Icons.Filled.Compress, "Pressure", "${Math.round(current.pressureHpa)} hPa",
                        Modifier.weight(1f)
                    )
                    DetailItem(
                        Icons.Filled.Grain, "Precipitation", Format.mm(current.precipitationMm),
                        Modifier.weight(1f)
                    )
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    DetailItem(
                        Icons.Filled.WbTwilight, "Sunrise",
                        current.sunriseEpoch?.let { Format.hour(it, tzOffset) } ?: "—",
                        Modifier.weight(1f)
                    )
                    DetailItem(
                        Icons.Filled.WbTwilight, "Sunset",
                        current.sunsetEpoch?.let { Format.hour(it, tzOffset) } ?: "—",
                        Modifier.weight(1f)
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun QuickStat(icon: ImageVector, label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, contentDescription = label, tint = Color.White.copy(alpha = 0.9f), modifier = Modifier.size(20.dp))
        Spacer(Modifier.height(4.dp))
        Text(value, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        Text(label, color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
    }
}

@Composable
private fun SunTime(epoch: Long?, tzOffset: Long, rising: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            Icons.Filled.WbTwilight,
            contentDescription = if (rising) "Sunrise" else "Sunset",
            tint = Color(0xFFFFD54A),
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(3.dp))
        Text(
            "${if (rising) "↑" else "↓"} ${epoch?.let { Format.hour(it, tzOffset) } ?: "—"}",
            color = Color.White.copy(alpha = 0.85f),
            fontSize = 12.sp
        )
    }
}

@Composable
private fun HourCell(hour: HourPoint, tzOffset: Long) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .width(58.dp)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(Format.hour(hour.epochSeconds, tzOffset), color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp)
        WeatherGlyph(hour.condition.scene, hour.isDay, modifier = Modifier.size(32.dp))
        Text(Format.temp(hour.temperatureC), color = Color.White, fontWeight = FontWeight.SemiBold)
        Text(
            if (hour.precipitationProbabilityPct > 0) "${hour.precipitationProbabilityPct}%" else " ",
            color = Color(0xFF9FD0FF),
            fontSize = 11.sp
        )
    }
}
