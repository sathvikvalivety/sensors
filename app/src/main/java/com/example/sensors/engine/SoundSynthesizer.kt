package com.example.sensors.engine

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.sin
import kotlin.random.Random

class SoundSynthesizer {

    private var audioTrack: AudioTrack? = null
    private val isRunning = AtomicBoolean(false)
    private var synthesisThread: Thread? = null

    @Volatile
    var isMuted: Boolean = false

    @Volatile
    var currentGhostScore: Int = 0

    fun start() {
        if (isRunning.get()) return
        isRunning.set(true)

        val sampleRate = 22050
        val bufferSize = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        ).coerceAtLeast(sampleRate / 10)

        audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(bufferSize)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

        try {
            audioTrack?.play()
        } catch (e: Exception) {
            // Audio permission or track init exception
        }

        synthesisThread = Thread {
            val shortBuffer = ShortArray(1024)
            var phase = 0.0

            while (isRunning.get()) {
                val score = currentGhostScore
                val muted = isMuted

                if (muted || score < 15) {
                    shortBuffer.fill(0)
                    try {
                        audioTrack?.write(shortBuffer, 0, shortBuffer.size)
                        Thread.sleep(40)
                    } catch (e: Exception) {
                        break
                    }
                    continue
                }

                // Geiger click probability / pulse frequency scales with ghost score
                val baseFreq = 220.0 + (score * 6.5) // Hz
                val clickProbability = (score / 100f) * 0.35f

                for (i in shortBuffer.indices) {
                    if (Random.nextFloat() < clickProbability) {
                        // Click / static crackle
                        val noise = (Random.nextInt(8000) - 4000).toShort()
                        shortBuffer[i] = noise
                    } else if (score > 60) {
                        // Spooky low sine frequency drone
                        phase += (2.0 * Math.PI * baseFreq) / sampleRate
                        val sample = (sin(phase) * (score * 40)).toInt().toShort()
                        shortBuffer[i] = sample
                    } else {
                        shortBuffer[i] = 0
                    }
                }

                try {
                    audioTrack?.write(shortBuffer, 0, shortBuffer.size)
                } catch (e: Exception) {
                    break
                }
            }
        }.apply {
            name = "GhostAudioSynthesizer"
            priority = Thread.MIN_PRIORITY
            start()
        }
    }

    fun stop() {
        isRunning.set(false)
        try {
            synthesisThread?.interrupt()
            synthesisThread = null
            audioTrack?.stop()
            audioTrack?.release()
            audioTrack = null
        } catch (e: Exception) {
            // Safe cleanup
        }
    }
}
