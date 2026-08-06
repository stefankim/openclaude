package com.openclaude.weather.ui.radar

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Point
import android.graphics.Rect
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Overlay

/**
 * Draws a SHMÚ radar composite PNG over the map at its published geographic extent.
 * The bounds match the ones shmu.sk uses for its own Leaflet image overlay, so the
 * imagery lines up with the basemap.
 */
class ShmuRadarOverlay : Overlay() {

    var bitmap: Bitmap? = null

    /** Extent of the radar composite: N 50.7, S 46.05, W 13.6, E 23.79. */
    private val nw = GeoPoint(50.7, 13.6)
    private val se = GeoPoint(46.05, 23.79)

    private val paint = Paint(Paint.FILTER_BITMAP_FLAG).apply {
        isAntiAlias = true
        isDither = true
    }
    private val pNW = Point()
    private val pSE = Point()
    private val dst = Rect()

    override fun draw(canvas: Canvas, mapView: MapView, shadow: Boolean) {
        if (shadow) return
        val bmp = bitmap ?: return
        if (bmp.isRecycled) return
        val proj = mapView.projection
        proj.toPixels(nw, pNW)
        proj.toPixels(se, pSE)
        dst.set(pNW.x, pNW.y, pSE.x, pSE.y)
        canvas.drawBitmap(bmp, null, dst, paint)
    }
}
