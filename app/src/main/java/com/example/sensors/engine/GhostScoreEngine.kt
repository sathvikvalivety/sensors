package com.example.sensors.engine

import com.example.sensors.model.HunterRank
import com.example.sensors.model.ParanormalLevel
import kotlin.math.roundToInt
import kotlin.random.Random

object GhostScoreEngine {

    // Weight coefficients as per specification
    const val WEIGHT_MAGNETIC = 0.25f
    const val WEIGHT_MOVEMENT = 0.20f
    const val WEIGHT_ROTATION = 0.15f
    const val WEIGHT_LIGHT = 0.15f
    const val WEIGHT_SOUND = 0.15f
    const val WEIGHT_PROXIMITY = 0.10f

    /**
     * Computes the composite Ghost Activity Score (0 - 100).
     */
    fun calculateCompositeScore(
        magneticScore: Int,
        movementScore: Int,
        rotationScore: Int,
        lightScore: Int,
        soundScore: Int,
        proximityScore: Int
    ): Int {
        val rawComposite = (magneticScore * WEIGHT_MAGNETIC) +
                (movementScore * WEIGHT_MOVEMENT) +
                (rotationScore * WEIGHT_ROTATION) +
                (lightScore * WEIGHT_LIGHT) +
                (soundScore * WEIGHT_SOUND) +
                (proximityScore * WEIGHT_PROXIMITY)

        return rawComposite.roundToInt().coerceIn(0, 100)
    }

    /**
     * Classifies score into structured level.
     */
    fun getParanormalLevel(score: Int): ParanormalLevel {
        return when (score) {
            in 0..20 -> ParanormalLevel.DORMANT
            in 21..40 -> ParanormalLevel.SUSPICIOUS
            in 41..60 -> ParanormalLevel.WEIRD
            in 61..80 -> ParanormalLevel.HIGH_ACTIVITY
            else -> ParanormalLevel.CRITICAL_BREACH
        }
    }

    /**
     * Humorous messages based on score tier.
     */
    fun getHumorousMessage(score: Int): String {
        return when (score) {
            in 0..20 -> listOf(
                "😴 No ghost detected. Even the ghosts are sleeping.",
                "🍃 Quiet as a digital graveyard. Zero spectral activity.",
                "🛋️ Not even a spooky dust particle in sight.",
                "💤 Ghosts took a coffee break. Try shaking your phone!"
            ).random()

            in 21..40 -> listOf(
                "👀 Something feels suspicious...",
                "🕯️ A faint chill just entered the room.",
                "📶 Is that an EMF ripple or just your neighbor's microwave?",
                "👁️ A ghost is casually observing your screen."
            ).random()

            in 41..60 -> listOf(
                "🤨 Okay... that's definitely weird.",
                "📻 Fluctuations detected in the ethereal spectrum!",
                "🔦 Room illumination is unstable. Ghost is playing with switches.",
                "🌀 The phone feels heavy. Spectral gravity anomaly detected!"
            ).random()

            in 61..80 -> listOf(
                "😱 HIGH GHOST ACTIVITY! Maybe start running.",
                "⚡ Poltergeist kinetic vortex detected in proximity!",
                "🎙️ EVP waveform spiking! A ghost is humming a spooky tune.",
                "⚠️ Significant magnetic disturbance. Hide your snacks!"
            ).random()

            else -> listOf(
                "👻 GHOST DETECTED!!! LEAVE THE ROOM IMMEDIATELY!",
                "💀 CLASS 5 APPARITION BREACH. DO NOT LOOK BEHIND YOU.",
                "🏃 Take your phone, delete browser history, and RUN!",
                "🛑 Spectral energy at 100%! The ghost is rating your wallpaper 2/10."
            ).random()
        }
    }

    /**
     * Determines Hunter Rank based on total score or hunts completed.
     */
    fun calculateHunterRank(huntsCompleted: Int, highestScore: Int): HunterRank {
        val xp = (huntsCompleted * 150) + (highestScore * 5)
        return when {
            xp < 200 -> HunterRank("Ghost Rookie", "🔰", xp, huntsCompleted)
            xp < 500 -> HunterRank("Ghost Observer", "👀", xp, huntsCompleted)
            xp < 1000 -> HunterRank("Paranormal Intern", "📋", xp, huntsCompleted)
            xp < 2000 -> HunterRank("Ghost Hunter", "🔦", xp, huntsCompleted)
            xp < 4000 -> HunterRank("Ghost Whisperer", "🔮", xp, huntsCompleted)
            else -> HunterRank("DON'T ASK QUESTIONS", "💀", xp, huntsCompleted)
        }
    }
}
