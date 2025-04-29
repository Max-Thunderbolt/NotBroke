package com.example.notbroke.models

import org.junit.Test
import org.junit.Assert.*
import org.junit.Before
import java.util.*

class TransactionTest {
    private lateinit var incomeTransaction: Transaction
    private lateinit var expenseTransaction: Transaction
    
    @Before
    fun setup() {
        // Initialize test data before each test
        incomeTransaction = Transaction(
            id = 1L,
            firestoreId = "income123",
            type = Transaction.Type.INCOME,
            amount = 1000.0,
            description = "Salary",
            category = "Work",
            date = System.currentTimeMillis(),
            receiptImageUri = null
        )
        
        expenseTransaction = Transaction(
            id = 2L,
            firestoreId = "expense456",
            type = Transaction.Type.EXPENSE,
            amount = 50.0,
            description = "Groceries",
            category = "Food",
            date = System.currentTimeMillis(),
            receiptImageUri = "content://receipts/123"
        )
    }
    
    @Test
    fun `test transaction creation with valid data`() {
        // Test income transaction
        assertEquals(1L, incomeTransaction.id)
        assertEquals("income123", incomeTransaction.firestoreId)
        assertEquals(Transaction.Type.INCOME, incomeTransaction.type)
        assertEquals(1000.0, incomeTransaction.amount, 0.001)
        assertEquals("Salary", incomeTransaction.description)
        assertEquals("Work", incomeTransaction.category)
        assertNull(incomeTransaction.receiptImageUri)
        
        // Test expense transaction
        assertEquals(2L, expenseTransaction.id)
        assertEquals("expense456", expenseTransaction.firestoreId)
        assertEquals(Transaction.Type.EXPENSE, expenseTransaction.type)
        assertEquals(50.0, expenseTransaction.amount, 0.001)
        assertEquals("Groceries", expenseTransaction.description)
        assertEquals("Food", expenseTransaction.category)
        assertEquals("content://receipts/123", expenseTransaction.receiptImageUri)
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
    fun `test transaction type enum`() {
        assertEquals(2, Transaction.Type.values().size)
        assertTrue(Transaction.Type.values().contains(Transaction.Type.INCOME))
        assertTrue(Transaction.Type.values().contains(Transaction.Type.EXPENSE))
    }
    
    @Test
    fun `test transaction with default values`() {
        val defaultTransaction = Transaction(
            type = Transaction.Type.INCOME,
            amount = 100.0,
            description = "Test",
            category = "Test",
            date = System.currentTimeMillis()
        )
        
        assertEquals(0L, defaultTransaction.id)
        assertNull(defaultTransaction.firestoreId)
        assertNull(defaultTransaction.receiptImageUri)
    }
    
    @Test
    fun `test transaction toString`() {
        val toString = incomeTransaction.toString()
        assertTrue(toString.contains("INCOME"))
        assertTrue(toString.contains("1000.0"))
        assertTrue(toString.contains("Salary"))
        assertTrue(toString.contains("Work"))
    }
} 