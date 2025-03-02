package com.example.logic

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.Calendar

data class Expense(val id: Int, val description: String, val amount: Double, val category: String, val date: LocalDate = LocalDate.now())

class BudgetTracker {
    private val expenses = mutableListOf<Expense>()
    private var totalBudget: Double = 0.0
    private var startDate: LocalDate = LocalDate.now()
    private var endDate: LocalDate = LocalDate.now()
    private var allocationPerDay: Double = 0.0
    private var totalRemainingBudget: Double = 0.0

    fun setBudget(amount: Double, endDate: LocalDate) {
        totalBudget = amount
        this.endDate = endDate
        startDate = LocalDate.now()
        val totalDays = ChronoUnit.DAYS.between(startDate, endDate).toInt() + 1
        allocationPerDay = if (totalDays > 0) amount / totalDays else 0.0
        totalRemainingBudget = amount
    }

    fun addExpense(description: String, amount: Double, category: String) {
        val id = if (expenses.isEmpty()) 1 else expenses.maxOf { it.id } + 1
        val expense = Expense(id, description, amount, category)
        expenses.add(expense)
        totalRemainingBudget -= amount

        recalculateDailyAllocationIfNeeded() // Call this function after adding expense
    }

    private fun recalculateDailyAllocationIfNeeded() {
        val today = LocalDate.now()
        val dailySpent = expenses.filter { it.date == today }.sumOf { it.amount }

        if (dailySpent > allocationPerDay) {
            val overSpentAmount = dailySpent - allocationPerDay
            if (totalRemainingBudget < 0) { // To prevent negative remaining budget, consider overspent amount only within remaining budget
                return // Or handle it as per your requirement, e.g., set allocation to 0
            }

            val remainingDays = ChronoUnit.DAYS.between(today.plusDays(1), endDate).toInt() + 1 // Days from tomorrow onwards
            if (remainingDays > 0) {
                allocationPerDay = if (totalRemainingBudget > 0) totalRemainingBudget / remainingDays else 0.0
            } else {
                allocationPerDay = 0.0 // No more days to allocate to
            }
        }
    }

    fun getRemainingDailyAllocation(date: LocalDate = LocalDate.now()): Double {
        val dailySpent = expenses.filter { it.date == date }.sumOf { it.amount }
        return allocationPerDay - dailySpent
    }

    fun getExpensesList(): String {
        return expenses.joinToString("\n") { "${it.description} - $${it.amount} (${it.category}) on ${it.date}" }
    }

    fun getBudgetSummary(): String {
        return "Total Budget: $$totalBudget\n" +
                "Daily Allocation: $$allocationPerDay\n" +
                "Remaining for today: $${getRemainingDailyAllocation()}\n" +
                "Remaining Amount: $${getTotalRemainingBudget()}" // Call the getter here as well!
    }

    // Public getter function to access totalRemainingBudget
    fun getTotalRemainingBudget(): Double {
        return totalRemainingBudget
    }
}

class MainActivity : AppCompatActivity() {

    private val tracker = BudgetTracker()
    private var selectedEndDate: LocalDate? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val budgetInput: EditText = findViewById(R.id.budgetInput)
        val endDateButton: Button = findViewById(R.id.endDateButton)
        val descriptionInput: EditText = findViewById(R.id.descriptionInput)
        val amountInput: EditText = findViewById(R.id.amountInput)
        val categoryInput: EditText = findViewById(R.id.categoryInput)
        val budgetTextView: TextView = findViewById(R.id.budgetTextView)
        val expensesTextView: TextView = findViewById(R.id.expensesTextView)
        val addExpenseButton: Button = findViewById(R.id.addExpenseButton)
        val setBudgetButton: Button = findViewById(R.id.setBudgetButton)
        val endBudgetButton: Button = findViewById(R.id.endBudgetButton) // Find the new button

        endDateButton.setOnClickListener {
            val calendar = Calendar.getInstance()
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            val datePicker = DatePickerDialog(this, { _, selectedYear, selectedMonth, selectedDay ->
                selectedEndDate = LocalDate.of(selectedYear, selectedMonth + 1, selectedDay)
                endDateButton.text = "End Date: $selectedEndDate"
            }, year, month, day)

            datePicker.show()
        }

        setBudgetButton.setOnClickListener {
            val budget = budgetInput.text.toString().toDoubleOrNull()
            if (budget != null && selectedEndDate != null) {
                tracker.setBudget(budget, selectedEndDate!!)
                budgetTextView.text = tracker.getBudgetSummary()
            } else {
                Toast.makeText(this, "Please enter a valid budget and pick an end date", Toast.LENGTH_SHORT).show()
            }
        }

        addExpenseButton.setOnClickListener {
            val description = descriptionInput.text.toString()
            val amount = amountInput.text.toString().toDoubleOrNull()
            val category = categoryInput.text.toString()

            if (description.isNotEmpty() && amount != null && category.isNotEmpty()) {
                tracker.addExpense(description, amount, category)
                expensesTextView.text = tracker.getExpensesList()
                budgetTextView.text = tracker.getBudgetSummary()
                Toast.makeText(this, "Expense added!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Please fill out all fields", Toast.LENGTH_SHORT).show()
            }
        }

        endBudgetButton.setOnClickListener {  // ADD THIS setOnClickListener for endBudgetButton
            val remainingAmount = tracker.getTotalRemainingBudget() // Call the getter function!
            val message = "You saved: $$remainingAmount" // Create the message
            Toast.makeText(this, message, Toast.LENGTH_LONG).show() // Show the Toast
        }
    }
}