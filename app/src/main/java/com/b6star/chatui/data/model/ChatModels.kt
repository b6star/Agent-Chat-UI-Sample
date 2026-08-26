package com.b6star.chatui.data.model

data class ChatSession(
    val id: Int? = null,
    val title: String,
    val lastMessageTime: Long = System.currentTimeMillis(),
    val remoteId: String = ""
)

data class ChatMessage(
    val id: Long? = null,
    val sessionId: Int,
    val content: String,
    val role: String,
    val timestamp: Long = System.currentTimeMillis(),
    val promptTokens: Int? = null,
    val candidatesTokens: Int? = null,
    val totalTokens: Int? = null,
    val thoughtsTokens: Int? = null,
    val responseTimeMs: Long? = null,
    val estimatedCostUsd: Double? = null,
    val estimatedCostKrw: Double? = null,
    val modelName: String? = null,
    val agentVersion: String? = null,
    val provider: String? = null,
    val inputHistoryCount: Int? = null,
    val deviceModel: String? = null,
    val osVersion: String? = null,
    val detailsJson: String? = null,
    val firstPassPrompt: String? = null,
    val firstPassResponse: String? = null,
    val secondPassPrompt: String? = null,
    val secondPassResponse: String? = null
) {
    companion object {
        const val ROLE_USER = "user"
        const val ROLE_ASSISTANT = "assistant"
    }
}
