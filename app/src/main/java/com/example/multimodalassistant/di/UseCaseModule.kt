package com.example.multimodalassistant.di

import com.example.multimodalassistant.domain.repository.AssistantRepository
import com.example.multimodalassistant.domain.repository.ImageClassifier
import com.example.multimodalassistant.domain.repository.OcrRepository
import com.example.multimodalassistant.domain.repository.TextLanguageIdentifier
import com.example.multimodalassistant.domain.usecase.AnalyzeImageLocallyUseCase
import com.example.multimodalassistant.domain.usecase.InstructionSuggestionGenerator
import com.example.multimodalassistant.domain.usecase.ProcessAssistantQueryUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.CoroutineDispatcher

@Module
@InstallIn(ViewModelComponent::class)
object UseCaseModule {
    @Provides
    @ViewModelScoped
    fun provideAnalyzeImageLocallyUseCase(
        imageClassifier: ImageClassifier,
        ocrRepository: OcrRepository,
        textLanguageIdentifier: TextLanguageIdentifier,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
    ) = AnalyzeImageLocallyUseCase(
        imageClassifier = imageClassifier,
        ocrRepository = ocrRepository,
        textLanguageIdentifier = textLanguageIdentifier,
        ioDispatcher = ioDispatcher,
    )

    @Provides
    @ViewModelScoped
    fun provideInstructionSuggestionGenerator() = InstructionSuggestionGenerator()

    @Provides
    fun provideProcessAssistantQueryUseCase(
        assistantRepository: AssistantRepository,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
    ) = ProcessAssistantQueryUseCase(assistantRepository, ioDispatcher)
}
