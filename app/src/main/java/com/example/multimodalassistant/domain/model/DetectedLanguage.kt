package com.example.multimodalassistant.domain.model

data class DetectedLanguage(
    val languageTag: String,
    val displayName: String,
    val confidence: Float,
) {
    val isEnglish: Boolean
        get() = languageTag.substringBefore('-').equals("en", ignoreCase = true)
}
