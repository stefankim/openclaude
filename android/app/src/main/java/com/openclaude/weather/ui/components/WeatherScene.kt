package com.openclaude.weather.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import com.openclaude.weather.domain.WeatherScene
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * A fully Compose-drawn, continuously animated weather scene. Used large on the Today
 * screen and small inside cards. No image assets — everything is procedural so it scales
 * crisply and themes with the weather.
 */
@Composable
fun WeatherSceneView(
    scene: WeatherScene,
    isDay: Boolean,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "weather")

    // Master clock 0..1 driving every periodic motion.
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "t"
    )

    // Slow clock for sun rotation / cloud drift.
    val slow by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(24000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "slow"
    )

    // Lightning flash pulses.
    val flash by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "flash"
    )

    // Stable random seeds for particles so they don't jump each recomposition.
    val rng = remember { Random(scene.ordinal * 31 + if (isDay) 1 else 0) }
    val rainSeeds = remember(scene) { List(70) { rng.nextFloat() to rng.nextFloat() } }
    val snowSeeds = remember(scene) { List(50) { Triple(rng.nextFloat(), rng.nextFloat(), rng.nextFloat()) } }
    val starSeeds = remember(scene) { List(40) { Triple(rng.nextFloat(), rng.nextFloat(), rng.nextFloat()) } }

    Canvas(modifier = modifier) {
        when (scene) {
            WeatherScene.CLEAR_DAY -> drawSun(slow, t)
            WeatherScene.CLEAR_NIGHT -> { drawStars(starSeeds, t); drawMoon() }
            WeatherScene.PARTLY_CLOUDY -> {
                if (isDay) drawSun(slow, t, scale = 0.7f, center = Offset(size.width * 0.72f, size.height * 0.34f))
                else { drawStars(starSeeds, t); drawMoon(Offset(size.width * 0.72f, size.height * 0.32f)) }
                drawCloud(slow, Offset(size.width * 0.40f, size.height * 0.52f), 1f, Color.White.copy(alpha = 0.95f))
            }
            WeatherScene.CLOUDY -> {
                drawCloud(slow, Offset(size.width * 0.36f, size.height * 0.42f), 1.1f, Color.White.copy(alpha = 0.85f))
                drawCloud(slow * 0.7f, Offset(size.width * 0.60f, size.height * 0.58f), 0.9f, Color(0xFFDDE3EA).copy(alpha = 0.9f))
            }
            WeatherScene.FOG -> drawFog(slow)
            WeatherScene.RAIN -> {
                drawCloud(slow, Offset(size.width * 0.5f, size.height * 0.34f), 1.1f, Color(0xFFB9C2CC))
                drawRain(rainSeeds, t)
            }
            WeatherScene.SNOW -> {
                drawCloud(slow, Offset(size.width * 0.5f, size.height * 0.32f), 1.1f, Color(0xFFD7DEE6))
                drawSnow(snowSeeds, t)
            }
            WeatherScene.THUNDERSTORM -> {
                drawCloud(slow, Offset(size.width * 0.5f, size.height * 0.32f), 1.2f, Color(0xFF7E8794))
                drawRain(rainSeeds, t, color = Color(0xCCBFD0E0))
                drawLightning(flash)
            }
        }
    }
}

private fun DrawScope.drawSun(
    slow: Float,
    t: Float,
    scale: Float = 1f,
    center: Offset = Offset(size.width * 0.5f, size.height * 0.4f)
) {
    val radius = size.minDimension * 0.13f * scale
    // Soft glow
    drawCircle(Color(0x33FFE08A), radius * 2.4f, center)
    drawCircle(Color(0x55FFD56B), radius * 1.7f, center)
    // Rotating rays
    rotate(degrees = slow * 360f, pivot = center) {
        val rayLen = radius * (0.9f + 0.12f * sin(t * 2 * PI).toFloat())
        for (i in 0 until 12) {
            val a = (i * 30f) * PI.toFloat() / 180f
            val start = Offset(center.x + cos(a) * radius * 1.35f, center.y + sin(a) * radius * 1.35f)
            val end = Offset(center.x + cos(a) * (radius * 1.35f + rayLen), center.y + sin(a) * (radius * 1.35f + rayLen))
            drawLine(Color(0xFFFFD25E), start, end, strokeWidth = radius * 0.16f)
        }
    }
    drawCircle(Color(0xFFFFC94D), radius, center)
    drawCircle(Color(0xFFFFE08A), radius * 0.82f, center)
}

private fun DrawScope.drawMoon(center: Offset = Offset.Unspecified) {
    val c = if (center == Offset.Unspecified) Offset(size.width * 0.6f, size.height * 0.32f) else center
    val r = size.minDimension * 0.11f
    drawCircle(Color(0x33FFFFFF), r * 1.8f, c)
    drawCircle(Color(0xFFF2F4F8), r, c)
    // Crescent shadow
    drawCircle(Color(0x00000000), r, c)
    drawCircle(
        color = Color(0x551B2A4A),
        radius = r,
        center = Offset(c.x + r * 0.45f, c.y - r * 0.2f)
    )
}

private fun DrawScope.drawStars(seeds: List<Triple<Float, Float, Float>>, t: Float) {
    seeds.forEach { (x, y, phase) ->
        val twinkle = 0.4f + 0.6f * (0.5f + 0.5f * sin((t + phase) * 2 * PI).toFloat())
        val r = size.minDimension * (0.004f + 0.006f * phase)
        drawCircle(
            color = Color.White.copy(alpha = twinkle),
            radius = r,
            center = Offset(x * size.width, y * size.height * 0.7f)
        )
    }
}

private fun DrawScope.drawCloud(drift: Float, center: Offset, scale: Float, color: Color) {
    val dx = (drift % 1f - 0.5f) * size.width * 0.12f
    val c = Offset(center.x + dx, center.y)
    val u = size.minDimension * 0.09f * scale
    drawCircle(color, u * 1.0f, Offset(c.x - u * 1.4f, c.y))
    drawCircle(color, u * 1.4f, Offset(c.x - u * 0.4f, c.y - u * 0.4f))
    drawCircle(color, u * 1.2f, Offset(c.x + u * 0.8f, c.y - u * 0.1f))
    drawCircle(color, u * 1.0f, Offset(c.x + u * 1.7f, c.y))
    drawRect(
        color = color,
        topLeft = Offset(c.x - u * 1.9f, c.y),
        size = Size(u * 3.8f, u * 1.1f)
    )
}

private fun DrawScope.drawRain(seeds: List<Pair<Float, Float>>, t: Float, color: Color = Color(0xCC9FC3E8)) {
    val len = size.height * 0.06f
    seeds.forEach { (x, phase) ->
        val prog = (t + phase) % 1f
        val sx = x * size.width
        val sy = size.height * 0.42f + prog * size.height * 0.6f
        drawLine(
            color = color,
            start = Offset(sx, sy),
            end = Offset(sx - len * 0.25f, sy + len),
            strokeWidth = size.minDimension * 0.006f
        )
    }
}

private fun DrawScope.drawSnow(seeds: List<Triple<Float, Float, Float>>, t: Float) {
    seeds.forEach { (x, phase, spd) ->
        val prog = (t * (0.5f + spd) + phase) % 1f
        val sway = sin((prog + phase) * 4 * PI).toFloat() * size.width * 0.03f
        val sx = x * size.width + sway
        val sy = size.height * 0.36f + prog * size.height * 0.64f
        drawCircle(
            color = Color.White.copy(alpha = 0.9f),
            radius = size.minDimension * (0.006f + 0.006f * spd),
            center = Offset(sx, sy)
        )
    }
}

private fun DrawScope.drawFog(slow: Float) {
    for (i in 0 until 5) {
        val y = size.height * (0.3f + i * 0.12f)
        val dx = sin((slow + i * 0.2f) * 2 * PI).toFloat() * size.width * 0.06f
        drawRect(
            color = Color.White.copy(alpha = 0.10f + 0.04f * (i % 2)),
            topLeft = Offset(dx, y),
            size = Size(size.width, size.height * 0.06f)
        )
    }
}

private fun DrawScope.drawLightning(flash: Float) {
    // Two short flashes per cycle.
    val intensity = when {
        flash < 0.06f -> 1f - flash / 0.06f
        flash in 0.5f..0.56f -> 1f - (flash - 0.5f) / 0.06f
        else -> 0f
    }
    if (intensity <= 0f) return
    drawRect(Color.White.copy(alpha = 0.18f * intensity), size = size)
    val bolt = Path().apply {
        val x = size.width * 0.5f
        moveTo(x, size.height * 0.36f)
        lineTo(x - size.width * 0.05f, size.height * 0.55f)
        lineTo(x + size.width * 0.02f, size.height * 0.55f)
        lineTo(x - size.width * 0.06f, size.height * 0.78f)
        lineTo(x + size.width * 0.04f, size.height * 0.52f)
        lineTo(x - size.width * 0.02f, size.height * 0.52f)
        close()
    }
    drawPath(bolt, Color(0xFFFFE57A).copy(alpha = intensity))
}
