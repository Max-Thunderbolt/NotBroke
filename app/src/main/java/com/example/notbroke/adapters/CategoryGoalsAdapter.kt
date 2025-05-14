package com.example.notbroke.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.notbroke.R
import com.example.notbroke.models.CategoryGoalDisplayItem
import com.google.android.material.progressindicator.LinearProgressIndicator
import java.text.NumberFormat
import java.util.Locale

class CategoryGoalsAdapter : ListAdapter<CategoryGoalDisplayItem, CategoryGoalsAdapter.ViewHolder>(CategoryGoalDiffCallback()) {

    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale("en", "ZA")) // For R currency format

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_category_goal, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item, currencyFormat)
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val categoryNameTextView: TextView = itemView.findViewById(R.id.categoryNameTextView)
        private val categoryProgressTextView: TextView = itemView.findViewById(R.id.categoryProgressTextView)
        private val categoryProgressBar: LinearProgressIndicator = itemView.findViewById(R.id.categoryProgressBar)

        fun bind(item: CategoryGoalDisplayItem, currencyFormat: NumberFormat) {
            categoryNameTextView.text = item.categoryName
            val progressText = "${currencyFormat.format(item.currentSpend)} / ${currencyFormat.format(item.monthlyLimit)}"
            categoryProgressTextView.text = progressText
            categoryProgressBar.progress = item.progress

            // Change progress bar color if over budget
            if (item.currentSpend > item.monthlyLimit && item.monthlyLimit > 0) {
                categoryProgressBar.setIndicatorColor(itemView.context.getColor(R.color.material_red_700))
            } else {
                categoryProgressBar.setIndicatorColor(itemView.context.getColor(R.color.dashboard_yellow_accent))
            }
        }
    }

    class CategoryGoalDiffCallback : DiffUtil.ItemCallback<CategoryGoalDisplayItem>() {
        override fun areItemsTheSame(oldItem: CategoryGoalDisplayItem, newItem: CategoryGoalDisplayItem): Boolean {
            return oldItem.categoryName == newItem.categoryName
        }

        override fun areContentsTheSame(oldItem: CategoryGoalDisplayItem, newItem: CategoryGoalDisplayItem): Boolean {
            return oldItem == newItem
        }
    }
}