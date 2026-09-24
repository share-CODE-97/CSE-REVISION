package com.example.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "revision_subjects")
data class RevisionSubjectEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val sortOrder: Int = 0,
    val enabled: Boolean = true,
    val completedCount: Int = 0,
    val nextDueAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "revision_history",
    foreignKeys = [
        ForeignKey(
            entity = RevisionSubjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["revisionSubjectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("revisionSubjectId"), Index("completedAt")]
)
data class RevisionHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val revisionSubjectId: String,
    val completedAt: Long = System.currentTimeMillis(),
    val scheduledFor: Long? = null,
    val action: String = "COMPLETE"
)

@Entity(
    tableName = "revision_comments",
    foreignKeys = [
        ForeignKey(
            entity = RevisionSubjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["revisionSubjectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("revisionSubjectId")]
)
data class RevisionCommentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val revisionSubjectId: String,
    val text: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "topics",
    foreignKeys = [
        ForeignKey(
            entity = RevisionSubjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["subjectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("subjectId"), Index("parentTopicId")]
)
data class TopicEntity(
    @PrimaryKey
    val id: String,
    val subjectId: String,
    val title: String,
    val parentTopicId: String? = null,
    val sortOrder: Int = 0
)

@Entity(
    tableName = "notes",
    foreignKeys = [
        ForeignKey(
            entity = RevisionSubjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["subjectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("subjectId"), Index("parentTopicId")]
)
data class NoteEntity(
    @PrimaryKey
    val id: String,
    val subjectId: String,
    val parentTopicId: String? = null,
    val title: String,
    val content: String,
    val noteType: String = "STANDARD",
    val isLocked: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val sortOrder: Int = 0
)

@Entity(
    tableName = "questions",
    foreignKeys = [
        ForeignKey(
            entity = RevisionSubjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["subjectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("subjectId"), Index("topicId")]
)
data class QuestionEntity(
    @PrimaryKey
    val id: String,
    val subjectId: String,
    val topicId: String? = null,
    val questionText: String,
    val optionsJson: String, // Stored as JSON array: ["Option A", "Option B", "Option C", "Option D"]
    val correctAnswerIndex: Int,
    val explanation: String = "",
    val difficulty: String = "MEDIUM"
)

@Entity(
    tableName = "test_attempts",
    indices = [Index("timestamp"), Index("subjectId")]
)
data class TestAttemptEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val subjectId: String? = null,
    val topicId: String? = null,
    val totalQuestions: Int,
    val correctCount: Int,
    val wrongCount: Int,
    val skippedCount: Int,
    val timeSpentSeconds: Long,
    val accuracyPercentage: Float
)
