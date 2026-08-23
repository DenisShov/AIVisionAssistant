package com.example.multimodalassistant.domain.usecase

import com.example.multimodalassistant.domain.model.ClassificationResult
import com.example.multimodalassistant.domain.model.NormalizedBoundingBox
import com.example.multimodalassistant.domain.model.OcrResult
import com.example.multimodalassistant.domain.model.OcrTextBlock
import org.junit.Assert.assertTrue
import org.junit.Test

class PromptComposerTest {
    @Test
    fun includesUserQueryAndLocalContext() {
        val prompt = PromptComposer.compose(
            query = "Summarize this and translate it to Polish",
            classification = ClassificationResult(
                label = "notebook",
                confidence = 0.82f,
                accelerator = "GPU",
            ),
        )

        assertTrue(prompt.contains("notebook"))
        assertTrue(prompt.contains("82%"))
        assertTrue(prompt.contains("GPU"))
        assertTrue(prompt.contains("translate it to Polish"))
        assertTrue(prompt.contains("hint, not as ground truth"))
    }

    @Test
    fun includesNumberedOcrBlocksAsLocalEvidence() {
        val prompt = PromptComposer.compose(
            query = "When is this due?",
            classification = ClassificationResult("invoice", 0.91f, "GPU"),
            ocrResult = OcrResult(
                fullText = "Payment is due on 30 September 2026",
                blocks = listOf(
                    OcrTextBlock(
                        id = 4,
                        text = "Payment is due on 30 September 2026",
                        boundingBox = NormalizedBoundingBox(0.1f, 0.2f, 0.8f, 0.3f),
                        languageCodes = listOf("en"),
                    ),
                ),
            ),
        )

        assertTrue(prompt.contains("[OCR block 4]"))
        assertTrue(prompt.contains("30 September 2026"))
        assertTrue(prompt.contains("cite supporting recognized text"))
    }
}
