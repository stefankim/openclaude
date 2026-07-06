package com.openclaude.weather.ui.radar

import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.openclaude.weather.data.local.SavedLocation

/**
 * Windy multi-day radar. Windy's map is WebGL-based and does not render inside many
 * devices' WebViews (blank grey canvas), so instead of embedding we launch it in a
 * Chrome Custom Tab — real browser rendering that always works, opening over the app.
 */
@Composable
fun WindyRadarView(location: SavedLocation?, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val lat = location?.latitude ?: 48.7
    val lon = location?.longitude ?: 19.7 // roughly the centre of Slovakia
    val url = remember(location?.id) { "https://www.windy.com/?radar,%.3f,%.3f,8".format(lat, lon) }

    fun open() {
        runCatching {
            CustomTabsIntent.Builder().setShowTitle(true).build()
                .launchUrl(context, Uri.parse(url))
        }.onFailure {
            // No Custom Tabs provider — plain browser intent as last resort.
            runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
        }
    }

    Column(
        modifier = modifier.padding(32.dp),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Filled.Radar, contentDescription = null,
            tint = Color(0xFF9FD0FF), modifier = Modifier.size(48.dp)
        )
        Spacer(Modifier.height(16.dp))
        Text(
            "Windy radar",
            color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Windy's interactive multi-day radar opens in the browser — its map engine " +
                "doesn't render inside the app. It opens centred on your location.",
            color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(20.dp))
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(Color.White)
                .clickable { open() }
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Filled.OpenInBrowser, contentDescription = null,
                tint = Color(0xFF0F172A), modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.size(8.dp))
            Text(
                "Open Windy radar",
                color = Color(0xFF0F172A), fontSize = 15.sp, fontWeight = FontWeight.SemiBold
            )
        }
    }
}
