package com.openclaude.weather.ui.locations

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import com.openclaude.weather.R
import com.openclaude.weather.data.local.LocationStore
import com.openclaude.weather.data.local.SavedLocation
import com.openclaude.weather.data.remote.GeoResult
import com.openclaude.weather.data.repository.WeatherRepository
import com.openclaude.weather.ui.SearchState
import com.openclaude.weather.ui.components.SectionTitle
import com.openclaude.weather.ui.components.WeatherGlyph
import com.openclaude.weather.util.Format
import com.openclaude.weather.util.WeatherCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

@Composable
fun LocationsScreen(
    locations: List<SavedLocation>,
    selectedId: String?,
    search: SearchState,
    previews: Map<String, WeatherRepository.PlacePreview>,
    onQueryChange: (String) -> Unit,
    onAdd: (GeoResult) -> Unit,
    onAddManual: (name: String, region: String?, country: String?, lat: Double, lon: Double) -> Unit,
    onSelect: (String) -> Unit,
    onRemove: (String) -> Unit,
    onMove: (String, Boolean) -> Unit,
    onMessage: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var locating by remember { mutableStateOf(false) }
    val msgLocating = stringResource(R.string.locating)
    val msgDenied = stringResource(R.string.location_permission_needed)
    val msgFailed = stringResource(R.string.location_failed)
    val fallbackName = stringResource(R.string.my_location)

    fun resolveAndAdd() {
        locating = true
        onMessage(msgLocating)
        scope.launch {
            val loc = currentLocation(context)
            if (loc == null) {
                locating = false
                onMessage(msgFailed)
                return@launch
            }
            val (name, region, country) = withContext(Dispatchers.IO) {
                runCatching {
                    @Suppress("DEPRECATION")
                    val a = Geocoder(context).getFromLocation(loc.latitude, loc.longitude, 1)?.firstOrNull()
                    Triple(a?.locality ?: a?.subAdminArea ?: fallbackName, a?.adminArea, a?.countryName)
                }.getOrDefault(Triple(fallbackName, null, null))
            }
            locating = false
            onAddManual(name, region, country, loc.latitude, loc.longitude)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        if (grants.values.any { it }) resolveAndAdd() else onMessage(msgDenied)
    }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {

        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = search.query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = { Text(stringResource(R.string.search_city), color = Color.White.copy(alpha = 0.6f)) },
            leadingIcon = { Icon(Icons.Filled.Search, null, tint = Color.White) },
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                cursorColor = Color.White,
                focusedBorderColor = Color.White.copy(alpha = 0.6f),
                unfocusedBorderColor = Color.White.copy(alpha = 0.3f)
            )
        )

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // "Use my location" chip.
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White.copy(alpha = 0.14f))
                    .clickable(enabled = !locating) {
                        val fine = ContextCompat.checkSelfPermission(
                            context, Manifest.permission.ACCESS_FINE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED
                        val coarse = ContextCompat.checkSelfPermission(
                            context, Manifest.permission.ACCESS_COARSE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED
                        if (fine || coarse) resolveAndAdd()
                        else permissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_COARSE_LOCATION,
                                Manifest.permission.ACCESS_FINE_LOCATION
                            )
                        )
                    }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (locating) {
                    CircularProgressIndicator(
                        color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(14.dp)
                    )
                } else {
                    Icon(Icons.Filled.MyLocation, null, tint = Color(0xFF9FD0FF), modifier = Modifier.size(16.dp))
                }
                Spacer(Modifier.size(6.dp))
                Text(stringResource(R.string.use_my_location), color = Color.White, fontSize = 13.sp)
            }
            Spacer(Modifier.weight(1f))
            Text(
                stringResource(R.string.saved_count, locations.size, LocationStore.MAX_LOCATIONS),
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 12.sp
            )
        }

        // Search results take over the list when the user is typing.
        if (search.query.length >= 2) {
            Spacer(Modifier.height(8.dp))
            SectionTitle(stringResource(R.string.results))
            if (search.loading) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.padding(16.dp))
            } else if (search.message != null) {
                Text(search.message, color = Color.White.copy(alpha = 0.8f), modifier = Modifier.padding(8.dp))
            }
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(search.results) { r ->
                    ResultRow(r) { onAdd(r) }
                }
            }
        } else {
            Spacer(Modifier.height(8.dp))
            SectionTitle(stringResource(R.string.your_locations))
            if (locations.isEmpty()) {
                Text(
                    stringResource(R.string.no_locations, LocationStore.MAX_LOCATIONS),
                    color = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.padding(8.dp)
                )
            }
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(locations, key = { it.id }) { loc ->
                    SavedRow(
                        location = loc,
                        selected = loc.id == selectedId,
                        preview = previews[loc.id],
                        canMoveUp = locations.firstOrNull()?.id != loc.id,
                        canMoveDown = locations.lastOrNull()?.id != loc.id,
                        onSelect = { onSelect(loc.id) },
                        onRemove = { onRemove(loc.id) },
                        onMove = { up -> onMove(loc.id, up) }
                    )
                }
            }
        }
    }
}

/** Single-shot current location via the framework LocationManager (no Play Services). */
private suspend fun currentLocation(context: Context): Location? {
    val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
    val provider = when {
        lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
        lm.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
        else -> return null
    }
    // Fresh-enough last-known fix is fine for weather.
    runCatching {
        lm.getLastKnownLocation(provider)?.let {
            if (System.currentTimeMillis() - it.time < 10 * 60 * 1000) return it
        }
    }
    return suspendCancellableCoroutine { cont ->
        val consumer = androidx.core.util.Consumer<Location?> { location ->
            if (cont.isActive) cont.resume(location)
        }
        runCatching {
            LocationManagerCompat.getCurrentLocation(
                lm, provider, null as android.os.CancellationSignal?,
                ContextCompat.getMainExecutor(context), consumer
            )
        }.onFailure { if (cont.isActive) cont.resume(null) }
    }
}

@Composable
private fun ResultRow(result: GeoResult, onAdd: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.14f))
            .clickable { onAdd() }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Filled.LocationCity, null, tint = Color.White, modifier = Modifier.size(22.dp))
        Spacer(Modifier.size(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(result.name, color = Color.White, fontWeight = FontWeight.SemiBold)
            Text(
                listOfNotNull(result.admin1, result.country).joinToString(", "),
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 12.sp
            )
        }
        Icon(Icons.Filled.Add, stringResource(R.string.add), tint = Color(0xFF9FD0FF))
    }
}

@Composable
private fun SavedRow(
    location: SavedLocation,
    selected: Boolean,
    preview: WeatherRepository.PlacePreview?,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onSelect: () -> Unit,
    onRemove: () -> Unit,
    onMove: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) Color.White.copy(alpha = 0.26f) else Color.White.copy(alpha = 0.14f))
            .clickable { onSelect() }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (selected) {
            Icon(Icons.Filled.CheckCircle, stringResource(R.string.selected), tint = Color(0xFF9FD0FF), modifier = Modifier.size(20.dp))
        } else {
            Icon(Icons.Filled.LocationCity, null, tint = Color.White, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.size(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(location.displayName, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            location.country?.let {
                Text(it, color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
            }
        }
        // Live mini-preview: current temp + condition glyph.
        preview?.let { p ->
            val cond = WeatherCode.map(p.weatherCode, p.isDay)
            WeatherGlyph(cond.scene, p.isDay, modifier = Modifier.size(26.dp))
            Spacer(Modifier.size(4.dp))
            Text(Format.temp(p.temperatureC), color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        }
        Column {
            IconButton(onClick = { onMove(true) }, enabled = canMoveUp, modifier = Modifier.size(26.dp)) {
                Icon(
                    Icons.Filled.KeyboardArrowUp, stringResource(R.string.move_up),
                    tint = if (canMoveUp) Color.White.copy(alpha = 0.8f) else Color.White.copy(alpha = 0.25f)
                )
            }
            IconButton(onClick = { onMove(false) }, enabled = canMoveDown, modifier = Modifier.size(26.dp)) {
                Icon(
                    Icons.Filled.KeyboardArrowDown, stringResource(R.string.move_down),
                    tint = if (canMoveDown) Color.White.copy(alpha = 0.8f) else Color.White.copy(alpha = 0.25f)
                )
            }
        }
        IconButton(onClick = onRemove) {
            Icon(Icons.Filled.Delete, stringResource(R.string.remove), tint = Color.White.copy(alpha = 0.85f))
        }
    }
}
