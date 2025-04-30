package com.example.notbroke.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
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
    private var editingEntry: NetWorthEntry? = null

    private val authService = AuthService.getInstance()
    private lateinit var repositoryFactory: RepositoryFactory
    private val netWorthRepository by lazy { repositoryFactory.netWorthRepository }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_net_worth, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        repositoryFactory = RepositoryFactory.getInstance(requireContext())

        totalNetWorthText = view.findViewById(R.id.totalNetWorthText)
        addAmountEditText = view.findViewById(R.id.addAmountEditText)
        addDateButton = view.findViewById(R.id.addDateButton)
        addButton = view.findViewById(R.id.addButton)
        recyclerView = view.findViewById(R.id.netWorthRecyclerView)

        adapter = NetWorthAdapter(
            onClick = { entry -> startEditingEntry(entry) },
            onLongClick = { entry -> confirmDeleteEntry(entry) }
        )

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        val toggleFormButton = view.findViewById<Button>(R.id.toggleFormButton)
        val entryFormLayout = view.findViewById<LinearLayout>(R.id.entryFormLayout)

        toggleFormButton.setOnClickListener {
            if (entryFormLayout.visibility == View.GONE) {
                entryFormLayout.visibility = View.VISIBLE
                toggleFormButton.text = "HIDE FORM"
            } else {
                entryFormLayout.visibility = View.GONE
                toggleFormButton.text = "ADD ENTRY"
            }
        }


        addDateButton.setOnClickListener { openDatePicker() }
        addButton.setOnClickListener { handleAddOrUpdate() }

        observeNetWorthEntries()
    }

    private fun openDatePicker() {
        val datePicker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Select Date")
            .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
            .build()

        datePicker.addOnPositiveButtonClickListener { selection ->
            selectedDate = Date(selection)
            val formatted = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(selectedDate!!)
            addDateButton.text = formatted
        }

        datePicker.show(parentFragmentManager, "DATE_PICKER")
    }

    private fun handleAddOrUpdate() {
        val nameText = view?.findViewById<EditText>(R.id.assetNameEditText)?.text.toString().trim()
        val amountText = addAmountEditText.text.toString()
        val amount = amountText.toDoubleOrNull()
        val date = selectedDate
        val userId = authService.getCurrentUserId() ?: return

        if (nameText.isEmpty() || amount == null || date == null) {
            Toast.makeText(context, "Enter all fields", Toast.LENGTH_SHORT).show()
            return
        }

        val entry = editingEntry?.copy(name = nameText, amount = amount, date = date)
            ?: NetWorthEntry(id = "", userId = userId, name = nameText, amount = amount, date = date)


        lifecycleScope.launch {
            val result = if (editingEntry != null) {
                netWorthRepository.updateEntry(entry)
            } else {
                netWorthRepository.addEntry(entry)
            }

            result.onSuccess {
                Toast.makeText(context, if (editingEntry != null) "Entry updated" else "Entry added", Toast.LENGTH_SHORT).show()
                resetForm()
            }.onFailure {
                Toast.makeText(context, "Failed: ${it.message}", Toast.LENGTH_SHORT).show()
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

    private fun startEditingEntry(entry: NetWorthEntry) {
        val nameEditText = view?.findViewById<EditText>(R.id.assetNameEditText)
        nameEditText?.setText(entry.name)

        editingEntry = entry
        addAmountEditText.setText(entry.amount.toString())
        selectedDate = entry.date
        addDateButton.text = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(entry.date)
        addButton.text = "Update Entry"
    }

    private fun confirmDeleteEntry(entry: NetWorthEntry) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Entry")
            .setMessage("Are you sure you want to delete this entry?")
            .setPositiveButton("Delete") { _, _ -> deleteEntry(entry) }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteEntry(entry: NetWorthEntry) {
        lifecycleScope.launch {
            netWorthRepository.deleteEntry(entry.id).onSuccess {
                Toast.makeText(context, "Entry deleted", Toast.LENGTH_SHORT).show()
                resetForm()
            }.onFailure {
                Toast.makeText(context, "Failed to delete: ${it.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun resetForm() {
        editingEntry = null
        selectedDate = null
        view?.findViewById<EditText>(R.id.assetNameEditText)?.text?.clear() // 👈 CLEAR NAME FIELD
        addAmountEditText.text.clear()
        addDateButton.text = "Select Date"
        addButton.text = "Add Entry"
    }


    companion object {
        fun newInstance() = NetWorthFragment()
    }
}
