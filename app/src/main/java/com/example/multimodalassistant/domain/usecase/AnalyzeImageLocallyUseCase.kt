package com.example.multimodalassistant.domain.usecase

import android.graphics.Bitmap
import com.example.multimodalassistant.domain.model.ClassificationResult
import com.example.multimodalassistant.domain.model.DetectedLanguage
import com.example.multimodalassistant.domain.model.OcrResult
import com.example.multimodalassistant.domain.repository.ImageClassifier
import com.example.multimodalassistant.domain.repository.OcrRepository
import com.example.multimodalassistant.domain.repository.TextLanguageIdentifier
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch

sealed interface LocalImageAnalysisUpdate {
    data class OcrReady(val result: OcrResult) : LocalImageAnalysisUpdate
    data class OcrFailed(val cause: Throwable) : LocalImageAnalysisUpdate
    data object LanguageIdentificationStarted : LocalImageAnalysisUpdate
    data class LanguageReady(val language: DetectedLanguage?) : LocalImageAnalysisUpdate
    data class LanguageFailed(val cause: Throwable) : LocalImageAnalysisUpdate
    data class ClassificationReady(val result: ClassificationResult) : LocalImageAnalysisUpdate
    data class ClassificationFailed(val cause: Throwable) : LocalImageAnalysisUpdate
}

class AnalyzeImageLocallyUseCase(
    private val imageClassifier: ImageClassifier,
    private val ocrRepository: OcrRepository,
    private val textLanguageIdentifier: TextLanguageIdentifier,
    private val ioDispatcher: CoroutineDispatcher,
) : AutoCloseable {

    operator fun invoke(image: Bitmap): Flow<LocalImageAnalysisUpdate> = channelFlow {
        launch(ioDispatcher) {
            try {
                send(LocalImageAnalysisUpdate.ClassificationReady(imageClassifier.classify(image)))
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Exception) {
                send(LocalImageAnalysisUpdate.ClassificationFailed(error))
            }
        }

        launch(ioDispatcher) {
            try {
                val ocrResult = ocrRepository.recognize(image)
                send(LocalImageAnalysisUpdate.OcrReady(ocrResult))
                if (ocrResult.hasText) identifyLanguage(ocrResult)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Exception) {
                send(LocalImageAnalysisUpdate.OcrFailed(error))
            }
        }
    }

    private suspend fun ProducerScope<LocalImageAnalysisUpdate>.identifyLanguage(
        ocrResult: OcrResult,
    ) {
        send(LocalImageAnalysisUpdate.LanguageIdentificationStarted)
        try {
            val language = textLanguageIdentifier.identify(ocrResult.fullText)
            send(LocalImageAnalysisUpdate.LanguageReady(language))
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Exception) {
            send(LocalImageAnalysisUpdate.LanguageFailed(error))
        }
    }

    override fun close() {
        imageClassifier.close()
        ocrRepository.close()
        textLanguageIdentifier.close()
    }
}
