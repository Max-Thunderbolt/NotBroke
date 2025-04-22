package com.example.notbroke.rewards

import com.example.notbroke.models.Reward
import com.example.notbroke.models.RewardType
import com.example.notbroke.R
import com.example.notbroke.models.Transaction
import java.util.*
import kotlin.math.roundToInt

object RewardManager {

    fun checkEligibleRewards(
        transactions: List<Transaction>,
        currentMonth: Int,
        currentYear: Int,
        monthlyBudget: Double,
        savingsGoalQuarterly: Double
    ): List<Reward> {
        val eligibleRewards = mutableListOf<Reward>()

        if (checkStayWithinBudget(transactions, currentMonth, currentYear, monthlyBudget)) {
            eligibleRewards.add(getRewardById(1))
        }

        if (checkSave20PercentOfIncome(transactions, currentMonth, currentYear)) {
            eligibleRewards.add(getRewardById(2))
        }

        if (checkLoggedExpensesFor30Days(transactions)) {
            eligibleRewards.add(getRewardById(3))
        }

        if (checkCompletedMonthlyChallenges(transactions, monthlyBudget, currentMonth, currentYear)) {
            eligibleRewards.add(getRewardById(4))
        }

        if (checkMaintainBudgetQuarterly(transactions)) {
            eligibleRewards.add(getRewardById(5))
        }

        if (checkAchievedSavingsGoal(transactions, savingsGoalQuarterly)) {
            eligibleRewards.add(getRewardById(6))
        }

        return eligibleRewards
    }

    private fun checkStayWithinBudget(
        transactions: List<Transaction>,
        month: Int,
        year: Int,
        budget: Double
    ): Boolean {
        val expenses = transactions.filter {
            it.type == Transaction.Type.EXPENSE &&
                    getMonth(it.date) == month &&
                    getYear(it.date) == year
        }.sumOf { it.amount }

        return expenses <= budget
    }

    private fun checkSave20PercentOfIncome(
        transactions: List<Transaction>,
        month: Int,
        year: Int
    ): Boolean {
        val income = transactions.filter {
            it.type == Transaction.Type.INCOME &&
                    getMonth(it.date) == month &&
                    getYear(it.date) == year
        }.sumOf { it.amount }

        val expenses = transactions.filter {
            it.type == Transaction.Type.EXPENSE &&
                    getMonth(it.date) == month &&
                    getYear(it.date) == year
        }.sumOf { it.amount }

        val savings = income - expenses
        return income > 0 && savings / income >= 0.20
    }

    private fun checkLoggedExpensesFor30Days(transactions: List<Transaction>): Boolean {
        val expenseDates = transactions.filter {
            it.type == Transaction.Type.EXPENSE
        }.map {
            val cal = Calendar.getInstance()
            cal.timeInMillis = it.date
            "${cal.get(Calendar.YEAR)}-${cal.get(Calendar.MONTH)}-${cal.get(Calendar.DAY_OF_MONTH)}"
        }.toSet()

        return expenseDates.size >= 30
    }

    private fun checkCompletedMonthlyChallenges(
        transactions: List<Transaction>,
        budget: Double,
        month: Int,
        year: Int
    ): Boolean {
        return checkStayWithinBudget(transactions, month, year, budget)
                && checkSave20PercentOfIncome(transactions, month, year)
                && checkLoggedExpensesFor30Days(transactions)
    }

    private fun checkMaintainBudgetQuarterly(transactions: List<Transaction>): Boolean {
        val calendar = Calendar.getInstance()
        val currentMonth = calendar.get(Calendar.MONTH)
        val currentYear = calendar.get(Calendar.YEAR)

        val monthsInQuarter = when (currentMonth) {
            in 0..2 -> listOf(0, 1, 2)
            in 3..5 -> listOf(3, 4, 5)
            in 6..8 -> listOf(6, 7, 8)
            else -> listOf(9, 10, 11)
        }

        return monthsInQuarter.all { month ->
            checkStayWithinBudget(transactions, month, currentYear, 1000.0) // Replace 1000.0 with actual monthly budget if available
        }
    }

    private fun checkAchievedSavingsGoal(transactions: List<Transaction>, savingsGoal: Double): Boolean {
        val income = transactions.filter {
            it.type == Transaction.Type.INCOME
        }.sumOf { it.amount }

        val expenses = transactions.filter {
            it.type == Transaction.Type.EXPENSE
        }.sumOf { it.amount }

        val savings = income - expenses
        return savings >= savingsGoal
    }

    private fun getRewardById(id: Int): Reward {
        return when (id) {
            1 -> Reward(1, "Budget Master", "Stay within budget for all categories", 100, R.drawable.ic_reward, RewardType.MONTHLY)
            2 -> Reward(2, "Savings Champion", "Save 20% of income", 200, R.drawable.ic_reward, RewardType.MONTHLY)
            3 -> Reward(3, "Expense Tracker", "Log all expenses for 30 days", 300, R.drawable.ic_reward, RewardType.MONTHLY)
            4 -> Reward(4, "Financial Guru", "Complete all monthly challenges", 500, R.drawable.ic_reward, RewardType.SEASONAL)
            5 -> Reward(5, "Money Master", "Maintain budget for entire quarter", 800, R.drawable.ic_reward, RewardType.SEASONAL)
            6 -> Reward(6, "Wealth Builder", "Achieve savings goal for the quarter", 1000, R.drawable.ic_reward, RewardType.SEASONAL)
            else -> throw IllegalArgumentException("Invalid reward ID")
        }
    }

    private fun getMonth(timestamp: Long): Int {
        val cal = Calendar.getInstance()
        cal.timeInMillis = timestamp
        return cal.get(Calendar.MONTH)
    }

    private fun getYear(timestamp: Long): Int {
        val cal = Calendar.getInstance()
        cal.timeInMillis = timestamp
        return cal.get(Calendar.YEAR)
    }

}
