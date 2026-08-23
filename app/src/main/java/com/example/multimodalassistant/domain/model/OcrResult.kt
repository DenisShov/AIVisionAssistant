package com.example.multimodalassistant.domain.model

data class NormalizedBoundingBox(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
) {
    val width: Float get() = (right - left).coerceAtLeast(0f)
    val height: Float get() = (bottom - top).coerceAtLeast(0f)
}

data class OcrTextBlock(
    val id: Int,
    val text: String,
    val boundingBox: NormalizedBoundingBox,
    val languageCodes: List<String>,
)

data class OcrResult(
    val fullText: String,
    val blocks: List<OcrTextBlock>,
) {
    val hasText: Boolean get() = fullText.isNotBlank()

    companion object {
        val Empty = OcrResult(fullText = "", blocks = emptyList())
    }
}
