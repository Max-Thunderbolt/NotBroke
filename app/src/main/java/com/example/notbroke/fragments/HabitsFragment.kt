package com.example.notbroke.fragments

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.notbroke.R
import com.example.notbroke.repositories.RepositoryFactory
import com.example.notbroke.services.AuthService
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.*

class HabitsFragment : Fragment() {

    private lateinit var lineChartTop: LineChart
    private lateinit var lineChartBottom: LineChart
    private lateinit var titleTextView: TextView
    private lateinit var lastUpdatedTextView: TextView
    private lateinit var repositoryFactory: RepositoryFactory
    private val authService = AuthService.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repositoryFactory = RepositoryFactory.getInstance(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_habits, container, false)
        titleTextView = view.findViewById(R.id.titleTextView)
        lastUpdatedTextView = view.findViewById(R.id.lastUpdatedTextView)
        lineChartTop = view.findViewById(R.id.lineChartTop)
        lineChartBottom = view.findViewById(R.id.lineChartBottom)

        setupLineChart(lineChartTop)
        setupLineChart(lineChartBottom)

        loadCurrentMonthSpending()
        loadYearlySpendingTotals()

        return view
    }

    private fun setupLineChart(chart: LineChart) {
        chart.apply {
            description.isEnabled = false
            setTouchEnabled(true)
            setScaleEnabled(true)
            isDragEnabled = true
            setBackgroundColor(Color.TRANSPARENT)
            animateX(1000)
            axisRight.isEnabled = false
            legend.isEnabled = false

            axisLeft.apply {
                textColor = Color.WHITE
                valueFormatter = object : ValueFormatter() {
                    override fun getFormattedValue(value: Float): String {
                        return "R ${value.toInt()}"
                    }
                }
            }

            marker = SpendingMarkerView(requireContext())
        }
    }

    private fun loadCurrentMonthSpending() {
        val calendarStart = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val calendarEnd = Calendar.getInstance()

        lifecycleScope.launch {
            try {
                val userId = authService.getCurrentUserId() ?: return@launch
                repositoryFactory.transactionRepository
                    .getTransactionsByDateRange(calendarStart.timeInMillis, calendarEnd.timeInMillis, userId)
                    .collectLatest { transactions ->
                        val dailyTotals = TreeMap<Int, Double>()

                        transactions.filter { it.type == com.example.notbroke.models.Transaction.Type.EXPENSE }
                            .forEach { transaction ->
                                val cal = Calendar.getInstance().apply { timeInMillis = transaction.date }
                                val day = cal.get(Calendar.DAY_OF_MONTH)
                                dailyTotals[day] = (dailyTotals[day] ?: 0.0) + transaction.amount
                            }

                        val entries = mutableListOf<Entry>()
                        var runningTotal = 0.0
                        for (day in 1..calendarEnd.get(Calendar.DAY_OF_MONTH)) {
                            runningTotal += dailyTotals[day] ?: 0.0
                            entries.add(Entry(day.toFloat(), runningTotal.toFloat()))
                        }

                        lineChartTop.xAxis.apply {
                            setDrawLabels(false)
                            setDrawGridLines(false)
                            setDrawAxisLine(false)
                        }

                        updateLineChart(entries, lineChartTop)
                        fadeInChart(lineChartTop)
                        updateLastUpdatedText()
                    }
            } catch (e: Exception) {
                Log.e("HabitsFragment", "Error loading current month data", e)
                Toast.makeText(context, "Error loading current month data: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadYearlySpendingTotals() {
        val calendarStart = Calendar.getInstance().apply {
            set(Calendar.MONTH, Calendar.JANUARY)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val calendarEnd = Calendar.getInstance()

        lifecycleScope.launch {
            try {
                val userId = authService.getCurrentUserId() ?: return@launch
                repositoryFactory.transactionRepository
                    .getTransactionsByDateRange(calendarStart.timeInMillis, calendarEnd.timeInMillis, userId)
                    .collectLatest { transactions ->
                        val monthTotals = mutableMapOf<Int, Double>()

                        transactions.filter { it.type == com.example.notbroke.models.Transaction.Type.EXPENSE }
                            .forEach { transaction ->
                                val cal = Calendar.getInstance().apply { timeInMillis = transaction.date }
                                val month = cal.get(Calendar.MONTH)
                                monthTotals[month] = (monthTotals[month] ?: 0.0) + transaction.amount
                            }

                        val entries = mutableListOf<Entry>()
                        val monthLabels = mutableListOf<String>()

                        for (month in Calendar.JANUARY..calendarEnd.get(Calendar.MONTH)) {
                            val total = monthTotals[month] ?: 0.0
                            entries.add(Entry(month.toFloat(), total.toFloat()))

                            val label = Calendar.getInstance().apply { set(Calendar.MONTH, month) }
                                .getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.getDefault()) ?: ""
                            monthLabels.add(label)
                        }

                        lineChartBottom.xAxis.apply {
                            setDrawLabels(true)
                            setDrawGridLines(false)
                            setDrawAxisLine(true)
                            granularity = 1f
                            valueFormatter = object : ValueFormatter() {
                                override fun getFormattedValue(value: Float): String {
                                    val index = value.toInt()
                                    return monthLabels.getOrNull(index) ?: ""
                                }
                            }
                            position = XAxis.XAxisPosition.BOTTOM
                            textColor = Color.WHITE
                        }

                        updateLineChart(entries, lineChartBottom)
                        fadeInChart(lineChartBottom)
                    }
            } catch (e: Exception) {
                Log.e("HabitsFragment", "Error loading yearly totals", e)
                Toast.makeText(context, "Error loading yearly totals: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateLineChart(entries: List<Entry>, chart: LineChart) {
        val dataSet = LineDataSet(entries, "").apply {
            color = Color.YELLOW
            valueTextColor = Color.WHITE
            lineWidth = 2f
            setDrawCircles(true)
            circleRadius = 4f
            setCircleColor(Color.YELLOW)
            setDrawValues(false)
            mode = LineDataSet.Mode.LINEAR
        }
        chart.data = LineData(dataSet)
        chart.invalidate()
    }

    private fun fadeInChart(chart: LineChart) {
        chart.alpha = 0f
        chart.animate().alpha(1f).setDuration(800).start()
    }

    private fun updateLastUpdatedText() {
        val today = Calendar.getInstance()
        val dateStr = "${today.get(Calendar.DAY_OF_MONTH)} ${today.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault())}"
        lastUpdatedTextView.text = "Last updated: $dateStr"
    }
}