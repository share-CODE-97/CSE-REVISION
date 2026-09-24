package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.data.ai.GeminiClient
import com.example.data.database.AppDatabase
import com.example.data.importer.QuestionImportRepository
import com.example.data.repository.BackupRepository
import com.example.data.repository.RevisionRepository
import com.example.data.repository.StudyRepository
import com.example.data.repository.TestRepository
import com.example.domain.model.TestSummary
import com.example.preferences.AppThemeMode
import com.example.preferences.SecureKeyStorage
import com.example.preferences.UserPreferencesDataStore
import com.example.ui.ai.AiStudyGeneratorScreen
import com.example.ui.comments.CommentsScreen
import com.example.ui.dashboard.DashboardScreen
import com.example.ui.dashboard.DashboardViewModel
import com.example.ui.faq.FAQScreen
import com.example.ui.history.SubjectHistoryScreen
import com.example.ui.importer.QuestionImporterScreen
import com.example.ui.notes.NoteDetailScreen
import com.example.ui.notes.NoteEditorScreen
import com.example.ui.revision.RevisionScreen
import com.example.ui.revision.RevisionViewModel
import com.example.ui.settings.AiConfigScreen
import com.example.ui.settings.BackupScreen
import com.example.ui.settings.SettingsScreen
import com.example.ui.subjects.SubjectManagementScreen
import com.example.ui.test.ActiveTestScreen
import com.example.ui.test.TestResultScreen
import com.example.ui.test.TestSetupScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.topics.TopicListScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val db = AppDatabase.getInstance(applicationContext)
        val revisionRepository = RevisionRepository(db)
        val studyRepository = StudyRepository(db)
        val testRepository = TestRepository(db)
        val backupRepository = BackupRepository(db)
        val questionImportRepository = QuestionImportRepository(db)
        val secureKeyStorage = SecureKeyStorage(applicationContext)
        val userPreferences = UserPreferencesDataStore(applicationContext)
        val geminiClient = GeminiClient(secureKeyStorage)
        val dashboardViewModel = DashboardViewModel(revisionRepository)

        setContent {
            val themeMode by userPreferences.themeMode.collectAsStateWithLifecycle(initialValue = AppThemeMode.DARK)
            val isDark = when (themeMode) {
                AppThemeMode.DARK -> true
                AppThemeMode.LIGHT -> false
                AppThemeMode.SYSTEM -> isSystemInDarkTheme()
            }

            MyApplicationTheme(darkTheme = isDark) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    RevisionTrackerApp(
                        revisionRepository = revisionRepository,
                        studyRepository = studyRepository,
                        testRepository = testRepository,
                        backupRepository = backupRepository,
                        questionImportRepository = questionImportRepository,
                        secureKeyStorage = secureKeyStorage,
                        userPreferences = userPreferences,
                        geminiClient = geminiClient,
                        dashboardViewModel = dashboardViewModel
                    )
                }
            }
        }
    }
}

@Composable
fun RevisionTrackerApp(
    revisionRepository: RevisionRepository,
    studyRepository: StudyRepository,
    testRepository: TestRepository,
    backupRepository: BackupRepository,
    questionImportRepository: QuestionImportRepository,
    secureKeyStorage: SecureKeyStorage,
    userPreferences: UserPreferencesDataStore,
    geminiClient: GeminiClient,
    dashboardViewModel: DashboardViewModel
) {
    val navController = rememberNavController()
    var latestTestResult by remember { mutableStateOf<TestSummary?>(null) }

    NavHost(
        navController = navController,
        startDestination = "dashboard"
    ) {
        // Dashboard Screen
        composable("dashboard") {
            DashboardScreen(
                viewModel = dashboardViewModel,
                onNavigateToRevision = { subjectId ->
                    navController.navigate("revision/$subjectId")
                },
                onNavigateToTest = {
                    navController.navigate("test_setup")
                },
                onNavigateToSettings = {
                    navController.navigate("settings")
                },
                onOpenComments = { subjectId ->
                    navController.navigate("comments/$subjectId")
                },
                onOpenHistory = { subjectId ->
                    navController.navigate("history/$subjectId")
                }
            )
        }

        // Revision Screen
        composable(
            route = "revision/{subjectId}",
            arguments = listOf(navArgument("subjectId") { type = NavType.StringType })
        ) { backStackEntry ->
            val subjectId = backStackEntry.arguments?.getString("subjectId") ?: ""
            val revisionViewModel = remember(subjectId) {
                RevisionViewModel(subjectId, revisionRepository, studyRepository)
            }
            RevisionScreen(
                viewModel = revisionViewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToTopics = { subId ->
                    navController.navigate("topics/$subId")
                },
                onNavigateToNote = { noteId ->
                    navController.navigate("note_detail/$noteId")
                }
            )
        }

        // Topics & Notes List for Subject
        composable(
            route = "topics/{subjectId}",
            arguments = listOf(navArgument("subjectId") { type = NavType.StringType })
        ) { backStackEntry ->
            val subjectId = backStackEntry.arguments?.getString("subjectId") ?: ""
            TopicListScreen(
                subjectId = subjectId,
                revisionRepository = revisionRepository,
                studyRepository = studyRepository,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToNote = { noteId ->
                    navController.navigate("note_detail/$noteId")
                },
                onNavigateToCreateNote = { subId, topicId ->
                    val route = if (topicId != null) "note_editor/$subId?topicId=$topicId" else "note_editor/$subId"
                    navController.navigate(route)
                }
            )
        }

        // Note Detail View
        composable(
            route = "note_detail/{noteId}",
            arguments = listOf(navArgument("noteId") { type = NavType.StringType })
        ) { backStackEntry ->
            val noteId = backStackEntry.arguments?.getString("noteId") ?: ""
            NoteDetailScreen(
                noteId = noteId,
                studyRepository = studyRepository,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToEdit = { nId ->
                    navController.navigate("note_editor_by_id/$nId")
                }
            )
        }

        // Note Editor (create new note)
        composable(
            route = "note_editor/{subjectId}?topicId={topicId}",
            arguments = listOf(
                navArgument("subjectId") { type = NavType.StringType },
                navArgument("topicId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val subjectId = backStackEntry.arguments?.getString("subjectId") ?: ""
            val topicId = backStackEntry.arguments?.getString("topicId")
            NoteEditorScreen(
                noteId = null,
                subjectId = subjectId,
                initialTopicId = topicId,
                studyRepository = studyRepository,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Note Editor (edit existing note)
        composable(
            route = "note_editor_by_id/{noteId}",
            arguments = listOf(navArgument("noteId") { type = NavType.StringType })
        ) { backStackEntry ->
            val noteId = backStackEntry.arguments?.getString("noteId") ?: ""
            val note by studyRepository.getNote(noteId).collectAsStateWithLifecycle(initialValue = null)
            if (note != null) {
                NoteEditorScreen(
                    noteId = noteId,
                    subjectId = note!!.subjectId,
                    initialTopicId = note!!.parentTopicId,
                    studyRepository = studyRepository,
                    onNavigateBack = { navController.popBackStack() }
                )
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        }

        // Manage Subjects / Revisions
        composable("manage_subjects") {
            SubjectManagementScreen(
                repository = revisionRepository,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // AI Study Generator
        composable("ai_generator") {
            AiStudyGeneratorScreen(
                geminiClient = geminiClient,
                studyRepository = studyRepository,
                revisionRepository = revisionRepository,
                userPreferences = userPreferences,
                secureKeyStorage = secureKeyStorage,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToSettings = { navController.navigate("ai_config") }
            )
        }

        // AI / Gemini Config (BYOK)
        composable("ai_config") {
            AiConfigScreen(
                secureKeyStorage = secureKeyStorage,
                geminiClient = geminiClient,
                userPreferences = userPreferences,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Test Setup
        composable("test_setup") {
            TestSetupScreen(
                revisionRepository = revisionRepository,
                testRepository = testRepository,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToImporter = { navController.navigate("question_importer") },
                onStartTest = { subId, count, isTimed, minutes ->
                    val encodedSub = subId ?: "ALL"
                    navController.navigate("active_test/$encodedSub/$count/$isTimed/$minutes")
                }
            )
        }

        // Bulk Question Importer
        composable("question_importer") {
            QuestionImporterScreen(
                importRepository = questionImportRepository,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToTest = {
                    navController.navigate("test_setup") {
                        popUpTo("dashboard")
                    }
                }
            )
        }

        // Active Test
        composable(
            route = "active_test/{subjectId}/{count}/{isTimed}/{minutes}",
            arguments = listOf(
                navArgument("subjectId") { type = NavType.StringType },
                navArgument("count") { type = NavType.IntType },
                navArgument("isTimed") { type = NavType.BoolType },
                navArgument("minutes") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val rawSub = backStackEntry.arguments?.getString("subjectId") ?: "ALL"
            val subjectId = if (rawSub == "ALL") null else rawSub
            val count = backStackEntry.arguments?.getInt("count") ?: 5
            val isTimed = backStackEntry.arguments?.getBoolean("isTimed") ?: false
            val minutes = backStackEntry.arguments?.getInt("minutes") ?: 10

            ActiveTestScreen(
                subjectId = subjectId,
                count = count,
                isTimed = isTimed,
                timeLimitMinutes = minutes,
                testRepository = testRepository,
                onNavigateBack = { navController.popBackStack() },
                onFinishTest = { summary ->
                    latestTestResult = summary
                    navController.navigate("test_result") {
                        popUpTo("active_test/{subjectId}/{count}/{isTimed}/{minutes}") { inclusive = true }
                    }
                }
            )
        }

        // Test Result
        composable("test_result") {
            val result = latestTestResult
            if (result != null) {
                TestResultScreen(
                    result = result,
                    onRetake = {
                        navController.navigate("test_setup") {
                            popUpTo("dashboard")
                        }
                    },
                    onNavigateToHome = {
                        navController.navigate("dashboard") {
                            popUpTo("dashboard") { inclusive = true }
                        }
                    }
                )
            } else {
                navController.popBackStack()
            }
        }

        // Comments Screen
        composable(
            route = "comments/{subjectId}",
            arguments = listOf(navArgument("subjectId") { type = NavType.StringType })
        ) { backStackEntry ->
            val subjectId = backStackEntry.arguments?.getString("subjectId") ?: ""
            CommentsScreen(
                subjectId = subjectId,
                revisionRepository = revisionRepository,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // History Screen
        composable(
            route = "history/{subjectId}",
            arguments = listOf(navArgument("subjectId") { type = NavType.StringType })
        ) { backStackEntry ->
            val subjectId = backStackEntry.arguments?.getString("subjectId") ?: ""
            SubjectHistoryScreen(
                subjectId = subjectId,
                revisionRepository = revisionRepository,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Settings Screen
        composable("settings") {
            SettingsScreen(
                userPreferences = userPreferences,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToManageSubjects = { navController.navigate("manage_subjects") },
                onNavigateToAiConfig = { navController.navigate("ai_config") },
                onNavigateToAiGenerator = { navController.navigate("ai_generator") },
                onNavigateToTestSetup = { navController.navigate("test_setup") },
                onNavigateToQuestionImporter = { navController.navigate("question_importer") },
                onNavigateToBackup = { navController.navigate("backup") },
                onNavigateToFaq = { navController.navigate("faq") }
            )
        }

        // Backup Screen
        composable("backup") {
            BackupScreen(
                backupRepository = backupRepository,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // FAQ Screen
        composable("faq") {
            FAQScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
