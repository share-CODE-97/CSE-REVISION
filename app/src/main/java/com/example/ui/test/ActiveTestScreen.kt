package com.example.ui.test

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.entity.QuestionEntity
import com.example.data.repository.TestRepository
import com.example.domain.model.TestSummary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONArray

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveTestScreen(
    subjectId: String?,
    count: Int,
    isTimed: Boolean,
    timeLimitMinutes: Int,
    testRepository: TestRepository,
    onNavigateBack: () -> Unit,
    onFinishTest: (TestSummary) -> Unit
) {
    val scope = rememberCoroutineScope()

    var questions by remember { mutableStateOf<List<QuestionEntity>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var currentIndex by remember { mutableStateOf(0) }

    // Map of questionId -> selectedOptionIndex (0..3)
    val userAnswers = remember { mutableStateMapOf<String, Int>() }

    // Timer state
    var remainingSeconds by remember { mutableStateOf(timeLimitMinutes * 60) }
    var showQuitDialog by remember { mutableStateOf(false) }
    var showSubmitDialog by remember { mutableStateOf(false) }

    val startTimeMillis = remember { System.currentTimeMillis() }

    LaunchedEffect(Unit) {
        questions = testRepository.getTestQuestions(
            subjectId = subjectId,
            topicId = null,
            requestedCount = count,
            randomize = true
        )
        isLoading = false
    }

    // Timer countdown
    if (isTimed) {
        LaunchedEffect(remainingSeconds) {
            if (remainingSeconds > 0) {
                delay(1000)
                remainingSeconds--
            } else if (!isLoading && questions.isNotEmpty()) {
                // Time up! Automatically submit
                finishTest(questions, userAnswers, startTimeMillis, testRepository, scope, onFinishTest)
            }
        }
    }

    BackHandler {
        showQuitDialog = true
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (questions.isNotEmpty()) "Question ${currentIndex + 1} of ${questions.size}"
                        else "Revision Test"
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { showQuitDialog = true }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quit Quiz")
                    }
                },
                actions = {
                    if (isTimed) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (remainingSeconds < 60) MaterialTheme.colorScheme.errorContainer
                            else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.HourglassTop, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                val mins = remainingSeconds / 60
                                val secs = remainingSeconds % 60
                                Text(
                                    text = String.format("%02d:%02d", mins, secs),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (questions.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding).padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("No questions found for the selected subject.", style = MaterialTheme.typography.titleMedium)
            }
        } else {
            val currentQuestion = questions[currentIndex]
            val options = remember(currentQuestion) {
                testRepository.parseOptions(currentQuestion.optionsJson)
            }
            val selectedAnswer = userAnswers[currentQuestion.id]

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Progress Bar
                LinearProgressIndicator(
                    progress = { (currentIndex + 1).toFloat() / questions.size.toFloat() },
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp))
                )

                // Question Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "QUESTION ${currentIndex + 1}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    text = currentQuestion.difficulty,
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = currentQuestion.questionText,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            lineHeight = MaterialTheme.typography.titleMedium.lineHeight * 1.25f
                        )
                    }
                }

                // Options List
                Text(
                    text = "SELECT THE BEST ANSWER:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                    fontWeight = FontWeight.Bold
                )

                options.forEachIndexed { optIndex, optionText ->
                    val isSelected = selectedAnswer == optIndex
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                userAnswers[currentQuestion.id] = optIndex
                            }
                            .testTag("option_${currentIndex}_$optIndex"),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                            else MaterialTheme.colorScheme.surface
                        ),
                        shape = RoundedCornerShape(10.dp),
                        border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                        else androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = { userAnswers[currentQuestion.id] = optIndex }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            val prefix = when (optIndex) {
                                0 -> "A. "
                                1 -> "B. "
                                2 -> "C. "
                                else -> "D. "
                            }
                            Text(
                                text = "$prefix$optionText",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Navigation Buttons (Prev, Next, Submit)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    OutlinedButton(
                        onClick = {
                            if (currentIndex > 0) currentIndex--
                        },
                        enabled = currentIndex > 0
                    ) {
                        Text("Previous")
                    }

                    if (currentIndex < questions.size - 1) {
                        Button(
                            onClick = { currentIndex++ },
                            modifier = Modifier.testTag("next_question_button")
                        ) {
                            Text("Next")
                        }
                    } else {
                        Button(
                            onClick = { showSubmitDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier.testTag("submit_test_button")
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Finish Test")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Submit Confirmation Dialog
    if (showSubmitDialog) {
        val answeredCount = userAnswers.size
        val totalCount = questions.size
        AlertDialog(
            onDismissRequest = { showSubmitDialog = false },
            title = { Text("Submit Quiz?") },
            text = {
                Text("You have answered $answeredCount of $totalCount questions. Are you ready to see your results?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSubmitDialog = false
                        finishTest(questions, userAnswers, startTimeMillis, testRepository, scope, onFinishTest)
                    }
                ) {
                    Text("Submit Now")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSubmitDialog = false }) {
                    Text("Review More")
                }
            }
        )
    }

    // Quit Dialog
    if (showQuitDialog) {
        AlertDialog(
            onDismissRequest = { showQuitDialog = false },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Leave Quiz?") },
            text = { Text("Your current test progress will be discarded.") },
            confirmButton = {
                Button(
                    onClick = {
                        showQuitDialog = false
                        onNavigateBack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Quit")
                }
            },
            dismissButton = {
                TextButton(onClick = { showQuitDialog = false }) {
                    Text("Continue Quiz")
                }
            }
        )
    }
}

private fun finishTest(
    questions: List<QuestionEntity>,
    userAnswers: Map<String, Int>,
    startTimeMillis: Long,
    testRepository: TestRepository,
    scope: kotlinx.coroutines.CoroutineScope,
    onFinishTest: (TestSummary) -> Unit
) {
    val durationSecs = (System.currentTimeMillis() - startTimeMillis) / 1000
    val subjectId = questions.firstOrNull()?.subjectId

    scope.launch {
        val summary = testRepository.recordTestAttempt(
            subjectId = subjectId,
            topicId = null,
            totalQuestions = questions.size,
            userAnswers = userAnswers,
            questions = questions,
            timeSpentSeconds = durationSecs
        )
        onFinishTest(summary)
    }
}
