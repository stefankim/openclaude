package com.openclaude.weather.ui.radar

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.openclaude.weather.data.local.SavedLocation
import com.openclaude.weather.util.Format
import kotlinx.coroutines.delay
import org.osmdroid.tileprovider.MapTileProviderBasic
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.util.MapTileIndex
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.TilesOverlay

/** RainViewer tile source: formats a "{z}/{x}/{y}" template into the precipitation tile URL. */
private class RainViewerTileSource(name: String, private val template: String) :
    OnlineTileSourceBase(name, 0, 12, 256, ".png", arrayOf("")) {
    override fun getTileURLString(pMapTileIndex: Long): String {
        val zoom = MapTileIndex.getZoom(pMapTileIndex)
        val x = MapTileIndex.getX(pMapTileIndex)
        val y = MapTileIndex.getY(pMapTileIndex)
        return String.format(template, zoom, x, y)
    }
}

private enum class RadarMode(val label: String) { LIVE("Live"), FORECAST("Forecast"), WINDY("Windy") }

/** Coarser relative label for forecast hours, e.g. "now", "in 5 h", "in 2 days". */
private fun relativeDayLabel(epochSeconds: Long): String {
    val deltaH = ((epochSeconds - System.currentTimeMillis() / 1000) / 3600.0)
    val h = Math.round(deltaH).toInt()
    return when {
        h <= 0 -> "now"
        h < 24 -> "in $h h"
        else -> "in ${h / 24} day${if (h / 24 == 1) "" else "s"}"
    }
}

/** Human label for a radar frame relative to now, e.g. "now", "−40 min", "in 20 min". */
private fun relativeLabel(epochSeconds: Long): String {
    val deltaMin = ((epochSeconds - System.currentTimeMillis() / 1000) / 60.0).let {
        if (it < 0) Math.ceil(it).toInt() else Math.floor(it).toInt()
    }
    return when {
        deltaMin in -2..2 -> "now"
        deltaMin < 0 -> "−${-deltaMin} min"
        else -> "in $deltaMin min"
    }
}

@Composable
fun RadarScreen(
    state: RadarUiState,
    forecast: PrecipForecastState,
    location: SavedLocation?,
    onRetry: () -> Unit,
    onLoadForecast: (Double, Double) -> Unit
) {
    val context = LocalContext.current

    // osmdroid User-Agent is configured once in WeatherApp.onCreate(), before any MapView
    // is built, so the OpenStreetMap base tiles load correctly here.

    var frameIndex by remember { mutableStateOf(0) }
    var playing by remember { mutableStateOf(true) }
    var mode by remember { mutableStateOf(RadarMode.LIVE) }
    var forecastHour by remember { mutableStateOf(0) }

    // Load the native forecast grid when entering Forecast mode or changing location.
    LaunchedEffect(mode, location?.id) {
        if (mode == RadarMode.FORECAST && location != null) {
            onLoadForecast(location.latitude, location.longitude)
        }
    }

    // Default the forecast slider to the current hour once the grid arrives.
    LaunchedEffect(forecast.grid) {
        val times = forecast.grid?.times ?: return@LaunchedEffect
        val now = System.currentTimeMillis() / 1000
        forecastHour = times.indexOfFirst { it >= now }.coerceAtLeast(0)
    }

    // Default to the last "past" frame (closest to now) once frames arrive.
    LaunchedEffect(state.frames.size) {
        if (state.frames.isNotEmpty()) {
            frameIndex = state.frames.indexOfLast { !it.isForecast }.coerceAtLeast(0)
        }
    }

    // Animation loop (Live mode only).
    LaunchedEffect(playing, mode, state.frames.size) {
        if (mode != RadarMode.LIVE || state.frames.isEmpty()) return@LaunchedEffect
        while (playing) {
            delay(700)
            frameIndex = (frameIndex + 1) % state.frames.size
        }
    }

    val mapView = remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(7.5)
            isHorizontalMapRepetitionEnabled = false
            isTilesScaledToDpi = true
        }
    }
    var radarOverlay by remember { mutableStateOf<TilesOverlay?>(null) }
    val precipOverlay = remember { PrecipForecastOverlay() }
    val locationMarker = remember {
        Marker(mapView).apply {
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        }
    }

    DisposableEffect(Unit) {
        onDispose { mapView.onDetach() }
    }

    // Recenter and drop a pin when the chosen location changes.
    LaunchedEffect(location?.id) {
        location?.let {
            val point = GeoPoint(it.latitude, it.longitude)
            mapView.controller.setCenter(point)
            locationMarker.position = point
            locationMarker.title = it.displayName
            if (!mapView.overlays.contains(locationMarker)) mapView.overlays.add(locationMarker)
            mapView.invalidate()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (mode == RadarMode.WINDY) {
            // Multi-day forecast radar for Slovakia (Windy: blends radar + forecast model).
            WindyRadarView(location = location, modifier = Modifier.fillMaxSize())
        } else {
            AndroidView(
                factory = { mapView },
                modifier = Modifier.fillMaxSize(),
                update = { mv ->
                    when (mode) {
                        RadarMode.LIVE -> {
                            // Observed RainViewer frames; remove the forecast overlay if present.
                            mv.overlays.remove(precipOverlay)
                            val frame = state.frames.getOrNull(frameIndex) ?: return@AndroidView
                            radarOverlay?.let { mv.overlays.remove(it) }
                            val source = RainViewerTileSource("rainviewer-${frame.time}", frame.tileUrlTemplate)
                            val provider = MapTileProviderBasic(mv.context, source)
                            val overlay = TilesOverlay(provider, mv.context).apply {
                                loadingBackgroundColor = AndroidColor.TRANSPARENT
                                loadingLineColor = AndroidColor.TRANSPARENT
                            }
                            mv.overlays.add(0, overlay)
                            radarOverlay = overlay
                        }
                        RadarMode.FORECAST -> {
                            // Native Open-Meteo precipitation cells; remove the RainViewer layer.
                            radarOverlay?.let { mv.overlays.remove(it); radarOverlay = null }
                            precipOverlay.grid = forecast.grid
                            precipOverlay.hourIndex = forecastHour
                            if (!mv.overlays.contains(precipOverlay)) mv.overlays.add(0, precipOverlay)
                        }
                        else -> Unit
                    }
                    mv.invalidate()
                }
            )
        }

        // Title chip.
        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xAA0F172A))
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text(
                text = "Storm & rain radar",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Text(
                text = when (mode) {
                    RadarMode.LIVE -> "Live · last 2 h + nowcast"
                    RadarMode.FORECAST -> "Forecast · next 3 days (Open-Meteo)"
                    RadarMode.WINDY -> "Forecast · Windy"
                },
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 11.sp
            )
        }

        // Live / Forecast toggle.
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xAA0F172A))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            RadarMode.entries.forEach { m ->
                val selected = m == mode
                Text(
                    text = m.label,
                    color = if (selected) Color(0xFF0F172A) else Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (selected) Color.White else Color.Transparent)
                        .clickable { mode = m }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }

        if (mode == RadarMode.LIVE) {
            when {
                state.loading -> CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = Color.White
                )
                state.error != null -> Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(state.error, color = Color.White)
                    Text(
                        "Tap to retry",
                        color = Color(0xFF9FD0FF),
                        modifier = Modifier.padding(8.dp).clip(RoundedCornerShape(8.dp)).clickable { onRetry() }
                    )
                }
            }
        }

        if (mode == RadarMode.FORECAST) {
            when {
                forecast.loading -> CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = Color.White
                )
                forecast.error != null -> Text(
                    forecast.error,
                    color = Color.White,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .clickable { location?.let { onLoadForecast(it.latitude, it.longitude) } }
                )
            }

            val grid = forecast.grid
            if (grid != null && grid.times.isNotEmpty()) {
                val time = grid.times.getOrNull(forecastHour)
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(16.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xCC0F172A))
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Text(
                        time?.let { Format.localDayTime(it) } ?: "",
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        time?.let { relativeDayLabel(it) } ?: "",
                        color = Color(0xFF9FD0FF),
                        fontSize = 12.sp
                    )
                    Slider(
                        value = forecastHour.toFloat(),
                        onValueChange = { forecastHour = it.toInt().coerceIn(0, grid.times.lastIndex) },
                        valueRange = 0f..(grid.times.lastIndex.coerceAtLeast(1).toFloat()),
                        modifier = Modifier.fillMaxWidth()
                    )
                    PrecipLegend()
                }
            }
        }

        // Playback controls (Live mode only — Windy has its own timeline).
        if (mode == RadarMode.LIVE && state.frames.isNotEmpty()) {
            val frame = state.frames.getOrNull(frameIndex)
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(16.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xCC0F172A))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { playing = !playing }) {
                        Icon(
                            if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = if (playing) "Pause" else "Play",
                            tint = Color.White
                        )
                    }
                    Column(modifier = Modifier.width(120.dp)) {
                        Text(
                            frame?.let { Format.clock(it.time * 1000) } ?: "",
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            frame?.let { relativeLabel(it.time) } ?: "",
                            color = if (frame?.isForecast == true) Color(0xFFFBBF24) else Color(0xFF9FD0FF),
                            fontSize = 12.sp
                        )
                    }
                    Slider(
                        value = frameIndex.toFloat(),
                        onValueChange = {
                            playing = false
                            frameIndex = it.toInt().coerceIn(0, state.frames.lastIndex)
                        },
                        valueRange = 0f..(state.frames.lastIndex.coerceAtLeast(1).toFloat()),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

/** Compact precipitation colour-ramp legend for the native forecast radar. */
@Composable
private fun PrecipLegend() {
    val steps = listOf(
        0.3 to "light",
        1.5 to "moderate",
        3.5 to "heavy",
        7.0 to "v. heavy",
        12.0 to "extreme"
    )
    Row(
        modifier = Modifier.padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        steps.forEach { (mm, label) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color(PrecipForecastOverlay.legendColor(mm)))
                )
                Text(
                    label,
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 10.sp,
                    modifier = Modifier.padding(start = 3.dp)
                )
            }
        }
    }
}
