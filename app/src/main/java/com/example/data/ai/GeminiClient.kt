package com.example.data.ai

import android.graphics.Bitmap
import android.util.Base64
import com.example.BuildConfig
import com.example.domain.model.AiGenerationMode
import com.example.preferences.SecureKeyStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit

sealed class AiResult {
    data class Success(val text: String, val modelUsed: String) : AiResult()
    data class QuotaExceeded(val message: String) : AiResult()
    data class Error(val message: String, val isKeyMissing: Boolean = false) : AiResult()
}

class GeminiClient(
    private val secureKeyStorage: SecureKeyStorage
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    fun getEffectiveApiKey(): String? {
        val byok = secureKeyStorage.getApiKey()
        if (!byok.isNullOrBlank()) return byok
        val buildKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Throwable) {
            ""
        }
        return if (buildKey.isNotBlank() && buildKey != "MY_GEMINI_API_KEY") buildKey else null
    }

    suspend fun testConnection(apiKey: String, model: String = "gemini-3.5-flash"): Result<String> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("API Key cannot be empty"))
        }

        try {
            val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"
            val jsonBody = JSONObject().apply {
                val contents = JSONArray().apply {
                    val content = JSONObject().apply {
                        val parts = JSONArray().apply {
                            put(JSONObject().apply { put("text", "Respond with 'Connection successful!' in 3 words.") })
                        }
                        put("parts", parts)
                    }
                    put(content)
                }
                put("contents", contents)
            }

            val request = Request.Builder()
                .url(url)
                .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            val code = response.code
            val bodyString = response.body?.string() ?: ""

            if (code == 200) {
                val json = JSONObject(bodyString)
                val candidates = json.optJSONArray("candidates")
                val text = candidates?.optJSONObject(0)
                    ?.optJSONObject("content")
                    ?.optJSONArray("parts")
                    ?.optJSONObject(0)
                    ?.optString("text", "Connected successfully!")
                    ?: "Connected successfully!"
                Result.success(text.trim())
            } else if (code == 429) {
                Result.failure(Exception("HTTP 429: Gemini quota limit reached."))
            } else {
                val errMsg = try {
                    JSONObject(bodyString).optJSONObject("error")?.optString("message") ?: "HTTP $code"
                } catch (e: Exception) {
                    "HTTP $code: $bodyString"
                }
                Result.failure(Exception(errMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun generateStudyContent(
        prompt: String,
        imageBitmap: Bitmap? = null,
        mode: AiGenerationMode,
        model: String = "gemini-3.5-flash"
    ): AiResult = withContext(Dispatchers.IO) {
        val apiKey = getEffectiveApiKey()
        if (apiKey.isNullOrBlank()) {
            return@withContext AiResult.Error(
                message = "No Gemini API Key found. Please add your key in Settings -> AI Configuration or choose 'Generate Offline Notes'.",
                isKeyMissing = true
            )
        }

        val systemInstruction = when (mode) {
            AiGenerationMode.MIND_MAP -> """
                You are an expert study assistant. Generate a structured Mind Map in JSON format for the provided material.
                Format as:
                {
                  "id": "root",
                  "title": "<Main Topic>",
                  "notes": "<Brief Overview>",
                  "children": [
                    {
                      "id": "c1",
                      "title": "<Subconcept 1>",
                      "notes": "<Explanation>",
                      "children": [
                        { "id": "c1_1", "title": "<Detail>", "notes": "<Note>" }
                      ]
                    }
                  ]
                }
                Output ONLY valid JSON.
            """.trimIndent()
            AiGenerationMode.SIMPLIFY -> """
                You are an expert tutor. Explain the topic simply using the Feynman technique (Explain Like I'm 5 / ELI5).
                Use clear everyday analogies, plain language, and intuitive breakdowns.
                Include:
                1. Core Idea in One Sentence
                2. The Simple Analogy
                3. Step-by-Step Explanation
                4. Why It Matters
                5. Common Misconceptions
            """.trimIndent()
            AiGenerationMode.CHEAT_SHEET -> """
                You are an exam prep specialist. Create a High-Yield Cheat Sheet for rapid revision.
                Include:
                • Key Formulas / Definitions
                • Critical Dates & Facts
                • Step-by-Step Algorithms / Procedures
                • Common Exam Pitfalls & Traps
                • Quick Memory Tricks / Mnemonics
            """.trimIndent()
            AiGenerationMode.CUSTOM -> """
                You are a personal academic study assistant. Create a comprehensive, well-structured study note with clear headings, key takeaways, and revision questions based on the user's prompt.
            """.trimIndent()
        }

        try {
            val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"

            val partsArray = JSONArray()
            partsArray.put(JSONObject().apply {
                put("text", "$systemInstruction\n\nStudy Material / Query:\n$prompt")
            })

            if (imageBitmap != null) {
                val stream = ByteArrayOutputStream()
                imageBitmap.compress(Bitmap.CompressFormat.JPEG, 85, stream)
                val base64 = Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)
                partsArray.put(JSONObject().apply {
                    put("inlineData", JSONObject().apply {
                        put("mimeType", "image/jpeg")
                        put("data", base64)
                    })
                })
            }

            val requestJson = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", partsArray)
                    })
                })
            }

            val request = Request.Builder()
                .url(url)
                .post(requestJson.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            val code = response.code
            val bodyString = response.body?.string() ?: ""

            if (code == 200) {
                val json = JSONObject(bodyString)
                val candidates = json.optJSONArray("candidates")
                val text = candidates?.optJSONObject(0)
                    ?.optJSONObject("content")
                    ?.optJSONArray("parts")
                    ?.optJSONObject(0)
                    ?.optString("text")
                if (!text.isNullOrBlank()) {
                    AiResult.Success(text.trim(), model)
                } else {
                    AiResult.Error("No content received from AI model.")
                }
            } else if (code == 429) {
                AiResult.QuotaExceeded(
                    "Gemini quota has been reached.\n\nYou can:\n• Change API key\n• Change model\n• Generate offline notes\n• Try again later"
                )
            } else {
                val errMsg = try {
                    JSONObject(bodyString).optJSONObject("error")?.optString("message") ?: "HTTP error $code"
                } catch (e: Exception) {
                    "HTTP $code: $bodyString"
                }
                AiResult.Error("API Error ($code): $errMsg")
            }
        } catch (e: IOException) {
            AiResult.Error("Network error: Check internet connection or use offline notes generator.")
        } catch (e: Exception) {
            AiResult.Error("Error: ${e.message}")
        }
    }

    /**
     * Offline local structured note generator when internet/Gemini is unavailable.
     */
    fun generateOfflineStructuredTemplate(topicTitle: String, inputDetails: String, mode: AiGenerationMode): String {
        val title = if (topicTitle.isBlank()) "Study Topic" else topicTitle.trim()
        val details = if (inputDetails.isBlank()) "Key points to be added by student." else inputDetails.trim()

        return when (mode) {
            AiGenerationMode.MIND_MAP -> """
                {
                  "id": "root",
                  "title": "$title",
                  "notes": "Offline Mind Map Template",
                  "children": [
                    {
                      "id": "branch_1",
                      "title": "Fundamental Concepts",
                      "notes": "$details",
                      "children": [
                        { "id": "sub_1", "title": "Definition & Scope", "notes": "Write core definitions here" },
                        { "id": "sub_2", "title": "Core Formula / Rule", "notes": "Write key formulas here" }
                      ]
                    },
                    {
                      "id": "branch_2",
                      "title": "Applications & Examples",
                      "notes": "Practical use cases",
                      "children": [
                        { "id": "sub_3", "title": "Example Case 1", "notes": "Step-by-step example" },
                        { "id": "sub_4", "title": "Edge Cases", "notes": "Things to watch out for" }
                      ]
                    },
                    {
                      "id": "branch_3",
                      "title": "Exam Revision Points",
                      "notes": "High yield review",
                      "children": [
                        { "id": "sub_5", "title": "Common Questions", "notes": "Frequently tested areas" }
                      ]
                    }
                  ]
                }
            """.trimIndent()

            AiGenerationMode.SIMPLIFY -> """
                # $title (Simplified / ELI5)
                [Generated via Local Structured Offline Template]

                ## 1. The Core Idea in 30 Seconds
                $details

                ## 2. Real-World Analogy
                Imagine $title like a everyday system where inputs produce predictable outputs under specific conditions.

                ## 3. Step-by-Step Breakdown
                1. **First Principle**: What is the foundation?
                2. **The Mechanism**: How does it actually work?
                3. **The Result**: What outcome is produced?

                ## 4. Why This Matters
                Understanding this concept connects directly with higher-level topics and practical problem solving.

                ## 5. Quick Self-Test
                • Can you explain this to a 10-year-old in under 2 minutes?
            """.trimIndent()

            AiGenerationMode.CHEAT_SHEET -> """
                # High-Yield Cheat Sheet: $title
                [Generated via Local Structured Offline Template]

                ## Key Definitions & Formulas
                • **Primary Definition**: $details
                • **Key Formula**: [Formula here]
                • **Units & Dimensions**: [SI Units here]

                ## Critical Facts & Classifications
                • Point A: Core mechanism
                • Point B: Standard operational condition
                • Point C: Crucial limitation

                ## Exam Pitfalls & Traps
                ⚠️ Watch out for sign conventions and boundary assumptions.
                ⚠️ Do not confuse similar definitions.

                ## Mnemonics & Quick Revision
                • Memory hook: [Add custom mnemonic here]
            """.trimIndent()

            AiGenerationMode.CUSTOM -> """
                # Study Notes: $title
                [Generated via Local Structured Offline Template]

                ## Overview
                $details

                ## Key Points
                • Point 1: Fundamental principle
                • Point 2: Essential properties and behavior
                • Point 3: Important theorems or applications

                ## Important Terms & Definitions
                • **Term 1**: Detailed explanation
                • **Term 2**: Detailed explanation

                ## Examples & Problem Solving
                • Example Problem:
                • Solution Steps:

                ## Quick Revision Summary
                • Master the core definitions first.
                • Re-solve key numericals and derivations.
            """.trimIndent()
        }
    }
}
