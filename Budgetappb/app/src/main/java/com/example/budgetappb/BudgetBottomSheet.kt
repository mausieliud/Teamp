import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import com.example.budgetappb.R
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class BudgetBottomSheet : BottomSheetDialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_budget_bottom_sheet, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val btnClose: Button = view.findViewById(R.id.btnClose)
        val btnSave: Button = view.findViewById(R.id.btnSave)
        val etBudget: EditText = view.findViewById(R.id.etBudget)

        btnClose.setOnClickListener {
            dismiss() // Closes the bottom sheet
        }

        btnSave.setOnClickListener {
            val budget = etBudget.text.toString()
            if (budget.isNotEmpty()) {
                saveBudget(requireContext(), budget)
                Toast.makeText(requireContext(), "Budget saved successfully", Toast.LENGTH_SHORT).show()
                dismiss()
            } else {
                Toast.makeText(requireContext(), "Please enter a budget", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveBudget(context: Context, budget: String) {
        val sharedPreferences = context.getSharedPreferences("BudgetPrefs", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString("budget", budget)
        editor.apply()
    }
}

