package com.b6star.chatui.data

import com.b6star.chatui.ai.ChatResponse
import com.b6star.chatui.data.model.ChatMessage
import com.b6star.chatui.data.model.ChatSession
import com.b6star.chatui.util.Utils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * 인메모리 목업 저장소. 앱을 종료하면 대화가 사라진다.
 * Room/Firestore 등 영속 저장소로 교체할 때는 이 클래스의 공개 메서드 시그니처를 유지하면 된다.
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
        estimatedCostKrw: Double?,
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
                estimatedCostKrw = estimatedCostKrw,
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
        val sessionId = insertSeedSession(
            ChatSession(
                title = "AgentChatUI 샘플 대화",
                lastMessageTime = System.currentTimeMillis() - 60_000L
            )
        )
        _messages.value = listOf(
            ChatMessage(
                id = nextMessageId++,
                sessionId = sessionId,
                role = ChatMessage.ROLE_USER,
                timestamp = System.currentTimeMillis() - 120_000L,
                content = "이 템플릿에서 어떤 걸 확인할 수 있어?"
            ),
            ChatMessage(
                id = nextMessageId++,
                sessionId = sessionId,
                role = ChatMessage.ROLE_ASSISTANT,
                timestamp = System.currentTimeMillis() - 60_000L,
                content = SAMPLE_REPLY,
                detailsJson = json.encodeToString(
                    listOf(
                        ChatResponse.ShowDetails(
                            title = "템플릿 구성 요소",
                            items = listOf(
                                ChatResponse.ShowDetails.DetailEntry("ai/ - Provider 교체 지점", "2026-08-26", 0.0),
                                ChatResponse.ShowDetails.DetailEntry("data/ - 목업 저장소", "2026-08-26", 0.0),
                                ChatResponse.ShowDetails.DetailEntry("ui/ - 채팅 화면 전체", "2026-08-26", 0.0)
                            )
                        )
                    )
                ),
                promptTokens = 24,
                candidatesTokens = 210,
                totalTokens = 234,
                responseTimeMs = 1450L,
                modelName = "models/gemini-2.0-flash",
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

    private companion object {
        val SAMPLE_REPLY = """
            ## AgentChatUI 샘플

            이 화면은 **목업 데이터**로 동작하는 채팅 UI 샘플입니다.

            ### 확인해 볼 수 있는 것
            - 실시간 **스트리밍** 응답 (MockAiGateway)
            - 마크다운 렌더링: `코드`, **굵게**, 리스트
            - 코드 하이라이팅과 Mermaid 다이어그램
            - [자세히 보기] 상세 패널

            ```kotlin
            // ai/AiGateway 를 구현해 어떤 LLM이든 연결할 수 있습니다.
            val gateway: AiGateway = ServiceLocator.aiGateway
            ```

            ```mermaid
            graph TD
                U[사용자] -->|메시지| VM[AgentViewModel]
                VM -->|chatStream| G[AiGateway]
                G -->|Flow<ChatResponse>| VM
                VM -->|StateFlow| S[AgentScreen]
            ```
        """.trimIndent()
    }
}
