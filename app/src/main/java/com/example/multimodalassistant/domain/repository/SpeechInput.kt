package com.example.multimodalassistant.domain.repository

import com.example.multimodalassistant.domain.model.SpeechRecognitionState
import kotlinx.coroutines.flow.StateFlow

interface SpeechInput : AutoCloseable {
    val state: StateFlow<SpeechRecognitionState>

    fun startListening()
    fun stopListening()
}
