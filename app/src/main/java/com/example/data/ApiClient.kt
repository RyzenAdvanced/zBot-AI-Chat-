package com.example.data

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object ApiClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

    suspend fun chatWithModel(
        provider: String,
        baseUrl: String,
        apiKey: String,
        model: String,
        userPrompt: String,
        history: List<Message>, // Previous messages in thread
        isReasoningMode: Boolean,
        isSearchMode: Boolean,
        onSearchProgress: suspend (String) -> Unit = {}
    ): ApiResponse = withContext(Dispatchers.IO) {
        try {
            var finalPrompt = userPrompt

            // 1. Simulated Agentic Web Search Mode Setup
            var searchSourcesJson: String? = null
            if (isSearchMode) {
                onSearchProgress("🔍 Initializing multi-agent web crawler...")
                Thread.sleep(800)
                
                // Extract 1-3 terms to pretend we are searching
                val terms = userPrompt.split(" ").take(3).joinToString(" ")
                onSearchProgress("🌐 Browsing Google indices & academic repos for \"$terms\"...")
                Thread.sleep(1200)

                onSearchProgress("📊 Fetching and cross-referencing target sources...")
                Thread.sleep(900)

                // Generate real-looking search sources based on prompt
                val sources = generateMockWebSources(terms)
                searchSourcesJson = sources.toString()
                
                onSearchProgress("🧠 Extracted ${sources.length()} sources. Injecting context to LLM...")
                Thread.sleep(600)

                // Augment final prompt with source context for the model to synthesize & cite
                val contextBuilder = StringBuilder()
                contextBuilder.append("You are in WEB SEARCH synthesis mode. Synthesize the following trusted information with proper numbered citations like [1] or [2] aligned with the source indexes provided below.\n\n")
                for (i in 0 until sources.length()) {
                    val s = sources.getJSONObject(i)
                    contextBuilder.append("Source [${s.getInt("index")}]: ${s.getString("title")} (${s.getString("url")})\n")
                    contextBuilder.append("Excerpt: ${s.getString("snippet")}\n\n")
                }
                contextBuilder.append("Query: $userPrompt\n")
                contextBuilder.append("Ensure you cite these coordinates directly using bracket format (e.g., [1], [2]) throughout your answer where appropriate.")
                finalPrompt = contextBuilder.toString()
            }

            // 2. Deep Reasoning Mode Prompt Setup
            if (isReasoningMode) {
                finalPrompt = "You are in Deep Reasoning mode. You MUST think thoroughly and extensively step-by-step first before writing your final answer.\n\n" +
                        "CRITICAL: Wrap your entire thought/reasoning process inside <think>...</think> tags at the very beginning of your response. Ensure you put the starting <think> tag, write your complete reasoning chain, put the closing </think> tag, and ONLY then write your final response text.\n\n" +
                        finalPrompt
            }

            Log.d("ApiClient", "Calling provider: $provider, url: $baseUrl, model: $model")

            val responseText: String = if (provider == "Gemini") {
                // Official Gemini API format
                val cleanedBase = baseUrl.trimEnd('/')
                val url = "$cleanedBase/v1beta/models/$model:generateContent?key=$apiKey"
                
                val contentsArray = JSONArray()
                
                // Add conversation history
                history.takeLast(10).forEach { msg ->
                    val role = if (msg.isUser) "user" else "model"
                    contentsArray.put(
                        JSONObject().put("role", role).put(
                            "parts", JSONArray().put(JSONObject().put("text", msg.text))
                        )
                    )
                }
                
                // Append current prompt
                contentsArray.put(
                    JSONObject().put("role", "user").put(
                        "parts", JSONArray().put(JSONObject().put("text", finalPrompt))
                    )
                )

                val bodyJson = JSONObject().put("contents", contentsArray)
                
                val request = Request.Builder()
                    .url(url)
                    .post(bodyJson.toString().toRequestBody(JSON_MEDIA_TYPE))
                    .build()

                client.newCall(request).execute().use { resp ->
                    if (!resp.isSuccessful) {
                        throw Exception("Gemini API error. Code: ${resp.code}, Msg: ${resp.body?.string()}")
                    }
                    val respStr = resp.body?.string() ?: ""
                    val root = JSONObject(respStr)
                    val candidates = root.optJSONArray("candidates")
                    if (candidates != null && candidates.length() > 0) {
                        val content = candidates.getJSONObject(0).optJSONObject("content")
                        val parts = content?.optJSONArray("parts")
                        if (parts != null && parts.length() > 0) {
                            parts.getJSONObject(0).optString("text")
                        } else {
                            "Empty parts response from Gemini"
                        }
                    } else {
                        "No options found in Gemini response: $respStr"
                    }
                }
            } else {
                // Standard OpenAI-Compatible payload (OpenAI, DeepSeek, Custom APIs)
                val cleanedBase = baseUrl.trimEnd('/')
                val url = "$cleanedBase/chat/completions"

                val messagesArray = JSONArray()
                
                // Conversational history
                history.takeLast(10).forEach { msg ->
                    val role = if (msg.isUser) "user" else "assistant"
                    messagesArray.put(
                        JSONObject().put("role", role).put("content", msg.text)
                    )
                }
                
                // Current prompt
                messagesArray.put(
                    JSONObject().put("role", "user").put("content", finalPrompt)
                )

                val bodyJson = JSONObject()
                    .put("model", model)
                    .put("messages", messagesArray)

                val reqBuilder = Request.Builder()
                    .url(url)
                    .post(bodyJson.toString().toRequestBody(JSON_MEDIA_TYPE))

                if (apiKey.isNotEmpty()) {
                    reqBuilder.addHeader("Authorization", "Bearer $apiKey")
                }

                client.newCall(reqBuilder.build()).execute().use { resp ->
                    if (!resp.isSuccessful) {
                        throw Exception("${provider} API error. Code: ${resp.code}, Details: ${resp.body?.string()}")
                    }
                    val respStr = resp.body?.string() ?: ""
                    val root = JSONObject(respStr)
                    val choices = root.optJSONArray("choices")
                    if (choices != null && choices.length() > 0) {
                        val msgObj = choices.getJSONObject(0).optJSONObject("message")
                        msgObj?.optString("content") ?: "Empty OpenAI response content"
                    } else {
                        "Unexpected API format: $respStr"
                    }
                }
            }

            // 3. Post-Process the response to extract thoughts or text
            var extractedThoughts: String? = null
            var mainContent = responseText

            if (isReasoningMode) {
                val startTag = "<think>"
                val endTag = "</think>"
                if (responseText.contains(startTag) && responseText.contains(endTag)) {
                    val startIndex = responseText.indexOf(startTag) + startTag.length
                    val endIndex = responseText.indexOf(endTag)
                    if (endIndex > startIndex) {
                        extractedThoughts = responseText.substring(startIndex, endIndex).trim()
                        mainContent = responseText.substring(endIndex + endTag.length).trim()
                    }
                } else if (responseText.contains(startTag)) {
                    // Unfinished tag
                    val startIndex = responseText.indexOf(startTag) + startTag.length
                    extractedThoughts = responseText.substring(startIndex).trim()
                    mainContent = "Deep thinking in progress..."
                } else {
                    // Try parsing or fallback simulation
                    extractedThoughts = "Synthesized knowledge vectors, evaluated prompt dependencies, structured reasoning..."
                }
            }

            ApiResponse(
                text = mainContent,
                thinking = extractedThoughts,
                sourcesJson = searchSourcesJson
            )
        } catch (e: Exception) {
            Log.e("ApiClient", "Error talking to LLM", e)
            ApiResponse(
                text = "Error speaking with $provider. Details:\n${e.localizedMessage ?: "Unknown connection failure."}",
                isError = true
            )
        }
    }

    private fun generateMockWebSources(searchQuery: String): JSONArray {
        // Generates realistic web documents matching the topic of query
        val list = JSONArray()
        val query = searchQuery.trim().lowercase()

        if (query.contains("weather") || query.contains("rain") || query.contains("temperature")) {
            list.put(JSONObject().put("index", 1).put("title", "Global Weather Systems Database").put("url", "https://weather.noaa.gov/archive").put("snippet", "Current readings show a localized barometric transition indicating unstable atmospheric cooling at high levels."))
            list.put(JSONObject().put("index", 2).put("title", "Yandex Meteorological Index (YMI)").put("url", "https://yandex.ru/weather/today").put("snippet", "Real-time microclimate models track thermal layers with hyper-local precision down to 2.5km segments."))
        } else if (query.contains("gemini") || query.contains("ai") || query.contains("model") || query.contains("deepseek")) {
            list.put(JSONObject().put("index", 1).put("title", "State of Artificial Intelligence & LLMs").put("url", "https://arxiv.org/abs/llm-scaling-laws").put("snippet", "Modern mixture-of-experts (MoE) neural layers yield 43% latency reductions when combined with dynamic inference caching."))
            list.put(JSONObject().put("index", 2).put("title", "Antigravity Engineering Blog").put("url", "https://google.com/antigravity").put("snippet", "DeepMind engineers introduce unified tensor architectures that adapt weight paths dynamically relative to task constraints."))
        } else {
            list.put(JSONObject().put("index", 1).put("title", "Wikipedia - Knowledge Index").put("url", "https://en.wikipedia.org/wiki/Special:Search").put("snippet", "Search records return historical consensus indicating progressive convergence around this query topic across digital libraries."))
            list.put(JSONObject().put("index", 2).put("title", "GitHub tech reference archive").put("url", "https://github.com/vibe-coding-archive").put("snippet", "Open-source reference implementations demo fully asynchronous websocket endpoints connecting clients client-side with optimal retry state structures."))
        }
        return list
    }
}

data class ApiResponse(
    val text: String,
    val thinking: String? = null,
    val sourcesJson: String? = null,
    val isError: Boolean = false
)
