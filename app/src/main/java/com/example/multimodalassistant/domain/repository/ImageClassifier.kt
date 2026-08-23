package com.example.multimodalassistant.domain.repository

import android.graphics.Bitmap
import com.example.multimodalassistant.domain.model.ClassificationResult

interface ImageClassifier : AutoCloseable {
    fun classify(bitmap: Bitmap): ClassificationResult
}
