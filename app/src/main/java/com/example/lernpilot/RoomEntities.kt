package com.example.lernpilot

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "topics")
data class TopicEntity(
    @PrimaryKey val id: Long,
    val name: String
)

@Entity(
    tableName = "questions",
    foreignKeys = [
        ForeignKey(
            entity = TopicEntity::class,
            parentColumns = ["id"],
            childColumns = ["topicId"]
        )
    ],
    indices = [Index("topicId")]
)
data class QuestionEntity(
    @PrimaryKey val id: Long,
    val topicId: Long,
    val text: String,
    val optionA: String,
    val optionB: String,
    val optionC: String,
    val optionD: String,
    val correctIndex: Int,
    val explanation: String
) {
    fun toQuestion(): Question = Question(
        id = id,
        text = text,
        options = listOf(optionA, optionB, optionC, optionD),
        correctIndex = correctIndex,
        explanation = explanation
    )
}

@Entity(
    tableName = "quiz_sessions",
    foreignKeys = [
        ForeignKey(
            entity = TopicEntity::class,
            parentColumns = ["id"],
            childColumns = ["topicId"]
        )
    ],
    indices = [Index("topicId")]
)
data class SessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val topicId: Long,
    val startedAt: Long = System.currentTimeMillis(),
    val finishedAt: Long? = null,
    val position: Int = 0
)

@Entity(
    tableName = "session_questions",
    primaryKeys = ["sessionId", "position"],
    foreignKeys = [
        ForeignKey(
            entity = SessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"]
        ),
        ForeignKey(
            entity = QuestionEntity::class,
            parentColumns = ["id"],
            childColumns = ["questionId"]
        )
    ],
    indices = [
        Index(value = ["sessionId", "questionId"], unique = true),
        Index("questionId")
    ]
)
data class SessionQuestionEntity(
    val sessionId: Long,
    val position: Int,
    val questionId: Long
)

@Entity(
    tableName = "answers",
    foreignKeys = [
        ForeignKey(
            entity = SessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"]
        ),
        ForeignKey(
            entity = QuestionEntity::class,
            parentColumns = ["id"],
            childColumns = ["questionId"]
        )
    ],
    indices = [
        Index(value = ["sessionId", "questionId"], unique = true),
        Index("questionId")
    ]
)
data class AnswerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val questionId: Long,
    val selectedIndex: Int,
    val isCorrect: Boolean,
    val answeredAt: Long = System.currentTimeMillis()
)

// Holds the result of an aggregate query; it is not a table.
data class ScoreRow(
    val correct: Int,
    val answered: Int
)