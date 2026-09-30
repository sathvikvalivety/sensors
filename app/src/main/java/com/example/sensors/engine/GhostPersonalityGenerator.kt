package com.example.sensors.engine

import com.example.sensors.model.GhostPersonality
import kotlin.math.roundToInt
import kotlin.random.Random

object GhostPersonalityGenerator {

    private val GHOST_NAMES = listOf(
        "Casper the Inconvenient",
        "Lord ShadowByte 404",
        "Sir Rustington III",
        "The Glitch Phantom",
        "Poltergeist Pete",
        "Baron Von EMF",
        "Spooky NullPointer",
        "Count StackOverflow",
        "Ethereal Evelyn",
        "Professor Ghoulbert"
    )

    private val GHOST_MOODS = listOf(
        "Angry 😡 (Looking for lost socks)",
        "Curious 👀 (Inspecting your screen brightness)",
        "Confused 😵 (Tried to walk through a wall, got stuck in Wi-Fi)",
        "Playful 🎈 (Wants to spin your gyroscope)",
        "Furious 👿 (Hates ambient lighting)",
        "Chill ☕ (Just haunting for free room & board)",
        "Sassy 💅 (Judging your notification count)"
    )

    private val GHOST_ACTIVITIES = listOf(
        "Spinning your phone's gyroscope",
        "Draining 3% of your battery per minute",
        "Whispering random binary code into the microphone",
        "Flipping the light sensor when you look away",
        "Messing with the magnetometer compass needle",
        "Hovering 10cm behind your chair"
    )

    private val GHOST_QUOTES = listOf(
        "\"I was just looking for the Wi-Fi password, honestly.\"",
        "\"Your sensors are surprisingly accurate, mortal!\"",
        "\"Did you really think closing the door would keep me out?\"",
        "\"Please shake the phone again, it's like a roller coaster for me.\"",
        "\"I rate this haunting experience 4.5 out of 5 stars.\""
    )

    fun generatePersonality(finalScore: Int): GhostPersonality {
        val name = GHOST_NAMES.random()
        val mood = GHOST_MOODS.random()
        val activity = GHOST_ACTIVITIES.random()
        val threatLevel = (finalScore * 0.95f + Random.nextInt(5)).roundToInt().coerceIn(10, 100)
        val distanceMeters = String.format("%.1f", Random.nextDouble(0.4, 4.8))
        val distanceText = "Ghost is ~$distanceMeters meters away (100% fake AI math 😂)"
        val quote = GHOST_QUOTES.random()

        return GhostPersonality(
            name = name,
            mood = mood,
            favoriteActivity = activity,
            threatLevel = threatLevel,
            spectralDistance = distanceText,
            quote = quote
        )
    }
}
