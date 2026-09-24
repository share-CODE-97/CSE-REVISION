package com.example.domain.model

enum class NoteType(val displayName: String) {
    STANDARD("Standard Note"),
    MIND_MAP("Mind Map"),
    CHEAT_SHEET("Cheat Sheet / High Yield"),
    SIMPLIFIED("Simplified / ELI5"),
    CUSTOM("Custom")
}

data class MindMapNode(
    val id: String,
    val title: String,
    val notes: String = "",
    val children: List<MindMapNode> = emptyList(),
    val isExpanded: Boolean = true
)

sealed interface RevisionScheduleChoice {
    object None : RevisionScheduleChoice
    object Plus24Hours : RevisionScheduleChoice
    object Plus48Hours : RevisionScheduleChoice
    data class Custom(val targetTimestamp: Long) : RevisionScheduleChoice
}

enum class TestMode(val displayName: String, val durationMinutes: Int?) {
    UNTIMED("Untimed", null),
    TIMED_5("5 Minutes", 5),
    TIMED_10("10 Minutes", 10),
    TIMED_15("15 Minutes", 15),
    TIMED_30("30 Minutes", 30)
}

data class TestConfiguration(
    val subjectId: String? = null,
    val topicId: String? = null,
    val questionCount: Int = 10,
    val mode: TestMode = TestMode.UNTIMED,
    val randomize: Boolean = true
)

data class QuestionOption(
    val index: Int,
    val text: String
)

data class QuestionAnswerState(
    val questionId: String,
    val selectedOptionIndex: Int?,
    val isCorrect: Boolean,
    val explanation: String
)

data class TestSummary(
    val totalQuestions: Int,
    val attemptedCount: Int,
    val correctCount: Int,
    val wrongCount: Int,
    val skippedCount: Int,
    val timeSpentSeconds: Long,
    val accuracyPercentage: Float,
    val questionsReview: List<QuestionReviewItem>
)

data class QuestionReviewItem(
    val questionNumber: Int,
    val questionText: String,
    val options: List<String>,
    val selectedIndex: Int?,
    val correctIndex: Int,
    val explanation: String
)

enum class AiGenerationMode(val label: String, val description: String) {
    MIND_MAP("Mind Map", "Hierarchical tree of core concepts, branches, and details"),
    SIMPLIFY("Simplify / ELI5", "Clear explanations with intuitive analogies and everyday examples"),
    CHEAT_SHEET("Cheat Sheet / High Yield", "Formulas, definitions, key dates, facts, and exam pitfalls"),
    CUSTOM("Custom Prompt", "Free-form structured study note from your prompt")
}

data class DashboardOverviewMetrics(
    val totalRevisions: Int,
    val completedToday: Int,
    val overdue: Int,
    val currentStreak: Int
)
