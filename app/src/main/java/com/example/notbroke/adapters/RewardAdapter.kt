package com.example.notbroke.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.notbroke.R
import com.example.notbroke.models.Reward

class RewardAdapter(
    private val onRewardClicked: (Reward) -> Unit
) : RecyclerView.Adapter<RewardAdapter.RewardViewHolder>() {

    private var rewards: List<Reward> = emptyList()

    fun updateRewards(newRewards: List<Reward>) {
        rewards = newRewards
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RewardViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_reward, parent, false)
        return RewardViewHolder(view)
    }

    override fun onBindViewHolder(holder: RewardViewHolder, position: Int) {
        holder.bind(rewards[position])
    }

    override fun getItemCount(): Int = rewards.size

    inner class RewardViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val iconImageView: ImageView = itemView.findViewById(R.id.rewardIcon)
        private val nameTextView: TextView = itemView.findViewById(R.id.rewardName)
        private val descriptionTextView: TextView = itemView.findViewById(R.id.rewardDescription)
        private val experienceTextView: TextView = itemView.findViewById(R.id.rewardExperience)

        fun bind(reward: Reward) {
            iconImageView.setImageResource(reward.iconResId)
            nameTextView.text = reward.name
            descriptionTextView.text = reward.description
            experienceTextView.text = "${reward.experiencePoints} XP"

            itemView.setOnClickListener {
                onRewardClicked(reward)
            }
        }
    }
} 