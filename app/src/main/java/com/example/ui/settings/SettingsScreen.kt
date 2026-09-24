package com.example.ui.settings

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.preferences.AppThemeMode
import com.example.preferences.UserPreferencesDataStore
import kotlinx.coroutines.launch

import androidx.compose.material.icons.filled.UploadFile

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    userPreferences: UserPreferencesDataStore,
    onNavigateBack: () -> Unit,
    onNavigateToManageSubjects: () -> Unit,
    onNavigateToAiConfig: () -> Unit,
    onNavigateToAiGenerator: () -> Unit,
    onNavigateToTestSetup: () -> Unit,
    onNavigateToQuestionImporter: () -> Unit,
    onNavigateToBackup: () -> Unit,
    onNavigateToFaq: () -> Unit
) {
    val themeMode by userPreferences.themeMode.collectAsStateWithLifecycle(initialValue = AppThemeMode.DARK)
    val scope = rememberCoroutineScope()
    var showThemeDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("settings_back")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Study & Revision Category
            item {
                SettingsCategoryTitle("STUDY & REVISIONS")
            }

            item {
                SettingsCard(
                    title = "Manage Subjects / Revisions",
                    subtitle = "Add, rename, delete subjects and view revision totals",
                    icon = Icons.Default.MenuBook,
                    onClick = onNavigateToManageSubjects,
                    testTag = "settings_manage_subjects"
                )
            }

            item {
                SettingsCard(
                    title = "Test & Quiz System",
                    subtitle = "Configure and start multiple-choice knowledge quizzes",
                    icon = Icons.Default.Quiz,
                    onClick = onNavigateToTestSetup,
                    testTag = "settings_quiz_system"
                )
            }

            item {
                SettingsCard(
                    title = "Bulk Question Importer (.js / .json)",
                    subtitle = "Import 20,000+ MCQs from multiple JS files or paste code",
                    icon = Icons.Default.UploadFile,
                    onClick = onNavigateToQuestionImporter,
                    testTag = "settings_question_importer"
                )
            }

            item {
                SettingsCard(
                    title = "AI Study Generator",
                    subtitle = "Generate Mind Maps, Cheat Sheets, and ELI5 notes",
                    icon = Icons.Default.AutoAwesome,
                    onClick = onNavigateToAiGenerator,
                    testTag = "settings_ai_generator"
                )
            }

            // Configuration & Security Category
            item {
                Spacer(modifier = Modifier.height(4.dp))
                SettingsCategoryTitle("AI & SYSTEM CONFIGURATION")
            }

            item {
                SettingsCard(
                    title = "AI / Gemini Configuration",
                    subtitle = "Bring Your Own Key (BYOK), Test Connection, Model selector",
                    icon = Icons.Default.VpnKey,
                    onClick = onNavigateToAiConfig,
                    testTag = "settings_ai_config"
                )
            }

            item {
                SettingsCard(
                    title = "Appearance & Theme",
                    subtitle = "Current: ${themeMode.name} (Default: Dark Mode)",
                    icon = Icons.Default.DarkMode,
                    onClick = { showThemeDialog = true },
                    testTag = "settings_theme"
                )
            }

            item {
                SettingsCard(
                    title = "Data & Backup",
                    subtitle = "Export JSON backup, Restore data with SAF",
                    icon = Icons.Default.Backup,
                    onClick = onNavigateToBackup,
                    testTag = "settings_backup"
                )
            }

            // Help & About Category
            item {
                Spacer(modifier = Modifier.height(4.dp))
                SettingsCategoryTitle("HELP & SUPPORT")
            }

            item {
                SettingsCard(
                    title = "Frequently Asked Questions (FAQ)",
                    subtitle = "Searchable guides for revisions, time, notes, unlock, and AI",
                    icon = Icons.Default.HelpOutline,
                    onClick = onNavigateToFaq,
                    testTag = "settings_faq"
                )
            }

            item {
                SettingsCard(
                    title = "About Revision Tracker",
                    subtitle = "Version 1.0 • 100% Offline-First Architecture",
                    icon = Icons.Default.Info,
                    onClick = { showAboutDialog = true },
                    testTag = "settings_about"
                )
            }
        }
    }

    // Theme Selection Dialog (Requirement 67: Dark default, Light, System)
    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text("Choose Theme") },
            text = {
                Column {
                    AppThemeMode.values().forEach { mode ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    scope.launch {
                                        userPreferences.setThemeMode(mode)
                                        showThemeDialog = false
                                    }
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = themeMode == mode,
                                onClick = {
                                    scope.launch {
                                        userPreferences.setThemeMode(mode)
                                        showThemeDialog = false
                                    }
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = when (mode) {
                                    AppThemeMode.DARK -> "Dark Mode (Default)"
                                    AppThemeMode.LIGHT -> "Light Mode"
                                    AppThemeMode.SYSTEM -> "Follow System"
                                },
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemeDialog = false }) { Text("Close") }
            }
        )
    }

    // About Dialog
    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = { Text("About Revision Tracker") },
            text = {
                Column {
                    Text(
                        "Revision Tracker is a native Android study companion designed for spaced revision, structured notes, mind maps, and active recall tests.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("• Local Persistence: SQLite Room Database", style = MaterialTheme.typography.bodySmall)
                    Text("• Security: Hardware Keystore AES-256-GCM for BYOK", style = MaterialTheme.typography.bodySmall)
                    Text("• Offline First: Complete revision without internet", style = MaterialTheme.typography.bodySmall)
                    Text("• Multimodal AI: Google Gemini Integration", style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) { Text("OK") }
            }
        )
    }
}

@Composable
fun SettingsCategoryTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
    )
}

@Composable
fun SettingsCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    testTag: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag(testTag),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(10.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.outline
            )
        }
    }
}
