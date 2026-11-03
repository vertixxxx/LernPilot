package com.example.lernpilot

import android.content.Intent
import android.os.Bundle

class TopicsActivity : BaseActivity() {

    private var openingQuiz = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setup(R.layout.activity_topics)

        label("Choose a topic", 28f)
        label("Five questions per quiz. Select a topic to start or resume.")

        load(
            work = { db.getTopics() },
            done = { topics ->
                topics.forEach { topic ->
                    button(topic.name) {
                        if (!openingQuiz) {
                            openingQuiz = true

                            val intent = Intent(
                                this,
                                QuizActivity::class.java
                            )

                            intent.putExtra("topic_id", topic.id)
                            startActivity(intent)
                        }
                    }
                }
            }
        )
    }

    override fun onResume() {
        super.onResume()
        openingQuiz = false
    }
}