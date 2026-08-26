package com.b6star.chatui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.b6star.chatui.ui.AgentScreen
import com.b6star.chatui.ui.theme.AgentChatUISampleTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AgentChatUISampleTheme {
                AgentScreen()
            }
        }
    }
}
