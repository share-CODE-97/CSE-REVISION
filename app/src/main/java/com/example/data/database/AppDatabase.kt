package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.dao.NoteDao
import com.example.data.dao.QuestionDao
import com.example.data.dao.RevisionCommentDao
import com.example.data.dao.RevisionHistoryDao
import com.example.data.dao.RevisionSubjectDao
import com.example.data.dao.TestAttemptDao
import com.example.data.dao.TopicDao
import com.example.data.entity.NoteEntity
import com.example.data.entity.QuestionEntity
import com.example.data.entity.RevisionCommentEntity
import com.example.data.entity.RevisionHistoryEntity
import com.example.data.entity.RevisionSubjectEntity
import com.example.data.entity.TestAttemptEntity
import com.example.data.entity.TopicEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        RevisionSubjectEntity::class,
        RevisionHistoryEntity::class,
        RevisionCommentEntity::class,
        TopicEntity::class,
        NoteEntity::class,
        QuestionEntity::class,
        TestAttemptEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun revisionSubjectDao(): RevisionSubjectDao
    abstract fun revisionHistoryDao(): RevisionHistoryDao
    abstract fun revisionCommentDao(): RevisionCommentDao
    abstract fun topicDao(): TopicDao
    abstract fun noteDao(): NoteDao
    abstract fun questionDao(): QuestionDao
    abstract fun testAttemptDao(): TestAttemptDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "revision_tracker.db"
                )
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            // Seed initial database content on background thread
                            CoroutineScope(Dispatchers.IO).launch {
                                val database = getInstance(context)
                                database.revisionSubjectDao().insertAll(InitialData.initialSubjects)
                                database.topicDao().insertAll(InitialData.initialTopics)
                                database.noteDao().insertAll(InitialData.initialNotes)
                                database.questionDao().insertAll(InitialData.initialQuestions)
                            }
                        }
                    })
                    .fallbackToDestructiveMigration(false)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
