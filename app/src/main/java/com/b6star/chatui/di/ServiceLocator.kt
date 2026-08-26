package com.b6star.chatui.di

import android.content.Context
import com.b6star.chatui.ai.AiGateway
import com.b6star.chatui.ai.MockAiGateway
import com.b6star.chatui.data.ChatRepository

object ServiceLocator {

    private lateinit var appContext: Context

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    val context: Context
        get() = appContext

    val chatRepository: ChatRepository by lazy { ChatRepository() }

    // TODO: 실제 LLM 연결 시 MockAiGateway 대신 AiGateway 구현체로 교체
    val aiGateway: AiGateway by lazy { MockAiGateway() }
}
