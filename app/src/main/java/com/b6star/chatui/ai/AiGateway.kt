package com.b6star.chatui.ai

import com.b6star.chatui.data.model.ChatMessage
import kotlinx.coroutines.flow.Flow

/**
 * LLM Provider replacement point.
 * To connect actual models (Gemini/OpenAI/Local, etc.) in a forked project,
 * simply change the return value in ServiceLocator.aiGateway to a class implementing this interface.
 *
 * Implementation Rules:
 * - Sequentially emit response text as [ChatResponse.Chunk] (Streaming).
 * - Emitting [ChatResponse.Metadata] once just before the stream ends will
 *   display token counts / response time in the metadata at the bottom of the bubble. (Optional)
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
