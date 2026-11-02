package com.example.lernpilot

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.content.Intent
import android.widget.Button
import android.widget.RadioGroup
import android.widget.TextView

class QuizActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_quiz)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        val answerGroup = findViewById<RadioGroup>(R.id.answerGroup)
        val checkButton = findViewById<Button>(R.id.checkAnswerButton)
        val feedbackText = findViewById<TextView>(R.id.feedbackText)
        val resultsButton = findViewById<Button>(R.id.showResultsButton)

        var score = 0

        checkButton.setOnClickListener {
            val selectedId = answerGroup.checkedRadioButtonId

            if (selectedId == -1) {
                feedbackText.text = "Please select an answer."
                return@setOnClickListener
            }

            val isCorrect = selectedId == R.id.answerB
            score = if (isCorrect) 1 else 0

            feedbackText.text = if (isCorrect) {
                "Correct! val declares a read-only variable."
            } else {
                "Incorrect. The correct answer is val."
            }

            // Prevent changing or submitting the answer again.
            for (index in 0 until answerGroup.childCount) {
                answerGroup.getChildAt(index).isEnabled = false
            }

            checkButton.isEnabled = false
            resultsButton.isEnabled = true
        }

        resultsButton.setOnClickListener {
            val intent = Intent(this, ResultsActivity::class.java)
            intent.putExtra("score", score)
            intent.putExtra("total", 1)
            startActivity(intent)
            finish()
        }
    }
}