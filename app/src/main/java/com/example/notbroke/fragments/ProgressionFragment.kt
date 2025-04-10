package com.example.notbroke.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.example.notbroke.R
import com.example.notbroke.adapters.RewardAdapter
import com.example.notbroke.models.Reward
import com.example.notbroke.models.RewardType
import com.example.notbroke.models.Season
import java.text.SimpleDateFormat
import java.util.*

class ProgressionFragment : Fragment() {
    private lateinit var seasonNameText: TextView
    private lateinit var seasonDatesText: TextView
    private lateinit var seasonProgressBar: ProgressBar
    private lateinit var experienceText: TextView
    private lateinit var monthlyRewardsRecyclerView: RecyclerView
    private lateinit var seasonalRewardsRecyclerView: RecyclerView

    private lateinit var monthlyRewardsAdapter: RewardAdapter
    private lateinit var seasonalRewardsAdapter: RewardAdapter
    private lateinit var currentSeason: Season

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_progression, null)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Initialize views
        initializeViews(view)
        
        // Setup adapters
        setupAdapters()
        
        // Load current season data
        loadCurrentSeason()
        
        // Update UI
        updateUI()
    }

    private fun initializeViews(view: View) {
        seasonNameText = view.findViewById(R.id.seasonNameText)
        seasonDatesText = view.findViewById(R.id.seasonDatesText)
        seasonProgressBar = view.findViewById(R.id.seasonProgressBar)
        experienceText = view.findViewById(R.id.experienceText)
        monthlyRewardsRecyclerView = view.findViewById(R.id.monthlyRewardsRecyclerView)
        seasonalRewardsRecyclerView = view.findViewById(R.id.seasonalRewardsRecyclerView)
    }

    private fun setupAdapters() {
        monthlyRewardsAdapter = RewardAdapter { reward ->
            onRewardClaimed(reward)
        }
        seasonalRewardsAdapter = RewardAdapter { reward ->
            onRewardClaimed(reward)
        }

        monthlyRewardsRecyclerView.adapter = monthlyRewardsAdapter
        seasonalRewardsRecyclerView.adapter = seasonalRewardsAdapter
    }

    private fun loadCurrentSeason() {
        // Create sample season data
        val calendar = Calendar.getInstance()
        val startDate = calendar.time
        calendar.add(Calendar.MONTH, 3)
        val endDate = calendar.time

        val monthlyRewards = listOf(
            Reward(1, "Budget Master", "Stay within budget for all categories", 100, R.drawable.ic_reward, RewardType.MONTHLY),
            Reward(2, "Savings Champion", "Save 20% of income", 200, R.drawable.ic_reward, RewardType.MONTHLY),
            Reward(3, "Expense Tracker", "Log all expenses for 30 days", 300, R.drawable.ic_reward, RewardType.MONTHLY)
        )

        val seasonalRewards = listOf(
            Reward(4, "Financial Guru", "Complete all monthly challenges", 500, R.drawable.ic_reward, RewardType.SEASONAL),
            Reward(5, "Money Master", "Maintain budget for entire quarter", 800, R.drawable.ic_reward, RewardType.SEASONAL),
            Reward(6, "Wealth Builder", "Achieve savings goal for the quarter", 1000, R.drawable.ic_reward, RewardType.SEASONAL)
        )

        currentSeason = Season(
            id = 1,
            name = "Spring 2024",
            startDate = startDate,
            endDate = endDate,
            rewards = monthlyRewards + seasonalRewards
        )
    }

    private fun updateUI() {
        // Update season info
        seasonNameText.text = currentSeason.name
        val dateFormat = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault())
        seasonDatesText.text = "${dateFormat.format(currentSeason.startDate)} - ${dateFormat.format(currentSeason.endDate)}"

        // Update progress
        val progress = (currentSeason.currentExperience.toFloat() / currentSeason.maxExperience * 100).toInt()
        seasonProgressBar.progress = progress
        experienceText.text = "${currentSeason.currentExperience}/${currentSeason.maxExperience} XP"

        // Update rewards
        monthlyRewardsAdapter.updateRewards(currentSeason.rewards.filter { it.type == RewardType.MONTHLY })
        seasonalRewardsAdapter.updateRewards(currentSeason.rewards.filter { it.type == RewardType.SEASONAL })
    }

    private fun onRewardClaimed(reward: Reward) {
        Toast.makeText(context, "Claimed: ${reward.name}", Toast.LENGTH_SHORT).show()
        // Here you would typically update the reward status in your database
        // and possibly trigger some celebration animation
    }

    // Call this method when the user stays within budget for a category
    fun addExperienceForBudgetCompliance(amount: Int) {
        currentSeason.addExperience(amount)
        updateUI()
    }

    companion object {
        fun newInstance(): ProgressionFragment {
            return ProgressionFragment()
        }
    }
} 