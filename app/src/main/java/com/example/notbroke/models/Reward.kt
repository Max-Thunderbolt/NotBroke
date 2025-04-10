package com.example.notbroke.models

data class Reward(
    val id: Int,
    val name: String,
    val description: String,
    val experiencePoints: Int,
    val iconResId: Int, // Resource ID for the reward icon
    val type: RewardType,
    var isUnlocked: Boolean = false,
    var claimed: Boolean = false
)

enum class RewardType {
    MONTHLY,    // Monthly rewards (easier to obtain)
    SEASONAL    // Quarterly rewards (harder to obtain)
} 