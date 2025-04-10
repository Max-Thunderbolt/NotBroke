package com.example.notbroke.fragments

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.notbroke.R
import com.example.notbroke.TestData
import com.example.notbroke.adapters.BudgetCategoryAdapter
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.PercentFormatter

/**
 * Fragment that displays budget information and categories
 */
class BudgetFragment : Fragment(), BudgetCategoryAdapter.BudgetCategoryListener {
    
    private val TAG = "BudgetFragment"
    private lateinit var budgetRecyclerView: RecyclerView
    private lateinit var budgetAdapter: BudgetCategoryAdapter
    private lateinit var totalBudgetTextView: TextView
    private lateinit var totalSpentTextView: TextView
    private lateinit var remainingTextView: TextView
    private lateinit var periodSpinner: Spinner
    private lateinit var pieChart: PieChart

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.d(TAG, "onCreateView: Creating BudgetFragment")
        
        // Inflate the layout
        val view = inflater.inflate(R.layout.fragment_budget, container, false)
        
        try {
            // Initialize views
            budgetRecyclerView = view.findViewById(R.id.budgetRecyclerView)
            totalBudgetTextView = view.findViewById(R.id.totalBudgetTextView)
            totalSpentTextView = view.findViewById(R.id.totalSpentTextView)
            remainingTextView = view.findViewById(R.id.remainingTextView)
            periodSpinner = view.findViewById(R.id.periodSpinner)
            pieChart = view.findViewById(R.id.pieChart)
            
            // Setup period spinner
            setupPeriodSpinner()
            
            // Setup RecyclerView
            setupRecyclerView()
            
            // Setup PieChart
            setupPieChart()
            
            // Load budget data
            loadBudgetData()
            
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up BudgetFragment", e)
            showSimplifiedView(view)
        }
        
        Log.d(TAG, "onCreateView: BudgetFragment created successfully")
        return view
    }
    
    private fun setupPieChart() {
        pieChart.apply {
            description.isEnabled = false
            setUsePercentValues(true)
            setExtraOffsets(5f, 10f, 5f, 5f)
            dragDecelerationFrictionCoef = 0.95f
            isDrawHoleEnabled = true
            setHoleColor(Color.TRANSPARENT)
            setTransparentCircleColor(Color.WHITE)
            setTransparentCircleAlpha(110)
            holeRadius = 58f
            transparentCircleRadius = 61f
            setDrawCenterText(true)
            rotationAngle = 0f
            isRotationEnabled = true
            isHighlightPerTapEnabled = true
            animateY(1400)
            legend.isEnabled = true
            legend.textColor = Color.WHITE
            setEntryLabelColor(Color.WHITE)
            setEntryLabelTextSize(12f)
        }
    }
    
    private fun setupPeriodSpinner() {
        val periods = arrayOf("This Month", "Last Month", "This Year", "Custom")
        
        // Create adapter with white text color
        val adapter = object : ArrayAdapter<String>(
            requireContext(),
            android.R.layout.simple_spinner_item,
            periods
        ) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getView(position, convertView, parent)
                (view as? TextView)?.setTextColor(Color.WHITE)
                return view
            }
            
            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getDropDownView(position, convertView, parent)
                (view as? TextView)?.setTextColor(Color.WHITE)
                return view
            }
        }
        
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        periodSpinner.adapter = adapter
    }
    
    private fun setupRecyclerView() {
        // Create empty adapter first
        budgetAdapter = BudgetCategoryAdapter(emptyList(), this)
        
        // Setup RecyclerView
        budgetRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = budgetAdapter
        }
        
        Log.d(TAG, "RecyclerView setup complete")
    }
    
    private fun loadBudgetData() {
        try {
            // Get sample budget categories from TestData
            val budgetCategories = TestData.getSampleBudgetCategories()
            
            // Calculate totals
            var totalBudget = 0.0
            var totalSpent = 0.0
            
            for (category in budgetCategories) {
                totalBudget += category.budgetAmount
                totalSpent += category.spentAmount
            }
            
            val remaining = totalBudget - totalSpent
            
            // Update UI
            totalBudgetTextView.text = "R%.2f".format(totalBudget)
            totalSpentTextView.text = "R%.2f".format(totalSpent)
            remainingTextView.text = "R%.2f".format(remaining)
            
            // Update pie chart
            updatePieChart(budgetCategories)
            
            // Set categories to adapter
            budgetAdapter = BudgetCategoryAdapter(budgetCategories, this)
            budgetRecyclerView.adapter = budgetAdapter
            
            Log.d(TAG, "Loaded ${budgetCategories.size} budget categories")
        } catch (e: Exception) {
            Log.e(TAG, "Error loading budget data", e)
        }
    }
    
    private fun updatePieChart(categories: List<BudgetCategory>) {
        val entries = ArrayList<PieEntry>()
        val colors = ArrayList<Int>()
        
        // Add entries for each category
        categories.forEach { category ->
            if (category.spentAmount > 0) {
                entries.add(PieEntry(category.spentAmount.toFloat(), category.name))
                // Add a unique color for each category
                colors.add(when (category.name) {
                    "Groceries" -> Color.parseColor("#4CAF50")  // Green
                    "Food" -> Color.parseColor("#FF9800")       // Orange
                    "Entertainment" -> Color.parseColor("#E91E63") // Pink
                    "Transport" -> Color.parseColor("#2196F3")   // Blue
                    "Utilities" -> Color.parseColor("#9C27B0")   // Purple
                    "Rent" -> Color.parseColor("#F44336")       // Red
                    else -> Color.parseColor("#607D8B")         // Blue Grey
                })
            }
        }
        
        val dataSet = PieDataSet(entries, "Spending Categories")
        dataSet.apply {
            this.colors = colors
            valueTextSize = 14f
            valueTextColor = Color.WHITE
            valueFormatter = PercentFormatter(pieChart)
        }
        
        val data = PieData(dataSet)
        pieChart.data = data
        pieChart.invalidate() // refresh
    }
    
    override fun onEditBudget(category: BudgetCategory) {
        Log.d(TAG, "Edit budget requested for category: ${category.name}")
        // Will implement edit functionality later
        // For now, just show a toast
        android.widget.Toast.makeText(
            requireContext(),
            "Editing ${category.name} budget (coming soon)",
            android.widget.Toast.LENGTH_SHORT
        ).show()
    }
    
    private fun showSimplifiedView(rootView: View) {
        // If we encounter an error, show a simplified view
        try {
            // Hide all views in the fragment
            if (rootView is ViewGroup) {
                for (i in 0 until rootView.childCount) {
                    rootView.getChildAt(i).visibility = View.GONE
                }
            }
            
            // Create a simple message view
            val messageView = TextView(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.MATCH_PARENT
                )
                
                text = "Budget Feature\nCould not load properly\nTry again later"
                textSize = 24f
                gravity = Gravity.CENTER
                setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
            }
            
            // Add message view to root
            if (rootView is ViewGroup) {
                rootView.addView(messageView)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error showing simplified view", e)
        }
    }
    
    override fun onDestroyView() {
        Log.d(TAG, "onDestroyView: Cleaning up BudgetFragment")
        super.onDestroyView()
    }
    
    // BudgetCategory data class
    data class BudgetCategory(
        val name: String,
        var budgetAmount: Double,
        var spentAmount: Double
    ) {
        val percentUsed: Double
            get() = if (budgetAmount > 0) (spentAmount / budgetAmount) * 100 else 0.0
    }
} 