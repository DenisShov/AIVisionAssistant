package com.example.multimodalassistant.ui.state

import com.example.multimodalassistant.domain.model.ClassificationResult
import com.example.multimodalassistant.domain.model.DetectedLanguage
import com.example.multimodalassistant.domain.model.OcrResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AssistantUiStateTest {
    @Test
    fun loadingStatesExposeOnlyProgress() {
        val state = AssistantUiState(
            ocr = Loadable.Loading,
            language = Loadable.Loading,
            classification = Loadable.Loading,
        )

        assertTrue(state.isRecognizingText)
        assertTrue(state.isIdentifyingLanguage)
        assertTrue(state.isClassifying)
        assertNull(state.ocrResult)
        assertNull(state.classificationResult)
    }

    @Test
    fun readyStatesExposeTheirValuesWithoutErrors() {
        val ocr = OcrResult("Hello", emptyList())
        val language = DetectedLanguage("en", "English", 0.9f)
        val classification = ClassificationResult("document", 0.8f, "GPU")
        val state = AssistantUiState(
            ocr = Loadable.Ready(ocr),
            language = Loadable.Ready(language),
            classification = Loadable.Ready(classification),
        )

        assertEquals(ocr, state.ocrResult)
        assertEquals(language, state.detectedLanguage)
        assertEquals(classification, state.classificationResult)
        assertFalse(state.isRecognizingText)
        assertNull(state.ocrError)
    }

    @Test
    fun failedStateCannotAlsoExposeAStaleResult() {
        val state = AssistantUiState(
            classification = Loadable.Failed("Classifier unavailable"),
        )

        assertEquals("Classifier unavailable", state.classificationError)
        assertNull(state.classificationResult)
        assertFalse(state.isClassifying)
    }
}
