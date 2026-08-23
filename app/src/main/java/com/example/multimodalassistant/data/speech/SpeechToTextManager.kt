package com.example.multimodalassistant.data.speech

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

sealed interface SpeechState {
    data object Idle : SpeechState
    data object Listening : SpeechState
    data class Success(val text: String) : SpeechState
    data class Error(val message: String) : SpeechState
}

class SpeechToTextManager(context: Context) : AutoCloseable {
    private val appContext = context.applicationContext
    private val recognizer = SpeechRecognizer.createSpeechRecognizer(appContext)
    private val _state = MutableStateFlow<SpeechState>(SpeechState.Idle)

    val state: StateFlow<SpeechState> = _state.asStateFlow()

    init {
        recognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                _state.value = SpeechState.Listening
            }

            override fun onBeginningOfSpeech() = Unit
            override fun onRmsChanged(rmsdB: Float) = Unit
            override fun onBufferReceived(buffer: ByteArray?) = Unit
            override fun onEndOfSpeech() = Unit

            override fun onError(error: Int) {
                _state.value = SpeechState.Error(error.toMessage())
            }

            override fun onResults(results: Bundle?) {
                val text = results
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                _state.value = if (text.isNullOrBlank()) {
                    SpeechState.Error("No speech was recognized. Please try again.")
                } else {
                    SpeechState.Success(text)
                }
            }

            override fun onPartialResults(partialResults: Bundle?) = Unit
            override fun onEvent(eventType: Int, params: Bundle?) = Unit
        })
    }

    fun startListening() {
        if (!SpeechRecognizer.isRecognitionAvailable(appContext)) {
            _state.value = SpeechState.Error("Speech recognition is not available on this device.")
            return
        }

        _state.value = SpeechState.Listening
        recognizer.startListening(
            Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM,
                )
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            },
        )
    }

    fun stopListening() {
        recognizer.stopListening()
    }

    fun reset() {
        _state.value = SpeechState.Idle
    }

    override fun close() {
        recognizer.destroy()
    }
}

private fun Int.toMessage(): String = when (this) {
    SpeechRecognizer.ERROR_AUDIO -> "The microphone could not capture audio."
    SpeechRecognizer.ERROR_CLIENT -> "Speech recognition was cancelled."
    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission is required."
    SpeechRecognizer.ERROR_NETWORK,
    SpeechRecognizer.ERROR_NETWORK_TIMEOUT,
    -> "Speech recognition could not reach the network service."
    SpeechRecognizer.ERROR_NO_MATCH -> "No matching speech was recognized."
    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Speech recognition is busy. Try again shortly."
    SpeechRecognizer.ERROR_SERVER -> "The speech recognition service returned an error."
    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech was heard."
    else -> "Speech recognition failed (error $this)."
}
