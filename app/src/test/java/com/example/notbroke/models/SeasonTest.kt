package com.example.notbroke.models

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.*

class SeasonTest {
    private lateinit var rewards: MutableList<Reward>
    private lateinit var season: Season
    private lateinit var startDate: Date
    private lateinit var endDate: Date

    @Before
    fun setup() {
        rewards = mutableListOf(
            Reward(1, "Reward1", "desc1", 100, 0, RewardType.MONTHLY, isUnlocked = false),
            Reward(2, "Reward2", "desc2", 200, 0, RewardType.MONTHLY, isUnlocked = false)
        )
        startDate = Date(System.currentTimeMillis() - 100000)
        endDate = Date(System.currentTimeMillis() + 100000)
        season = Season(
            id = 1,
            name = "Spring",
            startDate = startDate,
            endDate = endDate,
            rewards = rewards,
            currentExperience = 0,
            maxExperience = 200
        )
    }

    @Test
    fun `test Season creation and properties`() {
        assertEquals(1, season.id)
        assertEquals("Spring", season.name)
        assertEquals(startDate, season.startDate)
        assertEquals(endDate, season.endDate)
        assertEquals(rewards, season.rewards)
        assertEquals(0, season.currentExperience)
        assertEquals(200, season.maxExperience)
    }

    @Test
    fun `test getProgress calculation`() {
        season.currentExperience = 50
        assertEquals(0.25f, season.getProgress(), 0.001f)
    }

    @Test
    fun `test isActive true and false`() {
        assertTrue(season.isActive())
        val pastSeason = Season(2, "Past", Date(0), Date(1), rewards, 0, 100)
        assertFalse(pastSeason.isActive())
    }

    @Test
    fun `test getUnlockedRewards and getNextReward`() {
        assertTrue(season.getUnlockedRewards().isEmpty())
        assertEquals(1, season.getNextReward()?.id)
        // Unlock first reward
        rewards[0].isUnlocked = true
        assertEquals(1, season.getUnlockedRewards().size)
        assertEquals(2, season.getNextReward()?.id)
    }

    @Test
    fun `test addExperience and reward unlocking`() {
        season.addExperience(150)
        assertEquals(150, season.currentExperience)
        // Should unlock first reward (100 points)
        assertTrue(rewards[0].isUnlocked)
        // Should not unlock second reward yet
        assertFalse(rewards[1].isUnlocked)
        // Add more experience
        season.addExperience(100)
        assertEquals(200, season.currentExperience)
        assertTrue(rewards[1].isUnlocked)
        // Should not exceed maxExperience
        season.addExperience(1000)
        assertEquals(200, season.currentExperience)
    }
} 