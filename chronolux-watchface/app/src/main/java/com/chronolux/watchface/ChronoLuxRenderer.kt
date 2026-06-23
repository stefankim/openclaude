package com.chronolux.watchface

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Rect
import android.graphics.Shader
import android.graphics.Typeface
import android.view.SurfaceHolder
import androidx.wear.watchface.CanvasType
import androidx.wear.watchface.ComplicationSlotsManager
import androidx.wear.watchface.DrawMode
import androidx.wear.watchface.Renderer
import androidx.wear.watchface.WatchState
import androidx.wear.watchface.style.CurrentUserStyleRepository
import androidx.wear.watchface.style.UserStyle
import androidx.wear.watchface.style.UserStyleSetting
import com.chronolux.watchface.style.ColorTheme
import com.chronolux.watchface.style.LayoutMode
import com.chronolux.watchface.style.StyleIds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

private const val FRAME_PERIOD_MS_INTERACTIVE = 16L
private const val TWO_PI = 2.0 * Math.PI

/**
 * Canvas renderer for ChronoLux. Re-reads the user style reactively and keeps
 * all Paint objects cached, only rebuilding when the style actually changes.
 */
class ChronoLuxRenderer(
    private val context: Context,
    surfaceHolder: SurfaceHolder,
    watchState: WatchState,
    private val complicationSlotsManager: ComplicationSlotsManager,
    currentUserStyleRepository: CurrentUserStyleRepository
) : Renderer.CanvasRenderer2<ChronoLuxRenderer.ChronoLuxSharedAssets>(
    surfaceHolder,
    currentUserStyleRepository,
    watchState,
    CanvasType.HARDWARE,
    FRAME_PERIOD_MS_INTERACTIVE,
    clearWithBackgroundTintBeforeRenderingHighlightLayer = false
) {

    class ChronoLuxSharedAssets : SharedAssets {
        override fun onDestroy() {}
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private var theme = ColorTheme.MIDNIGHT_GOLD
    private var layoutMode = LayoutMode.HYBRID
    private var showTicks = true

    private var backgroundShader: RadialGradient? = null
    private var lastShaderSize = -1

    private val backgroundPaint = Paint()

    private val hourHandPaint = Paint().apply {
        isAntiAlias = true
        strokeCap = Paint.Cap.ROUND
        style = Paint.Style.STROKE
    }
    private val minuteHandPaint = Paint().apply {
        isAntiAlias = true
        strokeCap = Paint.Cap.ROUND
        style = Paint.Style.STROKE
    }
    private val secondHandPaint = Paint().apply {
        isAntiAlias = true
        strokeCap = Paint.Cap.ROUND
        style = Paint.Style.STROKE
    }
    private val tickPaint = Paint().apply {
        isAntiAlias = true
        strokeCap = Paint.Cap.BUTT
        style = Paint.Style.STROKE
    }
    private val centerDotPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
    }
    private val digitalPaint = Paint().apply {
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
    }
    private val datePaint = Paint().apply {
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
        typeface = Typeface.SANS_SERIF
    }

    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())
    private val dateFormatter = DateTimeFormatter.ofPattern("EEE d MMM", Locale.getDefault())

    init {
        scope.launch {
            currentUserStyleRepository.userStyle.collect { style -> applyStyle(style) }
        }
        applyStyle(currentUserStyleRepository.userStyle.value)
    }

    private fun applyStyle(style: UserStyle) {
        style.forEach { (setting, option) ->
            when (setting.id.value) {
                StyleIds.COLOR_THEME ->
                    theme = ColorTheme.fromId(option.id.toString())
                StyleIds.LAYOUT_MODE ->
                    layoutMode = LayoutMode.fromId(option.id.toString())
                StyleIds.SHOW_TICKS ->
                    showTicks = (option as UserStyleSetting.BooleanUserStyleSetting.BooleanOption).value
            }
        }
        // Force the gradient to rebuild with the new palette on next frame.
        lastShaderSize = -1
        updatePaints()
    }

    private fun updatePaints() {
        hourHandPaint.color = theme.accent
        minuteHandPaint.color = theme.accent
        secondHandPaint.color = theme.secondary
        tickPaint.color = theme.secondary
        centerDotPaint.color = theme.accent
        digitalPaint.color = theme.accent
        datePaint.color = theme.secondary
    }

    override suspend fun createSharedAssets(): ChronoLuxSharedAssets = ChronoLuxSharedAssets()

    override fun render(
        canvas: Canvas,
        bounds: Rect,
        zonedDateTime: ZonedDateTime,
        sharedAssets: ChronoLuxSharedAssets
    ) {
        val isAmbient = renderParameters.drawMode == DrawMode.AMBIENT

        drawBackground(canvas, bounds, isAmbient)

        if (showTicks && !isAmbient) {
            drawTicks(canvas, bounds)
        }

        when (layoutMode) {
            LayoutMode.ANALOG -> drawAnalog(canvas, bounds, zonedDateTime, isAmbient)
            LayoutMode.DIGITAL -> drawDigital(canvas, bounds, zonedDateTime, isAmbient, centered = true)
            LayoutMode.HYBRID -> {
                drawDigital(canvas, bounds, zonedDateTime, isAmbient, centered = false)
                drawAnalog(canvas, bounds, zonedDateTime, isAmbient)
            }
        }

        if (!isAmbient) {
            for ((_, slot) in complicationSlotsManager.complicationSlots) {
                if (slot.enabled) {
                    slot.render(canvas, zonedDateTime, renderParameters)
                }
            }
        }
    }

    override fun renderHighlightLayer(
        canvas: Canvas,
        bounds: Rect,
        zonedDateTime: ZonedDateTime,
        sharedAssets: ChronoLuxSharedAssets
    ) {
        canvas.drawColor(renderParameters.highlightLayer!!.backgroundTint)
        for ((_, slot) in complicationSlotsManager.complicationSlots) {
            if (slot.enabled) {
                slot.renderHighlightLayer(canvas, zonedDateTime, renderParameters)
            }
        }
    }

    private fun drawBackground(canvas: Canvas, bounds: Rect, isAmbient: Boolean) {
        if (isAmbient) {
            // Pure black in ambient mode: saves power on AMOLED and avoids burn-in.
            canvas.drawColor(Color.BLACK)
            return
        }
        if (lastShaderSize != bounds.width()) {
            backgroundShader = RadialGradient(
                bounds.exactCenterX(),
                bounds.exactCenterY(),
                bounds.width() / 1.6f,
                theme.backgroundInner,
                theme.backgroundOuter,
                Shader.TileMode.CLAMP
            )
            backgroundPaint.shader = backgroundShader
            lastShaderSize = bounds.width()
        }
        canvas.drawRect(bounds, backgroundPaint)
    }

    private fun drawTicks(canvas: Canvas, bounds: Rect) {
        val centerX = bounds.exactCenterX()
        val centerY = bounds.exactCenterY()
        val outerRadius = min(centerX, centerY)

        for (tick in 0 until 60) {
            val isMajor = tick % 5 == 0
            tickPaint.strokeWidth = if (isMajor) outerRadius * 0.014f else outerRadius * 0.006f
            tickPaint.alpha = if (isMajor) 255 else 130

            val angle = tick / 60.0 * TWO_PI
            val innerRadius = outerRadius * if (isMajor) 0.90f else 0.94f
            val sinA = sin(angle).toFloat()
            val cosA = cos(angle).toFloat()
            canvas.drawLine(
                centerX + sinA * innerRadius,
                centerY - cosA * innerRadius,
                centerX + sinA * outerRadius * 0.97f,
                centerY - cosA * outerRadius * 0.97f,
                tickPaint
            )
        }
    }

    private fun drawAnalog(
        canvas: Canvas,
        bounds: Rect,
        time: ZonedDateTime,
        isAmbient: Boolean
    ) {
        val centerX = bounds.exactCenterX()
        val centerY = bounds.exactCenterY()
        val radius = min(centerX, centerY)

        val seconds = time.second + time.nano / 1_000_000_000.0
        val minutes = time.minute + seconds / 60.0
        val hours = (time.hour % 12) + minutes / 60.0

        val hourAngle = hours / 12.0 * TWO_PI
        val minuteAngle = minutes / 60.0 * TWO_PI

        hourHandPaint.strokeWidth = radius * 0.045f
        minuteHandPaint.strokeWidth = radius * 0.030f
        secondHandPaint.strokeWidth = radius * 0.014f

        if (isAmbient) {
            // Thin outlined hands in always-on mode (burn-in friendly).
            hourHandPaint.style = Paint.Style.STROKE
            minuteHandPaint.style = Paint.Style.STROKE
            hourHandPaint.strokeWidth = radius * 0.020f
            minuteHandPaint.strokeWidth = radius * 0.014f
        }

        drawHand(canvas, centerX, centerY, hourAngle, radius * 0.50f, hourHandPaint)
        drawHand(canvas, centerX, centerY, minuteAngle, radius * 0.72f, minuteHandPaint)

        if (!isAmbient) {
            val secondAngle = seconds / 60.0 * TWO_PI
            drawHand(canvas, centerX, centerY, secondAngle, radius * 0.80f, secondHandPaint)
        }

        canvas.drawCircle(centerX, centerY, radius * 0.03f, centerDotPaint)
    }

    private fun drawHand(
        canvas: Canvas,
        centerX: Float,
        centerY: Float,
        angle: Double,
        length: Float,
        paint: Paint
    ) {
        canvas.drawLine(
            centerX,
            centerY,
            centerX + sin(angle).toFloat() * length,
            centerY - cos(angle).toFloat() * length,
            paint
        )
    }

    private fun drawDigital(
        canvas: Canvas,
        bounds: Rect,
        time: ZonedDateTime,
        isAmbient: Boolean,
        centered: Boolean
    ) {
        val centerX = bounds.exactCenterX()
        val height = bounds.height()

        digitalPaint.textSize = height * if (centered) 0.22f else 0.11f
        datePaint.textSize = height * 0.055f
        digitalPaint.alpha = if (isAmbient) 180 else 255

        val timeY = if (centered) bounds.exactCenterY() + digitalPaint.textSize * 0.35f
                    else height * 0.30f
        canvas.drawText(time.format(timeFormatter), centerX, timeY, digitalPaint)

        if (!isAmbient) {
            val dateY = timeY + datePaint.textSize * 1.6f
            canvas.drawText(time.format(dateFormatter).uppercase(Locale.getDefault()), centerX, dateY, datePaint)
        }
    }

    override fun onDestroy() {
        scope.cancel("ChronoLuxRenderer destroyed")
        super.onDestroy()
    }
}
