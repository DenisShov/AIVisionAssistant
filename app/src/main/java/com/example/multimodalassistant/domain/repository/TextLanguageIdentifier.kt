package com.example.multimodalassistant.domain.repository

import com.example.multimodalassistant.domain.model.DetectedLanguage

interface TextLanguageIdentifier : AutoCloseable {
    suspend fun identify(text: String): DetectedLanguage?
}
