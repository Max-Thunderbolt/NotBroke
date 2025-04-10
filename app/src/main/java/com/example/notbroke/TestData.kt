package com.example.notbroke

import android.util.Log
import com.example.notbroke.models.Transaction
import java.util.*
import com.example.notbroke.fragments.BudgetFragment.BudgetCategory

/**
 * Utility class to generate test data for the app
 */
object TestData {
    private const val TAG = "TestData"
    
    /**
     * Creates and returns a list of sample transactions
     */
    fun getSampleTransactions(): List<Transaction> {
        return listOf(
            Transaction(
                id = 1,
                category = "Salary",
                amount = 15000.0,
                type = Transaction.Type.INCOME,
                description = "March 2024 Salary",
                date = System.currentTimeMillis()
            ),
            Transaction(
                id = 2,
                category = "Groceries",
                amount = 2500.0,
                type = Transaction.Type.EXPENSE,
                description = "Monthly groceries",
                date = System.currentTimeMillis() - 86400000, // 1 day ago
                receiptImageUri = "content://sample/grocery_receipt.jpg" // Sample receipt URI
            ),
            Transaction(
                id = 3,
                category = "Rent",
                amount = 5000.0,
                type = Transaction.Type.EXPENSE,
                description = "March Rent",
                date = System.currentTimeMillis() - 172800000 // 2 days ago
            ),
            Transaction(
                id = 4,
                category = "Freelance",
                amount = 3000.0,
                type = Transaction.Type.INCOME,
                description = "Web development project",
                date = System.currentTimeMillis() - 259200000 // 3 days ago
            ),
            Transaction(
                id = 5,
                category = "Internet",
                amount = 899.0,
                type = Transaction.Type.EXPENSE,
                description = "Internet bill",
                date = System.currentTimeMillis() - 345600000, // 4 days ago
                receiptImageUri = "content://sample/internet_bill.jpg" // Sample receipt URI
            )
        )
    }
    
    /**
     * Creates and returns a list of sample budget categories
     */
    fun getSampleBudgetCategories(): List<BudgetCategory> {
        return listOf(
            BudgetCategory("Groceries", 3000.0, 2500.0),
            BudgetCategory("Rent", 5000.0, 5000.0),
            BudgetCategory("Transport", 1500.0, 800.0),
            BudgetCategory("Entertainment", 1000.0, 500.0),
            BudgetCategory("Utilities", 2000.0, 1500.0)
        )
    }
}

data class BudgetCategory(
    val name: String,
    var budgetAmount: Double,
    var spentAmount: Double
) {
    val percentUsed: Double
        get() = if (budgetAmount > 0) (spentAmount / budgetAmount) * 100 else 0.0
} 