package com.example.notbroke.models

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
     * @return A new list of debts with the strategy applied
     */
    fun applyStrategy(debts: List<Debt>, strategyType: DebtStrategyType): List<Debt> {
        return when (strategyType) {
            DebtStrategyType.AVALANCHE -> applyAvalancheMethod(debts)
            DebtStrategyType.SNOWBALL -> applySnowballMethod(debts)
            DebtStrategyType.DEBT_CONSOLIDATION -> applyDebtConsolidation(debts)
            DebtStrategyType.HIGHEST_INTEREST_FIRST -> applyHighestInterestFirst(debts)
            DebtStrategyType.BALANCE_PROPORTION -> applyBalanceProportion(debts)
            DebtStrategyType.DEBT_STACKING -> applyDebtStacking(debts)
        }
    }
    
    /**
     * Avalanche Method: Pay minimum on all debts, then put extra money towards highest interest rate debt first
     */
    private fun applyAvalancheMethod(debts: List<Debt>): List<Debt> {
        return debts.sortedByDescending { it.interestRate }
    }
    
    /**
     * Snowball Method: Pay minimum on all debts, then put extra money towards smallest balance debt first
     */
    private fun applySnowballMethod(debts: List<Debt>): List<Debt> {
        return debts.sortedBy { it.getRemainingBalance() }
    }
    
    /**
     * Debt Consolidation: Combine multiple debts into a single loan with lower interest rate
     * This is a simulation - in a real app, this would calculate the consolidated loan details
     */
    private fun applyDebtConsolidation(debts: List<Debt>): List<Debt> {
        // In a real implementation, this would create a new consolidated debt
        // For now, we'll just return the original debts sorted by interest rate
        return debts.sortedByDescending { it.interestRate }
    }
    
    /**
     * Highest Interest First: Focus exclusively on paying off debts with the highest interest rates first
     */
    private fun applyHighestInterestFirst(debts: List<Debt>): List<Debt> {
        return debts.sortedByDescending { it.interestRate }
    }
    
    /**
     * Balance Proportion: Distribute extra payments proportionally based on debt balances
     */
    private fun applyBalanceProportion(debts: List<Debt>): List<Debt> {
        // In a real implementation, this would calculate proportional payments
        // For now, we'll just return the original debts
        return debts
    }
    
    /**
     * Debt Stacking: After paying off one debt, add that payment amount to the next debt payment
     */
    private fun applyDebtStacking(debts: List<Debt>): List<Debt> {
        return debts.sortedBy { it.getMonthsRemaining() }
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