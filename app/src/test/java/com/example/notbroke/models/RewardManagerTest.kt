package com.example.notbroke.models

import com.example.notbroke.rewards.RewardManager
import org.junit.Assert.*
import org.junit.Test
import java.util.*

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
        // Create a calendar instance for the current month
        val calendar = Calendar.getInstance()
        val currentMonth = calendar.get(Calendar.MONTH)
        val currentYear = calendar.get(Calendar.YEAR)
        
        // Create transactions for the current month that meet the criteria for the "Budget Master" reward
        val transactions = mutableListOf<Transaction>()
        
        // Add income transaction
        transactions.add(createTransaction(Transaction.Type.INCOME, 2000.0, calendar.timeInMillis))
        
        // Add expense transactions that stay within budget (500.0)
        for (i in 0 until 5) {
            calendar.add(Calendar.DAY_OF_MONTH, 1)
            transactions.add(createTransaction(Transaction.Type.EXPENSE, 50.0, calendar.timeInMillis))
        }
        
        val rewards = RewardManager.checkEligibleRewards(transactions, currentMonth, currentYear, 500.0, 100.0)
        
        // Verify that we got at least the Budget Master reward
        assertTrue("Should have at least one reward", rewards.isNotEmpty())
        assertTrue("Should have Budget Master reward", rewards.any { it.name == "Budget Master" })
    }

    @Test
    fun `test checkEligibleRewards returns empty when no criteria met`() {
        val calendar = Calendar.getInstance()
        val currentMonth = calendar.get(Calendar.MONTH)
        val currentYear = calendar.get(Calendar.YEAR)
        
        // Create transactions that exceed budget
        val transactions = listOf(
            createTransaction(Transaction.Type.EXPENSE, 2000.0, calendar.timeInMillis),
            createTransaction(Transaction.Type.INCOME, 1000.0, calendar.timeInMillis)
        )
        
        val rewards = RewardManager.checkEligibleRewards(transactions, currentMonth, currentYear, 100.0, 10000.0)
        assertTrue("Should have no rewards when criteria not met", rewards.isEmpty())
    }
} 