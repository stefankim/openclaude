package com.openclaude.weather.ui.radar

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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

/**
 * Multi-day precipitation / storm radar via the free Windy embed in a WebView, with an
 * always-available "open in browser" escape hatch — WebView rendering of Windy's map can
 * be flaky on some devices, and the browser route always works.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WindyRadarView(location: SavedLocation?, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val lat = location?.latitude ?: 48.7
    val lon = location?.longitude ?: 19.7 // roughly the centre of Slovakia
    // Recompute the URL only when the location changes — never on unrelated recompositions.
    val url = remember(location?.id) { buildWindyUrl(lat, lon) }
    val browserUrl = remember(location?.id) {
        "https://www.windy.com/?radar,%.3f,%.3f,8".format(lat, lon)
    }
    var failed by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                WebView(ctx).apply {
                    webChromeClient = WebChromeClient()
                    webViewClient = object : WebViewClient() {
                        override fun onReceivedError(
                            view: WebView?, request: WebResourceRequest?, error: WebResourceError?
                        ) {
                            // Only a failed main-frame load counts as broken.
                            if (request?.isForMainFrame == true) failed = true
                        }
                    }
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.databaseEnabled = true
                    // NOTE: deliberately NOT setting useWideViewPort/loadWithOverviewMode —
                    // they rescale Windy's responsive layout and break the map canvas.
                    settings.mediaPlaybackRequiresUserGesture = false
                    tag = url
                    loadUrl(url)
                }
            },
            update = { web ->
                // Only (re)load when the target URL actually changes; reloading on every
                // recomposition would restart Windy before its map can render.
                if (web.tag != url) {
                    web.tag = url
                    failed = false
                    web.loadUrl(url)
                }
            }
        )

        if (failed) {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Windy could not load here", color = Color.White, fontWeight = FontWeight.SemiBold)
                Text(
                    "Use the button below to open it in your browser",
                    color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp
                )
            }
        }

        // Escape hatch: open the same radar view in the browser (always renders correctly).
        Row(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(Color(0xCC0F172A))
                .clickable {
                    runCatching {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(browserUrl)))
                    }
                }
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Filled.OpenInBrowser, contentDescription = null,
                tint = Color(0xFF9FD0FF), modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.size(6.dp))
            Text("Windy.com", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

private fun buildWindyUrl(lat: Double, lon: Double): String =
    "https://embed.windy.com/embed2.html" +
        "?lat=$lat&lon=$lon&detailLat=$lat&detailLon=$lon" +
        "&zoom=7&level=surface&overlay=radar&product=radar" +
        "&menu=&message=true&marker=true&calendar=now&pressure=" +
        "&type=map&location=coordinates&detail=" +
        "&metricWind=km%2Fh&metricTemp=%C2%B0C&radarRange=-1"
