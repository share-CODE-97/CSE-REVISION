package com.example.ui.notes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.repository.StudyRepository
import com.example.domain.model.NoteType
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditorScreen(
    noteId: String?,
    subjectId: String,
    initialTopicId: String?,
    studyRepository: StudyRepository,
    onNavigateBack: () -> Unit
) {
    val topics by studyRepository.getTopicsForSubject(subjectId).collectAsStateWithLifecycle(initialValue = emptyList())
    val scope = rememberCoroutineScope()

    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var selectedNoteType by remember { mutableStateOf(NoteType.STANDARD) }
    var selectedTopicId by remember { mutableStateOf<String?>(initialTopicId) }
    var isLocked by remember { mutableStateOf(true) }
    var isLoaded by remember { mutableStateOf(false) }

    var typeMenuExpanded by remember { mutableStateOf(false) }
    var topicMenuExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(noteId) {
        if (!noteId.isNullOrBlank()) {
            val existing = studyRepository.getNoteOnce(noteId)
            if (existing != null) {
                title = existing.title
                content = existing.content
                selectedNoteType = NoteType.values().find { it.name == existing.noteType } ?: NoteType.STANDARD
                selectedTopicId = existing.parentTopicId
                isLocked = existing.isLocked
            }
        }
        isLoaded = true
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (noteId == null) "New Note" else "Edit Note") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Button(
                        onClick = {
                            if (title.isNotBlank()) {
                                scope.launch {
                                    studyRepository.saveNote(
                                        id = noteId,
                                        subjectId = subjectId,
                                        parentTopicId = selectedTopicId,
                                        title = title.trim(),
                                        content = content.trim(),
                                        noteType = selectedNoteType.name,
                                        isLocked = isLocked
                                    )
                                    onNavigateBack()
                                }
                            }
                        },
                        modifier = Modifier.padding(end = 8.dp).testTag("save_note_action")
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Save")
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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Title Input
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Note Title *") },
                placeholder = { Text("e.g. Newton's 2nd Law, SQL Joins") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("note_title_input")
            )

            // Note Type Selector
            ExposedDropdownMenuBox(
                expanded = typeMenuExpanded,
                onExpandedChange = { typeMenuExpanded = !typeMenuExpanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = selectedNoteType.displayName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Note Type") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeMenuExpanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = typeMenuExpanded,
                    onDismissRequest = { typeMenuExpanded = false }
                ) {
                    NoteType.values().forEach { type ->
                        DropdownMenuItem(
                            text = { Text(type.displayName) },
                            onClick = {
                                selectedNoteType = type
                                typeMenuExpanded = false
                            }
                        )
                    }
                }
            }

            // Topic Selector (Optional)
            ExposedDropdownMenuBox(
                expanded = topicMenuExpanded,
                onExpandedChange = { topicMenuExpanded = !topicMenuExpanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                val currentTopicName = topics.find { it.id == selectedTopicId }?.title ?: "None (General Note)"
                OutlinedTextField(
                    value = currentTopicName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Topic (Optional)") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = topicMenuExpanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = topicMenuExpanded,
                    onDismissRequest = { topicMenuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("None (General Note)") },
                        onClick = {
                            selectedTopicId = null
                            topicMenuExpanded = false
                        }
                    )
                    topics.forEach { topic ->
                        DropdownMenuItem(
                            text = { Text(topic.title) },
                            onClick = {
                                selectedTopicId = topic.id
                                topicMenuExpanded = false
                            }
                        )
                    }
                }
            }

            // Lock State Toggle on Save
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = isLocked,
                    onCheckedChange = { isLocked = it },
                    modifier = Modifier.testTag("lock_checkbox")
                )
                Spacer(modifier = Modifier.width(4.dp))
                Column {
                    Text("Lock note after saving 🔒", fontWeight = FontWeight.Medium)
                    Text(
                        "Prevents accidental editing while reviewing",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }

            // Note Content Editor
            OutlinedTextField(
                value = content,
                onValueChange = { content = it },
                label = { Text("Content *") },
                placeholder = {
                    Text(
                        if (selectedNoteType == NoteType.MIND_MAP)
                            "Enter JSON mind map structure or bullet points..."
                        else
                            "Write your study notes, formulas, definitions, key points..."
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(320.dp)
                    .testTag("note_content_input"),
                maxLines = 40
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
