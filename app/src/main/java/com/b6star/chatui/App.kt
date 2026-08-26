package com.b6star.chatui

import android.app.Application
import com.b6star.chatui.di.ServiceLocator

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        ServiceLocator.init(this)
    }
}
