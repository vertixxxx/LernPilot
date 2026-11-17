package com.example.lernpilot

object RoomSeed {

    val topics = listOf(
        TopicEntity(1, "Kotlin basics"),
        TopicEntity(2, "SQL basics")
    )

    val questions = listOf(
        QuestionEntity(
            1, 1,
            "Which keyword declares a read-only variable?",
            "var", "val", "fun", "class",
            1, "val prevents reassignment of the variable."
        ),
        QuestionEntity(
            2, 1,
            "Which keyword declares a function?",
            "fun", "val", "return", "object",
            0, "fun introduces a Kotlin function."
        ),
        QuestionEntity(
            3, 1,
            "What is the type of true?",
            "String", "Int", "Boolean", "Double",
            2, "Boolean values are true or false."
        ),
        QuestionEntity(
            4, 1,
            "Which type can hold null?",
            "String", "Int", "Boolean", "String?",
            3, "A question mark marks a nullable type."
        ),
        QuestionEntity(
            5, 1,
            "What does listOf(2, 4, 6).size return?",
            "2", "3", "4", "6",
            1, "There are three elements."
        ),
        QuestionEntity(
            6, 2,
            "Which statement reads rows?",
            "INSERT", "DELETE", "SELECT", "UPDATE",
            2, "SELECT retrieves data from a table."
        ),
        QuestionEntity(
            7, 2,
            "Which clause filters rows?",
            "WHERE", "ORDER BY", "VALUES", "CREATE",
            0, "WHERE restricts rows using a condition."
        ),
        QuestionEntity(
            8, 2,
            "What identifies a row uniquely?",
            "Table name", "Foreign key", "Column order", "Primary key",
            3, "A primary key uniquely identifies a row."
        ),
        QuestionEntity(
            9, 2,
            "Which statement adds a row?",
            "UPDATE", "INSERT", "DROP", "SELECT",
            1, "INSERT adds rows to a table."
        ),
        QuestionEntity(
            10, 2,
            "Which expression counts rows?",
            "SUM(*)", "TOTAL(*)", "COUNT(*)", "SIZE(*)",
            2, "COUNT(*) counts rows in the result."
        )
    )
}