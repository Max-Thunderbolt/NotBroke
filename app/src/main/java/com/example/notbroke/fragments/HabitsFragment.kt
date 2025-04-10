package com.example.notbroke.fragments

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.notbroke.R
import com.example.notbroke.TestData
import com.example.notbroke.models.Transaction
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.max
import kotlin.math.min

class HabitsFragment : Fragment() {
    private val TAG = "HabitsFragment"
    
    // Views
    private lateinit var lineChart: LineChart
    private lateinit var periodSpinner: Spinner
    private lateinit var categoryChipGroup: ChipGroup
    private lateinit var noDataTextView: TextView
    private lateinit var totalSpentTextView: TextView
    private lateinit var averageSpentTextView: TextView
    private lateinit var trendTextView: TextView
    
    // Data
    private var transactions: List<Transaction> = listOf()
    private var selectedCategories: MutableSet<String> = mutableSetOf()
    private var allCategories: List<String> = listOf()
    private var selectedPeriod: Period = Period.MONTH
    
    // Date periods
    enum class Period {
        WEEK, MONTH, THREE_MONTHS, SIX_MONTHS, YEAR
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_habits, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        try {
            // Initialize views
            initializeViews(view)
            
            // Load sample transactions
            loadTransactions()
            
            // Setup UI components
            setupPeriodSpinner()
            setupCategoryChips()
            setupLineChart()
            
            // Initial load of data
            updateChart()
            
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up habits fragment", e)
            showToast("Error: ${e.message}")
        }
    }
    
    private fun initializeViews(view: View) {
        lineChart = view.findViewById(R.id.lineChart)
        periodSpinner = view.findViewById(R.id.periodSpinner)
        categoryChipGroup = view.findViewById(R.id.categoryChipGroup)
        noDataTextView = view.findViewById(R.id.noDataTextView)
        totalSpentTextView = view.findViewById(R.id.totalSpentTextView)
        averageSpentTextView = view.findViewById(R.id.averageSpentTextView)
        trendTextView = view.findViewById(R.id.trendTextView)
    }
    
    private fun loadTransactions() {
        transactions = TestData.getSampleTransactions()
        
        // Extract all unique categories from expense transactions
        allCategories = transactions
            .filter { it.type == Transaction.Type.EXPENSE }
            .map { it.category }
            .distinct()
            .sorted()
        
        // Add "All" as a special category
        selectedCategories.add("All")
    }
    
    private fun setupPeriodSpinner() {
        val periods = arrayOf("This Month", "Last Week", "Last 3 Months", "Last 6 Months", "This Year")
        
        // Custom adapter for styling the spinner
        val adapter = object : ArrayAdapter<String>(
            requireContext(),
            android.R.layout.simple_spinner_item,
            periods
        ) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getView(position, convertView, parent)
                (view as? TextView)?.apply {
                    setTextColor(Color.parseColor("#FFD700"))
                    textSize = 16f
                    setPadding(8, 8, 8, 8)
                }
                return view
            }
            
            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getDropDownView(position, convertView, parent)
                (view as? TextView)?.apply {
                    setTextColor(Color.WHITE)
                    textSize = 16f
                    setPadding(16, 16, 16, 16)
                    setBackgroundColor(Color.parseColor("#1E1E1E"))
                }
                return view
            }
        }
        
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        periodSpinner.adapter = adapter
        
        // Set the background tint of the spinner
        periodSpinner.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#FFD700"))
        
        // Set listener for period selection
        periodSpinner.setOnItemSelectedListener(object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedPeriod = when(position) {
                    0 -> Period.MONTH
                    1 -> Period.WEEK
                    2 -> Period.THREE_MONTHS
                    3 -> Period.SIX_MONTHS
                    4 -> Period.YEAR
                    else -> Period.MONTH
                }
                updateChart()
            }
            
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {
                // Do nothing
            }
        })
    }
    
    private fun setupCategoryChips() {
        // Clear existing chips
        categoryChipGroup.removeAllViews()
        
        // Add "All" category chip
        val allChip = Chip(requireContext()).apply {
            text = "All"
            isCheckable = true
            isChecked = selectedCategories.contains("All")
            chipBackgroundColor = ColorStateList.valueOf(Color.parseColor("#3C3C3C"))
            setTextColor(Color.WHITE)
            chipStrokeColor = ColorStateList.valueOf(Color.parseColor("#FFD700"))
            chipStrokeWidth = 1f
            id = View.generateViewId()
        }
        
        allChip.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                // Clear other selections and select only "All"
                selectedCategories.clear()
                selectedCategories.add("All")
                
                // Update other chips' state
                for (i in 0 until categoryChipGroup.childCount) {
                    val chip = categoryChipGroup.getChildAt(i) as? Chip
                    if (chip != null && chip.text != "All") {
                        chip.isChecked = false
                    }
                }
            } else if (selectedCategories.isEmpty()) {
                // Don't allow unchecking if no other categories are selected
                allChip.isChecked = true
                selectedCategories.add("All")
            }
            updateChart()
        }
        
        categoryChipGroup.addView(allChip)
        
        // Add individual category chips
        val categoryColorMap = mapOf(
            "Rent" to "#F44336",
            "Groceries" to "#4CAF50",
            "Transport" to "#2196F3",
            "Entertainment" to "#E91E63",
            "Utilities" to "#9C27B0"
        )
        
        for (category in allCategories) {
            val chip = Chip(requireContext()).apply {
                text = category
                isCheckable = true
                isChecked = selectedCategories.contains(category)
                chipBackgroundColor = ColorStateList.valueOf(Color.parseColor("#3C3C3C"))
                setTextColor(Color.WHITE)
                
                // Set stroke color based on category
                val strokeColor = categoryColorMap[category] ?: "#607D8B"
                chipStrokeColor = ColorStateList.valueOf(Color.parseColor(strokeColor))
                chipStrokeWidth = 1f
                id = View.generateViewId()
            }
            
            chip.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    // If a specific category is checked, uncheck "All"
                    selectedCategories.add(category)
                    
                    // Find and uncheck the "All" chip
                    for (i in 0 until categoryChipGroup.childCount) {
                        val c = categoryChipGroup.getChildAt(i) as? Chip
                        if (c != null && c.text == "All") {
                            c.isChecked = false
                            selectedCategories.remove("All")
                            break
                        }
                    }
                } else {
                    // Remove from selected categories
                    selectedCategories.remove(category)
                    
                    // If no categories are selected, check "All"
                    if (selectedCategories.isEmpty()) {
                        for (i in 0 until categoryChipGroup.childCount) {
                            val c = categoryChipGroup.getChildAt(i) as? Chip
                            if (c != null && c.text == "All") {
                                c.isChecked = true
                                selectedCategories.add("All")
                                break
                            }
                        }
                    }
                }
                updateChart()
            }
            
            categoryChipGroup.addView(chip)
        }
    }
    
    private fun setupLineChart() {
        lineChart.apply {
            // General chart settings
            description.isEnabled = false
            legend.apply {
                textColor = Color.WHITE
                textSize = 12f
                verticalAlignment = com.github.mikephil.charting.components.Legend.LegendVerticalAlignment.TOP
                horizontalAlignment = com.github.mikephil.charting.components.Legend.LegendHorizontalAlignment.RIGHT
                orientation = com.github.mikephil.charting.components.Legend.LegendOrientation.VERTICAL
                setDrawInside(false)
                form = com.github.mikephil.charting.components.Legend.LegendForm.CIRCLE
            }
            
            // X Axis settings
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                textColor = Color.WHITE
                textSize = 10f
                granularity = 1f
                setDrawGridLines(false)
                setDrawAxisLine(true)
                axisLineColor = Color.WHITE
            }
            
            // Left Y Axis settings
            axisLeft.apply {
                textColor = Color.WHITE
                textSize = 10f
                setDrawGridLines(true)
                gridColor = Color.parseColor("#444444")
                setDrawAxisLine(true)
                axisLineColor = Color.WHITE
            }
            
            // Right Y Axis (disabled)
            axisRight.isEnabled = false
            
            // Disable pinch zoom
            isScaleXEnabled = false
            isScaleYEnabled = false
            
            // Enable touch gestures
            setTouchEnabled(true)
            isDragEnabled = true
            setPinchZoom(false)
            
            // Empty data placeholder
            setNoDataText("No data available")
            setNoDataTextColor(Color.WHITE)
            
            // Disable grid background
            setDrawGridBackground(false)
            
            // Animation
            animateXY(1000, 1000)
        }
    }
    
    private fun updateChart() {
        val filteredTransactions = getFilteredTransactions()
        
        if (filteredTransactions.isEmpty()) {
            noDataTextView.visibility = View.VISIBLE
            lineChart.visibility = View.GONE
            updateStats(emptyList())
            return
        }
        
        noDataTextView.visibility = View.GONE
        lineChart.visibility = View.VISIBLE
        
        val dataSets = ArrayList<ILineDataSet>()
        val categoryColorMap = mapOf(
            "Rent" to Color.parseColor("#F44336"),
            "Groceries" to Color.parseColor("#4CAF50"),
            "Transport" to Color.parseColor("#2196F3"),
            "Entertainment" to Color.parseColor("#E91E63"),
            "Utilities" to Color.parseColor("#9C27B0"),
            "All" to Color.parseColor("#FFD700")
        )
        
        val dateLabels = ArrayList<String>()
        val dateFormatter = SimpleDateFormat("MMM dd", Locale.getDefault())
        
        // If "All" is selected, show total spending over time
        if (selectedCategories.contains("All")) {
            val entries = ArrayList<Entry>()
            val groupedByDate = filteredTransactions.groupBy { 
                val date = Date(it.date)
                val cal = Calendar.getInstance()
                cal.time = date
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                cal.timeInMillis
            }
            
            // Sort dates in ascending order
            val sortedDates = groupedByDate.keys.sorted()
            
            // Add each date's total to entries with a running cumulative sum
            var cumulativeTotal = 0.0f
            sortedDates.forEachIndexed { index, dateMillis ->
                val transactions = groupedByDate[dateMillis] ?: emptyList()
                val dailyTotal = transactions.sumOf { it.amount }.toFloat()
                cumulativeTotal += dailyTotal
                entries.add(Entry(index.toFloat(), cumulativeTotal))
                dateLabels.add(dateFormatter.format(Date(dateMillis)))
            }
            
            // Create the dataset with filled area under the line
            if (entries.isNotEmpty()) {
                val dataSet = LineDataSet(entries, "All Categories").apply {
                    color = categoryColorMap["All"] ?: Color.GRAY
                    setDrawCircles(true)
                    setCircleColor(color)
                    circleRadius = 4f
                    lineWidth = 2f
                    setDrawFilled(true) // Enable area filling
                    fillColor = color
                    fillAlpha = 50 // Semi-transparent fill
                    mode = LineDataSet.Mode.CUBIC_BEZIER // Smooth curves
                    valueTextColor = Color.WHITE
                    valueTextSize = 10f
                }
                dataSets.add(dataSet)
            }
        } else {
            // For individual categories, create separate lines
            // Get all unique dates from all filtered transactions
            val allDates = filteredTransactions.map { 
                val date = Date(it.date)
                val cal = Calendar.getInstance()
                cal.time = date
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                cal.timeInMillis
            }.distinct().sorted()
            
            // Prepare date labels once
            if (dateLabels.isEmpty()) {
                allDates.forEach { dateMillis ->
                    dateLabels.add(dateFormatter.format(Date(dateMillis)))
                }
            }
            
            for (category in selectedCategories) {
                val categoryTransactions = filteredTransactions.filter { it.category == category }
                
                // Group by date
                val groupedByDate = categoryTransactions.groupBy { 
                    val date = Date(it.date)
                    val cal = Calendar.getInstance()
                    cal.time = date
                    cal.set(Calendar.HOUR_OF_DAY, 0)
                    cal.set(Calendar.MINUTE, 0)
                    cal.set(Calendar.SECOND, 0)
                    cal.set(Calendar.MILLISECOND, 0)
                    cal.timeInMillis
                }
                
                // Create entries for this category with cumulative spending
                val entries = ArrayList<Entry>()
                var categoryTotal = 0.0f
                
                allDates.forEachIndexed { index, dateMillis ->
                    val transactions = groupedByDate[dateMillis] ?: emptyList()
                    val dailyTotal = transactions.sumOf { it.amount }.toFloat()
                    categoryTotal += dailyTotal
                    entries.add(Entry(index.toFloat(), categoryTotal))
                }
                
                // Create a dataset for this category
                if (entries.isNotEmpty()) {
                    val color = categoryColorMap[category] ?: Color.GRAY
                    val dataSet = LineDataSet(entries, category).apply {
                        this.color = color
                        setDrawCircles(true)
                        setCircleColor(color)
                        circleRadius = 4f
                        lineWidth = 2f
                        setDrawFilled(true) // Enable area filling
                        fillColor = color
                        fillAlpha = 50 // Semi-transparent fill
                        mode = LineDataSet.Mode.CUBIC_BEZIER // Smooth curves
                        valueTextColor = Color.WHITE
                        valueTextSize = 10f
                    }
                    dataSets.add(dataSet)
                }
            }
        }
        
        // Create the line data and set it to the chart
        if (dataSets.isNotEmpty()) {
            val lineData = LineData(dataSets)
            lineChart.data = lineData
            
            // Set X axis labels to dates
            lineChart.xAxis.valueFormatter = IndexAxisValueFormatter(dateLabels)
            lineChart.xAxis.labelCount = min(dateLabels.size, 5) // Limit to 5 labels to avoid crowding
            
            // Update chart and stats
            lineChart.invalidate()
            updateStats(filteredTransactions)
        } else {
            lineChart.clear()
            noDataTextView.visibility = View.VISIBLE
            updateStats(emptyList())
        }
    }
    
    private fun getFilteredTransactions(): List<Transaction> {
        // Get the time range based on selected period
        val endDate = Calendar.getInstance()
        val startDate = Calendar.getInstance()
        
        when (selectedPeriod) {
            Period.WEEK -> startDate.add(Calendar.DAY_OF_YEAR, -7)
            Period.MONTH -> startDate.add(Calendar.MONTH, -1)
            Period.THREE_MONTHS -> startDate.add(Calendar.MONTH, -3)
            Period.SIX_MONTHS -> startDate.add(Calendar.MONTH, -6)
            Period.YEAR -> startDate.add(Calendar.YEAR, -1)
        }
        
        // Filter transactions by date and category
        return transactions.filter { transaction ->
            // Only include expenses
            val isExpense = transaction.type == Transaction.Type.EXPENSE
            
            // Date is within range
            val isInDateRange = transaction.date in startDate.timeInMillis..endDate.timeInMillis
            
            // Category matches selection
            val isCategorySelected = if (selectedCategories.contains("All")) {
                true
            } else {
                selectedCategories.contains(transaction.category)
            }
            
            isExpense && isInDateRange && isCategorySelected
        }
    }
    
    private fun updateStats(filteredTransactions: List<Transaction>) {
        if (filteredTransactions.isEmpty()) {
            totalSpentTextView.text = "R0.00"
            averageSpentTextView.text = "R0.00"
            trendTextView.text = "--"
            return
        }
        
        // Calculate total
        val totalSpent = filteredTransactions.sumOf { it.amount }
        totalSpentTextView.text = "R%.2f".format(totalSpent)
        
        // Calculate average per day
        val firstDate = filteredTransactions.minByOrNull { it.date }?.date ?: System.currentTimeMillis()
        val lastDate = filteredTransactions.maxByOrNull { it.date }?.date ?: System.currentTimeMillis()
        
        val daysDiff = max(1.0, TimeUnit.MILLISECONDS.toDays(lastDate - firstDate).toDouble())
        val average = totalSpent / daysDiff
        
        averageSpentTextView.text = "R%.2f".format(average)
        
        // Calculate trend
        val trend = calculateTrend(filteredTransactions)
        
        trendTextView.text = when {
            trend > 0 -> "+%.1f%%".format(trend)
            trend < 0 -> "%.1f%%".format(trend)
            else -> "0.0%"
        }
        
        trendTextView.setTextColor(when {
            trend > 0 -> Color.parseColor("#E91E1E") // Red for increased spending (bad)
            trend < 0 -> Color.parseColor("#4CAF50") // Green for decreased spending (good)
            else -> Color.parseColor("#FFD700") // Gold for no change
        })
    }
    
    private fun calculateTrend(transactions: List<Transaction>): Double {
        if (transactions.size < 2) return 0.0
        
        // Split transactions into two equal time periods
        val sortedTransactions = transactions.sortedBy { it.date }
        val midPoint = sortedTransactions.size / 2
        
        val firstHalf = sortedTransactions.subList(0, midPoint)
        val secondHalf = sortedTransactions.subList(midPoint, sortedTransactions.size)
        
        val firstTotal = firstHalf.sumOf { it.amount }
        val secondTotal = secondHalf.sumOf { it.amount }
        
        // Avoid division by zero
        if (firstTotal == 0.0) return if (secondTotal > 0) 100.0 else 0.0
        
        // Calculate percentage change
        return ((secondTotal - firstTotal) / firstTotal) * 100.0
    }
    
    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }
    
    companion object {
        fun newInstance(): HabitsFragment {
            return HabitsFragment()
        }
    }
} 