package com.example.notbroke.models

// *** Ensure this is the only definition of Transaction data class in your project ***
data class Transaction(
    val id: Long = 0L, // Local ID (can keep if needed, but Firestore ID is primary)
    val firestoreId: String? = null, // *** ADDED: Firestore document ID ***
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