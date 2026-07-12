package com.openclaude.weather.ui.daily

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.openclaude.weather.R
import com.openclaude.weather.domain.DayPoint
import com.openclaude.weather.domain.Forecast
import com.openclaude.weather.domain.HourPoint
import com.openclaude.weather.ui.components.GlassCard
import com.openclaude.weather.ui.components.SectionTitle
import com.openclaude.weather.ui.components.WeatherGlyph
import com.openclaude.weather.util.Format

/** Week view: the next 7 days. */
@Composable
fun WeekScreen(forecast: Forecast) {
    DailyList(forecast = forecast, days = 7, title = stringResource(R.string.seven_day_forecast))
}

/** Extended view: up to 14 days with extra metrics. */
@Composable
fun ExtendedScreen(forecast: Forecast) {
    DailyList(
        forecast = forecast, days = 14,
        title = stringResource(R.string.extended_forecast), extended = true
    )
}

@Composable
private fun DailyList(forecast: Forecast, days: Int, title: String, extended: Boolean = false) {
    val list = forecast.daily.take(days)
    val globalMin = list.minOfOrNull { it.tempMinC } ?: 0.0
    val globalMax = list.maxOfOrNull { it.tempMaxC } ?: 1.0
    var expandedDay by remember { mutableStateOf<Long?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item { SectionTitle(title) }
        items(list) { day ->
            DayRow(
                day = day,
                globalMin = globalMin,
                globalMax = globalMax,
                tzOffset = forecast.utcOffsetSeconds,
                extended = extended,
                isToday = day == list.first(),
                expanded = expandedDay == day.epochSeconds,
                dayHours = if (expandedDay == day.epochSeconds) {
                    forecast.hourly.filter {
                        it.epochSeconds >= day.epochSeconds &&
                            it.epochSeconds < day.epochSeconds + 86_400
                    }
                } else emptyList(),
                onToggle = {
                    expandedDay = if (expandedDay == day.epochSeconds) null else day.epochSeconds
                }
            )
        }
        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
private fun DayRow(
    day: DayPoint,
    globalMin: Double,
    globalMax: Double,
    tzOffset: Long,
    extended: Boolean,
    isToday: Boolean,
    expanded: Boolean,
    dayHours: List<HourPoint>,
    onToggle: () -> Unit
) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.clickable { onToggle() }) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.width(64.dp)) {
                    Text(
                        if (isToday) stringResource(R.string.today) else Format.weekday(day.epochSeconds, tzOffset),
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    )
                    Text(
                        Format.dayMonth(day.epochSeconds, tzOffset),
                        color = Color.White.copy(alpha = 0.65f),
                        fontSize = 11.sp
                    )
                }
                WeatherGlyph(day.condition.scene, isDay = true, modifier = Modifier.size(34.dp))
                Spacer(Modifier.width(8.dp))
                if (day.precipitationProbabilityPct > 0) {
                    Icon(
                        Icons.Filled.WaterDrop, contentDescription = null,
                        tint = Color(0xFF9FD0FF), modifier = Modifier.size(14.dp)
                    )
                    Text(
                        "${day.precipitationProbabilityPct}%",
                        color = Color(0xFF9FD0FF), fontSize = 12.sp,
                        modifier = Modifier.padding(start = 2.dp)
                    )
                }
                Spacer(Modifier.weight(1f))
                Text(Format.temp(day.tempMinC), color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp)
                Spacer(Modifier.width(8.dp))
                TempRangeBar(
                    dayMin = day.tempMinC, dayMax = day.tempMaxC,
                    globalMin = globalMin, globalMax = globalMax,
                    modifier = Modifier.width(90.dp).height(8.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(Format.temp(day.tempMaxC), color = Color.White, fontWeight = FontWeight.SemiBold)
            }
            if (extended) {
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 64.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    MetaChip(Icons.Filled.Air, Format.wind(day.windMaxKmh))
                    MetaChip(Icons.Filled.WaterDrop, Format.mm(day.precipitationSumMm))
                    Text(
                        "UV ${Math.round(day.uvIndexMax)}",
                        color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp
                    )
                    day.sunriseEpoch?.let {
                        Text("☀ ${Format.hour(it, tzOffset)}", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                    }
                }
            }
            // Hour-by-hour strip for the tapped day.
            AnimatedVisibility(visible = expanded && dayHours.isNotEmpty()) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(top = 12.dp)
                ) {
                    items(dayHours) { hour ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                Format.hour(hour.epochSeconds, tzOffset),
                                color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp
                            )
                            WeatherGlyph(hour.condition.scene, hour.isDay, modifier = Modifier.size(26.dp))
                            Text(
                                Format.temp(hour.temperatureC),
                                color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                if (hour.precipitationProbabilityPct > 0) "${hour.precipitationProbabilityPct}%" else " ",
                                color = Color(0xFF9FD0FF), fontSize = 10.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MetaChip(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(14.dp))
        Text(text, color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp, modifier = Modifier.padding(start = 4.dp))
    }
}

/** Horizontal bar showing this day's min..max relative to the whole range, gradient-filled. */
@Composable
private fun TempRangeBar(
    dayMin: Double,
    dayMax: Double,
    globalMin: Double,
    globalMax: Double,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val span = (globalMax - globalMin).coerceAtLeast(1.0)
        val startFrac = ((dayMin - globalMin) / span).toFloat().coerceIn(0f, 1f)
        val endFrac = ((dayMax - globalMin) / span).toFloat().coerceIn(0f, 1f)
        val y = size.height / 2
        drawLine(
            color = Color.White.copy(alpha = 0.18f),
            start = Offset(0f, y), end = Offset(size.width, y),
            strokeWidth = size.height, cap = StrokeCap.Round
        )
        drawLine(
            brush = Brush.horizontalGradient(listOf(Color(0xFF5BC0EB), Color(0xFFFBBF24), Color(0xFFEF4444))),
            start = Offset(startFrac * size.width, y),
            end = Offset(endFrac * size.width, y),
            strokeWidth = size.height, cap = StrokeCap.Round
        )
    }
}
