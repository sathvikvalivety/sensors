package com.example.sensors

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.sensors.database.GhostHuntDbHelper
import com.example.sensors.databinding.ActivityMainBinding
import com.example.sensors.engine.GhostPersonalityGenerator
import com.example.sensors.engine.GhostScoreEngine
import com.example.sensors.engine.HapticFeedbackManager
import com.example.sensors.engine.SoundSynthesizer
import com.example.sensors.model.GhostHuntRecord
import com.example.sensors.model.GhostPersonality
import com.example.sensors.model.ParanormalLevel
import com.example.sensors.model.SensorData
import com.example.sensors.sensor.AudioLevelMeter
import com.example.sensors.sensor.GhostSensorManager
import com.example.sensors.ui.GhostHuntAdapter
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    // Core Managers & Engines
    private lateinit var dbHelper: GhostHuntDbHelper
    private lateinit var sensorManager: GhostSensorManager
    private lateinit var audioMeter: AudioLevelMeter
    private lateinit var soundSynth: SoundSynthesizer
    private lateinit var haptics: HapticFeedbackManager
    private lateinit var historyAdapter: GhostHuntAdapter

    // Session State
    private var isHunting = false
    private var huntStartTime = 0L
    private var huntDurationSeconds = 0
    private var peakGhostScore = 0
    private var peakSensorData = SensorData()
    private var currentPersonality: GhostPersonality? = null

    // Handlers
    private val mainHandler = Handler(Looper.getMainLooper())
    private val timerRunnable = object : Runnable {
        override fun run() {
            if (isHunting) {
                huntDurationSeconds++
                val minutes = huntDurationSeconds / 60
                val seconds = huntDurationSeconds % 60
                binding.tvScannerTimer.text = String.format(Locale.US, "⏱️ HUNT DURATION: %02d:%02d", minutes, seconds)
                mainHandler.postDelayed(this, 1000)
            }
        }
    }

    // Permission launcher
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted && isHunting) {
            audioMeter.start { amp, db, score ->
                runOnUiThread {
                    sensorManager.updateSoundTelemetry(amp, db, score)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initServices()
        initUI()
        refreshDashboardStats()
    }

    private fun initServices() {
        dbHelper = GhostHuntDbHelper(this)
        haptics = HapticFeedbackManager(this)
        soundSynth = SoundSynthesizer()
        audioMeter = AudioLevelMeter(this)

        sensorManager = GhostSensorManager(this) { telemetry ->
            runOnUiThread {
                handleLiveTelemetry(telemetry)
            }
        }
    }

    private fun initUI() {
        // Setup RecyclerView
        historyAdapter = GhostHuntAdapter(emptyList()) { record ->
            dbHelper.deleteHunt(record.id)
            refreshHistoryList()
            refreshDashboardStats()
            Toast.makeText(this, "Record deleted from dossier", Toast.LENGTH_SHORT).show()
        }
        binding.rvHuntHistory.layoutManager = LinearLayoutManager(this)
        binding.rvHuntHistory.adapter = historyAdapter

        // Home Buttons
        binding.btnHomeStartHunt.setOnClickListener {
            startGhostHunt()
        }

        // Scanner Buttons
        binding.btnScannerCaptureEvidence.setOnClickListener {
            captureEvidenceAndConclude()
        }

        binding.btnScannerFinishHunt.setOnClickListener {
            stopGhostHunt(showResult = true)
        }

        binding.btnToggleSound.setOnClickListener {
            soundSynth.isMuted = !soundSynth.isMuted
            binding.btnToggleSound.text = if (soundSynth.isMuted) "🔇 Audio Muted" else "🔊 EMF Audio"
        }

        binding.btnToggleHaptics.setOnClickListener {
            haptics.isHapticsEnabled = !haptics.isHapticsEnabled
            binding.btnToggleHaptics.text = if (haptics.isHapticsEnabled) "📳 Haptics ON" else "📳 Haptics OFF"
        }

        // Result Screen Buttons
        binding.btnResultSaveDossier.setOnClickListener {
            saveCurrentHuntToDossier()
        }

        binding.btnResultPanicRun.setOnClickListener {
            haptics.triggerPanic()
            navigateToScreen(0)
            Toast.makeText(this, "🏃 You safely fled the paranormal perimeter!", Toast.LENGTH_SHORT).show()
        }

        // History Clear Button
        binding.btnClearHistory.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Clear Paranormal Dossier?")
                .setMessage("Are you sure you want to delete all recorded ghost hunt records?")
                .setPositiveButton("Clear All") { _, _ ->
                    dbHelper.clearAllHunts()
                    refreshHistoryList()
                    refreshDashboardStats()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        // Bottom Navigation Tabs
        binding.navBtnHome.setOnClickListener { navigateToScreen(0) }
        binding.navBtnScanner.setOnClickListener {
            if (!isHunting) startGhostHunt()
            else navigateToScreen(1)
        }
        binding.navBtnHistory.setOnClickListener {
            refreshHistoryList()
            navigateToScreen(3)
        }
    }

    private fun navigateToScreen(index: Int) {
        binding.viewFlipper.displayedChild = index

        // Update Bottom Nav Styling
        val activeColor = ContextCompat.getColor(this, R.color.neon_green)
        val inactiveColor = ContextCompat.getColor(this, R.color.text_muted)

        binding.tvNavHome.setTextColor(if (index == 0) activeColor else inactiveColor)
        binding.tvNavScanner.setTextColor(if (index == 1) activeColor else inactiveColor)
        binding.tvNavHistory.setTextColor(if (index == 3) activeColor else inactiveColor)

        if (index == 3) {
            refreshHistoryList()
        }
    }

    private fun startGhostHunt() {
        if (isHunting) {
            navigateToScreen(1)
            return
        }

        isHunting = true
        huntStartTime = System.currentTimeMillis()
        huntDurationSeconds = 0
        peakGhostScore = 0
        peakSensorData = SensorData()
        currentPersonality = null

        binding.tvHeaderStatusDot.text = "● ACTIVE SCAN"
        binding.tvHeaderStatusDot.setTextColor(ContextCompat.getColor(this, R.color.danger_red))

        // Start Sensors & Engines
        sensorManager.start()
        soundSynth.start()
        mainHandler.post(timerRunnable)

        // Request audio if needed
        if (audioMeter.hasRecordPermission()) {
            audioMeter.start { amp, db, score ->
                runOnUiThread {
                    sensorManager.updateSoundTelemetry(amp, db, score)
                }
            }
        } else {
            requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }

        navigateToScreen(1)
        Toast.makeText(this, "👻 Sensor Telemetry Activated! Begin scanning.", Toast.LENGTH_SHORT).show()
    }

    private fun stopGhostHunt(showResult: Boolean) {
        if (!isHunting) return
        isHunting = false
        mainHandler.removeCallbacks(timerRunnable)

        sensorManager.stop()
        audioMeter.stop()
        soundSynth.stop()

        binding.tvHeaderStatusDot.text = "● SYSTEM ARMED"
        binding.tvHeaderStatusDot.setTextColor(ContextCompat.getColor(this, R.color.neon_green))

        if (showResult) {
            prepareResultScreen()
            navigateToScreen(2)
        }
    }

    private fun captureEvidenceAndConclude() {
        haptics.triggerPanic()
        stopGhostHunt(showResult = true)
        Toast.makeText(this, "📸 Paranormal Evidence Logged!", Toast.LENGTH_SHORT).show()
    }

    private fun handleLiveTelemetry(data: SensorData) {
        if (!isHunting) return

        val score = data.ghostScore
        if (score > peakGhostScore) {
            peakGhostScore = score
            peakSensorData = data
        }

        // 1. Radar View
        binding.radarView.setGhostScore(score)

        // 2. Sound & Haptics
        soundSynth.currentGhostScore = score
        haptics.triggerPulseForScore(score)

        // 3. Composite Score Dial
        binding.tvLiveScorePercent.text = "$score%"
        binding.progressGhostScore.progress = score

        val level = GhostScoreEngine.getParanormalLevel(score)
        val levelColor = Color.parseColor(level.colorHex)
        binding.tvLiveScorePercent.setTextColor(levelColor)
        binding.progressGhostScore.setIndicatorColor(levelColor)
        binding.tvLiveGhostEmoji.text = level.emoji
        binding.tvLiveGhostTitle.text = level.title
        binding.tvLiveGhostThreat.text = "Threat: ${level.dangerLevel} • Active Sensor Fusion"

        // Update random funny message
        binding.tvLiveHumorousMessage.text = GhostScoreEngine.getHumorousMessage(score)

        // 4. Update individual sensor UI bars & text
        // Magnetometer
        binding.tvSensorMagVal.text = String.format(Locale.US, "%.1f μT (%d%%)", data.magneticMagnitude, data.magneticScore)
        binding.pbSensorMag.progress = data.magneticScore

        // Accelerometer
        binding.tvSensorAccelVal.text = String.format(Locale.US, "%.2f m/s² (%d%%)", data.accelMotionMagnitude, data.movementScore)
        binding.pbSensorAccel.progress = data.movementScore

        // Gyroscope
        binding.tvSensorGyroVal.text = String.format(Locale.US, "%.2f rad/s (%d%%)", data.rotationSpeed, data.rotationScore)
        binding.pbSensorGyro.progress = data.rotationScore

        // Light
        binding.tvSensorLightVal.text = String.format(Locale.US, "%.0f lx (%d%%)", data.lightLux, data.lightScore)
        binding.pbSensorLight.progress = data.lightScore

        // Sound
        binding.tvSensorSoundVal.text = String.format(Locale.US, "%.0f dB (%d%%)", data.soundDecibels, data.soundScore)
        binding.pbSensorSound.progress = data.soundScore

        // Proximity
        val proxText = if (data.isNear) "ENTITY CLOSE! (${data.proximityDistanceCm.toInt()} cm)" else "CLEAR (5 cm)"
        binding.tvSensorProxVal.text = "$proxText (${data.proximityScore}%)"
        binding.pbSensorProx.progress = data.proximityScore
    }

    private fun prepareResultScreen() {
        val score = peakGhostScore.coerceAtLeast(12)
        val personality = GhostPersonalityGenerator.generatePersonality(score)
        currentPersonality = personality

        val level = GhostScoreEngine.getParanormalLevel(score)

        binding.tvResultPeakScore.text = "PEAK GHOST SCORE: $score%"
        binding.tvResultPeakScore.setTextColor(Color.parseColor(level.colorHex))

        binding.tvResultGhostName.text = "👻 ${personality.name}"
        binding.tvResultGhostMood.text = "Mood: ${personality.mood}"
        binding.tvResultGhostActivity.text = "Favorite Activity: ${personality.favoriteActivity}"
        binding.tvResultThreatLevel.text = "Threat Level: ${personality.threatLevel}% (${level.dangerLevel})"
        binding.tvResultDistance.text = personality.spectralDistance
        binding.tvResultQuote.text = personality.quote

        val breakdown = """
            🧲 Mag: ${peakSensorData.magneticScore}% | 📱 Mov: ${peakSensorData.movementScore}% | 🔄 Gyr: ${peakSensorData.rotationScore}%
            💡 Lux: ${peakSensorData.lightScore}% | 🎤 Snd: ${peakSensorData.soundScore}% | 📏 Prox: ${peakSensorData.proximityScore}%
        """.trimIndent()
        binding.tvResultSensorBreakdown.text = "📊 SENSOR BREAKDOWN AT PEAK:\n$breakdown"
    }

    private fun saveCurrentHuntToDossier() {
        val personality = currentPersonality ?: GhostPersonalityGenerator.generatePersonality(peakGhostScore)
        val level = GhostScoreEngine.getParanormalLevel(peakGhostScore)
        val minutes = huntDurationSeconds / 60
        val seconds = huntDurationSeconds % 60
        val durationFormatted = String.format(Locale.US, "%02d:%02d", minutes, seconds)

        val breakdown = "🧲 Mag: ${peakSensorData.magneticScore}% | 📱 Mov: ${peakSensorData.movementScore}% | 🔄 Gyr: ${peakSensorData.rotationScore}% | 💡 Lux: ${peakSensorData.lightScore}% | 🎤 Snd: ${peakSensorData.soundScore}% | 📏 Prox: ${peakSensorData.proximityScore}%"

        val record = GhostHuntRecord(
            timestamp = System.currentTimeMillis(),
            durationFormatted = durationFormatted,
            peakScore = peakGhostScore,
            paranormalLevel = level.title,
            ghostName = personality.name,
            ghostMood = personality.mood,
            locationTag = "Sector Alpha (Lab)",
            evidenceSummary = personality.spectralDistance,
            sensorBreakdown = breakdown
        )

        dbHelper.insertHunt(record)
        refreshDashboardStats()
        refreshHistoryList()
        Toast.makeText(this, "💾 Hunt dossier successfully archived!", Toast.LENGTH_SHORT).show()
        navigateToScreen(3)
    }

    private fun refreshHistoryList() {
        val hunts = dbHelper.getAllHunts()
        historyAdapter.updateData(hunts)
        binding.tvHistoryEmptyState.visibility = if (hunts.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun refreshDashboardStats() {
        val count = dbHelper.getHuntCount()
        val maxScore = dbHelper.getHighestScore()

        binding.tvHomeTotalHunts.text = count.toString()
        binding.tvHomeMaxScore.text = "$maxScore%"

        val rank = GhostScoreEngine.calculateHunterRank(count, maxScore)
        binding.tvHomeRankTitle.text = rank.levelName
        binding.tvHomeRankEmoji.text = rank.badgeEmoji
        binding.tvHomeRankXp.text = "XP: ${rank.experiencePoints} • Completed: ${rank.huntsCompleted} Hunts"
    }

    override fun onDestroy() {
        stopGhostHunt(showResult = false)
        super.onDestroy()
    }
}
