package com.example.notbroke.models

import org.junit.Assert.*
import org.junit.Test

class UserPreferencesTest {
    @Test
    fun `test UserPreferences creation with valid data`() {
        val prefs = UserPreferences(
            userId = "user1",
            selectedDebtStrategy = DebtStrategyType.SNOWBALL,
            lastUpdated = 123456789L
        )
        assertEquals("user1", prefs.userId)
        assertEquals(DebtStrategyType.SNOWBALL, prefs.selectedDebtStrategy)
        assertEquals(123456789L, prefs.lastUpdated)
    }

    @Test
    fun `test UserPreferences default values`() {
        val prefs = UserPreferences(userId = "user2")
        assertEquals(DebtStrategyType.AVALANCHE, prefs.selectedDebtStrategy)
        assertTrue(prefs.lastUpdated > 0)
    }

    @Test
    fun `test UserPreferences no-arg constructor`() {
        val prefs = UserPreferences()
        assertEquals("", prefs.userId)
        assertEquals(DebtStrategyType.AVALANCHE, prefs.selectedDebtStrategy)
        assertTrue(prefs.lastUpdated > 0)
    }

    @Test
    fun `test setSelectedDebtStrategyFromString with valid value`() {
        val prefs = UserPreferences(userId = "user3")
        prefs.setSelectedDebtStrategyFromString("SNOWBALL")
        assertEquals(DebtStrategyType.SNOWBALL, prefs.selectedDebtStrategy)
    }

    @Test
    fun `test setSelectedDebtStrategyFromString with invalid value defaults to AVALANCHE`() {
        val prefs = UserPreferences(userId = "user4")
        prefs.setSelectedDebtStrategyFromString("INVALID")
        assertEquals(DebtStrategyType.AVALANCHE, prefs.selectedDebtStrategy)
    }
} 