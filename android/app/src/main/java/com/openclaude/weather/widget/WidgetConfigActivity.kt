package com.openclaude.weather.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.openclaude.weather.R
import com.openclaude.weather.WeatherApp
import com.openclaude.weather.ui.theme.WeatherTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Shown when a widget is placed: pick which saved location this instance shows. */
class WidgetConfigActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID
        // Default to cancelled: if the user backs out, the widget is not placed.
        setResult(RESULT_CANCELED)
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish(); return
        }
        val store = WeatherApp.container().locationStore

        setContent {
            WeatherTheme {
                val locations by store.locations.collectAsState(initial = emptyList())
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(listOf(Color(0xFF0F172A), Color(0xFF1E293B)))
                        )
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        stringResource(R.string.widget_pick_location),
                        color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                    OptionRow(
                        label = stringResource(R.string.widget_follow_app),
                        icon = true
                    ) { confirm(appWidgetId, WidgetState.KEY_SELECTED) }
                    locations.forEach { loc ->
                        OptionRow(label = loc.displayName, icon = false) {
                            confirm(appWidgetId, loc.id)
                        }
                    }
                }
            }
        }
    }

    private fun confirm(appWidgetId: Int, locationKey: String) {
        WidgetState.setWidgetLocation(this, appWidgetId, locationKey)
        WeatherWidgetWorker.schedule(this)
        WeatherWidgetService.start(this)
        WeatherWidgetService.refreshData(this)
        // Render immediately with whatever snapshot exists.
        WidgetUpdater.notifyWidgets(this)
        CoroutineScope(Dispatchers.IO).launch {
            runCatching { WidgetUpdater.refresh(applicationContext) }
        }
        setResult(RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId))
        finish()
    }
}

@androidx.compose.runtime.Composable
private fun OptionRow(label: String, icon: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.12f))
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            if (icon) Icons.Filled.Smartphone else Icons.Filled.LocationCity,
            contentDescription = null, tint = Color(0xFF9FD0FF), modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.size(12.dp))
        Text(label, color = Color.White, fontSize = 15.sp)
    }
}
