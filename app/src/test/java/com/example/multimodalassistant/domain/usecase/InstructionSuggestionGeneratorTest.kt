package com.example.multimodalassistant.domain.usecase

import com.example.multimodalassistant.domain.model.ClassificationResult
import com.example.multimodalassistant.domain.model.DetectedLanguage
import com.example.multimodalassistant.domain.model.OcrResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InstructionSuggestionGeneratorTest {
    private val generator = InstructionSuggestionGenerator()

    @Test
    fun suggestsInvoiceAndDateTasksFromRecognizedText() {
        val suggestions = generator.generate(
            ocrResult = OcrResult(
                fullText = "Invoice 42\nDue date 2026-09-30\nTotal PLN 120.00",
                blocks = emptyList(),
            ),
            classification = null,
        )

        assertTrue(suggestions.first().contains("invoice", ignoreCase = true))
        assertTrue(suggestions.any { it.contains("dates", ignoreCase = true) })
        assertTrue(suggestions.any { it.contains("Summarize") })
    }

    @Test
    fun usesClassificationForImagesWithoutText() {
        val suggestions = generator.generate(
            ocrResult = OcrResult.Empty,
            classification = ClassificationResult("golden retriever", 0.82f, "GPU"),
        )

        assertTrue(suggestions.any { it.contains("golden retriever") })
        assertEquals(suggestions.distinct(), suggestions)
    }

    @Test
    fun suggestsEnglishTranslationForDetectedUkrainian() {
        val suggestions = generator.generate(
            ocrResult = OcrResult(
                fullText = "Це український текст із достатньою кількістю слів для визначення мови.",
                blocks = emptyList(),
            ),
            classification = null,
            detectedLanguage = DetectedLanguage("uk", "Ukrainian", 0.96f),
        )

        assertTrue(suggestions.any { it == "Translate the Ukrainian text to English" })
        assertTrue(suggestions.none { it.contains("to Polish") })
    }

    @Test
    fun doesNotSuggestTranslationWhenEnglishIsDetected() {
        val suggestions = generator.generate(
            ocrResult = OcrResult(
                fullText = "This is a sufficiently long English document for language detection.",
                blocks = emptyList(),
            ),
            classification = null,
            detectedLanguage = DetectedLanguage("en", "English", 0.98f),
        )

        assertTrue(suggestions.none { it.startsWith("Translate") })
    }
}
