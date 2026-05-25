package com.example.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ChatViewModel(
    private val messageDao: MessageDao,
    val settingsRepository: SettingsRepository,
    private val buildConfigGeminiKey: String // Exposed safely from BuildConfig
) : ViewModel() {

    // Active configuration & chat session states
    val threads = messageDao.getAllThreads()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _activeThreadId = MutableStateFlow<Long?>(null)
    val activeThreadId: StateFlow<Long?> = _activeThreadId.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    // Mode: "standard", "reasoning", "search", "coder"
    private val _appMode = MutableStateFlow("standard")
    val appMode: StateFlow<String> = _appMode.asStateFlow()

    // Simulated web-crawling real-time log indicators
    private val _searchProgressLogs = MutableStateFlow<List<String>>(emptyList())
    val searchProgressLogs: StateFlow<List<String>> = _searchProgressLogs.asStateFlow()

    // Active parsed code block for Artifact visualizer
    private val _activeArtifact = MutableStateFlow<ArtifactData?>(null)
    val activeArtifact: StateFlow<ArtifactData?> = _activeArtifact.asStateFlow()

    // Load messages dynamically based on active thread ID
    val messages = _activeThreadId.flatMapLatest { threadId ->
        if (threadId != null) {
            messageDao.getMessagesForThread(threadId)
        } else {
            flowOf(emptyList())
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        // Automatically select or create first thread if empty
        viewModelScope.launch {
            val initialThreads = messageDao.getAllThreads().first()
            if (initialThreads.isNotEmpty()) {
                _activeThreadId.value = initialThreads.first().id
            } else {
                createNewThread("Vibe Space ✨")
            }
        }
    }

    fun setAppMode(mode: String) {
        _appMode.value = mode
    }

    fun clearArtifact() {
        _activeArtifact.value = null
    }

    suspend fun createNewThread(title: String): Long {
        val newId = messageDao.insertThread(ChatThread(title = title))
        _activeThreadId.value = newId
        // Add a friendly greeting message with instructions matching their provider
        val welcomeMessage = when (settingsRepository.provider) {
            "Gemini" -> "Welcome to **AI VibeChat**! Linked with Gemini API (using key from AI Studio secrets config)."
            "OpenAI" -> "Welcome! Connected to OpenAI compatible URL with model **${settingsRepository.openAIModel}**."
            "DeepSeek" -> "Welcome! Connected to DeepSeek API with model **${settingsRepository.deepseekModel}**."
            else -> "Welcome! Running through Custom API Endpoint model **${settingsRepository.customModel}**."
        }
        messageDao.insertMessage(
            Message(
                threadId = newId,
                text = "$welcomeMessage\n\nToggle **Deep Reasoning** to view real-time thought trace collapsible components, or **Web Search** to simulate localized knowledge crawlers with hoverable citations! Try pasting code to launch high-fidelity Artifacts.",
                isUser = false
            )
        )
        return newId
    }

    fun selectThread(id: Long) {
        _activeThreadId.value = id
        // Reset artifact panel when switching chats
        _activeArtifact.value = null
    }

    fun deleteThread(id: Long) {
        viewModelScope.launch {
            messageDao.deleteThread(id)
            val updatedList = threads.value.filter { it.id != id }
            if (updatedList.isNotEmpty()) {
                _activeThreadId.value = updatedList.first().id
            } else {
                createNewThread("Vibe Space ✨")
            }
        }
    }

    fun sendMessage(text: String) {
        val threadId = _activeThreadId.value ?: return
        if (text.isBlank() || _isGenerating.value) return

        viewModelScope.launch {
            _isGenerating.value = true
            _searchProgressLogs.value = emptyList()

            // 1. Insert User Message
            messageDao.insertMessage(
                Message(
                    threadId = threadId,
                    text = text,
                    isUser = true,
                    appMode = _appMode.value
                )
            )

            // Auto rename conversation from first query if default name
            val currentThread = threads.value.find { it.id == threadId }
            if (currentThread?.title == "Vibe Space ✨") {
                val cleanedTitle = if (text.length > 20) text.take(17) + "..." else text
                messageDao.updateThreadTitle(threadId, cleanedTitle)
            }

            // Gather preceding messages for prompt context
            val currentHistory = messages.value

            // Fetch dynamic active connectivity variables
            val (baseUrl, apiKey, model) = settingsRepository.getActiveUrlAndKeyAndModel(buildConfigGeminiKey)

            val isReasoning = _appMode.value == "reasoning"
            val isSearch = _appMode.value == "search"

            // 2. Query Client-Side wrapper with progress reporting
            val apiResponse = ApiClient.chatWithModel(
                provider = settingsRepository.provider,
                baseUrl = baseUrl,
                apiKey = apiKey,
                model = model,
                userPrompt = text,
                history = currentHistory,
                isReasoningMode = isReasoning,
                isSearchMode = isSearch,
                onSearchProgress = { logMessage ->
                    _searchProgressLogs.value = _searchProgressLogs.value + logMessage
                }
            )

            // 3. Save response to database
            messageDao.insertMessage(
                Message(
                    threadId = threadId,
                    text = apiResponse.text,
                    isUser = false,
                    thinkingText = apiResponse.thinking,
                    searchSourcesJson = apiResponse.sourcesJson,
                    appMode = _appMode.value
                )
            )

            // Check if response contains a code block to configure Artifacts
            detectAndLoadArtifact(text, apiResponse.text)

            _isGenerating.value = false
            _searchProgressLogs.value = emptyList()
        }
    }

    private fun detectAndLoadArtifact(query: String, answer: String) {
        val codeBlockRegex = """```(\w*)\n([\s\S]*?)```""".toRegex()
        val match = codeBlockRegex.find(answer)
        if (match != null) {
            val language = match.groupValues[1].ifEmpty { "code" }
            val code = match.groupValues[2].trim()
            _activeArtifact.value = ArtifactData(
                title = extractArtifactTitle(query, language),
                language = language,
                code = code
            )
        }
    }

    private fun extractArtifactTitle(query: String, lang: String): String {
        val firstWords = query.trim().split(" ").take(4).joinToString(" ")
        return "${firstWords.ifEmpty { "Rendered" }} ($lang)"
    }
}

data class ArtifactData(
    val title: String,
    val language: String,
    val code: String
)
