package com.example.notbroke.models

import com.example.notbroke.rewards.RewardManager
import org.junit.Assert.*
import org.junit.Test

class RewardManagerTest {
    private fun createTransaction(type: Transaction.Type, amount: Double, date: Long): Transaction {
        return Transaction(
            id = 0L,
            firestoreId = null,
            type = type,
            amount = amount,
            description = "desc",
            category = "cat",
            date = date,
            receiptImageUri = null
        )
    }

    @Test
    fun `test checkEligibleRewards returns expected rewards`() {
        val now = System.currentTimeMillis()
        val transactions = (0 until 30).map {
            createTransaction(Transaction.Type.EXPENSE, 10.0, now - it * 24 * 60 * 60 * 1000)
        } + createTransaction(Transaction.Type.INCOME, 1000.0, now)
        val rewards = RewardManager.checkEligibleRewards(transactions, 0, 2024, 500.0, 100.0)
        assertTrue(rewards.isNotEmpty())
    }

    @Test
    fun `test checkEligibleRewards returns empty when no criteria met`() {
        val now = System.currentTimeMillis()
        val transactions = listOf(
            createTransaction(Transaction.Type.EXPENSE, 1000.0, now),
            createTransaction(Transaction.Type.INCOME, 1000.0, now)
        )
        val rewards = RewardManager.checkEligibleRewards(transactions, 0, 2024, 100.0, 10000.0)
        assertTrue(rewards.isEmpty())
    }
} 