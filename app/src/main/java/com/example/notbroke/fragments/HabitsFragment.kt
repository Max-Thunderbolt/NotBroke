package com.example.notbroke.fragments

import com.google.android.material.datepicker.MaterialDatePicker
import android.app.DatePickerDialog
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.notbroke.R
import com.example.notbroke.models.Transaction
import com.example.notbroke.repositories.RepositoryFactory
import com.example.notbroke.services.AuthService
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.*
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import java.text.SimpleDateFormat

class HabitsFragment : Fragment() {

    private lateinit var lineChartTop: LineChart
    private lateinit var lineChartBottom: LineChart
    private lateinit var monthSpinner: Spinner
    private lateinit var compareYearSpinner: Spinner
    private lateinit var compareMonth1Spinner: Spinner
    private lateinit var compareMonth2Spinner: Spinner
    private lateinit var titleTextViewTop: TextView
    private lateinit var titleTextViewBottom: TextView
    private lateinit var amountInput: EditText
    private lateinit var dateButton: Button
    private lateinit var addTestTransactionBtn: Button
    private var selectedDate: Calendar = Calendar.getInstance()
    private lateinit var lineChartCategory: LineChart
    private lateinit var categorySpinner: Spinner
    private lateinit var categoryMonthSpinner: Spinner
    private lateinit var startDateButton: Button
    private lateinit var endDateButton: Button
    private lateinit var lineChartDateRange: LineChart
    private var startDate: Calendar? = null
    private var endDate: Calendar? = null

    private lateinit var repositoryFactory: RepositoryFactory
    private val transactionRepository by lazy { repositoryFactory.transactionRepository }
    private val authService = AuthService.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_habits, container, false)

        // Bind views

        titleTextViewTop = view.findViewById(R.id.titleTextViewTop)
        titleTextViewBottom = view.findViewById(R.id.titleTextViewBottom)
        lineChartTop = view.findViewById(R.id.lineChartTop)
        lineChartBottom = view.findViewById(R.id.lineChartBottom)
        monthSpinner = view.findViewById(R.id.monthSpinner)
        compareYearSpinner = view.findViewById(R.id.compareYearSpinner)
        compareMonth1Spinner = view.findViewById(R.id.compareMonth1Spinner)
        compareMonth2Spinner = view.findViewById(R.id.compareMonth2Spinner)
        amountInput = view.findViewById(R.id.amountInput)
        dateButton = view.findViewById(R.id.dateButton)
        addTestTransactionBtn = view.findViewById(R.id.addTestTransactionBtn)
        lineChartCategory = view.findViewById(R.id.lineChartCategory)
        categorySpinner = view.findViewById(R.id.categorySpinner)
        categoryMonthSpinner = view.findViewById(R.id.categoryMonthSpinner)
        startDateButton = view.findViewById(R.id.startDateButton)
        endDateButton = view.findViewById(R.id.endDateButton)
        lineChartDateRange = view.findViewById(R.id.lineChartDateRange)

        setupLineChart(lineChartDateRange)
        setupDateRangePicker()


        repositoryFactory = RepositoryFactory.getInstance(requireContext())

        setupLineChart(lineChartTop)
        setupLineChart(lineChartBottom)
        setupLineChart(lineChartCategory)
        setupMonthSpinner()
        setupComparisonSpinners()
        setupTestTransaction()
        setupCategoryAndMonthSpinners()

        return view
    }

    private fun setupLineChart(chart: LineChart) {
        chart.apply {
            description.isEnabled = false
            setTouchEnabled(true)
            isDragEnabled = true
            setScaleEnabled(true)
            setPinchZoom(true)
            setBackgroundColor(Color.TRANSPARENT)
            axisRight.isEnabled = false
            legend.isEnabled = true

            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                textColor = Color.WHITE
                granularity = 1f
                setDrawGridLines(false)

                if (chart.id == R.id.lineChartDateRange) {
                    setDrawLabels(false)
                    setDrawAxisLine(false)
                } else {
                    labelCount = 15
                    setLabelCount(15, true)
                }
            }

            axisLeft.apply {
                textColor = Color.WHITE
                valueFormatter = object : com.github.mikephil.charting.formatter.ValueFormatter() {
                    override fun getFormattedValue(value: Float): String {
                        return "R%.2f".format(value)
                    }
                }
            }

            animateX(1000)
            animateY(1000)
        }
    }

    private fun setupMonthSpinner() {
        val months = resources.getStringArray(R.array.months_array)
        val adapter = ArrayAdapter(requireContext(), R.layout.spinner_item2, months)
        adapter.setDropDownViewResource(R.layout.spinner_background2)
        monthSpinner.adapter = adapter

        val currentMonth = Calendar.getInstance().get(Calendar.MONTH)
        monthSpinner.setSelection(currentMonth)

        monthSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                loadCurrentMonthSpending(position)
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    private fun setupComparisonSpinners() {
        val months = resources.getStringArray(R.array.months_array)
        val adapter = ArrayAdapter(requireContext(), R.layout.spinner_item2, months)
        adapter.setDropDownViewResource(R.layout.spinner_background2)
        compareMonth1Spinner.adapter = adapter
        compareMonth2Spinner.adapter = adapter

        val currentMonth = Calendar.getInstance().get(Calendar.MONTH)
        compareMonth1Spinner.setSelection(0)
        compareMonth2Spinner.setSelection(currentMonth)

        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        val years = (currentYear - 5..currentYear).map { it.toString() }.toTypedArray()
        val yearAdapter = ArrayAdapter(requireContext(), R.layout.spinner_item2, years)
        yearAdapter.setDropDownViewResource(R.layout.spinner_background2)
        compareYearSpinner.adapter = yearAdapter
        compareYearSpinner.setSelection(years.size - 1)

        val listener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val month1 = compareMonth1Spinner.selectedItemPosition
                val month2 = compareMonth2Spinner.selectedItemPosition
                val year = compareYearSpinner.selectedItem.toString().toInt()
                loadComparisonGraph(month1, month2, year)
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        compareMonth1Spinner.onItemSelectedListener = listener
        compareMonth2Spinner.onItemSelectedListener = listener
        compareYearSpinner.onItemSelectedListener = listener
    }

    private fun setupTestTransaction() {
        dateButton.setOnClickListener {
            val picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Select Date")
                .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
                .build()

            picker.addOnPositiveButtonClickListener { selection ->
                selectedDate.timeInMillis = selection
                val formatted = java.text.SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(selectedDate.time)
                dateButton.text = formatted
            }

            picker.show(parentFragmentManager, "MATERIAL_DATE_PICKER")
        }


        addTestTransactionBtn.setOnClickListener {
            val amount = amountInput.text.toString().toDoubleOrNull()
            if (amount != null) {
                addTestTransaction(selectedDate.timeInMillis, amount)
            } else {
                Toast.makeText(requireContext(), "Enter a valid amount", Toast.LENGTH_SHORT).show()
            }
        }
    }
    private fun setupDateRangePicker() {
        startDateButton.setOnClickListener {
            val picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Select Start Date")
                .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
                .build()

            picker.addOnPositiveButtonClickListener { selection ->
                startDate = Calendar.getInstance().apply {
                    timeInMillis = selection
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val formatted = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(startDate!!.time)
                startDateButton.text = "Start: $formatted"
                tryLoadDateRangeSpending()
            }

            picker.show(parentFragmentManager, "START_DATE_PICKER")
        }


        endDateButton.setOnClickListener {
            val picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Select End Date")
                .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
                .build()

            picker.addOnPositiveButtonClickListener { selection ->
                endDate = Calendar.getInstance().apply {
                    timeInMillis = selection
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                    set(Calendar.SECOND, 59)
                    set(Calendar.MILLISECOND, 999)
                }
                val formatted = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(endDate!!.time)
                endDateButton.text = "End: $formatted"
                tryLoadDateRangeSpending()
            }

            picker.show(parentFragmentManager, "END_DATE_PICKER")
        }

    }

    private fun addTestTransaction(date: Long, amount: Double) {
        val userId = authService.getCurrentUserId() ?: return

        val transaction = Transaction(
            firestoreId = null,
            userId = userId,
            type = Transaction.Type.EXPENSE,
            amount = amount,
            description = "Test Transaction",
            category = "Test",
            date = date,
            receiptImageUri = null
        )

        lifecycleScope.launch {
            transactionRepository.saveTransaction(transaction, userId)
            val selectedMonth = Calendar.getInstance().apply { timeInMillis = date }.get(Calendar.MONTH)
            loadCurrentMonthSpending(selectedMonth)
        }
    }

    private fun loadCurrentMonthSpending(month: Int) {
        val userId = authService.getCurrentUserId() ?: return

        val calendarStart = Calendar.getInstance().apply {
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val calendarEnd = (calendarStart.clone() as Calendar).apply {
            add(Calendar.MONTH, 1)
        }

        lifecycleScope.launch {
            try {
                val transactions = transactionRepository.getTransactionsByDateRange(
                    calendarStart.timeInMillis,
                    calendarEnd.timeInMillis,
                    userId
                ).first().filter { it.type == Transaction.Type.EXPENSE }

                val dailyTotals = mutableMapOf<Int, Double>()
                transactions.forEach {
                    val day = Calendar.getInstance().apply { timeInMillis = it.date }.get(Calendar.DAY_OF_MONTH)
                    dailyTotals[day] = (dailyTotals[day] ?: 0.0) + it.amount
                }

                var runningTotal = 0.0
                val entries = mutableListOf<Entry>()
                for (day in 1..31) {
                    runningTotal += dailyTotals[day] ?: 0.0
                    entries.add(Entry(day.toFloat(), runningTotal.toFloat()))
                }

                val dataSet = LineDataSet(entries, "Monthly Spending").apply {
                    color = Color.CYAN
                    setDrawCircles(false)
                    setDrawValues(false)
                    lineWidth = 2f
                    setDrawFilled(true)
                    fillColor = Color.CYAN
                }

                lineChartTop.data = LineData(dataSet)
                lineChartTop.invalidate()
            } catch (e: Exception) {
                Log.e("HabitsFragment", "Error loading spending data", e)
            }
        }
    }

    private fun loadComparisonGraph(month1: Int, month2: Int, year: Int) {
        val userId = authService.getCurrentUserId() ?: return

        lifecycleScope.launch {
            try {
                val dataSets = mutableListOf<ILineDataSet>()
                val monthNames = resources.getStringArray(R.array.months_array)
                val monthsToLoad = listOf(month1 to monthNames[month1], month2 to monthNames[month2])
                val colors = listOf(Color.MAGENTA, Color.YELLOW)
                val legend1: TextView = requireView().findViewById(R.id.legendMonth1)
                val legend2: TextView = requireView().findViewById(R.id.legendMonth2)

                legend1.text = monthNames[month1]
                legend2.text = monthNames[month2]

                for ((index, pair) in monthsToLoad.withIndex()) {
                    val (month, label) = pair

                    val startCal = Calendar.getInstance().apply {
                        set(Calendar.YEAR, year)
                        set(Calendar.MONTH, month)
                        set(Calendar.DAY_OF_MONTH, 1)
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }

                    val endCal = (startCal.clone() as Calendar).apply { add(Calendar.MONTH, 1) }

                    val transactions = transactionRepository.getTransactionsByDateRange(
                        startCal.timeInMillis,
                        endCal.timeInMillis,
                        userId
                    ).first().filter { it.type == Transaction.Type.EXPENSE }

                    val dailyTotals = mutableMapOf<Int, Double>()
                    transactions.forEach {
                        val day = Calendar.getInstance().apply { timeInMillis = it.date }.get(Calendar.DAY_OF_MONTH)
                        dailyTotals[day] = (dailyTotals[day] ?: 0.0) + it.amount
                    }

                    var runningTotal = 0.0
                    val entries = mutableListOf<Entry>()
                    for (day in 1..31) {
                        runningTotal += dailyTotals[day] ?: 0.0
                        entries.add(Entry(day.toFloat(), runningTotal.toFloat()))
                    }

                    val dataSet = LineDataSet(entries, label).apply {
                        color = colors[index]
                        setDrawCircles(false)
                        setDrawValues(false)
                        lineWidth = 2f
                        setDrawFilled(true)
                        fillColor = colors[index]
                    }

                    dataSets.add(dataSet)
                }

                lineChartBottom.data = LineData(dataSets)
                lineChartBottom.invalidate()

            } catch (e: Exception) {
                Log.e("HabitsFragment", "Error loading comparison graph", e)
            }
        }
    }
    private fun setupCategoryAndMonthSpinners() {
        val categories = listOf(
            "Air Travel", "Banking","Clothing & Fashion","Electronics & Appliances" ,"Groceries & Household", "Home & Garden", "Food & Beverage", "Entertainment",
            "Financial Services", "Insurance", "Gaming & Gambling", "Education", "Health & Wellness", "Mobile & Internet", "Personal Care & Beauty",
            "Property & Accommodation", "Restaurants & Takeaways", "Shopping & Retail", "Sports & Outdoors", "Transport & Automotive", "Travel & Tourism",
            "Utilities & Municipal Services", "Other"
        )
        val categoryAdapter = ArrayAdapter(requireContext(), R.layout.spinner_item2, categories)
        categoryAdapter.setDropDownViewResource(R.layout.spinner_background2)
        categorySpinner.adapter = categoryAdapter

        val months = resources.getStringArray(R.array.months_array)
        val monthAdapter = ArrayAdapter(requireContext(), R.layout.spinner_item2, months)
        monthAdapter.setDropDownViewResource(R.layout.spinner_background2)
        categoryMonthSpinner.adapter = monthAdapter

        val currentMonth = Calendar.getInstance().get(Calendar.MONTH)
        categoryMonthSpinner.setSelection(currentMonth)

        val listener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val selectedCategory = categorySpinner.selectedItem as String
                val selectedMonth = categoryMonthSpinner.selectedItemPosition
                loadCategorySpending(selectedCategory, selectedMonth)
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        categorySpinner.onItemSelectedListener = listener
        categoryMonthSpinner.onItemSelectedListener = listener
    }
    private fun tryLoadDateRangeSpending() {
        val userId = authService.getCurrentUserId() ?: return
        val start = startDate ?: return
        val end = endDate ?: return

        if (end.timeInMillis < start.timeInMillis) {
            Toast.makeText(requireContext(), "End date must be after start date", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            try {
                val transactions = transactionRepository.getTransactionsByDateRange(
                    start.timeInMillis, end.timeInMillis, userId
                ).first().filter { it.type == Transaction.Type.EXPENSE }

                val entries = mutableListOf<Entry>()
                val sorted = transactions.sortedBy { it.date }

                var runningTotal = 0.0f
                sorted.forEachIndexed { index, tx ->
                    runningTotal += tx.amount.toFloat()
                    entries.add(Entry(index.toFloat(), runningTotal))
                }

                val dataSet = LineDataSet(entries, "Date Range Spending").apply {
                    color = Color.MAGENTA
                    fillColor = Color.MAGENTA
                    setDrawFilled(true)
                    setDrawCircles(false)
                    setDrawValues(false)
                    lineWidth = 2f
                }

                lineChartDateRange.data = LineData(dataSet)
                lineChartDateRange.invalidate()
            } catch (e: Exception) {
                Log.e("HabitsFragment", "Error loading date range graph", e)
            }
        }
    }
    private fun loadCategorySpending(category: String, month: Int) {
        val userId = authService.getCurrentUserId() ?: return

        val calendarStart = Calendar.getInstance().apply {
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val calendarEnd = (calendarStart.clone() as Calendar).apply {
            add(Calendar.MONTH, 1)
        }

        lifecycleScope.launch {
            try {
                val transactions = transactionRepository.getTransactionsByDateRange(
                    calendarStart.timeInMillis,
                    calendarEnd.timeInMillis,
                    userId
                ).first().filter {
                    it.type == Transaction.Type.EXPENSE && it.category.equals(category, ignoreCase = true)
                }

                val dailyTotals = mutableMapOf<Int, Double>()
                transactions.forEach {
                    val day = Calendar.getInstance().apply { timeInMillis = it.date }.get(Calendar.DAY_OF_MONTH)
                    dailyTotals[day] = (dailyTotals[day] ?: 0.0) + it.amount
                }

                var runningTotal = 0.0
                val entries = mutableListOf<Entry>()
                for (day in 1..31) {
                    runningTotal += dailyTotals[day] ?: 0.0
                    entries.add(Entry(day.toFloat(), runningTotal.toFloat()))
                }

                val dataSet = LineDataSet(entries, "$category Spending").apply {
                    color = Color.CYAN
                    setDrawCircles(false)
                    setDrawValues(false)
                    lineWidth = 2f
                    setDrawFilled(true)
                    fillColor = Color.CYAN
                }

                lineChartCategory.data = LineData(dataSet)
                lineChartCategory.invalidate()

            } catch (e: Exception) {
                Log.e("HabitsFragment", "Error loading category spending", e)
            }
        }
    }
}