package com.example.ui.dashboard

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.entity.RevisionHistoryEntity
import com.example.data.entity.RevisionSubjectEntity
import com.example.domain.model.DashboardOverviewMetrics
import com.example.util.DateTimeUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onNavigateToRevision: (String) -> Unit,
    onNavigateToTest: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onOpenComments: (String) -> Unit,
    onOpenHistory: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Collapsible section states per requirements
    var isTodayExpanded by remember { mutableStateOf(true) }
    var isOverdueExpanded by remember { mutableStateOf(false) }
    var isNext6HoursExpanded by remember { mutableStateOf(false) }
    var isOverviewExpanded by remember { mutableStateOf(true) }
    var isAllRevisionsExpanded by remember { mutableStateOf(true) }
    var isHistoryExpanded by remember { mutableStateOf(false) }
    var isNoMoreExpanded by remember { mutableStateOf(false) }

    // Dialog state for Rename / Delete
    var renameSubjectTarget by remember { mutableStateOf<RevisionSubjectEntity?>(null) }
    var renameText by remember { mutableStateOf("") }
    var deleteSubjectTarget by remember { mutableStateOf<RevisionSubjectEntity?>(null) }
    var infoSubjectTarget by remember { mutableStateOf<RevisionSubjectEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Revision Tracker",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                actions = {
                    // TEST Button immediately before Settings Button (Requirement 40)
                    FilledTonalButton(
                        onClick = onNavigateToTest,
                        modifier = Modifier
                            .padding(end = 4.dp)
                            .testTag("top_bar_test_button"),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            Icons.Default.Quiz,
                            contentDescription = "Test / Quiz",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "TEST",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }

                    // Settings Button
                    IconButton(
                        onClick = onNavigateToSettings,
                        modifier = Modifier.testTag("top_bar_settings_button")
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. TODAY SECTION
            item {
                CollapsibleSectionHeader(
                    title = "TODAY",
                    badgeCount = uiState.todaySubjects.size,
                    badgeColor = MaterialTheme.colorScheme.primary,
                    isExpanded = isTodayExpanded,
                    onToggle = { isTodayExpanded = !isTodayExpanded },
                    testTag = "section_today"
                )
            }
            if (isTodayExpanded) {
                if (uiState.todaySubjects.isEmpty()) {
                    item {
                        EmptySectionCard(message = "No revisions due today. Great job!")
                    }
                } else {
                    items(uiState.todaySubjects, key = { "today_${it.id}" }) { subject ->
                        SubjectCard(
                            subject = subject,
                            currentTime = uiState.currentTimeMillis,
                            onOpen = { onNavigateToRevision(subject.id) },
                            onRename = {
                                renameSubjectTarget = subject
                                renameText = subject.title
                            },
                            onDelete = { deleteSubjectTarget = subject },
                            onComments = { onOpenComments(subject.id) },
                            onHistory = { onOpenHistory(subject.id) },
                            onInfo = { infoSubjectTarget = subject }
                        )
                    }
                }
            }

            // 2. OVERDUE SECTION
            item {
                CollapsibleSectionHeader(
                    title = "OVERDUE",
                    badgeCount = uiState.overdueSubjects.size,
                    badgeColor = MaterialTheme.colorScheme.error,
                    isExpanded = isOverdueExpanded,
                    onToggle = { isOverdueExpanded = !isOverdueExpanded },
                    testTag = "section_overdue"
                )
            }
            if (isOverdueExpanded) {
                if (uiState.overdueSubjects.isEmpty()) {
                    item {
                        EmptySectionCard(message = "Zero overdue revisions! You're on track.")
                    }
                } else {
                    items(uiState.overdueSubjects, key = { "overdue_${it.id}" }) { subject ->
                        SubjectCard(
                            subject = subject,
                            currentTime = uiState.currentTimeMillis,
                            isOverdue = true,
                            onOpen = { onNavigateToRevision(subject.id) },
                            onRename = {
                                renameSubjectTarget = subject
                                renameText = subject.title
                            },
                            onDelete = { deleteSubjectTarget = subject },
                            onComments = { onOpenComments(subject.id) },
                            onHistory = { onOpenHistory(subject.id) },
                            onInfo = { infoSubjectTarget = subject }
                        )
                    }
                }
            }

            // 3. NEXT 6 HOURS SECTION
            item {
                CollapsibleSectionHeader(
                    title = "NEXT 6 HOURS",
                    badgeCount = uiState.next6HoursSubjects.size,
                    badgeColor = MaterialTheme.colorScheme.tertiary,
                    isExpanded = isNext6HoursExpanded,
                    onToggle = { isNext6HoursExpanded = !isNext6HoursExpanded },
                    testTag = "section_next_6_hours"
                )
            }
            if (isNext6HoursExpanded) {
                if (uiState.next6HoursSubjects.isEmpty()) {
                    item {
                        EmptySectionCard(message = "No revisions scheduled in the next 6 hours.")
                    }
                } else {
                    items(uiState.next6HoursSubjects, key = { "next6_${it.id}" }) { subject ->
                        SubjectCard(
                            subject = subject,
                            currentTime = uiState.currentTimeMillis,
                            onOpen = { onNavigateToRevision(subject.id) },
                            onRename = {
                                renameSubjectTarget = subject
                                renameText = subject.title
                            },
                            onDelete = { deleteSubjectTarget = subject },
                            onComments = { onOpenComments(subject.id) },
                            onHistory = { onOpenHistory(subject.id) },
                            onInfo = { infoSubjectTarget = subject }
                        )
                    }
                }
            }

            // 4. OVERVIEW / PROGRESS SECTION (Preserved before All Revisions)
            item {
                CollapsibleSectionHeader(
                    title = "OVERVIEW / PROGRESS",
                    badgeCount = null,
                    isExpanded = isOverviewExpanded,
                    onToggle = { isOverviewExpanded = !isOverviewExpanded },
                    testTag = "section_overview"
                )
            }
            if (isOverviewExpanded) {
                item {
                    OverviewProgressGrid(metrics = uiState.metrics)
                }
            }

            // 5. ALL REVISIONS SECTION (Preserved before History)
            item {
                CollapsibleSectionHeader(
                    title = "ALL REVISIONS",
                    badgeCount = uiState.allActiveSubjects.size,
                    badgeColor = MaterialTheme.colorScheme.secondary,
                    isExpanded = isAllRevisionsExpanded,
                    onToggle = { isAllRevisionsExpanded = !isAllRevisionsExpanded },
                    testTag = "section_all_revisions"
                )
            }
            if (isAllRevisionsExpanded) {
                if (uiState.allActiveSubjects.isEmpty()) {
                    item {
                        EmptySectionCard(message = "No active subjects. Add subjects in Settings!")
                    }
                } else {
                    items(uiState.allActiveSubjects, key = { "all_${it.id}" }) { subject ->
                        SubjectCard(
                            subject = subject,
                            currentTime = uiState.currentTimeMillis,
                            onOpen = { onNavigateToRevision(subject.id) },
                            onRename = {
                                renameSubjectTarget = subject
                                renameText = subject.title
                            },
                            onDelete = { deleteSubjectTarget = subject },
                            onComments = { onOpenComments(subject.id) },
                            onHistory = { onOpenHistory(subject.id) },
                            onInfo = { infoSubjectTarget = subject }
                        )
                    }
                }
            }

            // 6. HISTORY SECTION
            item {
                CollapsibleSectionHeader(
                    title = "HISTORY",
                    badgeCount = uiState.globalHistory.size,
                    isExpanded = isHistoryExpanded,
                    onToggle = { isHistoryExpanded = !isHistoryExpanded },
                    testTag = "section_history"
                )
            }
            if (isHistoryExpanded) {
                if (uiState.globalHistory.isEmpty()) {
                    item {
                        EmptySectionCard(message = "No revisions completed yet. Finish a revision to see history.")
                    }
                } else {
                    items(uiState.globalHistory, key = { "hist_${it.id}" }) { history ->
                        HistoryItemRow(history = history)
                    }
                }
            }

            // 7. NO MORE REVISION NEEDED SECTION
            item {
                CollapsibleSectionHeader(
                    title = "NO MORE REVISION NEEDED",
                    badgeCount = uiState.noMoreSubjects.size,
                    isExpanded = isNoMoreExpanded,
                    onToggle = { isNoMoreExpanded = !isNoMoreExpanded },
                    testTag = "section_no_more"
                )
            }
            if (isNoMoreExpanded) {
                if (uiState.noMoreSubjects.isEmpty()) {
                    item {
                        EmptySectionCard(message = "No completed subjects marked as 'No more revision needed'.")
                    }
                } else {
                    items(uiState.noMoreSubjects, key = { "nomore_${it.id}" }) { subject ->
                        SubjectCard(
                            subject = subject,
                            currentTime = uiState.currentTimeMillis,
                            onOpen = { onNavigateToRevision(subject.id) },
                            onRename = {
                                renameSubjectTarget = subject
                                renameText = subject.title
                            },
                            onDelete = { deleteSubjectTarget = subject },
                            onComments = { onOpenComments(subject.id) },
                            onHistory = { onOpenHistory(subject.id) },
                            onInfo = { infoSubjectTarget = subject }
                        )
                    }
                }
            }
        }
    }

    // Rename Dialog
    renameSubjectTarget?.let { subject ->
        AlertDialog(
            onDismissRequest = { renameSubjectTarget = null },
            title = { Text("Rename Subject") },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    label = { Text("Subject Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("rename_input")
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (renameText.isNotBlank()) {
                            viewModel.renameSubject(subject.id, renameText.trim())
                            renameSubjectTarget = null
                        }
                    },
                    modifier = Modifier.testTag("rename_save_button")
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { renameSubjectTarget = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Delete Confirmation Dialog (Requirement 4: strong two-step confirmation)
    deleteSubjectTarget?.let { subject ->
        AlertDialog(
            onDismissRequest = { deleteSubjectTarget = null },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Delete Subject?") },
            text = {
                Text(
                    "Are you sure you want to delete '${subject.title}'?\n\nThis will safely cascade and delete all associated topics, study notes, comments, and revision history permanently."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteSubject(subject.id)
                        deleteSubjectTarget = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("delete_confirm_button")
                ) {
                    Text("Delete Subject")
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteSubjectTarget = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Info Dialog
    infoSubjectTarget?.let { subject ->
        AlertDialog(
            onDismissRequest = { infoSubjectTarget = null },
            title = { Text(subject.title) },
            text = {
                Column {
                    Text("Total Revisions Completed: ${subject.completedCount}")
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Next Revision Due: ${DateTimeUtils.formatDateTime(subject.nextDueAt)}")
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Internal ID: ${subject.id}")
                }
            },
            confirmButton = {
                TextButton(onClick = { infoSubjectTarget = null }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
fun CollapsibleSectionHeader(
    title: String,
    badgeCount: Int?,
    badgeColor: Color = MaterialTheme.colorScheme.primary,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    testTag: String
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onToggle)
            .testTag(testTag),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (badgeCount != null) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(badgeColor)
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = badgeCount.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.surface,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            Icon(
                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = if (isExpanded) "Collapse" else "Expand",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun SubjectCard(
    subject: RevisionSubjectEntity,
    currentTime: Long,
    isOverdue: Boolean = false,
    onOpen: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onComments: () -> Unit,
    onHistory: () -> Unit,
    onInfo: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("subject_card_${subject.id}"),
        colors = CardDefaults.cardColors(
            containerColor = if (isOverdue) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
            else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = subject.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )

                // Revision count chip
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.padding(horizontal = 6.dp)
                ) {
                    Text(
                        text = "Rev #${subject.completedCount}",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontWeight = FontWeight.Medium
                    )
                }

                // 3-dot menu
                Box {
                    IconButton(
                        onClick = { menuExpanded = true },
                        modifier = Modifier.size(36.dp).testTag("subject_menu_${subject.id}")
                    ) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Options")
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Total Revisions: ${subject.completedCount}") },
                            onClick = {
                                menuExpanded = false
                                onInfo()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Comments") },
                            onClick = {
                                menuExpanded = false
                                onComments()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Revision History") },
                            onClick = {
                                menuExpanded = false
                                onHistory()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Rename") },
                            onClick = {
                                menuExpanded = false
                                onRename()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                            onClick = {
                                menuExpanded = false
                                onDelete()
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Next revision date/time
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Schedule,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = if (isOverdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(6.dp))
                val dueStr = if (subject.nextDueAt != null) {
                    DateTimeUtils.formatDateTime(subject.nextDueAt)
                } else if (subject.completedCount > 0) {
                    "No more revision needed"
                } else {
                    "Not started yet"
                }
                Text(
                    text = "Next revision: $dueStr",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isOverdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Countdown display (Requirement 31)
            if (subject.nextDueAt != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.HourglassTop,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = if (isOverdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Countdown: ${DateTimeUtils.formatRemainingTime(subject.nextDueAt, currentTime)}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = if (isOverdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Open Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Button(
                    onClick = onOpen,
                    modifier = Modifier.testTag("open_subject_${subject.id}")
                ) {
                    Text("OPEN")
                }
            }
        }
    }
}

@Composable
fun OverviewProgressGrid(metrics: DashboardOverviewMetrics) {
    Card(
        modifier = Modifier.fillMaxWidth().testTag("overview_progress_card"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricItem(
                    label = "Total Revisions",
                    value = metrics.totalRevisions.toString(),
                    icon = Icons.Default.CheckCircle,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                MetricItem(
                    label = "Completed Today",
                    value = metrics.completedToday.toString(),
                    icon = Icons.Default.Assignment,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricItem(
                    label = "Overdue",
                    value = metrics.overdue.toString(),
                    icon = Icons.Default.Warning,
                    color = if (metrics.overdue > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline,
                    modifier = Modifier.weight(1f)
                )
                MetricItem(
                    label = "Current Streak",
                    value = "${metrics.currentStreak} Days",
                    icon = Icons.Default.History,
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun MetricItem(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

@Composable
fun HistoryItemRow(history: RevisionHistoryEntity) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${history.revisionSubjectId.replace('_', ' ').uppercase()} - ${history.action}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Completed: ${DateTimeUtils.formatDateTime(history.completedAt)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (history.scheduledFor != null) {
                Text(
                    text = "Next: ${DateTimeUtils.formatDateTime(history.scheduledFor)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun EmptySectionCard(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}
