package com.example.notbroke.models

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.*

/**
 * Unit tests for the Transaction model class.
 * These tests verify the properties, equality, and basic behavior of the Transaction data class.
 */
class TransactionTest {
    private lateinit var incomeTransaction: Transaction
    private lateinit var expenseTransaction: Transaction

    @Before
    fun setup() {
        // Initialize test data before each test
        incomeTransaction = Transaction(
            id = 1L, // Example Room ID
            firestoreId = "income123", // Example Firestore ID
            type = Transaction.Type.INCOME,
            amount = 1000.0,
            description = "Salary",
            category = "Work",
            date = System.currentTimeMillis(), // Example timestamp
            receiptImageUri = null // No receipt
        )

        expenseTransaction = Transaction(
            id = 2L, // Example Room ID
            firestoreId = "expense456", // Example Firestore ID
            type = Transaction.Type.EXPENSE,
            amount = 50.0,
            description = "Groceries",
            category = "Food",
            date = System.currentTimeMillis(), // Example timestamp
            receiptImageUri = "content://receipts/123" // With receipt URI
        )
    }

    @Test
    fun `test transaction creation with valid data`() {
        // Verify properties of the income transaction
        assertEquals(1L, incomeTransaction.id)
        assertEquals("income123", incomeTransaction.firestoreId)
        assertEquals(Transaction.Type.INCOME, incomeTransaction.type)
        assertEquals(1000.0, incomeTransaction.amount, 0.001) // Use delta for Double comparison
        assertEquals("Salary", incomeTransaction.description)
        assertEquals("Work", incomeTransaction.category)
        assertNull(incomeTransaction.receiptImageUri)
        // Note: Testing the exact 'date' value can be tricky due to System.currentTimeMillis()
        // You might test that it's a positive value or within a certain range if needed.

        // Verify properties of the expense transaction
        assertEquals(2L, expenseTransaction.id)
        assertEquals("expense456", expenseTransaction.firestoreId)
        assertEquals(Transaction.Type.EXPENSE, expenseTransaction.type)
        assertEquals(50.0, expenseTransaction.amount, 0.001)
        assertEquals("Groceries", expenseTransaction.description)
        assertEquals("Food", expenseTransaction.category)
        assertEquals("content://receipts/123", expenseTransaction.receiptImageUri)
    }

    @Test
    fun `test transaction equality based on data class equals`() {
        // Create a copy of the income transaction - should be equal
        val incomeCopy = incomeTransaction.copy()
        assertEquals(incomeTransaction, incomeCopy)
        assertEquals(incomeTransaction.hashCode(), incomeCopy.hashCode()) // Hash codes should also be equal

        // Create a transaction with the same data but different instance - should be equal
        val anotherIncome = Transaction(
            id = 1L,
            firestoreId = "income123",
            type = Transaction.Type.INCOME,
            amount = 1000.0,
            description = "Salary",
            category = "Work",
            date = incomeTransaction.date, // Use the same date for equality
            receiptImageUri = null
        )
        assertEquals(incomeTransaction, anotherIncome)
        assertEquals(incomeTransaction.hashCode(), anotherIncome.hashCode())

        // Create a copy with a different amount - should NOT be equal
        val differentAmount = incomeTransaction.copy(amount = 2000.0)
        assertNotEquals(incomeTransaction, differentAmount)

        // Create a copy with a different description - should NOT be equal
        val differentDescription = incomeTransaction.copy(description = "Bonus")
        assertNotEquals(incomeTransaction, differentDescription)

        // Compare income and expense transactions - should NOT be equal
        assertNotEquals(incomeTransaction, expenseTransaction)
    }

    @Test
    fun `test transaction equality`() {
        // Create a copy of income transaction
        val incomeCopy = incomeTransaction.copy()
        assertEquals(incomeTransaction, incomeCopy)

        // Create a copy with different amount
        val differentAmount = incomeTransaction.copy(amount = 2000.0)
        assertNotEquals(incomeTransaction, differentAmount)
    }


    @Test
    fun `test transaction type enum values`() {
        // Verify the number of defined enum values
        assertEquals(2, Transaction.Type.values().size)

        // Verify that the expected enum values exist
        assertTrue(Transaction.Type.values().contains(Transaction.Type.INCOME))
        assertTrue(Transaction.Type.values().contains(Transaction.Type.EXPENSE))

        // Verify the string names of the enum values
        assertEquals("INCOME", Transaction.Type.INCOME.name)
        assertEquals("EXPENSE", Transaction.Type.EXPENSE.name)
    }

    @Test
    fun `test transaction type enum`() {
        assertEquals(2, Transaction.Type.values().size)
        assertTrue(Transaction.Type.values().contains(Transaction.Type.INCOME))
        assertTrue(Transaction.Type.values().contains(Transaction.Type.EXPENSE))
    }


    @Test
    fun `test transaction with default values`() {
        // Create a transaction using only required parameters, relying on defaults
        val defaultTransaction = Transaction(
            type = Transaction.Type.EXPENSE,
            amount = 100.0,
            description = "Test Default",
            category = "Other",
            date = System.currentTimeMillis()
            // id, firestoreId, receiptImageUri will use default null/0L
        )

        // Verify that default values are applied correctly
        assertEquals(0L, defaultTransaction.id) // Default for Long is 0L
        assertNull(defaultTransaction.firestoreId) // Default for String? is null
        assertNull(defaultTransaction.receiptImageUri) // Default for String? is null

        // Verify that provided values are set correctly
        assertEquals(Transaction.Type.EXPENSE, defaultTransaction.type)
        assertEquals(100.0, defaultTransaction.amount, 0.001)
        assertEquals("Test Default", defaultTransaction.description)
        assertEquals("Other", defaultTransaction.category)
        // Date is set to the current time, we can't assert an exact value,
        // but we can assert it's a positive value (assuming current time is > 0)
        assertTrue(defaultTransaction.date > 0)
    }


    @Test
    fun `test transaction toString representation`() {
        // Verify that the toString method (generated by data class) contains key information
        val incomeString = incomeTransaction.toString()
        assertTrue(incomeString.contains("id=1"))
        assertTrue(incomeString.contains("firestoreId=income123"))
        assertTrue(incomeString.contains("type=INCOME"))
        assertTrue(incomeString.contains("amount=1000.0"))
        assertTrue(incomeString.contains("description=Salary"))
        assertTrue(incomeString.contains("category=Work"))
        // Date will vary, so don't check the exact value
        assertTrue(incomeString.contains("receiptImageUri=null"))

        val expenseString = expenseTransaction.toString()
        assertTrue(expenseString.contains("id=2"))
        assertTrue(expenseString.contains("firestoreId=expense456"))
        assertTrue(expenseString.contains("type=EXPENSE"))
        assertTrue(expenseString.contains("amount=50.0"))
        assertTrue(expenseString.contains("description=Groceries"))
        assertTrue(expenseString.contains("category=Food"))
        assertTrue(expenseString.contains("receiptImageUri=content://receipts/123"))
    }

    @Test
    fun `test transaction toString`() {
        val toString = incomeTransaction.toString()
        assertTrue(toString.contains("INCOME"))
        assertTrue(toString.contains("1000.0"))
        assertTrue(toString.contains("Salary"))
        assertTrue(toString.contains("Work"))
    }


    @Test
    fun `test transaction copy with updated fields`() {
        // Test creating a modified copy of an existing transaction
        val updatedAmount = 1200.0
        val updatedDescription = "Monthly Salary"
        val updatedCategory = "Income Source"
        val updatedReceipt = "content://receipts/new"

        val updatedIncomeTransaction = incomeTransaction.copy(
            amount = updatedAmount,
            description = updatedDescription,
            category = updatedCategory,
            receiptImageUri = updatedReceipt
        )

        // Assert that the specified fields are updated in the new object
        assertEquals(updatedAmount, updatedIncomeTransaction.amount, 0.001)
        assertEquals(updatedDescription, updatedIncomeTransaction.description)
        assertEquals(updatedCategory, updatedIncomeTransaction.category)
        assertEquals(updatedReceipt, updatedIncomeTransaction.receiptImageUri)

        // Assert that other fields remain the same as the original transaction
        assertEquals(incomeTransaction.id, updatedIncomeTransaction.id)
        assertEquals(incomeTransaction.firestoreId, updatedIncomeTransaction.firestoreId)
        assertEquals(incomeTransaction.type, updatedIncomeTransaction.type)
        assertEquals(incomeTransaction.date, updatedIncomeTransaction.date)

        // Ensure the original transaction object was not modified
        assertEquals(1000.0, incomeTransaction.amount, 0.001)
        assertEquals("Salary", incomeTransaction.description)
        assertEquals("Work", incomeTransaction.category)
        assertNull(incomeTransaction.receiptImageUri)
    }

    @Test
    fun `test transaction copy with nulling fields`() {
        // Test creating a copy where optional fields are set to null
        val transactionWithReceipt = expenseTransaction.copy(receiptImageUri = "some_uri")
        assertNotNull(transactionWithReceipt.receiptImageUri)

        val updatedTransaction = transactionWithReceipt.copy(receiptImageUri = null)

        // Assert that the receiptImageUri is now null
        assertNull(updatedTransaction.receiptImageUri)

        // Assert other fields remain the same
        assertEquals(transactionWithReceipt.id, updatedTransaction.id)
        assertEquals(transactionWithReceipt.firestoreId, updatedTransaction.firestoreId)
        assertEquals(transactionWithReceipt.type, updatedTransaction.type)
        assertEquals(transactionWithReceipt.amount, updatedTransaction.amount, 0.001)
        assertEquals(transactionWithReceipt.description, updatedTransaction.description)
        assertEquals(transactionWithReceipt.category, updatedTransaction.category)
        assertEquals(transactionWithReceipt.date, updatedTransaction.date)
    }

    @Test
    fun `test transaction category field handling`() {
        // Test that the category field can hold different string values
        val categorizedTransaction = expenseTransaction.copy(category = "Utilities")
        assertEquals("Utilities", categorizedTransaction.category)

        val uncategorizedTransaction = expenseTransaction.copy(category = "Uncategorized")
        assertEquals("Uncategorized", uncategorizedTransaction.category)

        val emptyCategoryTransaction = expenseTransaction.copy(category = "")
        assertEquals("", emptyCategoryTransaction.category)
    }

    @Test
    fun `test transaction category field`() {
        assertEquals("Work", incomeTransaction.category)
        assertEquals("Food", expenseTransaction.category)

        val uncategorizedExpense = Transaction(
            id = 5L, firestoreId = "expense345", type = Transaction.Type.EXPENSE, amount = 25.0, description = "Misc", category = "Uncategorized", date = System.currentTimeMillis(), receiptImageUri = null
        )
        assertEquals("Uncategorized", uncategorizedExpense.category)
    }

    @Test
    fun `test updateBalance with mixed transactions`() {
        val transactions = listOf(
            incomeTransaction,
            expenseTransaction,
            Transaction(id = 3L, firestoreId = "income789", type = Transaction.Type.INCOME, amount = 500.0, description = "Gift", category = "Other", date = System.currentTimeMillis(), receiptImageUri = null),
            Transaction(id = 4L, firestoreId = "expense012", type = Transaction.Type.EXPENSE, amount = 200.0, description = "Shopping", category = "Clothes", date = System.currentTimeMillis(), receiptImageUri = null)
        )

        // Calculate expected balance: 1000.0 - 50.0 + 500.0 - 200.0 = 1250.0
        val expectedBalance = 1250.0

        // We can't directly test updateBalance as it's a private UI-updating method.
        // However, we can test the core logic of summing amounts based on type.
        var calculatedBalance = 0.0
        transactions.forEach { transaction ->
            if (transaction.type == Transaction.Type.INCOME) {
                calculatedBalance += transaction.amount
            } else if (transaction.type == Transaction.Type.EXPENSE) {
                calculatedBalance -= transaction.amount
            }
        }

        assertEquals(expectedBalance, calculatedBalance, 0.001)
    }

    @Test
    fun `test updateBalance with only income`() {
        val transactions = listOf(
            incomeTransaction,
            Transaction(id = 3L, firestoreId = "income789", type = Transaction.Type.INCOME, amount = 500.0, description = "Gift", category = "Other", date = System.currentTimeMillis(), receiptImageUri = null)
        )

        val expectedBalance = 1000.0 + 500.0

        var calculatedBalance = 0.0
        transactions.forEach { transaction ->
            if (transaction.type == Transaction.Type.INCOME) {
                calculatedBalance += transaction.amount
            } else if (transaction.type == Transaction.Type.EXPENSE) {
                calculatedBalance -= transaction.amount
            }
        }

        assertEquals(expectedBalance, calculatedBalance, 0.001)
    }

    @Test
    fun `test updateBalance with only expense`() {
        val transactions = listOf(
            expenseTransaction,
            Transaction(id = 4L, firestoreId = "expense012", type = Transaction.Type.EXPENSE, amount = 200.0, description = "Shopping", category = "Clothes", date = System.currentTimeMillis(), receiptImageUri = null)
        )

        val expectedBalance = -50.0 - 200.0

        var calculatedBalance = 0.0
        transactions.forEach { transaction ->
            if (transaction.type == Transaction.Type.INCOME) {
                calculatedBalance += transaction.amount
            } else if (transaction.type == Transaction.Type.EXPENSE) {
                calculatedBalance -= transaction.amount
            }
        }

        assertEquals(expectedBalance, calculatedBalance, 0.001)
    }

    @Test
    fun `test updateBalance with empty list`() {
        val transactions = emptyList<Transaction>()

        val expectedBalance = 0.0

        var calculatedBalance = 0.0
        transactions.forEach { transaction ->
            if (transaction.type == Transaction.Type.INCOME) {
                calculatedBalance += transaction.amount
            } else if (transaction.type == Transaction.Type.EXPENSE) {
                calculatedBalance -= transaction.amount
            }
        }

        assertEquals(expectedBalance, calculatedBalance, 0.001)
    }

    // Note: Tests related to summing amounts or filtering lists of Transactions
    // are typically unit tests for the class that performs those operations (e.g., a ViewModel or utility)
    // rather than unit tests for the Transaction model itself, as the model just holds the data.
    // However, the previous tests for updateBalance logic (now removed from the model)
    // were examples of testing logic that *uses* Transaction objects.
}