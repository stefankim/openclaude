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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.openclaude.weather.R
import com.openclaude.weather.data.local.SavedLocation
import com.openclaude.weather.util.Format
import kotlinx.coroutines.delay
import org.osmdroid.events.DelayedMapListener
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.tileprovider.MapTileProviderBasic
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.util.GeoPoint
import org.osmdroid.util.MapTileIndex
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.CopyrightOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.TilesOverlay

/** RainViewer tile source: formats a "{z}/{x}/{y}" template into the precipitation tile URL. */
private class RainViewerTileSource(name: String, private val template: String) :
    OnlineTileSourceBase(name, 0, 12, 512, ".png", arrayOf("")) {
    override fun getTileURLString(pMapTileIndex: Long): String {
        val zoom = MapTileIndex.getZoom(pMapTileIndex)
        val x = MapTileIndex.getX(pMapTileIndex)
        val y = MapTileIndex.getY(pMapTileIndex)
        return String.format(template, zoom, x, y)
    }
}

/**
 * Base map: Esri Light Gray Canvas. OpenStreetMap's tile server frequently blocks mobile
 * apps (tiles never load, leaving the blank placeholder grid); Esri's canvas tiles are
 * app-friendly and their muted style makes the rain colours stand out like TV radars.
 */
private val EsriLightGray = object : OnlineTileSourceBase(
    "EsriLightGray", 0, 16, 256, "",
    arrayOf("https://server.arcgisonline.com/ArcGIS/rest/services/Canvas/World_Light_Gray_Base/MapServer/tile/"),
    "© Esri — Esri, HERE, Garmin, © OpenStreetMap contributors"
) {
    override fun getTileURLString(pMapTileIndex: Long): String {
        val zoom = MapTileIndex.getZoom(pMapTileIndex)
        val x = MapTileIndex.getX(pMapTileIndex)
        val y = MapTileIndex.getY(pMapTileIndex)
        // Esri tile order is zoom/y/x.
        return baseUrl + "$zoom/$y/$x"
    }
}

private enum class RadarMode(val labelRes: Int) {
    LIVE(R.string.radar_live), SHMU(R.string.radar_shmu),
    FORECAST(R.string.radar_forecast), WINDY(R.string.radar_windy)
}

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
    shmu: ShmuState,
    location: SavedLocation?,
    onRetry: () -> Unit,
    onLoadForecast: (Double, Double, Boolean) -> Unit,
    onLoadShmu: () -> Unit
) {
    val context = LocalContext.current

    // osmdroid User-Agent is configured once in WeatherApp.onCreate(), before any MapView
    // is built, so the OpenStreetMap base tiles load correctly here.

    var frameIndex by remember { mutableStateOf(0) }
    var playing by remember { mutableStateOf(true) }
    var mode by remember { mutableStateOf(RadarMode.LIVE) }
    var forecastHour by remember { mutableStateOf(0) }
    var forecastPlaying by remember { mutableStateOf(false) }
    var fineZoom by remember { mutableStateOf(false) }
    var shmuIndex by remember { mutableStateOf(0) }
    var shmuPlaying by remember { mutableStateOf(true) }

    // Fetch SHMU frames on first entry to that mode.
    LaunchedEffect(mode) { if (mode == RadarMode.SHMU) onLoadShmu() }

    // SHMU animation: only step onto frames whose image has already decoded.
    LaunchedEffect(shmuPlaying, mode, shmu.bitmaps.size) {
        if (mode != RadarMode.SHMU) return@LaunchedEffect
        val ready = shmu.frames.count { shmu.bitmaps.containsKey(it.url) }
        if (ready == 0) return@LaunchedEffect
        while (shmuPlaying) {
            delay(500)
            shmuIndex = (shmuIndex + 1) % ready
        }
    }

    // Load the native forecast grid when entering Forecast mode, changing location, or
    // crossing the zoom threshold (finer grid when zoomed in).
    LaunchedEffect(mode, location?.id, fineZoom) {
        if (mode == RadarMode.FORECAST && location != null) {
            onLoadForecast(location.latitude, location.longitude, fineZoom)
        }
    }

    // Forecast autoplay: step through the hours while playing.
    LaunchedEffect(forecastPlaying, mode, forecast.grid?.times?.size) {
        val count = forecast.grid?.times?.size ?: return@LaunchedEffect
        if (mode != RadarMode.FORECAST || count == 0) return@LaunchedEffect
        while (forecastPlaying) {
            delay(600)
            forecastHour = (forecastHour + 1) % count
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
            setTileSource(EsriLightGray)
            setMultiTouchControls(true)
            controller.setZoom(7.5)
            isHorizontalMapRepetitionEnabled = false
            isTilesScaledToDpi = true
            overlays.add(CopyrightOverlay(context))
        }
    }
    // Track zoom so the Forecast grid can switch to a finer resolution when zoomed in.
    DisposableEffect(mapView) {
        val listener = DelayedMapListener(object : MapListener {
            override fun onZoom(event: ZoomEvent?): Boolean {
                fineZoom = (event?.zoomLevel ?: 7.5) >= 8.2
                return false
            }
            override fun onScroll(event: ScrollEvent?): Boolean = false
        }, 300)
        mapView.addMapListener(listener)
        onDispose { mapView.removeMapListener(listener) }
    }

    // One overlay per RainViewer frame, kept alive so replays hit warm tile caches
    // instead of stuttering through re-downloads. Rebuilt when a new frame set arrives.
    val frameOverlays = remember(state.frames) { mutableMapOf<Long, TilesOverlay>() }
    val precipOverlay = remember { PrecipForecastOverlay() }
    val shmuOverlay = remember { ShmuRadarOverlay() }
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
        // The MapView must never leave the composition: removing and re-attaching the same
        // View instance (e.g. after visiting the Windy mode) leaves it blank. The Windy
        // launcher panel is drawn over the map instead.
        run {
            AndroidView(
                factory = { mapView },
                modifier = Modifier.fillMaxSize(),
                update = { mv ->
                    when (mode) {
                        RadarMode.LIVE -> {
                            mv.overlays.remove(precipOverlay)
                            mv.overlays.remove(shmuOverlay)
                            // Drop overlays that belong to an outdated frame set.
                            mv.overlays.removeAll { it is TilesOverlay && it !in frameOverlays.values }
                            val frame = state.frames.getOrNull(frameIndex) ?: return@AndroidView
                            // Lazily build one overlay per frame; keeping them alive means
                            // each loop after the first plays from cached tiles (no stutter).
                            val overlay = frameOverlays.getOrPut(frame.time) {
                                val source = RainViewerTileSource("rainviewer-${frame.time}", frame.tileUrlTemplate)
                                TilesOverlay(MapTileProviderBasic(mv.context, source), mv.context).apply {
                                    loadingBackgroundColor = AndroidColor.TRANSPARENT
                                    loadingLineColor = AndroidColor.TRANSPARENT
                                }
                            }
                            if (!mv.overlays.contains(overlay)) mv.overlays.add(0, overlay)
                            frameOverlays.values.forEach { it.setEnabled(it === overlay) }
                        }
                        RadarMode.SHMU -> {
                            // Native SHMU composite image; hide the other precipitation layers.
                            frameOverlays.values.forEach { it.setEnabled(false) }
                            mv.overlays.remove(precipOverlay)
                            val ready = shmu.frames.filter { shmu.bitmaps.containsKey(it.url) }
                            shmuOverlay.bitmap = ready.getOrNull(shmuIndex.coerceIn(0, (ready.size - 1).coerceAtLeast(0)))
                                ?.let { shmu.bitmaps[it.url] }
                            if (!mv.overlays.contains(shmuOverlay)) mv.overlays.add(0, shmuOverlay)
                        }
                        RadarMode.FORECAST -> {
                            // Native Open-Meteo precipitation cells; hide the RainViewer layers.
                            frameOverlays.values.forEach { it.setEnabled(false) }
                            mv.overlays.remove(shmuOverlay)
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

        // Windy launcher panel drawn over the (still-composed) map.
        if (mode == RadarMode.WINDY) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xF20F172A))
            )
            WindyRadarView(location = location, modifier = Modifier.fillMaxSize())
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
                text = stringResource(R.string.storm_rain_radar),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Text(
                text = when (mode) {
                    RadarMode.LIVE -> stringResource(R.string.radar_live_sub)
                    RadarMode.SHMU -> stringResource(R.string.radar_shmu_sub)
                    RadarMode.FORECAST -> stringResource(R.string.radar_forecast_sub)
                    RadarMode.WINDY -> stringResource(R.string.radar_windy_sub)
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
                    text = stringResource(m.labelRes),
                    color = if (selected) Color(0xFF0F172A) else Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (selected) Color.White else Color.Transparent)
                        .clickable { mode = m }
                        .padding(horizontal = 9.dp, vertical = 6.dp)
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
                        stringResource(R.string.tap_retry),
                        color = Color(0xFF9FD0FF),
                        modifier = Modifier.padding(8.dp).clip(RoundedCornerShape(8.dp)).clickable { onRetry() }
                    )
                }
            }
        }

        if (mode == RadarMode.SHMU) {
            val ready = shmu.frames.filter { shmu.bitmaps.containsKey(it.url) }
            when {
                shmu.loading && ready.isEmpty() -> CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center), color = Color.White
                )
                shmu.error != null -> Text(
                    shmu.error, color = Color.White,
                    modifier = Modifier.align(Alignment.Center).clickable { onLoadShmu() }
                )
            }
            if (ready.isNotEmpty()) {
                val frame = ready.getOrNull(shmuIndex.coerceIn(0, ready.lastIndex))
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
                        IconButton(onClick = { shmuPlaying = !shmuPlaying }) {
                            Icon(
                                if (shmuPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                contentDescription = null, tint = Color.White
                            )
                        }
                        Column(modifier = Modifier.width(120.dp)) {
                            Text(
                                frame?.let { Format.clock(it.timeUtc * 1000) } ?: "",
                                color = Color.White, fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                frame?.let { relativeLabel(it.timeUtc) } ?: "",
                                color = Color(0xFF9FD0FF), fontSize = 12.sp
                            )
                        }
                        Slider(
                            value = shmuIndex.coerceIn(0, ready.lastIndex).toFloat(),
                            onValueChange = {
                                shmuPlaying = false
                                shmuIndex = it.toInt().coerceIn(0, ready.lastIndex)
                            },
                            valueRange = 0f..ready.lastIndex.coerceAtLeast(1).toFloat(),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Text(
                        stringResource(R.string.radar_shmu_credit),
                        color = Color.White.copy(alpha = 0.6f), fontSize = 10.sp
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
                        .clickable { location?.let { onLoadForecast(it.latitude, it.longitude, fineZoom) } }
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { forecastPlaying = !forecastPlaying }) {
                            Icon(
                                if (forecastPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                contentDescription = if (forecastPlaying) "Pause" else "Play",
                                tint = Color.White
                            )
                        }
                        Column {
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
                        }
                    }
                    Slider(
                        value = forecastHour.toFloat(),
                        onValueChange = {
                            forecastPlaying = false
                            forecastHour = it.toInt().coerceIn(0, grid.times.lastIndex)
                        },
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
