package com.example.notbroke.models

import org.junit.Assert.*
import org.junit.Test

class RewardTest {
    @Test
    fun `test Reward creation with valid data`() {
        val reward = Reward(
            id = 1,
            name = "Budget Master",
            description = "Stay within budget",
            experiencePoints = 100,
            iconResId = 123,
            type = RewardType.MONTHLY,
            isUnlocked = false,
            claimed = false
        )
        assertEquals(1, reward.id)
        assertEquals("Budget Master", reward.name)
        assertEquals("Stay within budget", reward.description)
        assertEquals(100, reward.experiencePoints)
        assertEquals(123, reward.iconResId)
        assertEquals(RewardType.MONTHLY, reward.type)
        assertFalse(reward.isUnlocked)
        assertFalse(reward.claimed)
    }

    @Test
    fun `test Reward default values`() {
        val reward = Reward(2, "Test", "desc", 50, 0, RewardType.SEASONAL)
        assertFalse(reward.isUnlocked)
        assertFalse(reward.claimed)
    }

    @Test
    fun `test Reward equality`() {
        val r1 = Reward(1, "A", "desc", 10, 0, RewardType.MONTHLY)
        val r2 = Reward(1, "A", "desc", 10, 0, RewardType.MONTHLY)
        val r3 = Reward(2, "B", "desc2", 20, 1, RewardType.SEASONAL)
        assertEquals(r1, r2)
        assertNotEquals(r1, r3)
    }

    @Test
    fun `test RewardType enum values`() {
        val values = RewardType.values()
        assertTrue(values.contains(RewardType.MONTHLY))
        assertTrue(values.contains(RewardType.SEASONAL))
    }

    @Test
    fun `test Reward property changes`() {
        val reward = Reward(3, "C", "desc", 30, 2, RewardType.SEASONAL)
        reward.isUnlocked = true
        reward.claimed = true
        assertTrue(reward.isUnlocked)
        assertTrue(reward.claimed)
    }
} 