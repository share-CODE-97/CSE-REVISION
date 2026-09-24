package com.example.data.repository

import com.example.data.database.AppDatabase
import com.example.data.entity.QuestionEntity
import com.example.data.entity.TestAttemptEntity
import com.example.domain.model.QuestionReviewItem
import com.example.domain.model.TestSummary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.json.JSONArray

class TestRepository(private val db: AppDatabase) {
    private val questionDao = db.questionDao()
    private val attemptDao = db.testAttemptDao()

    companion object {
        // Standard question count options that can easily be expanded later without changing test engine
        val DEFAULT_QUESTION_COUNT_OPTIONS = listOf(5, 10, 15, 20, 25, 30, 50)
    }

    fun getAllQuestions(): Flow<List<QuestionEntity>> = questionDao.getAllQuestions()

    fun getAllAttempts(): Flow<List<TestAttemptEntity>> = attemptDao.getAllAttempts()

    suspend fun getAvailableCount(subjectId: String?, topicId: String?): Int = withContext(Dispatchers.IO) {
        questionDao.getAvailableQuestionCount(subjectId, topicId)
    }

    suspend fun getTestQuestions(
        subjectId: String?,
        topicId: String?,
        requestedCount: Int,
        randomize: Boolean = true
    ): List<QuestionEntity> = withContext(Dispatchers.IO) {
        val totalAvailable = questionDao.getAvailableQuestionCount(subjectId, topicId)
        val countToFetch = requestedCount.coerceAtMost(totalAvailable)
        if (countToFetch <= 0) return@withContext emptyList()

        if (randomize) {
            questionDao.getRandomQuestions(subjectId, topicId, countToFetch)
        } else {
            // First N questions
            questionDao.getRandomQuestions(subjectId, topicId, countToFetch)
        }
    }

    fun parseOptions(optionsJson: String): List<String> {
        return try {
            val arr = JSONArray(optionsJson)
            val list = mutableListOf<String>()
            for (i in 0 until arr.length()) {
                list.add(arr.getString(i))
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun recordTestAttempt(
        subjectId: String?,
        topicId: String?,
        totalQuestions: Int,
        userAnswers: Map<String, Int?>,
        questions: List<QuestionEntity>,
        timeSpentSeconds: Long
    ): TestSummary = withContext(Dispatchers.IO) {
        var correct = 0
        var wrong = 0
        var skipped = 0

        val reviews = mutableListOf<QuestionReviewItem>()

        questions.forEachIndexed { index, q ->
            val selected = userAnswers[q.id]
            val options = parseOptions(q.optionsJson)

            if (selected == null) {
                skipped++
            } else if (selected == q.correctAnswerIndex) {
                correct++
            } else {
                wrong++
            }

            reviews.add(
                QuestionReviewItem(
                    questionNumber = index + 1,
                    questionText = q.questionText,
                    options = options,
                    selectedIndex = selected,
                    correctIndex = q.correctAnswerIndex,
                    explanation = q.explanation
                )
            )
        }

        val attempted = correct + wrong
        val accuracy = if (attempted > 0) (correct.toFloat() / attempted.toFloat()) * 100f else 0f

        val attemptEntity = TestAttemptEntity(
            timestamp = System.currentTimeMillis(),
            subjectId = subjectId,
            topicId = topicId,
            totalQuestions = totalQuestions,
            correctCount = correct,
            wrongCount = wrong,
            skippedCount = skipped,
            timeSpentSeconds = timeSpentSeconds,
            accuracyPercentage = accuracy
        )
        attemptDao.insert(attemptEntity)

        TestSummary(
            totalQuestions = totalQuestions,
            attemptedCount = attempted,
            correctCount = correct,
            wrongCount = wrong,
            skippedCount = skipped,
            timeSpentSeconds = timeSpentSeconds,
            accuracyPercentage = accuracy,
            questionsReview = reviews
        )
    }

    suspend fun addQuestion(question: QuestionEntity) = withContext(Dispatchers.IO) {
        questionDao.insert(question)
    }

    suspend fun addQuestions(questions: List<QuestionEntity>) = withContext(Dispatchers.IO) {
        questionDao.insertAll(questions)
    }
}
