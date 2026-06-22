package com.openclaude.weather.ui.radar

import android.annotation.SuppressLint
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
    // Recompute the URL only when the location changes — never on unrelated recompositions.
    val url = remember(location?.id) { buildWindyUrl(lat, lon) }

    AndroidView(
        modifier = modifier,
        factory = { context ->
            WebView(context).apply {
                webViewClient = WebViewClient()
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.databaseEnabled = true
                // Windy renders its map with WebGL; keep the WebView hardware-accelerated.
                setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null)
                tag = url
                loadUrl(url)
            }
        },
        update = { web ->
            // Only (re)load when the target URL actually changes; reloading on every
            // recomposition would restart Windy before its map can render.
            if (web.tag != url) {
                web.tag = url
                web.loadUrl(url)
            }
        }
    )
}

private fun buildWindyUrl(lat: Double, lon: Double): String =
    "https://embed.windy.com/embed2.html" +
        "?lat=$lat&lon=$lon&detailLat=$lat&detailLon=$lon" +
        "&zoom=7&level=surface&overlay=radar&product=radar" +
        "&menu=&message=true&marker=true&calendar=now&pressure=" +
        "&type=map&location=coordinates&detail=" +
        "&metricWind=km%2Fh&metricTemp=%C2%B0C&radarRange=-1"
