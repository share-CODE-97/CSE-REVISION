package com.example.ui.topics

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Note
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
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
import com.example.data.entity.NoteEntity
import com.example.data.repository.RevisionRepository
import com.example.data.repository.StudyRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopicListScreen(
    subjectId: String,
    revisionRepository: RevisionRepository,
    studyRepository: StudyRepository,
    onNavigateBack: () -> Unit,
    onNavigateToNote: (String) -> Unit,
    onNavigateToCreateNote: (String, String?) -> Unit // subjectId, topicId
) {
    val subject by revisionRepository.getSubject(subjectId).collectAsStateWithLifecycle(initialValue = null)
    val topics by studyRepository.getTopicsForSubject(subjectId).collectAsStateWithLifecycle(initialValue = emptyList())
    val notes by studyRepository.getNotesForSubject(subjectId).collectAsStateWithLifecycle(initialValue = emptyList())
    val scope = rememberCoroutineScope()

    var showAddTopicDialog by remember { mutableStateOf(false) }
    var newTopicTitle by remember { mutableStateOf("") }
    var selectedParentTopicId by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(subject?.title ?: "Topics & Notes") },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("topics_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            newTopicTitle = ""
                            selectedParentTopicId = null
                            showAddTopicDialog = true
                        },
                        modifier = Modifier.testTag("add_topic_action")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Topic")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Action button to add note directly
            item {
                Button(
                    onClick = { onNavigateToCreateNote(subjectId, null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("add_note_button"),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Note, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("+ Add Note to ${subject?.title ?: "Subject"}")
                }
            }

            // Topics and their notes
            if (topics.isEmpty() && notes.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "No topics or notes created yet.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(onClick = { showAddTopicDialog = true }) {
                                Text("Create First Topic")
                            }
                        }
                    }
                }
            } else {
                // Group notes by topic
                topics.forEach { topic ->
                    val topicNotes = notes.filter { it.parentTopicId == topic.id }
                    item(key = "topic_${topic.id}") {
                        TopicHeaderCard(
                            topicTitle = topic.title,
                            noteCount = topicNotes.size,
                            onAddNoteToTopic = { onNavigateToCreateNote(subjectId, topic.id) }
                        )
                    }

                    if (topicNotes.isNotEmpty()) {
                        items(topicNotes, key = { "note_${it.id}" }) { note ->
                            NoteRowItem(
                                note = note,
                                onOpen = { onNavigateToNote(note.id) }
                            )
                        }
                    }
                }

                // Notes not attached to a specific topic
                val orphanNotes = notes.filter { it.parentTopicId == null }
                if (orphanNotes.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "GENERAL NOTES",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    items(orphanNotes, key = { "gen_note_${it.id}" }) { note ->
                        NoteRowItem(
                            note = note,
                            onOpen = { onNavigateToNote(note.id) }
                        )
                    }
                }
            }
        }
    }

    if (showAddTopicDialog) {
        AlertDialog(
            onDismissRequest = { showAddTopicDialog = false },
            title = { Text("+ Add Topic") },
            text = {
                OutlinedTextField(
                    value = newTopicTitle,
                    onValueChange = { newTopicTitle = it },
                    label = { Text("Topic Title") },
                    placeholder = { Text("e.g. Operating System, Normalization") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("add_topic_input")
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newTopicTitle.isNotBlank()) {
                            scope.launch {
                                studyRepository.addTopic(subjectId, newTopicTitle.trim(), selectedParentTopicId)
                                showAddTopicDialog = false
                            }
                        }
                    },
                    modifier = Modifier.testTag("save_topic_button")
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddTopicDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun TopicHeaderCard(
    topicTitle: String,
    noteCount: Int,
    onAddNoteToTopic: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Icon(
                    Icons.Default.Folder,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = topicTitle,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "($noteCount)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
            TextButton(
                onClick = onAddNoteToTopic,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(2.dp))
                Text("+ Note", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
fun NoteRowItem(
    note: NoteEntity,
    onOpen: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen)
            .testTag("note_row_${note.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = note.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (note.isLocked) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = "Locked",
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${note.noteType.replace('_', ' ')} • ${note.content.take(60)}...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Text(
                    text = if (note.isLocked) "🔒 Locked" else "🔓 Unlocked",
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
        }
    }
}
