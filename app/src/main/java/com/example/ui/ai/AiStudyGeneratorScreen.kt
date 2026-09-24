package com.example.ui.ai

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.ai.AiResult
import com.example.data.ai.GeminiClient
import com.example.data.repository.RevisionRepository
import com.example.data.repository.StudyRepository
import com.example.domain.model.AiGenerationMode
import com.example.domain.model.NoteType
import com.example.preferences.SecureKeyStorage
import com.example.preferences.UserPreferencesDataStore
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiStudyGeneratorScreen(
    geminiClient: GeminiClient,
    studyRepository: StudyRepository,
    revisionRepository: RevisionRepository,
    userPreferences: UserPreferencesDataStore,
    secureKeyStorage: SecureKeyStorage,
    onNavigateBack: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val subjects by revisionRepository.allSubjects.collectAsStateWithLifecycle(initialValue = emptyList())
    val selectedModelPref by userPreferences.selectedModel.collectAsStateWithLifecycle(initialValue = "gemini-3.5-flash")

    var promptInput by remember { mutableStateOf("") }
    var selectedMode by remember { mutableStateOf(AiGenerationMode.MIND_MAP) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var selectedImageBitmap by remember { mutableStateOf<Bitmap?>(null) }

    var isGenerating by remember { mutableStateOf(false) }
    var quotaErrorMessage by remember { mutableStateOf<String?>(null) }
    var generalErrorMessage by remember { mutableStateOf<String?>(null) }

    // Pre-Save Review State (Requirement 14)
    var generatedContent by remember { mutableStateOf<String?>(null) }
    var reviewTitle by remember { mutableStateOf("") }
    var reviewSubjectId by remember { mutableStateOf<String?>(null) }
    var reviewNoteType by remember { mutableStateOf(NoteType.MIND_MAP) }
    var reviewIsLocked by remember { mutableStateOf(true) }
    var isSavedSuccess by remember { mutableStateOf(false) }

    // Photo Picker launcher (zero broad permissions per Play Policy)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        selectedImageUri = uri
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri).use { stream ->
                    selectedImageBitmap = BitmapFactory.decodeStream(stream)
                }
            } catch (e: Exception) {
                selectedImageBitmap = null
            }
        } else {
            selectedImageBitmap = null
        }
    }

    var modelMenuExpanded by remember { mutableStateOf(false) }
    var subjectMenuExpanded by remember { mutableStateOf(false) }
    var noteTypeMenuExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI Study Generator") },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("ai_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = onNavigateToSettings,
                        modifier = Modifier.testTag("ai_settings_shortcut")
                    ) {
                        Icon(Icons.Default.VpnKey, contentDescription = "API Key Configuration")
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Error Banners
            quotaErrorMessage?.let { msg ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Quota Reached (HTTP 429)", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(msg, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onErrorContainer)
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = onNavigateToSettings,
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                            ) {
                                Text("Change API Key")
                            }
                            OutlinedButton(
                                onClick = {
                                    // Trigger offline generator directly
                                    val offlineTemplate = geminiClient.generateOfflineStructuredTemplate(
                                        topicTitle = promptInput.take(30),
                                        inputDetails = promptInput,
                                        mode = selectedMode
                                    )
                                    generatedContent = offlineTemplate
                                    reviewTitle = if (promptInput.isNotBlank()) promptInput.take(30) else "Offline Study Note"
                                    reviewNoteType = when (selectedMode) {
                                        AiGenerationMode.MIND_MAP -> NoteType.MIND_MAP
                                        AiGenerationMode.SIMPLIFY -> NoteType.SIMPLIFIED
                                        AiGenerationMode.CHEAT_SHEET -> NoteType.CHEAT_SHEET
                                        AiGenerationMode.CUSTOM -> NoteType.CUSTOM
                                    }
                                    quotaErrorMessage = null
                                }
                            ) {
                                Text("Generate Offline")
                            }
                        }
                    }
                }
            }

            generalErrorMessage?.let { msg ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(msg, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                        IconButton(onClick = { generalErrorMessage = null }) {
                            Icon(Icons.Default.Close, contentDescription = "Dismiss")
                        }
                    }
                }
            }

            // Generation Mode Selection Chips (Requirement 13)
            Text(
                text = "1. CHOOSE GENERATION MODE",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AiGenerationMode.values().forEach { mode ->
                    FilterChip(
                        selected = selectedMode == mode,
                        onClick = { selectedMode = mode },
                        label = { Text(mode.label) },
                        modifier = Modifier.testTag("mode_chip_${mode.name}")
                    )
                }
            }

            // Model Selector (Requirement 16)
            ExposedDropdownMenuBox(
                expanded = modelMenuExpanded,
                onExpandedChange = { modelMenuExpanded = !modelMenuExpanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = selectedModelPref,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Gemini Model") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = modelMenuExpanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = modelMenuExpanded,
                    onDismissRequest = { modelMenuExpanded = false }
                ) {
                    UserPreferencesDataStore.AVAILABLE_MODELS.forEach { model ->
                        DropdownMenuItem(
                            text = { Text(model) },
                            onClick = {
                                scope.launch { userPreferences.setSelectedModel(model) }
                                modelMenuExpanded = false
                            }
                        )
                    }
                }
            }

            // Multimodal Inputs (Requirement 12)
            Text(
                text = "2. INPUT STUDY MATERIAL OR PROMPT",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            OutlinedTextField(
                value = promptInput,
                onValueChange = { promptInput = it },
                label = { Text("Questions, topics, or pasted syllabus") },
                placeholder = { Text("Paste text or ask a question (e.g. 'Operating System scheduling algorithms with formulas and comparison')...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .testTag("ai_prompt_input"),
                maxLines = 8
            )

            // Image Picker
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                OutlinedButton(
                    onClick = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    modifier = Modifier.testTag("ai_pick_image_button")
                ) {
                    Icon(Icons.Default.Image, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (selectedImageBitmap != null) "Change Photo" else "+ Attach Photo (Notes/Book)")
                }

                if (selectedImageBitmap != null) {
                    TextButton(onClick = {
                        selectedImageBitmap = null
                        selectedImageUri = null
                    }) {
                        Text("Remove Photo", color = MaterialTheme.colorScheme.error)
                    }
                }
            }

            // Image preview
            selectedImageBitmap?.let { bmp ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                ) {
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = "Selected study page",
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            // Action Buttons: AI Generate vs Generate Offline Notes (Requirement 18)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Online AI Generate Button
                Button(
                    onClick = {
                        if (promptInput.isNotBlank() || selectedImageBitmap != null) {
                            isGenerating = true
                            quotaErrorMessage = null
                            generalErrorMessage = null
                            scope.launch {
                                val res = geminiClient.generateStudyContent(
                                    prompt = promptInput,
                                    imageBitmap = selectedImageBitmap,
                                    mode = selectedMode,
                                    model = selectedModelPref
                                )
                                isGenerating = false
                                when (res) {
                                    is AiResult.Success -> {
                                        generatedContent = res.text
                                        reviewTitle = promptInput.take(30).trim().ifBlank { "AI Study Note" }
                                        reviewNoteType = when (selectedMode) {
                                            AiGenerationMode.MIND_MAP -> NoteType.MIND_MAP
                                            AiGenerationMode.SIMPLIFY -> NoteType.SIMPLIFIED
                                            AiGenerationMode.CHEAT_SHEET -> NoteType.CHEAT_SHEET
                                            AiGenerationMode.CUSTOM -> NoteType.CUSTOM
                                        }
                                    }
                                    is AiResult.QuotaExceeded -> {
                                        quotaErrorMessage = res.message
                                    }
                                    is AiResult.Error -> {
                                        generalErrorMessage = res.message
                                    }
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("generate_ai_button"),
                    enabled = !isGenerating && (promptInput.isNotBlank() || selectedImageBitmap != null)
                ) {
                    if (isGenerating) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
                    } else {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Generate AI")
                    }
                }

                // Offline Template Button (Requirement 18: offline-first!)
                OutlinedButton(
                    onClick = {
                        val template = geminiClient.generateOfflineStructuredTemplate(
                            topicTitle = promptInput.take(30).trim(),
                            inputDetails = promptInput,
                            mode = selectedMode
                        )
                        generatedContent = template
                        reviewTitle = promptInput.take(30).trim().ifBlank { "Offline Study Template" }
                        reviewNoteType = when (selectedMode) {
                            AiGenerationMode.MIND_MAP -> NoteType.MIND_MAP
                            AiGenerationMode.SIMPLIFY -> NoteType.SIMPLIFIED
                            AiGenerationMode.CHEAT_SHEET -> NoteType.CHEAT_SHEET
                            AiGenerationMode.CUSTOM -> NoteType.CUSTOM
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("generate_offline_button")
                ) {
                    Icon(Icons.Default.CloudOff, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Offline Note")
                }
            }

            // 3. PRE-SAVE REVIEW WORKFLOW (Requirement 14: Never auto-save, allow review/edit before Room)
            generatedContent?.let { content ->
                Spacer(modifier = Modifier.height(12.dp))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("pre_save_review_card"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "3. PRE-SAVE REVIEW & EDIT",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Review and edit the generated note, assign a subject and title, then save to your local database.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // Title field
                        OutlinedTextField(
                            value = reviewTitle,
                            onValueChange = { reviewTitle = it },
                            label = { Text("Note Title *") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("review_title_input")
                        )

                        // Subject Selector
                        ExposedDropdownMenuBox(
                            expanded = subjectMenuExpanded,
                            onExpandedChange = { subjectMenuExpanded = !subjectMenuExpanded },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val curSubName = subjects.find { it.id == reviewSubjectId }?.title ?: "Select Subject *"
                            OutlinedTextField(
                                value = curSubName,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Save to Subject *") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = subjectMenuExpanded) },
                                modifier = Modifier.menuAnchor().fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                expanded = subjectMenuExpanded,
                                onDismissRequest = { subjectMenuExpanded = false }
                            ) {
                                subjects.forEach { sub ->
                                    DropdownMenuItem(
                                        text = { Text(sub.title) },
                                        onClick = {
                                            reviewSubjectId = sub.id
                                            subjectMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        // Note Type Selector
                        ExposedDropdownMenuBox(
                            expanded = noteTypeMenuExpanded,
                            onExpandedChange = { noteTypeMenuExpanded = !noteTypeMenuExpanded },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = reviewNoteType.displayName,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Note Type") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = noteTypeMenuExpanded) },
                                modifier = Modifier.menuAnchor().fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                expanded = noteTypeMenuExpanded,
                                onDismissRequest = { noteTypeMenuExpanded = false }
                            ) {
                                NoteType.values().forEach { t ->
                                    DropdownMenuItem(
                                        text = { Text(t.displayName) },
                                        onClick = {
                                            reviewNoteType = t
                                            noteTypeMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        // Lock State Checkbox (Requirement 9: defaults to locked)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = reviewIsLocked,
                                onCheckedChange = { reviewIsLocked = it },
                                modifier = Modifier.testTag("review_lock_checkbox")
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("🔒 Save as Locked (prevents accidental editing)", style = MaterialTheme.typography.bodyMedium)
                        }

                        // Editable Preview Box
                        OutlinedTextField(
                            value = content,
                            onValueChange = { generatedContent = it },
                            label = { Text("Note Content (Editable)") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(260.dp)
                                .testTag("review_content_editor"),
                            maxLines = 30
                        )

                        // Save to Room Button
                        Button(
                            onClick = {
                                val targetSubId = reviewSubjectId ?: subjects.firstOrNull()?.id
                                if (targetSubId != null && reviewTitle.isNotBlank()) {
                                    scope.launch {
                                        studyRepository.saveNote(
                                            subjectId = targetSubId,
                                            title = reviewTitle.trim(),
                                            content = content.trim(),
                                            noteType = reviewNoteType.name,
                                            isLocked = reviewIsLocked
                                        )
                                        isSavedSuccess = true
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("save_to_room_button"),
                            enabled = reviewTitle.isNotBlank() && (reviewSubjectId != null || subjects.isNotEmpty())
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Save Note to Room", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (isSavedSuccess) {
        AlertDialog(
            onDismissRequest = {
                isSavedSuccess = false
                onNavigateBack()
            },
            title = { Text("Note Saved Successfully! 🎉") },
            text = { Text("Your study note has been saved to the database. You can view, revise, and unlock it anytime.") },
            confirmButton = {
                Button(
                    onClick = {
                        isSavedSuccess = false
                        onNavigateBack()
                    }
                ) {
                    Text("Done")
                }
            }
        )
    }
}
