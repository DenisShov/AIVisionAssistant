package com.example.multimodalassistant.di

import com.example.multimodalassistant.data.speech.SpeechToTextManager
import com.example.multimodalassistant.domain.repository.SpeechInput
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import dagger.hilt.android.scopes.ActivityScoped

@Module
@InstallIn(ActivityComponent::class)
abstract class ActivityModule {
    @Binds
    @ActivityScoped
    abstract fun bindSpeechInput(implementation: SpeechToTextManager): SpeechInput
}
