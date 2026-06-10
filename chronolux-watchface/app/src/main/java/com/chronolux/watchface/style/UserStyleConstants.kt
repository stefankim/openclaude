package com.chronolux.watchface.style

import android.content.Context
import androidx.wear.watchface.style.UserStyleSchema
import androidx.wear.watchface.style.UserStyleSetting
import androidx.wear.watchface.style.UserStyleSetting.BooleanUserStyleSetting
import androidx.wear.watchface.style.UserStyleSetting.ListUserStyleSetting
import androidx.wear.watchface.style.WatchFaceLayer
import com.chronolux.watchface.R

/** Identifiers for the user-configurable style settings. */
object StyleIds {
    const val COLOR_THEME = "color_theme"
    const val LAYOUT_MODE = "layout_mode"
    const val SHOW_TICKS = "show_ticks"
}

/** Layout modes supported by the face. */
enum class LayoutMode(val id: String) {
    ANALOG("analog"),
    DIGITAL("digital"),
    HYBRID("hybrid");

    companion object {
        fun fromId(id: String): LayoutMode = entries.firstOrNull { it.id == id } ?: HYBRID
    }
}

/** Builds the schema the system editor and our on-watch editor both consume. */
fun createUserStyleSchema(context: Context): UserStyleSchema {
    val colorSetting = ListUserStyleSetting(
        UserStyleSetting.Id(StyleIds.COLOR_THEME),
        context.resources,
        R.string.setting_color_theme,
        R.string.setting_color_theme_description,
        icon = null,
        options = ColorTheme.entries.map { theme ->
            ListUserStyleSetting.ListOption(
                UserStyleSetting.Option.Id(theme.id),
                context.resources,
                theme.displayNameRes,
                icon = null
            )
        },
        listOf(
            WatchFaceLayer.BASE,
            WatchFaceLayer.COMPLICATIONS,
            WatchFaceLayer.COMPLICATIONS_OVERLAY
        )
    )

    val layoutSetting = ListUserStyleSetting(
        UserStyleSetting.Id(StyleIds.LAYOUT_MODE),
        context.resources,
        R.string.setting_layout_mode,
        R.string.setting_layout_mode_description,
        icon = null,
        options = LayoutMode.entries.map { mode ->
            ListUserStyleSetting.ListOption(
                UserStyleSetting.Option.Id(mode.id),
                context.resources,
                when (mode) {
                    LayoutMode.ANALOG -> R.string.layout_analog
                    LayoutMode.DIGITAL -> R.string.layout_digital
                    LayoutMode.HYBRID -> R.string.layout_hybrid
                },
                icon = null
            )
        },
        listOf(WatchFaceLayer.BASE, WatchFaceLayer.COMPLICATIONS_OVERLAY)
    )

    val ticksSetting = BooleanUserStyleSetting(
        UserStyleSetting.Id(StyleIds.SHOW_TICKS),
        context.resources,
        R.string.setting_show_ticks,
        R.string.setting_show_ticks_description,
        icon = null,
        listOf(WatchFaceLayer.BASE),
        defaultValue = true
    )

    return UserStyleSchema(listOf(colorSetting, layoutSetting, ticksSetting))
}
