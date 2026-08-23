package com.example.multimodalassistant

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory

object AppCheckConfigurator {
    fun install(context: Context) {
        if (!BuildConfig.FIREBASE_CONFIGURED) return
        FirebaseApp.initializeApp(context) ?: return
        FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
            PlayIntegrityAppCheckProviderFactory.getInstance(),
        )
    }
}
