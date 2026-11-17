package com.example.lernpilot

import android.content.Context

class QuizRepository private constructor(context: Context) {

    private val database = LernPilotDatabase.get(context)
    private val dao = database.quizDao()

    companion object {
        @Volatile
        private var instance: QuizRepository? = null

        fun get(context: Context): QuizRepository {
            return instance ?: synchronized(this) {
                instance ?: QuizRepository(context.applicationContext)
                    .also { instance = it }
            }
        }
    }

    private fun ensureSeeded() {
        database.runInTransaction {
            if (dao.topicCount() == 0) {
                dao.insertTopics(RoomSeed.topics)
                dao.insertQuestions(RoomSeed.questions)
            }
        }
    }

    fun getTopics(): List<Topic> {
        ensureSeeded()

        return dao.getTopics().map {
            Topic(it.id, it.name)
        }
    }

    fun startOrResumeSession(topicId: Long): Long {
        ensureSeeded()
        var sessionId = -1L

        database.runInTransaction {
            val existing = dao.findUnfinishedSession(topicId)

            if (existing != null) {
                sessionId = existing.id
            } else {
                val questions = dao.getQuestions(topicId)

                require(questions.isNotEmpty()) {
                    "No questions for this topic."
                }

                sessionId = dao.insertSession(
                    SessionEntity(topicId = topicId)
                )

                val sessionQuestions = questions.mapIndexed { index, q ->
                    SessionQuestionEntity(
                        sessionId = sessionId,
                        position = index,
                        questionId = q.id
                    )
                }

                dao.insertSessionQuestions(sessionQuestions)
            }
        }

        return sessionId
    }

    fun getQuizState(sessionId: Long): QuizState {
        var result: QuizState? = null

        database.runInTransaction {
            val session = requireNotNull(dao.getSession(sessionId)) {
                "Session not found."
            }

            val total = dao.questionCount(sessionId)

            result = if (session.finishedAt != null) {
                QuizState(
                    sessionId = sessionId,
                    position = session.position,
                    total = total,
                    finished = true,
                    question = null,
                    selectedIndex = null
                )
            } else {
                val entity = requireNotNull(
                    dao.getQuestionAt(sessionId, session.position)
                ) {
                    "Question not found."
                }

                val answer = dao.getAnswer(sessionId, entity.id)

                QuizState(
                    sessionId = sessionId,
                    position = session.position,
                    total = total,
                    finished = false,
                    question = entity.toQuestion(),
                    selectedIndex = answer?.selectedIndex
                )
            }
        }

        return requireNotNull(result)
    }

    fun submitAnswer(
        sessionId: Long,
        questionId: Long,
        selectedIndex: Int
    ) {
        require(selectedIndex in 0..3)

        database.runInTransaction {
            val session = requireNotNull(dao.getSession(sessionId))
            require(session.finishedAt == null)

            val question = requireNotNull(
                dao.getQuestionAt(sessionId, session.position)
            )

            require(question.id == questionId)

            if (dao.getAnswer(sessionId, questionId) == null) {
                dao.insertAnswer(
                    AnswerEntity(
                        sessionId = sessionId,
                        questionId = questionId,
                        selectedIndex = selectedIndex,
                        isCorrect = selectedIndex == question.correctIndex
                    )
                )
            }
        }
    }

    fun advance(sessionId: Long, expectedPosition: Int) {
        database.runInTransaction {
            val session = requireNotNull(dao.getSession(sessionId))

            if (
                session.finishedAt == null &&
                session.position == expectedPosition
            ) {
                val question = requireNotNull(
                    dao.getQuestionAt(sessionId, session.position)
                )

                require(dao.getAnswer(sessionId, question.id) != null) {
                    "Answer the question first."
                }

                val total = dao.questionCount(sessionId)

                if (session.position + 1 == total) {
                    dao.finishSession(
                        sessionId,
                        System.currentTimeMillis()
                    )
                } else {
                    dao.advancePosition(sessionId)
                }
            }
        }
    }

    fun getResult(sessionId: Long): Pair<Int, Int> {
        val score = dao.getResult(sessionId)
        return score.correct to score.answered
    }

    fun getDashboardStats(): DashboardStats {
        ensureSeeded()
        var result: DashboardStats? = null

        database.runInTransaction {
            val score = dao.completedScore()

            result = DashboardStats(
                completed = dao.completedCount(),
                correct = score.correct,
                answered = score.answered
            )
        }

        return requireNotNull(result)
    }
}