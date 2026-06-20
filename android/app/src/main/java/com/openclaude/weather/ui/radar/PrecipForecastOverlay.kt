package com.openclaude.weather.ui.radar

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Point
import com.openclaude.weather.data.repository.WeatherRepository
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Overlay

/**
 * Draws a native precipitation-forecast "radar" by filling each grid cell with a
 * radar-style colour based on the precipitation (mm/h) at the selected [hourIndex].
 * Data is Open-Meteo (CC-BY) — fully licensed for any use.
 */
class PrecipForecastOverlay : Overlay() {

    var grid: WeatherRepository.PrecipGrid? = null
    var hourIndex: Int = 0

    private val paint = Paint().apply { isAntiAlias = false; style = Paint.Style.FILL }
    private val tl = Point()
    private val br = Point()
    private val tlGeo = GeoPoint(0.0, 0.0)
    private val brGeo = GeoPoint(0.0, 0.0)

    override fun draw(canvas: Canvas, mapView: MapView, shadow: Boolean) {
        if (shadow) return
        val g = grid ?: return
        if (g.isEmpty) return
        val proj = mapView.projection
        val halfLat = g.latStep / 2
        val halfLon = g.lonStep / 2

        for (cell in g.cells) {
            val value = cell.precip.getOrNull(hourIndex) ?: 0.0
            val color = colorFor(value) ?: continue
            tlGeo.setCoords(cell.lat + halfLat, cell.lon - halfLon)
            brGeo.setCoords(cell.lat - halfLat, cell.lon + halfLon)
            proj.toPixels(tlGeo, tl)
            proj.toPixels(brGeo, br)
            paint.color = color
            canvas.drawRect(tl.x.toFloat(), tl.y.toFloat(), br.x.toFloat(), br.y.toFloat(), paint)
        }
    }

    companion object {
        /** Standard precipitation colour ramp (ARGB). Returns null below the visible threshold. */
        fun colorFor(mmPerHour: Double): Int? = when {
            mmPerHour < 0.1 -> null
            mmPerHour < 0.5 -> 0x664FA3F7.toInt()  // very light – pale blue
            mmPerHour < 1.0 -> 0x803377FF.toInt()  // light – blue
            mmPerHour < 2.5 -> 0x9933CC66.toInt()  // moderate – green
            mmPerHour < 5.0 -> 0xA6FFD23F.toInt()  // heavy – yellow
            mmPerHour < 10.0 -> 0xB3FF8C2B.toInt() // very heavy – orange
            else -> 0xC6E53935.toInt()             // extreme – red
        }
    }
}
