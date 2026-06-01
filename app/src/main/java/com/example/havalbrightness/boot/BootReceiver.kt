package com.example.havalbrightness.boot

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.havalbrightness.util.BrightnessAdjuster

/**
 * Fires once on device boot. `onReceive` runs on the main thread and must return quickly —
 * writing two ints to Settings.System is a few-millisecond operation, well under the ~10s
 * ANR budget, so we do the work inline rather than spinning up a service.
 *
 * We listen for both BOOT_COMPLETED and LOCKED_BOOT_COMPLETED (see AndroidManifest). On
 * direct-boot-aware ROMs LOCKED_BOOT_COMPLETED arrives first; on others, only BOOT_COMPLETED
 * fires. Either way the brightness is set once per boot.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action != Intent.ACTION_BOOT_COMPLETED &&
            action != Intent.ACTION_LOCKED_BOOT_COMPLETED
        ) {
            return
        }
        Log.i(TAG, "Boot signal received ($action) — applying brightness.")
        val written = BrightnessAdjuster.apply(context)
        if (written == null) {
            Log.w(TAG, "Brightness not applied: WRITE_SETTINGS permission missing.")
        }
    }

    private companion object {
        const val TAG = "BootReceiver"
    }
}
