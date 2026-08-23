package com.example.multimodalassistant.domain.repository

import android.graphics.Bitmap
import com.example.multimodalassistant.domain.model.OcrResult

interface OcrRepository : AutoCloseable {
    suspend fun recognize(bitmap: Bitmap): OcrResult
}
