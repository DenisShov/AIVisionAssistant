package com.example.multimodalassistant.ui.state

import com.example.multimodalassistant.domain.model.ClassificationResult
import com.example.multimodalassistant.domain.model.OcrResult
import com.example.multimodalassistant.domain.usecase.InstructionSuggestionGenerator
import com.example.multimodalassistant.domain.usecase.LocalImageAnalysisUpdate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalAnalysisStateReducerTest {
    private val reducer = LocalAnalysisStateReducer(InstructionSuggestionGenerator())

    @Test
    fun ocrSuccessReplacesLoadingStateAndRefreshesSuggestions() {
        val ocrResult = OcrResult("Invoice 42\nTotal 100", emptyList())

        val state = reducer.reduce(
            current = AssistantUiState(ocr = Loadable.Loading),
            update = LocalImageAnalysisUpdate.OcrReady(ocrResult),
        )

        assertEquals(ocrResult, state.ocrResult)
        assertFalse(state.isRecognizingText)
        assertTrue(state.suggestedInstructions.any { it.contains("invoice", ignoreCase = true) })
    }

    @Test
    fun classifierFailureCannotRetainPreviousClassifierResult() {
        val current = AssistantUiState(
            classification = Loadable.Ready(ClassificationResult("document", 0.8f, "GPU")),
        )

        val state = reducer.reduce(
            current = current,
            update = LocalImageAnalysisUpdate.ClassificationFailed(
                IllegalStateException("NPU unavailable"),
            ),
        )

        assertEquals("NPU unavailable", state.classificationError)
        assertNull(state.classificationResult)
    }
}
