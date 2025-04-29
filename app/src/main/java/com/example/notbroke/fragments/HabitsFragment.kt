package com.example.notbroke.fragments

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.notbroke.R
import com.example.notbroke.models.Transaction
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

    private lateinit var lineChart: LineChart
    private lateinit var titleTextView: TextView
    private lateinit var monthSpinner: Spinner
    private var selectedMonth: Int = Calendar.getInstance().get(Calendar.MONTH)
    private lateinit var btnAddTestTransaction: Button
    
    private lateinit var repositoryFactory: RepositoryFactory
    private val transactionRepository by lazy { repositoryFactory.transactionRepository }
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

        // Find views
        titleTextView = view.findViewById(R.id.titleTextView)
        lineChart = view.findViewById(R.id.lineChart)
        monthSpinner = view.findViewById(R.id.monthSpinner)
        btnAddTestTransaction = view.findViewById(R.id.btnAddTestTransaction)

        btnAddTestTransaction.setOnClickListener {
            addTestTransaction()
        }

        setupLineChart()
        setupMonthSpinner()
        observeTransactionsForMonth(selectedMonth)

        return view
    }

    private fun setupLineChart() {
        lineChart.apply {
            description.isEnabled = false
            setTouchEnabled(true)
            setDragEnabled(true)
            setScaleEnabled(true)
            setPinchZoom(true)
            setBackgroundColor(Color.TRANSPARENT)

            // Animate X and Y axes
            animateX(1000)
            animateY(1000)

            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                textColor = Color.WHITE
                granularity = 1f
                axisMinimum = 1f
                axisMaximum = 31f
                labelRotationAngle = 0f
                setDrawAxisLine(true)
                setDrawLabels(true)
                valueFormatter = object : ValueFormatter() {
                    override fun getFormattedValue(value: Float): String {
                        return value.toInt().toString()
                    }
                }
            }

            axisLeft.apply {
                textColor = Color.WHITE
                valueFormatter = object : ValueFormatter() {
                    override fun getFormattedValue(value: Float): String {
                        return "R ${value.toInt()}"
                    }
                }
            }

            axisRight.isEnabled = false // Disable right Y-axis
            legend.isEnabled = false // Hide legend
        }
    }

    private fun setupMonthSpinner() {
        val months = listOf(
            "January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December"
        )

        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, months)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        monthSpinner.adapter = adapter
        monthSpinner.setSelection(selectedMonth) // Default to current month

        monthSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedMonth = position
                observeTransactionsForMonth(selectedMonth)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) { }
        }
    }

    private fun observeTransactionsForMonth(month: Int) {
        val calendarStart = Calendar.getInstance()
        calendarStart.set(Calendar.YEAR, Calendar.getInstance().get(Calendar.YEAR))
        calendarStart.set(Calendar.MONTH, month)
        calendarStart.set(Calendar.DAY_OF_MONTH, 1)
        calendarStart.set(Calendar.HOUR_OF_DAY, 0)
        calendarStart.set(Calendar.MINUTE, 0)
        calendarStart.set(Calendar.SECOND, 0)
        calendarStart.set(Calendar.MILLISECOND, 0)

        val calendarEnd = calendarStart.clone() as Calendar
        calendarEnd.add(Calendar.MONTH, 1)

        lifecycleScope.launch {
            try {
                transactionRepository.getTransactionsByDateRange(
                    calendarStart.timeInMillis,
                    calendarEnd.timeInMillis,
                    authService.getCurrentUserId()
                ).collectLatest { transactions ->
                    val daySums = mutableMapOf<Int, Double>()
                    
                    // Filter and process transactions
                    transactions.filter { it.type == Transaction.Type.EXPENSE }
                        .forEach { transaction ->
                            val calendar = Calendar.getInstance()
                            calendar.timeInMillis = transaction.date
                            val dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH)
                            daySums[dayOfMonth] = (daySums[dayOfMonth] ?: 0.0) + transaction.amount
                        }

                    val entries = ArrayList<Entry>()
                    for ((day, totalAmount) in daySums) {
                        entries.add(Entry(day.toFloat(), totalAmount.toFloat()))
                    }

                    updateLineChart(entries)
                }
            } catch (e: Exception) {
                Log.e("HabitsFragment", "Error loading transactions", e)
                Toast.makeText(context, "Error loading transactions: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateLineChart(entries: List<Entry>) {
        if (entries.isEmpty()) {
            lineChart.clear()
            lineChart.setNoDataText("No spending history found.")
            lineChart.setNoDataTextColor(Color.LTGRAY)
            return
        }

        val dataSet = LineDataSet(entries, "Spending (R)").apply {
            color = Color.YELLOW
            valueTextColor = Color.WHITE
            lineWidth = 2f
            setDrawCircles(true)
            circleRadius = 5f
            setCircleColor(Color.CYAN)
            mode = LineDataSet.Mode.CUBIC_BEZIER // Smooth curve
            valueTextSize = 10f
        }

        val lineData = LineData(dataSet)
        lineChart.data = lineData
        lineChart.invalidate()
    }

    private fun addTestTransaction() {
        val userId = authService.getCurrentUserId() ?: return

        // --- MANUAL CONFIGURATION ---
        val amount = 100.0 // <<== SET your custom test amount here
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.YEAR, 2025) // <<== SET year here
        calendar.set(Calendar.MONTH, Calendar.APRIL) // <<== SET month here
        calendar.set(Calendar.DAY_OF_MONTH, 5) // <<== SET day here
        calendar.set(Calendar.HOUR_OF_DAY, 12) // Optional: time
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        // ----------------------------

        val transaction = Transaction(
            type = Transaction.Type.EXPENSE,
            amount = amount,
            description = "Test Transaction",
            category = "Test",
            date = calendar.timeInMillis
        )

        lifecycleScope.launch {
            try {
                transactionRepository.saveTransaction(transaction, authService.getCurrentUserId())
                Log.d("HabitsFragment", "Test transaction added successfully!")
                observeTransactionsForMonth(selectedMonth) // Refresh graph
            } catch (e: Exception) {
                Log.e("HabitsFragment", "Failed to add test transaction", e)
                Toast.makeText(context, "Failed to add test transaction: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}