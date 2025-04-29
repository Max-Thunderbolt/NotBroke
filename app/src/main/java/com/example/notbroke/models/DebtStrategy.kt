package com.example.notbroke.models

import java.util.*
import java.util.Calendar

/**
 * Enum representing different debt payoff strategies
 */
enum class DebtStrategyType {
    AVALANCHE,
    SNOWBALL,
    DEBT_CONSOLIDATION,
    HIGHEST_INTEREST_FIRST,
    BALANCE_PROPORTION,
    DEBT_STACKING
}

/**
 * Class to handle debt payoff strategies
 */
class DebtStrategy {
    
    /**
     * Apply a debt payoff strategy to a list of debts
     * @param debts List of debts to apply the strategy to
     * @param strategyType The type of strategy to apply
     * @param extraPayment Optional extra payment amount to apply to the strategy
     * @return A new list of debts with the strategy applied
     */
    fun applyStrategy(debts: List<Debt>, strategyType: DebtStrategyType, extraPayment: Double = 0.0): List<Debt> {
        return when (strategyType) {
            DebtStrategyType.AVALANCHE -> applyAvalancheMethod(debts, extraPayment)
            DebtStrategyType.SNOWBALL -> applySnowballMethod(debts, extraPayment)
            DebtStrategyType.DEBT_CONSOLIDATION -> applyDebtConsolidation(debts)
            DebtStrategyType.HIGHEST_INTEREST_FIRST -> applyHighestInterestFirst(debts, extraPayment)
            DebtStrategyType.BALANCE_PROPORTION -> applyBalanceProportion(debts, extraPayment)
            DebtStrategyType.DEBT_STACKING -> applyDebtStacking(debts, extraPayment)
        }
    }
    
    /**
     * Calculate the estimated payoff date based on the current strategy
     * @param debts List of debts
     * @param strategyType The type of strategy to use
     * @return Estimated date when all debts will be paid off
     */
    fun calculateEstimatedPayoffDate(debts: List<Debt>, strategyType: DebtStrategyType): Date {
        if (debts.isEmpty()) {
            return Date()
        }
        
        // Calculate total monthly payment
        val totalMonthlyPayment = debts.sumOf { it.monthlyPayment }
        
        // Calculate total remaining debt
        val totalRemainingDebt = debts.sumOf { it.getRemainingBalance() }
        
        // Calculate months until payoff (simplified calculation)
        val monthsUntilPayoff = (totalRemainingDebt / totalMonthlyPayment).toInt()
        
        // Create calendar and add months
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.MONTH, monthsUntilPayoff)
        
        return calendar.time
    }
    
    /**
     * Avalanche Method: Pay minimum on all debts, then put extra money towards highest interest rate debt first
     */
    private fun applyAvalancheMethod(debts: List<Debt>, extraPayment: Double): List<Debt> {
        if (debts.isEmpty()) return emptyList()
        
        val sortedDebts = debts.sortedByDescending { it.interestRate }
        val result = mutableListOf<Debt>()
        var remainingExtraPayment = extraPayment

        // First, apply minimum payments to all debts
        sortedDebts.forEach { debt ->
            val debtCopy = debt.copy()
            debtCopy.makePayment(debt.monthlyPayment)
            result.add(debtCopy)
        }

        // Then apply extra payment to highest interest debt
        if (remainingExtraPayment > 0) {
            val highestInterestDebt = result.first()
            val payment = minOf(remainingExtraPayment, highestInterestDebt.getRemainingBalance())
            highestInterestDebt.makePayment(payment)
            remainingExtraPayment -= payment
        }

        return result
    }
    
    /**
     * Snowball Method: Pay minimum on all debts, then put extra money towards smallest balance debt first
     */
    private fun applySnowballMethod(debts: List<Debt>, extraPayment: Double): List<Debt> {
        if (debts.isEmpty()) return emptyList()
        
        val sortedDebts = debts.sortedBy { it.getRemainingBalance() }
        val result = mutableListOf<Debt>()
        var remainingExtraPayment = extraPayment

        // First, apply minimum payments to all debts
        sortedDebts.forEach { debt ->
            val debtCopy = debt.copy()
            debtCopy.makePayment(debt.monthlyPayment)
            result.add(debtCopy)
        }

        // Then apply extra payment to smallest balance debt
        if (remainingExtraPayment > 0) {
            val smallestDebt = result.minByOrNull { it.getRemainingBalance() }
            smallestDebt?.let {
                val payment = minOf(remainingExtraPayment, it.getRemainingBalance())
                it.makePayment(payment)
                remainingExtraPayment -= payment
            }
        }

        return result
    }
    
    /**
     * Debt Consolidation: Combine multiple debts into a single loan with lower interest rate
     */
    private fun applyDebtConsolidation(debts: List<Debt>): List<Debt> {
        if (debts.isEmpty()) return emptyList()

        val totalDebt = debts.sumOf { it.getRemainingBalance() }
        val weightedInterestRate = debts.sumOf { it.getRemainingBalance() * it.interestRate } / totalDebt
        val consolidatedRate = (weightedInterestRate - 2.0).coerceAtLeast(4.0) // Minimum 4%
        
        // Create a new consolidated debt
        val consolidatedDebt = Debt(
            name = "Consolidated Loan",
            balance = totalDebt,
            interestRate = consolidatedRate,
            monthlyPayment = calculateConsolidatedMonthlyPayment(totalDebt, consolidatedRate)
        )

        return listOf(consolidatedDebt)
    }
    
    /**
     * Highest Interest First: Focus exclusively on paying off debts with the highest interest rates first
     */
    private fun applyHighestInterestFirst(debts: List<Debt>, extraPayment: Double): List<Debt> {
        if (debts.isEmpty()) return emptyList()
        
        val sortedDebts = debts.sortedByDescending { it.interestRate }
        val result = mutableListOf<Debt>()
        var remainingExtraPayment = extraPayment

        // Apply minimum payments to all debts
        sortedDebts.forEach { debt ->
            val debtCopy = debt.copy()
            debtCopy.makePayment(debt.monthlyPayment)
            result.add(debtCopy)
        }

        // Apply extra payment to highest interest debt
        if (remainingExtraPayment > 0) {
            val highestInterestDebt = result.first()
            val payment = minOf(remainingExtraPayment, highestInterestDebt.getRemainingBalance())
            highestInterestDebt.makePayment(payment)
        }

        return result
    }
    
    /**
     * Balance Proportion: Distribute extra payments proportionally based on debt balances
     */
    private fun applyBalanceProportion(debts: List<Debt>, extraPayment: Double): List<Debt> {
        if (debts.isEmpty()) return emptyList()
        
        val result = mutableListOf<Debt>()
        val totalDebt = debts.sumOf { it.getRemainingBalance() }

        // Apply minimum payments and calculate proportional extra payments
        debts.forEach { debt ->
            val debtCopy = debt.copy()
            val minimumPayment = debt.monthlyPayment
            val proportionalExtra = (debt.getRemainingBalance() / totalDebt) * extraPayment
            val totalPayment = minimumPayment + proportionalExtra
            
            debtCopy.makePayment(totalPayment)
            result.add(debtCopy)
        }

        return result
    }
    
    /**
     * Debt Stacking: After paying off one debt, add that payment amount to the next debt payment
     */
    private fun applyDebtStacking(debts: List<Debt>, extraPayment: Double): List<Debt> {
        if (debts.isEmpty()) return emptyList()
        
        val sortedDebts = debts.sortedBy { it.getMonthsRemaining() }
        val result = mutableListOf<Debt>()
        var availablePayment = extraPayment

        // Apply minimum payments and stack payments from paid-off debts
        sortedDebts.forEach { debt ->
            val debtCopy = debt.copy()
            val payment = debt.monthlyPayment + availablePayment
            debtCopy.makePayment(payment)
            
            if (debtCopy.getRemainingBalance() <= 0) {
                // If debt is paid off, add its minimum payment to available payment
                availablePayment += debt.monthlyPayment
            } else {
                availablePayment = 0.0
            }
            
            result.add(debtCopy)
        }

        return result
    }
    
    /**
     * Calculate the monthly payment for a consolidated loan
     */
    private fun calculateConsolidatedMonthlyPayment(principal: Double, annualInterestRate: Double): Double {
        val monthlyRate = annualInterestRate / 100.0 / 12.0
        val numberOfPayments = 60.0 // 5 years term
        
        return if (monthlyRate == 0.0) {
            principal / numberOfPayments
        } else {
            principal * (monthlyRate * Math.pow(1 + monthlyRate, numberOfPayments)) / 
                    (Math.pow(1 + monthlyRate, numberOfPayments) - 1)
        }
    }
    
    /**
     * Calculate the estimated savings from debt consolidation
     */
    fun calculateConsolidationSavings(debts: List<Debt>): Double {
        if (debts.isEmpty()) return 0.0
        
        val totalDebt = debts.sumOf { it.getRemainingBalance() }
        val weightedInterestRate = debts.sumOf { it.getRemainingBalance() * it.interestRate } / totalDebt
        val consolidatedRate = (weightedInterestRate - 2.0).coerceAtLeast(4.0) // Minimum 4%
        
        // Estimate savings (very simplified)
        val originalInterest = debts.sumOf { it.getRemainingBalance() * it.interestRate / 100.0 * (it.getMonthsRemaining() / 12.0) }
        val consolidatedInterest = totalDebt * consolidatedRate / 100.0 * (debts.maxOf { it.getMonthsRemaining() } / 12.0)
        
        return (originalInterest - consolidatedInterest).coerceAtLeast(0.0)
    }
    
    /**
     * Get the description of a strategy
     */
    fun getStrategyDescription(strategyType: DebtStrategyType): String {
        return when (strategyType) {
            DebtStrategyType.AVALANCHE -> "Avalanche Method: Pay minimum on all debts, then put extra money towards highest interest rate debt first. This minimizes total interest paid over time."
            DebtStrategyType.SNOWBALL -> "Snowball Method: Pay minimum on all debts, then put extra money towards smallest balance debt first. This creates psychological wins and momentum."
            DebtStrategyType.DEBT_CONSOLIDATION -> "Debt Consolidation: Combine multiple debts into a single loan with lower interest rate. This simplifies payments and can reduce overall interest."
            DebtStrategyType.HIGHEST_INTEREST_FIRST -> "Highest Interest First: Focus exclusively on paying off debts with the highest interest rates first to minimize total interest paid."
            DebtStrategyType.BALANCE_PROPORTION -> "Balance Proportion: Distribute extra payments proportionally based on debt balances. Balanced approach that tackles all debts simultaneously."
            DebtStrategyType.DEBT_STACKING -> "Debt Stacking: After paying off one debt, add that payment amount to the next debt payment. Creates an accelerating payoff schedule."
        }
    }
    
    companion object {
        @Volatile
        private var INSTANCE: DebtStrategy? = null
        
        fun getInstance(): DebtStrategy {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: DebtStrategy().also { INSTANCE = it }
            }
        }
    }
} 