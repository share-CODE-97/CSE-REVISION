package com.example.ui.notes

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.entity.NoteEntity
import com.example.data.repository.StudyRepository
import com.example.domain.model.MindMapNode
import com.example.util.DateTimeUtils
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteDetailScreen(
    noteId: String,
    studyRepository: StudyRepository,
    onNavigateBack: () -> Unit,
    onNavigateToEdit: (String) -> Unit
) {
    val note by studyRepository.getNote(noteId).collectAsStateWithLifecycle(initialValue = null)
    val scope = rememberCoroutineScope()
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(note?.title ?: "Study Note") },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("note_detail_back")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    note?.let { currentNote ->
                        // Lock / Unlock toggle button (Requirement 9)
                        IconButton(
                            onClick = {
                                scope.launch {
                                    studyRepository.setNoteLock(currentNote.id, !currentNote.isLocked)
                                }
                            },
                            modifier = Modifier.testTag("note_lock_toggle")
                        ) {
                            if (currentNote.isLocked) {
                                Icon(Icons.Default.Lock, contentDescription = "Locked. Tap to unlock.", tint = MaterialTheme.colorScheme.primary)
                            } else {
                                Icon(Icons.Default.LockOpen, contentDescription = "Unlocked. Tap to lock.", tint = MaterialTheme.colorScheme.error)
                            }
                        }

                        // Edit button (only allowed when unlocked or informs user)
                        IconButton(
                            onClick = {
                                if (!currentNote.isLocked) {
                                    onNavigateToEdit(currentNote.id)
                                } else {
                                    // Unlock and edit
                                    scope.launch {
                                        studyRepository.setNoteLock(currentNote.id, false)
                                        onNavigateToEdit(currentNote.id)
                                    }
                                }
                            },
                            modifier = Modifier.testTag("note_edit_button")
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit Note")
                        }

                        // Delete button
                        IconButton(
                            onClick = { showDeleteConfirm = true },
                            modifier = Modifier.testTag("note_delete_button")
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete Note", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        if (note == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text("Note not found or deleted.", color = MaterialTheme.colorScheme.outline)
            }
        } else {
            val currentNote = note!!
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Status Banner
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (currentNote.isLocked)
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                            else
                                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (currentNote.isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                                    contentDescription = null,
                                    tint = if (currentNote.isLocked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (currentNote.isLocked) "🔒 Locked Note (Read-Only)" else "🔓 Unlocked (Editing Allowed)",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            TextButton(
                                onClick = {
                                    scope.launch {
                                        studyRepository.setNoteLock(currentNote.id, !currentNote.isLocked)
                                    }
                                }
                            ) {
                                Text(if (currentNote.isLocked) "Unlock" else "Lock")
                            }
                        }
                    }
                }

                // Note Meta
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = currentNote.noteType.replace('_', ' '),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                        Text(
                            text = "Last updated: ${DateTimeUtils.formatDateTime(currentNote.updatedAt)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }

                // Note Content Display (Mind Map vs Structured Text)
                item {
                    if (currentNote.noteType == "MIND_MAP") {
                        MindMapView(contentJson = currentNote.content)
                    } else {
                        StructuredTextView(content = currentNote.content)
                    }
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Delete Note?") },
            text = { Text("Are you sure you want to delete this study note?") },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            studyRepository.deleteNote(noteId)
                            showDeleteConfirm = false
                            onNavigateBack()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun StructuredTextView(content: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Split into paragraphs / lines for responsive layout
            val lines = content.lines()
            lines.forEach { line ->
                val trimmed = line.trim()
                when {
                    trimmed.startsWith("###") -> {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = trimmed.removePrefix("###").trim(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                    trimmed.startsWith("##") -> {
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = trimmed.removePrefix("##").trim(),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                    trimmed.startsWith("#") -> {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = trimmed.removePrefix("#").trim(),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    trimmed.startsWith("•") || trimmed.startsWith("-") || trimmed.startsWith("*") -> {
                        Row(modifier = Modifier.padding(vertical = 2.dp)) {
                            Text("• ", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            Text(
                                text = trimmed.removePrefix("•").removePrefix("-").removePrefix("*").trim(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    trimmed.isBlank() -> {
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                    else -> {
                        Text(
                            text = trimmed,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * 1.3f,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MindMapView(contentJson: String) {
    val rootNode = remember(contentJson) {
        parseMindMapJson(contentJson)
    }

    if (rootNode == null) {
        // Fallback to text view if not valid JSON
        StructuredTextView(content = contentJson)
    } else {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "MIND MAP HIERARCHICAL TREE",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(12.dp))
                MindMapNodeView(node = rootNode, level = 0)
            }
        }
    }
}

@Composable
fun MindMapNodeView(node: MindMapNode, level: Int) {
    var isExpanded by remember { mutableStateOf(node.isExpanded) }
    val indent = (level * 16).dp

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = indent, top = 4.dp, bottom = 4.dp)
            .animateContentSize()
    ) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = when (level) {
                0 -> MaterialTheme.colorScheme.primaryContainer
                1 -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            },
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    if (node.children.isNotEmpty()) {
                        isExpanded = !isExpanded
                    }
                }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = node.title,
                            style = if (level == 0) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
                            fontWeight = if (level <= 1) FontWeight.Bold else FontWeight.Medium
                        )
                        if (node.notes.isNotBlank()) {
                            Text(
                                text = node.notes,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                if (node.children.isNotEmpty()) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        AnimatedVisibility(visible = isExpanded) {
            Column {
                node.children.forEach { child ->
                    MindMapNodeView(node = child, level = level + 1)
                }
            }
        }
    }
}

fun parseMindMapJson(jsonString: String): MindMapNode? {
    return try {
        val root = JSONObject(jsonString)
        parseNode(root)
    } catch (e: Exception) {
        null
    }
}

fun parseNode(obj: JSONObject): MindMapNode {
    val id = obj.optString("id", "")
    val title = obj.optString("title", "Topic")
    val notes = obj.optString("notes", "")
    val childrenList = mutableListOf<MindMapNode>()
    if (obj.has("children")) {
        val arr = obj.getJSONArray("children")
        for (i in 0 until arr.length()) {
            val childObj = arr.getJSONObject(i)
            childrenList.add(parseNode(childObj))
        }
    }
    return MindMapNode(
        id = id,
        title = title,
        notes = notes,
        children = childrenList,
        isExpanded = true
    )
}
