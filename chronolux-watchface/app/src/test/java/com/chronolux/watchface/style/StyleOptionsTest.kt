package com.chronolux.watchface.style

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure-JVM unit tests for the style option enums. These hold no Android
 * framework dependency now that colors are packed ints, so they run under
 * plain JUnit without Robolectric.
 */
class StyleOptionsTest {

    @Test
    fun colorTheme_fromId_roundTripsEveryEntry() {
        ColorTheme.entries.forEach { theme ->
            assertEquals(theme, ColorTheme.fromId(theme.id))
        }
    }

    @Test
    fun colorTheme_fromId_unknownFallsBackToMidnightGold() {
        assertEquals(ColorTheme.MIDNIGHT_GOLD, ColorTheme.fromId("does_not_exist"))
    }

    @Test
    fun colorTheme_allColorsAreOpaque() {
        // Every packed color must have a full alpha byte (0xFF) so nothing
        // renders unexpectedly transparent.
        ColorTheme.entries.forEach { theme ->
            listOf(theme.accent, theme.secondary, theme.backgroundInner, theme.backgroundOuter)
                .forEach { color ->
                    assertEquals("alpha of ${'$'}{theme.id}", 0xFF, (color ushr 24) and 0xFF)
                }
        }
    }

    @Test
    fun layoutMode_fromId_roundTripsAndFallsBackToHybrid() {
        LayoutMode.entries.forEach { mode -> assertEquals(mode, LayoutMode.fromId(mode.id)) }
        assertEquals(LayoutMode.HYBRID, LayoutMode.fromId("bogus"))
    }

    @Test
    fun timeFormat_patternsAreValid() {
        assertEquals("HH:mm", TimeFormat.H24.pattern)
        assertEquals("hh:mm", TimeFormat.H12.pattern)
        assertEquals(TimeFormat.H24, TimeFormat.fromId("nope"))
    }

    @Test
    fun dateFormat_fromId_roundTripsAndFallsBack() {
        DateFormat.entries.forEach { fmt -> assertEquals(fmt, DateFormat.fromId(fmt.id)) }
        assertEquals(DateFormat.WEEKDAY, DateFormat.fromId("nope"))
    }

    @Test
    fun accentColor_defaultHasNoOverride_othersDo() {
        assertNull(AccentColor.DEFAULT.colorOverride)
        AccentColor.entries.filter { it != AccentColor.DEFAULT }.forEach { accent ->
            assertNotNull("${'$'}{accent.id} should define a color", accent.colorOverride)
            assertEquals(0xFF, (accent.colorOverride!! ushr 24) and 0xFF)
        }
    }

    @Test
    fun accentColor_idsAreUnique() {
        val ids = AccentColor.entries.map { it.id }
        assertTrue("ids must be unique", ids.size == ids.toSet().size)
    }
}
