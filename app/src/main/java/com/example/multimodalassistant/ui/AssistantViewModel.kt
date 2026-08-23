package com.example.multimodalassistant.ui

import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.multimodalassistant.BuildConfig
import com.example.multimodalassistant.data.classifier.LiteRtImageClassifier
import com.example.multimodalassistant.data.ocr.MlKitOcrRepository
import com.example.multimodalassistant.data.remote.FirebaseAssistantRepository
import com.example.multimodalassistant.domain.model.ClassificationResult
import com.example.multimodalassistant.domain.model.OcrResult
import com.example.multimodalassistant.domain.repository.OcrRepository
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
    CLASSIFYING("Running MobileNet V2 locally with LiteRT…"),
    SYNTHESIZING("LiteRT complete. Asking Gemini 3.6 Flash…"),
}

data class AssistantUiState(
    val prompt: String = "",
    val image: Bitmap? = null,
    val isScanning: Boolean = false,
    val isRecognizingText: Boolean = false,
    val ocrResult: OcrResult? = null,
    val ocrError: String? = null,
    val showOcrBoxes: Boolean = true,
    val processingStage: ProcessingStage? = null,
    val classification: ClassificationResult? = null,
    val response: String? = null,
    val error: String? = null,
    val firebaseConfigured: Boolean = BuildConfig.FIREBASE_CONFIGURED,
) {
    val canSubmit: Boolean
        get() = image != null && prompt.isNotBlank() && !isRecognizingText && processingStage == null
}

class AssistantViewModel(
    private val processAssistantQuery: ProcessAssistantQueryUseCase,
    private val ocrRepository: OcrRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(AssistantUiState())
    val uiState: StateFlow<AssistantUiState> = _uiState.asStateFlow()
    private var ocrJob: Job? = null

    fun updatePrompt(prompt: String) {
        _uiState.update { it.copy(prompt = prompt, error = null) }
    }

    fun setImage(bitmap: Bitmap) {
        ocrJob?.cancel()
        _uiState.update {
            it.copy(
                image = bitmap,
                isRecognizingText = true,
                ocrResult = null,
                ocrError = null,
                showOcrBoxes = true,
                classification = null,
                response = null,
                error = null,
                isScanning = false,
            )
        }

        ocrJob = viewModelScope.launch {
            try {
                val result = ocrRepository.recognize(bitmap)
                _uiState.update { current ->
                    if (current.image !== bitmap) current else current.copy(
                        isRecognizingText = false,
                        ocrResult = result,
                        ocrError = null,
                    )
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Exception) {
                _uiState.update { current ->
                    if (current.image !== bitmap) current else current.copy(
                        isRecognizingText = false,
                        ocrResult = null,
                        ocrError = error.localizedMessage ?: "Text recognition failed.",
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

    fun showError(message: String) {
        _uiState.update { it.copy(error = message, processingStage = null, isScanning = false) }
    }

    fun process() {
        val current = _uiState.value
        val image = current.image ?: return showError("Scan or import an image first.")
        val query = current.prompt.trim()
        if (query.isBlank()) return showError("Enter or record a question first.")
        if (!current.firebaseConfigured) {
            return showError("Add app/google-services.json from your Firebase project, then rebuild.")
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    processingStage = ProcessingStage.CLASSIFYING,
                    classification = null,
                    response = null,
                    error = null,
                )
            }

            try {
                val (classification, response) = withContext(Dispatchers.IO) {
                    processAssistantQuery(query, image, current.ocrResult) { localResult ->
                        _uiState.update {
                            it.copy(
                                processingStage = ProcessingStage.SYNTHESIZING,
                                classification = localResult,
                            )
                        }
                    }
                }
                _uiState.update {
                    it.copy(
                        processingStage = null,
                        classification = classification,
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
        ocrRepository.close()
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
                return AssistantViewModel(
                    ProcessAssistantQueryUseCase(classifier, repository),
                    ocrRepository,
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
