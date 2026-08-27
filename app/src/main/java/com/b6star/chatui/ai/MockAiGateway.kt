package com.b6star.chatui.ai

import com.b6star.chatui.data.model.ChatMessage
import com.b6star.chatui.di.ServiceLocator
import com.b6star.chatui.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class MockAiGateway : AiGateway {

    override fun chatStream(
        history: List<ChatMessage>,
        model: String,
        historyLimit: Int,
        images: List<AiImageAttachment>,
        files: List<AiFileAttachment>
    ): Flow<ChatResponse> = flow {
        val context = ServiceLocator.context
        val sampleAnswer = context.getString(R.string.mock_answer)
        
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
