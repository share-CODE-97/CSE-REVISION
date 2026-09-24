package com.example.data.repository

import com.example.data.database.AppDatabase
import com.example.data.entity.RevisionCommentEntity
import com.example.data.entity.RevisionHistoryEntity
import com.example.data.entity.RevisionSubjectEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.util.Locale
import java.util.UUID

class RevisionRepository(private val db: AppDatabase) {
    private val subjectDao = db.revisionSubjectDao()
    private val historyDao = db.revisionHistoryDao()
    private val commentDao = db.revisionCommentDao()

    val allSubjects: Flow<List<RevisionSubjectEntity>> = subjectDao.getAllSubjects()
    val globalHistory: Flow<List<RevisionHistoryEntity>> = historyDao.getGlobalHistory(50)
    val allCompletionTimestamps: Flow<List<Long>> = historyDao.getAllCompletionTimestamps()

    fun getSubject(id: String): Flow<RevisionSubjectEntity?> = subjectDao.getSubjectById(id)

    suspend fun getSubjectOnce(id: String): RevisionSubjectEntity? = withContext(Dispatchers.IO) {
        subjectDao.getSubjectByIdOnce(id)
    }

    suspend fun addSubject(title: String): String = withContext(Dispatchers.IO) {
        val cleanTitle = title.trim()
        val slug = cleanTitle.lowercase(Locale.ROOT)
            .replace(Regex("[^a-z0-9]"), "_")
            .take(20)
        val uniqueId = if (slug.isBlank()) "subj_${System.currentTimeMillis()}" else "${slug}_${System.currentTimeMillis() % 10000}"
        
        val newSubject = RevisionSubjectEntity(
            id = uniqueId,
            title = cleanTitle,
            sortOrder = 99,
            enabled = true,
            completedCount = 0,
            nextDueAt = null,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        subjectDao.insert(newSubject)
        uniqueId
    }

    suspend fun renameSubject(id: String, newTitle: String) = withContext(Dispatchers.IO) {
        subjectDao.renameSubject(id, newTitle.trim())
    }

    suspend fun deleteSubject(id: String) = withContext(Dispatchers.IO) {
        subjectDao.deleteById(id)
    }

    /**
     * Commits completion atomically:
     * 1. Captures actual completion time
     * 2. Increments count
     * 3. Creates history record
     * 4. Applies selected schedule
     * 5. Saves atomically
     */
    suspend fun completeRevision(subjectId: String, nextDueAt: Long?) = withContext(Dispatchers.IO) {
        val subject = subjectDao.getSubjectByIdOnce(subjectId) ?: return@withContext
        val now = System.currentTimeMillis()
        val newCount = subject.completedCount + 1

        val history = RevisionHistoryEntity(
            revisionSubjectId = subjectId,
            completedAt = now,
            scheduledFor = nextDueAt,
            action = "COMPLETE #$newCount"
        )
        historyDao.insert(history)
        subjectDao.updateScheduleAndCount(
            id = subjectId,
            completedCount = newCount,
            nextDueAt = nextDueAt,
            updatedAt = now
        )
    }

    /**
     * Undo last completion:
     * Restores previous count, previous schedule, removes latest history record.
     */
    suspend fun undoLastComplete(subjectId: String) = withContext(Dispatchers.IO) {
        val subject = subjectDao.getSubjectByIdOnce(subjectId) ?: return@withContext
        if (subject.completedCount <= 0) return@withContext

        val lastHistory = historyDao.getLastHistoryForSubject(subjectId)
        if (lastHistory != null) {
            historyDao.delete(lastHistory)
        }

        val restoredCount = (subject.completedCount - 1).coerceAtLeast(0)
        // Find previous schedule from remaining history if any
        val previousHistory = historyDao.getLastHistoryForSubject(subjectId)
        val restoredDue = previousHistory?.scheduledFor

        subjectDao.updateScheduleAndCount(
            id = subjectId,
            completedCount = restoredCount,
            nextDueAt = restoredDue,
            updatedAt = System.currentTimeMillis()
        )
    }

    fun getSubjectHistory(subjectId: String): Flow<List<RevisionHistoryEntity>> {
        return historyDao.getHistoryForSubject(subjectId)
    }

    suspend fun clearSubjectHistory(subjectId: String) = withContext(Dispatchers.IO) {
        historyDao.deleteHistoryForSubject(subjectId)
    }

    fun getSubjectComments(subjectId: String): Flow<List<RevisionCommentEntity>> {
        return commentDao.getCommentsForSubject(subjectId)
    }

    suspend fun addComment(subjectId: String, text: String) = withContext(Dispatchers.IO) {
        val comment = RevisionCommentEntity(
            revisionSubjectId = subjectId,
            text = text.trim(),
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        commentDao.insert(comment)
    }

    suspend fun updateComment(id: Long, subjectId: String, text: String) = withContext(Dispatchers.IO) {
        val comment = RevisionCommentEntity(
            id = id,
            revisionSubjectId = subjectId,
            text = text.trim(),
            updatedAt = System.currentTimeMillis()
        )
        commentDao.update(comment)
    }

    suspend fun deleteComment(id: Long) = withContext(Dispatchers.IO) {
        commentDao.deleteById(id)
    }

    suspend fun resetAllComments() = withContext(Dispatchers.IO) {
        commentDao.clearAll()
    }

    suspend fun resetAllRevisionCounts() = withContext(Dispatchers.IO) {
        subjectDao.resetAllRevisionCounts()
    }

    suspend fun resetAllHistory() = withContext(Dispatchers.IO) {
        historyDao.clearAll()
    }

    suspend fun masterReset() = withContext(Dispatchers.IO) {
        db.clearAllTables()
        // Re-seed default subjects and initial content
        db.revisionSubjectDao().insertAll(com.example.data.database.InitialData.initialSubjects)
        db.topicDao().insertAll(com.example.data.database.InitialData.initialTopics)
        db.noteDao().insertAll(com.example.data.database.InitialData.initialNotes)
        db.questionDao().insertAll(com.example.data.database.InitialData.initialQuestions)
    }
}
