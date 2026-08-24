package com.example.multimodalassistant.ui

import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.multimodalassistant.BuildConfig
import com.example.multimodalassistant.data.classifier.LiteRtImageClassifier
import com.example.multimodalassistant.data.language.MlKitTextLanguageIdentifier
import com.example.multimodalassistant.data.ocr.MlKitOcrRepository
import com.example.multimodalassistant.data.remote.FirebaseAssistantRepository
import com.example.multimodalassistant.domain.model.ClassificationResult
import com.example.multimodalassistant.domain.model.DetectedLanguage
import com.example.multimodalassistant.domain.model.OcrResult
import com.example.multimodalassistant.domain.repository.ImageClassifier
import com.example.multimodalassistant.domain.repository.OcrRepository
import com.example.multimodalassistant.domain.repository.TextLanguageIdentifier
import com.example.multimodalassistant.domain.usecase.InstructionSuggestionGenerator
import com.example.multimodalassistant.domain.usecase.ProcessAssistantQueryUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class ProcessingStage(val message: String) {
    SYNTHESIZING("Asking Gemini 3.6 Flash…"),
}

data class AssistantUiState(
    val image: Bitmap? = null,
    val isScanning: Boolean = false,
    val isImporting: Boolean = false,
    val isRecognizingText: Boolean = false,
    val isIdentifyingLanguage: Boolean = false,
    val isClassifying: Boolean = false,
    val ocrResult: OcrResult? = null,
    val ocrError: String? = null,
    val detectedLanguage: DetectedLanguage? = null,
    val languageIdentificationAttempted: Boolean = false,
    val languageIdentificationError: String? = null,
    val showOcrBoxes: Boolean = true,
    val processingStage: ProcessingStage? = null,
    val classification: ClassificationResult? = null,
    val classificationError: String? = null,
    val suggestedInstructions: List<String> = emptyList(),
    val response: String? = null,
    val error: String? = null,
    val firebaseConfigured: Boolean = BuildConfig.FIREBASE_CONFIGURED,
) {
    fun canSubmit(prompt: String): Boolean =
        image != null &&
            prompt.isNotBlank() &&
            !isRecognizingText &&
            !isClassifying &&
            processingStage == null
}

class AssistantViewModel(
    private val processAssistantQuery: ProcessAssistantQueryUseCase,
    private val imageClassifier: ImageClassifier,
    private val ocrRepository: OcrRepository,
    private val textLanguageIdentifier: TextLanguageIdentifier,
) : ViewModel() {
    private val _uiState = MutableStateFlow(AssistantUiState())
    val uiState: StateFlow<AssistantUiState> = _uiState.asStateFlow()
    private var ocrJob: Job? = null
    private var classificationJob: Job? = null

    fun setImage(bitmap: Bitmap) {
        ocrJob?.cancel()
        classificationJob?.cancel()
        _uiState.update {
            it.copy(
                image = bitmap,
                isRecognizingText = true,
                isIdentifyingLanguage = false,
                isClassifying = true,
                ocrResult = null,
                ocrError = null,
                detectedLanguage = null,
                languageIdentificationAttempted = false,
                languageIdentificationError = null,
                showOcrBoxes = true,
                classification = null,
                classificationError = null,
                suggestedInstructions = InstructionSuggestionGenerator.generate(null, null),
                response = null,
                error = null,
                isScanning = false,
                isImporting = false,
            )
        }

        ocrJob = viewModelScope.launch {
            try {
                val result = ocrRepository.recognize(bitmap)
                _uiState.update { current ->
                    if (current.image !== bitmap) current else current.copy(
                        isRecognizingText = false,
                        isIdentifyingLanguage = result.hasText,
                        ocrResult = result,
                        ocrError = null,
                        suggestedInstructions = InstructionSuggestionGenerator.generate(
                            result,
                            current.classification,
                        ),
                    )
                }

                if (result.hasText) {
                    try {
                        val detectedLanguage = textLanguageIdentifier.identify(result.fullText)
                        _uiState.update { current ->
                            if (current.image !== bitmap) current else current.copy(
                                isIdentifyingLanguage = false,
                                detectedLanguage = detectedLanguage,
                                languageIdentificationAttempted = true,
                                languageIdentificationError = null,
                                suggestedInstructions = InstructionSuggestionGenerator.generate(
                                    result,
                                    current.classification,
                                    detectedLanguage,
                                ),
                            )
                        }
                    } catch (cancellation: CancellationException) {
                        throw cancellation
                    } catch (error: Exception) {
                        _uiState.update { current ->
                            if (current.image !== bitmap) current else current.copy(
                                isIdentifyingLanguage = false,
                                detectedLanguage = null,
                                languageIdentificationAttempted = true,
                                languageIdentificationError = error.localizedMessage
                                    ?: "Language identification failed.",
                                suggestedInstructions = InstructionSuggestionGenerator.generate(
                                    result,
                                    current.classification,
                                ),
                            )
                        }
                    }
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Exception) {
                _uiState.update { current ->
                    if (current.image !== bitmap) current else current.copy(
                        isRecognizingText = false,
                        isIdentifyingLanguage = false,
                        ocrResult = null,
                        ocrError = error.localizedMessage ?: "Text recognition failed.",
                        suggestedInstructions = InstructionSuggestionGenerator.generate(
                            null,
                            current.classification,
                        ),
                    )
                }
            }
        }

        classificationJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                val result = imageClassifier.classify(bitmap)
                _uiState.update { current ->
                    if (current.image !== bitmap) current else current.copy(
                        isClassifying = false,
                        classification = result,
                        classificationError = null,
                        suggestedInstructions = InstructionSuggestionGenerator.generate(
                            current.ocrResult,
                            result,
                            current.detectedLanguage,
                        ),
                    )
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Exception) {
                _uiState.update { current ->
                    if (current.image !== bitmap) current else current.copy(
                        isClassifying = false,
                        classification = null,
                        classificationError = error.localizedMessage
                            ?: "On-device image classification failed.",
                        suggestedInstructions = InstructionSuggestionGenerator.generate(
                            current.ocrResult,
                            null,
                            current.detectedLanguage,
                        ),
                    )
                }
            }
        }
    }

    fun toggleOcrBoxes() {
        _uiState.update { it.copy(showOcrBoxes = !it.showOcrBoxes) }
    }

    fun setScanning(isScanning: Boolean) {
        _uiState.update { it.copy(isScanning = isScanning, error = null) }
    }

    fun setImporting(isImporting: Boolean) {
        _uiState.update { it.copy(isImporting = isImporting, error = null) }
    }

    fun showError(message: String) {
        _uiState.update {
            it.copy(
                error = message,
                processingStage = null,
                isScanning = false,
                isImporting = false,
            )
        }
    }

    fun process(promptInput: String) {
        val current = _uiState.value
        val image = current.image ?: return showError("Scan or import an image first.")
        if (current.isClassifying) return showError("Wait for on-device classification to finish.")
        val query = promptInput.trim()
        if (query.isBlank()) return showError("Enter, select, or record instructions first.")
        if (!current.firebaseConfigured) {
            return showError("Add app/google-services.json from your Firebase project, then rebuild.")
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    processingStage = ProcessingStage.SYNTHESIZING,
                    response = null,
                    error = null,
                )
            }

            try {
                val classification = current.classification ?: ClassificationResult(
                    label = "On-device classification unavailable",
                    confidence = 0f,
                    accelerator = "Unavailable",
                )
                val response = withContext(Dispatchers.IO) {
                    processAssistantQuery(
                        query = query,
                        image = image,
                        ocrResult = current.ocrResult,
                        classification = classification,
                    )
                }
                _uiState.update {
                    it.copy(
                        processingStage = null,
                        response = response,
                    )
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Exception) {
                showError(error.toUserMessage())
            }
        }
    }

    override fun onCleared() {
        processAssistantQuery.close()
        imageClassifier.close()
        ocrRepository.close()
        textLanguageIdentifier.close()
        _uiState.value.image?.recycle()
    }

    companion object {
        fun factory(context: Context): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                require(modelClass.isAssignableFrom(AssistantViewModel::class.java))
                val classifier = LiteRtImageClassifier(context.applicationContext)
                val repository = FirebaseAssistantRepository()
                val ocrRepository = MlKitOcrRepository()
                val textLanguageIdentifier = MlKitTextLanguageIdentifier()
                return AssistantViewModel(
                    ProcessAssistantQueryUseCase(repository),
                    classifier,
                    ocrRepository,
                    textLanguageIdentifier,
                ) as T
            }
        }
    }
}

private fun Throwable.toUserMessage(): String {
    val messages = generateSequence(this) { it.cause }
        .mapNotNull { it.localizedMessage }
        .toList()
    val isInvalidAppCheckToken = messages.any { message ->
        message.contains("App Check token is invalid", ignoreCase = true) ||
            message.contains("App attestation failed", ignoreCase = true)
    }

    if (isInvalidAppCheckToken) {
        return if (BuildConfig.DEBUG) {
            "Firebase rejected this device's debug App Check token. In Logcat, search for " +
                "DebugAppCheckProvider, then register that secret under Firebase Console > " +
                "App Check > Apps > Manage debug tokens. If you changed Firebase projects, " +
                "clear this app's data first to generate a new secret."
        } else {
            "Firebase rejected Play Integrity. Install the release from a Google Play testing " +
                "track and register the Play app-signing SHA-256 certificate in Firebase App Check."
        }
    }

    return messages.firstOrNull { it.isNotBlank() } ?: "The multimodal request failed."
}
