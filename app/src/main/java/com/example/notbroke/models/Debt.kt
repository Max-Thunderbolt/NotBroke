package com.example.notbroke.models

import java.util.Date
import java.util.UUID

data class Debt(
    val id: String = UUID.randomUUID().toString(),
    val userId: String = "",
    val name: String = "",
    val totalAmount: Double = 0.0,
    var amountPaid: Double = 0.0,
    val interestRate: Double = 0.0,
    var monthlyPayment: Double = 0.0,
    val creationDate: Long = System.currentTimeMillis(),
    var lastPaymentDate: Long? = null
) {
    // No-argument constructor for Firestore
    constructor() : this(
        id = UUID.randomUUID().toString(),
        userId = "",
        name = "",
        totalAmount = 0.0,
        amountPaid = 0.0,
        interestRate = 0.0,
        monthlyPayment = 0.0,
        creationDate = System.currentTimeMillis(),
        lastPaymentDate = null
    )

    /**
     * Calculates the remaining balance on the debt
     */
    fun getRemainingBalance(): Double {
        return totalAmount - amountPaid
    }
    
    /**
     * Calculates the progress percentage (0-100)
     */
    fun getProgressPercentage(): Int {
        return ((amountPaid / totalAmount) * 100).toInt().coerceIn(0, 100)
    }
    
    /**
     * Estimates the number of months remaining to pay off the debt
     * based on monthly payment and remaining balance
     */
    fun getMonthsRemaining(): Int {
        val remaining = getRemainingBalance()
        if (remaining <= 0) return 0
        if (monthlyPayment <= 0) return Int.MAX_VALUE
        
        // Simple calculation without considering compounding interest
        var months = 0
        var balance = remaining
        val monthlyInterest = interestRate / 100.0 / 12.0
        
        while (balance > 0 && months < 1000) { // Cap at 1000 months (83 years)
            // Add monthly interest
            balance += balance * monthlyInterest
            // Subtract monthly payment
            balance -= monthlyPayment
            months++
        }
        
        return months
    }
    
    /**
     * Makes a payment on the debt and updates the paid amount
     */
    fun makePayment(amount: Double): Double {
        if (amount <= 0) return 0.0
        val remaining = getRemainingBalance()
        val paymentToApply = amount.coerceAtMost(remaining)
        
        amountPaid += paymentToApply
        lastPaymentDate = System.currentTimeMillis()
        
        return paymentToApply
    }
    
    /**
     * Calculates the projected payoff date based on the current monthly payment
     */
    fun getProjectedPayoffDate(): Date {
        val months = getMonthsRemaining()
        val calendar = java.util.Calendar.getInstance()
        calendar.add(java.util.Calendar.MONTH, months)
        return calendar.time
    }
    
    companion object {
        fun createSampleDebts(userId: String): List<Debt> {
            return listOf(
                Debt(
                    userId = userId,
                    name = "Personal Loan",
                    totalAmount = 75000.0,
                    amountPaid = 15000.0,
                    interestRate = 12.5,
                    monthlyPayment = 2500.0
                ),
                Debt(
                    userId = userId,
                    name = "Credit Card",
                    totalAmount = 25000.0,
                    amountPaid = 5000.0,
                    interestRate = 21.0,
                    monthlyPayment = 1500.0
                ),
                Debt(
                    userId = userId,
                    name = "Car Loan",
                    totalAmount = 150000.0,
                    amountPaid = 65000.0,
                    interestRate = 8.5,
                    monthlyPayment = 3500.0
                ),
                Debt(
                    userId = userId,
                    name = "Student Loan",
                    totalAmount = 120000.0,
                    amountPaid = 40000.0,
                    interestRate = 6.0,
                    monthlyPayment = 2200.0
                )
            )
        }
    }
} 