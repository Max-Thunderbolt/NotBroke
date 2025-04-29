package com.example.notbroke.models

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.*

/**
 * Regression tests for the debt management functionality.
 * These tests verify that the entire debt management workflow functions correctly.
 */
class DebtRegressionTest {
    private lateinit var debts: List<Debt>
    private lateinit var strategy: DebtStrategy
    
    @Before
    fun setup() {
        // Create a realistic set of debts for testing
        debts = listOf(
            Debt(
                id = "1",
                name = "Credit Card",
                totalAmount = 5000.0,
                amountPaid = 1000.0,
                interestRate = 18.0,
                monthlyPayment = 200.0
            ),
            Debt(
                id = "2",
                name = "Car Loan",
                totalAmount = 15000.0,
                amountPaid = 5000.0,
                interestRate = 5.0,
                monthlyPayment = 300.0
            ),
            Debt(
                id = "3",
                name = "Student Loan",
                totalAmount = 25000.0,
                amountPaid = 5000.0,
                interestRate = 4.5,
                monthlyPayment = 250.0
            )
        )
        strategy = DebtStrategy()
    }
    
    /**
     * Test the complete debt management workflow with the Snowball method
     */
    @Test
    fun `test complete debt management workflow with Snowball method`() {
        // 1. Verify initial debt state
        assertEquals(3, debts.size)
        assertEquals(4000.0, debts[0].getRemainingBalance(), 0.01)
        assertEquals(10000.0, debts[1].getRemainingBalance(), 0.01)
        assertEquals(20000.0, debts[2].getRemainingBalance(), 0.01)
        
        // 2. Apply Snowball strategy with extra payment
        val extraPayment = 500.0
        val result = strategy.applyStrategy(debts, DebtStrategyType.SNOWBALL, extraPayment)
        
        // 3. Verify strategy results
        assertEquals(3, result.size)
        assertEquals("Credit Card", result[0].name) // Should be first (smallest balance)
        
        // 4. Verify payments were applied correctly
        // Credit Card: 4000 - 200 (min payment) - 500 (extra) = 3300
        assertEquals(3300.0, result[0].getRemainingBalance(), 0.01)
        // Car Loan: 10000 - 300 (min payment) = 9700
        assertEquals(9700.0, result[1].getRemainingBalance(), 0.01)
        // Student Loan: 20000 - 250 (min payment) = 19750
        assertEquals(19750.0, result[2].getRemainingBalance(), 0.01)
        
        // 5. Calculate and verify payoff date
        val payoffDate = strategy.calculateEstimatedPayoffDate(result, DebtStrategyType.SNOWBALL)
        assertTrue("Payoff date should be in the future", payoffDate.after(Date()))
        
        // 6. Verify strategy description
        val description = strategy.getStrategyDescription(DebtStrategyType.SNOWBALL)
        assertTrue("Description should mention smallest balance", description.contains("smallest balance"))
    }
    
    /**
     * Test the complete debt management workflow with the Avalanche method
     */
    @Test
    fun `test complete debt management workflow with Avalanche method`() {
        // 1. Apply Avalanche strategy with extra payment
        val extraPayment = 500.0
        val result = strategy.applyStrategy(debts, DebtStrategyType.AVALANCHE, extraPayment)
        
        // 2. Verify strategy results
        assertEquals(3, result.size)
        assertEquals("Credit Card", result[0].name) // Should be first (highest interest)
        
        // 3. Verify payments were applied correctly
        // Credit Card: 4000 - 200 (min payment) - 500 (extra) = 3300
        assertEquals(3300.0, result[0].getRemainingBalance(), 0.01)
        // Car Loan: 10000 - 300 (min payment) = 9700
        assertEquals(9700.0, result[1].getRemainingBalance(), 0.01)
        // Student Loan: 20000 - 250 (min payment) = 19750
        assertEquals(19750.0, result[2].getRemainingBalance(), 0.01)
    }
    
    /**
     * Test debt consolidation workflow
     */
    @Test
    fun `test debt consolidation workflow`() {
        // 1. Apply debt consolidation strategy
        val result = strategy.applyStrategy(debts, DebtStrategyType.DEBT_CONSOLIDATION)
        
        // 2. Verify consolidation results
        assertEquals(1, result.size)
        assertEquals("Consolidated Loan", result[0].name)
        
        // 3. Verify total amount matches sum of original debts
        val expectedTotal = debts.sumOf { it.totalAmount }
        assertEquals(expectedTotal, result[0].totalAmount, 0.01)
        
        // 4. Verify interest rate is lower than highest original rate
        val highestOriginalRate = debts.maxOf { it.interestRate }
        assertTrue("Consolidated rate should be lower than highest original rate", 
            result[0].interestRate < highestOriginalRate)
        
        // 5. Calculate and verify consolidation savings
        val savings = strategy.calculateConsolidationSavings(debts)
        assertTrue("Consolidation should result in savings", savings > 0.0)
    }
    
    /**
     * Test balance proportion workflow
     */
    @Test
    fun `test balance proportion workflow`() {
        // 1. Apply balance proportion strategy with extra payment
        val extraPayment = 1000.0
        val result = strategy.applyStrategy(debts, DebtStrategyType.BALANCE_PROPORTION, extraPayment)
        
        // 2. Verify strategy results
        assertEquals(3, result.size)
        
        // 3. Calculate expected proportions
        val totalDebt = debts.sumOf { it.getRemainingBalance() }
        val creditCardProportion = debts[0].getRemainingBalance() / totalDebt
        val carLoanProportion = debts[1].getRemainingBalance() / totalDebt
        val studentLoanProportion = debts[2].getRemainingBalance() / totalDebt
        
        // 4. Verify extra payments were distributed proportionally
        val creditCardExtra = result[0].monthlyPayment - debts[0].monthlyPayment
        val carLoanExtra = result[1].monthlyPayment - debts[1].monthlyPayment
        val studentLoanExtra = result[2].monthlyPayment - debts[2].monthlyPayment
        
        assertEquals(extraPayment * creditCardProportion, creditCardExtra, 0.01)
        assertEquals(extraPayment * carLoanProportion, carLoanExtra, 0.01)
        assertEquals(extraPayment * studentLoanProportion, studentLoanExtra, 0.01)
    }
    
    /**
     * Test debt stacking workflow
     */
    @Test
    fun `test debt stacking workflow`() {
        // 1. Apply debt stacking strategy with extra payment
        val extraPayment = 200.0
        val result = strategy.applyStrategy(debts, DebtStrategyType.DEBT_STACKING, extraPayment)
        
        // 2. Verify strategy results
        assertEquals(3, result.size)
        
        // 3. Find the debt with shortest payoff time (should be first)
        val shortestPayoffDebt = debts.minByOrNull { it.getMonthsRemaining() }
        assertEquals(shortestPayoffDebt?.name, result[0].name)
        
        // 4. Verify extra payment was applied to first debt
        val firstDebtOriginalPayment = shortestPayoffDebt?.monthlyPayment ?: 0.0
        val firstDebtNewPayment = result[0].monthlyPayment
        assertTrue("First debt should receive extra payment", 
            firstDebtNewPayment >= firstDebtOriginalPayment + extraPayment)
    }
} 