package com.example.notbroke.models

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.*

class DebtTest {
    private lateinit var debt: Debt

    @Before
    fun setup() {
        debt = Debt(
            id = "debt1",
            userId = "user1",
            name = "Car Loan",
            totalAmount = 10000.0,
            amountPaid = 2500.0,
            interestRate = 5.0,
            monthlyPayment = 500.0,
            creationDate = 1000L,
            lastPaymentDate = 2000L
        )
    }

    @Test
    fun `test Debt creation with valid data`() {
        assertEquals("debt1", debt.id)
        assertEquals("user1", debt.userId)
        assertEquals("Car Loan", debt.name)
        assertEquals(10000.0, debt.totalAmount, 0.001)
        assertEquals(2500.0, debt.amountPaid, 0.001)
        assertEquals(5.0, debt.interestRate, 0.001)
        assertEquals(500.0, debt.monthlyPayment, 0.001)
        assertEquals(1000L, debt.creationDate)
        assertEquals(2000L, debt.lastPaymentDate)
    }

    @Test
    fun `test Debt default values`() {
        val defaultDebt = Debt()
        assertTrue(defaultDebt.id.isNotBlank())
        assertEquals("", defaultDebt.userId)
        assertEquals("", defaultDebt.name)
        assertEquals(0.0, defaultDebt.totalAmount, 0.001)
        assertEquals(0.0, defaultDebt.amountPaid, 0.001)
        assertEquals(0.0, defaultDebt.interestRate, 0.001)
        assertEquals(0.0, defaultDebt.monthlyPayment, 0.001)
        assertTrue(defaultDebt.creationDate > 0)
        assertNull(defaultDebt.lastPaymentDate)
    }

    @Test
    fun `test getRemainingBalance`() {
        assertEquals(7500.0, debt.getRemainingBalance(), 0.001)
    }

    @Test
    fun `test getProgressPercentage`() {
        assertEquals(25, debt.getProgressPercentage())
    }

    @Test
    fun `test getMonthsRemaining`() {
        val months = debt.getMonthsRemaining()
        assertTrue(months > 0 && months < 1000)
    }

    @Test
    fun `test makePayment updates amountPaid and lastPaymentDate`() {
        val oldPaid = debt.amountPaid
        val payment = debt.makePayment(1000.0)
        assertEquals(1000.0, payment, 0.001)
        assertEquals(oldPaid + 1000.0, debt.amountPaid, 0.001)
        assertNotNull(debt.lastPaymentDate)
    }

    @Test
    fun `test makePayment does not overpay`() {
        val payment = debt.makePayment(10000.0)
        assertEquals(7500.0, payment, 0.001)
        assertEquals(10000.0, debt.amountPaid, 0.001)
    }

    @Test
    fun `test getProjectedPayoffDate returns future date`() {
        val payoffDate = debt.getProjectedPayoffDate()
        assertTrue(payoffDate.after(Date(System.currentTimeMillis() - 1000)))
    }

    @Test
    fun `test createSampleDebts returns non-empty list`() {
        val sampleDebts = Debt.createSampleDebts("userX")
        assertTrue(sampleDebts.isNotEmpty())
        assertTrue(sampleDebts.all { it.userId == "userX" })
    }
} 