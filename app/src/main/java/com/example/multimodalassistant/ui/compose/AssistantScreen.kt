package com.example.multimodalassistant.ui.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.multimodalassistant.domain.model.SpeechRecognitionState
import com.example.multimodalassistant.ui.state.AssistantUiState
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssistantScreen(
    state: AssistantUiState,
    speechState: SpeechRecognitionState,
    onScan: () -> Unit,
    onPickImage: () -> Unit,
    onStartSpeech: () -> Unit,
    onStopSpeech: () -> Unit,
    onToggleOcrBoxes: () -> Unit,
    onProcess: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var promptText by rememberSaveable { mutableStateOf("") }
    val screenScrollState = rememberScrollState()

    LaunchedEffect(speechState) {
        if (speechState is SpeechRecognitionState.Success) {
            promptText = speechState.text
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("AI Vision Assistant")
                        Text(
                            "Local vision · cloud reasoning",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(screenScrollState)
                .imePadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (!state.firebaseConfigured) {
                StatusSurface(
                    text = "Firebase AI Logic is not configured. Add app/google-services.json and rebuild.",
                    isError = true,
                )
            }

            InputCard(
                number = "1",
                title = "Document or image",
                subtitle = "Scan with ML Kit or import one page from the gallery.",
            ) {
                ImagePreview(
                    bitmap = state.image,
                    ocrResult = state.ocrResult,
                    showOcrBoxes = state.showOcrBoxes,
                    onToggleOcrBoxes = onToggleOcrBoxes,
                )
                LiteRtResultPanel(
                    isClassifying = state.isClassifying,
                    classification = state.classificationResult,
                    error = state.classificationError,
                )
                OcrResultPanel(
                    isRecognizing = state.isRecognizingText,
                    result = state.ocrResult,
                    error = state.ocrError,
                    showBoxes = state.showOcrBoxes,
                    onToggleBoxes = onToggleOcrBoxes,
                )
                LanguageResultPanel(
                    isIdentifying = state.isIdentifyingLanguage,
                    detectedLanguage = state.detectedLanguage,
                    attempted = state.languageIdentificationAttempted,
                    error = state.languageIdentificationError,
                )
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Button(
                        onClick = onScan,
                        enabled = !state.isScanning &&
                            !state.isImporting &&
                            state.processingStage == null,
                        modifier = Modifier.weight(1f),
                    ) {
                        if (state.isScanning) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Icon(Icons.Default.DocumentScanner, contentDescription = null)
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(if (state.isScanning) "Opening…" else "Scan")
                    }
                    OutlinedButton(
                        onClick = onPickImage,
                        enabled = !state.isScanning &&
                            !state.isImporting &&
                            state.processingStage == null,
                        modifier = Modifier.weight(1f),
                    ) {
                        if (state.isImporting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(if (state.isImporting) "Opening…" else "Gallery")
                    }
                }
            }

            InputCard(
                number = "2",
                title = "Your instructions",
                subtitle = "Choose suggestions, type your own instructions, or use speech.",
            ) {
                OutlinedTextField(
                    value = promptText,
                    onValueChange = { promptText = it },
                    label = { Text("What should Gemini do?") },
                    placeholder = { Text("For example: summarize this and translate it to English") },
                    minLines = 3,
                    maxLines = 6,
                    modifier = Modifier.fillMaxWidth(),
                )
                InstructionSuggestions(
                    suggestions = state.suggestedInstructions,
                    enabled = state.processingStage == null,
                    onSuggestionSelected = { suggestion ->
                        promptText = when {
                            promptText.isBlank() -> suggestion
                            promptText.lineSequence().any { it.trim() == suggestion } -> promptText
                            else -> "${promptText.trimEnd()}\n$suggestion"
                        }
                    },
                )
                Spacer(Modifier.height(12.dp))
                val isListening = speechState is SpeechRecognitionState.Listening
                OutlinedButton(
                    onClick = if (isListening) onStopSpeech else onStartSpeech,
                    enabled = state.processingStage == null,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(
                        if (isListening) Icons.Default.Stop else Icons.Default.Mic,
                        contentDescription = null,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(if (isListening) "Stop listening" else "Record speech")
                }
                if (speechState is SpeechRecognitionState.Error) {
                    Text(
                        speechState.message,
                        modifier = Modifier.padding(top = 8.dp),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }

            Button(
                onClick = { onProcess(promptText) },
                enabled = state.canSubmit(promptText),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Send instructions to Gemini")
            }

            state.processingStage?.let { stage ->
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        Text(stage.message, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            state.error?.let { StatusSurface(text = it, isError = true) }

            state.response?.let { response ->
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            "Gemini 3.6 Flash",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        MarkdownText(response)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}
