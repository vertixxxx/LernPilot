package com.example.lernpilot

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        TopicEntity::class,
        QuestionEntity::class,
        SessionEntity::class,
        SessionQuestionEntity::class,
        AnswerEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class LernPilotDatabase : RoomDatabase() {

    abstract fun quizDao(): QuizDao

    companion object {
        @Volatile
        private var instance: LernPilotDatabase? = null

        fun get(context: Context): LernPilotDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    LernPilotDatabase::class.java,
                    "lernpilot_room.db"
                ).build().also {
                    instance = it
                }
            }
        }
    }
}