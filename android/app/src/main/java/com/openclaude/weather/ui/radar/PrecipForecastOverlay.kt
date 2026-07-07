package com.openclaude.weather.ui.radar

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Point
import android.graphics.Rect
import com.openclaude.weather.data.repository.WeatherRepository
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Overlay

/**
 * Draws a native precipitation-forecast "radar". Each grid cell becomes one pixel of a
 * tiny bitmap which is then drawn stretched over the map with bilinear filtering, so the
 * discrete cells blend into smooth gradient blobs with soft edges — rather than hard
 * squares. Data is Open-Meteo (CC-BY), fully licensed for any use.
 */
class PrecipForecastOverlay : Overlay() {

    var grid: WeatherRepository.PrecipGrid? = null
    var hourIndex: Int = 0

    private val paint = Paint().apply {
        isAntiAlias = true
        isFilterBitmap = true // bilinear smoothing when the small bitmap is scaled up
        isDither = true
    }
    private val nw = GeoPoint(0.0, 0.0)
    private val se = GeoPoint(0.0, 0.0)
    private val pNW = Point()
    private val pSE = Point()
    private val dst = Rect()

    private var cachedBitmap: Bitmap? = null
    private var cachedHour: Int = -1
    private var cachedGrid: WeatherRepository.PrecipGrid? = null

    override fun draw(canvas: Canvas, mapView: MapView, shadow: Boolean) {
        if (shadow) return
        val g = grid ?: return
        if (g.isEmpty || g.rows <= 0 || g.cols <= 0) return
        if (g.cells.size != g.rows * g.cols) return
        val bmp = bitmapFor(g) ?: return

        // Geographic extent of the grid (cell centres expanded by half a step).
        var minLat = Double.MAX_VALUE; var maxLat = -Double.MAX_VALUE
        var minLon = Double.MAX_VALUE; var maxLon = -Double.MAX_VALUE
        for (cell in g.cells) {
            if (cell.lat < minLat) minLat = cell.lat
            if (cell.lat > maxLat) maxLat = cell.lat
            if (cell.lon < minLon) minLon = cell.lon
            if (cell.lon > maxLon) maxLon = cell.lon
        }
        nw.setCoords(maxLat + g.latStep / 2, minLon - g.lonStep / 2)
        se.setCoords(minLat - g.latStep / 2, maxLon + g.lonStep / 2)

        val proj = mapView.projection
        proj.toPixels(nw, pNW)
        proj.toPixels(se, pSE)
        dst.set(pNW.x, pNW.y, pSE.x, pSE.y)
        canvas.drawBitmap(bmp, null, dst, paint)
    }

    private fun bitmapFor(g: WeatherRepository.PrecipGrid): Bitmap? {
        if (cachedBitmap != null && cachedHour == hourIndex && cachedGrid === g) return cachedBitmap
        val w = g.cols
        val h = g.rows
        val pixels = IntArray(w * h)
        for (idx in g.cells.indices) {
            val r = idx / w
            val c = idx % w
            // Row 0 of the request is the southernmost; bitmap row 0 is north (top).
            val y = h - 1 - r
            val v = g.cells[idx].precip.getOrNull(hourIndex) ?: 0.0
            pixels[y * w + c] = smoothColor(v)
        }
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        bmp.setPixels(pixels, 0, w, 0, 0, w, h)
        cachedBitmap = bmp
        cachedHour = hourIndex
        cachedGrid = g
        return bmp
    }

    companion object {
        /**
         * ARGB colour for a precipitation rate (mm/h); 0 (transparent) below threshold.
         * Dark teal->navy ramp like TV radars: light rain teal, heavy rain deep blue/violet.
         */
        fun smoothColor(mmPerHour: Double): Int = when {
            mmPerHour < 0.08 -> 0
            mmPerHour < 0.3 -> 0xBF28A0BE.toInt()  // teal
            mmPerHour < 0.7 -> 0xCC1478B4.toInt()  // steel blue
            mmPerHour < 1.5 -> 0xD90F5096.toInt()  // deep blue
            mmPerHour < 3.0 -> 0xE30A3270.toInt()  // dark blue
            mmPerHour < 6.0 -> 0xEB0A1E50.toInt()  // navy
            mmPerHour < 12.0 -> 0xF11E1450.toInt() // dark indigo
            else -> 0xF646145A.toInt()             // violet (extreme)
        }

        /** Opaque swatch colour for the legend. */
        fun legendColor(mmPerHour: Double): Int = when {
            mmPerHour < 0.3 -> 0xFF28A0BE.toInt()
            mmPerHour < 1.5 -> 0xFF0F5096.toInt()
            mmPerHour < 3.0 -> 0xFF0A3270.toInt()
            mmPerHour < 6.0 -> 0xFF0A1E50.toInt()
            mmPerHour < 12.0 -> 0xFF1E1450.toInt()
            else -> 0xFF46145A.toInt()
        }
    }
}
