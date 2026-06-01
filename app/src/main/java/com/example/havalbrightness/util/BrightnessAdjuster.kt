package com.example.havalbrightness.util

import android.content.ContentResolver
import android.content.Context
import android.provider.Settings
import android.util.Log
import java.util.Calendar

/**
 * Pure logic + the single Settings.System write used to change screen brightness.
 *
 * Brightness model
 * ----------------
 * Android stores the system brightness as an integer 0..255 in `Settings.System.SCREEN_BRIGHTNESS`.
 * 0 is "screen as dim as the panel allows" (not necessarily off); 255 is full brightness. The
 * value is only honoured when `SCREEN_BRIGHTNESS_MODE` is set to MANUAL — if the OS is in
 * AUTOMATIC mode it ignores the manual value and uses the ambient light sensor instead. So the
 * adjuster always switches the mode to MANUAL first, then writes the level.
 *
 * Day / night rule
 * ----------------
 * Pure hour-of-day check against [DAY_START_HOUR, DAY_END_HOUR). No location, no light sensor.
 * Deterministic and trivially unit-testable: see ExponentialBackoff-style splitting of the
 * pure function `brightnessForHour` from the impure `apply()`.
 */
object BrightnessAdjuster {

    private const val TAG = "BrightnessAdjuster"

    /** Inclusive lower bound of the "day" window, 24-hour clock. */
    const val DAY_START_HOUR = 6

    /** Exclusive upper bound of the "day" window. 19 means the last day-hour is 18:xx. */
    const val DAY_END_HOUR = 19

    /** Brightness value written during the day. 255 = full brightness on the panel. */
    const val DAY_BRIGHTNESS = 255

    /**
     * Brightness value written at night. Not zero on purpose: 0 can render the panel effectively
     * black on some head units, which is hostile when the driver glances at it. 10 ≈ 4% of max.
     */
    const val NIGHT_BRIGHTNESS = 10

    /**
     * Pure: returns the brightness we'd write for [hour] (24-hour clock, 0..23).
     * Day window is half-open: [DAY_START_HOUR, DAY_END_HOUR).
     */
    fun brightnessForHour(hour: Int): Int =
        if (hour in DAY_START_HOUR until DAY_END_HOUR) DAY_BRIGHTNESS else NIGHT_BRIGHTNESS

    /** Convenience: which side of the threshold are we on, for UI display. */
    fun isDaytime(hour: Int): Boolean = hour in DAY_START_HOUR until DAY_END_HOUR

    /**
     * Applies the brightness for the current wall-clock hour.
     *
     * Returns the value that was written, or `null` if we lacked WRITE_SETTINGS permission.
     * (We never throw from here — the BootReceiver calls this and there is no one to handle
     * a thrown exception in that context.)
     */
    fun apply(context: Context): Int? {
        if (!Settings.System.canWrite(context)) {
            Log.w(TAG, "WRITE_SETTINGS not granted — cannot change brightness.")
            return null
        }
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val value = brightnessForHour(hour)
        return writeBrightness(context.contentResolver, value)
    }

    /**
     * Writes the brightness mode + level in the right order. Switching the mode AFTER writing
     * the level can race on some OEM ROMs (the auto-mode handler can overwrite the level we
     * just set), so do mode first.
     */
    private fun writeBrightness(resolver: ContentResolver, value: Int): Int {
        Settings.System.putInt(
            resolver,
            Settings.System.SCREEN_BRIGHTNESS_MODE,
            Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
        )
        Settings.System.putInt(resolver, Settings.System.SCREEN_BRIGHTNESS, value)
        Log.i(TAG, "Brightness set to $value (mode=MANUAL).")
        return value
    }
}
