package com.example.data.database

import com.example.data.entity.NoteEntity
import com.example.data.entity.QuestionEntity
import com.example.data.entity.RevisionSubjectEntity
import com.example.data.entity.TopicEntity
import org.json.JSONArray

object InitialData {
    val initialSubjects = listOf(
        RevisionSubjectEntity(id = "maths", title = "Maths", sortOrder = 1),
        RevisionSubjectEntity(id = "physics", title = "Physics", sortOrder = 2),
        RevisionSubjectEntity(id = "history", title = "History", sortOrder = 3),
        RevisionSubjectEntity(id = "table", title = "Table", sortOrder = 4),
        RevisionSubjectEntity(id = "cube", title = "Cube", sortOrder = 5),
        RevisionSubjectEntity(id = "medival", title = "Medival History", sortOrder = 6)
    )

    val initialTopics = listOf(
        // Maths
        TopicEntity(id = "m_calc", subjectId = "maths", title = "Calculus & Derivatives", sortOrder = 1),
        TopicEntity(id = "m_alg", subjectId = "maths", title = "Algebra & Quadratics", sortOrder = 2),
        // Physics
        TopicEntity(id = "p_mech", subjectId = "physics", title = "Mechanics & Newton's Laws", sortOrder = 1),
        TopicEntity(id = "p_em", subjectId = "physics", title = "Electromagnetism & Waves", sortOrder = 2),
        // History
        TopicEntity(id = "h_ancient", subjectId = "history", title = "Ancient Civilizations", sortOrder = 1),
        TopicEntity(id = "h_modern", subjectId = "history", title = "World Wars & Treaties", sortOrder = 2),
        // Table
        TopicEntity(id = "t_1_to_5", subjectId = "table", title = "Multiplication Tables 1 to 5", sortOrder = 1),
        TopicEntity(id = "t_6_to_10", subjectId = "table", title = "Multiplication Tables 6 to 10", sortOrder = 2),
        // Cube
        TopicEntity(id = "c_1_to_15", subjectId = "cube", title = "Cubes 1 to 15", sortOrder = 1),
        TopicEntity(id = "c_16_to_30", subjectId = "cube", title = "Cubes 16 to 30 & Formulas", sortOrder = 2),
        // Medival History
        TopicEntity(id = "mh_delhi", subjectId = "medival", title = "Delhi Sultanate & Dynasties", sortOrder = 1),
        TopicEntity(id = "mh_mughal", subjectId = "medival", title = "Mughal Empire & Administration", sortOrder = 2)
    )

    // Table Content (1x to 10x) formatted cleanly so it wraps nicely without horizontal page overflow!
    private fun buildTableContent(start: Int, end: Int): String {
        val sb = StringBuilder()
        for (n in start..end) {
            sb.append("### Table of $n\n")
            for (i in 1..10) {
                sb.append("• $n × $i = ${n * i}\n")
            }
            sb.append("\n")
        }
        return sb.toString().trim()
    }

    private fun buildCubeContent(start: Int, end: Int): String {
        val sb = StringBuilder()
        sb.append("### Perfect Cubes ($start³ to $end³)\n")
        for (i in start..end) {
            sb.append("• $i³ = ${i * i * i}\n")
        }
        sb.append("\n**Key Cube Formulas:**\n")
        sb.append("• (a + b)³ = a³ + 3a²b + 3ab² + b³ = a³ + b³ + 3ab(a + b)\n")
        sb.append("• (a - b)³ = a³ - 3a²b + 3ab² - b³ = a³ - b³ - 3ab(a - b)\n")
        sb.append("• a³ + b³ = (a + b)(a² - ab + b²)\n")
        sb.append("• a³ - b³ = (a - b)(a² + ab + b²)\n")
        return sb.toString().trim()
    }

    val initialNotes = listOf(
        // Table Notes
        NoteEntity(
            id = "note_table_1_5",
            subjectId = "table",
            parentTopicId = "t_1_to_5",
            title = "Multiplication Tables 1 to 5",
            content = buildTableContent(1, 5),
            noteType = "CHEAT_SHEET",
            isLocked = true,
            sortOrder = 1
        ),
        NoteEntity(
            id = "note_table_6_10",
            subjectId = "table",
            parentTopicId = "t_6_to_10",
            title = "Multiplication Tables 6 to 10",
            content = buildTableContent(6, 10),
            noteType = "CHEAT_SHEET",
            isLocked = true,
            sortOrder = 2
        ),
        // Cube Notes
        NoteEntity(
            id = "note_cube_1_15",
            subjectId = "cube",
            parentTopicId = "c_1_to_15",
            title = "Cubes 1 to 15 Reference",
            content = buildCubeContent(1, 15),
            noteType = "CHEAT_SHEET",
            isLocked = true,
            sortOrder = 1
        ),
        NoteEntity(
            id = "note_cube_16_30",
            subjectId = "cube",
            parentTopicId = "c_16_to_30",
            title = "Cubes 16 to 30 & Identities",
            content = buildCubeContent(16, 30),
            noteType = "CHEAT_SHEET",
            isLocked = true,
            sortOrder = 2
        ),
        // Maths Notes
        NoteEntity(
            id = "note_maths_calc",
            subjectId = "maths",
            parentTopicId = "m_calc",
            title = "Differentiation Rules & Derivatives",
            content = """
                # Core Differentiation Rules
                • Power Rule: d/dx(xⁿ) = n·xⁿ⁻¹
                • Product Rule: (uv)' = u'v + uv'
                • Quotient Rule: (u/v)' = (u'v - uv') / v²
                • Chain Rule: d/dx[f(g(x))] = f'(g(x)) · g'(x)

                ## Trigonometric Derivatives
                • d/dx(sin x) = cos x
                • d/dx(cos x) = -sin x
                • d/dx(tan x) = sec² x
                • d/dx(sec x) = sec x · tan x
                • d/dx(ln x) = 1/x
                • d/dx(eˣ) = eˣ
            """.trimIndent(),
            noteType = "CHEAT_SHEET",
            isLocked = true,
            sortOrder = 1
        ),
        // Physics Mind Map Note
        NoteEntity(
            id = "note_physics_mm",
            subjectId = "physics",
            parentTopicId = "p_mech",
            title = "Classical Mechanics Mind Map",
            content = """
                {
                  "id": "root",
                  "title": "Classical Mechanics",
                  "notes": "Study of motion and forces",
                  "children": [
                    {
                      "id": "kinematics",
                      "title": "Kinematics",
                      "notes": "Describing motion without causes",
                      "children": [
                        { "id": "k1", "title": "Displacement & Velocity (v = ds/dt)" },
                        { "id": "k2", "title": "Acceleration (a = dv/dt)" },
                        { "id": "k3", "title": "Equations of Motion (v = u + at, s = ut + 0.5at²)" }
                      ]
                    },
                    {
                      "id": "dynamics",
                      "title": "Dynamics (Newton's Laws)",
                      "notes": "Forces causing motion",
                      "children": [
                        { "id": "d1", "title": "1st Law: Inertia (ΣF = 0 → v = const)" },
                        { "id": "d2", "title": "2nd Law: Momentum rate (F = dp/dt = ma)" },
                        { "id": "d3", "title": "3rd Law: Action & Reaction (F_ab = -F_ba)" }
                      ]
                    },
                    {
                      "id": "energy",
                      "title": "Work & Energy",
                      "children": [
                        { "id": "e1", "title": "Work: W = F · d · cos(θ)" },
                        { "id": "e2", "title": "Kinetic Energy: KE = 0.5 m v²" },
                        { "id": "e3", "title": "Potential Energy: PE = m g h" },
                        { "id": "e4", "title": "Conservation of Mechanical Energy" }
                      ]
                    }
                  ]
                }
            """.trimIndent(),
            noteType = "MIND_MAP",
            isLocked = true,
            sortOrder = 1
        ),
        // Medival History
        NoteEntity(
            id = "note_medival_sultanate",
            subjectId = "medival",
            parentTopicId = "mh_delhi",
            title = "Delhi Sultanate Chronology",
            content = """
                # Delhi Sultanate (1206 – 1526 CE)
                5 Major Dynasties:
                1. **Slave / Mamluk Dynasty (1206 - 1290)**:
                   • Founded by Qutb-ud-din Aibak
                   • Consolidated by Iltutmish
                   • Razia Sultan: First woman monarch of Delhi
                   • Balban: Policy of 'Blood and Iron'
                
                2. **Khilji Dynasty (1290 - 1320)**:
                   • Alauddin Khilji: Market control regulations, military reforms
                
                3. **Tughlaq Dynasty (1320 - 1414)**:
                   • Muhammad bin Tughlaq: Token currency experiment, capital shift to Daulatabad
                   • Firoz Shah Tughlaq: Canals, public works
                
                4. **Sayyid Dynasty (1414 - 1451)**:
                   • Khizr Khan
                
                5. **Lodi Dynasty (1451 - 1526)**:
                   • Bahlul Lodi, Sikandar Lodi (founded Agra in 1504)
                   • Ibrahim Lodi defeated in First Battle of Panipat (1526) by Babur
            """.trimIndent(),
            noteType = "STANDARD",
            isLocked = true,
            sortOrder = 1
        )
    )

    private fun jsonOptions(vararg opts: String): String {
        val arr = JSONArray()
        opts.forEach { arr.put(it) }
        return arr.toString()
    }

    val initialQuestions = listOf(
        // Maths Questions
        QuestionEntity(
            id = "q_m_1",
            subjectId = "maths",
            topicId = "m_calc",
            questionText = "What is the derivative of f(x) = x³ with respect to x?",
            optionsJson = jsonOptions("3x", "3x²", "x²", "3x³"),
            correctAnswerIndex = 1,
            explanation = "By power rule: d/dx(xⁿ) = n·xⁿ⁻¹. Thus d/dx(x³) = 3x².",
            difficulty = "EASY"
        ),
        QuestionEntity(
            id = "q_m_2",
            subjectId = "maths",
            topicId = "m_calc",
            questionText = "What is the derivative of sin(2x)?",
            optionsJson = jsonOptions("cos(2x)", "2cos(2x)", "-2cos(2x)", "2sin(2x)"),
            correctAnswerIndex = 1,
            explanation = "Using chain rule: d/dx[sin(u)] = cos(u) · du/dx. For u = 2x, du/dx = 2. So answer is 2cos(2x).",
            difficulty = "MEDIUM"
        ),
        QuestionEntity(
            id = "q_m_3",
            subjectId = "maths",
            topicId = "m_alg",
            questionText = "What are the roots of the quadratic equation x² - 5x + 6 = 0?",
            optionsJson = jsonOptions("x = 2, 3", "x = -2, -3", "x = 1, 6", "x = -1, -6"),
            correctAnswerIndex = 0,
            explanation = "Factorizing: (x - 2)(x - 3) = 0, which gives x = 2 and x = 3.",
            difficulty = "EASY"
        ),
        QuestionEntity(
            id = "q_m_4",
            subjectId = "maths",
            topicId = "m_calc",
            questionText = "What is ∫ 2x dx?",
            optionsJson = jsonOptions("x² + C", "2x² + C", "x + C", "2 + C"),
            correctAnswerIndex = 0,
            explanation = "Integration of 2x with respect to x is 2*(x²/2) + C = x² + C.",
            difficulty = "EASY"
        ),
        // Physics Questions
        QuestionEntity(
            id = "q_p_1",
            subjectId = "physics",
            topicId = "p_mech",
            questionText = "Which law defines Force as the rate of change of momentum (F = dp/dt)?",
            optionsJson = jsonOptions("Newton's 1st Law", "Newton's 2nd Law", "Newton's 3rd Law", "Law of Gravitation"),
            correctAnswerIndex = 1,
            explanation = "Newton's second law states that rate of change of momentum is proportional to the applied force: F = dp/dt = ma.",
            difficulty = "EASY"
        ),
        QuestionEntity(
            id = "q_p_2",
            subjectId = "physics",
            topicId = "p_mech",
            questionText = "What is the SI unit of work and energy?",
            optionsJson = jsonOptions("Newton", "Watt", "Joule", "Pascal"),
            correctAnswerIndex = 2,
            explanation = "The SI unit of both work and energy is the Joule (J = N·m).",
            difficulty = "EASY"
        ),
        QuestionEntity(
            id = "q_p_3",
            subjectId = "physics",
            topicId = "p_em",
            questionText = "What is the speed of light in vacuum?",
            optionsJson = jsonOptions("3 × 10⁶ m/s", "3 × 10⁸ m/s", "3 × 10¹⁰ m/s", "3 × 10⁵ m/s"),
            correctAnswerIndex = 1,
            explanation = "The speed of light in vacuum (c) is approximately 3 × 10⁸ m/s.",
            difficulty = "EASY"
        ),
        // Table Questions
        QuestionEntity(
            id = "q_t_1",
            subjectId = "table",
            topicId = "t_6_to_10",
            questionText = "What is 7 × 8?",
            optionsJson = jsonOptions("54", "56", "58", "64"),
            correctAnswerIndex = 1,
            explanation = "7 × 8 = 56.",
            difficulty = "EASY"
        ),
        QuestionEntity(
            id = "q_t_2",
            subjectId = "table",
            topicId = "t_6_to_10",
            questionText = "What is 9 × 9?",
            optionsJson = jsonOptions("79", "81", "83", "99"),
            correctAnswerIndex = 1,
            explanation = "9 × 9 = 81.",
            difficulty = "EASY"
        ),
        QuestionEntity(
            id = "q_t_3",
            subjectId = "table",
            topicId = "t_6_to_10",
            questionText = "What is 8 × 6?",
            optionsJson = jsonOptions("46", "48", "52", "54"),
            correctAnswerIndex = 1,
            explanation = "8 × 6 = 48.",
            difficulty = "EASY"
        ),
        // Cube Questions
        QuestionEntity(
            id = "q_c_1",
            subjectId = "cube",
            topicId = "c_1_to_15",
            questionText = "What is the cube of 6 (6³)?",
            optionsJson = jsonOptions("196", "216", "256", "36"),
            correctAnswerIndex = 1,
            explanation = "6 × 6 × 6 = 216.",
            difficulty = "EASY"
        ),
        QuestionEntity(
            id = "q_c_2",
            subjectId = "cube",
            topicId = "c_1_to_15",
            questionText = "What is 12³?",
            optionsJson = jsonOptions("1440", "1728", "1824", "1628"),
            correctAnswerIndex = 1,
            explanation = "12³ = 12 × 12 × 12 = 144 × 12 = 1728.",
            difficulty = "MEDIUM"
        ),
        QuestionEntity(
            id = "q_c_3",
            subjectId = "cube",
            topicId = "c_1_to_15",
            questionText = "Which number's cube is 343?",
            optionsJson = jsonOptions("5", "6", "7", "8"),
            correctAnswerIndex = 2,
            explanation = "7 × 7 × 7 = 49 × 7 = 343.",
            difficulty = "EASY"
        ),
        // Medival History Questions
        QuestionEntity(
            id = "q_mh_1",
            subjectId = "medival",
            topicId = "mh_delhi",
            questionText = "Who was the first woman monarch of the Delhi Sultanate?",
            optionsJson = jsonOptions("Chand Bibi", "Razia Sultan", "Nur Jahan", "Rani Padmini"),
            correctAnswerIndex = 1,
            explanation = "Razia Sultan was the daughter of Iltutmish and ruled Delhi from 1236 to 1240 CE.",
            difficulty = "EASY"
        ),
        QuestionEntity(
            id = "q_mh_2",
            subjectId = "medival",
            topicId = "mh_delhi",
            questionText = "In which year did the First Battle of Panipat take place?",
            optionsJson = jsonOptions("1526", "1556", "1761", "1192"),
            correctAnswerIndex = 0,
            explanation = "The First Battle of Panipat took place on 21 April 1526 between Babur and Ibrahim Lodi.",
            difficulty = "EASY"
        ),
        QuestionEntity(
            id = "q_mh_3",
            subjectId = "medival",
            topicId = "mh_delhi",
            questionText = "Which Delhi Sultan introduced market control regulations and price caps?",
            optionsJson = jsonOptions("Balban", "Alauddin Khilji", "Muhammad bin Tughlaq", "Firoz Shah Tughlaq"),
            correctAnswerIndex = 1,
            explanation = "Alauddin Khilji instituted strict market control regulations, fixing commodity prices and appointing Shahna-i-Mandi.",
            difficulty = "MEDIUM"
        )
    )
}
