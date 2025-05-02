package com.example.notbroke.models

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.Exclude

/**
 * Data class representing a category for transactions
 */
data class Category(
    val id: Long = 0L, // Local ID
    val firestoreId: String? = null, // Firestore document ID
    val userId: String,
    val categoryName: String,
    val categoryType: Type,
    val monthLimit: Double? = null,
    val keyword: String? = null // Keywords for auto-categorization
) {
    enum class Type {
        INCOME, EXPENSE
    }
}