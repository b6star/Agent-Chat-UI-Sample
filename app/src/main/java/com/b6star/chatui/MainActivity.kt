package com.b6star.chatui

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.b6star.chatui.ui.AgentScreen
import com.b6star.chatui.ui.theme.AgentChatUISampleTheme

class MainActivity : AppCompatActivity() {
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
