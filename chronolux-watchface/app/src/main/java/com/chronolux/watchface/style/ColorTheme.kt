package com.chronolux.watchface.style

import androidx.annotation.StringRes
import com.chronolux.watchface.R

/**
 * Color palettes for the face. Each theme defines the accent used for hands
 * and highlights, a secondary tone for ticks/labels, and a background pair
 * used to paint a subtle radial gradient.
 *
 * Colors are stored as packed ARGB ints (0xAARRGGBB) rather than parsed from
 * strings via `android.graphics.Color`, so this enum has no Android framework
 * dependency and is unit-testable on a plain JVM.
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
        accent = 0xFFE8C36A.toInt(),
        secondary = 0xFF8A7A52.toInt(),
        backgroundInner = 0xFF1A1A22.toInt(),
        backgroundOuter = 0xFF06060A.toInt()
    ),
    OCEAN(
        id = "ocean",
        displayNameRes = R.string.theme_ocean,
        accent = 0xFF5AC8FA.toInt(),
        secondary = 0xFF3A7A96.toInt(),
        backgroundInner = 0xFF0E1B26.toInt(),
        backgroundOuter = 0xFF04080C.toInt()
    ),
    CRIMSON(
        id = "crimson",
        displayNameRes = R.string.theme_crimson,
        accent = 0xFFF25C5C.toInt(),
        secondary = 0xFF8C3A3A.toInt(),
        backgroundInner = 0xFF22100F.toInt(),
        backgroundOuter = 0xFF0A0404.toInt()
    ),
    FOREST(
        id = "forest",
        displayNameRes = R.string.theme_forest,
        accent = 0xFF7ED491.toInt(),
        secondary = 0xFF447750.toInt(),
        backgroundInner = 0xFF0F1F14.toInt(),
        backgroundOuter = 0xFF040A06.toInt()
    );

    companion object {
        fun fromId(id: String): ColorTheme = entries.firstOrNull { it.id == id } ?: MIDNIGHT_GOLD
    }
}
