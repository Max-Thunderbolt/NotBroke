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
import com.example.notbroke.adapters.NetWorthAdapter // Make sure this adapter is set up for item_net_worth.xml
import com.example.notbroke.models.NetWorthEntry
import com.example.notbroke.repositories.RepositoryFactory
import com.example.notbroke.services.AuthService
import com.google.android.material.card.MaterialCardView
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class NetWorthFragment : Fragment() {

    private lateinit var totalNetWorthText: TextView
    private lateinit var assetNameEditText: TextInputEditText // Changed to TextInputEditText
    private lateinit var addAmountEditText: TextInputEditText // Changed to TextInputEditText
    private lateinit var addDateButton: Button
    private lateinit var addButton: Button
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: NetWorthAdapter
    private lateinit var toggleFormButton: Button
    private lateinit var entryFormCardView: MaterialCardView // Changed from LinearLayout

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
        assetNameEditText = view.findViewById(R.id.assetNameEditText)
        addAmountEditText = view.findViewById(R.id.addAmountEditText)
        addDateButton = view.findViewById(R.id.addDateButton)
        addButton = view.findViewById(R.id.addButton)
        recyclerView = view.findViewById(R.id.netWorthRecyclerView)
        toggleFormButton = view.findViewById(R.id.toggleFormButton)
        entryFormCardView = view.findViewById(R.id.entryFormCardView) // Changed

        adapter = NetWorthAdapter(
            onClick = { entry -> startEditingEntry(entry) },
            onLongClick = { entry -> confirmDeleteEntry(entry) }
        )

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter
        recyclerView.isNestedScrollingEnabled = false // Good for ScrollView performance

        toggleFormButton.setOnClickListener {
            if (entryFormCardView.visibility == View.GONE) {
                entryFormCardView.visibility = View.VISIBLE
                toggleFormButton.text = "HIDE FORM"
            } else {
                entryFormCardView.visibility = View.GONE
                toggleFormButton.text = "ADD NET WORTH ENTRY"
                resetForm() // Optionally reset form when hiding
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
            // Adjust for timezone offset if MaterialDatePicker.todayInUtcMilliseconds() is used
            val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
            calendar.timeInMillis = selection
            selectedDate = calendar.time // Store as Date object
            val formatted = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(selectedDate!!)
            addDateButton.text = formatted
        }

        datePicker.show(parentFragmentManager, "DATE_PICKER")
    }

    private fun handleAddOrUpdate() {
        val nameText = assetNameEditText.text.toString().trim()
        val amountText = addAmountEditText.text.toString()
        // Allow negative numbers for liabilities
        val amount = amountText.toDoubleOrNull()
        val date = selectedDate
        val userId = authService.getCurrentUserId() ?: return

        if (nameText.isEmpty()) {
            assetNameEditText.error = "Asset name cannot be empty"
            Toast.makeText(context, "Please enter an asset/liability name.", Toast.LENGTH_SHORT).show()
            return
        }
        if (amount == null) {
            addAmountEditText.error = "Amount cannot be empty"
            Toast.makeText(context, "Please enter a valid amount.", Toast.LENGTH_SHORT).show()
            return
        }
        if (date == null) {
            Toast.makeText(context, "Please select a date.", Toast.LENGTH_SHORT).show()
            return
        }
        assetNameEditText.error = null // Clear error
        addAmountEditText.error = null // Clear error


        val entry = editingEntry?.copy(name = nameText, amount = amount, date = date)
            ?: NetWorthEntry(id = UUID.randomUUID().toString(), userId = userId, name = nameText, amount = amount, date = date)


        lifecycleScope.launch {
            val result = if (editingEntry != null) {
                netWorthRepository.updateEntry(entry)
            } else {
                netWorthRepository.addEntry(entry)
            }

            result.onSuccess {
                Toast.makeText(context, if (editingEntry != null) "Entry updated" else "Entry added", Toast.LENGTH_SHORT).show()
                resetForm()
                if (entryFormCardView.visibility == View.VISIBLE && editingEntry == null) {
                    // If adding a new entry and form is visible, you might want to keep it open or hide it
                    // entryFormCardView.visibility = View.GONE
                    // toggleFormButton.text = "ADD NET WORTH ENTRY"
                }
            }.onFailure {
                Toast.makeText(context, "Failed: ${it.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun observeNetWorthEntries() {
        val userId = authService.getCurrentUserId() ?: return

        lifecycleScope.launch {
            netWorthRepository.getAllEntries(userId).collectLatest { entries ->
                adapter.submitList(entries.sortedByDescending { it.date }) // Show newest first
                val total = entries.sumOf { it.amount }
                totalNetWorthText.text = "R%.2f".format(total) // Displaying only the value as per new XML
            }
        }
    }

    private fun startEditingEntry(entry: NetWorthEntry) {
        editingEntry = entry
        assetNameEditText.setText(entry.name)
        addAmountEditText.setText(entry.amount.toString())
        selectedDate = entry.date
        addDateButton.text = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(entry.date)
        addButton.text = "Update Entry"

        // Show the form if it's hidden
        if (entryFormCardView.visibility == View.GONE) {
            entryFormCardView.visibility = View.VISIBLE
            toggleFormButton.text = "HIDE FORM"
        }
        assetNameEditText.requestFocus() // Focus on the first field
    }

    private fun confirmDeleteEntry(entry: NetWorthEntry) {
        AlertDialog.Builder(requireContext(), R.style.AlertDialogTheme) // Apply a theme if you have one
            .setTitle("Delete Entry")
            .setMessage("Are you sure you want to delete '${entry.name}'?")
            .setPositiveButton("Delete") { _, _ -> deleteEntry(entry) }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteEntry(entry: NetWorthEntry) {
        lifecycleScope.launch {
            netWorthRepository.deleteEntry(entry.id).onSuccess {
                Toast.makeText(context, "Entry deleted", Toast.LENGTH_SHORT).show()
                resetForm() // Reset form in case it was populated with this entry's data
            }.onFailure {
                Toast.makeText(context, "Failed to delete: ${it.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun resetForm() {
        editingEntry = null
        selectedDate = null
        assetNameEditText.text?.clear()
        addAmountEditText.text?.clear()
        assetNameEditText.error = null
        addAmountEditText.error = null
        addDateButton.text = "SELECT DATE"
        addButton.text = "SAVE ENTRY"
        // Consider if you want to hide the form on reset:
        // entryFormCardView.visibility = View.GONE
        // toggleFormButton.text = "ADD NET WORTH ENTRY"
    }

    companion object {
        fun newInstance() = NetWorthFragment()
    }
}