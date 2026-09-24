package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.entity.NoteEntity
import com.example.data.entity.QuestionEntity
import com.example.data.entity.RevisionCommentEntity
import com.example.data.entity.RevisionHistoryEntity
import com.example.data.entity.RevisionSubjectEntity
import com.example.data.entity.TestAttemptEntity
import com.example.data.entity.TopicEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RevisionSubjectDao {
    @Query("SELECT * FROM revision_subjects ORDER BY sortOrder ASC, title ASC")
    fun getAllSubjects(): Flow<List<RevisionSubjectEntity>>

    @Query("SELECT * FROM revision_subjects ORDER BY sortOrder ASC, title ASC")
    suspend fun getAllSubjectsOnce(): List<RevisionSubjectEntity>

    @Query("SELECT * FROM revision_subjects WHERE id = :id LIMIT 1")
    fun getSubjectById(id: String): Flow<RevisionSubjectEntity?>

    @Query("SELECT * FROM revision_subjects WHERE id = :id LIMIT 1")
    suspend fun getSubjectByIdOnce(id: String): RevisionSubjectEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(subject: RevisionSubjectEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(subjects: List<RevisionSubjectEntity>)

    @Update
    suspend fun update(subject: RevisionSubjectEntity)

    @Delete
    suspend fun delete(subject: RevisionSubjectEntity)

    @Query("DELETE FROM revision_subjects WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("UPDATE revision_subjects SET completedCount = :completedCount, nextDueAt = :nextDueAt, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateScheduleAndCount(id: String, completedCount: Int, nextDueAt: Long?, updatedAt: Long)

    @Query("UPDATE revision_subjects SET completedCount = 0, nextDueAt = NULL, updatedAt = :now")
    suspend fun resetAllRevisionCounts(now: Long = System.currentTimeMillis())

    @Query("UPDATE revision_subjects SET title = :newTitle, updatedAt = :now WHERE id = :id")
    suspend fun renameSubject(id: String, newTitle: String, now: Long = System.currentTimeMillis())

    @Query("DELETE FROM revision_subjects")
    suspend fun clearAll()
}

@Dao
interface RevisionHistoryDao {
    @Query("SELECT * FROM revision_history WHERE revisionSubjectId = :subjectId ORDER BY completedAt DESC")
    fun getHistoryForSubject(subjectId: String): Flow<List<RevisionHistoryEntity>>

    @Query("SELECT * FROM revision_history ORDER BY completedAt DESC LIMIT :limit")
    fun getGlobalHistory(limit: Int = 50): Flow<List<RevisionHistoryEntity>>

    @Query("SELECT * FROM revision_history ORDER BY completedAt DESC")
    fun getAllHistory(): Flow<List<RevisionHistoryEntity>>

    @Query("SELECT completedAt FROM revision_history ORDER BY completedAt DESC")
    fun getAllCompletionTimestamps(): Flow<List<Long>>

    @Query("SELECT * FROM revision_history WHERE revisionSubjectId = :subjectId ORDER BY completedAt DESC LIMIT 1")
    suspend fun getLastHistoryForSubject(subjectId: String): RevisionHistoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(history: RevisionHistoryEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(histories: List<RevisionHistoryEntity>)

    @Delete
    suspend fun delete(history: RevisionHistoryEntity)

    @Query("DELETE FROM revision_history WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM revision_history WHERE revisionSubjectId = :subjectId")
    suspend fun deleteHistoryForSubject(subjectId: String)

    @Query("DELETE FROM revision_history")
    suspend fun clearAll()
}

@Dao
interface RevisionCommentDao {
    @Query("SELECT * FROM revision_comments WHERE revisionSubjectId = :subjectId ORDER BY createdAt DESC")
    fun getCommentsForSubject(subjectId: String): Flow<List<RevisionCommentEntity>>

    @Query("SELECT * FROM revision_comments")
    fun getAllComments(): Flow<List<RevisionCommentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(comment: RevisionCommentEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(comments: List<RevisionCommentEntity>)

    @Update
    suspend fun update(comment: RevisionCommentEntity)

    @Delete
    suspend fun delete(comment: RevisionCommentEntity)

    @Query("DELETE FROM revision_comments WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM revision_comments")
    suspend fun clearAll()
}

@Dao
interface TopicDao {
    @Query("SELECT * FROM topics WHERE subjectId = :subjectId ORDER BY sortOrder ASC, title ASC")
    fun getTopicsForSubject(subjectId: String): Flow<List<TopicEntity>>

    @Query("SELECT * FROM topics ORDER BY title ASC")
    fun getAllTopics(): Flow<List<TopicEntity>>

    @Query("SELECT * FROM topics ORDER BY title ASC")
    suspend fun getAllTopicsOnce(): List<TopicEntity>

    @Query("SELECT * FROM topics WHERE subjectId = :subjectId")
    suspend fun getTopicsForSubjectOnce(subjectId: String): List<TopicEntity>

    @Query("SELECT * FROM topics WHERE id = :id LIMIT 1")
    fun getTopicById(id: String): Flow<TopicEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(topic: TopicEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(topics: List<TopicEntity>)

    @Update
    suspend fun update(topic: TopicEntity)

    @Delete
    suspend fun delete(topic: TopicEntity)

    @Query("DELETE FROM topics WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM topics")
    suspend fun clearAll()
}

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes WHERE subjectId = :subjectId ORDER BY sortOrder ASC, createdAt DESC")
    fun getNotesForSubject(subjectId: String): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE parentTopicId = :topicId ORDER BY sortOrder ASC, createdAt DESC")
    fun getNotesForTopic(topicId: String): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes ORDER BY createdAt DESC")
    fun getAllNotes(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE id = :id LIMIT 1")
    fun getNoteById(id: String): Flow<NoteEntity?>

    @Query("SELECT * FROM notes WHERE id = :id LIMIT 1")
    suspend fun getNoteByIdOnce(id: String): NoteEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(note: NoteEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(notes: List<NoteEntity>)

    @Update
    suspend fun update(note: NoteEntity)

    @Delete
    suspend fun delete(note: NoteEntity)

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("UPDATE notes SET isLocked = :isLocked, updatedAt = :updatedAt WHERE id = :id")
    suspend fun setLockState(id: String, isLocked: Boolean, updatedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM notes")
    suspend fun clearAll()
}

@Dao
interface QuestionDao {
    @Query("SELECT * FROM questions WHERE (:subjectId IS NULL OR subjectId = :subjectId) AND (:topicId IS NULL OR topicId = :topicId)")
    fun getQuestions(subjectId: String?, topicId: String?): Flow<List<QuestionEntity>>

    @Query("SELECT * FROM questions ORDER BY id ASC")
    fun getAllQuestions(): Flow<List<QuestionEntity>>

    @Query("SELECT COUNT(*) FROM questions WHERE (:subjectId IS NULL OR subjectId = :subjectId) AND (:topicId IS NULL OR topicId = :topicId)")
    suspend fun getAvailableQuestionCount(subjectId: String?, topicId: String?): Int

    @Query("SELECT COUNT(*) FROM questions")
    suspend fun getTotalQuestionCount(): Int

    @Query("SELECT subjectId, COUNT(*) as count FROM questions GROUP BY subjectId")
    suspend fun getQuestionCountsGroupedBySubject(): List<SubjectQuestionCountTuple>

    @Query("""
        SELECT * FROM questions 
        WHERE (:subjectId IS NULL OR subjectId = :subjectId) 
          AND (:topicId IS NULL OR topicId = :topicId) 
        ORDER BY RANDOM() 
        LIMIT :limit
    """)
    suspend fun getRandomQuestions(subjectId: String?, topicId: String?, limit: Int): List<QuestionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(question: QuestionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(questions: List<QuestionEntity>)

    @Query("DELETE FROM questions WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM questions WHERE subjectId = :subjectId")
    suspend fun deleteBySubject(subjectId: String)

    @Query("DELETE FROM questions")
    suspend fun clearAll()
}

data class SubjectQuestionCountTuple(
    val subjectId: String,
    val count: Int
)

@Dao
interface TestAttemptDao {
    @Query("SELECT * FROM test_attempts ORDER BY timestamp DESC")
    fun getAllAttempts(): Flow<List<TestAttemptEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(attempt: TestAttemptEntity): Long

    @Query("DELETE FROM test_attempts")
    suspend fun clearAll()
}
