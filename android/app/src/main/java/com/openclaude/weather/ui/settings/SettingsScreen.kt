package com.openclaude.weather.ui.settings

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.openclaude.weather.R
import com.openclaude.weather.data.local.LocationStore
import com.openclaude.weather.ui.CompareState
import com.openclaude.weather.ui.components.SectionTitle
import com.openclaude.weather.util.Format
import com.openclaude.weather.util.Units

private val MODELS = listOf(
    "best_match" to "Auto",
    "ecmwf_ifs025" to "ECMWF",
    "icon_seamless" to "ICON",
    "gfs_seamless" to "GFS"
)

@Composable
fun SettingsScreen(
    settings: LocationStore.Settings,
    model: String,
    compare: CompareState,
    onBack: () -> Unit,
    onSetTempUnit: (String) -> Unit,
    onSetWindUnit: (String) -> Unit,
    onSetModel: (String) -> Unit,
    onSetAnimIntensity: (String) -> Unit,
    onSetNotifMorning: (Boolean) -> Unit,
    onSetNotifRain: (Boolean) -> Unit,
    onSetNotifAlerts: (Boolean) -> Unit,
    onLoadCompare: () -> Unit
) {
    var showCompare by remember { mutableStateOf(false) }

    // Ask for notification permission (API 33+) when any notification toggle is enabled.
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }
    fun ensureNotifPermission() {
        if (Build.VERSION.SDK_INT >= 33) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back), tint = Color.White)
            }
            Text(
                stringResource(R.string.settings),
                color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold
            )
        }

        SectionTitle(stringResource(R.string.units))
        SettingRowLabel(stringResource(R.string.temperature))
        ChipRow(
            options = listOf("C" to "°C", "F" to "°F"),
            selected = settings.tempUnit,
            onSelect = onSetTempUnit
        )
        SettingRowLabel(stringResource(R.string.wind))
        ChipRow(
            options = listOf("kmh" to "km/h", "ms" to "m/s", "mph" to "mph"),
            selected = settings.windUnit,
            onSelect = onSetWindUnit
        )

        Spacer(Modifier.height(16.dp))
        SectionTitle(stringResource(R.string.forecast_model))
        Text(
            stringResource(R.string.model_hint),
            color = Color.White.copy(alpha = 0.65f), fontSize = 11.sp,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
        )
        ChipRow(options = MODELS, selected = model, onSelect = onSetModel)
        TextButton(onClick = { showCompare = true; onLoadCompare() }) {
            Text(stringResource(R.string.compare_models), color = Color(0xFF9FD0FF))
        }

        Spacer(Modifier.height(8.dp))
        SectionTitle(stringResource(R.string.notifications))
        ToggleRow(stringResource(R.string.notif_morning), settings.notifMorning) {
            if (it) ensureNotifPermission(); onSetNotifMorning(it)
        }
        ToggleRow(stringResource(R.string.notif_rain), settings.notifRain) {
            if (it) ensureNotifPermission(); onSetNotifRain(it)
        }
        ToggleRow(stringResource(R.string.notif_alerts), settings.notifAlerts) {
            if (it) ensureNotifPermission(); onSetNotifAlerts(it)
        }

        Spacer(Modifier.height(16.dp))
        SectionTitle(stringResource(R.string.animations))
        ChipRow(
            options = listOf(
                "low" to stringResource(R.string.anim_low),
                "normal" to stringResource(R.string.anim_normal),
                "high" to stringResource(R.string.anim_high)
            ),
            selected = settings.animIntensity,
            onSelect = onSetAnimIntensity
        )

        Spacer(Modifier.height(32.dp))
    }

    if (showCompare) {
        AlertDialog(
            onDismissRequest = { showCompare = false },
            confirmButton = {
                TextButton(onClick = { showCompare = false }) { Text(stringResource(R.string.close)) }
            },
            title = { Text(stringResource(R.string.compare_models)) },
            text = {
                if (compare.loading) {
                    Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    CompareTable(compare)
                }
            }
        )
    }
}

@Composable
private fun CompareTable(compare: CompareState) {
    val days = compare.rows.firstOrNull()?.days ?: emptyList()
    Column {
        Row {
            Text("", modifier = Modifier.weight(1f))
            compare.rows.forEach { row ->
                Text(
                    MODELS.firstOrNull { it.first == row.model }?.second ?: row.model,
                    modifier = Modifier.weight(1f),
                    fontWeight = FontWeight.Bold, fontSize = 12.sp
                )
            }
        }
        days.forEachIndexed { i, day ->
            Row(modifier = Modifier.padding(vertical = 2.dp)) {
                Text(Format.weekday(day), modifier = Modifier.weight(1f), fontSize = 12.sp)
                compare.rows.forEach { row ->
                    val v = row.tMax.getOrNull(i)
                    Text(
                        if (v != null) "${Math.round(Units.convertTemp(v))}°" else "—",
                        modifier = Modifier.weight(1f), fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingRowLabel(text: String) {
    Text(
        text, color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp,
        modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 4.dp)
    )
}

@Composable
private fun ChipRow(
    options: List<Pair<String, String>>,
    selected: String,
    onSelect: (String) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { (id, label) ->
            val isSel = id == selected
            Text(
                text = label,
                color = if (isSel) Color(0xFF0F172A) else Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (isSel) Color.White else Color.White.copy(alpha = 0.14f))
                    .clickable { onSelect(id) }
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            )
        }
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = Color.White, fontSize = 14.sp, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
