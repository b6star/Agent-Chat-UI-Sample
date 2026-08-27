package com.b6star.chatui.data

import com.b6star.chatui.ai.AiModelCatalog
import com.b6star.chatui.ai.ChatResponse
import com.b6star.chatui.data.model.ChatMessage
import com.b6star.chatui.data.model.ChatSession
import com.b6star.chatui.util.Utils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

/**
 * In-memory mockup repository. Conversations disappear when the app is closed.
 * Maintain the public method signatures of this class when switching to persistent storage like Room/Firestore.
 */
class ChatRepository {

    private val json = Json { encodeDefaults = true }

    private var nextSessionId = 1L
    private var nextMessageId = 1L

    private val _sessions = MutableStateFlow<List<ChatSession>>(emptyList())
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())

    init {
        seed()
    }

    fun getSessions(): Flow<List<ChatSession>> = _sessions

    fun getMessages(sessionId: Int): Flow<List<ChatMessage>> =
        _messages.map { list -> list.filter { it.sessionId == sessionId } }

    suspend fun insertSession(session: ChatSession): Long {
        val id = nextSessionId++
        _sessions.value = _sessions.value + session.copy(id = id.toInt())
        return id
    }

    suspend fun insertMessage(message: ChatMessage): Long {
        val id = nextMessageId++
        _messages.value = _messages.value + message.copy(id = id)
        return id
    }

    suspend fun updateMessageContent(messageId: Long, content: String) {
        updateMessage(messageId) { it.copy(content = content) }
    }

    suspend fun updateMessageMetadata(
        messageId: Long,
        promptTokens: Int?,
        candidatesTokens: Int?,
        totalTokens: Int?,
        thoughtsTokens: Int?,
        responseTimeMs: Long?,
        estimatedCostUsd: Double?,
        modelName: String?,
        agentVersion: String?,
        provider: String?,
        inputHistoryCount: Int?
    ) {
        updateMessage(messageId) {
            it.copy(
                promptTokens = promptTokens,
                candidatesTokens = candidatesTokens,
                totalTokens = totalTokens,
                thoughtsTokens = thoughtsTokens,
                responseTimeMs = responseTimeMs,
                estimatedCostUsd = estimatedCostUsd,
                modelName = modelName,
                agentVersion = agentVersion,
                provider = provider,
                inputHistoryCount = inputHistoryCount,
                deviceModel = Utils.getDeviceModel(),
                osVersion = Utils.getOsVersion()
            )
        }
    }

    suspend fun updateMessageDetails(messageId: Long, detailsJson: String) {
        updateMessage(messageId) { it.copy(detailsJson = detailsJson) }
    }

    suspend fun updateSessionLastTime(sessionId: Int, time: Long) {
        _sessions.value = _sessions.value.map {
            if (it.id == sessionId) it.copy(lastMessageTime = time) else it
        }
    }

    suspend fun renameSession(sessionId: Int, newTitle: String) {
        _sessions.value = _sessions.value.map {
            if (it.id == sessionId) it.copy(title = newTitle) else it
        }
    }

    suspend fun deleteSession(sessionId: Int) {
        _sessions.value = _sessions.value.filter { it.id != sessionId }
        _messages.value = _messages.value.filter { it.sessionId != sessionId }
    }

    suspend fun clearChat() {
        _sessions.value = emptyList()
        _messages.value = emptyList()
    }

    private fun updateMessage(messageId: Long, transform: (ChatMessage) -> ChatMessage) {
        _messages.value = _messages.value.map {
            if (it.id == messageId) transform(it) else it
        }
    }

    private fun seed() {
        val context = com.b6star.chatui.di.ServiceLocator.context
        val sessionId = insertSeedSession(
            ChatSession(
                title = context.getString(com.b6star.chatui.R.string.seed_session_title),
                lastMessageTime = System.currentTimeMillis() - 60_000L
            )
        )
        _messages.value = listOf(
            ChatMessage(
                id = nextMessageId++,
                sessionId = sessionId,
                role = ChatMessage.ROLE_USER,
                timestamp = System.currentTimeMillis() - 120_000L,
                content = context.getString(com.b6star.chatui.R.string.seed_user_question)
            ),
            ChatMessage(
                id = nextMessageId++,
                sessionId = sessionId,
                role = ChatMessage.ROLE_ASSISTANT,
                timestamp = System.currentTimeMillis() - 60_000L,
                content = context.getString(com.b6star.chatui.R.string.sample_reply),
                detailsJson = json.encodeToString(
                    listOf(
                        ChatResponse.ShowDetails(
                            title = context.getString(com.b6star.chatui.R.string.seed_details_title),
                            items = listOf(
                                ChatResponse.ShowDetails.DetailEntry(context.getString(com.b6star.chatui.R.string.seed_details_entry_ai), "2026-08-26", 0.0),
                                ChatResponse.ShowDetails.DetailEntry(context.getString(com.b6star.chatui.R.string.seed_details_entry_data), "2026-08-26", 0.0),
                                ChatResponse.ShowDetails.DetailEntry(context.getString(com.b6star.chatui.R.string.seed_details_entry_ui), "2026-08-26", 0.0)
                            )
                        )
                    )
                ),
                promptTokens = 24,
                candidatesTokens = 210,
                totalTokens = 234,
                responseTimeMs = 1450L,
                modelName = AiModelCatalog.defaultModelName,
                agentVersion = "mock-seed",
                provider = "mock",
                deviceModel = Utils.getDeviceModel(),
                osVersion = Utils.getOsVersion()
            )
        )
        nextSessionId = sessionId + 1L
    }

    private fun insertSeedSession(session: ChatSession): Int {
        val id = nextSessionId++
        _sessions.value = _sessions.value + session.copy(id = id.toInt())
        return id.toInt()
    }
}
