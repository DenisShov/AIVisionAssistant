package com.example.multimodalassistant.domain.usecase

import android.graphics.Bitmap
import com.example.multimodalassistant.domain.model.ClassificationResult
import com.example.multimodalassistant.domain.model.OcrResult
import com.example.multimodalassistant.domain.repository.AssistantRepository

class ProcessAssistantQueryUseCase(
    private val assistantRepository: AssistantRepository,
) : AutoCloseable {

    suspend operator fun invoke(
        query: String,
        image: Bitmap,
        ocrResult: OcrResult?,
        classification: ClassificationResult,
    ): String {
        return assistantRepository.analyze(query, image, classification, ocrResult)
    }

    override fun close() {
        assistantRepository.close()
    }
}
