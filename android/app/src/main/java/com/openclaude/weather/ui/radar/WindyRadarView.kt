package com.openclaude.weather.ui.radar

import android.annotation.SuppressLint
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.openclaude.weather.data.local.SavedLocation

/**
 * Multi-day precipitation / storm radar for Slovakia (and anywhere) via the free Windy
 * embed. Windy blends live radar (including SHMÚ data over Slovakia) with forecast model
 * precipitation, so its timeline reaches days into the future — covering the gap left by
 * the free RainViewer radar's ~2 h window.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WindyRadarView(location: SavedLocation?, modifier: Modifier = Modifier) {
    val lat = location?.latitude ?: 48.7
    val lon = location?.longitude ?: 19.7 // roughly the centre of Slovakia

    AndroidView(
        modifier = modifier,
        factory = { context ->
            WebView(context).apply {
                webViewClient = WebViewClient()
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.loadWithOverviewMode = true
                settings.useWideViewPort = true
                loadUrl(buildWindyUrl(lat, lon))
            }
        },
        update = { it.loadUrl(buildWindyUrl(lat, lon)) }
    )
}

private fun buildWindyUrl(lat: Double, lon: Double): String =
    "https://embed.windy.com/embed2.html" +
        "?lat=$lat&lon=$lon&detailLat=$lat&detailLon=$lon" +
        "&zoom=7&level=surface&overlay=radar&product=radar" +
        "&menu=&message=true&marker=true&calendar=now&pressure=" +
        "&type=map&location=coordinates&detail=" +
        "&metricWind=km%2Fh&metricTemp=%C2%B0C&radarRange=-1"
