package com.chronolux.watchface.complications

import android.content.Context
import androidx.wear.watchface.CanvasComplicationFactory
import androidx.wear.watchface.ComplicationSlot
import androidx.wear.watchface.ComplicationSlotsManager
import androidx.wear.watchface.complications.ComplicationSlotBounds
import androidx.wear.watchface.complications.DefaultComplicationDataSourcePolicy
import androidx.wear.watchface.complications.SystemDataSources
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.rendering.CanvasComplicationDrawable
import androidx.wear.watchface.complications.rendering.ComplicationDrawable
import androidx.wear.watchface.style.CurrentUserStyleRepository
import com.chronolux.watchface.R

object ComplicationIds {
    const val LEFT = 100
    const val RIGHT = 101
    const val BOTTOM = 102
}

private val SUPPORTED_TYPES = listOf(
    ComplicationType.RANGED_VALUE,
    ComplicationType.SHORT_TEXT,
    ComplicationType.MONOCHROMATIC_IMAGE,
    ComplicationType.SMALL_IMAGE
)

/**
 * Three slots: heart rate on the left, step count on the right and watch
 * battery along the bottom. All are user-reassignable from the editor.
 */
fun createComplicationSlotsManager(
    context: Context,
    currentUserStyleRepository: CurrentUserStyleRepository
): ComplicationSlotsManager {
    val factory = CanvasComplicationFactory { watchState, listener ->
        CanvasComplicationDrawable(
            ComplicationDrawable.getDrawable(context, R.drawable.complication_style)!!,
            watchState,
            listener
        )
    }

    val left = ComplicationSlot.createRoundRectComplicationSlotBuilder(
        id = ComplicationIds.LEFT,
        canvasComplicationFactory = factory,
        supportedTypes = SUPPORTED_TYPES,
        defaultDataSourcePolicy = DefaultComplicationDataSourcePolicy(
            SystemDataSources.DATA_SOURCE_HEART_RATE,
            ComplicationType.SHORT_TEXT
        ),
        bounds = ComplicationSlotBounds(
            android.graphics.RectF(0.12f, 0.40f, 0.32f, 0.60f)
        )
    ).build()

    val right = ComplicationSlot.createRoundRectComplicationSlotBuilder(
        id = ComplicationIds.RIGHT,
        canvasComplicationFactory = factory,
        supportedTypes = SUPPORTED_TYPES,
        defaultDataSourcePolicy = DefaultComplicationDataSourcePolicy(
            SystemDataSources.DATA_SOURCE_DAILY_STEPS,
            ComplicationType.SHORT_TEXT
        ),
        bounds = ComplicationSlotBounds(
            android.graphics.RectF(0.68f, 0.40f, 0.88f, 0.60f)
        )
    ).build()

    val bottom = ComplicationSlot.createRoundRectComplicationSlotBuilder(
        id = ComplicationIds.BOTTOM,
        canvasComplicationFactory = factory,
        supportedTypes = SUPPORTED_TYPES,
        defaultDataSourcePolicy = DefaultComplicationDataSourcePolicy(
            SystemDataSources.DATA_SOURCE_WATCH_BATTERY,
            ComplicationType.RANGED_VALUE
        ),
        bounds = ComplicationSlotBounds(
            android.graphics.RectF(0.40f, 0.66f, 0.60f, 0.86f)
        )
    ).build()

    return ComplicationSlotsManager(listOf(left, right, bottom), currentUserStyleRepository)
}
