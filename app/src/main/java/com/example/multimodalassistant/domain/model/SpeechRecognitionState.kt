package com.example.multimodalassistant.domain.model

sealed interface SpeechRecognitionState {
    data object Idle : SpeechRecognitionState
    data object Listening : SpeechRecognitionState
    data class Success(val text: String) : SpeechRecognitionState
    data class Error(val message: String) : SpeechRecognitionState
}
