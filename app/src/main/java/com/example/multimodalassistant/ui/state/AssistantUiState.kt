package com.example.multimodalassistant.ui.state

import android.graphics.Bitmap
import com.example.multimodalassistant.domain.model.ClassificationResult
import com.example.multimodalassistant.domain.model.DetectedLanguage
import com.example.multimodalassistant.domain.model.OcrResult

data class AssistantUiState(
    val image: Bitmap? = null,
    val isScanning: Boolean = false,
    val isImporting: Boolean = false,
    val ocr: Loadable<OcrResult> = Loadable.Idle,
    val language: Loadable<DetectedLanguage?> = Loadable.Idle,
    val classification: Loadable<ClassificationResult> = Loadable.Idle,
    val showOcrBoxes: Boolean = true,
    val processingStage: ProcessingStage? = null,
    val suggestedInstructions: List<String> = emptyList(),
    val response: String? = null,
    val error: String? = null,
    val firebaseConfigured: Boolean = false,
) {
    val isRecognizingText: Boolean get() = ocr is Loadable.Loading
    val ocrResult: OcrResult? get() = ocr.valueOrNull()
    val ocrError: String? get() = ocr.errorOrNull()

    val isIdentifyingLanguage: Boolean get() = language is Loadable.Loading
    val detectedLanguage: DetectedLanguage? get() = language.valueOrNull()
    val languageIdentificationAttempted: Boolean
        get() = language is Loadable.Ready || language is Loadable.Failed
    val languageIdentificationError: String? get() = language.errorOrNull()

    val isClassifying: Boolean get() = classification is Loadable.Loading
    val classificationResult: ClassificationResult? get() = classification.valueOrNull()
    val classificationError: String? get() = classification.errorOrNull()

    fun canSubmit(prompt: String): Boolean =
        image != null &&
            prompt.isNotBlank() &&
            !isRecognizingText &&
            !isClassifying &&
            processingStage == null
}

enum class ProcessingStage(val message: String) {
    SYNTHESIZING("Asking Gemini 3.6 Flash…"),
}
