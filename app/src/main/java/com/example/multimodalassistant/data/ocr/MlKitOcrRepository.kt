package com.example.multimodalassistant.data.ocr

import android.graphics.Bitmap
import com.example.multimodalassistant.domain.model.NormalizedBoundingBox
import com.example.multimodalassistant.domain.model.OcrResult
import com.example.multimodalassistant.domain.model.OcrTextBlock
import com.example.multimodalassistant.domain.repository.OcrRepository
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.tasks.await

class MlKitOcrRepository : OcrRepository {
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    override suspend fun recognize(bitmap: Bitmap): OcrResult {
        val result = recognizer.process(InputImage.fromBitmap(bitmap, 0)).await()
        val imageWidth = bitmap.width.toFloat().coerceAtLeast(1f)
        val imageHeight = bitmap.height.toFloat().coerceAtLeast(1f)
        val blocks = result.textBlocks.mapIndexedNotNull { index, block ->
            val bounds = block.boundingBox ?: return@mapIndexedNotNull null
            OcrTextBlock(
                id = index + 1,
                text = block.text.trim(),
                boundingBox = NormalizedBoundingBox(
                    left = (bounds.left / imageWidth).coerceIn(0f, 1f),
                    top = (bounds.top / imageHeight).coerceIn(0f, 1f),
                    right = (bounds.right / imageWidth).coerceIn(0f, 1f),
                    bottom = (bounds.bottom / imageHeight).coerceIn(0f, 1f),
                ),
                languageCodes = listOf(block.recognizedLanguage)
                    .filter { it.isNotBlank() && it != "und" },
            )
        }

        return OcrResult(
            fullText = result.text.trim(),
            blocks = blocks,
        )
    }

    override fun close() {
        recognizer.close()
    }
}
