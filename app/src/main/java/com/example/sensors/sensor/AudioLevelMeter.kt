package com.example.sensors.sensor

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.core.content.ContextCompat
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.log10
import kotlin.math.sqrt

class AudioLevelMeter(private val context: Context) {

    private var audioRecord: AudioRecord? = null
    private val isRecording = AtomicBoolean(false)
    private var recordThread: Thread? = null

    @Volatile
    var currentAmplitude: Float = 0f

    @Volatile
    var currentDecibels: Float = 0f

    @Volatile
    var currentSoundScore: Int = 0

    fun hasRecordPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun start(onSoundUpdate: (amplitude: Float, db: Float, score: Int) -> Unit) {
        if (isRecording.get() || !hasRecordPermission()) return
        isRecording.set(true)

        val sampleRate = 44100
        val channelConfig = AudioFormat.CHANNEL_IN_MONO
        val audioFormat = AudioFormat.ENCODING_PCM_16BIT
        val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat).coerceAtLeast(2048)

        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                channelConfig,
                audioFormat,
                bufferSize
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                isRecording.set(false)
                return
            }

            audioRecord?.startRecording()
        } catch (e: Exception) {
            isRecording.set(false)
            return
        }

        recordThread = Thread {
            val buffer = ShortArray(bufferSize)
            var baselineDb = 40f

            while (isRecording.get()) {
                val readSize = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                if (readSize > 0) {
                    var sumSquare = 0.0
                    for (i in 0 until readSize) {
                        val sample = buffer[i].toDouble()
                        sumSquare += sample * sample
                    }
                    val rms = sqrt(sumSquare / readSize)
                    val ampNorm = (rms / 32768.0).toFloat().coerceIn(0f, 1f)

                    // Approximate decibels (reference 1.0)
                    val db = if (rms > 0) {
                        (20.0 * log10(rms)).toFloat().coerceIn(0f, 90f)
                    } else 0f

                    // Rolling baseline adaptation
                    baselineDb = (baselineDb * 0.95f) + (db * 0.05f)

                    // Score calculation: noise spikes above baseline
                    val spikeDelta = (db - baselineDb).coerceAtLeast(0f)
                    val score = when {
                        db < 25f -> 5
                        spikeDelta > 20f -> 90
                        spikeDelta > 12f -> 75
                        spikeDelta > 6f -> 50
                        db > 65f -> 80
                        db > 50f -> 45
                        else -> 15
                    }

                    currentAmplitude = ampNorm
                    currentDecibels = db
                    currentSoundScore = score

                    onSoundUpdate(ampNorm, db, score)
                }
                try {
                    Thread.sleep(80)
                } catch (e: InterruptedException) {
                    break
                }
            }
        }.apply {
            name = "GhostAudioMeter"
            priority = Thread.MIN_PRIORITY
            start()
        }
    }

    fun stop() {
        isRecording.set(false)
        try {
            recordThread?.interrupt()
            recordThread = null
            audioRecord?.stop()
            audioRecord?.release()
            audioRecord = null
        } catch (e: Exception) {
            // Safe teardown
        }
    }
}
