package com.example.notbroke.models

data class Transaction(
    val id: Long = 0L,
    val firestoreId: String? = null,
    val userId: String = "",
    val type: Type,
    val amount: Double,
    val description: String,
    val category: String,
    val date: Long, // Timestamp in milliseconds
    val receiptImageUri: String? = null // URI as a String
) {
    enum class Type {
        INCOME, EXPENSE
    }
}