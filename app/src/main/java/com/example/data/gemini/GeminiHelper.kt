package com.example.data.gemini

import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object GeminiHelper {
    private const val TAG = "GeminiHelper"

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    /**
     * Determines if a real Gemini API Key is available.
     */
    fun isApiKeyAvailable(): Boolean {
        val key = BuildConfig.GEMINI_API_KEY
        return key.isNotEmpty() && key != "MY_GEMINI_API_KEY" && !key.contains("PLACEHOLDER")
    }

    /**
     * Calls Gemini 3.5 Flash REST API or falls back to simulated responses if key is missing.
     */
    suspend fun generateBroadcastCreative(
        taskType: String, // "caption", "hashtags", "title", "caption_video", "transcribe"
        inputText: String,
        mediaType: String = "" // "video", "podcast", "image", "text"
    ): String = withContext(Dispatchers.IO) {
        if (!isApiKeyAvailable()) {
            return@withContext getLocalSimulatedResponse(taskType, inputText, mediaType)
        }

        val prompt = when (taskType) {
            "caption" -> "Create an engaging, punchy, creator-first social caption based on this topic: \"$inputText\". It should feel slightly futuristic, digital-first, fast-paced, and fit a multi-format broadcast social network. Keep it under 150 characters."
            "hashtags" -> "Suggest 4 trending, high-velocity hashtag tags for a broadcast post about: \"$inputText\". Return only the space-separated hashtags, e.g. #Tech #AI #Broadcast."
            "title" -> "Optimize this post title for maximum reach and cinematic velocity on a creator network: \"$inputText\". Give me 1 optimized title, short, catchy, under 50 characters, do not include quotes."
            "caption_video" -> "Generate custom video subtitles or an intro speech description from this video prompt or outline: \"$inputText\". Fast-paced social reel style."
            "transcribe" -> "Simulate a highly detailed transcription of this audio/podcast episode outline: \"$inputText\". Break it down as a neat summary segment timestamps style."
            else -> "Optimize this creator text: \"$inputText\""
        }

        try {
            val key = BuildConfig.GEMINI_API_KEY
            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$key"

            // Construct manual JSON body to ensure maximum compilation robustness without dependency complications
            val requestBodyJson = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", prompt)
                            })
                        })
                    })
                })
            }

            val requestBody = requestBodyJson.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errBody = response.body?.string() ?: ""
                    Log.e(TAG, "Gemini API error request failed: ${response.code} - $errBody")
                    return@withContext getLocalSimulatedResponse(taskType, inputText, mediaType) + "\n*(API responded with error ${response.code}, showing simulation)*"
                }

                val responseBody = response.body?.string() ?: return@withContext "Error reading Gemini response body"
                val jsonResponse = JSONObject(responseBody)
                val candidates = jsonResponse.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val parts = candidates.getJSONObject(0)
                        .optJSONObject("content")
                        ?.optJSONArray("parts")
                    if (parts != null && parts.length() > 0) {
                        return@withContext parts.getJSONObject(0).optString("text", "No text candidate returned")
                    }
                }
                return@withContext "No response candidates parsed"
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception calling Gemini API: ${e.message}", e)
            return@withContext getLocalSimulatedResponse(taskType, inputText, mediaType) + "\n*(Network offline or expired key, showing simulation)*"
        }
    }

    private fun getLocalSimulatedResponse(taskType: String, inputText: String, mediaType: String): String {
        val normalized = inputText.trim()
        val baseText = if (normalized.isEmpty()) "digital aesthetics" else normalized

        return when (taskType) {
            "caption" -> {
                "🚀 [Broadcast Stream Input] Just deployed a new layer of $baseText. Real-time feedback in full motion. Who's tuning in? #RealTimeBroadcasting"
            }
            "hashtags" -> {
                val tags = mutableListOf("#Broadcaster")
                if (baseText.length > 3) {
                    val cleaned = baseText.replace(Regex("[^a-zA-Z0-9]"), "").take(12)
                    tags.add("#$cleaned")
                }
                tags.add("#CreatorFirst")
                tags.add("#FutureFlow")
                tags.joinToString(" ")
            }
            "title" -> {
                "Unlocking the Spectrum: $baseText"
            }
            "caption_video" -> {
                "🎥 Clip Preview:\n" +
                "0:01 - \"We are looking directly at $baseText.\"\n" +
                "0:12 - \"This signals a complete protocol shift in creator-to-audience broadcasting.\""
            }
            "transcribe" -> {
                "📻 [Signal Transcribed]\n" +
                "\"Welcome to the channel. Today, we're deep-diving into why $baseText is taking over our social streams... Let's analyze the velocity of this trend.\""
            }
            else -> "Optimized: $inputText"
        }
    }
}
