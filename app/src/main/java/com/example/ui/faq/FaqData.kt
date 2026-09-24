package com.example.ui.faq

data class FaqItem(
    val id: String,
    val category: String,
    val question: String,
    val answer: String
)

object FaqData {
    val items: List<FaqItem> = listOf(
        // Getting Started
        FaqItem(
            id = "gs_1",
            category = "Getting Started",
            question = "What is this app?",
            answer = "Revision Tracker is a 100% native Android study companion. It combines spaced revision scheduling, structured notes with mind-map trees, a customizable native quiz/test engine, and optional AI study generation—all working completely offline by default."
        ),
        FaqItem(
            id = "gs_2",
            category = "Getting Started",
            question = "How do I start?",
            answer = "On the Dashboard, you will see your subjects. Tap 'OPEN' on any subject (like Maths or Table) to read its study notes and multiplication tables. When finished revising, scroll to the bottom, choose your next revision interval (+24 Hours, +48 Hours, or Custom Date & Time), and tap 'COMPLETE'."
        ),
        FaqItem(
            id = "gs_3",
            category = "Getting Started",
            question = "What should I do first?",
            answer = "1. Explore existing subjects like Maths, Physics, and Table.\n2. Tap the 'TEST' button in the top app bar to try a practice quiz.\n3. Open Settings -> 'Manage Subjects / Revisions' to add your own subjects.\n4. Optionally enter your Gemini API key in Settings if you wish to use AI generation."
        ),

        // Subjects
        FaqItem(
            id = "sub_1",
            category = "Subjects",
            question = "How do I add a new subject?",
            answer = "1. Tap the Settings gear ⚙ in the top app bar.\n2. Tap 'Manage Subjects / Revisions'.\n3. Tap '+ Add Subject'.\n4. Type your subject name (e.g., 'Computer Science').\n5. Tap 'Save'. The subject instantly appears across Dashboard, Topics, Notes, Scheduling, and Quiz filters."
        ),
        FaqItem(
            id = "sub_2",
            category = "Subjects",
            question = "How do I rename a subject?",
            answer = "Go to Settings -> 'Manage Subjects / Revisions', or tap the three-dot menu on any subject card on the Dashboard, select 'Rename', enter the new name, and tap 'Save'. Internal database IDs remain stable so all notes and history are preserved."
        ),
        FaqItem(
            id = "sub_3",
            category = "Subjects",
            question = "How do I delete a subject?",
            answer = "Go to Settings -> 'Manage Subjects / Revisions' or the subject three-dot menu, and choose 'Delete Subject'. A safety dialog requires explicit confirmation. Deleting safely cascades and removes related topics, notes, comments, and history."
        ),
        FaqItem(
            id = "sub_4",
            category = "Subjects",
            question = "What happens when I delete a subject?",
            answer = "All topics, notes, revision logs, comments, and linked questions belonging to that subject are cleanly removed from the local SQLite database. The app returns to the Dashboard with no orphaned data."
        ),

        // Topics & Notes
        FaqItem(
            id = "top_1",
            category = "Topics & Notes",
            question = "How do I add a topic?",
            answer = "Open any subject from the Dashboard or Subjects screen, tap 'Topics & Notes', then tap '+ Add Topic'. Enter the topic name and save."
        ),
        FaqItem(
            id = "top_2",
            category = "Topics & Notes",
            question = "How do I add a note?",
            answer = "In the subject's Topics & Notes screen, tap '+ Add Note'. Choose a title, select the note type (Standard, Mind Map, Cheat Sheet, or Simplified), enter your content, and tap 'Save'."
        ),
        FaqItem(
            id = "top_3",
            category = "Topics & Notes",
            question = "How do I edit a note?",
            answer = "If the note is locked 🔒, tap 'Unlock Note' 🔓. Once unlocked, edit the title, content, or type in the editor, then tap 'Save Note'."
        ),
        FaqItem(
            id = "top_4",
            category = "Topics & Notes",
            question = "What is a mind-map note?",
            answer = "A Mind Map note renders hierarchical concepts as an interactive, expandable and collapsible tree (e.g. Main Concept -> Branches -> Sub-points). It allows visual breakdown of complex topics without clutter."
        ),

        // Lock / Unlock
        FaqItem(
            id = "lock_1",
            category = "Lock / Unlock",
            question = "Why is my note locked?",
            answer = "All finalized notes and AI-generated notes default to 🔒 Locked mode to prevent accidental keystrokes, unintended text deletion, or keyboard popups while you are in focused revision mode."
        ),
        FaqItem(
            id = "lock_2",
            category = "Lock / Unlock",
            question = "How do I unlock a note?",
            answer = "Tap the 'Unlock Note' button (or padlock icon) on the note viewer. The lock state will change to 🔓 Unlocked and open editing controls. The lock state is permanently saved in Room."
        ),

        // Revision Tracker & Scheduling
        FaqItem(
            id = "rev_1",
            category = "Scheduling",
            question = "How does revision scheduling work?",
            answer = "Spaced repetition helps move knowledge into long-term memory. When you revise a subject, choose an interval (+24h, +48h, or a specific date & time). Selecting an option does NOT save it immediately—only pressing 'COMPLETE' commits the revision."
        ),
        FaqItem(
            id = "rev_2",
            category = "Scheduling",
            question = "What does +24 mean?",
            answer = "+24 schedules the next revision exactly 24 hours from the current moment using timestamp arithmetic (e.g., today 20:00 becomes tomorrow 20:00)."
        ),
        FaqItem(
            id = "rev_3",
            category = "Scheduling",
            question = "What does +48 mean?",
            answer = "+48 schedules the next revision exactly 48 hours from the current moment (2 days later at the same hour)."
        ),
        FaqItem(
            id = "rev_4",
            category = "Scheduling",
            question = "Can I schedule for later today?",
            answer = "Yes! If the current time is 08:27, choosing 08:30 today is completely valid. The date/time validator only rejects times that have already passed in the past."
        ),
        FaqItem(
            id = "rev_5",
            category = "Scheduling",
            question = "What happens when I press Complete?",
            answer = "Pressing 'COMPLETE': 1) Records the exact completion timestamp, 2) Increments your subject's revision count, 3) Creates a history log, 4) Sets the next due timestamp, and 5) Atomically updates the database and refreshes the Dashboard."
        ),
        FaqItem(
            id = "rev_6",
            category = "Scheduling",
            question = "How does Undo work?",
            answer = "If you accidentally tapped 'Complete', tap 'Undo Last Complete' on the subject screen. This restores your previous completion count, reverts the schedule, and removes the latest history entry."
        ),

        // Test / Quiz System
        FaqItem(
            id = "test_1",
            category = "Test / Quiz",
            question = "What is the Test section?",
            answer = "Tap the 'TEST' button in the top app bar to access the native quiz system. It evaluates your knowledge with multiple-choice questions independently from your revision notes."
        ),
        FaqItem(
            id = "test_2",
            category = "Test / Quiz",
            question = "How do I start a test?",
            answer = "Tap 'TEST' in the top app bar -> choose a Subject (or All Subjects) -> optionally pick a Topic -> select your Question Count -> pick Timed or Untimed -> tap 'Start Test'."
        ),
        FaqItem(
            id = "test_3",
            category = "Question Count",
            question = "How is question count selected and validated?",
            answer = "The question count is 100% data-driven! You can pick from available options (5, 10, 20, etc.). If you select 50 questions but only 32 exist in the database, the app clearly displays: 'Only 32 questions are currently available. Maximum possible test size: 32' without crashing or generating fake questions."
        ),
        FaqItem(
            id = "test_4",
            category = "Test / Quiz",
            question = "How does random question selection work?",
            answer = "When you start a test, the database randomly selects the requested number of unique questions from the chosen subject/topic bank with no duplicates within that test."
        ),
        FaqItem(
            id = "test_5",
            category = "Test / Quiz",
            question = "Can I review my test answers?",
            answer = "Yes! After finishing a test, the Result screen shows your score, accuracy %, and time spent, along with a complete question-by-question review highlighting your selected answer, the correct answer, and detailed explanations."
        ),

        // AI Study Generator
        FaqItem(
            id = "ai_1",
            category = "AI Study Generator",
            question = "What is AI Study Generator?",
            answer = "It generates structured study notes using text prompts or uploaded images (such as textbook pages or handwritten notes) via Google's Gemini models."
        ),
        FaqItem(
            id = "ai_2",
            category = "AI Study Generator",
            question = "Can I use an image?",
            answer = "Yes! Tap 'Select Image' to pick a photo from your gallery using Android's secure Photo Picker. No broad storage permissions are requested."
        ),
        FaqItem(
            id = "ai_3",
            category = "AI Study Generator",
            question = "What are the 4 generation modes?",
            answer = "• Mind Map: Builds a hierarchical concept tree.\n• Simplify / ELI5: Uses intuitive analogies and simple breakdowns.\n• Cheat Sheet: Concentrates formulas, definitions, and exam pitfalls.\n• Custom Prompt: Follows your specific free-form study instructions."
        ),
        FaqItem(
            id = "ai_4",
            category = "AI Study Generator",
            question = "Does AI save notes automatically?",
            answer = "Never! AI output always goes into Pre-Save Review where you can preview, edit the text, choose a title, pick a subject/topic, set note type, and decide lock state before saving to Room."
        ),

        // Gemini API Key (BYOK)
        FaqItem(
            id = "gem_1",
            category = "Gemini API Key",
            question = "What is BYOK (Bring Your Own Key)?",
            answer = "You can enter your personal Gemini API key from Google AI Studio. It is stored securely on your device using hardware-backed Android Keystore encryption."
        ),
        FaqItem(
            id = "gem_2",
            category = "Gemini API Key",
            question = "How do I test my Gemini key?",
            answer = "Go to Settings -> 'AI / Gemini Configuration', enter your key, and tap 'Test Connection'. The app will verify connectivity and model readiness."
        ),
        FaqItem(
            id = "gem_3",
            category = "Gemini API Key",
            question = "What does HTTP 429 mean?",
            answer = "HTTP 429 indicates that your Gemini API rate or quota limit has been exceeded. The app provides clear options: switch API key, switch model, try again later, or use 'Generate Offline Notes'."
        ),

        // Offline Mode
        FaqItem(
            id = "off_1",
            category = "Offline Mode",
            question = "What works without internet?",
            answer = "Everything core! All revision tracking, multiplication tables, cubes, study notes, mind-map trees, quizzes/tests, comments, history, and offline note generation work 100% locally with zero internet connection."
        ),
        FaqItem(
            id = "off_2",
            category = "Offline Mode",
            question = "What is 'Generate Offline Notes'?",
            answer = "If you don't have an internet connection or Gemini API key, tap 'Generate Offline Notes' in the AI screen. It generates a comprehensive, pre-structured academic template with definitions, formulas, and revision checkpoints directly on device."
        ),

        // History & Streak
        FaqItem(
            id = "his_1",
            category = "History & Streak",
            question = "How is Current Streak calculated?",
            answer = "Your streak is the number of consecutive calendar days ending today where you completed at least one revision. If you haven't completed any revision today yet, streak displays 0 until you complete one."
        ),

        // Backup & Restore
        FaqItem(
            id = "bak_1",
            category = "Backup & Restore",
            question = "How do I export and restore my data?",
            answer = "Go to Settings -> 'Data & Backup'. Tap 'Export Backup' to save all subjects, topics, notes, questions, and revision history into a JSON file via Android Storage Access Framework. Tap 'Import Backup' to restore from a backup file."
        ),
        FaqItem(
            id = "bak_2",
            category = "Backup & Restore",
            question = "What happens if a backup file is corrupt or invalid?",
            answer = "The import validator checks the JSON structure first. If invalid, the import fails safely and your existing database remains 100% untouched."
        )
    )
}
