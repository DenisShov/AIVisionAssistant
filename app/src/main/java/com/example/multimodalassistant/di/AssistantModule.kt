package com.example.multimodalassistant.di

import com.example.multimodalassistant.data.classifier.LiteRtImageClassifier
import com.example.multimodalassistant.data.language.MlKitTextLanguageIdentifier
import com.example.multimodalassistant.data.ocr.MlKitOcrRepository
import com.example.multimodalassistant.data.remote.FirebaseAssistantRepository
import com.example.multimodalassistant.domain.repository.AssistantRepository
import com.example.multimodalassistant.domain.repository.ImageClassifier
import com.example.multimodalassistant.domain.repository.OcrRepository
import com.example.multimodalassistant.domain.repository.TextLanguageIdentifier
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.scopes.ViewModelScoped

@Module
@InstallIn(ViewModelComponent::class)
abstract class AssistantModule {
    @Binds
    @ViewModelScoped
    abstract fun bindAssistantRepository(
        implementation: FirebaseAssistantRepository,
    ): AssistantRepository

    @Binds
    @ViewModelScoped
    abstract fun bindImageClassifier(
        implementation: LiteRtImageClassifier,
    ): ImageClassifier

    @Binds
    @ViewModelScoped
    abstract fun bindOcrRepository(
        implementation: MlKitOcrRepository,
    ): OcrRepository

    @Binds
    @ViewModelScoped
    abstract fun bindTextLanguageIdentifier(
        implementation: MlKitTextLanguageIdentifier,
    ): TextLanguageIdentifier
}
