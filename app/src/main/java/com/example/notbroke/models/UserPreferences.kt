package com.example.notbroke.models

/**
 * Data class to store user preferences in Firestore
 */
data class UserPreferences(
    val userId: String,
    var selectedDebtStrategy: DebtStrategyType = DebtStrategyType.AVALANCHE,
    val lastUpdated: Long = System.currentTimeMillis()
) {
    // No-argument constructor for Firestore
    constructor() : this(
        userId = "",
        selectedDebtStrategy = DebtStrategyType.AVALANCHE,
        lastUpdated = System.currentTimeMillis()
    )
    
    // Helper method to safely convert string to enum
    fun setSelectedDebtStrategyFromString(strategyName: String) {
        try {
            selectedDebtStrategy = DebtStrategyType.valueOf(strategyName)
        } catch (e: IllegalArgumentException) {
            // Default to AVALANCHE if the string doesn't match any enum value
            selectedDebtStrategy = DebtStrategyType.AVALANCHE
        }
    }
} 