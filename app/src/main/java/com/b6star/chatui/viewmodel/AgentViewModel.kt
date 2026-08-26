package com.b6star.chatui.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.b6star.chatui.ai.AiFileAttachment
import com.b6star.chatui.ai.AiImageAttachment
import com.b6star.chatui.ai.AiModelCatalog
import com.b6star.chatui.ai.ChatResponse
import com.b6star.chatui.data.model.ChatMessage
import com.b6star.chatui.data.model.ChatSession
import com.b6star.chatui.di.ServiceLocator
import com.b6star.chatui.R
import com.b6star.chatui.ui.theme.AgentThemeType
import com.b6star.chatui.util.Utils
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

private const val CONTENT_SYNC_INTERVAL_MS = 250L

enum class AppLanguage(val code: String) {
    SYSTEM(""),
    ENGLISH("en"),
    KOREAN("ko")
}

@OptIn(ExperimentalCoroutinesApi::class)
@Suppress("unused")
class AgentViewModel(
    private val appContext: Context
) : ViewModel() {

    companion object {
        // Commands shown for a bare slash or general prefix. Language variants are shown after selecting a language prefix.
        val slashCommands = listOf("/help", "/clear", "/kotlin", "/python", "/java", "/c", "/cpp", "/csharp", "/sql", "/javascript", "/mermaid")
        val kotlinCommands = listOf("/kotlin", "/kotlin-long", "/kotlin-very-long")
        val pythonCommands = listOf("/python", "/python-long", "/python-very-long")
        val javaCommands = listOf("/java", "/java-long", "/java-very-long")
        val cCommands = listOf("/c", "/c-long", "/c-very-long")
        val cppCommands = listOf("/cpp", "/cpp-long", "/cpp-very-long")
        val csharpCommands = listOf("/csharp", "/csharp-long", "/csharp-very-long")
        val sqlCommands = listOf("/sql", "/sql-long", "/sql-very-long")
        val javascriptCommands = listOf("/javascript", "/javascript-long", "/javascript-very-long")
    }

    private val chatRepository = ServiceLocator.chatRepository
    private val aiGateway = ServiceLocator.aiGateway

    // 1. Property initialization order matters:
    // Initialize StateFlows first so they are non-null when referenced by init or selectSession.
    private val _currentSessionId = MutableStateFlow<Int?>(null)
    val currentSessionId: StateFlow<Int?> = _currentSessionId.asStateFlow()

    private val _selectedModel = MutableStateFlow(AiModelCatalog.liteModelName)
    val selectedModel: StateFlow<String> = _selectedModel.asStateFlow()

    private val _selectedHistoryLimit = MutableStateFlow(20)
    val selectedHistoryLimit: StateFlow<Int> = _selectedHistoryLimit.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isStreaming = MutableStateFlow(false)
    val isStreaming: StateFlow<Boolean> = _isStreaming.asStateFlow()

    private val _chatInput = MutableStateFlow("")
    val chatInput: StateFlow<String> = _chatInput.asStateFlow()

    private val _selectedTheme = MutableStateFlow(AgentThemeType.DEFAULT)
    val selectedTheme: StateFlow<AgentThemeType> = _selectedTheme.asStateFlow()

    private val _selectedLanguage = MutableStateFlow(AppLanguage.SYSTEM)
    val selectedLanguage: StateFlow<AppLanguage> = _selectedLanguage.asStateFlow()

    private val _detailItems = MutableStateFlow<ChatResponse.ShowDetails?>(null)
    val detailItems: StateFlow<ChatResponse.ShowDetails?> = _detailItems.asStateFlow()

    private val _sessionStats = MutableStateFlow<SessionStats?>(null)
    val sessionStats: StateFlow<SessionStats?> = _sessionStats.asStateFlow()

    // 2. Load the session list.
    val sessions: StateFlow<List<ChatSession>> = chatRepository.getSessions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 3. Load the message list (depends on currentSessionId).
    val messages: StateFlow<List<ChatMessage>> = _currentSessionId
        .flatMapLatest { sessionId ->
            if (sessionId == null) flowOf(emptyList())
            else chatRepository.getMessages(sessionId)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val sessionTotalTokens: StateFlow<Int> = messages.map { list ->
        list.sumOf { it.totalTokens ?: 0 }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val sessionTotalCost: StateFlow<Double> = messages.map { list ->
        list.sumOf { it.estimatedCostUsd ?: 0.0 }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    private var chatJob: Job? = null

    init {
        // Check currently set language and synchronize state
        val currentLocales = AppCompatDelegate.getApplicationLocales()
        if (!currentLocales.isEmpty) {
            val code = currentLocales.toLanguageTags()
            AppLanguage.entries.find { it.code == code }?.let {
                _selectedLanguage.value = it
            }
        }

        // Separate the logic for automatically selecting the first session on app launch into a separate coroutine
        viewModelScope.launch {
            // Wait until actual data arrives since sessions might have an initial value
            sessions.filter { it.isNotEmpty() }.firstOrNull()?.let { sessionList ->
                if (_currentSessionId.value == null) {
                    selectSession(sessionList.first().id!!)
                }
            }
        }
    }

    fun selectSession(sessionId: Int) {
        // Ensure null safety
        _currentSessionId.value = sessionId
        
        viewModelScope.launch {
            val msgs = chatRepository.getMessages(sessionId).first()
            val lastUsedModel = msgs.lastOrNull {
                it.role == ChatMessage.ROLE_ASSISTANT && !it.modelName.isNullOrBlank()
            }?.modelName
            
            if (_currentSessionId.value == sessionId) {
                _selectedModel.value = lastUsedModel ?: AiModelCatalog.liteModelName
            }
        }
    }

    fun setModel(model: String) {
        _selectedModel.value = model
    }

    fun setTheme(theme: AgentThemeType) {
        _selectedTheme.value = theme
    }

    fun setLanguage(language: AppLanguage) {
        _selectedLanguage.value = language
        val appLocale: LocaleListCompat = if (language == AppLanguage.SYSTEM) {
            LocaleListCompat.getEmptyLocaleList()
        } else {
            LocaleListCompat.forLanguageTags(language.code)
        }
        AppCompatDelegate.setApplicationLocales(appLocale)
    }

    fun setHistoryLimit(limit: Int) {
        _selectedHistoryLimit.value = limit
    }

    fun onChatInputChanged(input: String) {
        _chatInput.value = input
    }

    fun createNewChat() {
        _currentSessionId.value = null
        _selectedModel.value = AiModelCatalog.liteModelName
    }

    data class SessionStats(
        val assistantMessageCount: Int,
        val avgResponseTimeMs: Long?,
        val totalTokens: Int,
        val totalCost: Double
    )

    fun loadSessionStats(sessionId: Int) {
        _sessionStats.value = null
        viewModelScope.launch {
            val msgs = chatRepository.getMessages(sessionId).first()
            val assistantMsgs = msgs.filter { it.role == ChatMessage.ROLE_ASSISTANT }
            val responseTimes = assistantMsgs.mapNotNull { it.responseTimeMs }
            _sessionStats.value = SessionStats(
                assistantMessageCount = assistantMsgs.size,
                avgResponseTimeMs = if (responseTimes.isEmpty()) null else responseTimes.average().toLong(),
                totalTokens = msgs.sumOf { it.totalTokens ?: 0 },
                totalCost = msgs.sumOf { it.estimatedCostUsd ?: 0.0 }
            )
        }
    }

    fun renameSession(sessionId: Int, newTitle: String) {
        viewModelScope.launch {
            chatRepository.renameSession(sessionId, newTitle.trim())
        }
    }

    fun showDetailsAtIndex(message: ChatMessage, index: Int) {
        val jsonStr = message.detailsJson
        if (jsonStr.isNullOrBlank()) return
        try {
            val allDetails = Json.decodeFromString<List<ChatResponse.ShowDetails>>(jsonStr)
            if (index in allDetails.indices) {
                _detailItems.value = allDetails[index]
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun dismissDetailItems() {
        _detailItems.value = null
    }

    fun sendMessage(
        content: String,
        imageUris: List<Uri> = emptyList(),
        fileUris: List<Uri> = emptyList()
    ) {
        if (content.isBlank() && imageUris.isEmpty() && fileUris.isEmpty()) return
        require(imageUris.size + fileUris.size <= 10) { appContext.getString(R.string.error_max_attachments) }

        val slashCommand = if (imageUris.isEmpty() && fileUris.isEmpty()) {
            parseSlashCommand(content)
        } else {
            null
        }
        if (slashCommand != null) {
            _chatInput.value = ""
            chatJob?.cancel()
            chatJob = viewModelScope.launch {
                executeSlashCommand(slashCommand)
                chatJob = null
            }
            return
        }

        val attachmentsContent = (imageUris + fileUris).joinToString("\n") { uri ->
            val fileName = getFileName(uri)
            "[$fileName]($uri)"
        }

        val messageText = buildString {
            if (content.isNotBlank()) {
                append(content)
                if (attachmentsContent.isNotEmpty()) {
                    append("\n\n")
                    append(attachmentsContent)
                }
            } else {
                append(attachmentsContent)
            }
        }

        _chatInput.value = ""
        chatJob?.cancel()
        chatJob = viewModelScope.launch {
            var sessionId = _currentSessionId.value

            if (sessionId == null) {
                val title = if (messageText.length > 20) messageText.take(20) + "..." else messageText
                val newSessionId = chatRepository.insertSession(ChatSession(title = title)).toInt()
                _currentSessionId.value = newSessionId
                sessionId = newSessionId
            }

            val userMessage = ChatMessage(
                sessionId = sessionId!!,
                content = messageText,
                role = ChatMessage.ROLE_USER
            )
            chatRepository.insertMessage(userMessage)
            chatRepository.updateSessionLastTime(sessionId, System.currentTimeMillis())

            _isLoading.value = true
            _isStreaming.value = false

            kotlinx.coroutines.delay(1500)

            _isLoading.value = false
            _isStreaming.value = true

            val currentMessages = chatRepository.getMessages(sessionId).first()

            val assistantMessageId = chatRepository.insertMessage(
                ChatMessage(
                    sessionId = sessionId,
                    content = "",
                    role = ChatMessage.ROLE_ASSISTANT
                )
            )

            var fullAssistantContent = ""
            var lastContentSyncAt = 0L
            var streamCompleted = false
            val tempDetailsList = mutableListOf<ChatResponse.ShowDetails>()

            try {
                fun readAttachment(uri: Uri): AiImageAttachment {
                    val bytes = appContext.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                        ?: error(appContext.getString(R.string.error_read_attachment))
                    return AiImageAttachment(
                        bytes = bytes,
                        mimeType = appContext.contentResolver.getType(uri) ?: "application/octet-stream"
                    )
                }
                val images = imageUris.map(::readAttachment)
                val files = fileUris.map { uri ->
                    readAttachment(uri).let { AiFileAttachment(it.bytes, it.mimeType) }
                }
                val attachments: List<AiImageAttachment> =
                    images + files.map { AiImageAttachment(it.bytes, it.mimeType) }
                require(attachments.sumOf { it.bytes.size } <= 20 * 1024 * 1024) {
                    appContext.getString(R.string.error_max_size)
                }

                val flow = aiGateway.chatStream(
                    history = currentMessages,
                    model = _selectedModel.value,
                    historyLimit = _selectedHistoryLimit.value,
                    images = attachments,
                    files = files
                )

                flow.collect { response ->
                    when (response) {
                        is ChatResponse.Chunk -> {
                            fullAssistantContent += response.text
                            val now = System.currentTimeMillis()
                            if (now - lastContentSyncAt >= CONTENT_SYNC_INTERVAL_MS) {
                                lastContentSyncAt = now
                                chatRepository.updateMessageContent(assistantMessageId, fullAssistantContent)
                            }
                        }
                        is ChatResponse.Metadata -> {
                            val promptTokens = response.promptTokens ?: 0
                            val candidatesTokens = response.candidatesTokens ?: 0
                            val totalTokens = response.totalTokens ?: (promptTokens + candidatesTokens)
                            val thoughtsTokens = response.thoughtsTokens ?: 0

                            val costUsd = Utils.calculateCostUsd(
                                modelName = response.modelName ?: _selectedModel.value,
                                promptTokens = promptTokens,
                                candidatesTokens = candidatesTokens,
                                thoughtsTokens = thoughtsTokens
                            )
                            val costKrw = Utils.calculateCostKrw(costUsd)

                            chatRepository.updateMessageMetadata(
                                messageId = assistantMessageId,
                                promptTokens = promptTokens,
                                candidatesTokens = candidatesTokens,
                                totalTokens = totalTokens,
                                thoughtsTokens = thoughtsTokens,
                                responseTimeMs = response.responseTimeMs,
                                estimatedCostUsd = costUsd,
                                modelName = response.modelName,
                                agentVersion = response.agentVersion,
                                provider = response.provider,
                                inputHistoryCount = response.inputHistoryCount
                            )
                        }
                        is ChatResponse.ShowDetails -> {
                            tempDetailsList.add(response)
                        }
                    }
                }
                streamCompleted = true
            } catch (e: CancellationException) {
                if (fullAssistantContent.isEmpty()) {
                    chatRepository.updateMessageContent(assistantMessageId, appContext.getString(R.string.msg_stopped))
                }
            } catch (e: Exception) {
                chatRepository.updateMessageContent(
                    assistantMessageId,
                    appContext.getString(R.string.error_failed, e.message ?: appContext.getString(R.string.error_unknown))
                )
            } finally {
                if (streamCompleted && fullAssistantContent.isNotEmpty()) {
                    chatRepository.updateMessageContent(assistantMessageId, fullAssistantContent)
                }

                if (tempDetailsList.isNotEmpty()) {
                    try {
                        val jsonString = Json.encodeToString(tempDetailsList)
                        chatRepository.updateMessageDetails(assistantMessageId, jsonString)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                chatRepository.updateSessionLastTime(sessionId, System.currentTimeMillis())
                _isLoading.value = false
                _isStreaming.value = false
                chatJob = null
            }
        }
    }

    private fun parseSlashCommand(content: String): String? {
        val trimmed = content.trim()
        if (!trimmed.startsWith('/')) return null
        return trimmed.substringBefore(' ').lowercase()
    }

    private suspend fun executeSlashCommand(command: String) {
        if (command == "/clear") {
            chatRepository.clearChat()
            _currentSessionId.value = null
            _sessionStats.value = null
            return
        }

        val response = when (command) {
            "/help" -> appContext.getString(R.string.slash_help_response) +
                appContext.getString(R.string.slash_help_extra_response)
            "/kotlin" -> appContext.getString(R.string.slash_kotlin_response)
            "/kotlin-long" -> appContext.getString(R.string.slash_kotlin_long_response)
            "/kotlin-very-long" -> appContext.getString(R.string.slash_kotlin_very_long_response)
            "/python" -> appContext.getString(R.string.slash_python_response)
            "/python-long" -> appContext.getString(R.string.slash_python_long_response)
            "/python-very-long" -> appContext.getString(R.string.slash_python_very_long_response)
            "/java" -> appContext.getString(R.string.slash_java_response)
            "/java-long" -> appContext.getString(R.string.slash_java_long_response)
            "/java-very-long" -> appContext.getString(R.string.slash_java_very_long_response)
            "/c" -> appContext.getString(R.string.slash_c_response)
            "/c-long" -> appContext.getString(R.string.slash_c_long_response)
            "/c-very-long" -> appContext.getString(R.string.slash_c_very_long_response)
            "/cpp" -> appContext.getString(R.string.slash_cpp_response)
            "/cpp-long" -> appContext.getString(R.string.slash_cpp_long_response)
            "/cpp-very-long" -> appContext.getString(R.string.slash_cpp_very_long_response)
            "/csharp" -> appContext.getString(R.string.slash_csharp_response)
            "/csharp-long" -> appContext.getString(R.string.slash_csharp_long_response)
            "/csharp-very-long" -> appContext.getString(R.string.slash_csharp_very_long_response)
            "/sql" -> appContext.getString(R.string.slash_sql_response)
            "/sql-long" -> appContext.getString(R.string.slash_sql_long_response)
            "/sql-very-long" -> appContext.getString(R.string.slash_sql_very_long_response)
            "/javascript" -> appContext.getString(R.string.slash_javascript_response)
            "/javascript-long" -> appContext.getString(R.string.slash_javascript_long_response)
            "/javascript-very-long" -> appContext.getString(R.string.slash_javascript_very_long_response)
            "/mermaid" -> appContext.getString(R.string.slash_mermaid_response)
            "/mermaid-error" -> appContext.getString(R.string.slash_mermaid_error_response)
            else -> appContext.getString(R.string.slash_unknown_response, command)
        }

        var sessionId = _currentSessionId.value
        if (sessionId == null) {
            sessionId = chatRepository.insertSession(
                ChatSession(title = command)
            ).toInt()
            _currentSessionId.value = sessionId
        }

        chatRepository.insertMessage(
            ChatMessage(
                sessionId = sessionId,
                content = command,
                role = ChatMessage.ROLE_USER
            )
        )
        chatRepository.insertMessage(
            ChatMessage(
                sessionId = sessionId,
                content = response,
                role = ChatMessage.ROLE_ASSISTANT
            )
        )
        chatRepository.updateSessionLastTime(sessionId, System.currentTimeMillis())
    }

    fun stopGeneration() {
        chatJob?.cancel()
        chatJob = null
        _isLoading.value = false
        _isStreaming.value = false
    }


    fun deleteSession(sessionId: Int) {
        viewModelScope.launch {
            chatRepository.deleteSession(sessionId)
            if (_currentSessionId.value == sessionId) {
                _currentSessionId.value = null
                _selectedModel.value = AiModelCatalog.liteModelName
            }
        }
    }

    fun clearChat() {
        viewModelScope.launch {
            chatRepository.clearChat()
            _currentSessionId.value = null
            _selectedModel.value = AiModelCatalog.liteModelName
        }
    }

    private fun getFileName(uri: Uri): String {
        var name = "File"
        appContext.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (nameIndex != -1 && cursor.moveToFirst()) {
                name = cursor.getString(nameIndex)
            }
        }
        return name
    }
}
