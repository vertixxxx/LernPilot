package com.example.lernpilot

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.RadioButton
import android.widget.RadioGroup
import android.graphics.drawable.GradientDrawable
import android.widget.LinearLayout

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
        val progressRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL

            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(12)
            ).apply {
                topMargin = dp(4)
                bottomMargin = dp(20)
            }
        }

        repeat(state.total) { index ->
            val segmentColor = when {
                index < state.position -> cyan
                index == state.position -> pink
                else -> android.graphics.Color.rgb(37, 58, 75)
            }

            val segment = View(this).apply {
                background = GradientDrawable().apply {
                    setColor(segmentColor)
                    cornerRadius = dp(2).toFloat()
                }

                layoutParams = LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    1f
                ).apply {
                    if (index < state.total - 1) {
                        rightMargin = dp(6)
                    }
                }
            }

            progressRow.addView(segment)
        }

        content.addView(progressRow)

        label(question.text, 22f)

        val group = RadioGroup(this)
        content.addView(group)

        question.options.forEachIndexed { index, option ->
            val radio = RadioButton(this)

            radio.id = View.generateViewId()
            radio.text = "${'A' + index}  $option"
            radio.isEnabled = state.selectedIndex == null

            group.addView(radio)

            radio.isChecked =
                index == (state.selectedIndex ?: pendingChoice)
        }

        fun refreshAnswerStyles() {
            for (index in 0 until group.childCount) {
                val radio = group.getChildAt(index) as RadioButton

                styleAnswer(
                    radio = radio,
                    selected = radio.isChecked,
                    submitted = state.selectedIndex != null,
                    correct = index == question.correctIndex
                )
            }
        }

        refreshAnswerStyles()

        group.setOnCheckedChangeListener { _, checkedId ->
            pendingChoice = (0 until group.childCount)
                .firstOrNull {
                    group.getChildAt(it).id == checkedId
                } ?: -1

            refreshAnswerStyles()
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