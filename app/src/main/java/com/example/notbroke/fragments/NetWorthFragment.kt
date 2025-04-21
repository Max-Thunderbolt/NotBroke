package com.example.notbroke.fragments

import android.app.DatePickerDialog
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.MaterialDatePicker.Builder
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.notbroke.R
import com.example.notbroke.models.NetWorthEntry
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class NetWorthFragment : Fragment() {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var totalNetWorthText: TextView
    private lateinit var addAmountEditText: EditText
    private lateinit var addDateButton: Button
    private lateinit var addButton: Button
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: NetWorthAdapter
    private var selectedDate: Date? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_net_worth, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        totalNetWorthText = view.findViewById(R.id.totalNetWorthText)
        addAmountEditText = view.findViewById(R.id.addAmountEditText)
        addDateButton = view.findViewById(R.id.addDateButton)
        addButton = view.findViewById(R.id.addButton)
        recyclerView = view.findViewById(R.id.netWorthRecyclerView)

        adapter = NetWorthAdapter()
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        addDateButton.setOnClickListener { openDatePicker() }
        addButton.setOnClickListener { addNetWorthEntry() }

        loadNetWorthEntries()
    }

    private fun openDatePicker() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(requireContext(), { _, selectedYear, selectedMonth, selectedDay ->
            calendar.set(Calendar.YEAR, selectedYear)
            calendar.set(Calendar.MONTH, selectedMonth)
            calendar.set(Calendar.DAY_OF_MONTH, selectedDay)
            selectedDate = calendar.time
            val formatted = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(selectedDate!!)
            addDateButton.text = formatted
        }, year, month, day)

        datePickerDialog.show()
    }




    private fun addNetWorthEntry() {
        val amount = addAmountEditText.text.toString().toDoubleOrNull()
        if (amount == null || selectedDate == null) {
            Toast.makeText(context, "Enter valid amount and date", Toast.LENGTH_SHORT).show()
            return
        }

        val userId = auth.currentUser?.uid ?: return
        val entry = NetWorthEntry(amount, selectedDate!!)

        firestore.collection("users")
            .document(userId)
            .collection("net_worth")
            .add(entry)
            .addOnSuccessListener {
                Toast.makeText(context, "Net worth entry added", Toast.LENGTH_SHORT).show()
                addAmountEditText.text.clear()
                addDateButton.text = "Select Date"
                selectedDate = null
                loadNetWorthEntries()
            }
            .addOnFailureListener {
                Toast.makeText(context, "Failed to add entry", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadNetWorthEntries() {
        val userId = auth.currentUser?.uid ?: return
        firestore.collection("users")
            .document(userId)
            .collection("net_worth")
            .get()
            .addOnSuccessListener { snapshot ->
                val entries = snapshot.mapNotNull { it.toObject(NetWorthEntry::class.java) }
                adapter.setItems(entries)
                val total = entries.sumOf { it.amount }
                totalNetWorthText.text = "Total Net Worth: R%.2f".format(total)
            }
    }

    companion object {
        fun newInstance() = NetWorthFragment()
    }
}
