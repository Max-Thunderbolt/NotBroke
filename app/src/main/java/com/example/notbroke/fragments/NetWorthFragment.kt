package com.example.notbroke.fragments

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.notbroke.R
import com.example.notbroke.adapters.NetWorthAdapter
import com.example.notbroke.models.NetWorthEntry
import com.example.notbroke.repositories.RepositoryFactory
import com.example.notbroke.services.AuthService
import com.google.android.material.datepicker.MaterialDatePicker
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class NetWorthFragment : Fragment() {

    private lateinit var totalNetWorthText: TextView
    private lateinit var addAmountEditText: EditText
    private lateinit var addDateButton: Button
    private lateinit var addButton: Button
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: NetWorthAdapter
    private var selectedDate: Date? = null
    
    private val authService = AuthService.getInstance()
    private lateinit var repositoryFactory: RepositoryFactory
    private val netWorthRepository by lazy { repositoryFactory.netWorthRepository }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_net_worth, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize repository factory
        repositoryFactory = RepositoryFactory.getInstance(requireContext())

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

        observeNetWorthEntries()
    }

    private fun openDatePicker() {
        val datePicker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Select Date")
            .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
            .setTheme(com.google.android.material.R.style.ThemeOverlay_MaterialComponents_MaterialCalendar)
            .build()

        datePicker.addOnPositiveButtonClickListener { selection ->
            selectedDate = Date(selection)
            val formatted = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(selectedDate!!)
            addDateButton.text = formatted
        }

        datePicker.show(parentFragmentManager, "DATE_PICKER")
    }

    private fun addNetWorthEntry() {
        val amount = addAmountEditText.text.toString().toDoubleOrNull()
        if (amount == null || selectedDate == null) {
            Toast.makeText(context, "Enter valid amount and date", Toast.LENGTH_SHORT).show()
            return
        }

        val userId = authService.getCurrentUserId() ?: return
        val entry = NetWorthEntry(
            amount = amount,
            date = selectedDate!!
        )

        lifecycleScope.launch {
            try {
                val result = netWorthRepository.addEntry(entry)
                result.onSuccess {
                    Toast.makeText(context, "Net worth entry added", Toast.LENGTH_SHORT).show()
                    addAmountEditText.text.clear()
                    addDateButton.text = "Select Date"
                    selectedDate = null
                }.onFailure {
                    Toast.makeText(context, "Failed to add entry: ${it.message}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun observeNetWorthEntries() {
        val userId = authService.getCurrentUserId() ?: return
        
        lifecycleScope.launch {
            netWorthRepository.getAllEntries(userId).collectLatest { entries ->
                adapter.submitList(entries)
                val total = entries.sumOf { it.amount }
                totalNetWorthText.text = "Total Net Worth: R%.2f".format(total)
            }
        }
    }

    companion object {
        fun newInstance() = NetWorthFragment()
    }
}
