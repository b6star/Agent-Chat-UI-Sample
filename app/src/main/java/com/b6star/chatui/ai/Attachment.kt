package com.b6star.chatui.ai

data class AiImageAttachment(
    val bytes: ByteArray,
    val mimeType: String
)

data class AiFileAttachment(
    val bytes: ByteArray,
    val mimeType: String
)
