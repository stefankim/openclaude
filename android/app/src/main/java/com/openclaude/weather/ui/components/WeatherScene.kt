package com.openclaude.weather.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
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
    modifier: Modifier = Modifier,
    windKmh: Double = 0.0
) {
    val transition = rememberInfiniteTransition(label = "weather")

    // Master clock 0..1 driving fast periodic motion (rain/snow particles).
    val t by transition.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(6000, easing = LinearEasing), RepeatMode.Restart),
        label = "t"
    )
    // Slow clock for sun rotation / cloud drift.
    val slow by transition.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(26000, easing = LinearEasing), RepeatMode.Restart),
        label = "slow"
    )
    // Gentle breathing clock for glow pulsing and cloud bob.
    val pulse by transition.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(4200, easing = LinearEasing), RepeatMode.Restart),
        label = "pulse"
    )
    // Lightning flash cycle.
    val flash by transition.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(3200, easing = LinearEasing), RepeatMode.Restart),
        label = "flash"
    )

    // Stable random seeds for particles so they don't jump each recomposition.
    val rng = remember(scene, isDay) { Random(scene.ordinal * 31 + if (isDay) 1 else 0) }
    // rain: x, phase, speed, length
    val rainSeeds = remember(scene, isDay) {
        List(80) { listOf(rng.nextFloat(), rng.nextFloat(), 0.7f + rng.nextFloat() * 0.6f, 0.7f + rng.nextFloat() * 0.7f) }
    }
    val splashSeeds = remember(scene, isDay) { List(7) { rng.nextFloat() to rng.nextFloat() } }
    // snow: x, phase, speed, size
    val snowSeeds = remember(scene, isDay) {
        List(60) { listOf(rng.nextFloat(), rng.nextFloat(), 0.5f + rng.nextFloat(), 0.5f + rng.nextFloat()) }
    }
    val starSeeds = remember(scene, isDay) { List(46) { Triple(rng.nextFloat(), rng.nextFloat(), rng.nextFloat()) } }
    // wind streaks: y, phase, length, speed
    val windSeeds = remember(scene, isDay) {
        List(9) { listOf(0.12f + rng.nextFloat() * 0.7f, rng.nextFloat(), 0.5f + rng.nextFloat() * 0.5f, 0.7f + rng.nextFloat() * 0.6f) }
    }
    // leaves: y, phase, spin, size, speed
    val leafSeeds = remember(scene, isDay) {
        List(6) { listOf(0.15f + rng.nextFloat() * 0.7f, rng.nextFloat(), rng.nextFloat(), 0.6f + rng.nextFloat() * 0.8f, 0.7f + rng.nextFloat() * 0.6f) }
    }
    // mist blobs (fog): x, y, phase, size
    val mistSeeds = remember(scene, isDay) {
        List(16) { listOf(rng.nextFloat(), 0.25f + rng.nextFloat() * 0.6f, rng.nextFloat(), 0.6f + rng.nextFloat() * 0.8f) }
    }

    // Wind only animates when it's actually breezy.
    val windFactor = ((windKmh - 18.0) / 40.0).coerceIn(0.0, 1.0).toFloat()

    Canvas(modifier = modifier) {
        when (scene) {
            WeatherScene.CLEAR_DAY -> drawSun(slow, pulse)
            WeatherScene.CLEAR_NIGHT -> {
                drawStars(starSeeds, t)
                drawShootingStar(t)
                drawMoon()
            }
            WeatherScene.PARTLY_CLOUDY -> {
                if (isDay) drawSun(slow, pulse, scale = 0.66f, center = Offset(size.width * 0.74f, size.height * 0.32f))
                else { drawStars(starSeeds, t); drawMoon(Offset(size.width * 0.74f, size.height * 0.30f)) }
                drawCloud(slow, Offset(size.width * 0.40f, size.height * 0.56f), 1f, Color.White, pulse)
            }
            WeatherScene.CLOUDY -> {
                drawCloud(slow * 0.6f, Offset(size.width * 0.30f, size.height * 0.40f), 1.25f, Color(0xFFCED6DF), pulse, depth = true)
                drawCloud(slow, Offset(size.width * 0.58f, size.height * 0.55f), 1.05f, Color.White, pulse)
            }
            WeatherScene.FOG -> {
                drawCloud(slow, Offset(size.width * 0.5f, size.height * 0.34f), 1.0f, Color(0xFFDDE3EA), pulse)
                drawFog(slow)
                drawMist(mistSeeds, t)
            }
            WeatherScene.RAIN -> {
                drawCloud(slow, Offset(size.width * 0.5f, size.height * 0.32f), 1.15f, Color(0xFFB9C2CC), pulse, depth = true)
                drawRain(rainSeeds, t)
                drawSplashes(splashSeeds, t)
            }
            WeatherScene.SNOW -> {
                drawCloud(slow, Offset(size.width * 0.5f, size.height * 0.30f), 1.15f, Color(0xFFE0E6EC), pulse, depth = true)
                drawSnow(snowSeeds, t)
            }
            WeatherScene.THUNDERSTORM -> {
                drawCloud(slow, Offset(size.width * 0.5f, size.height * 0.30f), 1.25f, Color(0xFF7E8794), pulse, depth = true)
                drawRain(rainSeeds, t, color = Color(0xCCBFD0E0))
                drawSplashes(splashSeeds, t)
                drawLightning(flash)
            }
        }

        // Wind streaks + tumbling leaves overlay any scene when it's breezy.
        if (windFactor > 0f) {
            drawWind(windSeeds, t, windFactor)
            drawLeaves(leafSeeds, t, windFactor)
        }
    }
}

// ---------------------------------------------------------------------------------------

private fun DrawScope.drawSun(
    slow: Float,
    pulse: Float,
    scale: Float = 1f,
    center: Offset = Offset(size.width * 0.5f, size.height * 0.42f)
) {
    val radius = size.minDimension * 0.135f * scale
    val breathe = 1f + 0.06f * sin(pulse * 2 * PI).toFloat()

    // Soft radial glow.
    drawCircle(
        brush = Brush.radialGradient(
            colorStops = arrayOf(
                0f to Color(0x66FFE08A),
                0.5f to Color(0x33FFD56B),
                1f to Color(0x00FFD56B)
            ),
            center = center,
            radius = radius * 3.2f * breathe
        ),
        radius = radius * 3.2f * breathe,
        center = center
    )

    // Rotating rays with a gentle length pulse.
    rotate(degrees = slow * 360f, pivot = center) {
        val rayLen = radius * (0.85f + 0.18f * sin(pulse * 2 * PI).toFloat())
        for (i in 0 until 12) {
            val a = (i * 30f) * PI.toFloat() / 180f
            val r1 = radius * 1.4f
            val start = Offset(center.x + cos(a) * r1, center.y + sin(a) * r1)
            val end = Offset(center.x + cos(a) * (r1 + rayLen), center.y + sin(a) * (r1 + rayLen))
            drawLine(Color(0xFFFFD25E), start, end, strokeWidth = radius * 0.16f, cap = StrokeCap.Round)
        }
    }

    // Disc with a soft inner gradient.
    drawCircle(
        brush = Brush.radialGradient(
            colorStops = arrayOf(0f to Color(0xFFFFE9A6), 0.7f to Color(0xFFFFC94D), 1f to Color(0xFFFFB300)),
            center = Offset(center.x - radius * 0.25f, center.y - radius * 0.25f),
            radius = radius * 1.3f
        ),
        radius = radius,
        center = center
    )
}

private fun DrawScope.drawMoon(center: Offset = Offset(size.width * 0.6f, size.height * 0.32f)) {
    val r = size.minDimension * 0.12f
    drawCircle(
        brush = Brush.radialGradient(
            colorStops = arrayOf(0f to Color(0x55FFFFFF), 1f to Color(0x00FFFFFF)),
            center = center, radius = r * 2.4f
        ),
        radius = r * 2.4f, center = center
    )
    drawCircle(Color(0xFFF2F4F8), r, center)
    // Crescent shadow.
    drawCircle(Color(0x66152138), r, Offset(center.x + r * 0.5f, center.y - r * 0.22f))
    // A couple of craters for character.
    drawCircle(Color(0x22152138), r * 0.16f, Offset(center.x - r * 0.3f, center.y + r * 0.1f))
    drawCircle(Color(0x1A152138), r * 0.1f, Offset(center.x - r * 0.05f, center.y + r * 0.4f))
}

private fun DrawScope.drawStars(seeds: List<Triple<Float, Float, Float>>, t: Float) {
    seeds.forEach { (x, y, phase) ->
        val twinkle = 0.35f + 0.65f * (0.5f + 0.5f * sin((t + phase) * 6 * PI).toFloat())
        val r = size.minDimension * (0.004f + 0.006f * phase)
        drawCircle(Color.White.copy(alpha = twinkle), r, Offset(x * size.width, y * size.height * 0.72f))
    }
}

private fun DrawScope.drawShootingStar(t: Float) {
    // Streaks across once per master cycle, briefly.
    val window = 0.12f
    if (t > window) return
    val k = t / window
    val sx = (0.15f + 0.55f * k) * size.width
    val sy = (0.12f + 0.25f * k) * size.height
    val tail = size.minDimension * 0.18f
    val alpha = (1f - k)
    drawLine(
        brush = Brush.linearGradient(
            colors = listOf(Color.White.copy(alpha = 0f), Color.White.copy(alpha = alpha)),
            start = Offset(sx - tail, sy - tail * 0.5f),
            end = Offset(sx, sy)
        ),
        start = Offset(sx - tail, sy - tail * 0.5f),
        end = Offset(sx, sy),
        strokeWidth = size.minDimension * 0.01f,
        cap = StrokeCap.Round
    )
    drawCircle(Color.White.copy(alpha = alpha), size.minDimension * 0.012f, Offset(sx, sy))
}

private fun DrawScope.drawCloud(
    driftPhase: Float,
    center: Offset,
    scale: Float,
    color: Color,
    pulse: Float,
    depth: Boolean = false
) {
    val dx = ((driftPhase % 1f) - 0.5f) * size.width * 0.14f
    val bob = sin((driftPhase + pulse) * 2 * PI).toFloat() * size.minDimension * 0.008f
    val c = Offset(center.x + dx, center.y + bob)
    val u = size.minDimension * 0.09f * scale
    val left = c.x - u * 2.1f
    val right = c.x + u * 2.1f
    val baseline = c.y + u

    // Soft drop shadow under the cloud for depth.
    if (depth) {
        drawOval(
            color = Color(0x22000000),
            topLeft = Offset(left + u * 0.2f, baseline - u * 0.1f),
            size = Size((right - left) - u * 0.4f, u * 0.5f)
        )
    }

    // Body: rounded slab with a soft top→bottom gradient for volume.
    val bodyTop = c.y + u * 0.1f
    drawRoundRect(
        brush = Brush.verticalGradient(
            colors = listOf(lighten(color, 0.10f), color, darken(color, 0.10f)),
            startY = bodyTop - u, endY = baseline
        ),
        topLeft = Offset(left, bodyTop),
        size = Size(right - left, baseline - bodyTop),
        cornerRadius = CornerRadius(u * 0.55f, u * 0.55f)
    )

    // Fluffy puffs with soft (radial-gradient) edges.
    val puffs = listOf(
        Triple(-1.45f, 0.05f, 0.80f),
        Triple(-0.60f, -0.45f, 1.08f),
        Triple(0.30f, -0.64f, 1.24f),
        Triple(1.15f, -0.30f, 0.98f),
        Triple(1.80f, 0.05f, 0.74f)
    )
    puffs.forEach { (fx, fy, fr) ->
        softPuff(Offset(c.x + u * fx, c.y + u * fy), u * fr, color)
    }

    // Top highlight sheen.
    drawCircle(Color.White.copy(alpha = 0.20f), u * 0.8f, Offset(c.x - u * 0.1f, c.y - u * 0.78f))
}

/** A circle with an opaque core fading to transparent at the rim — gives clouds soft edges. */
private fun DrawScope.softPuff(center: Offset, radius: Float, color: Color) {
    drawCircle(
        brush = Brush.radialGradient(
            colorStops = arrayOf(0f to color, 0.78f to color, 1f to color.copy(alpha = 0f)),
            center = center, radius = radius
        ),
        radius = radius,
        center = center
    )
}

private fun DrawScope.drawRain(seeds: List<List<Float>>, t: Float, color: Color = Color(0xCC9FC3E8)) {
    seeds.forEach { s ->
        val x = s[0]; val phase = s[1]; val speed = s[2]; val lenF = s[3]
        val prog = (t * speed + phase) % 1f
        val len = size.height * 0.07f * lenF
        val sx = x * size.width
        val sy = size.height * 0.40f + prog * size.height * 0.62f
        // depth: closer drops (bigger lenF) are brighter/thicker
        drawLine(
            color = color.copy(alpha = (color.alpha * (0.5f + 0.5f * lenF)).coerceIn(0f, 1f)),
            start = Offset(sx, sy),
            end = Offset(sx - len * 0.22f, sy + len),
            strokeWidth = size.minDimension * (0.004f + 0.004f * lenF),
            cap = StrokeCap.Round
        )
    }
}

private fun DrawScope.drawSplashes(seeds: List<Pair<Float, Float>>, t: Float) {
    val groundY = size.height * 0.94f
    seeds.forEach { (x, phase) ->
        val prog = (t * 1.6f + phase) % 1f
        if (prog > 0.6f) return@forEach
        val k = prog / 0.6f
        val r = size.minDimension * (0.01f + 0.05f * k)
        val alpha = (1f - k) * 0.5f
        drawCircle(
            color = Color(0xFF9FC3E8).copy(alpha = alpha),
            radius = r,
            center = Offset(x * size.width, groundY),
            style = Stroke(width = size.minDimension * 0.006f)
        )
    }
}

private fun DrawScope.drawSnow(seeds: List<List<Float>>, t: Float) {
    seeds.forEach { s ->
        val x = s[0]; val phase = s[1]; val spd = s[2]; val sizeF = s[3]
        val prog = (t * (0.4f + spd * 0.5f) + phase) % 1f
        val sway = sin((prog + phase) * 4 * PI).toFloat() * size.width * 0.035f
        val sx = x * size.width + sway
        val sy = size.height * 0.32f + prog * size.height * 0.66f
        val r = size.minDimension * (0.006f + 0.008f * sizeF)
        drawCircle(Color.White.copy(alpha = 0.55f + 0.4f * sizeF), r, Offset(sx, sy))
    }
}

private fun DrawScope.drawFog(slow: Float) {
    for (i in 0 until 6) {
        val y = size.height * (0.28f + i * 0.11f)
        val dx = sin((slow + i * 0.18f) * 2 * PI).toFloat() * size.width * 0.08f
        val alpha = 0.10f + 0.05f * (i % 2)
        drawRoundRect(
            brush = Brush.horizontalGradient(
                listOf(Color.White.copy(alpha = 0f), Color.White.copy(alpha = alpha), Color.White.copy(alpha = 0f))
            ),
            topLeft = Offset(dx, y),
            size = Size(size.width, size.height * 0.07f),
            cornerRadius = CornerRadius(size.height * 0.04f, size.height * 0.04f)
        )
    }
}

private fun DrawScope.drawLightning(flash: Float) {
    val intensity = when {
        flash < 0.05f -> 1f - flash / 0.05f
        flash in 0.10f..0.14f -> 1f - (flash - 0.10f) / 0.04f // double-flash flicker
        else -> 0f
    }
    if (intensity <= 0f) return

    // Whole-scene flash + afterglow.
    drawRect(Color.White.copy(alpha = 0.16f * intensity), size = size)

    val x = size.width * 0.5f
    val top = size.height * 0.34f
    val main = Path().apply {
        moveTo(x, top)
        lineTo(x - size.width * 0.06f, top + size.height * 0.16f)
        lineTo(x + size.width * 0.01f, top + size.height * 0.18f)
        lineTo(x - size.width * 0.07f, top + size.height * 0.40f)
        lineTo(x + size.width * 0.05f, top + size.height * 0.14f)
        lineTo(x - size.width * 0.01f, top + size.height * 0.13f)
        close()
    }
    // Branch.
    val branch = Path().apply {
        moveTo(x - size.width * 0.025f, top + size.height * 0.18f)
        lineTo(x - size.width * 0.12f, top + size.height * 0.30f)
        lineTo(x - size.width * 0.05f, top + size.height * 0.30f)
    }
    val glow = Color(0xFFFFE57A).copy(alpha = intensity)
    drawPath(branch, Color(0xFFFFF3C0).copy(alpha = intensity * 0.8f), style = Stroke(width = size.minDimension * 0.012f, cap = StrokeCap.Round))
    drawPath(main, glow)
    // Bright core.
    drawPath(main, Color.White.copy(alpha = intensity * 0.6f), style = Stroke(width = size.minDimension * 0.01f))
}

private fun DrawScope.drawWind(seeds: List<List<Float>>, t: Float, factor: Float) {
    val count = (3 + (seeds.size - 3) * factor).toInt().coerceIn(3, seeds.size)
    for (i in 0 until count) {
        val s = seeds[i]
        val y = s[0] * size.height
        val phase = s[1]; val lenF = s[2]; val speed = s[3]
        val travel = (t * (0.6f + speed) * (0.6f + factor) + phase) % 1.25f - 0.15f
        val x = travel * size.width
        val len = size.width * (0.16f + 0.14f * lenF)
        val dip = size.height * 0.02f
        val path = Path().apply {
            moveTo(x, y)
            quadraticBezierTo(x + len * 0.5f, y - dip, x + len, y)
        }
        drawPath(
            path,
            Color.White.copy(alpha = (0.10f + 0.16f * lenF) * factor),
            style = Stroke(width = size.minDimension * 0.01f, cap = StrokeCap.Round)
        )
    }
}

private fun DrawScope.drawLeaves(seeds: List<List<Float>>, t: Float, factor: Float) {
    val count = (1 + (seeds.size - 1) * factor).toInt().coerceIn(1, seeds.size)
    for (i in 0 until count) {
        val s = seeds[i]
        val baseY = s[0]; val phase = s[1]; val spin = s[2]; val sizeF = s[3]; val speed = s[4]
        val prog = (t * (0.7f + speed) * (0.7f + factor) + phase) % 1.3f - 0.15f
        val x = prog * size.width
        val y = baseY * size.height + sin((prog + phase) * 4 * PI).toFloat() * size.height * 0.06f
        val r = size.minDimension * 0.018f * sizeF
        rotate(degrees = (t * 360f * (1f + spin) + phase * 360f), pivot = Offset(x, y)) {
            val leaf = Path().apply {
                moveTo(x, y - r)
                quadraticBezierTo(x + r, y, x, y + r)
                quadraticBezierTo(x - r, y, x, y - r)
                close()
            }
            // Autumn-ish tones.
            val col = if (i % 2 == 0) Color(0xFFC98A3C) else Color(0xFF8FA869)
            drawPath(leaf, col.copy(alpha = 0.75f * factor))
        }
    }
}

private fun DrawScope.drawMist(seeds: List<List<Float>>, t: Float) {
    seeds.forEach { s ->
        val x0 = s[0]; val y0 = s[1]; val phase = s[2]; val sizeF = s[3]
        val drift = ((t * (0.3f + sizeF * 0.3f) + phase) % 1.2f - 0.1f)
        val x = drift * size.width
        val y = y0 * size.height + sin((t + phase) * 2 * PI).toFloat() * size.height * 0.015f
        val r = size.minDimension * (0.12f + 0.10f * sizeF)
        drawCircle(
            brush = Brush.radialGradient(
                colorStops = arrayOf(0f to Color.White.copy(alpha = 0.12f), 1f to Color.White.copy(alpha = 0f)),
                center = Offset(x, y), radius = r
            ),
            radius = r,
            center = Offset(x, y)
        )
    }
}

// ---- small colour helpers ----

private fun lighten(c: Color, amount: Float): Color = Color(
    red = (c.red + (1f - c.red) * amount).coerceIn(0f, 1f),
    green = (c.green + (1f - c.green) * amount).coerceIn(0f, 1f),
    blue = (c.blue + (1f - c.blue) * amount).coerceIn(0f, 1f),
    alpha = c.alpha
)

private fun darken(c: Color, amount: Float): Color = Color(
    red = (c.red * (1f - amount)).coerceIn(0f, 1f),
    green = (c.green * (1f - amount)).coerceIn(0f, 1f),
    blue = (c.blue * (1f - amount)).coerceIn(0f, 1f),
    alpha = c.alpha
)
