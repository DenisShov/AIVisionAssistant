package com.example.multimodalassistant.domain.repository

import android.graphics.Bitmap
import com.example.multimodalassistant.domain.model.ClassificationResult
import com.example.multimodalassistant.domain.model.OcrResult

interface AssistantRepository : AutoCloseable {
    suspend fun analyze(
        query: String,
        image: Bitmap,
        classification: ClassificationResult,
        ocrResult: OcrResult?,
    ): String
}
