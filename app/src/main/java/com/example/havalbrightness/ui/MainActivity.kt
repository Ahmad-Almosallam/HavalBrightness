package com.example.havalbrightness.ui

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.havalbrightness.databinding.ActivityMainBinding
import com.example.havalbrightness.util.BrightnessAdjuster
import java.util.Calendar

/**
 * Tiny one-screen UI. The app is supposed to run silently after boot — this Activity exists
 * mostly so the user can:
 *   1. Grant the WRITE_SETTINGS special permission (one-time setup).
 *   2. Tap "Apply now" to verify the logic without rebooting the car.
 *   3. See at a glance which side of the day/night threshold we're on.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.textDeviceInfo.text = getString(
            com.example.havalbrightness.R.string.device_info_fmt,
            Build.MODEL ?: "?",
            Build.VERSION.RELEASE ?: "?",
            Build.VERSION.SDK_INT
        )

        binding.buttonGrantPermission.setOnClickListener { openWriteSettingsScreen() }
        binding.buttonApplyNow.setOnClickListener { applyNow() }
    }

    override fun onResume() {
        super.onResume()
        refreshState()
    }

    /**
     * Repaints the status block. Called from onResume so that returning from the system
     * "Modify system settings" screen instantly reflects the new permission state without
     * the user having to tap anything.
     */
    private fun refreshState() {
        val canWrite = Settings.System.canWrite(this)
        binding.textPermission.text = getString(
            if (canWrite) com.example.havalbrightness.R.string.permission_granted
            else com.example.havalbrightness.R.string.permission_missing
        )
        binding.buttonGrantPermission.isEnabled = !canWrite
        binding.buttonApplyNow.isEnabled = canWrite

        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val daytime = BrightnessAdjuster.isDaytime(hour)
        val brightness = BrightnessAdjuster.brightnessForHour(hour)
        binding.textWindow.text = getString(
            com.example.havalbrightness.R.string.window_fmt,
            BrightnessAdjuster.DAY_START_HOUR,
            BrightnessAdjuster.DAY_END_HOUR
        )
        binding.textCurrent.text = getString(
            com.example.havalbrightness.R.string.current_fmt,
            hour,
            if (daytime) getString(com.example.havalbrightness.R.string.daytime)
            else getString(com.example.havalbrightness.R.string.nighttime),
            brightness
        )
    }

    /**
     * Sends the user to the per-app "Modify system settings" toggle. There is no programmatic
     * grant for WRITE_SETTINGS — this is the only path.
     */
    private fun openWriteSettingsScreen() {
        val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
            data = Uri.parse("package:$packageName")
        }
        startActivity(intent)
    }

    private fun applyNow() {
        val written = BrightnessAdjuster.apply(this)
        val msg = if (written != null) {
            getString(com.example.havalbrightness.R.string.applied_fmt, written)
        } else {
            getString(com.example.havalbrightness.R.string.apply_failed_no_permission)
        }
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
        refreshState()
    }
}
