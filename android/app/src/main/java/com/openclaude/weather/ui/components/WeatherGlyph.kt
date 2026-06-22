package com.openclaude.weather.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.openclaude.weather.domain.WeatherScene
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Colorful, filled weather glyphs drawn with Canvas — closer to the look of commercial
 * weather apps than flat monochrome Material icons. Used in hourly cells, daily rows and
 * anywhere a compact condition icon is needed.
 */
@Composable
fun WeatherGlyph(
    scene: WeatherScene,
    isDay: Boolean,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        when (scene) {
            WeatherScene.CLEAR_DAY -> sun(Offset(size.width * 0.5f, size.height * 0.5f), size.minDimension * 0.24f)
            WeatherScene.CLEAR_NIGHT -> moon()
            WeatherScene.PARTLY_CLOUDY -> {
                if (isDay) sun(Offset(size.width * 0.38f, size.height * 0.38f), size.minDimension * 0.18f)
                else moon(Offset(size.width * 0.4f, size.height * 0.38f), size.minDimension * 0.16f)
                cloud(Offset(size.width * 0.56f, size.height * 0.6f), size.minDimension * 0.30f, Color.White)
            }
            WeatherScene.CLOUDY -> {
                cloud(Offset(size.width * 0.4f, size.height * 0.42f), size.minDimension * 0.28f, Color(0xFFE6ECF2))
                cloud(Offset(size.width * 0.58f, size.height * 0.58f), size.minDimension * 0.32f, Color.White)
            }
            WeatherScene.FOG -> {
                cloud(Offset(size.width * 0.5f, size.height * 0.42f), size.minDimension * 0.30f, Color(0xFFD7DEE6))
                fogLines()
            }
            WeatherScene.RAIN -> {
                cloud(Offset(size.width * 0.5f, size.height * 0.4f), size.minDimension * 0.30f, Color(0xFFCAD3DD))
                rainDrops()
            }
            WeatherScene.SNOW -> {
                cloud(Offset(size.width * 0.5f, size.height * 0.4f), size.minDimension * 0.30f, Color(0xFFE6ECF2))
                snowDots()
            }
            WeatherScene.THUNDERSTORM -> {
                cloud(Offset(size.width * 0.5f, size.height * 0.4f), size.minDimension * 0.30f, Color(0xFF8A94A1))
                bolt()
            }
        }
    }
}

private fun DrawScope.sun(center: Offset, radius: Float) {
    // rays
    for (i in 0 until 8) {
        val a = (i * 45f) * PI.toFloat() / 180f
        val start = Offset(center.x + cos(a) * radius * 1.35f, center.y + sin(a) * radius * 1.35f)
        val end = Offset(center.x + cos(a) * radius * 1.95f, center.y + sin(a) * radius * 1.95f)
        drawLine(Color(0xFFFFC531), start, end, strokeWidth = radius * 0.22f, cap = StrokeCap.Round)
    }
    drawCircle(Color(0xFFFFB300), radius, center)
    drawCircle(Color(0xFFFFD54A), radius * 0.78f, center)
}

private fun DrawScope.moon(center: Offset = Offset(size.width * 0.52f, size.height * 0.45f), radius: Float = size.minDimension * 0.26f) {
    drawCircle(Color(0xFFF4D03F), radius, center)
    drawCircle(Color(0xFFFFF6D6), radius * 0.82f, center)
    // crescent shadow (background-ish tint)
    drawCircle(Color(0x33102040), radius, Offset(center.x + radius * 0.5f, center.y - radius * 0.25f))
}

private fun DrawScope.cloud(center: Offset, u: Float, color: Color) {
    val baseline = center.y + u * 0.5f
    val left = center.x - u * 1.15f
    val right = center.x + u * 1.15f
    // Rounded slab base for a soft, flat bottom.
    drawRoundRect(
        color = color,
        topLeft = Offset(left, center.y - u * 0.05f),
        size = Size(right - left, baseline - (center.y - u * 0.05f)),
        cornerRadius = CornerRadius(u * 0.3f, u * 0.3f)
    )
    // Puffs kept high so their bottoms stay within the body.
    drawCircle(color, u * 0.5f, Offset(center.x - u * 0.7f, center.y - u * 0.05f))
    drawCircle(color, u * 0.78f, Offset(center.x - u * 0.12f, center.y - u * 0.38f))
    drawCircle(color, u * 0.6f, Offset(center.x + u * 0.55f, center.y - u * 0.2f))
    drawCircle(color, u * 0.46f, Offset(center.x + u * 0.95f, center.y - u * 0.02f))
    // Soft highlight.
    drawCircle(Color.White.copy(alpha = 0.18f), u * 0.4f, Offset(center.x - u * 0.05f, center.y - u * 0.45f))
}

private fun DrawScope.rainDrops() {
    val color = Color(0xFF4FA3F7)
    val ys = size.height * 0.7f
    listOf(0.34f, 0.5f, 0.66f).forEachIndexed { i, fx ->
        val x = size.width * fx
        val drop = Path().apply {
            moveTo(x, ys)
            lineTo(x - size.width * 0.05f, ys + size.height * 0.13f)
            lineTo(x + size.width * 0.05f, ys + size.height * 0.13f)
            close()
        }
        drawPath(drop, color)
        drawCircle(color, size.width * 0.05f, Offset(x, ys + size.height * 0.12f))
    }
}

private fun DrawScope.snowDots() {
    val color = Color.White
    listOf(0.34f to 0.74f, 0.5f to 0.82f, 0.66f to 0.74f).forEach { (fx, fy) ->
        drawCircle(color, size.minDimension * 0.06f, Offset(size.width * fx, size.height * fy))
    }
}

private fun DrawScope.fogLines() {
    val color = Color(0xFFB4BDC7)
    listOf(0.62f, 0.74f, 0.86f).forEachIndexed { i, fy ->
        val inset = size.width * (0.22f + 0.04f * i)
        drawLine(
            color = color,
            start = Offset(inset, size.height * fy),
            end = Offset(size.width - inset, size.height * fy),
            strokeWidth = size.minDimension * 0.06f,
            cap = StrokeCap.Round
        )
    }
}

private fun DrawScope.bolt() {
    val x = size.width * 0.5f
    val bolt = Path().apply {
        moveTo(x + size.width * 0.04f, size.height * 0.6f)
        lineTo(x - size.width * 0.1f, size.height * 0.82f)
        lineTo(x, size.height * 0.82f)
        lineTo(x - size.width * 0.06f, size.height * 0.98f)
        lineTo(x + size.width * 0.14f, size.height * 0.74f)
        lineTo(x + size.width * 0.03f, size.height * 0.74f)
        close()
    }
    drawPath(bolt, Color(0xFFFFCB2E))
}
