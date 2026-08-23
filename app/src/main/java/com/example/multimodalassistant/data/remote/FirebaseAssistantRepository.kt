package com.example.multimodalassistant.data.remote

import android.graphics.Bitmap
import androidx.core.graphics.scale
import com.example.multimodalassistant.domain.model.ClassificationResult
import com.example.multimodalassistant.domain.model.OcrResult
import com.example.multimodalassistant.domain.repository.AssistantRepository
import com.example.multimodalassistant.domain.usecase.PromptComposer
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.content

class FirebaseAssistantRepository : AssistantRepository {
    private val model by lazy {
        Firebase.ai(backend = GenerativeBackend.googleAI())
            .generativeModel(MODEL_NAME)
    }

    override suspend fun analyze(
        query: String,
        image: Bitmap,
        classification: ClassificationResult,
        ocrResult: OcrResult?,
    ): String {
        val uploadBitmap = image.forUpload()
        return try {
            val prompt = content {
                image(uploadBitmap)
                text(PromptComposer.compose(query, classification, ocrResult))
            }
            model.generateContent(prompt).text?.takeIf(String::isNotBlank)
                ?: error("Gemini returned no text response.")
        } finally {
            if (uploadBitmap !== image) uploadBitmap.recycle()
        }
    }

    override fun close() = Unit

    private fun Bitmap.forUpload(): Bitmap {
        val maxSide = maxOf(width, height)
        if (maxSide <= MAX_IMAGE_SIDE) return this
        val scaleFactor = MAX_IMAGE_SIDE.toFloat() / maxSide
        return scale(
            (width * scaleFactor).toInt().coerceAtLeast(1),
            (height * scaleFactor).toInt().coerceAtLeast(1),
        )
    }

    private companion object {
        // Gemini 2.5 Flash is scheduled to shut down in October 2026.
        const val MODEL_NAME = "gemini-3.6-flash"
        const val MAX_IMAGE_SIDE = 2_048
    }
}
