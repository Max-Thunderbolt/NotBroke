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
        
        // Find the debt with smallest remaining balance
        val smallestDebt = debts.minByOrNull { it.totalAmount - it.amountPaid }
        assertEquals("Smallest remaining balance debt should be first", 
            smallestDebt?.name, result[0].name)
    }

    @Test
    fun `test applyStrategy snowball with extra payment`() {
        val extraPayment = 500.0
        val result = strategy.applyStrategy(debts, DebtStrategyType.SNOWBALL, extraPayment)
        assertEquals(3, result.size)
        
        // Find the debt with smallest remaining balance
        val smallestDebt = debts.minByOrNull { it.totalAmount - it.amountPaid }
        assertEquals("Smallest remaining balance debt should be first", 
            smallestDebt?.name, result[0].name)
            
        // Calculate expected remaining balance
        val initialRemaining = smallestDebt?.totalAmount?.minus(smallestDebt.amountPaid) ?: 0.0
        val totalPayment = (smallestDebt?.monthlyPayment ?: 0.0) + extraPayment
        val expectedRemaining = (initialRemaining - totalPayment).coerceAtLeast(0.0)
        
        assertEquals("Extra payment should be applied to smallest debt", 
            expectedRemaining, result[0].getRemainingBalance(), 0.01)
    }

    @Test
    fun `test applyStrategy avalanche with extra payment`() {
        val extraPayment = 500.0
        val result = strategy.applyStrategy(debts, DebtStrategyType.AVALANCHE, extraPayment)
        assertEquals(3, result.size)
        // Loan3 should be first as it has highest interest
        assertEquals("Loan3", result[0].name)
        // Verify extra payment was applied to highest interest debt
        assertTrue(result[0].getRemainingBalance() < 500.0)
    }

    @Test
    fun `test applyStrategy debt consolidation`() {
        val result = strategy.applyStrategy(debts, DebtStrategyType.DEBT_CONSOLIDATION)
        assertEquals(1, result.size)
        assertEquals("Consolidated Loan", result[0].name)
        // Verify total amount matches sum of original debts
        assertEquals(4500.0, result[0].totalAmount, 0.01)
    }

    @Test
    fun `test applyStrategy highest interest first`() {
        val result = strategy.applyStrategy(debts, DebtStrategyType.HIGHEST_INTEREST_FIRST)
        assertEquals(3, result.size)
        assertEquals("Loan3", result[0].name)
    }

    @Test
    fun `test applyStrategy balance proportion`() {
        val extraPayment = 1000.0
        val result = strategy.applyStrategy(debts, DebtStrategyType.BALANCE_PROPORTION, extraPayment)
        assertEquals(3, result.size)
        // Verify all debts received proportional extra payments
        val totalDebt = debts.sumOf { it.getRemainingBalance() }
        debts.forEachIndexed { index, debt ->
            val expectedProportion = debt.getRemainingBalance() / totalDebt
            val actualPayment = result[index].monthlyPayment - debt.monthlyPayment
            assertEquals(extraPayment * expectedProportion, actualPayment, 0.01)
        }
    }

    @Test
    fun `test applyStrategy debt stacking`() {
        val extraPayment = 200.0
        val result = strategy.applyStrategy(debts, DebtStrategyType.DEBT_STACKING, extraPayment)
        assertEquals(3, result.size)
        
        // Get the first debt (sorted by months remaining)
        val firstDebt = result[0]
        val originalDebt = debts.minByOrNull { it.getMonthsRemaining() }
        
        // Verify the total payment includes both minimum payment and extra payment
        val expectedPayment = originalDebt?.monthlyPayment?.plus(extraPayment) ?: 0.0
        assertTrue("First debt should receive minimum payment plus extra payment", 
            firstDebt.getRemainingBalance() <= originalDebt?.getRemainingBalance()?.minus(expectedPayment) ?: 0.0)
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

    @Test
    fun `test applyStrategy with single debt returns correct result`() {
        val singleDebt = listOf(debts[0])
        val result = strategy.applyStrategy(singleDebt, DebtStrategyType.AVALANCHE, 100.0)
        assertEquals(1, result.size)
        assertEquals(debts[0].id, result[0].id)
    }

    @Test
    fun `test applyStrategy with all debts paid does not apply extra payment`() {
        val paidDebts = debts.map { it.copy(amountPaid = it.totalAmount) }
        val result = strategy.applyStrategy(paidDebts, DebtStrategyType.SNOWBALL, 500.0)
        assertTrue(result.all { it.getRemainingBalance() == 0.0 })
    }

    @Test
    fun `test avalanche tie-breaker with same interest rate`() {
        val tieDebts = listOf(
            Debt(id = "1", name = "A", totalAmount = 1000.0, amountPaid = 0.0, interestRate = 10.0, monthlyPayment = 100.0),
            Debt(id = "2", name = "B", totalAmount = 1000.0, amountPaid = 0.0, interestRate = 10.0, monthlyPayment = 100.0)
        )
        val result = strategy.applyStrategy(tieDebts, DebtStrategyType.AVALANCHE)
        assertEquals(2, result.size)
        // Order should be stable or as expected
        assertTrue(result[0].interestRate == result[1].interestRate)
    }

    @Test
    fun `test snowball tie-breaker with same remaining balance`() {
        val tieDebts = listOf(
            Debt(id = "1", name = "A", totalAmount = 1000.0, amountPaid = 500.0, interestRate = 10.0, monthlyPayment = 100.0),
            Debt(id = "2", name = "B", totalAmount = 1000.0, amountPaid = 500.0, interestRate = 5.0, monthlyPayment = 100.0)
        )
        val result = strategy.applyStrategy(tieDebts, DebtStrategyType.SNOWBALL)
        assertEquals(2, result.size)
        // Order should be stable or as expected
        assertTrue(result[0].getRemainingBalance() == result[1].getRemainingBalance())
    }
} 