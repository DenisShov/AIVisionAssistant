package com.example.multimodalassistant.domain.usecase

import com.example.multimodalassistant.domain.model.ClassificationResult
import com.example.multimodalassistant.domain.model.OcrResult

object PromptComposer {
    fun compose(
        query: String,
        classification: ClassificationResult,
        ocrResult: OcrResult? = null,
    ): String {
        val ocrContext = ocrResult
            ?.blocks
            ?.take(MAX_OCR_BLOCKS)
            ?.joinToString(separator = "\n") { block ->
                "[OCR block ${block.id}]: ${block.text.replace('\n', ' ')}"
            }
            ?.take(MAX_OCR_CHARACTERS)
            ?.takeIf(String::isNotBlank)
            ?: "No readable text was detected locally."

        return """
        Context from the local on-device LiteRT model:
        - Classification: ${classification.label}
        - Confidence: ${(classification.confidence * 100).toInt()}%
        - Accelerator: ${classification.accelerator}

        Text recognized locally with ML Kit (block numbers correspond to boxes in the app):
        $ocrContext

        User's instructions:
        "$query"

        Follow the user's instructions for the attached document or image. Treat the local
        classification as a hint, not as ground truth, and independently verify OCR against the
        image. When useful, cite supporting recognized text as [OCR block N]. Give a clear,
        practical answer and call out uncertainty where appropriate.
        """.trimIndent()
    }

    private const val MAX_OCR_BLOCKS = 100
    private const val MAX_OCR_CHARACTERS = 12_000
}
