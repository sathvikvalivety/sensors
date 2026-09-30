package com.example.sensors.engine

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

class HapticFeedbackManager(context: Context) {

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        vibratorManager?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    var isHapticsEnabled: Boolean = true
    private var lastVibeTime: Long = 0

    fun triggerPulseForScore(score: Int) {
        if (!isHapticsEnabled || vibrator == null || !vibrator.hasVibrator() || score < 30) return

        val now = System.currentTimeMillis()
        // Throttle interval inversely proportional to score
        val intervalMs = (1000 - (score * 8.5f)).toLong().coerceIn(120L, 1000L)
        if (now - lastVibeTime < intervalMs) return
        lastVibeTime = now

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val amplitude = (score * 2.5f).toInt().coerceIn(20, 255)
                val duration = if (score > 75) 80L else 35L
                vibrator.vibrate(VibrationEffect.createOneShot(duration, amplitude))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(40)
            }
        } catch (e: Exception) {
            // Ignore security exception if vibrator is restricted
        }
    }

    fun triggerPanic() {
        if (!isHapticsEnabled || vibrator == null) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val pattern = longArrayOf(0, 150, 100, 150, 100, 250)
                val amplitudes = intArrayOf(0, 255, 0, 255, 0, 255)
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, amplitudes, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(500)
            }
        } catch (e: Exception) {
            // Safe fallback
        }
    }
}
