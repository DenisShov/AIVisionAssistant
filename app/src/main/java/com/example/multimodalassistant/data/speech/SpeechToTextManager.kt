package com.example.multimodalassistant.data.speech

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import com.example.multimodalassistant.domain.model.SpeechRecognitionState
import com.example.multimodalassistant.domain.repository.SpeechInput
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class SpeechToTextManager @Inject constructor(
    @ApplicationContext context: Context,
) : SpeechInput {
    private val appContext = context.applicationContext
    private val recognizer = SpeechRecognizer.createSpeechRecognizer(appContext)
    private val _state = MutableStateFlow<SpeechRecognitionState>(SpeechRecognitionState.Idle)
    private var isClosed = false

    override val state: StateFlow<SpeechRecognitionState> = _state.asStateFlow()

    init {
        recognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                _state.value = SpeechRecognitionState.Listening
            }

            override fun onBeginningOfSpeech() = Unit
            override fun onRmsChanged(rmsdB: Float) = Unit
            override fun onBufferReceived(buffer: ByteArray?) = Unit
            override fun onEndOfSpeech() = Unit

            override fun onError(error: Int) {
                _state.value = SpeechRecognitionState.Error(error.toMessage())
            }

            override fun onResults(results: Bundle?) {
                val text = results
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                _state.value = if (text.isNullOrBlank()) {
                    SpeechRecognitionState.Error("No speech was recognized. Please try again.")
                } else {
                    SpeechRecognitionState.Success(text)
                }
            }

            override fun onPartialResults(partialResults: Bundle?) = Unit
            override fun onEvent(eventType: Int, params: Bundle?) = Unit
        })
    }

    override fun startListening() {
        if (isClosed) return
        if (!SpeechRecognizer.isRecognitionAvailable(appContext)) {
            _state.value = SpeechRecognitionState.Error(
                "Speech recognition is not available on this device.",
            )
            return
        }

        _state.value = SpeechRecognitionState.Listening
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

    override fun stopListening() {
        if (isClosed) return
        recognizer.stopListening()
    }

    override fun close() {
        if (isClosed) return
        isClosed = true
        recognizer.setRecognitionListener(null)
        recognizer.cancel()
        recognizer.destroy()
        _state.value = SpeechRecognitionState.Idle
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
