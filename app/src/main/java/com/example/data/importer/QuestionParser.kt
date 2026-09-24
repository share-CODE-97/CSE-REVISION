package com.example.data.importer

import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class ParsedQuestion(
    val id: String,
    val subjectTitle: String,
    val subtopicTitle: String?,
    val questionText: String,
    val options: List<String>,
    val correctAnswerIndex: Int,
    val explanation: String,
    val difficulty: String
)

object QuestionParser {

    /**
     * Parses questions from JS or JSON content.
     * Handles:
     * - JS variable assignments: `const questions = [...]`, `export default [...]`, `module.exports = [...]`
     * - JS comments: `//` and `/* ... */`
     * - Single quotes and unquoted keys
     * - Trailing commas
     * - Various answer representations (exact text, index, letter A/B/C/D)
     */
    fun parseQuestions(rawContent: String, fallbackSubject: String = "General"): List<ParsedQuestion> {
        val trimmed = rawContent.trim()
        if (trimmed.isEmpty()) return emptyList()

        // 1. Try standard / sanitized JSON array parsing first
        val jsonResult = tryParseAsJson(trimmed, fallbackSubject)
        if (jsonResult.isNotEmpty()) {
            return jsonResult
        }

        // 2. Fallback to resilient regex / object scanner for JS files with custom syntax
        return parseWithObjectScanner(trimmed, fallbackSubject)
    }

    private fun tryParseAsJson(raw: String, fallbackSubject: String): List<ParsedQuestion> {
        try {
            val cleaned = sanitizeJsToJson(raw)
            val arrayStart = cleaned.indexOf('[')
            val arrayEnd = cleaned.lastIndexOf(']')

            if (arrayStart != -1 && arrayEnd > arrayStart) {
                val arrayStr = cleaned.substring(arrayStart, arrayEnd + 1)
                val jsonArray = JSONArray(arrayStr)
                val list = ArrayList<ParsedQuestion>(jsonArray.length())

                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.optJSONObject(i) ?: continue
                    val parsed = parseJsonObject(obj, fallbackSubject, i)
                    if (parsed != null) {
                        list.add(parsed)
                    }
                }
                if (list.isNotEmpty()) return list
            }
        } catch (_: Exception) {
            // Fall through to object scanner
        }
        return emptyList()
    }

    fun sanitizeJsToJson(js: String): String {
        var s = js

        // Remove single-line comments // ...
        s = s.replace(Regex("""(?m)^[ \t]*//.*$"""), "")
        s = s.replace(Regex("""(?<=[^:])//.*$"""), "")

        // Remove multi-line comments /* ... */
        s = s.replace(Regex("""/\*[\s\S]*?\*/"""), "")

        // Remove variable declarations like 'const questions =', 'var q =', 'export default', 'module.exports ='
        s = s.replace(Regex("""^(?:export\s+default|module\.exports\s*=|const\s+\w+\s*=|let\s+\w+\s*=|var\s+\w+\s*=)\s*"""), "")

        // Remove trailing semicolons
        s = s.trimEnd(';', ' ', '\n', '\r', '\t')

        // Remove trailing commas before } or ]
        s = s.replace(Regex(""",\s*([}\]])"""), "$1")

        // Quotify unquoted keys (e.g. id: -> "id":)
        s = s.replace(Regex("""([{,]\s*)([a-zA-Z_$][a-zA-Z0-9_$]*)\s*:"""), "$1\"$2\":")

        return s
    }

    private fun parseJsonObject(obj: JSONObject, fallbackSubject: String, index: Int): ParsedQuestion? {
        val questionText = obj.optString("question", "")
            .ifEmpty { obj.optString("questionText", "") }
            .ifEmpty { obj.optString("q", "") }
            .ifEmpty { obj.optString("prompt", "") }
            .trim()

        if (questionText.isEmpty()) return null

        // Options
        val optionsList = mutableListOf<String>()
        val optionsArr = obj.optJSONArray("options")
            ?: obj.optJSONArray("choices")
            ?: obj.optJSONArray("answers")

        if (optionsArr != null) {
            for (j in 0 until optionsArr.length()) {
                optionsList.add(optionsArr.optString(j, "").trim())
            }
        } else {
            // Check if options are an object like { "A": "...", "B": "..." }
            val optionsObj = obj.optJSONObject("options") ?: obj.optJSONObject("choices")
            if (optionsObj != null) {
                val keys = listOf("A", "B", "C", "D", "E", "a", "b", "c", "d", "e", "0", "1", "2", "3")
                for (k in keys) {
                    if (optionsObj.has(k)) {
                        optionsList.add(optionsObj.optString(k, "").trim())
                    }
                }
            }
        }

        if (optionsList.size < 2) {
            return null // A valid MCQ needs at least 2 options
        }

        // Answer
        val rawAnswer = obj.opt("answer") ?: obj.opt("ans") ?: obj.opt("correctAnswer") ?: obj.opt("correct")
        val correctIndex = resolveCorrectAnswerIndex(rawAnswer, optionsList)

        // Subject & Subtopic
        val subject = obj.optString("subject", "")
            .ifEmpty { obj.optString("subjectName", "") }
            .ifEmpty { obj.optString("category", "") }
            .trim()
            .ifEmpty { fallbackSubject }

        val subtopic = obj.optString("subtopic", "")
            .ifEmpty { obj.optString("subTopic", "") }
            .ifEmpty { obj.optString("topic", "") }
            .ifEmpty { obj.optString("chapter", "") }
            .trim()
            .ifEmpty { null }

        // ID
        val rawId = obj.optString("id", "")
            .ifEmpty { obj.optString("questionId", "") }
            .ifEmpty { obj.optString("_id", "") }
            .trim()

        val id = if (rawId.isNotEmpty()) {
            rawId
        } else {
            "Q_${Math.abs((questionText + optionsList.firstOrNull()).hashCode()).toString(16)}_${index}"
        }

        val explanation = obj.optString("explanation", "")
            .ifEmpty { obj.optString("exp", "") }
            .ifEmpty { obj.optString("solution", "") }
            .trim()

        val difficulty = obj.optString("difficulty", "MEDIUM").uppercase().let {
            if (it in listOf("EASY", "MEDIUM", "HARD")) it else "MEDIUM"
        }

        return ParsedQuestion(
            id = id,
            subjectTitle = subject,
            subtopicTitle = subtopic,
            questionText = questionText,
            options = optionsList,
            correctAnswerIndex = correctIndex,
            explanation = explanation,
            difficulty = difficulty
        )
    }

    /**
     * Resilient scanner for files with non-standard JS syntax, unquoted keys, single quotes, etc.
     */
    private fun parseWithObjectScanner(content: String, fallbackSubject: String): List<ParsedQuestion> {
        val list = mutableListOf<ParsedQuestion>()
        var searchIndex = 0
        var itemCounter = 0

        while (searchIndex < content.length) {
            val openBrace = content.indexOf('{', searchIndex)
            if (openBrace == -1) break

            // Find matching closing brace
            var depth = 1
            var closeBrace = -1
            var inSingleQuote = false
            var inDoubleQuote = false
            var inBacktick = false
            var i = openBrace + 1

            while (i < content.length && depth > 0) {
                val c = content[i]
                val prev = if (i > 0) content[i - 1] else ' '

                if (prev != '\\') {
                    if (c == '\'' && !inDoubleQuote && !inBacktick) {
                        inSingleQuote = !inSingleQuote
                    } else if (c == '"' && !inSingleQuote && !inBacktick) {
                        inDoubleQuote = !inDoubleQuote
                    } else if (c == '`' && !inSingleQuote && !inDoubleQuote) {
                        inBacktick = !inBacktick
                    } else if (!inSingleQuote && !inDoubleQuote && !inBacktick) {
                        if (c == '{') depth++
                        else if (c == '}') {
                            depth--
                            if (depth == 0) {
                                closeBrace = i
                                break
                            }
                        }
                    }
                }
                i++
            }

            if (closeBrace != -1) {
                val objectBlock = content.substring(openBrace, closeBrace + 1)
                val parsed = parseSingleObjectBlock(objectBlock, fallbackSubject, itemCounter)
                if (parsed != null) {
                    list.add(parsed)
                    itemCounter++
                }
                searchIndex = closeBrace + 1
            } else {
                break
            }
        }

        return list
    }

    private fun parseSingleObjectBlock(block: String, fallbackSubject: String, counter: Int): ParsedQuestion? {
        val questionText = extractStringField(block, listOf("question", "questionText", "q", "prompt"))
            ?: return null

        val optionsList = extractArrayField(block, listOf("options", "choices", "answers"))
        if (optionsList.size < 2) return null

        val rawAnswer = extractRawField(block, listOf("answer", "ans", "correctAnswer", "correct"))
        val correctIndex = resolveCorrectAnswerIndex(rawAnswer, optionsList)

        val id = extractStringField(block, listOf("id", "questionId", "_id"))
            ?: "Q_${Math.abs((questionText + optionsList.firstOrNull()).hashCode()).toString(16)}_${counter}"

        val subject = extractStringField(block, listOf("subject", "subjectName", "category"))
            ?: fallbackSubject

        val subtopic = extractStringField(block, listOf("subtopic", "subTopic", "topic", "chapter"))

        val explanation = extractStringField(block, listOf("explanation", "exp", "solution", "reason")) ?: ""

        val difficulty = extractStringField(block, listOf("difficulty", "level"))?.uppercase()?.let {
            if (it in listOf("EASY", "MEDIUM", "HARD")) it else "MEDIUM"
        } ?: "MEDIUM"

        return ParsedQuestion(
            id = id.trim(),
            subjectTitle = subject.trim(),
            subtopicTitle = subtopic?.trim(),
            questionText = questionText.trim(),
            options = optionsList,
            correctAnswerIndex = correctIndex,
            explanation = explanation.trim(),
            difficulty = difficulty
        )
    }

    private fun extractStringField(block: String, keys: List<String>): String? {
        for (key in keys) {
            val pattern = Regex("""(?:"$key"|'$key'|\b$key\b)\s*:\s*(?:"((?:[^"\\]|\\.)*)"|'((?:[^'\\]|\\.)*)'|`((?:[^`\\]|\\.)*)`)""")
            val match = pattern.find(block)
            if (match != null) {
                val value = match.groups[1]?.value
                    ?: match.groups[2]?.value
                    ?: match.groups[3]?.value
                if (value != null) {
                    return unescapeString(value)
                }
            }
        }
        return null
    }

    private fun extractRawField(block: String, keys: List<String>): String? {
        for (key in keys) {
            val pattern = Regex("""(?:"$key"|'$key'|\b$key\b)\s*:\s*(?:"((?:[^"\\]|\\.)*)"|'((?:[^'\\]|\\.)*)'|([a-zA-Z0-9_]+))""")
            val match = pattern.find(block)
            if (match != null) {
                val value = match.groups[1]?.value
                    ?: match.groups[2]?.value
                    ?: match.groups[3]?.value
                if (value != null) {
                    return unescapeString(value)
                }
            }
        }
        return null
    }

    private fun extractArrayField(block: String, keys: List<String>): List<String> {
        for (key in keys) {
            val keyIndex = block.indexOf(key)
            if (keyIndex == -1) continue

            val bracketStart = block.indexOf('[', keyIndex)
            if (bracketStart == -1) continue

            val bracketEnd = block.indexOf(']', bracketStart)
            if (bracketEnd == -1) continue

            val arrayContent = block.substring(bracketStart + 1, bracketEnd)
            val items = mutableListOf<String>()

            // Extract quoted items
            val itemRegex = Regex("""(?:"((?:[^"\\]|\\.)*)"|'((?:[^'\\]|\\.)*)'|`((?:[^`\\]|\\.)*)`)""")
            for (match in itemRegex.findAll(arrayContent)) {
                val item = match.groups[1]?.value
                    ?: match.groups[2]?.value
                    ?: match.groups[3]?.value
                if (item != null) {
                    items.add(unescapeString(item).trim())
                }
            }

            if (items.isNotEmpty()) return items
        }
        return emptyList()
    }

    private fun unescapeString(str: String): String {
        return str
            .replace("\\\"", "\"")
            .replace("\\'", "'")
            .replace("\\n", "\n")
            .replace("\\t", "\t")
            .replace("\\r", "\r")
            .replace("\\\\", "\\")
    }

    /**
     * Resolves the correct option index from diverse answer formats:
     * - Exact string match with one of the options (e.g. "Maps IP addresses to MAC addresses")
     * - Letter: "A" -> 0, "B" -> 1, "C" -> 2, "D" -> 3, "E" -> 4
     * - Number: 0, 1, 2, 3 (0-based) or 1, 2, 3, 4 (1-based)
     * - "Option A", "Option 1"
     */
    fun resolveCorrectAnswerIndex(rawAnswer: Any?, options: List<String>): Int {
        if (rawAnswer == null || options.isEmpty()) return 0

        val strAnswer = when (rawAnswer) {
            is Number -> rawAnswer.toInt().toString()
            else -> rawAnswer.toString().trim()
        }

        // 1. Exact string match against options (case-insensitive & trimmed)
        val exactIndex = options.indexOfFirst { it.trim().equals(strAnswer, ignoreCase = true) }
        if (exactIndex != -1) return exactIndex

        // 2. Contains match if length is substantial
        if (strAnswer.length >= 4) {
            val partialIndex = options.indexOfFirst {
                it.trim().contains(strAnswer, ignoreCase = true) || strAnswer.contains(it.trim(), ignoreCase = true)
            }
            if (partialIndex != -1) return partialIndex
        }

        // 3. Single Letter (A, B, C, D, E)
        val singleLetter = strAnswer.uppercase()
        when (singleLetter) {
            "A" -> return 0.coerceAtMost(options.size - 1)
            "B" -> return 1.coerceAtMost(options.size - 1)
            "C" -> return 2.coerceAtMost(options.size - 1)
            "D" -> return 3.coerceAtMost(options.size - 1)
            "E" -> return 4.coerceAtMost(options.size - 1)
        }

        // 4. Matches "Option A", "Option B", etc.
        val optionLetterMatch = Regex("""(?:option|choice)\s*([A-Ea-e])""", RegexOption.IGNORE_CASE).find(strAnswer)
        if (optionLetterMatch != null) {
            val letter = optionLetterMatch.groupValues[1].uppercase()
            val idx = letter[0] - 'A'
            if (idx in options.indices) return idx
        }

        // 5. Numeric index (check 0-based vs 1-based)
        val num = strAnswer.toIntOrNull()
        if (num != null) {
            if (num in options.indices) {
                return num // 0-based match (e.g. 0, 1, 2, 3)
            }
            if (num - 1 in options.indices) {
                return num - 1 // 1-based match (e.g. 1, 2, 3, 4)
            }
        }

        return 0
    }
}
