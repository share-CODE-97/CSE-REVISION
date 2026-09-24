package com.example.data.repository

import com.example.data.database.AppDatabase
import com.example.data.entity.NoteEntity
import com.example.data.entity.QuestionEntity
import com.example.data.entity.RevisionCommentEntity
import com.example.data.entity.RevisionHistoryEntity
import com.example.data.entity.RevisionSubjectEntity
import com.example.data.entity.TopicEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class BackupRepository(private val db: AppDatabase) {

    suspend fun exportBackupJson(): String = withContext(Dispatchers.IO) {
        val root = JSONObject()
        root.put("app", "RevisionTracker")
        root.put("version", 1)
        root.put("exportedAt", System.currentTimeMillis())

        // 1. Subjects
        val subjects = db.revisionSubjectDao().getAllSubjects().first()
        val subjectsArr = JSONArray()
        subjects.forEach { s ->
            val obj = JSONObject().apply {
                put("id", s.id)
                put("title", s.title)
                put("sortOrder", s.sortOrder)
                put("enabled", s.enabled)
                put("completedCount", s.completedCount)
                if (s.nextDueAt != null) put("nextDueAt", s.nextDueAt)
                put("createdAt", s.createdAt)
                put("updatedAt", s.updatedAt)
            }
            subjectsArr.put(obj)
        }
        root.put("subjects", subjectsArr)

        // 2. Topics
        val topics = db.topicDao().getAllTopics().first()
        val topicsArr = JSONArray()
        topics.forEach { t ->
            val obj = JSONObject().apply {
                put("id", t.id)
                put("subjectId", t.subjectId)
                put("title", t.title)
                if (t.parentTopicId != null) put("parentTopicId", t.parentTopicId)
                put("sortOrder", t.sortOrder)
            }
            topicsArr.put(obj)
        }
        root.put("topics", topicsArr)

        // 3. Notes
        val notes = db.noteDao().getAllNotes().first()
        val notesArr = JSONArray()
        notes.forEach { n ->
            val obj = JSONObject().apply {
                put("id", n.id)
                put("subjectId", n.subjectId)
                if (n.parentTopicId != null) put("parentTopicId", n.parentTopicId)
                put("title", n.title)
                put("content", n.content)
                put("noteType", n.noteType)
                put("isLocked", n.isLocked)
                put("createdAt", n.createdAt)
                put("updatedAt", n.updatedAt)
                put("sortOrder", n.sortOrder)
            }
            notesArr.put(obj)
        }
        root.put("notes", notesArr)

        // 4. History
        val histories = db.revisionHistoryDao().getAllHistory().first()
        val historyArr = JSONArray()
        histories.forEach { h ->
            val obj = JSONObject().apply {
                put("revisionSubjectId", h.revisionSubjectId)
                put("completedAt", h.completedAt)
                if (h.scheduledFor != null) put("scheduledFor", h.scheduledFor)
                put("action", h.action)
            }
            historyArr.put(obj)
        }
        root.put("history", historyArr)

        // 5. Comments
        val comments = db.revisionCommentDao().getAllComments().first()
        val commentsArr = JSONArray()
        comments.forEach { c ->
            val obj = JSONObject().apply {
                put("revisionSubjectId", c.revisionSubjectId)
                put("text", c.text)
                put("createdAt", c.createdAt)
                put("updatedAt", c.updatedAt)
            }
            commentsArr.put(obj)
        }
        root.put("comments", commentsArr)

        // 6. Questions
        val questions = db.questionDao().getAllQuestions().first()
        val questionsArr = JSONArray()
        questions.forEach { q ->
            val obj = JSONObject().apply {
                put("id", q.id)
                put("subjectId", q.subjectId)
                if (q.topicId != null) put("topicId", q.topicId)
                put("questionText", q.questionText)
                put("optionsJson", q.optionsJson)
                put("correctAnswerIndex", q.correctAnswerIndex)
                put("explanation", q.explanation)
                put("difficulty", q.difficulty)
            }
            questionsArr.put(obj)
        }
        root.put("questions", questionsArr)

        root.toString(2)
    }

    suspend fun importBackupJson(jsonString: String): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val root = JSONObject(jsonString)
            if (!root.has("subjects")) {
                return@withContext Result.failure(IllegalArgumentException("Invalid backup: missing 'subjects' data"))
            }

            // Parse subjects
            val subjectsArr = root.getJSONArray("subjects")
            val parsedSubjects = mutableListOf<RevisionSubjectEntity>()
            for (i in 0 until subjectsArr.length()) {
                val obj = subjectsArr.getJSONObject(i)
                parsedSubjects.add(
                    RevisionSubjectEntity(
                        id = obj.getString("id"),
                        title = obj.getString("title"),
                        sortOrder = obj.optInt("sortOrder", i),
                        enabled = obj.optBoolean("enabled", true),
                        completedCount = obj.optInt("completedCount", 0),
                        nextDueAt = if (obj.has("nextDueAt")) obj.getLong("nextDueAt") else null,
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                        updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
                    )
                )
            }

            // Parse topics
            val parsedTopics = mutableListOf<TopicEntity>()
            if (root.has("topics")) {
                val topicsArr = root.getJSONArray("topics")
                for (i in 0 until topicsArr.length()) {
                    val obj = topicsArr.getJSONObject(i)
                    parsedTopics.add(
                        TopicEntity(
                            id = obj.getString("id"),
                            subjectId = obj.getString("subjectId"),
                            title = obj.getString("title"),
                            parentTopicId = if (obj.has("parentTopicId")) obj.getString("parentTopicId") else null,
                            sortOrder = obj.optInt("sortOrder", 0)
                        )
                    )
                }
            }

            // Parse notes
            val parsedNotes = mutableListOf<NoteEntity>()
            if (root.has("notes")) {
                val notesArr = root.getJSONArray("notes")
                for (i in 0 until notesArr.length()) {
                    val obj = notesArr.getJSONObject(i)
                    parsedNotes.add(
                        NoteEntity(
                            id = obj.getString("id"),
                            subjectId = obj.getString("subjectId"),
                            parentTopicId = if (obj.has("parentTopicId")) obj.getString("parentTopicId") else null,
                            title = obj.getString("title"),
                            content = obj.getString("content"),
                            noteType = obj.optString("noteType", "STANDARD"),
                            isLocked = obj.optBoolean("isLocked", true),
                            createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                            updatedAt = obj.optLong("updatedAt", System.currentTimeMillis()),
                            sortOrder = obj.optInt("sortOrder", 0)
                        )
                    )
                }
            }

            // Parse history
            val parsedHistory = mutableListOf<RevisionHistoryEntity>()
            if (root.has("history")) {
                val historyArr = root.getJSONArray("history")
                for (i in 0 until historyArr.length()) {
                    val obj = historyArr.getJSONObject(i)
                    parsedHistory.add(
                        RevisionHistoryEntity(
                            revisionSubjectId = obj.getString("revisionSubjectId"),
                            completedAt = obj.optLong("completedAt", System.currentTimeMillis()),
                            scheduledFor = if (obj.has("scheduledFor")) obj.getLong("scheduledFor") else null,
                            action = obj.optString("action", "COMPLETE")
                        )
                    )
                }
            }

            // Parse comments
            val parsedComments = mutableListOf<RevisionCommentEntity>()
            if (root.has("comments")) {
                val commentsArr = root.getJSONArray("comments")
                for (i in 0 until commentsArr.length()) {
                    val obj = commentsArr.getJSONObject(i)
                    parsedComments.add(
                        RevisionCommentEntity(
                            revisionSubjectId = obj.getString("revisionSubjectId"),
                            text = obj.getString("text"),
                            createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                            updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
                        )
                    )
                }
            }

            // Parse questions
            val parsedQuestions = mutableListOf<QuestionEntity>()
            if (root.has("questions")) {
                val questionsArr = root.getJSONArray("questions")
                for (i in 0 until questionsArr.length()) {
                    val obj = questionsArr.getJSONObject(i)
                    parsedQuestions.add(
                        QuestionEntity(
                            id = obj.getString("id"),
                            subjectId = obj.getString("subjectId"),
                            topicId = if (obj.has("topicId")) obj.getString("topicId") else null,
                            questionText = obj.getString("questionText"),
                            optionsJson = obj.getString("optionsJson"),
                            correctAnswerIndex = obj.getInt("correctAnswerIndex"),
                            explanation = obj.optString("explanation", ""),
                            difficulty = obj.optString("difficulty", "MEDIUM")
                        )
                    )
                }
            }

            // Atomic database update
            db.runInTransaction {
                db.clearAllTables()
                kotlinx.coroutines.runBlocking {
                    db.revisionSubjectDao().insertAll(parsedSubjects)
                    db.topicDao().insertAll(parsedTopics)
                    db.noteDao().insertAll(parsedNotes)
                    db.revisionHistoryDao().insertAll(parsedHistory)
                    db.revisionCommentDao().insertAll(parsedComments)
                    if (parsedQuestions.isNotEmpty()) {
                        db.questionDao().insertAll(parsedQuestions)
                    }
                }
            }

            Result.success(parsedSubjects.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
