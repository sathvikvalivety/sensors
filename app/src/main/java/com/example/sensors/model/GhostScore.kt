package com.example.sensors.model

enum class ParanormalLevel(
    val title: String,
    val emoji: String,
    val colorHex: String,
    val dangerLevel: String
) {
    DORMANT("Nothing Happening", "😴", "#64748B", "SAFE"),
    SUSPICIOUS("Slightly Suspicious", "👀", "#00E5FF", "LOW"),
    WEIRD("Something is Weird", "🤨", "#FFD600", "MODERATE"),
    HIGH_ACTIVITY("High Ghost Activity", "😱", "#FF9100", "HIGH"),
    CRITICAL_BREACH("CLASS 5 ENTITY - RUN!", "👻", "#FF1744", "EXTREME")
}

data class GhostPersonality(
    val name: String,
    val mood: String,
    val favoriteActivity: String,
    val threatLevel: Int,
    val spectralDistance: String,
    val quote: String
)

data class HunterRank(
    val levelName: String,
    val badgeEmoji: String,
    val experiencePoints: Int,
    val huntsCompleted: Int
)
