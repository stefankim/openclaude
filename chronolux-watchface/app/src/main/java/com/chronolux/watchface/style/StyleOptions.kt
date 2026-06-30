package com.chronolux.watchface.style

import androidx.annotation.StringRes
import com.chronolux.watchface.R

/** Clock format for the digital readout. */
enum class TimeFormat(val id: String, val pattern: String, @StringRes val displayNameRes: Int) {
    H24("h24", "HH:mm", R.string.time_format_24h),
    H12("h12", "hh:mm", R.string.time_format_12h);

    companion object {
        fun fromId(id: String): TimeFormat = entries.firstOrNull { it.id == id } ?: H24
    }
}

/** Date readout format. */
enum class DateFormat(val id: String, val pattern: String, @StringRes val displayNameRes: Int) {
    WEEKDAY("weekday", "EEE d MMM", R.string.date_format_weekday),
    NUMERIC("numeric", "d/M", R.string.date_format_numeric),
    MONTH_DAY("month_day", "MMM d", R.string.date_format_month_day);

    companion object {
        fun fromId(id: String): DateFormat = entries.firstOrNull { it.id == id } ?: WEEKDAY
    }
}

/**
 * Optional accent-color override. [DEFAULT] keeps the accent that ships with
 * the selected [ColorTheme]; any other value overrides it with a fixed swatch
 * (stored as a packed ARGB int). [colorOverride] is null for [DEFAULT].
 */
enum class AccentColor(
    val id: String,
    @StringRes val displayNameRes: Int,
    val colorOverride: Int?
) {
    DEFAULT("default", R.string.accent_default, null),
    GOLD("gold", R.string.accent_gold, 0xFFE8C36A.toInt()),
    CORAL("coral", R.string.accent_coral, 0xFFF2785C.toInt()),
    SKY("sky", R.string.accent_sky, 0xFF5AC8FA.toInt()),
    MINT("mint", R.string.accent_mint, 0xFF7ED491.toInt()),
    VIOLET("violet", R.string.accent_violet, 0xFFB18CF0.toInt()),
    ROSE("rose", R.string.accent_rose, 0xFFF06AA8.toInt()),
    SNOW("snow", R.string.accent_snow, 0xFFEDEDED.toInt());

    companion object {
        fun fromId(id: String): AccentColor = entries.firstOrNull { it.id == id } ?: DEFAULT
    }
}
