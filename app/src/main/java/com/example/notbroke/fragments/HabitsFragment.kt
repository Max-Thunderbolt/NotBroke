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
import androidx.fragment.app.Fragment
import com.example.notbroke.R
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*

class HabitsFragment : Fragment() {

    private lateinit var lineChart: LineChart
    private lateinit var titleTextView: TextView
    private lateinit var monthSpinner: Spinner
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private var selectedMonth: Int = Calendar.getInstance().get(Calendar.MONTH)
    private lateinit var btnAddTestTransaction: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
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
        loadTransactionsForMonth(selectedMonth)

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
                loadTransactionsForMonth(selectedMonth)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) { }
        }
    }

    private fun loadTransactionsForMonth(month: Int) {
        val userId = auth.currentUser?.uid ?: return

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

        db.collection("users")
            .document(userId)
            .collection("transactions")
            .whereGreaterThanOrEqualTo("date", com.google.firebase.Timestamp(calendarStart.time))
            .whereLessThan("date", com.google.firebase.Timestamp(calendarEnd.time))
            .get()
            .addOnSuccessListener { documents ->
                val daySums = mutableMapOf<Int, Double>()

                for (doc in documents) {
                    val amount = doc.getDouble("amount")
                    val timestamp = doc.getTimestamp("date")?.toDate()
                    val type = doc.getString("type")

                    if (amount != null && timestamp != null && type == "EXPENSE") {
                        val calendar = Calendar.getInstance()
                        calendar.time = timestamp
                        val dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH)
                        daySums[dayOfMonth] = (daySums[dayOfMonth] ?: 0.0) + amount
                    }
                }

                val entries = ArrayList<Entry>()
                for ((day, totalAmount) in daySums) {
                    entries.add(Entry(day.toFloat(), totalAmount.toFloat()))
                }

                updateLineChart(entries)
            }
            .addOnFailureListener { e ->
                e.printStackTrace()
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
        val userId = auth.currentUser?.uid ?: return

        // --- MANUAL CONFIGURATION ---
        val amount = 100 // <<== SET your custom test amount here
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.YEAR, 2025) // <<== SET year here
        calendar.set(Calendar.MONTH, Calendar.APRIL) // <<== SET month here
        calendar.set(Calendar.DAY_OF_MONTH, 5) // <<== SET day here
        calendar.set(Calendar.HOUR_OF_DAY, 12) // Optional: time
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        // ----------------------------

        val transaction = hashMapOf(
            "userId" to userId,
            "amount" to amount,
            "type" to "EXPENSE", // <<== Type must be "EXPENSE" to show on chart
            "date" to com.google.firebase.Timestamp(calendar.time)
        )

        db.collection("users")
            .document(userId)
            .collection("transactions")
            .add(transaction)
            .addOnSuccessListener {
                Log.d("HabitsFragment", "Test transaction added successfully!")
                loadTransactionsForMonth(selectedMonth) // Refresh graph
            }
            .addOnFailureListener { e ->
                Log.e("HabitsFragment", "Failed to add test transaction", e)
            }
    }
}