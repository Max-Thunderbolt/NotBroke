package com.example.notbroke.models

import java.util.Date

data class Season(
    val id: Int,
    val name: String,
    val startDate: Date,
    val endDate: Date,
    val rewards: List<Reward>,
    var currentExperience: Int = 0,
    val maxExperience: Int = 10000
) {
    fun getProgress(): Float = currentExperience.toFloat() / maxExperience.toFloat()
    
    fun isActive(): Boolean {
        val now = Date()
        return now in startDate..endDate
    }
    
    fun getUnlockedRewards(): List<Reward> = rewards.filter { it.isUnlocked }
    
    fun getNextReward(): Reward? = rewards.firstOrNull { !it.isUnlocked }
    
    fun addExperience(amount: Int) {
        currentExperience = (currentExperience + amount).coerceAtMost(maxExperience)
        updateRewards()
    }
    
    private fun updateRewards() {
        rewards.forEach { reward ->
            if (!reward.isUnlocked && currentExperience >= reward.experiencePoints) {
                reward.isUnlocked = true
            }
        }
    }
} 