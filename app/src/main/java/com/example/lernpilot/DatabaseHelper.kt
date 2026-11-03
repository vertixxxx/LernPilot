package com.example.lernpilot

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper private constructor(context: Context) :
    SQLiteOpenHelper(context, "lernpilot.db", null, 1) {

    companion object {
        @Volatile
        private var instance: DatabaseHelper? = null

        fun get(context: Context): DatabaseHelper {
            return instance ?: synchronized(this) {
                instance ?: DatabaseHelper(context.applicationContext)
                    .also { instance = it }
            }
        }
    }

    override fun onConfigure(db: SQLiteDatabase) {
        db.setForeignKeyConstraintsEnabled(true)
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE topics (
                id INTEGER PRIMARY KEY,
                name TEXT NOT NULL
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE questions (
                id INTEGER PRIMARY KEY,
                topic_id INTEGER NOT NULL REFERENCES topics(id),
                text TEXT NOT NULL,
                option_a TEXT NOT NULL,
                option_b TEXT NOT NULL,
                option_c TEXT NOT NULL,
                option_d TEXT NOT NULL,
                correct_index INTEGER NOT NULL
                    CHECK(correct_index BETWEEN 0 AND 3),
                explanation TEXT NOT NULL
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE quiz_sessions (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                topic_id INTEGER NOT NULL REFERENCES topics(id),
                started_at INTEGER NOT NULL,
                finished_at INTEGER,
                position INTEGER NOT NULL DEFAULT 0
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE session_questions (
                session_id INTEGER NOT NULL REFERENCES quiz_sessions(id),
                position INTEGER NOT NULL,
                question_id INTEGER NOT NULL REFERENCES questions(id),
                PRIMARY KEY(session_id, position),
                UNIQUE(session_id, question_id)
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE answers (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                session_id INTEGER NOT NULL REFERENCES quiz_sessions(id),
                question_id INTEGER NOT NULL REFERENCES questions(id),
                selected_index INTEGER NOT NULL
                    CHECK(selected_index BETWEEN 0 AND 3),
                is_correct INTEGER NOT NULL CHECK(is_correct IN (0, 1)),
                answered_at INTEGER NOT NULL,
                UNIQUE(session_id, question_id)
            )
            """.trimIndent()
        )

        db.execSQL(
            "INSERT INTO topics VALUES (1, 'Kotlin basics'), (2, 'SQL basics')"
        )

        insertQuestion(
            db, 1, 1,
            "Which keyword declares a read-only variable?",
            listOf("var", "val", "fun", "class"), 1,
            "val prevents reassignment of the variable."
        )

        insertQuestion(
            db, 2, 1,
            "Which keyword declares a function?",
            listOf("fun", "val", "return", "object"), 0,
            "fun introduces a Kotlin function."
        )

        insertQuestion(
            db, 3, 1,
            "What is the type of true?",
            listOf("String", "Int", "Boolean", "Double"), 2,
            "Boolean values are true or false."
        )

        insertQuestion(
            db, 4, 1,
            "Which type can hold null?",
            listOf("String", "Int", "Boolean", "String?"), 3,
            "A question mark marks a nullable type."
        )

        insertQuestion(
            db, 5, 1,
            "What does listOf(2, 4, 6).size return?",
            listOf("2", "3", "4", "6"), 1,
            "size is the number of elements: three."
        )

        insertQuestion(
            db, 6, 2,
            "Which statement reads rows?",
            listOf("INSERT", "DELETE", "SELECT", "UPDATE"), 2,
            "SELECT retrieves data from a table."
        )

        insertQuestion(
            db, 7, 2,
            "Which clause filters rows?",
            listOf("WHERE", "ORDER BY", "VALUES", "CREATE"), 0,
            "WHERE restricts rows using a condition."
        )

        insertQuestion(
            db, 8, 2,
            "What identifies a row uniquely?",
            listOf(
                "Table name", "Foreign key",
                "Column order", "Primary key"
            ), 3,
            "A primary key uniquely identifies a row."
        )

        insertQuestion(
            db, 9, 2,
            "Which statement adds a row?",
            listOf("UPDATE", "INSERT", "DROP", "SELECT"), 1,
            "INSERT adds rows to a table."
        )

        insertQuestion(
            db, 10, 2,
            "Which expression counts rows?",
            listOf("SUM(*)", "TOTAL(*)", "COUNT(*)", "SIZE(*)"), 2,
            "COUNT(*) counts rows in the result."
        )
    }

    private fun insertQuestion(
        db: SQLiteDatabase,
        id: Long,
        topicId: Long,
        text: String,
        options: List<String>,
        correctIndex: Int,
        explanation: String
    ) {
        val values = ContentValues().apply {
            put("id", id)
            put("topic_id", topicId)
            put("text", text)
            put("option_a", options[0])
            put("option_b", options[1])
            put("option_c", options[2])
            put("option_d", options[3])
            put("correct_index", correctIndex)
            put("explanation", explanation)
        }

        db.insertOrThrow("questions", null, values)
    }

    override fun onUpgrade(
        db: SQLiteDatabase,
        oldVersion: Int,
        newVersion: Int
    ) {
        // Add a migration here before increasing the database version.
        error("Missing database migration from $oldVersion to $newVersion")
    }

    fun getTopics(): List<Topic> {
        return readableDatabase.rawQuery(
            "SELECT id, name FROM topics ORDER BY id",
            null
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    add(Topic(cursor.getLong(0), cursor.getString(1)))
                }
            }
        }
    }

    fun startOrResumeSession(topicId: Long): Long {
        val db = writableDatabase
        db.beginTransaction()

        try {
            val existingId = db.rawQuery(
                """
                SELECT id FROM quiz_sessions
                WHERE topic_id = ? AND finished_at IS NULL
                ORDER BY id DESC LIMIT 1
                """.trimIndent(),
                arrayOf(topicId.toString())
            ).use { cursor ->
                if (cursor.moveToFirst()) cursor.getLong(0) else null
            }

            if (existingId != null) {
                db.setTransactionSuccessful()
                return existingId
            }

            val questionIds = db.rawQuery(
                """
                SELECT id FROM questions
                WHERE topic_id = ?
                ORDER BY id LIMIT 5
                """.trimIndent(),
                arrayOf(topicId.toString())
            ).use { cursor ->
                buildList {
                    while (cursor.moveToNext()) {
                        add(cursor.getLong(0))
                    }
                }
            }

            require(questionIds.isNotEmpty()) {
                "No questions found for this topic."
            }

            val values = ContentValues().apply {
                put("topic_id", topicId)
                put("started_at", System.currentTimeMillis())
            }

            val sessionId = db.insertOrThrow(
                "quiz_sessions", null, values
            )

            questionIds.forEachIndexed { index, questionId ->
                db.execSQL(
                    """
                    INSERT INTO session_questions
                    (session_id, position, question_id)
                    VALUES (?, ?, ?)
                    """.trimIndent(),
                    arrayOf(sessionId, index, questionId)
                )
            }

            db.setTransactionSuccessful()
            return sessionId
        } finally {
            db.endTransaction()
        }
    }

    fun getQuizState(sessionId: Long): QuizState {
        val db = readableDatabase

        val session = db.rawQuery(
            "SELECT position, finished_at FROM quiz_sessions WHERE id = ?",
            arrayOf(sessionId.toString())
        ).use { cursor ->
            require(cursor.moveToFirst()) { "Session not found." }
            cursor.getInt(0) to !cursor.isNull(1)
        }

        val position = session.first
        val finished = session.second

        val total = db.rawQuery(
            "SELECT COUNT(*) FROM session_questions WHERE session_id = ?",
            arrayOf(sessionId.toString())
        ).use { cursor ->
            cursor.moveToFirst()
            cursor.getInt(0)
        }

        if (finished) {
            return QuizState(
                sessionId, position, total, true, null, null
            )
        }

        val question = db.rawQuery(
            """
            SELECT q.id, q.text,
                   q.option_a, q.option_b, q.option_c, q.option_d,
                   q.correct_index, q.explanation
            FROM questions q
            JOIN session_questions sq ON sq.question_id = q.id
            WHERE sq.session_id = ? AND sq.position = ?
            """.trimIndent(),
            arrayOf(sessionId.toString(), position.toString())
        ).use { cursor ->
            require(cursor.moveToFirst()) { "Question not found." }

            Question(
                id = cursor.getLong(0),
                text = cursor.getString(1),
                options = (2..5).map { cursor.getString(it) },
                correctIndex = cursor.getInt(6),
                explanation = cursor.getString(7)
            )
        }

        val selectedIndex = db.rawQuery(
            """
            SELECT selected_index FROM answers
            WHERE session_id = ? AND question_id = ?
            """.trimIndent(),
            arrayOf(sessionId.toString(), question.id.toString())
        ).use { cursor ->
            if (cursor.moveToFirst()) cursor.getInt(0) else null
        }

        return QuizState(
            sessionId, position, total, false,
            question, selectedIndex
        )
    }

    fun submitAnswer(
        sessionId: Long,
        questionId: Long,
        selectedIndex: Int
    ) {
        require(selectedIndex in 0..3)

        val db = writableDatabase
        db.beginTransaction()

        try {
            val state = getQuizState(sessionId)
            val question = requireNotNull(state.question)

            require(!state.finished && question.id == questionId)

            if (state.selectedIndex == null) {
                val values = ContentValues().apply {
                    put("session_id", sessionId)
                    put("question_id", questionId)
                    put("selected_index", selectedIndex)
                    put(
                        "is_correct",
                        if (selectedIndex == question.correctIndex) 1 else 0
                    )
                    put("answered_at", System.currentTimeMillis())
                }

                db.insertOrThrow("answers", null, values)
            }

            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun advance(sessionId: Long, expectedPosition: Int) {
        val db = writableDatabase
        db.beginTransaction()

        try {
            val state = getQuizState(sessionId)

            // A stale or repeated Next action cannot skip a question.
            if (!state.finished && state.position == expectedPosition) {
                require(state.selectedIndex != null) {
                    "Answer the question first."
                }

                if (state.position + 1 == state.total) {
                    db.execSQL(
                        "UPDATE quiz_sessions SET finished_at = ? WHERE id = ?",
                        arrayOf(System.currentTimeMillis(), sessionId)
                    )
                } else {
                    db.execSQL(
                        """
                        UPDATE quiz_sessions
                        SET position = position + 1
                        WHERE id = ?
                        """.trimIndent(),
                        arrayOf(sessionId)
                    )
                }
            }

            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun getResult(sessionId: Long): Pair<Int, Int> {
        return readableDatabase.rawQuery(
            """
            SELECT COALESCE(SUM(is_correct), 0), COUNT(*)
            FROM answers WHERE session_id = ?
            """.trimIndent(),
            arrayOf(sessionId.toString())
        ).use { cursor ->
            cursor.moveToFirst()
            cursor.getInt(0) to cursor.getInt(1)
        }
    }

    fun getDashboardStats(): DashboardStats {
        val db = readableDatabase

        val completed = db.rawQuery(
            "SELECT COUNT(*) FROM quiz_sessions WHERE finished_at IS NOT NULL",
            null
        ).use { cursor ->
            cursor.moveToFirst()
            cursor.getInt(0)
        }

        return db.rawQuery(
            """
            SELECT COALESCE(SUM(a.is_correct), 0), COUNT(*)
            FROM answers a
            JOIN quiz_sessions s ON s.id = a.session_id
            WHERE s.finished_at IS NOT NULL
            """.trimIndent(),
            null
        ).use { cursor ->
            cursor.moveToFirst()

            DashboardStats(
                completed = completed,
                correct = cursor.getInt(0),
                answered = cursor.getInt(1)
            )
        }
    }
}