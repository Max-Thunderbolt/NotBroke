package com.example.notbroke.models

data class CategoryGoalDisplayItem(
    val categoryName: String,
    val currentSpend: Double,
    val monthlyLimit: Double,
    val progress: Int // Percentage from 0 to 100, can be >100 if over budget
)