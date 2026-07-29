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

/**
 * Stable identifiers for the complication slots.
 *
 * PERSISTENCE CONTRACT: Wear OS persists the user's chosen complication data
 * source ("selected app") per `(watch face component, instance id, slot id)`.
 * That mapping is what lets a selection survive switching to another watch
 * face and back. These ids MUST therefore stay constant across app versions —
 * renumbering a slot orphans every selection previously made for it. New slots
 * may be added with new ids, but existing ones are frozen. `ComplicationIdsTest`
 * guards this.
 */
object ComplicationIds {
    const val LEFT = 100
    const val RIGHT = 101
    const val BOTTOM = 102

    /** All slot ids, for iteration and validation. */
    val ALL = listOf(LEFT, RIGHT, BOTTOM)
}

private val SUPPORTED_TYPES = listOf(
    ComplicationType.RANGED_VALUE,
    ComplicationType.SHORT_TEXT,
    ComplicationType.MONOCHROMATIC_IMAGE,
    ComplicationType.SMALL_IMAGE
)

/**
 * Three slots: step count on the left, day/date on the right and watch
 * battery along the bottom. These are just the out-of-box defaults — every
 * slot is user-reassignable from the editor (e.g. to a heart-rate provider
 * if the watch exposes one). There is no system default source for heart
 * rate, so it is offered through the picker rather than pre-filled.
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
            SystemDataSources.DATA_SOURCE_STEP_COUNT,
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
            SystemDataSources.DATA_SOURCE_DAY_AND_DATE,
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
