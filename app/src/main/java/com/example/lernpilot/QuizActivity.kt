package com.example.lernpilot

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.RadioButton
import android.widget.RadioGroup

class QuizActivity : BaseActivity() {

    private var sessionId = -1L
    private var pendingChoice = -1
    private var pendingQuestionId = -1L
    private var busy = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setup(R.layout.activity_quiz)

        sessionId = savedInstanceState
            ?.getLong("session_id", -1L) ?: -1L

        pendingChoice = savedInstanceState
            ?.getInt("pending_choice", -1) ?: -1

        pendingQuestionId = savedInstanceState
            ?.getLong("pending_question_id", -1L) ?: -1L

        label("Loading quiz…")

        load(
            work = {
                val id = if (sessionId > 0) {
                    sessionId
                } else {
                    val topicId = intent.getLongExtra("topic_id", -1L)
                    db.startOrResumeSession(topicId)
                }

                db.getQuizState(id)
            },
            done = { state ->
                sessionId = state.sessionId
                render(state)
            }
        )
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putLong("session_id", sessionId)
        outState.putInt("pending_choice", pendingChoice)
        outState.putLong("pending_question_id", pendingQuestionId)

        super.onSaveInstanceState(outState)
    }

    private fun render(state: QuizState) {
        busy = false

        if (state.finished) {
            val intent = Intent(this, ResultsActivity::class.java)
            intent.putExtra("session_id", state.sessionId)

            startActivity(intent)
            finish()
            return
        }

        val question = requireNotNull(state.question)

        // Never apply a previous question's unsaved selection to a new one.
        if (pendingQuestionId != question.id) {
            pendingChoice = -1
            pendingQuestionId = question.id
        }

        content.removeAllViews()

        label(
            "Question ${state.position + 1} of ${state.total}",
            26f
        )

        label(question.text, 22f)

        val group = RadioGroup(this)
        content.addView(group)

        question.options.forEachIndexed { index, option ->
            val radio = RadioButton(this)

            radio.id = View.generateViewId()
            radio.text = option
            radio.isEnabled = state.selectedIndex == null

            group.addView(radio)

            radio.isChecked =
                index == (state.selectedIndex ?: pendingChoice)
        }

        group.setOnCheckedChangeListener { _, checkedId ->
            pendingChoice = (0 until group.childCount)
                .firstOrNull {
                    group.getChildAt(it).id == checkedId
                } ?: -1
        }

        val feedback = label("")

        if (state.selectedIndex == null) {
            button("Check answer") {
                if (pendingChoice == -1) {
                    feedback.text = "Select an answer first."
                } else if (!busy) {
                    busy = true

                    val selected = pendingChoice

                    load(
                        work = {
                            db.submitAnswer(
                                state.sessionId,
                                question.id,
                                selected
                            )

                            db.getQuizState(state.sessionId)
                        },
                        done = { updatedState ->
                            render(updatedState)
                        }
                    )
                }
            }
        } else {
            val correct =
                state.selectedIndex == question.correctIndex

            feedback.text = if (correct) {
                "Correct! ${question.explanation}"
            } else {
                "Incorrect. Correct answer: " +
                        question.options[question.correctIndex] +
                        ". ${question.explanation}"
            }

            val buttonText =
                if (state.position + 1 == state.total) {
                    "Show results"
                } else {
                    "Next question"
                }

            button(buttonText) {
                if (!busy) {
                    busy = true
                    pendingChoice = -1
                    pendingQuestionId = -1L

                    load(
                        work = {
                            db.advance(
                                state.sessionId,
                                state.position
                            )

                            db.getQuizState(state.sessionId)
                        },
                        done = { updatedState ->
                            render(updatedState)
                        }
                    )
                }
            }
        }
    }
}