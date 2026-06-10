package com.chronolux.watchface.style

import android.graphics.Color
import androidx.annotation.StringRes
import com.chronolux.watchface.R

/**
 * Color palettes for the face. Each theme defines the accent used for hands
 * and highlights, a secondary tone for ticks/labels, and a background pair
 * used to paint a subtle radial gradient.
 */
enum class ColorTheme(
    val id: String,
    @StringRes val displayNameRes: Int,
    val accent: Int,
    val secondary: Int,
    val backgroundInner: Int,
    val backgroundOuter: Int
) {
    MIDNIGHT_GOLD(
        id = "midnight_gold",
        displayNameRes = R.string.theme_midnight_gold,
        accent = Color.parseColor("#E8C36A"),
        secondary = Color.parseColor("#8A7A52"),
        backgroundInner = Color.parseColor("#1A1A22"),
        backgroundOuter = Color.parseColor("#06060A")
    ),
    OCEAN(
        id = "ocean",
        displayNameRes = R.string.theme_ocean,
        accent = Color.parseColor("#5AC8FA"),
        secondary = Color.parseColor("#3A7A96"),
        backgroundInner = Color.parseColor("#0E1B26"),
        backgroundOuter = Color.parseColor("#04080C")
    ),
    CRIMSON(
        id = "crimson",
        displayNameRes = R.string.theme_crimson,
        accent = Color.parseColor("#F25C5C"),
        secondary = Color.parseColor("#8C3A3A"),
        backgroundInner = Color.parseColor("#22100F"),
        backgroundOuter = Color.parseColor("#0A0404")
    ),
    FOREST(
        id = "forest",
        displayNameRes = R.string.theme_forest,
        accent = Color.parseColor("#7ED491"),
        secondary = Color.parseColor("#447750"),
        backgroundInner = Color.parseColor("#0F1F14"),
        backgroundOuter = Color.parseColor("#040A06")
    );

    companion object {
        fun fromId(id: String): ColorTheme = entries.firstOrNull { it.id == id } ?: MIDNIGHT_GOLD
    }
}
