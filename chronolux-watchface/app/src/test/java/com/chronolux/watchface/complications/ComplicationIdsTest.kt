package com.chronolux.watchface.complications

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Regression guard for the complication persistence contract.
 *
 * Wear OS remembers the user's chosen complication provider per slot id, so
 * these ids must never change — if they did, every previously selected
 * "widget app" would be orphaned when the user switches faces and comes back.
 * If one of these assertions fails, you are about to break persistence for
 * existing users; add a NEW id instead of changing an existing one.
 */
class ComplicationIdsTest {

    @Test
    fun slotIds_haveTheirFrozenValues() {
        assertEquals(100, ComplicationIds.LEFT)
        assertEquals(101, ComplicationIds.RIGHT)
        assertEquals(102, ComplicationIds.BOTTOM)
    }

    @Test
    fun slotIds_areUnique() {
        assertEquals(ComplicationIds.ALL.size, ComplicationIds.ALL.toSet().size)
    }

    @Test
    fun allList_containsEverySlot() {
        assertTrue(ComplicationIds.ALL.containsAll(listOf(100, 101, 102)))
        assertEquals(3, ComplicationIds.ALL.size)
    }
}
