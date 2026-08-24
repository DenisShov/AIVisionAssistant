package com.example.multimodalassistant.ui.state

import com.example.multimodalassistant.domain.usecase.InstructionSuggestionGenerator
import com.example.multimodalassistant.domain.usecase.LocalImageAnalysisUpdate
import javax.inject.Inject

class LocalAnalysisStateReducer @Inject constructor(
    private val suggestionGenerator: InstructionSuggestionGenerator,
) {
    fun initialSuggestions(): List<String> = suggestionGenerator.generate(null, null)

    fun reduce(
        current: AssistantUiState,
        update: LocalImageAnalysisUpdate,
    ): AssistantUiState {
        val updated = when (update) {
            is LocalImageAnalysisUpdate.OcrReady -> current.copy(
                ocr = Loadable.Ready(update.result),
                language = if (update.result.hasText) current.language else Loadable.Idle,
            )
            is LocalImageAnalysisUpdate.OcrFailed -> current.copy(
                ocr = Loadable.Failed(
                    update.cause.localizedMessage ?: "Text recognition failed.",
                ),
                language = Loadable.Idle,
            )
            LocalImageAnalysisUpdate.LanguageIdentificationStarted -> current.copy(
                language = Loadable.Loading,
            )
            is LocalImageAnalysisUpdate.LanguageReady -> current.copy(
                language = Loadable.Ready(update.language),
            )
            is LocalImageAnalysisUpdate.LanguageFailed -> current.copy(
                language = Loadable.Failed(
                    update.cause.localizedMessage ?: "Language identification failed.",
                ),
            )
            is LocalImageAnalysisUpdate.ClassificationReady -> current.copy(
                classification = Loadable.Ready(update.result),
            )
            is LocalImageAnalysisUpdate.ClassificationFailed -> current.copy(
                classification = Loadable.Failed(
                    update.cause.localizedMessage
                        ?: "On-device image classification failed.",
                ),
            )
        }

        return updated.copy(
            suggestedInstructions = suggestionGenerator.generate(
                updated.ocrResult,
                updated.classificationResult,
                updated.detectedLanguage,
            ),
        )
    }
}
