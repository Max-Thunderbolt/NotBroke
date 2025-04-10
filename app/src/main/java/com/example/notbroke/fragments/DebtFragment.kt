package com.example.notbroke.fragments

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.example.notbroke.R
import com.example.notbroke.models.Debt
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import android.widget.Toast
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.button.MaterialButton

class DebtFragment : Fragment() {
    
    // Views
    private lateinit var progressBar: ProgressBar
    private lateinit var progressPercentageTextView: TextView
    private lateinit var dateEstimateTextView: TextView
    private lateinit var totalDebtTextView: TextView
    private lateinit var paidOffTextView: TextView
    private lateinit var monthlyPaymentTextView: TextView
    private lateinit var debtsRecyclerView: RecyclerView
    private lateinit var noDebtsTextView: TextView
    private lateinit var addDebtButton: Button
    private lateinit var addDebtFab: FloatingActionButton
    private lateinit var strategySpinner: Spinner
    private lateinit var strategyDescriptionTextView: TextView
    
    // Data
    private var debts: MutableList<Debt> = mutableListOf()
    private lateinit var debtAdapter: DebtAdapter
    
    // Formatting
    private val currencyFormatter = NumberFormat.getCurrencyInstance(Locale("en", "ZA"))
    private val dateFormatter = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_debt, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Initialize views
        initializeViews(view)
        
        // Load sample debts
        loadDebts()
        
        // Setup UI components
        setupRecyclerView()
        setupStrategySpinner()
        setupButtons()
        
        // Update UI with data
        updateUI()
    }
    
    private fun initializeViews(view: View) {
        progressBar = view.findViewById(R.id.debtProgressBar)
        progressPercentageTextView = view.findViewById(R.id.progressPercentageTextView)
        dateEstimateTextView = view.findViewById(R.id.dateEstimateTextView)
        totalDebtTextView = view.findViewById(R.id.totalDebtTextView)
        paidOffTextView = view.findViewById(R.id.paidOffTextView)
        monthlyPaymentTextView = view.findViewById(R.id.monthlyPaymentTextView)
        debtsRecyclerView = view.findViewById(R.id.debtsRecyclerView)
        noDebtsTextView = view.findViewById(R.id.noDebtsTextView)
        addDebtButton = view.findViewById(R.id.addDebtButton)
        addDebtFab = view.findViewById(R.id.addDebtFab)
        strategySpinner = view.findViewById(R.id.strategySpinner)
        strategyDescriptionTextView = view.findViewById(R.id.strategyDescriptionTextView)
    }
    
    private fun loadDebts() {
        // For now, load sample debts
        debts = Debt.createSampleDebts().toMutableList()
    }
    
    private fun setupRecyclerView() {
        debtAdapter = DebtAdapter(debts)
        debtsRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = debtAdapter
        }
    }
    
    private fun setupStrategySpinner() {
        val strategies = arrayOf(
            "Avalanche Method",
            "Snowball Method",
            "Debt Consolidation",
            "Highest Interest First",
            "Balance Proportion",
            "Debt Stacking"
        )
        
        // Create custom adapter with application theme colors
        val adapter = ArrayAdapter(
            requireContext(),
            R.layout.item_spinner_strategy,
            strategies
        )
        adapter.setDropDownViewResource(R.layout.item_spinner_dropdown)
        strategySpinner.adapter = adapter
        
        // Set default description
        updateStrategyDescription(0)
        
        strategySpinner.setOnItemSelectedListener(object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                updateStrategyDescription(position)
                
                // Apply the selected strategy to the debts (simulation)
                applyPayoffStrategy(position)
            }
            
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {
                // Do nothing
            }
        })
    }
    
    private fun updateStrategyDescription(position: Int) {
        val description = when (position) {
            0 -> "Avalanche Method: Pay minimum on all debts, then put extra money towards highest interest rate debt first. This minimizes total interest paid over time."
            1 -> "Snowball Method: Pay minimum on all debts, then put extra money towards smallest balance debt first. This creates psychological wins and momentum."
            2 -> "Debt Consolidation: Combine multiple debts into a single loan with lower interest rate. This simplifies payments and can reduce overall interest."
            3 -> "Highest Interest First: Focus exclusively on paying off debts with the highest interest rates first to minimize total interest paid."
            4 -> "Balance Proportion: Distribute extra payments proportionally based on debt balances. Balanced approach that tackles all debts simultaneously."
            5 -> "Debt Stacking: After paying off one debt, add that payment amount to the next debt payment. Creates an accelerating payoff schedule."
            else -> ""
        }
        strategyDescriptionTextView.text = description
    }
    
    private fun applyPayoffStrategy(strategyIndex: Int) {
        // This is a simulation of applying different strategies
        // In a real implementation, this would reorder debts or calculate optimal payments
        
        when (strategyIndex) {
            0 -> {
                // Avalanche Method - Sort by highest interest rate
                debts.sortByDescending { it.interestRate }
            }
            1 -> {
                // Snowball Method - Sort by lowest remaining balance
                debts.sortBy { it.getRemainingBalance() }
            }
            2 -> {
                // Debt Consolidation - Simulate consolidation by showing effect
                // (Just a visual simulation - no actual consolidation)
                Toast.makeText(
                    context, 
                    "Consolidation would save approximately ${formatCurrency(calculateConsolidationSavings())} in interest", 
                    Toast.LENGTH_LONG
                ).show()
            }
            3 -> {
                // Highest Interest First (pure focus)
                debts.sortByDescending { it.interestRate }
            }
            4 -> {
                // Balance Proportion
                // In a real implementation, this would calculate proportional payments
                Toast.makeText(
                    context,
                    "Payments distributed proportionally across all debts",
                    Toast.LENGTH_SHORT
                ).show()
            }
            5 -> {
                // Debt Stacking
                debts.sortBy { it.getMonthsRemaining() }
            }
        }
        
        // Update the adapter to reflect any changes
        debtAdapter.notifyDataSetChanged()
    }
    
    private fun calculateConsolidationSavings(): Double {
        // Simple simulation of potential savings from consolidation
        // Assumes consolidation at 2% less than weighted average interest rate
        
        if (debts.isEmpty()) return 0.0
        
        val totalDebt = debts.sumOf { it.getRemainingBalance() }
        val weightedInterestRate = debts.sumOf { it.getRemainingBalance() * it.interestRate } / totalDebt
        val consolidatedRate = (weightedInterestRate - 2.0).coerceAtLeast(4.0) // Minimum 4%
        
        // Estimate savings (very simplified)
        val originalInterest = debts.sumOf { it.getRemainingBalance() * it.interestRate / 100.0 * (it.getMonthsRemaining() / 12.0) }
        val consolidatedInterest = totalDebt * consolidatedRate / 100.0 * (debts.maxOf { it.getMonthsRemaining() } / 12.0)
        
        return (originalInterest - consolidatedInterest).coerceAtLeast(0.0)
    }
    
    private fun setupButtons() {
        addDebtButton.setOnClickListener {
            showAddDebtDialog()
        }
        
        addDebtFab.setOnClickListener {
            showAddDebtDialog()
        }
    }
    
    private fun showAddDebtDialog() {
        // Create and configure the dialog
        val dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.dialog_add_debt)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        
        // Initialize dialog views
        val debtNameEditText: TextInputEditText = dialog.findViewById(R.id.debtNameEditText)
        val debtAmountEditText: TextInputEditText = dialog.findViewById(R.id.debtAmountEditText)
        val interestRateEditText: TextInputEditText = dialog.findViewById(R.id.interestRateEditText)
        val monthlyPaymentEditText: TextInputEditText = dialog.findViewById(R.id.monthlyPaymentEditText)
        val amountPaidEditText: TextInputEditText = dialog.findViewById(R.id.amountPaidEditText)
        val cancelButton: MaterialButton = dialog.findViewById(R.id.cancelButton)
        val saveButton: MaterialButton = dialog.findViewById(R.id.saveButton)
        
        // Set up default values
        amountPaidEditText.setText("0")
        
        // Set up button click listeners
        cancelButton.setOnClickListener {
            dialog.dismiss()
        }
        
        saveButton.setOnClickListener {
            // Validate inputs
            val debtName = debtNameEditText.text.toString().trim()
            val debtAmountStr = debtAmountEditText.text.toString().trim()
            val interestRateStr = interestRateEditText.text.toString().trim()
            val monthlyPaymentStr = monthlyPaymentEditText.text.toString().trim()
            val amountPaidStr = amountPaidEditText.text.toString().trim()
            
            if (validateInputs(
                    debtName, 
                    debtAmountStr, 
                    interestRateStr, 
                    monthlyPaymentStr, 
                    amountPaidStr
                )
            ) {
                // Create new debt
                val debtAmount = debtAmountStr.toDouble()
                val interestRate = interestRateStr.toDouble()
                val monthlyPayment = monthlyPaymentStr.toDouble()
                val amountPaid = amountPaidStr.toDouble()
                
                // Ensure amount paid isn't greater than total
                val validAmountPaid = amountPaid.coerceAtMost(debtAmount)
                
                val newDebt = Debt(
                    name = debtName,
                    totalAmount = debtAmount,
                    amountPaid = validAmountPaid,
                    interestRate = interestRate,
                    monthlyPayment = monthlyPayment
                )
                
                // Add to list and update UI
                debts.add(newDebt)
                debtAdapter.notifyItemInserted(debts.size - 1)
                updateUI()
                
                // Show success message and dismiss dialog
                Toast.makeText(
                    context,
                    "Added debt: ${newDebt.name}",
                    Toast.LENGTH_SHORT
                ).show()
                
                dialog.dismiss()
            }
        }
        
        dialog.show()
    }
    
    private fun validateInputs(
        name: String,
        amount: String,
        interestRate: String,
        monthlyPayment: String,
        amountPaid: String
    ): Boolean {
        // Check for empty fields
        if (name.isEmpty()) {
            showToast("Please enter a debt name")
            return false
        }
        
        if (amount.isEmpty()) {
            showToast("Please enter the total debt amount")
            return false
        }
        
        if (interestRate.isEmpty()) {
            showToast("Please enter the interest rate")
            return false
        }
        
        if (monthlyPayment.isEmpty()) {
            showToast("Please enter the monthly payment")
            return false
        }
        
        // Validate numeric inputs
        try {
            val amountValue = amount.toDouble()
            val interestRateValue = interestRate.toDouble()
            val monthlyPaymentValue = monthlyPayment.toDouble()
            val amountPaidValue = if (amountPaid.isEmpty()) 0.0 else amountPaid.toDouble()
            
            // Additional validation
            if (amountValue <= 0) {
                showToast("Debt amount must be greater than zero")
                return false
            }
            
            if (interestRateValue < 0) {
                showToast("Interest rate cannot be negative")
                return false
            }
            
            if (monthlyPaymentValue <= 0) {
                showToast("Monthly payment must be greater than zero")
                return false
            }
            
            if (amountPaidValue < 0) {
                showToast("Amount paid cannot be negative")
                return false
            }
            
            if (amountPaidValue > amountValue) {
                showToast("Amount paid cannot exceed total debt amount")
                return false
            }
            
            // Monthly payment validation for reasonable debt repayment
            val monthlyInterest = interestRateValue / 100.0 / 12.0
            val remainingAmount = amountValue - amountPaidValue
            
            if (monthlyPaymentValue <= remainingAmount * monthlyInterest && interestRateValue > 0) {
                showToast("Monthly payment is too low to reduce the debt principal")
                return false
            }
            
        } catch (e: NumberFormatException) {
            showToast("Please enter valid numeric values")
            return false
        }
        
        return true
    }
    
    private fun updateUI() {
        if (debts.isEmpty()) {
            noDebtsTextView.visibility = View.VISIBLE
            debtsRecyclerView.visibility = View.GONE
        } else {
            noDebtsTextView.visibility = View.GONE
            debtsRecyclerView.visibility = View.VISIBLE
        }
        
        // Calculate summary stats
        val totalDebt = debts.sumOf { it.totalAmount }
        val totalPaid = debts.sumOf { it.amountPaid }
        val totalMonthlyPayment = debts.sumOf { it.monthlyPayment }
        
        // Calculate overall progress percentage
        val overallProgress = if (totalDebt > 0) {
            ((totalPaid / totalDebt) * 100).toInt().coerceIn(0, 100)
        } else {
            0
        }
        
        // Update progress bar and percentage
        progressBar.progress = overallProgress
        progressPercentageTextView.text = "$overallProgress%"
        
        // Update summary statistics
        totalDebtTextView.text = formatCurrency(totalDebt)
        paidOffTextView.text = formatCurrency(totalPaid)
        monthlyPaymentTextView.text = formatCurrency(totalMonthlyPayment)
        
        // Calculate estimated debt-free date
        calculateDebtFreeDate()
    }
    
    private fun calculateDebtFreeDate() {
        if (debts.isEmpty()) {
            dateEstimateTextView.text = "Add debts to see estimated completion date"
            return
        }
        
        // Find the debt that will take the longest to pay off
        val longestTimeDebt = debts.maxByOrNull { it.getMonthsRemaining() }
        
        if (longestTimeDebt != null) {
            val projectedDate = longestTimeDebt.getProjectedPayoffDate()
            dateEstimateTextView.text = "Estimated Debt-Free Date: ${formatDate(projectedDate)}"
        } else {
            dateEstimateTextView.text = "Estimated Debt-Free Date: N/A"
        }
    }
    
    private fun formatCurrency(amount: Double): String {
        return currencyFormatter.format(amount)
    }
    
    private fun formatDate(date: Date): String {
        return dateFormatter.format(date)
    }
    
    inner class DebtAdapter(private val debts: List<Debt>) :
        RecyclerView.Adapter<DebtAdapter.DebtViewHolder>() {
        
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DebtViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_debt, parent, false)
            return DebtViewHolder(view)
        }
        
        override fun onBindViewHolder(holder: DebtViewHolder, position: Int) {
            holder.bind(debts[position])
        }
        
        override fun getItemCount(): Int = debts.size
        
        inner class DebtViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val nameTextView: TextView = itemView.findViewById(R.id.debtNameTextView)
            private val amountTextView: TextView = itemView.findViewById(R.id.debtAmountTextView)
            private val progressTextView: TextView = itemView.findViewById(R.id.debtProgressTextView)
            private val progressBar: ProgressBar = itemView.findViewById(R.id.debtItemProgressBar)
            private val interestRateTextView: TextView = itemView.findViewById(R.id.interestRateTextView)
            private val monthlyPaymentTextView: TextView = itemView.findViewById(R.id.monthlyPaymentItemTextView)
            private val timeRemainingTextView: TextView = itemView.findViewById(R.id.timeRemainingTextView)
            
            fun bind(debt: Debt) {
                nameTextView.text = debt.name
                amountTextView.text = formatCurrency(debt.getRemainingBalance())
                
                val progressText = "${formatCurrency(debt.amountPaid)} / ${formatCurrency(debt.totalAmount)}"
                progressTextView.text = progressText
                
                progressBar.progress = debt.getProgressPercentage()
                
                interestRateTextView.text = "${debt.interestRate}%"
                monthlyPaymentTextView.text = formatCurrency(debt.monthlyPayment)
                
                val monthsRemaining = debt.getMonthsRemaining()
                timeRemainingTextView.text = when {
                    monthsRemaining == 0 -> "Paid off"
                    monthsRemaining == 1 -> "1 month"
                    monthsRemaining < 12 -> "$monthsRemaining months"
                    monthsRemaining % 12 == 0 -> "${monthsRemaining / 12} years"
                    else -> "${monthsRemaining / 12}y ${monthsRemaining % 12}m"
                }
                
                // Set click listener for the item
                itemView.setOnClickListener {
                    // In a real implementation, this would show debt details or edit options
                    makePaymentOnDebt(debt)
                }
            }
        }
    }
    
    private fun makePaymentOnDebt(debt: Debt) {
        // Create and configure the dialog
        val dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.dialog_debt_payment)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        
        // Initialize dialog views
        val debtNameTextView: TextView = dialog.findViewById(R.id.debtNameTextView)
        val remainingBalanceTextView: TextView = dialog.findViewById(R.id.remainingBalanceTextView)
        val paymentProgressBar: ProgressBar = dialog.findViewById(R.id.paymentProgressBar)
        val paymentProgressTextView: TextView = dialog.findViewById(R.id.paymentProgressTextView)
        val paymentAmountEditText: TextInputEditText = dialog.findViewById(R.id.paymentAmountEditText)
        val minimumPaymentButton: MaterialButton = dialog.findViewById(R.id.minimumPaymentButton)
        val regularPaymentButton: MaterialButton = dialog.findViewById(R.id.regularPaymentButton)
        val fullPaymentButton: MaterialButton = dialog.findViewById(R.id.fullPaymentButton)
        val cancelPaymentButton: MaterialButton = dialog.findViewById(R.id.cancelPaymentButton)
        val makePaymentButton: MaterialButton = dialog.findViewById(R.id.makePaymentButton)
        
        // Set debt information
        debtNameTextView.text = debt.name
        remainingBalanceTextView.text = formatCurrency(debt.getRemainingBalance())
        paymentProgressBar.progress = debt.getProgressPercentage()
        
        val progressText = "${debt.getProgressPercentage()}% paid (${formatCurrency(debt.amountPaid)} / ${formatCurrency(debt.totalAmount)})"
        paymentProgressTextView.text = progressText
        
        // Set default payment amount (regular monthly payment)
        paymentAmountEditText.setText(debt.monthlyPayment.toString())
        
        // Set up payment option buttons
        val minimumPayment = (debt.getRemainingBalance() * debt.interestRate / 100.0 / 12.0).coerceAtLeast(10.0)
        val regularPayment = debt.monthlyPayment
        val fullPayment = debt.getRemainingBalance()
        
        minimumPaymentButton.setOnClickListener {
            paymentAmountEditText.setText(String.format("%.2f", minimumPayment))
        }
        
        regularPaymentButton.setOnClickListener {
            paymentAmountEditText.setText(String.format("%.2f", regularPayment))
        }
        
        fullPaymentButton.setOnClickListener {
            paymentAmountEditText.setText(String.format("%.2f", fullPayment))
        }
        
        // Set up button click listeners
        cancelPaymentButton.setOnClickListener {
            dialog.dismiss()
        }
        
        makePaymentButton.setOnClickListener {
            // Validate payment amount
            val paymentAmountStr = paymentAmountEditText.text.toString().trim()
            
            if (paymentAmountStr.isEmpty()) {
                showToast("Please enter a payment amount")
                return@setOnClickListener
            }
            
            try {
                val paymentAmount = paymentAmountStr.toDouble()
                
                if (paymentAmount <= 0) {
                    showToast("Payment amount must be greater than zero")
                    return@setOnClickListener
                }
                
                if (paymentAmount > debt.getRemainingBalance()) {
                    showToast("Payment exceeds remaining balance")
                    return@setOnClickListener
                }
                
                // Apply the payment
                val amountApplied = debt.makePayment(paymentAmount)
                
                // Update UI
                debtAdapter.notifyDataSetChanged()
                updateUI()
                
                // Show success message and dismiss dialog
                Toast.makeText(
                    context,
                    "Payment of ${formatCurrency(amountApplied)} applied to ${debt.name}",
                    Toast.LENGTH_SHORT
                ).show()
                
                dialog.dismiss()
                
                // Check if debt is fully paid
                if (debt.getRemainingBalance() <= 0) {
                    showDebtPaidOffCelebration(debt)
                }
                
            } catch (e: NumberFormatException) {
                showToast("Please enter a valid payment amount")
            }
        }
        
        dialog.show()
    }
    
    private fun showDebtPaidOffCelebration(debt: Debt) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Congratulations!")
            .setMessage("You've completely paid off your ${debt.name}! Keep up the great work!")
            .setIcon(android.R.drawable.ic_dialog_info)
            .setPositiveButton("Awesome!") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }
    
    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }
    
    companion object {
        fun newInstance(): DebtFragment {
            return DebtFragment()
        }
    }
} 