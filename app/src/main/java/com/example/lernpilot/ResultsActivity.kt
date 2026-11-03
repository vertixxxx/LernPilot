package com.example.lernpilot

import android.content.Intent
import android.os.Bundle

class ResultsActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setup(R.layout.activity_results)

        label("Results", 28f)

        val sessionId = intent.getLongExtra("session_id", -1L)

        load(
            work = {
                require(db.getQuizState(sessionId).finished) {
                    "This quiz has not been completed."
                }

                db.getResult(sessionId)
            },
            done = { result ->
                val correct = result.first
                val total = result.second

                val accuracy =
                    if (total == 0) 0 else 100 * correct / total

                label("$correct of $total correct", 24f)
                label("Accuracy: $accuracy%")
                label("Your answers have been saved on this device.")

                button("Back to dashboard") {
                    val intent = Intent(
                        this,
                        MainActivity::class.java
                    )

                    intent.addFlags(
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                                Intent.FLAG_ACTIVITY_SINGLE_TOP
                    )

                    startActivity(intent)
                    finish()
                }
            }
        )
    }
}