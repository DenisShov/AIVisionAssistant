package com.example.multimodalassistant.ui

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.multimodalassistant.domain.model.AssistantConfiguration
import com.example.multimodalassistant.domain.model.ClassificationResult
import com.example.multimodalassistant.domain.usecase.AnalyzeImageLocallyUseCase
import com.example.multimodalassistant.domain.usecase.LocalImageAnalysisUpdate
import com.example.multimodalassistant.domain.usecase.ProcessAssistantQueryUseCase
import com.example.multimodalassistant.ui.state.AssistantUiState
import com.example.multimodalassistant.ui.state.Loadable
import com.example.multimodalassistant.ui.state.LocalAnalysisStateReducer
import com.example.multimodalassistant.ui.state.ProcessingStage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AssistantViewModel @Inject constructor(
    private val processAssistantQuery: ProcessAssistantQueryUseCase,
    private val analyzeImageLocally: AnalyzeImageLocallyUseCase,
    private val localAnalysisStateReducer: LocalAnalysisStateReducer,
    private val configuration: AssistantConfiguration,
    private val errorMessageResolver: AssistantErrorMessageResolver,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        AssistantUiState(firebaseConfigured = configuration.isFirebaseConfigured),
    )
    val uiState: StateFlow<AssistantUiState> = _uiState.asStateFlow()

    private var analysisJob: Job? = null
    private var processingJob: Job? = null

    fun setImage(bitmap: Bitmap) {
        analysisJob?.cancel()
        processingJob?.cancel()
        _uiState.value = AssistantUiState(
            image = bitmap,
            ocr = Loadable.Loading,
            classification = Loadable.Loading,
            suggestedInstructions = localAnalysisStateReducer.initialSuggestions(),
            firebaseConfigured = configuration.isFirebaseConfigured,
        )

        analysisJob = viewModelScope.launch {
            analyzeImageLocally(bitmap).collect { update ->
                applyAnalysisUpdate(bitmap, update)
            }
        }
    }

    private fun applyAnalysisUpdate(bitmap: Bitmap, update: LocalImageAnalysisUpdate) {
        _uiState.update { current ->
            if (current.image !== bitmap) return@update current

            localAnalysisStateReducer.reduce(current, update)
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
        processingJob?.cancel()
        updateError(message)
    }

    private fun updateError(message: String) {
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
        if (current.isClassifying) {
            return showError("Wait for on-device classification to finish.")
        }
        val query = promptInput.trim()
        if (query.isBlank()) return showError("Enter, select, or record instructions first.")
        if (!current.firebaseConfigured) {
            return showError("Add app/google-services.json from your Firebase project, then rebuild.")
        }

        processingJob?.cancel()
        processingJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    processingStage = ProcessingStage.SYNTHESIZING,
                    response = null,
                    error = null,
                )
            }

            try {
                val classification = current.classificationResult ?: ClassificationResult(
                    label = "On-device classification unavailable",
                    confidence = 0f,
                    accelerator = "Unavailable",
                )
                val response = processAssistantQuery(
                    query = query,
                    image = image,
                    ocrResult = current.ocrResult,
                    classification = classification,
                )
                _uiState.update { latest ->
                    if (latest.image !== image) latest else latest.copy(
                        processingStage = null,
                        response = response,
                    )
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Exception) {
                updateError(errorMessageResolver.resolve(error))
            }
        }
    }

    override fun onCleared() {
        analyzeImageLocally.close()
    }
}
