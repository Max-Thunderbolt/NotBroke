package com.example.notbroke

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.notbroke.models.Debt
import com.example.notbroke.models.DebtStrategy
import com.example.notbroke.models.DebtStrategyType
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.*

/**
 * Integration tests for the debt strategy functionality.
 * These tests verify that the debt strategies work correctly in the UI context.
 */
@RunWith(AndroidJUnit4::class)
class DebtStrategyIntegrationTest {
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
        strategy = DebtStrategy.getInstance()
    }
    
    /**
     * Test that the debt strategy singleton works correctly
     */
    @Test
    fun testDebtStrategySingleton() {
        val instance1 = DebtStrategy.getInstance()
        val instance2 = DebtStrategy.getInstance()
        assertSame("DebtStrategy should be a singleton", instance1, instance2)
    }
    
    /**
     * Test that all strategy types are supported
     */
    @Test
    fun testAllStrategyTypesSupported() {
        for (type in DebtStrategyType.values()) {
            val result = strategy.applyStrategy(debts, type)
            assertNotNull("Strategy result should not be null for $type", result)
            assertTrue("Strategy result should not be empty for $type", result.isNotEmpty())
        }
    }
    
    /**
     * Test that strategy descriptions are provided for all types
     */
    @Test
    fun testStrategyDescriptions() {
        for (type in DebtStrategyType.values()) {
            val description = strategy.getStrategyDescription(type)
            assertNotNull("Description should not be null for $type", description)
            assertTrue("Description should not be empty for $type", description.isNotBlank())
        }
    }
    
    /**
     * Test that payoff date calculation works for all strategies
     */
    @Test
    fun testPayoffDateCalculation() {
        for (type in DebtStrategyType.values()) {
            val payoffDate = strategy.calculateEstimatedPayoffDate(debts, type)
            assertNotNull("Payoff date should not be null for $type", payoffDate)
            assertTrue("Payoff date should be in the future for $type", payoffDate.after(Date()))
        }
    }
    
    /**
     * Test that consolidation savings calculation works
     */
    @Test
    fun testConsolidationSavings() {
        val savings = strategy.calculateConsolidationSavings(debts)
        assertNotNull("Consolidation savings should not be null", savings)
        assertTrue("Consolidation should result in savings", savings >= 0.0)
    }
    
    /**
     * Test that the snowball method correctly identifies the smallest debt
     */
    @Test
    fun testSnowballMethodIdentifiesSmallestDebt() {
        val result = strategy.applyStrategy(debts, DebtStrategyType.SNOWBALL)
        val smallestDebt = debts.minByOrNull { it.getRemainingBalance() }
        assertEquals("Smallest debt should be first in snowball method", 
            smallestDebt?.name, result[0].name)
    }
    
    /**
     * Test that the avalanche method correctly identifies the highest interest debt
     */
    @Test
    fun testAvalancheMethodIdentifiesHighestInterestDebt() {
        val result = strategy.applyStrategy(debts, DebtStrategyType.AVALANCHE)
        val highestInterestDebt = debts.maxByOrNull { it.interestRate }
        assertEquals("Highest interest debt should be first in avalanche method", 
            highestInterestDebt?.name, result[0].name)
    }
    
    /**
     * Test that the balance proportion method distributes payments correctly
     */
    @Test
    fun testBalanceProportionDistributesPaymentsCorrectly() {
        val extraPayment = 1000.0
        val result = strategy.applyStrategy(debts, DebtStrategyType.BALANCE_PROPORTION, extraPayment)
        
        // Calculate expected proportions
        val totalDebt = debts.sumOf { it.getRemainingBalance() }
        val creditCardProportion = debts[0].getRemainingBalance() / totalDebt
        val carLoanProportion = debts[1].getRemainingBalance() / totalDebt
        val studentLoanProportion = debts[2].getRemainingBalance() / totalDebt
        
        // Verify extra payments were distributed proportionally
        val creditCardExtra = result[0].monthlyPayment - debts[0].monthlyPayment
        val carLoanExtra = result[1].monthlyPayment - debts[1].monthlyPayment
        val studentLoanExtra = result[2].monthlyPayment - debts[2].monthlyPayment
        
        assertEquals(extraPayment * creditCardProportion, creditCardExtra, 0.01)
        assertEquals(extraPayment * carLoanProportion, carLoanExtra, 0.01)
        assertEquals(extraPayment * studentLoanProportion, studentLoanExtra, 0.01)
    }
} 