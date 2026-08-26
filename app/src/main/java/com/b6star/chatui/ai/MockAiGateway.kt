package com.b6star.chatui.ai

import com.b6star.chatui.data.model.ChatMessage
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class MockAiGateway : AiGateway {

    private val sampleAnswer = """
        ## Mock 스트리밍 응답

        이 답변은 **MockAiGateway**가 조각(chunk) 단위로 흘려보내는 샘플입니다.
        실제 Provider를 연결하려면 `ai/AiGateway`를 구현한 클래스를 만들고
        `di/ServiceLocator.kt`의 `aiGateway` 반환값만 교체하면 됩니다.

        ### 지원하는 렌더링
        - `**굵게**`, `*기울임*`, `` `인라인 코드` `` 마크다운
        - 코드 블록 하이라이팅 (highlight.js)
        - Mermaid 다이어그램

        ```kotlin
        class GeminiGateway : AiGateway {
            override fun chatStream(
                history: List<ChatMessage>,
                model: String,
                historyLimit: Int,
                images: List<AiImageAttachment>,
                files: List<AiFileAttachment>
            ): Flow<ChatResponse> = flow {
                // 실제 SDK의 스트리밍 API를 여기서 Flow로 변환합니다.
            }
        }
        ```

        ```mermaid
        graph LR
            A[사용자 입력] --> B[AiGateway]
            B --> C{Provider}
            C --> D[Chunk 스트림]
            D --> E[ChatBubble]
        ```

        스트리밍 중에는 250ms 간격으로 DB에 반영되어 리컴포지션이 최적화됩니다.
        """.trimIndent()

    override fun chatStream(
        history: List<ChatMessage>,
        model: String,
        historyLimit: Int,
        images: List<AiImageAttachment>,
        files: List<AiFileAttachment>
    ): Flow<ChatResponse> = flow {
        val startedAt = System.currentTimeMillis()
        val trimmed = if (historyLimit in 1..history.size) history.takeLast(historyLimit) else history

        sampleAnswer.chunked(12).forEach { piece ->
            emit(ChatResponse.Chunk(piece))
            delay(25)
        }

        val promptTokens = trimmed.sumOf { it.content.length / 4 + 1 }
        val candidatesTokens = sampleAnswer.length / 4

        emit(
            ChatResponse.Metadata(
                promptTokens = promptTokens,
                candidatesTokens = candidatesTokens,
                totalTokens = promptTokens + candidatesTokens,
                responseTimeMs = System.currentTimeMillis() - startedAt,
                modelName = model,
                agentVersion = "mock-1.0",
                provider = "mock",
                inputHistoryCount = trimmed.size
            )
        )
    }
}
