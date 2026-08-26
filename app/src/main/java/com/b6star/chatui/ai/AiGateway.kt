package com.b6star.chatui.ai

import com.b6star.chatui.data.model.ChatMessage
import kotlinx.coroutines.flow.Flow

/**
 * LLM Provider 교체 지점.
 * 포크한 프로젝트에서 실제 모델(Gemini/OpenAI/로컬 등)을 연결하려면
 * 이 인터페이스를 구현한 클래스를 ServiceLocator.aiGateway 에서 반환만 바꾸면 된다.
 *
 * 구현 규칙:
 * - 응답 텍스트는 [ChatResponse.Chunk]로 순차 방출한다 (스트리밍).
 * - 스트림 종료 직전에 [ChatResponse.Metadata]를 한 번 방출하면
 *   토큰 수 / 응답 시간 등이 말풍선 하단 메타데이터에 표시된다. (생략 가능)
 */
interface AiGateway {
    fun chatStream(
        history: List<ChatMessage>,
        model: String,
        historyLimit: Int,
        images: List<AiImageAttachment> = emptyList(),
        files: List<AiFileAttachment> = emptyList()
    ): Flow<ChatResponse>
}
