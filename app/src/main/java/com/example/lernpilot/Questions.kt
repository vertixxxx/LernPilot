package com.example.lernpilot

data class Topic(
    val id: Long,
    val name: String
)

data class Question(
    val id: Long,
    val text: String,
    val options: List<String>,
    val correctIndex: Int,
    val explanation: String
)

data class QuizState(
    val sessionId: Long,
    val position: Int,
    val total: Int,
    val finished: Boolean,
    val question: Question?,
    val selectedIndex: Int?
)

data class DashboardStats(
    val completed: Int,
    val correct: Int,
    val answered: Int
)