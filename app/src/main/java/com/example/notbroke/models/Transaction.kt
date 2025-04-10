package com.example.notbroke.models

import android.net.Uri

data class Transaction(
    val id: Long,
    val amount: Double,
    val type: Type,
    val description: String,
    val category: String,
    val date: Long,
    val receiptImageUri: String? = null  // Optional receipt image URI stored as String
) {
    enum class Type {
        INCOME, EXPENSE
    }
} 