package com.example.multimodalassistant.di

import com.example.multimodalassistant.BuildConfig
import com.example.multimodalassistant.domain.model.AssistantConfiguration
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ApplicationModule {
    @Provides
    @Singleton
    fun provideAssistantConfiguration(): AssistantConfiguration = AssistantConfiguration(
        isFirebaseConfigured = BuildConfig.FIREBASE_CONFIGURED,
        isDebugBuild = BuildConfig.DEBUG,
    )
}
