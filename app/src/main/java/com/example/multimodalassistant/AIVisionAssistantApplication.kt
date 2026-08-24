package com.example.multimodalassistant

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class AIVisionAssistantApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // App Check must be configured before Firebase AI Logic is first accessed.
        AppCheckConfigurator.install(this)
    }
}
