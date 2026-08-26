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

    // TODO: Replace MockAiGateway with a real AiGateway implementation when connecting to an LLM.
    val aiGateway: AiGateway by lazy { MockAiGateway() }
}
