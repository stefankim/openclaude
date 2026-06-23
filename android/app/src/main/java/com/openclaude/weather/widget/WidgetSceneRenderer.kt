package com.openclaude.weather.widget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
import com.openclaude.weather.domain.WeatherScene
import java.util.Random
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Renders one animation frame of a weather scene to a [Bitmap] using the native Canvas API,
 * so the home-screen widget can show a genuinely live animation by cycling [phase].
 * Mirrors the richer in-app WeatherScene look (fluffy clouds, glow, splashes, wind, mist).
 */
object WidgetSceneRenderer {

    private val rng = Random(42)
    private val rainX = FloatArray(44) { rng.nextFloat() }
    private val rainPhase = FloatArray(44) { rng.nextFloat() }
    private val rainSpeed = FloatArray(44) { 0.7f + rng.nextFloat() * 0.6f }
    private val rainLen = FloatArray(44) { 0.7f + rng.nextFloat() * 0.7f }
    private val splashX = FloatArray(6) { rng.nextFloat() }
    private val splashPhase = FloatArray(6) { rng.nextFloat() }
    private val snowX = FloatArray(34) { rng.nextFloat() }
    private val snowPhase = FloatArray(34) { rng.nextFloat() }
    private val snowSize = FloatArray(34) { 0.5f + rng.nextFloat() }
    private val starX = FloatArray(26) { rng.nextFloat() }
    private val starY = FloatArray(26) { rng.nextFloat() }
    private val windY = FloatArray(8) { 0.12f + rng.nextFloat() * 0.7f }
    private val windPhase = FloatArray(8) { rng.nextFloat() }
    private val windLen = FloatArray(8) { 0.5f + rng.nextFloat() * 0.5f }
    private val leafY = FloatArray(5) { 0.15f + rng.nextFloat() * 0.7f }
    private val leafPhase = FloatArray(5) { rng.nextFloat() }
    private val leafSize = FloatArray(5) { 0.6f + rng.nextFloat() * 0.8f }
    private val mistX = FloatArray(14) { rng.nextFloat() }
    private val mistY = FloatArray(14) { 0.25f + rng.nextFloat() * 0.6f }
    private val mistPhase = FloatArray(14) { rng.nextFloat() }
    private val mistSize = FloatArray(14) { 0.6f + rng.nextFloat() * 0.8f }

    fun render(
        scene: WeatherScene,
        isDay: Boolean,
        phase: Float,
        widthPx: Int,
        heightPx: Int,
        windKmh: Double = 0.0
    ): Bitmap {
        val w = widthPx.coerceAtLeast(1)
        val h = heightPx.coerceAtLeast(1)
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        drawBackground(canvas, paint, scene, isDay, w, h)

        when (scene) {
            WeatherScene.CLEAR_DAY -> drawSun(canvas, paint, phase, w, h)
            WeatherScene.CLEAR_NIGHT -> { drawStars(canvas, paint, phase, w, h); drawShootingStar(canvas, paint, phase, w, h); drawMoon(canvas, paint, w, h) }
            WeatherScene.PARTLY_CLOUDY -> {
                if (isDay) drawSun(canvas, paint, phase, w, h, 0.6f) else drawMoon(canvas, paint, w, h)
                drawCloud(canvas, paint, phase, w * 0.45f, h * 0.58f, w * 0.16f, 0xF2FFFFFF.toInt())
            }
            WeatherScene.CLOUDY -> {
                drawCloud(canvas, paint, phase * 0.7f, w * 0.36f, h * 0.42f, w * 0.18f, 0xE6CED6DF.toInt())
                drawCloud(canvas, paint, phase, w * 0.62f, h * 0.60f, w * 0.15f, 0xF2FFFFFF.toInt())
            }
            WeatherScene.FOG -> { drawCloud(canvas, paint, phase, w * 0.5f, h * 0.34f, w * 0.16f, 0xE6DDE3EA.toInt()); drawFog(canvas, paint, phase, w, h); drawMist(canvas, paint, phase, w, h) }
            WeatherScene.RAIN -> {
                drawCloud(canvas, paint, phase, w * 0.5f, h * 0.32f, w * 0.18f, 0xFFB9C2CC.toInt())
                drawRain(canvas, paint, phase, w, h, 0xCC9FC3E8.toInt())
                drawSplashes(canvas, paint, phase, w, h)
            }
            WeatherScene.SNOW -> {
                drawCloud(canvas, paint, phase, w * 0.5f, h * 0.30f, w * 0.18f, 0xFFE0E6EC.toInt())
                drawSnow(canvas, paint, phase, w, h)
            }
            WeatherScene.THUNDERSTORM -> {
                drawCloud(canvas, paint, phase, w * 0.5f, h * 0.30f, w * 0.20f, 0xFF7E8794.toInt())
                drawRain(canvas, paint, phase, w, h, 0xCCBFD0E0.toInt())
                drawSplashes(canvas, paint, phase, w, h)
                drawLightning(canvas, paint, phase, w, h)
            }
        }

        val windFactor = ((windKmh - 18.0) / 40.0).coerceIn(0.0, 1.0).toFloat()
        if (windFactor > 0f) {
            drawWind(canvas, paint, phase, w, h, windFactor)
            drawLeaves(canvas, paint, phase, w, h, windFactor)
        }
        return bmp
    }

    private fun drawBackground(c: Canvas, p: Paint, scene: WeatherScene, isDay: Boolean, w: Int, h: Int) {
        val (top, bottom) = when (scene) {
            WeatherScene.CLEAR_DAY -> 0xFF2A93D5.toInt() to 0xFFA8DEF0.toInt()
            WeatherScene.CLEAR_NIGHT -> 0xFF0B1026.toInt() to 0xFF2C3E6B.toInt()
            WeatherScene.PARTLY_CLOUDY -> if (isDay) 0xFF4A8FC0.toInt() to 0xFFB9D4E6.toInt() else 0xFF15203A.toInt() to 0xFF394B6E.toInt()
            WeatherScene.CLOUDY -> if (isDay) 0xFF5D6D7E.toInt() to 0xFFAEB9C4.toInt() else 0xFF1E2530.toInt() to 0xFF3C4754.toInt()
            WeatherScene.FOG -> 0xFF6B7585.toInt() to 0xFFBFC7D1.toInt()
            WeatherScene.RAIN -> if (isDay) 0xFF3A4A5C.toInt() to 0xFF6E8295.toInt() else 0xFF12161F.toInt() to 0xFF2C3A49.toInt()
            WeatherScene.SNOW -> if (isDay) 0xFF6E7E92.toInt() to 0xFFD6E0EC.toInt() else 0xFF1A2230.toInt() to 0xFF42506A.toInt()
            WeatherScene.THUNDERSTORM -> 0xFF14161F.toInt() to 0xFF3A3F57.toInt()
        }
        p.shader = LinearGradient(0f, 0f, 0f, h.toFloat(), top, bottom, Shader.TileMode.CLAMP)
        val r = min(w, h) * 0.12f
        c.drawRoundRect(0f, 0f, w.toFloat(), h.toFloat(), r, r, p)
        p.shader = null
    }

    private fun drawSun(c: Canvas, p: Paint, phase: Float, w: Int, h: Int, scale: Float = 1f) {
        val cx = w * 0.5f
        val cy = h * 0.42f
        val radius = min(w, h) * 0.16f * scale
        val breathe = 1f + 0.06f * sin(phase * 2 * Math.PI).toFloat()

        // Soft glow.
        p.shader = RadialGradient(
            cx, cy, radius * 3f * breathe,
            intArrayOf(0x66FFE08A, 0x33FFD56B, 0x00FFD56B),
            floatArrayOf(0f, 0.5f, 1f), Shader.TileMode.CLAMP
        )
        c.drawCircle(cx, cy, radius * 3f * breathe, p)
        p.shader = null

        // Rays.
        p.color = 0xFFFFD25E.toInt()
        p.strokeWidth = radius * 0.16f
        p.strokeCap = Paint.Cap.ROUND
        val rot = phase * 2 * Math.PI
        val rayLen = radius * (0.85f + 0.18f * sin(phase * 2 * Math.PI).toFloat())
        for (i in 0 until 12) {
            val a = i * Math.PI / 6 + rot
            val r1 = radius * 1.4f
            c.drawLine(
                (cx + cos(a) * r1).toFloat(), (cy + sin(a) * r1).toFloat(),
                (cx + cos(a) * (r1 + rayLen)).toFloat(), (cy + sin(a) * (r1 + rayLen)).toFloat(), p
            )
        }
        // Disc with gradient.
        p.shader = RadialGradient(
            cx - radius * 0.25f, cy - radius * 0.25f, radius * 1.3f,
            intArrayOf(0xFFFFE9A6.toInt(), 0xFFFFC94D.toInt(), 0xFFFFB300.toInt()),
            floatArrayOf(0f, 0.7f, 1f), Shader.TileMode.CLAMP
        )
        c.drawCircle(cx, cy, radius, p)
        p.shader = null
    }

    private fun drawMoon(c: Canvas, p: Paint, w: Int, h: Int) {
        val cx = w * 0.62f
        val cy = h * 0.34f
        val r = min(w, h) * 0.14f
        p.shader = RadialGradient(cx, cy, r * 2.2f, 0x55FFFFFF, 0x00FFFFFF, Shader.TileMode.CLAMP)
        c.drawCircle(cx, cy, r * 2.2f, p)
        p.shader = null
        p.color = 0xFFF2F4F8.toInt()
        c.drawCircle(cx, cy, r, p)
        p.color = 0x66152138
        c.drawCircle(cx + r * 0.5f, cy - r * 0.22f, r, p)
        p.color = 0x22152138
        c.drawCircle(cx - r * 0.3f, cy + r * 0.1f, r * 0.16f, p)
    }

    private fun drawStars(c: Canvas, p: Paint, phase: Float, w: Int, h: Int) {
        for (i in starX.indices) {
            val tw = 0.35f + 0.65f * (0.5f + 0.5f * sin((phase + i * 0.13f) * 6 * Math.PI).toFloat())
            p.color = ((tw * 255).toInt() shl 24) or 0x00FFFFFF
            c.drawCircle(starX[i] * w, starY[i] * h * 0.72f, min(w, h) * 0.012f, p)
        }
    }

    private fun drawShootingStar(c: Canvas, p: Paint, phase: Float, w: Int, h: Int) {
        val window = 0.14f
        if (phase > window) return
        val k = phase / window
        val sx = (0.15f + 0.55f * k) * w
        val sy = (0.12f + 0.25f * k) * h
        val tail = min(w, h) * 0.18f
        val alpha = ((1f - k) * 255).toInt()
        p.color = (alpha shl 24) or 0x00FFFFFF
        p.strokeWidth = min(w, h) * 0.01f
        p.strokeCap = Paint.Cap.ROUND
        c.drawLine(sx - tail, sy - tail * 0.5f, sx, sy, p)
        c.drawCircle(sx, sy, min(w, h) * 0.012f, p)
    }

    private fun drawCloud(c: Canvas, p: Paint, phase: Float, cx0: Float, cy0: Float, u: Float, color: Int) {
        val dx = (phase % 1f - 0.5f) * u * 0.6f
        val bob = sin(phase * 2 * Math.PI).toFloat() * min(u, u) * 0.05f
        val cx = cx0 + dx
        val cy = cy0 + bob
        val left = cx - u * 1.25f
        val right = cx + u * 1.25f
        val baseline = cy + u * 0.55f
        val bodyTop = cy - u * 0.05f

        // Shadow.
        p.color = 0x22000000
        c.drawOval(left + u * 0.1f, baseline - u * 0.12f, right - u * 0.1f, baseline + u * 0.18f, p)

        // Body gradient.
        p.shader = LinearGradient(0f, bodyTop - u, 0f, baseline, lighten(color, 0.10f), darken(color, 0.10f), Shader.TileMode.CLAMP)
        c.drawRoundRect(left, bodyTop, right, baseline, u * 0.45f, u * 0.45f, p)
        p.shader = null

        // Fluffy puffs.
        softPuff(c, p, cx - u * 0.85f, cy - u * 0.02f, u * 0.55f, color)
        softPuff(c, p, cx - u * 0.15f, cy - u * 0.42f, u * 0.82f, color)
        softPuff(c, p, cx + u * 0.6f, cy - u * 0.22f, u * 0.62f, color)
        softPuff(c, p, cx + u * 1.0f, cy - u * 0.02f, u * 0.5f, color)

        // Highlight.
        p.color = 0x33FFFFFF
        c.drawCircle(cx - u * 0.1f, cy - u * 0.6f, u * 0.42f, p)
    }

    private fun softPuff(c: Canvas, p: Paint, cx: Float, cy: Float, r: Float, color: Int) {
        p.shader = RadialGradient(
            cx, cy, r,
            intArrayOf(color, color, color and 0x00FFFFFF),
            floatArrayOf(0f, 0.78f, 1f), Shader.TileMode.CLAMP
        )
        c.drawCircle(cx, cy, r, p)
        p.shader = null
    }

    private fun drawRain(c: Canvas, p: Paint, phase: Float, w: Int, h: Int, color: Int) {
        p.strokeCap = Paint.Cap.ROUND
        for (i in rainX.indices) {
            val prog = (phase * rainSpeed[i] + rainPhase[i]) % 1f
            val len = h * 0.13f * rainLen[i]
            val sx = rainX[i] * w
            val sy = h * 0.4f + prog * h * 0.6f
            val a = (((color ushr 24) and 0xFF) * (0.5f + 0.5f * rainLen[i])).toInt().coerceIn(0, 255)
            p.color = (a shl 24) or (color and 0x00FFFFFF)
            p.strokeWidth = min(w, h) * (0.008f + 0.006f * rainLen[i])
            c.drawLine(sx, sy, sx - len * 0.22f, sy + len, p)
        }
    }

    private fun drawSplashes(c: Canvas, p: Paint, phase: Float, w: Int, h: Int) {
        val groundY = h * 0.92f
        p.style = Paint.Style.STROKE
        p.strokeWidth = min(w, h) * 0.008f
        for (i in splashX.indices) {
            val prog = (phase * 1.6f + splashPhase[i]) % 1f
            if (prog > 0.6f) continue
            val k = prog / 0.6f
            val r = min(w, h) * (0.02f + 0.08f * k)
            val a = ((1f - k) * 0.5f * 255).toInt().coerceIn(0, 255)
            p.color = (a shl 24) or 0x9FC3E8
            c.drawCircle(splashX[i] * w, groundY, r, p)
        }
        p.style = Paint.Style.FILL
    }

    private fun drawSnow(c: Canvas, p: Paint, phase: Float, w: Int, h: Int) {
        for (i in snowX.indices) {
            val prog = (phase * (0.4f + snowSize[i] * 0.5f) + snowPhase[i]) % 1f
            val sway = sin((prog + snowPhase[i]) * 4 * Math.PI).toFloat() * w * 0.04f
            val sx = snowX[i] * w + sway
            val sy = h * 0.3f + prog * h * 0.68f
            val a = ((0.55f + 0.4f * snowSize[i]) * 255).toInt().coerceIn(0, 255)
            p.color = (a shl 24) or 0x00FFFFFF
            c.drawCircle(sx, sy, min(w, h) * (0.012f + 0.014f * snowSize[i]), p)
        }
    }

    private fun drawFog(c: Canvas, p: Paint, phase: Float, w: Int, h: Int) {
        for (i in 0 until 5) {
            val y = h * (0.3f + i * 0.12f)
            val dx = sin((phase + i * 0.2f) * 2 * Math.PI).toFloat() * w * 0.08f
            p.color = (((0.14f - 0.03f * (i % 2)) * 255).toInt() shl 24) or 0x00FFFFFF
            c.drawRoundRect(dx, y, w + dx, y + h * 0.08f, h * 0.04f, h * 0.04f, p)
        }
    }

    private fun drawMist(c: Canvas, p: Paint, phase: Float, w: Int, h: Int) {
        for (i in mistX.indices) {
            val drift = (phase * (0.3f + mistSize[i] * 0.3f) + mistPhase[i]) % 1.2f - 0.1f
            val x = drift * w
            val y = mistY[i] * h + sin((phase + mistPhase[i]) * 2 * Math.PI).toFloat() * h * 0.015f
            val r = min(w, h) * (0.12f + 0.10f * mistSize[i])
            p.shader = RadialGradient(x, y, r, 0x1FFFFFFF, 0x00FFFFFF, Shader.TileMode.CLAMP)
            c.drawCircle(x, y, r, p)
            p.shader = null
        }
    }

    private fun drawWind(c: Canvas, p: Paint, phase: Float, w: Int, h: Int, factor: Float) {
        val count = (3 + (windY.size - 3) * factor).toInt().coerceIn(3, windY.size)
        p.style = Paint.Style.STROKE
        p.strokeCap = Paint.Cap.ROUND
        p.strokeWidth = min(w, h) * 0.01f
        for (i in 0 until count) {
            val travel = (phase * (0.8f + windLen[i]) * (0.6f + factor) + windPhase[i]) % 1.25f - 0.15f
            val x = travel * w
            val y = windY[i] * h
            val len = w * (0.16f + 0.14f * windLen[i])
            val a = ((0.1f + 0.16f * windLen[i]) * factor * 255).toInt().coerceIn(0, 255)
            p.color = (a shl 24) or 0x00FFFFFF
            val path = Path().apply {
                moveTo(x, y)
                quadTo(x + len * 0.5f, y - h * 0.02f, x + len, y)
            }
            c.drawPath(path, p)
        }
        p.style = Paint.Style.FILL
    }

    private fun drawLeaves(c: Canvas, p: Paint, phase: Float, w: Int, h: Int, factor: Float) {
        val count = (1 + (leafY.size - 1) * factor).toInt().coerceIn(1, leafY.size)
        for (i in 0 until count) {
            val prog = (phase * (0.7f + leafSize[i]) * (0.7f + factor) + leafPhase[i]) % 1.3f - 0.15f
            val x = prog * w
            val y = leafY[i] * h + sin((prog + leafPhase[i]) * 4 * Math.PI).toFloat() * h * 0.06f
            val r = min(w, h) * 0.022f * leafSize[i]
            c.save()
            c.rotate((phase * 360f * 2f + leafPhase[i] * 360f), x, y)
            val leaf = Path().apply {
                moveTo(x, y - r)
                quadTo(x + r, y, x, y + r)
                quadTo(x - r, y, x, y - r)
                close()
            }
            val base = if (i % 2 == 0) 0xC98A3C else 0x8FA869
            p.color = ((0.75f * factor * 255).toInt() shl 24) or base
            c.drawPath(leaf, p)
            c.restore()
        }
    }

    private fun drawLightning(c: Canvas, p: Paint, phase: Float, w: Int, h: Int) {
        val intensity = when {
            phase < 0.05f -> 1f - phase / 0.05f
            phase in 0.10f..0.14f -> 1f - (phase - 0.10f) / 0.04f
            else -> 0f
        }
        if (intensity <= 0f) return
        p.color = ((0.16f * intensity * 255).toInt() shl 24) or 0x00FFFFFF
        c.drawRect(0f, 0f, w.toFloat(), h.toFloat(), p)

        val x = w * 0.5f
        val top = h * 0.32f
        val bolt = Path().apply {
            moveTo(x, top)
            lineTo(x - w * 0.06f, top + h * 0.16f)
            lineTo(x + w * 0.01f, top + h * 0.18f)
            lineTo(x - w * 0.07f, top + h * 0.42f)
            lineTo(x + w * 0.05f, top + h * 0.14f)
            lineTo(x - w * 0.01f, top + h * 0.13f)
            close()
        }
        p.color = ((intensity * 255).toInt() shl 24) or 0x00FFE57A
        c.drawPath(bolt, p)
        // Branch.
        p.style = Paint.Style.STROKE
        p.strokeWidth = min(w, h) * 0.012f
        p.color = ((intensity * 0.8f * 255).toInt() shl 24) or 0x00FFF3C0
        val branch = Path().apply {
            moveTo(x - w * 0.025f, top + h * 0.18f)
            lineTo(x - w * 0.12f, top + h * 0.30f)
            lineTo(x - w * 0.05f, top + h * 0.30f)
        }
        c.drawPath(branch, p)
        p.style = Paint.Style.FILL
    }

    private fun lighten(color: Int, amt: Float): Int {
        val a = (color ushr 24) and 0xFF
        val r = (color ushr 16) and 0xFF
        val g = (color ushr 8) and 0xFF
        val b = color and 0xFF
        val nr = (r + (255 - r) * amt).toInt().coerceIn(0, 255)
        val ng = (g + (255 - g) * amt).toInt().coerceIn(0, 255)
        val nb = (b + (255 - b) * amt).toInt().coerceIn(0, 255)
        return (a shl 24) or (nr shl 16) or (ng shl 8) or nb
    }

    private fun darken(color: Int, amt: Float): Int {
        val a = (color ushr 24) and 0xFF
        val r = ((color ushr 16) and 0xFF) * (1 - amt)
        val g = ((color ushr 8) and 0xFF) * (1 - amt)
        val b = (color and 0xFF) * (1 - amt)
        return (a shl 24) or (r.toInt() shl 16) or (g.toInt() shl 8) or b.toInt()
    }
}
