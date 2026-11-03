package com.example.lernpilot

import android.content.Intent
import android.os.Bundle

class MainActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setup(R.layout.activity_main)
    }

    override fun onResume() {
        super.onResume()

        content.removeAllViews()
        label("LernPilot", 28f)
        label("Loading progress…")

        load(
            work = { db.getDashboardStats() },
            done = { stats ->
                content.removeAllViews()

                label("LernPilot", 28f)
                label("Completed quizzes: ${stats.completed}")

                if (stats.answered == 0) {
                    label("Complete your first quiz to see your accuracy.")
                } else {
                    val accuracy = 100 * stats.correct / stats.answered

                    label(
                        "Accuracy: $accuracy% " +
                                "(${stats.correct}/${stats.answered})"
                    )
                }

                label(
                    "Choose a topic to start a new quiz " +
                            "or resume an unfinished one."
                )

                button("Start learning") {
                    startActivity(
                        Intent(this, TopicsActivity::class.java)
                    )
                }
            }
        )
    }
}