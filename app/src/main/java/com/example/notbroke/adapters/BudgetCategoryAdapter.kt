package com.example.notbroke.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.notbroke.fragments.BudgetFragment.BudgetCategory
import com.example.notbroke.R

class BudgetCategoryAdapter(
    private val categories: List<BudgetCategory>,
    private val listener: BudgetCategoryListener
) : RecyclerView.Adapter<BudgetCategoryAdapter.BudgetViewHolder>() {

    interface BudgetCategoryListener {
        fun onEditBudget(category: BudgetCategory)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BudgetViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_budget_category, parent, false)
        return BudgetViewHolder(view)
    }

    override fun onBindViewHolder(holder: BudgetViewHolder, position: Int) {
        holder.bind(categories[position])
    }

    override fun getItemCount(): Int = categories.size

    inner class BudgetViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val categoryNameTextView: TextView = itemView.findViewById(R.id.categoryNameTextView)
        private val budgetAmountTextView: TextView = itemView.findViewById(R.id.budgetAmountTextView)
        private val spentAmountTextView: TextView = itemView.findViewById(R.id.spentAmountTextView)
        private val percentageTextView: TextView = itemView.findViewById(R.id.percentageTextView)
        private val editButton: ImageButton = itemView.findViewById(R.id.editBudgetButton)
        private val progressBar: ProgressBar = itemView.findViewById(R.id.budgetProgressBar)

        fun bind(category: BudgetCategory) {
            categoryNameTextView.text = category.name
            budgetAmountTextView.text = "Budget: R%.2f".format(category.budgetAmount)
            spentAmountTextView.text = "Spent: R%.2f".format(category.spentAmount)
            
            val percentUsed = category.percentUsed
            percentageTextView.text = "%.1f%%".format(percentUsed)
            
            // Update progress bar
            progressBar.progress = percentUsed.toInt()
            
            // Set color based on percentage used
            val progressColor = when {
                percentUsed > 90 -> Color.parseColor("#E91E1E") // Red for > 90%
                percentUsed > 75 -> Color.parseColor("#FF9800") // Orange for > 75%
                else -> Color.parseColor("#4CAF50") // Green for ≤ 75%
            }
            progressBar.progressTintList = android.content.res.ColorStateList.valueOf(progressColor)
            
            // Setup edit button
            editButton.setOnClickListener {
                listener.onEditBudget(category)
            }
        }
    }
} 