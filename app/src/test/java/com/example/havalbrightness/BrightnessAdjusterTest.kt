package com.example.havalbrightness

import com.example.havalbrightness.util.BrightnessAdjuster
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure-JVM tests for the day/night decision. No Android dependency, no device needed.
 *
 * Day window is [6, 19). Sanity-check both sides of each boundary so a future tweak to the
 * threshold can't silently flip the inclusivity of the comparison.
 */
class BrightnessAdjusterTest {

    @Test
    fun midnight_is_night() {
        assertEquals(BrightnessAdjuster.NIGHT_BRIGHTNESS, BrightnessAdjuster.brightnessForHour(0))
        assertFalse(BrightnessAdjuster.isDaytime(0))
    }

    @Test
    fun five_am_is_still_night() {
        assertEquals(BrightnessAdjuster.NIGHT_BRIGHTNESS, BrightnessAdjuster.brightnessForHour(5))
    }

    @Test
    fun six_am_is_first_day_hour() {
        assertEquals(BrightnessAdjuster.DAY_BRIGHTNESS, BrightnessAdjuster.brightnessForHour(6))
        assertTrue(BrightnessAdjuster.isDaytime(6))
    }

    @Test
    fun noon_is_day() {
        assertEquals(BrightnessAdjuster.DAY_BRIGHTNESS, BrightnessAdjuster.brightnessForHour(12))
    }

    @Test
    fun eighteen_is_last_day_hour() {
        // Window is half-open: 19 is excluded, so 18:00–18:59 is still day.
        assertEquals(BrightnessAdjuster.DAY_BRIGHTNESS, BrightnessAdjuster.brightnessForHour(18))
    }

    @Test
    fun nineteen_flips_to_night() {
        assertEquals(BrightnessAdjuster.NIGHT_BRIGHTNESS, BrightnessAdjuster.brightnessForHour(19))
        assertFalse(BrightnessAdjuster.isDaytime(19))
    }

    @Test
    fun late_evening_is_night() {
        assertEquals(BrightnessAdjuster.NIGHT_BRIGHTNESS, BrightnessAdjuster.brightnessForHour(23))
    }
}
