package com.example.budgetappb

import BudgetBottomSheet
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat


class MainActivity : AppCompatActivity() {
    private lateinit var tvDisplay: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        tvDisplay = findViewById(R.id.tvDisplay)

        val buttons = listOf(
            findViewById<Button>(R.id.btn0),
            findViewById<Button>(R.id.btn1),
            findViewById<Button>(R.id.btn2),
            findViewById<Button>(R.id.btn3),
            findViewById<Button>(R.id.btn4),
            findViewById<Button>(R.id.btn5),
            findViewById<Button>(R.id.btn6),
            findViewById<Button>(R.id.btn7),
            findViewById<Button>(R.id.btn8),
            findViewById<Button>(R.id.btn9)
        )

        val btnBackspace = findViewById<Button>(R.id.btnBackspace)
        val btnAccept = findViewById<Button>(R.id.btnAccept)

        // Handle number button clicks
        for (button in buttons) {
            button.setOnClickListener {
                val number = button.text.toString()
                if (tvDisplay.text.toString() == "0") {
                    tvDisplay.text = number
                } else {
                    tvDisplay.text = tvDisplay.text.toString() + number
                }
            }
        }

        // Handle Backspace
        btnBackspace.setOnClickListener {
            val text = tvDisplay.text.toString()
            if (text.length > 1) {
                tvDisplay.text = text.dropLast(1)
            } else {
                tvDisplay.text = "0"
            }
        }

        // Handle Accept (✔)
        btnAccept.setOnClickListener {
            val enteredNumber = tvDisplay.text.toString()
            Toast.makeText(this, "Entered: $enteredNumber", Toast.LENGTH_SHORT).show()
        }
        //button for open budget
        val btnOpenBottomSheet: Button = findViewById(R.id.btnOpenBottomSheet)
        btnOpenBottomSheet.setOnClickListener {
            val bottomSheet = BudgetBottomSheet()
            bottomSheet.show(supportFragmentManager, "BudgetBottomSheet")
        }

        //inside oncreate
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}
