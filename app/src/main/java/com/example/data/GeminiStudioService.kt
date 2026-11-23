package com.example.data

import com.example.model.DirectorShotPlan
import com.example.model.InspectorReport
import com.example.model.ShotBreakdown
import com.example.model.TailorCopyPack
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

class GeminiStudioService {

    private val directorModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            systemInstruction = content {
                text(
                    "You are CineCraft AI Director, a concise, surgical mobile film production co-pilot. " +
                        "Provide actionable framing, lighting, pacing, voiceover, and editing direction in crisp studio language."
                )
            }
        )
    }

    private val shotPlanModel by lazy {
        val schema = Schema.obj(
            properties = mapOf(
                "title" to Schema.string(description = "Short production sequence title"),
                "logline" to Schema.string(description = "One-sentence cinematic premise"),
                "shots" to Schema.array(
                    description = "3 to 4 ordered shots",
                    items = Schema.obj(
                        properties = mapOf(
                            "shotCode" to Schema.string(description = "Code like S01, S02"),
                            "framing" to Schema.string(description = "Lens & camera movement e.g. 24mm Dolly In"),
                            "durationSec" to Schema.integer(description = "Duration in seconds between 2 and 12"),
                            "visualPrompt" to Schema.string(description = "Concise visual scene description")
                        )
                    )
                )
            )
        )
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = schema
            }
        )
    }

    private val inspectorModel by lazy {
        val schema = Schema.obj(
            properties = mapOf(
                "healthScore" to Schema.integer(description = "Production readiness score from 0 to 100"),
                "summary" to Schema.string(description = "Brief 2-sentence studio audit summary"),
                "recommendations" to Schema.array(
                    description = "3 concise actionable fixes or upgrades",
                    items = Schema.string()
                )
            )
        )
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = schema
            }
        )
    }

    private val tailorCopyModel by lazy {
        val schema = Schema.obj(
            properties = mapOf(
                "headline" to Schema.string(description = "Punchy hook headline under 60 chars"),
                "caption" to Schema.string(description = "Social post description under 160 chars"),
                "hashtags" to Schema.array(
                    description = "4 relevant hashtags without spaces",
                    items = Schema.string()
                )
            )
        )
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = schema
            }
        )
    }

    fun streamDirectorReply(prompt: String, contextSummary: String): Flow<String> = flow {
        val fullPrompt = """
            Current Studio Context:
            $contextSummary
            
            User Request:
            $prompt
        """.trimIndent()

        val accumulator = StringBuilder()
        withTimeout(60_000L) {
            directorModel.generateContentStream(fullPrompt).collect { chunk ->
                val text = chunk.text.orEmpty()
                if (text.isNotEmpty()) {
                    accumulator.append(text)
                    emit(accumulator.toString())
                }
            }
        }
        if (accumulator.isBlank()) {
            throw IllegalStateException("Gemini returned an empty response.")
        }
    }

    suspend fun generateShotPlan(concept: String, aspectLabel: String): DirectorShotPlan {
        return try {
            val response = withTimeout(60_000L) {
                shotPlanModel.generateContent(
                    "Build a 3-shot cinematic storyboard sequence in $aspectLabel aspect ratio for: $concept"
                )
            }
            val raw = response.text ?: throw IllegalStateException("Model returned empty shot plan.")
            val json = JSONObject(raw)
            val shotsArray = json.getJSONArray("shots")
            val shots = (0 until shotsArray.length()).map { idx ->
                val item = shotsArray.getJSONObject(idx)
                ShotBreakdown(
                    shotCode = item.getString("shotCode"),
                    framing = item.getString("framing"),
                    durationSec = item.optInt("durationSec", 4).coerceIn(2, 12),
                    visualPrompt = item.getString("visualPrompt")
                )
            }
            DirectorShotPlan(
                title = json.getString("title"),
                logline = json.getString("logline"),
                shots = shots
            )
        } catch (e: CancellationException) {
            throw e
        }
    }

    suspend fun polishVoiceLine(lineText: String, tone: String): String {
        return try {
            val response = withTimeout(45_000L) {
                directorModel.generateContent(
                    "Rewrite this voiceover line in a $tone studio tone so it sounds crisp and cinematic. " +
                        "Return ONLY the revised single line without quotes:\n$lineText"
                )
            }
            response.text?.trim()?.takeIf { it.isNotEmpty() }
                ?: throw IllegalStateException("Empty voiceover polish response.")
        } catch (e: CancellationException) {
            throw e
        }
    }

    suspend fun inspectProject(projectSummary: String): InspectorReport {
        return try {
            val response = withTimeout(45_000L) {
                inspectorModel.generateContent(
                    "Audit this mobile video production project and return readiness score, summary, and 3 tips:\n$projectSummary"
                )
            }
            val raw = response.text ?: throw IllegalStateException("Empty inspector response.")
            val json = JSONObject(raw)
            val recsJson = json.getJSONArray("recommendations")
            val recs = (0 until recsJson.length()).map { recsJson.getString(it) }
            InspectorReport(
                healthScore = json.optInt("healthScore", 88).coerceIn(0, 100),
                summary = json.getString("summary"),
                recommendations = recs
            )
        } catch (e: CancellationException) {
            throw e
        }
    }

    suspend fun generateTailorCopy(
        platform: String,
        aspectRatio: String,
        projectSummary: String
    ): TailorCopyPack {
        return try {
            val response = withTimeout(45_000L) {
                tailorCopyModel.generateContent(
                    "Create launch copy for $platform ($aspectRatio) based on this CineCraft edit:\n$projectSummary"
                )
            }
            val raw = response.text ?: throw IllegalStateException("Empty copy response.")
            val json = JSONObject(raw)
            val tagsJson = json.getJSONArray("hashtags")
            val tags = (0 until tagsJson.length()).map { idx ->
                val rawTag = tagsJson.getString(idx).trim()
                if (rawTag.startsWith("#")) rawTag else "#$rawTag"
            }
            TailorCopyPack(
                headline = json.getString("headline"),
                caption = json.getString("caption"),
                hashtags = tags
            )
        } catch (e: CancellationException) {
            throw e
        }
    }

    suspend fun draftDramaScript(
        dramaTitle: String,
        episode: String,
        targetSeconds: Int
    ): String {
        val targetWords = (targetSeconds * 2.2f).toInt().coerceIn(18, 220)
        return try {
            val response = withTimeout(45_000L) {
                directorModel.generateContent(
                    "Write a gripping Bengali voiceover recap script for drama '$dramaTitle' ($episode) " +
                        "in approximately $targetWords words (~${targetSeconds}s spoken). " +
                        "Return ONLY the clean Bengali narration script text."
                )
            }
            response.text?.trim()?.takeIf { it.isNotEmpty() }
                ?: throw IllegalStateException("Empty drama script response.")
        } catch (e: CancellationException) {
            throw e
        }
    }

    suspend fun adaptScriptToDuration(
        currentScript: String,
        targetSeconds: Int
    ): String {
        val targetWords = (targetSeconds * 2.2f).toInt().coerceIn(15, 240)
        return try {
            val response = withTimeout(45_000L) {
                directorModel.generateContent(
                    "Condense or expand the following narration script so it reads naturally in $targetSeconds seconds " +
                        "(around $targetWords words), keeping the same language and dramatic tone. " +
                        "Return ONLY the adapted script text:\n$currentScript"
                )
            }
            response.text?.trim()?.takeIf { it.isNotEmpty() }
                ?: throw IllegalStateException("Empty script adaptation response.")
        } catch (e: CancellationException) {
            throw e
        }
    }
}
