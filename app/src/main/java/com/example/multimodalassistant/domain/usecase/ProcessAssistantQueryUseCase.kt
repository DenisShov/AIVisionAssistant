package com.example.multimodalassistant.domain.usecase

import android.graphics.Bitmap
import com.example.multimodalassistant.domain.model.ClassificationResult
import com.example.multimodalassistant.domain.model.OcrResult
import com.example.multimodalassistant.domain.repository.AssistantRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

class ProcessAssistantQueryUseCase(
    private val assistantRepository: AssistantRepository,
    private val ioDispatcher: CoroutineDispatcher,
) {

    suspend operator fun invoke(
        query: String,
        image: Bitmap,
        ocrResult: OcrResult?,
        classification: ClassificationResult,
    ): String = withContext(ioDispatcher) {
        assistantRepository.analyze(query, image, classification, ocrResult)
    }
}
