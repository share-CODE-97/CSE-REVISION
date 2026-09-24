package com.example.data.repository

import com.example.data.database.AppDatabase
import com.example.data.entity.NoteEntity
import com.example.data.entity.TopicEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.util.UUID

class StudyRepository(private val db: AppDatabase) {
    private val topicDao = db.topicDao()
    private val noteDao = db.noteDao()

    fun getTopicsForSubject(subjectId: String): Flow<List<TopicEntity>> = topicDao.getTopicsForSubject(subjectId)

    fun getAllTopics(): Flow<List<TopicEntity>> = topicDao.getAllTopics()

    fun getNotesForSubject(subjectId: String): Flow<List<NoteEntity>> = noteDao.getNotesForSubject(subjectId)

    fun getNotesForTopic(topicId: String): Flow<List<NoteEntity>> = noteDao.getNotesForTopic(topicId)

    fun getNote(noteId: String): Flow<NoteEntity?> = noteDao.getNoteById(noteId)

    suspend fun getNoteOnce(noteId: String): NoteEntity? = withContext(Dispatchers.IO) {
        noteDao.getNoteByIdOnce(noteId)
    }

    suspend fun addTopic(subjectId: String, title: String, parentTopicId: String? = null): String = withContext(Dispatchers.IO) {
        val id = "top_${UUID.randomUUID().toString().take(8)}"
        val topic = TopicEntity(
            id = id,
            subjectId = subjectId,
            title = title.trim(),
            parentTopicId = parentTopicId,
            sortOrder = 0
        )
        topicDao.insert(topic)
        id
    }

    suspend fun deleteTopic(topicId: String) = withContext(Dispatchers.IO) {
        topicDao.deleteById(topicId)
    }

    suspend fun saveNote(
        id: String? = null,
        subjectId: String,
        parentTopicId: String? = null,
        title: String,
        content: String,
        noteType: String = "STANDARD",
        isLocked: Boolean = true
    ): String = withContext(Dispatchers.IO) {
        val noteId = id ?: "note_${UUID.randomUUID().toString().take(8)}"
        val now = System.currentTimeMillis()
        val existing = noteDao.getNoteByIdOnce(noteId)
        val note = NoteEntity(
            id = noteId,
            subjectId = subjectId,
            parentTopicId = parentTopicId,
            title = title.trim(),
            content = content,
            noteType = noteType,
            isLocked = isLocked,
            createdAt = existing?.createdAt ?: now,
            updatedAt = now,
            sortOrder = existing?.sortOrder ?: 0
        )
        noteDao.insert(note)
        noteId
    }

    suspend fun setNoteLock(noteId: String, isLocked: Boolean) = withContext(Dispatchers.IO) {
        noteDao.setLockState(noteId, isLocked, System.currentTimeMillis())
    }

    suspend fun deleteNote(noteId: String) = withContext(Dispatchers.IO) {
        noteDao.deleteById(noteId)
    }
}
