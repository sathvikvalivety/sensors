package com.example.sensors

import com.example.sensors.engine.GhostPersonalityGenerator
import com.example.sensors.engine.GhostScoreEngine
import com.example.sensors.model.ParanormalLevel
import org.junit.Assert.*
import org.junit.Test

class GhostScoreEngineTest {

    @Test
    fun testScoreCalculation_allZero() {
        val score = GhostScoreEngine.calculateCompositeScore(
            magneticScore = 0,
            movementScore = 0,
            rotationScore = 0,
            lightScore = 0,
            soundScore = 0,
            proximityScore = 0
        )
        assertEquals(0, score)
        assertEquals(ParanormalLevel.DORMANT, GhostScoreEngine.getParanormalLevel(score))
    }

    @Test
    fun testScoreCalculation_allMax() {
        val score = GhostScoreEngine.calculateCompositeScore(
            magneticScore = 100,
            movementScore = 100,
            rotationScore = 100,
            lightScore = 100,
            soundScore = 100,
            proximityScore = 100
        )
        assertEquals(100, score)
        assertEquals(ParanormalLevel.CRITICAL_BREACH, GhostScoreEngine.getParanormalLevel(score))
    }

    @Test
    fun testScoreCalculation_sampleScenario() {
        // Magnetic: 85 (25%) -> 21.25
        // Movement: 70 (20%) -> 14.00
        // Rotation: 60 (15%) -> 9.00
        // Light: 90 (15%) -> 13.50
        // Sound: 75 (15%) -> 11.25
        // Proximity: 80 (10%) -> 8.00
        // Total = 77
        val score = GhostScoreEngine.calculateCompositeScore(
            magneticScore = 85,
            movementScore = 70,
            rotationScore = 60,
            lightScore = 90,
            soundScore = 75,
            proximityScore = 80
        )
        assertEquals(77, score)
        assertEquals(ParanormalLevel.HIGH_ACTIVITY, GhostScoreEngine.getParanormalLevel(score))
    }

    @Test
    fun testHunterRankProgression() {
        val rank0 = GhostScoreEngine.calculateHunterRank(0, 0) // XP: 0
        assertEquals("Ghost Rookie", rank0.levelName)

        val rank1 = GhostScoreEngine.calculateHunterRank(1, 40) // XP: 150 + 200 = 350 (< 500)
        assertEquals("Ghost Observer", rank1.levelName)

        val rank2 = GhostScoreEngine.calculateHunterRank(4, 50) // XP: 600 + 250 = 850 (< 1000)
        assertEquals("Paranormal Intern", rank2.levelName)

        val rank3 = GhostScoreEngine.calculateHunterRank(8, 70) // XP: 1200 + 350 = 1550 (< 2000)
        assertEquals("Ghost Hunter", rank3.levelName)

        val rank4 = GhostScoreEngine.calculateHunterRank(15, 90) // XP: 2250 + 450 = 2700 (< 4000)
        assertEquals("Ghost Whisperer", rank4.levelName)

        val rankMaster = GhostScoreEngine.calculateHunterRank(30, 100) // XP: 4500 + 500 = 5000 (>= 4000)
        assertEquals("DON'T ASK QUESTIONS", rankMaster.levelName)
    }


    @Test
    fun testGhostPersonalityGeneration() {
        val personality = GhostPersonalityGenerator.generatePersonality(85)
        assertNotNull(personality.name)
        assertNotNull(personality.mood)
        assertTrue(personality.threatLevel >= 10)
        assertTrue(personality.spectralDistance.contains("fake AI math"))
    }
}
