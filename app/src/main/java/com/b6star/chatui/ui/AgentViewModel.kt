package com.b6star.chatui.ui

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
import com.b6star.chatui.util.Utils
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * [성능 최적화 2번: 청크 배치 처리]
 * 스트리밍 중 AI 응답은 아주 잘게 나뉜 청크로 수십~수백 회 도착한다.
 * 청크마다 저장소에 쓰면 → Flow가 다시 방출 → LazyColumn 전체 리컴포지션이
 * 초당 수회 반복되어 UI가 버벅거린다.
 * 이 간격(250ms) 동안 도착한 청크는 메모리(fullAssistantContent)에만 누적하고,
 * 일정 시간이 지난 뒤 한 번만 저장소에 반영해서 리컴포지션 빈도를 수십 배 줄인다.
 * 부작용: 화면의 스트리밍 텍스트가 최대 250ms 늦게 갱신된다(체감 거의 없음).
 */
private const val CONTENT_SYNC_INTERVAL_MS = 250L

@OptIn(ExperimentalCoroutinesApi::class)
class AgentViewModel(
    private val appContext: Context
) : ViewModel() {

    private val chatRepository = ServiceLocator.chatRepository
    private val aiGateway = ServiceLocator.aiGateway

    val sessions: StateFlow<List<ChatSession>> = chatRepository.getSessions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _currentSessionId = MutableStateFlow<Int?>(null)
    val currentSessionId: StateFlow<Int?> = _currentSessionId

    private val _selectedModel = MutableStateFlow(AiModelCatalog.liteModelName)
    val selectedModel: StateFlow<String> = _selectedModel

    private val _selectedHistoryLimit = MutableStateFlow(20) // 기본값 20
    val selectedHistoryLimit: StateFlow<Int> = _selectedHistoryLimit

    val messages: StateFlow<List<ChatMessage>> = _currentSessionId.flatMapLatest { sessionId ->
        if (sessionId == null) flowOf(emptyList())
        else chatRepository.getMessages(sessionId)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val sessionTotalTokens: StateFlow<Int> = messages.flatMapLatest { messageList ->
        flowOf(messageList.sumOf { it.totalTokens ?: 0 })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val sessionTotalCost: StateFlow<Double> = messages.flatMapLatest { messageList ->
        flowOf(messageList.sumOf { it.estimatedCostKrw ?: 0.0 })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _isStreaming = MutableStateFlow(false)
    val isStreaming: StateFlow<Boolean> = _isStreaming

    private val _chatInput = MutableStateFlow("")
    val chatInput: StateFlow<String> = _chatInput

    private val _detailItems = MutableStateFlow<ChatResponse.ShowDetails?>(null)
    val detailItems: StateFlow<ChatResponse.ShowDetails?> = _detailItems

    private var chatJob: Job? = null

    fun selectSession(sessionId: Int) {
        _currentSessionId.value = sessionId
        viewModelScope.launch {
            val lastUsedModel = chatRepository.getMessages(sessionId).first()
                .lastOrNull {
                    it.role == ChatMessage.ROLE_ASSISTANT && !it.modelName.isNullOrBlank()
                }
                ?.modelName
            if (_currentSessionId.value == sessionId) {
                _selectedModel.value = lastUsedModel ?: AiModelCatalog.liteModelName
            }
        }
    }

    fun setModel(model: String) {
        _selectedModel.value = model
    }

    fun setHistoryLimit(limit: Int) {
        _selectedHistoryLimit.value = limit
    }

    fun onChatInputChanged(input: String) {
        _chatInput.value = input
    }

    fun createNewChat() {
        _currentSessionId.value = null // 새로운 채팅 준비 상태
        _selectedModel.value = AiModelCatalog.liteModelName
    }

    data class SessionStats(
        val assistantMessageCount: Int,
        val avgResponseTimeMs: Long?,
        val totalTokens: Int,
        val totalCostKrw: Double
    )

    private val _sessionStats = MutableStateFlow<SessionStats?>(null)
    val sessionStats: StateFlow<SessionStats?> = _sessionStats

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
                totalCostKrw = msgs.sumOf { it.estimatedCostKrw ?: 0.0 }
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
        require(imageUris.size + fileUris.size <= 10) { "이미지와 파일은 최대 10개까지 첨부할 수 있습니다." }

        val messageText = content.ifBlank { "이 이미지를 분석해 주세요." }

        _chatInput.value = "" // 메시지 전송 시작 시 입력창 비우기
        chatJob?.cancel()
        chatJob = viewModelScope.launch {
            var sessionId = _currentSessionId.value

            if (sessionId == null) {
                // 새 세션 생성 (제목은 첫 질문의 일부로)
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
            _isStreaming.value = true

            val currentMessages = chatRepository.getMessages(sessionId).first()

            // AI 답변용 빈 메시지 먼저 삽입
            val assistantMessageId = chatRepository.insertMessage(
                ChatMessage(
                    sessionId = sessionId,
                    content = "",
                    role = ChatMessage.ROLE_ASSISTANT
                )
            )

            var fullAssistantContent = ""      // 지금까지 수신한 답변 전체 (누적 버퍼)
            var lastContentSyncAt = 0L         // 마지막으로 저장소에 반영한 시각 (스로틀용)
            var streamCompleted = false        // collect가 예외 없이 끝났는지 (최종 저장 판단용)
            val tempDetailsList = mutableListOf<ChatResponse.ShowDetails>()

            try {
                fun readAttachment(uri: Uri): AiImageAttachment {
                    val bytes = appContext.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                        ?: error("첨부 파일을 읽을 수 없습니다.")
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
                    "첨부 파일의 전체 크기는 20MB 이하여야 합니다."
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
                            // [성능 최적화 2번] 청크마다 저장소에 쓰지 않고, 마지막 반영에서
                            // CONTENT_SYNC_INTERVAL_MS 이상 지났을 때만 쓴다.
                            // (저장소 쓰기 → Flow 재방출 → 리컴포지션 연쇄를 줄이는 핵심)
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
                                estimatedCostKrw = costKrw,
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
                // collect가 끝까지 정상 완료했음을 표시.
                // true여야만 finally에서 최종 내용 저장을 수행한다.
                streamCompleted = true
            } catch (e: kotlinx.coroutines.CancellationException) {
                if (fullAssistantContent.isEmpty()) {
                    chatRepository.updateMessageContent(assistantMessageId, "답변이 중지되었습니다.")
                }
            } catch (e: Exception) {
                chatRepository.updateMessageContent(
                    assistantMessageId,
                    "응답에 실패했습니다: ${e.message ?: "알 수 없는 오류"}"
                )
            } finally {
                // [성능 최적화 2번의 마무리]
                // 스로틀 때문에 마지막 청크 몇 개는 아직 미반영 상태일 수 있다.
                // 정상 종료(streamCompleted == true)일 때만 최종 전체 내용을 한 번 더 저장해
                // 잘린 답변을 방지한다. 오류/중지 경로(catch에서 안내 문구를 쓴 경우)는
                // 그 문구를 덮어쓰지 않도록 건드리지 않는다.
                if (streamCompleted && fullAssistantContent.isNotEmpty()) {
                    chatRepository.updateMessageContent(assistantMessageId, fullAssistantContent)
                }

                // 스트리밍 완료 후 수집된 상세 내역들을 JSON으로 변환하여 저장
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
}
