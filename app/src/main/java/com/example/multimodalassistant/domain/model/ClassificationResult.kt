package com.example.multimodalassistant.domain.model

data class ClassificationResult(
    val label: String,
    val confidence: Float,
    val accelerator: String,
) {
    val summary: String
        get() = "$label (${(confidence * 100).toInt()}% confidence, $accelerator)"
}
