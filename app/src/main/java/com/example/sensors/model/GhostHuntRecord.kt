package com.example.sensors.model

data class GhostHuntRecord(
    val id: Long = 0,
    val timestamp: Long,
    val durationFormatted: String,
    val peakScore: Int,
    val paranormalLevel: String,
    val ghostName: String,
    val ghostMood: String,
    val locationTag: String,
    val evidenceSummary: String,
    val sensorBreakdown: String
)
