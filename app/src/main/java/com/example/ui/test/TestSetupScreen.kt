package com.example.ui.test

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.repository.RevisionRepository
import com.example.data.repository.TestRepository

import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.OutlinedButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TestSetupScreen(
    revisionRepository: RevisionRepository,
    testRepository: TestRepository,
    onNavigateBack: () -> Unit,
    onNavigateToImporter: () -> Unit,
    onStartTest: (subjectId: String?, count: Int, isTimed: Boolean, timeLimitMinutes: Int) -> Unit
) {
    val subjects by revisionRepository.allSubjects.collectAsStateWithLifecycle(initialValue = emptyList())

    var selectedSubjectId by remember { mutableStateOf<String?>(null) }
    var availableQuestionCount by remember { mutableStateOf(0) }

    var selectedCountOption by remember { mutableStateOf(5) }
    var isTimed by remember { mutableStateOf(false) }
    var selectedMinutes by remember { mutableStateOf(10) }

    var subjectMenuExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(selectedSubjectId) {
        availableQuestionCount = testRepository.getAvailableCount(selectedSubjectId, null)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Test / Quiz Setup") },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("test_setup_back")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = onNavigateToImporter,
                        modifier = Modifier.testTag("test_setup_importer_action")
                    ) {
                        Icon(Icons.Default.UploadFile, contentDescription = "Import Questions")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Header card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Quiz,
                        contentDescription = null,
                        modifier = Modifier.size(36.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            "Revision Knowledge Test",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            "Evaluate and reinforce your learning with active recall quizzes.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            // Subject Filter
            Text(
                "1. SELECT SUBJECT",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            ExposedDropdownMenuBox(
                expanded = subjectMenuExpanded,
                onExpandedChange = { subjectMenuExpanded = !subjectMenuExpanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                val currentName = if (selectedSubjectId == null) "All Subjects" else subjects.find { it.id == selectedSubjectId }?.title ?: "Select"
                OutlinedTextField(
                    value = currentName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Subject Filter") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = subjectMenuExpanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth().testTag("test_subject_filter")
                )
                ExposedDropdownMenu(
                    expanded = subjectMenuExpanded,
                    onDismissRequest = { subjectMenuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("All Subjects") },
                        onClick = {
                            selectedSubjectId = null
                            subjectMenuExpanded = false
                        }
                    )
                    subjects.forEach { s ->
                        DropdownMenuItem(
                            text = { Text(s.title) },
                            onClick = {
                                selectedSubjectId = s.id
                                subjectMenuExpanded = false
                            }
                        )
                    }
                }
            }

            // Available Questions Banner & Data Validation (Requirement 43)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Questions in Bank: $availableQuestionCount",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        OutlinedButton(
                            onClick = onNavigateToImporter,
                            modifier = Modifier.testTag("test_setup_btn_import")
                        ) {
                            Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (availableQuestionCount == 0) "Import MCQs" else "Add More")
                        }
                    }

                    if (availableQuestionCount == 0) {
                        Text(
                            text = "No questions found for this subject filter. Tap 'Import MCQs' to load your 16 CSE .js/.json files.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            // Question Count Selection (Requirement 43)
            Text(
                "2. NUMBER OF QUESTIONS",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            val countOptions = listOf(5, 10, 15, 20, 50)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                countOptions.forEach { count ->
                    FilterChip(
                        selected = selectedCountOption == count,
                        onClick = { selectedCountOption = count },
                        label = { Text("$count") },
                        modifier = Modifier.testTag("count_chip_$count")
                    )
                }
            }

            // Strict Validation Note if requested count exceeds available questions
            if (selectedCountOption > availableQuestionCount && availableQuestionCount > 0) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "Only $availableQuestionCount questions are currently available in the database. The test will use the maximum possible test size of $availableQuestionCount questions without generating fake questions.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }

            // Timing options
            Text(
                "3. TIMER SETTINGS",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Timer, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Timed Quiz", fontWeight = FontWeight.Medium)
                }
                Switch(
                    checked = isTimed,
                    onCheckedChange = { isTimed = it },
                    modifier = Modifier.testTag("timer_switch")
                )
            }

            if (isTimed) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(5, 10, 15, 30).forEach { mins ->
                        FilterChip(
                            selected = selectedMinutes == mins,
                            onClick = { selectedMinutes = mins },
                            label = { Text("$mins Mins") }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Start Test Button
            val effectiveCount = minOf(selectedCountOption, availableQuestionCount)
            val canStart = availableQuestionCount > 0

            Button(
                onClick = {
                    onStartTest(selectedSubjectId, effectiveCount, isTimed, selectedMinutes)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("start_test_button"),
                enabled = canStart,
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (canStart) "START TEST ($effectiveCount Questions)" else "No Questions Available",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
