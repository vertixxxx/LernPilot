package com.example.lernpilot

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface QuizDao {

    @Insert
    fun insertTopics(topics: List<TopicEntity>)

    @Insert
    fun insertQuestions(questions: List<QuestionEntity>)

    @Insert
    fun insertSession(session: SessionEntity): Long

    @Insert
    fun insertSessionQuestions(questions: List<SessionQuestionEntity>)

    @Insert
    fun insertAnswer(answer: AnswerEntity)

    @Query("SELECT COUNT(*) FROM topics")
    fun topicCount(): Int

    @Query("SELECT * FROM topics ORDER BY id")
    fun getTopics(): List<TopicEntity>

    @Query(
        """
        SELECT * FROM questions
        WHERE topicId = :topicId
        ORDER BY id LIMIT 5
        """
    )
    fun getQuestions(topicId: Long): List<QuestionEntity>

    @Query(
        """
        SELECT * FROM quiz_sessions
        WHERE topicId = :topicId AND finishedAt IS NULL
        ORDER BY id DESC LIMIT 1
        """
    )
    fun findUnfinishedSession(topicId: Long): SessionEntity?

    @Query("SELECT * FROM quiz_sessions WHERE id = :sessionId")
    fun getSession(sessionId: Long): SessionEntity?

    @Query(
        """
        SELECT COUNT(*) FROM session_questions
        WHERE sessionId = :sessionId
        """
    )
    fun questionCount(sessionId: Long): Int

    @Query(
        """
        SELECT q.*
        FROM questions q
        JOIN session_questions sq ON sq.questionId = q.id
        WHERE sq.sessionId = :sessionId AND sq.position = :position
        """
    )
    fun getQuestionAt(
        sessionId: Long,
        position: Int
    ): QuestionEntity?

    @Query(
        """
        SELECT * FROM answers
        WHERE sessionId = :sessionId AND questionId = :questionId
        """
    )
    fun getAnswer(
        sessionId: Long,
        questionId: Long
    ): AnswerEntity?

    @Query(
        """
        UPDATE quiz_sessions
        SET position = position + 1
        WHERE id = :sessionId
        """
    )
    fun advancePosition(sessionId: Long)

    @Query(
        """
        UPDATE quiz_sessions
        SET finishedAt = :time
        WHERE id = :sessionId
        """
    )
    fun finishSession(sessionId: Long, time: Long)

    @Query(
        """
        SELECT COALESCE(SUM(isCorrect), 0) AS correct,
               COUNT(*) AS answered
        FROM answers
        WHERE sessionId = :sessionId
        """
    )
    fun getResult(sessionId: Long): ScoreRow

    @Query(
        """
        SELECT COUNT(*) FROM quiz_sessions
        WHERE finishedAt IS NOT NULL
        """
    )
    fun completedCount(): Int

    @Query(
        """
        SELECT COALESCE(SUM(a.isCorrect), 0) AS correct,
               COUNT(*) AS answered
        FROM answers a
        JOIN quiz_sessions s ON s.id = a.sessionId
        WHERE s.finishedAt IS NOT NULL
        """
    )
    fun completedScore(): ScoreRow
}