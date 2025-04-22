package com.example.notbroke.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.notbroke.R
import com.example.notbroke.adapters.RewardAdapter
import com.example.notbroke.models.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class ProgressionFragment : Fragment() {
    private lateinit var seasonNameText: TextView
    private lateinit var seasonDatesText: TextView
    private lateinit var seasonProgressBar: ProgressBar
    private lateinit var experienceText: TextView
    private lateinit var claimedCount: TextView
    private lateinit var monthlyRewardsRecyclerView: RecyclerView
    private lateinit var seasonalRewardsRecyclerView: RecyclerView
    private lateinit var claimedRewardsLayout: LinearLayout
    private lateinit var claimedRewardsList: LinearLayout
    private lateinit var toggleClaimedButton: Button

    private lateinit var monthlyRewardsAdapter: RewardAdapter
    private lateinit var seasonalRewardsAdapter: RewardAdapter
    private lateinit var currentSeason: Season

    private val firestore = FirebaseFirestore.getInstance()
    private val userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    private val defaultRewards = listOf(
        Reward(1, "Budget Master", "Stay within budget for all categories", 100, R.drawable.ic_reward, RewardType.MONTHLY),
        Reward(2, "Savings Champion", "Save 20% of income", 200, R.drawable.ic_reward, RewardType.MONTHLY),
        Reward(3, "Expense Tracker", "Log all expenses for 30 days", 300, R.drawable.ic_reward, RewardType.MONTHLY),
        Reward(4, "Financial Guru", "Complete all monthly challenges", 500, R.drawable.ic_reward, RewardType.SEASONAL),
        Reward(5, "Money Master", "Maintain budget for entire quarter", 800, R.drawable.ic_reward, RewardType.SEASONAL),
        Reward(6, "Wealth Builder", "Achieve savings goal for the quarter", 1000, R.drawable.ic_reward, RewardType.SEASONAL)
    )

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_progression, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        initializeViews(view)
        setupAdapters()
        loadOrCreateSeason()
    }

    private fun initializeViews(view: View) {
        seasonNameText = view.findViewById(R.id.seasonNameText)
        seasonDatesText = view.findViewById(R.id.seasonDatesText)
        seasonProgressBar = view.findViewById(R.id.seasonProgressBar)
        experienceText = view.findViewById(R.id.experienceText)
        claimedCount = view.findViewById(R.id.claimedCount)
        monthlyRewardsRecyclerView = view.findViewById(R.id.monthlyRewardsRecyclerView)
        seasonalRewardsRecyclerView = view.findViewById(R.id.seasonalRewardsRecyclerView)
        claimedRewardsLayout = view.findViewById(R.id.claimedRewardsLayout)
        claimedRewardsList = view.findViewById(R.id.claimedRewardsList)
        toggleClaimedButton = view.findViewById(R.id.toggleClaimedButton)

        toggleClaimedButton.setOnClickListener {
            claimedRewardsList.visibility = if (claimedRewardsList.visibility == View.GONE) View.VISIBLE else View.GONE
        }
    }

    private fun setupAdapters() {
        monthlyRewardsAdapter = RewardAdapter { reward -> onRewardClaimed(reward) }
        seasonalRewardsAdapter = RewardAdapter { reward -> onRewardClaimed(reward) }

        monthlyRewardsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        seasonalRewardsRecyclerView.layoutManager = LinearLayoutManager(requireContext())

        monthlyRewardsRecyclerView.adapter = monthlyRewardsAdapter
        seasonalRewardsRecyclerView.adapter = seasonalRewardsAdapter
    }

    private fun getCurrentSeasonName(): String {
        val now = Calendar.getInstance()
        val month = now.get(Calendar.MONTH)
        return when (month) {
            in 2..4 -> "Spring"
            in 5..7 -> "Summer"
            in 8..10 -> "Fall"
            else -> "Winter"
        }
    }

    private fun loadOrCreateSeason() {
        if (userId.isEmpty()) {
            showToast("Please log in.")
            return
        }

        val seasonDoc = firestore.collection("seasons").document(userId)
        seasonDoc.get().addOnSuccessListener { doc ->
            val name = doc.getString("name") ?: getCurrentSeasonName()
            val start = doc.getDate("startDate") ?: Date()
            val end = doc.getDate("endDate") ?: Calendar.getInstance().apply { add(Calendar.MONTH, 3) }.time
            val xp = doc.getLong("currentExperience")?.toInt() ?: 0

            val rewards = defaultRewards.map { reward ->
                reward.copy(
                    claimed = doc.getBoolean("claimed_${reward.id}") ?: false
                )
            }

            currentSeason = Season(1, name, start, end, rewards, xp)

            updateUI()
        }.addOnFailureListener {
            createDefaultSeason()
        }
    }

    private fun createDefaultSeason() {
        val name = getCurrentSeasonName()
        val start = Calendar.getInstance().time
        val end = Calendar.getInstance().apply { add(Calendar.MONTH, 3) }.time

        val seasonMap = mapOf(
            "name" to name,
            "startDate" to start,
            "endDate" to end,
            "currentExperience" to 0
        )

        firestore.collection("seasons").document(userId).set(seasonMap)
            .addOnSuccessListener {
                currentSeason = Season(1, name, start, end, defaultRewards, 0)
                updateUI()
            }
            .addOnFailureListener {
                showToast("Failed to create season")
            }
    }

    private fun updateUI() {
        val season = currentSeason
        seasonNameText.text = season.name
        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        seasonDatesText.text = "${dateFormat.format(season.startDate)} - ${dateFormat.format(season.endDate)}"

        val progress = (season.currentExperience.toFloat() / season.maxExperience * 100).toInt()
        seasonProgressBar.progress = progress
        experienceText.text = "${season.currentExperience}/${season.maxExperience} XP"

        val claimed = season.rewards.count { it.claimed }
        claimedCount.text = "Claimed: $claimed"

        claimedRewardsList.removeAllViews()
        season.rewards.filter { it.claimed }.forEach { reward ->
            val textView = TextView(requireContext())
            textView.text = "✓ ${reward.name} - ${reward.experiencePoints} XP"
            textView.setTextColor(resources.getColor(R.color.white, null))
            claimedRewardsList.addView(textView)
        }

        monthlyRewardsAdapter.updateRewards(season.rewards.filter { it.type == RewardType.MONTHLY })
        seasonalRewardsAdapter.updateRewards(season.rewards.filter { it.type == RewardType.SEASONAL })
    }

    private fun onRewardClaimed(reward: Reward) {
        if (reward.claimed) {
            showToast("Reward already claimed for this season")
            return
        }

        reward.claimed = true
        currentSeason.currentExperience += reward.experiencePoints

        val updates = mapOf(
            "claimed_${reward.id}" to true,
            "currentExperience" to currentSeason.currentExperience
        )

        firestore.collection("seasons").document(userId)
            .set(updates, com.google.firebase.firestore.SetOptions.merge())
            .addOnSuccessListener {
                showToast("Claimed: ${reward.name} (+${reward.experiencePoints} XP)")
                updateUI()
            }
            .addOnFailureListener {
                showToast("❌ Failed to save reward to database")
                reward.claimed = false
                currentSeason.currentExperience -= reward.experiencePoints
            }
    }




    fun addExperienceForBudgetCompliance(amount: Int) {
        currentSeason.addExperience(amount)
        firestore.collection("seasons").document(userId)
            .update("currentExperience", currentSeason.currentExperience)
        updateUI()
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    companion object {
        fun newInstance(): ProgressionFragment = ProgressionFragment()
    }
}
