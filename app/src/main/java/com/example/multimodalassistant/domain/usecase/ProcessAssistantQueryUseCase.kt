package com.example.multimodalassistant.domain.usecase

import android.graphics.Bitmap
import com.example.multimodalassistant.domain.model.ClassificationResult
import com.example.multimodalassistant.domain.model.OcrResult
import com.example.multimodalassistant.domain.repository.AssistantRepository
import com.example.multimodalassistant.domain.repository.ImageClassifier

class ProcessAssistantQueryUseCase(
    private val classifier: ImageClassifier,
    private val assistantRepository: AssistantRepository,
) : AutoCloseable {

    suspend operator fun invoke(
        query: String,
        image: Bitmap,
        ocrResult: OcrResult?,
        onClassified: (ClassificationResult) -> Unit,
    ): Pair<ClassificationResult, String> {
        val classification = classifier.classify(image)
        onClassified(classification)
        val response = assistantRepository.analyze(query, image, classification, ocrResult)
        return classification to response
    }

    override fun close() {
        classifier.close()
        assistantRepository.close()
    }
}
