package com.example.data.importer

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.example.data.database.AppDatabase
import com.example.data.entity.QuestionEntity
import com.example.data.entity.RevisionSubjectEntity
import com.example.data.entity.TopicEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.Locale
import java.util.UUID

data class ImportProgress(
    val isRunning: Boolean = false,
    val currentFileIndex: Int = 0,
    val totalFiles: Int = 0,
    val currentFileName: String = "",
    val stage: String = "", // "Reading", "Parsing", "Writing to Database", "Completed", "Error"
    val progressPercent: Float = 0f,
    val questionsProcessed: Int = 0,
    val totalQuestionsFound: Int = 0,
    val statusMessage: String = "",
    val resultSummary: ImportResultSummary? = null,
    val errorMessage: String? = null
)

data class ImportResultSummary(
    val totalFilesProcessed: Int,
    val totalQuestionsFound: Int,
    val totalQuestionsImported: Int,
    val newSubjectsCreated: Int,
    val newTopicsCreated: Int,
    val subjectBreakdown: Map<String, Int>,
    val durationMs: Long
)

data class SubjectStats(
    val subjectId: String,
    val subjectTitle: String,
    val questionCount: Int
)

data class QuestionBankStats(
    val totalQuestions: Int,
    val subjectsWithQuestions: List<SubjectStats>
)

class QuestionImportRepository(private val db: AppDatabase) {

    private val _progress = MutableStateFlow(ImportProgress())
    val progress: StateFlow<ImportProgress> = _progress.asStateFlow()

    /**
     * Imports questions from a raw String content (e.g. pasted code or loaded file text).
     */
    suspend fun importFromText(
        rawContent: String,
        fallbackSubject: String = "General"
    ): ImportResultSummary = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        _progress.value = ImportProgress(
            isRunning = true,
            stage = "Parsing Content",
            statusMessage = "Parsing questions from text...",
            progressPercent = 0.1f
        )

        val parsedQuestions = QuestionParser.parseQuestions(rawContent, fallbackSubject)
        if (parsedQuestions.isEmpty()) {
            val summary = ImportResultSummary(
                totalFilesProcessed = 1,
                totalQuestionsFound = 0,
                totalQuestionsImported = 0,
                newSubjectsCreated = 0,
                newTopicsCreated = 0,
                subjectBreakdown = emptyMap(),
                durationMs = System.currentTimeMillis() - startTime
            )
            _progress.value = ImportProgress(
                isRunning = false,
                stage = "Completed",
                resultSummary = summary,
                statusMessage = "No valid questions found in provided content."
            )
            return@withContext summary
        }

        val result = saveParsedQuestions(
            questions = parsedQuestions,
            filesProcessed = 1,
            startTime = startTime
        )

        _progress.value = ImportProgress(
            isRunning = false,
            stage = "Completed",
            progressPercent = 1f,
            questionsProcessed = result.totalQuestionsImported,
            totalQuestionsFound = result.totalQuestionsFound,
            statusMessage = "Successfully imported ${result.totalQuestionsImported} questions!",
            resultSummary = result
        )
        result
    }

    /**
     * Imports questions from multiple files selected via Android Storage Access Framework (SAF).
     * Ideal for 16 different .js files with 20,000+ MCQs!
     */
    suspend fun importFromUris(
        context: Context,
        uris: List<Uri>,
        defaultFallbackSubject: String = "General"
    ): ImportResultSummary = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        val allParsedQuestions = mutableListOf<ParsedQuestion>()
        var filesProcessed = 0

        _progress.value = ImportProgress(
            isRunning = true,
            currentFileIndex = 0,
            totalFiles = uris.size,
            stage = "Reading Files",
            progressPercent = 0.05f,
            statusMessage = "Starting import of ${uris.size} file(s)..."
        )

        for ((idx, uri) in uris.withIndex()) {
            val fileName = getFileName(context, uri) ?: "File_${idx + 1}.js"
            val fallbackFromFileName = fileName
                .substringBeforeLast('.')
                .replace('_', ' ')
                .replace('-', ' ')
                .trim()
                .ifEmpty { defaultFallbackSubject }

            _progress.value = _progress.value.copy(
                currentFileIndex = idx + 1,
                totalFiles = uris.size,
                currentFileName = fileName,
                stage = "Parsing $fileName",
                progressPercent = 0.05f + (idx.toFloat() / uris.size.toFloat()) * 0.35f,
                statusMessage = "Reading $fileName (${idx + 1}/${uris.size})..."
            )

            val content = readFileContent(context, uri)
            if (content.isNotEmpty()) {
                val questionsFromFile = QuestionParser.parseQuestions(content, fallbackFromFileName)
                allParsedQuestions.addAll(questionsFromFile)
                filesProcessed++
            }
        }

        if (allParsedQuestions.isEmpty()) {
            val summary = ImportResultSummary(
                totalFilesProcessed = filesProcessed,
                totalQuestionsFound = 0,
                totalQuestionsImported = 0,
                newSubjectsCreated = 0,
                newTopicsCreated = 0,
                subjectBreakdown = emptyMap(),
                durationMs = System.currentTimeMillis() - startTime
            )
            _progress.value = ImportProgress(
                isRunning = false,
                stage = "Completed",
                resultSummary = summary,
                statusMessage = "No valid questions could be extracted from selected files."
            )
            return@withContext summary
        }

        val result = saveParsedQuestions(
            questions = allParsedQuestions,
            filesProcessed = filesProcessed,
            startTime = startTime
        )

        _progress.value = ImportProgress(
            isRunning = false,
            stage = "Completed",
            progressPercent = 1f,
            questionsProcessed = result.totalQuestionsImported,
            totalQuestionsFound = result.totalQuestionsFound,
            statusMessage = "Successfully imported ${result.totalQuestionsImported} questions from $filesProcessed files!",
            resultSummary = result
        )

        result
    }

    private suspend fun saveParsedQuestions(
        questions: List<ParsedQuestion>,
        filesProcessed: Int,
        startTime: Long
    ): ImportResultSummary = withContext(Dispatchers.IO) {
        val totalQuestions = questions.size
        _progress.value = _progress.value.copy(
            stage = "Resolving Subjects & Topics",
            totalQuestionsFound = totalQuestions,
            progressPercent = 0.45f,
            statusMessage = "Resolving subjects and categories for $totalQuestions questions..."
        )

        // 1. Fetch existing subjects and topics
        val existingSubjects = db.revisionSubjectDao().getAllSubjectsOnce().toMutableList()
        val existingTopics = db.topicDao().getAllTopicsOnce().toMutableList()

        val subjectMap = mutableMapOf<String, RevisionSubjectEntity>()
        existingSubjects.forEach { s ->
            subjectMap[s.title.lowercase(Locale.ROOT).trim()] = s
            subjectMap[s.id.lowercase(Locale.ROOT).trim()] = s
        }

        val topicMap = mutableMapOf<String, TopicEntity>() // Key: "subjectId::topicTitle"
        existingTopics.forEach { t ->
            topicMap["${t.subjectId}::${t.title.lowercase(Locale.ROOT).trim()}"] = t
        }

        var newSubjectsCreated = 0
        var newTopicsCreated = 0
        val subjectCounts = mutableMapOf<String, Int>()

        // 2. Resolve or create subjects and topics
        val questionsToInsert = mutableListOf<QuestionEntity>()

        for ((index, q) in questions.withIndex()) {
            val subKey = q.subjectTitle.lowercase(Locale.ROOT).trim()
            var subject = subjectMap[subKey]

            if (subject == null) {
                val cleanSlug = subKey.replace(Regex("[^a-z0-9]"), "_").take(24).trim('_')
                val newSubjectId = "subj_${cleanSlug}_${UUID.randomUUID().toString().take(6)}"
                val newSubject = RevisionSubjectEntity(
                    id = newSubjectId,
                    title = q.subjectTitle.trim(),
                    sortOrder = existingSubjects.size + newSubjectsCreated + 1
                )
                db.revisionSubjectDao().insert(newSubject)
                subject = newSubject
                subjectMap[subKey] = newSubject
                subjectMap[newSubject.id.lowercase(Locale.ROOT)] = newSubject
                existingSubjects.add(newSubject)
                newSubjectsCreated++
            }

            // Subtopic resolution
            var topicId: String? = null
            if (!q.subtopicTitle.isNullOrBlank()) {
                val topicKey = "${subject.id}::${q.subtopicTitle.lowercase(Locale.ROOT).trim()}"
                var topic = topicMap[topicKey]
                if (topic == null) {
                    val cleanTopicSlug = q.subtopicTitle.lowercase(Locale.ROOT).replace(Regex("[^a-z0-9]"), "_").take(24).trim('_')
                    val newTopicId = "top_${cleanTopicSlug}_${UUID.randomUUID().toString().take(6)}"
                    val newTopic = TopicEntity(
                        id = newTopicId,
                        subjectId = subject.id,
                        title = q.subtopicTitle.trim(),
                        sortOrder = existingTopics.size + newTopicsCreated + 1
                    )
                    db.topicDao().insert(newTopic)
                    topic = newTopic
                    topicMap[topicKey] = newTopic
                    existingTopics.add(newTopic)
                    newTopicsCreated++
                }
                topicId = topic.id
            }

            val questionEntity = QuestionEntity(
                id = q.id,
                subjectId = subject.id,
                topicId = topicId,
                questionText = q.questionText,
                optionsJson = JSONArray(q.options).toString(),
                correctAnswerIndex = q.correctAnswerIndex,
                explanation = q.explanation,
                difficulty = q.difficulty
            )
            questionsToInsert.add(questionEntity)

            subjectCounts[subject.title] = (subjectCounts[subject.title] ?: 0) + 1

            if (index % 1000 == 0) {
                _progress.value = _progress.value.copy(
                    progressPercent = 0.45f + (index.toFloat() / totalQuestions.toFloat()) * 0.15f,
                    statusMessage = "Mapped $index of $totalQuestions questions..."
                )
            }
        }

        // 3. Batch insert in Room transactions of 500
        val chunkSize = 500
        val chunks = questionsToInsert.chunked(chunkSize)
        val totalChunks = chunks.size

        for ((cIdx, chunk) in chunks.withIndex()) {
            db.withTransaction {
                db.questionDao().insertAll(chunk)
            }

            val currentProcessed = minOf((cIdx + 1) * chunkSize, questionsToInsert.size)
            val writeProgress = 0.6f + ((cIdx + 1).toFloat() / totalChunks.toFloat()) * 0.38f

            _progress.value = _progress.value.copy(
                stage = "Writing to Database",
                progressPercent = writeProgress,
                questionsProcessed = currentProcessed,
                totalQuestionsFound = totalQuestions,
                statusMessage = "Saving batch ${cIdx + 1} of $totalChunks ($currentProcessed / $totalQuestions)..."
            )
        }

        ImportResultSummary(
            totalFilesProcessed = filesProcessed,
            totalQuestionsFound = totalQuestions,
            totalQuestionsImported = questionsToInsert.size,
            newSubjectsCreated = newSubjectsCreated,
            newTopicsCreated = newTopicsCreated,
            subjectBreakdown = subjectCounts,
            durationMs = System.currentTimeMillis() - startTime
        )
    }

    suspend fun getQuestionBankStats(): QuestionBankStats = withContext(Dispatchers.IO) {
        val total = db.questionDao().getTotalQuestionCount()
        val groupedCounts = db.questionDao().getQuestionCountsGroupedBySubject()
        val allSubjects = db.revisionSubjectDao().getAllSubjectsOnce()
        val subjectTitleMap = allSubjects.associate { it.id to it.title }

        val list = groupedCounts.map {
            SubjectStats(
                subjectId = it.subjectId,
                subjectTitle = subjectTitleMap[it.subjectId] ?: it.subjectId,
                questionCount = it.count
            )
        }.sortedByDescending { it.questionCount }

        QuestionBankStats(
            totalQuestions = total,
            subjectsWithQuestions = list
        )
    }

    suspend fun clearAllQuestions(): Unit = withContext(Dispatchers.IO) {
        db.questionDao().clearAll()
    }

    suspend fun deleteQuestionsForSubject(subjectId: String): Unit = withContext(Dispatchers.IO) {
        db.questionDao().deleteBySubject(subjectId)
    }

    fun resetProgress() {
        _progress.value = ImportProgress()
    }

    private fun readFileContent(context: Context, uri: Uri): String {
        return try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BufferedReader(InputStreamReader(stream)).use { reader ->
                    reader.readText()
                }
            } ?: ""
        } catch (e: Exception) {
            ""
        }
    }

    private fun getFileName(context: Context, uri: Uri): String? {
        var name: String? = null
        try {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val index = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (index != -1) {
                        name = it.getString(index)
                    }
                }
            }
        } catch (_: Exception) {
            name = uri.lastPathSegment
        }
        return name
    }
}
