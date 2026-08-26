package com.b6star.chatui.ai

import kotlinx.serialization.Serializable

sealed interface ChatResponse {

    data class Chunk(val text: String) : ChatResponse

    data class Metadata(
        val promptTokens: Int? = null,
        val candidatesTokens: Int? = null,
        val totalTokens: Int? = null,
        val thoughtsTokens: Int? = 0,
        val responseTimeMs: Long? = null,
        val modelName: String? = null,
        val agentVersion: String? = null,
        val provider: String? = null,
        val inputHistoryCount: Int? = null,
        val firstPassPrompt: String? = null,
        val firstPassResponse: String? = null,
        val secondPassPrompt: String? = null,
        val secondPassResponse: String? = null
    ) : ChatResponse

    @Serializable
    data class ShowDetails(
        val title: String,
        val items: List<DetailEntry>
    ) : ChatResponse {

        @Serializable
        data class DetailEntry(
            val title: String,
            val date: String,
            val amount: Double
        )
    }
}
