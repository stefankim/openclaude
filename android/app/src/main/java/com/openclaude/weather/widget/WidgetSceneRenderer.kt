package com.openclaude.weather.widget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import com.openclaude.weather.domain.WeatherScene
import java.util.Random
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Renders one animation frame of a weather scene to a [Bitmap] using the native Canvas API,
 * so the home-screen widget can show a genuinely live animation by cycling [phase].
 */
object WidgetSceneRenderer {

    private val rng = Random(42)
    private val rainParticles = FloatArray(40) { rng.nextFloat() }
    private val rainPhase = FloatArray(40) { rng.nextFloat() }
    private val snowX = FloatArray(28) { rng.nextFloat() }
    private val snowPhase = FloatArray(28) { rng.nextFloat() }
    private val starX = FloatArray(24) { rng.nextFloat() }
    private val starY = FloatArray(24) { rng.nextFloat() }

    fun render(
        scene: WeatherScene,
        isDay: Boolean,
        phase: Float,
        widthPx: Int,
        heightPx: Int
    ): Bitmap {
        val w = widthPx.coerceAtLeast(1)
        val h = heightPx.coerceAtLeast(1)
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        drawBackground(canvas, paint, scene, isDay, w, h)

        when (scene) {
            WeatherScene.CLEAR_DAY -> drawSun(canvas, paint, phase, w, h)
            WeatherScene.CLEAR_NIGHT -> { drawStars(canvas, paint, phase, w, h); drawMoon(canvas, paint, w, h) }
            WeatherScene.PARTLY_CLOUDY -> {
                if (isDay) drawSun(canvas, paint, phase, w, h, 0.6f) else drawMoon(canvas, paint, w, h)
                drawCloud(canvas, paint, phase, w * 0.45f, h * 0.6f, w * 0.18f, 0xF2FFFFFF.toInt())
            }
            WeatherScene.CLOUDY -> {
                drawCloud(canvas, paint, phase, w * 0.38f, h * 0.45f, w * 0.20f, 0xE6FFFFFF.toInt())
                drawCloud(canvas, paint, phase * 0.7f, w * 0.62f, h * 0.62f, w * 0.16f, 0xE6DDE3EA.toInt())
            }
            WeatherScene.FOG -> drawFog(canvas, paint, phase, w, h)
            WeatherScene.RAIN -> {
                drawCloud(canvas, paint, phase, w * 0.5f, h * 0.34f, w * 0.20f, 0xFFB9C2CC.toInt())
                drawRain(canvas, paint, phase, w, h, 0xCC9FC3E8.toInt())
            }
            WeatherScene.SNOW -> {
                drawCloud(canvas, paint, phase, w * 0.5f, h * 0.32f, w * 0.20f, 0xFFD7DEE6.toInt())
                drawSnow(canvas, paint, phase, w, h)
            }
            WeatherScene.THUNDERSTORM -> {
                drawCloud(canvas, paint, phase, w * 0.5f, h * 0.32f, w * 0.22f, 0xFF7E8794.toInt())
                drawRain(canvas, paint, phase, w, h, 0xCCBFD0E0.toInt())
                drawLightning(canvas, paint, phase, w, h)
            }
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
        // Rounded rect background.
        val r = min(w, h) * 0.12f
        c.drawRoundRect(0f, 0f, w.toFloat(), h.toFloat(), r, r, p)
        p.shader = null
    }

    private fun drawSun(c: Canvas, p: Paint, phase: Float, w: Int, h: Int, scale: Float = 1f) {
        val cx = w * 0.5f
        val cy = h * 0.42f
        val radius = min(w, h) * 0.16f * scale
        p.color = 0x55FFD56B
        c.drawCircle(cx, cy, radius * 1.7f, p)
        p.color = 0xFFFFD25E.toInt()
        p.strokeWidth = radius * 0.16f
        val rot = phase * 2 * Math.PI
        for (i in 0 until 12) {
            val a = i * Math.PI / 6 + rot
            val r1 = radius * 1.35f
            val r2 = radius * 2.2f
            c.drawLine(
                (cx + cos(a) * r1).toFloat(), (cy + sin(a) * r1).toFloat(),
                (cx + cos(a) * r2).toFloat(), (cy + sin(a) * r2).toFloat(), p
            )
        }
        p.color = 0xFFFFC94D.toInt()
        c.drawCircle(cx, cy, radius, p)
        p.color = 0xFFFFE08A.toInt()
        c.drawCircle(cx, cy, radius * 0.82f, p)
    }

    private fun drawMoon(c: Canvas, p: Paint, w: Int, h: Int) {
        val cx = w * 0.62f
        val cy = h * 0.36f
        val r = min(w, h) * 0.14f
        p.color = 0x33FFFFFF
        c.drawCircle(cx, cy, r * 1.6f, p)
        p.color = 0xFFF2F4F8.toInt()
        c.drawCircle(cx, cy, r, p)
        p.color = 0x551B2A4A
        c.drawCircle(cx + r * 0.45f, cy - r * 0.2f, r, p)
    }

    private fun drawStars(c: Canvas, p: Paint, phase: Float, w: Int, h: Int) {
        for (i in starX.indices) {
            val tw = 0.4f + 0.6f * (0.5f + 0.5f * sin((phase + i * 0.13f) * 2 * Math.PI).toFloat())
            p.color = ((tw * 255).toInt() shl 24) or 0x00FFFFFF
            c.drawCircle(starX[i] * w, starY[i] * h * 0.7f, min(w, h) * 0.012f, p)
        }
    }

    private fun drawCloud(c: Canvas, p: Paint, phase: Float, cx: Float, cy: Float, u: Float, color: Int) {
        val dx = (phase % 1f - 0.5f) * u * 0.5f
        p.color = color
        c.drawCircle(cx - u * 0.8f + dx, cy, u * 0.6f, p)
        c.drawCircle(cx - u * 0.2f + dx, cy - u * 0.3f, u * 0.85f, p)
        c.drawCircle(cx + u * 0.5f + dx, cy - u * 0.1f, u * 0.7f, p)
        c.drawCircle(cx + u * 1.0f + dx, cy, u * 0.55f, p)
        c.drawRect(cx - u * 1.1f + dx, cy, cx + u * 1.1f + dx, cy + u * 0.6f, p)
    }

    private fun drawRain(c: Canvas, p: Paint, phase: Float, w: Int, h: Int, color: Int) {
        p.color = color
        p.strokeWidth = min(w, h) * 0.012f
        val len = h * 0.12f
        for (i in rainParticles.indices) {
            val prog = (phase + rainPhase[i]) % 1f
            val sx = rainParticles[i] * w
            val sy = h * 0.4f + prog * h * 0.6f
            c.drawLine(sx, sy, sx - len * 0.25f, sy + len, p)
        }
    }

    private fun drawSnow(c: Canvas, p: Paint, phase: Float, w: Int, h: Int) {
        p.color = 0xE6FFFFFF.toInt()
        for (i in snowX.indices) {
            val prog = (phase + snowPhase[i]) % 1f
            val sway = sin((prog + snowPhase[i]) * 4 * Math.PI).toFloat() * w * 0.04f
            val sx = snowX[i] * w + sway
            val sy = h * 0.34f + prog * h * 0.66f
            c.drawCircle(sx, sy, min(w, h) * 0.018f, p)
        }
    }

    private fun drawFog(c: Canvas, p: Paint, phase: Float, w: Int, h: Int) {
        for (i in 0 until 4) {
            val y = h * (0.35f + i * 0.14f)
            val dx = sin((phase + i * 0.2f) * 2 * Math.PI).toFloat() * w * 0.08f
            p.color = (((0.16f - 0.03f * (i % 2)) * 255).toInt() shl 24) or 0x00FFFFFF
            c.drawRect(dx, y, w + dx, y + h * 0.08f, p)
        }
    }

    private fun drawLightning(c: Canvas, p: Paint, phase: Float, w: Int, h: Int) {
        val intensity = when {
            phase < 0.08f -> 1f - phase / 0.08f
            phase in 0.5f..0.58f -> 1f - (phase - 0.5f) / 0.08f
            else -> 0f
        }
        if (intensity <= 0f) return
        p.color = ((0.16f * intensity * 255).toInt() shl 24) or 0x00FFFFFF
        c.drawRect(0f, 0f, w.toFloat(), h.toFloat(), p)
        val x = w * 0.5f
        val bolt = Path().apply {
            moveTo(x, h * 0.34f)
            lineTo(x - w * 0.05f, h * 0.55f)
            lineTo(x + w * 0.02f, h * 0.55f)
            lineTo(x - w * 0.06f, h * 0.8f)
            lineTo(x + w * 0.05f, h * 0.52f)
            lineTo(x - w * 0.02f, h * 0.52f)
            close()
        }
        p.color = ((intensity * 255).toInt() shl 24) or 0x00FFE57A
        c.drawPath(bolt, p)
    }
}
