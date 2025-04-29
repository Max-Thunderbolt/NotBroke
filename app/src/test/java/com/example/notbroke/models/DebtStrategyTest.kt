package com.example.notbroke.models

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.*

class DebtStrategyTest {
    private lateinit var debts: List<Debt>
    private lateinit var strategy: DebtStrategy

    @Before
    fun setup() {
        debts = listOf(
            Debt(id = "1", name = "Loan1", totalAmount = 1000.0, amountPaid = 200.0, interestRate = 10.0, monthlyPayment = 100.0),
            Debt(id = "2", name = "Loan2", totalAmount = 2000.0, amountPaid = 500.0, interestRate = 5.0, monthlyPayment = 150.0),
            Debt(id = "3", name = "Loan3", totalAmount = 1500.0, amountPaid = 1000.0, interestRate = 15.0, monthlyPayment = 200.0)
        )
        strategy = DebtStrategy()
    }

    @Test
    fun `test DebtStrategyType enum values`() {
        val values = DebtStrategyType.values()
        assertTrue(values.contains(DebtStrategyType.AVALANCHE))
        assertTrue(values.contains(DebtStrategyType.SNOWBALL))
    }

    @Test
    fun `test applyStrategy avalanche`() {
        val result = strategy.applyStrategy(debts, DebtStrategyType.AVALANCHE)
        assertEquals(3, result.size)
        assertEquals("Loan3", result[0].name) // Highest interest first
    }

    @Test
    fun `test applyStrategy snowball`() {
        val result = strategy.applyStrategy(debts, DebtStrategyType.SNOWBALL)
        assertEquals(3, result.size)
        assertEquals("Loan1", result[0].name) // Smallest balance first
    }

    @Test
    fun `test calculateEstimatedPayoffDate returns future date`() {
        val payoffDate = strategy.calculateEstimatedPayoffDate(debts, DebtStrategyType.AVALANCHE)
        assertTrue(payoffDate.after(Date(System.currentTimeMillis() - 1000)))
    }

    @Test
    fun `test calculateEstimatedPayoffDate with empty debts returns now`() {
        val payoffDate = strategy.calculateEstimatedPayoffDate(emptyList(), DebtStrategyType.AVALANCHE)
        assertTrue(Math.abs(payoffDate.time - Date().time) < 10000) // within 10 seconds
    }

    @Test
    fun `test calculateConsolidationSavings returns positive value`() {
        val savings = strategy.calculateConsolidationSavings(debts)
        assertTrue(savings >= 0.0)
    }

    @Test
    fun `test getStrategyDescription returns non-empty`() {
        for (type in DebtStrategyType.values()) {
            val desc = strategy.getStrategyDescription(type)
            assertTrue(desc.isNotBlank())
        }
    }

    @Test
    fun `test DebtStrategy singleton instance`() {
        val instance1 = DebtStrategy.getInstance()
        val instance2 = DebtStrategy.getInstance()
        assertSame(instance1, instance2)
    }
} 