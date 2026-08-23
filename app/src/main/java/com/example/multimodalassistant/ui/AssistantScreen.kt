package com.example.multimodalassistant.ui

import android.graphics.Paint as AndroidPaint
import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.multimodalassistant.data.speech.SpeechState
import com.example.multimodalassistant.domain.model.OcrResult
import kotlin.math.min

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssistantScreen(
    state: AssistantUiState,
    speechState: SpeechState,
    onPromptChanged: (String) -> Unit,
    onScan: () -> Unit,
    onStartSpeech: () -> Unit,
    onStopSpeech: () -> Unit,
    onToggleOcrBoxes: () -> Unit,
    onProcess: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(speechState) {
        if (speechState is SpeechState.Success) {
            onPromptChanged(speechState.text)
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
                .verticalScroll(rememberScrollState())
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
                )
                OcrResultPanel(
                    isRecognizing = state.isRecognizingText,
                    result = state.ocrResult,
                    error = state.ocrError,
                    showBoxes = state.showOcrBoxes,
                    onToggleBoxes = onToggleOcrBoxes,
                )
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = onScan,
                    enabled = !state.isScanning && state.processingStage == null,
                    modifier = Modifier.fillMaxWidth(),
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
                    Text(if (state.isScanning) "Opening scanner…" else "Scan document")
                }
            }

            InputCard(
                number = "2",
                title = "Your question",
                subtitle = "Speak naturally or edit the recognized text.",
            ) {
                OutlinedTextField(
                    value = state.prompt,
                    onValueChange = onPromptChanged,
                    label = { Text("Ask about the image") },
                    placeholder = { Text("Summarize this and translate it to Polish") },
                    minLines = 3,
                    maxLines = 6,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(12.dp))
                val isListening = speechState is SpeechState.Listening
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
                if (speechState is SpeechState.Error) {
                    Text(
                        speechState.message,
                        modifier = Modifier.padding(top = 8.dp),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }

            Button(
                onClick = onProcess,
                enabled = state.canSubmit,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Process multimodal query")
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

            state.classification?.let { classification ->
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "LiteRT on-device result",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                        Text(classification.summary)

                        state.response?.let { response ->
                            HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                            Text(
                                "Gemini 3.6 Flash",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(response, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun InputCard(
    number: String,
    title: String,
    subtitle: String,
    content: @Composable () -> Unit,
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                ) {
                    Text(
                        number,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(title, style = MaterialTheme.typography.titleMedium)
                    Text(
                        subtitle,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            content()
        }
    }
}

@Composable
private fun ImagePreview(
    bitmap: Bitmap?,
    ocrResult: OcrResult?,
    showOcrBoxes: Boolean,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
    ) {
        if (bitmap == null) {
            Box(contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Image,
                        contentDescription = null,
                        modifier = Modifier.size(42.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "No image selected",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(14.dp)),
            ) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Scanned document",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize(),
                )
                if (showOcrBoxes && ocrResult != null) {
                    OcrBoundingBoxOverlay(bitmap, ocrResult)
                }
            }
        }
    }
}

@Composable
private fun OcrBoundingBoxOverlay(bitmap: Bitmap, result: OcrResult) {
    val boxColor = MaterialTheme.colorScheme.primary
    val labelColor = MaterialTheme.colorScheme.onPrimary
    val textSize = with(LocalDensity.current) { 10.sp.toPx() }
    val textPaint = remember(boxColor, labelColor, textSize) {
        AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
            color = labelColor.toArgb()
            this.textSize = textSize
        }
    }
    val labelPaint = remember(boxColor) {
        AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
            color = boxColor.toArgb()
        }
    }

    Canvas(Modifier.fillMaxSize()) {
        val imageScale = min(size.width / bitmap.width, size.height / bitmap.height)
        val displayedWidth = bitmap.width * imageScale
        val displayedHeight = bitmap.height * imageScale
        val horizontalInset = (size.width - displayedWidth) / 2f
        val verticalInset = (size.height - displayedHeight) / 2f
        val strokeWidth = 2.dp.toPx()
        val labelPadding = 3.dp.toPx()

        result.blocks.forEach { block ->
            val bounds = block.boundingBox
            val left = horizontalInset + bounds.left * displayedWidth
            val top = verticalInset + bounds.top * displayedHeight
            val width = bounds.width * displayedWidth
            val height = bounds.height * displayedHeight
            if (width <= 0f || height <= 0f) return@forEach

            drawRect(
                color = boxColor.copy(alpha = 0.12f),
                topLeft = androidx.compose.ui.geometry.Offset(left, top),
                size = androidx.compose.ui.geometry.Size(width, height),
            )
            drawRect(
                color = boxColor,
                topLeft = androidx.compose.ui.geometry.Offset(left, top),
                size = androidx.compose.ui.geometry.Size(width, height),
                style = Stroke(width = strokeWidth),
            )

            drawIntoCanvas { canvas ->
                val label = block.id.toString()
                val metrics = textPaint.fontMetrics
                val labelWidth = textPaint.measureText(label) + labelPadding * 2
                val labelHeight = metrics.bottom - metrics.top + labelPadding * 2
                val labelTop = (top - labelHeight).coerceAtLeast(verticalInset)
                canvas.nativeCanvas.drawRect(
                    left,
                    labelTop,
                    left + labelWidth,
                    labelTop + labelHeight,
                    labelPaint,
                )
                canvas.nativeCanvas.drawText(
                    label,
                    left + labelPadding,
                    labelTop + labelPadding - metrics.top,
                    textPaint,
                )
            }
        }
    }
}

@Composable
private fun OcrResultPanel(
    isRecognizing: Boolean,
    result: OcrResult?,
    error: String?,
    showBoxes: Boolean,
    onToggleBoxes: () -> Unit,
) {
    when {
        isRecognizing -> {
            Row(
                modifier = Modifier.padding(top = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(8.dp))
                Text("Recognizing text on device…", style = MaterialTheme.typography.bodyMedium)
            }
        }

        error != null -> {
            Text(
                text = "OCR unavailable: $error",
                modifier = Modifier.padding(top = 10.dp),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }

        result != null && result.hasText -> {
            var expanded by rememberSaveable(result.fullText) { mutableStateOf(false) }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "OCR: ${result.blocks.size} text blocks",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                TextButton(onClick = onToggleBoxes) {
                    Text(if (showBoxes) "Hide boxes" else "Show boxes")
                }
            }
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
            ) {
                SelectionContainer {
                    Text(
                        text = result.fullText,
                        modifier = Modifier.padding(12.dp),
                        maxLines = if (expanded) Int.MAX_VALUE else 6,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            if (result.fullText.count { it == '\n' } >= 6 || result.fullText.length > 500) {
                TextButton(onClick = { expanded = !expanded }) {
                    Text(if (expanded) "Show less" else "Show all recognized text")
                }
            }
        }

        result != null -> {
            Text(
                "No readable text detected.",
                modifier = Modifier.padding(top = 10.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun StatusSurface(text: String, isError: Boolean) {
    val container = if (isError) {
        MaterialTheme.colorScheme.errorContainer
    } else {
        MaterialTheme.colorScheme.secondaryContainer
    }
    val content = if (isError) {
        MaterialTheme.colorScheme.onErrorContainer
    } else {
        MaterialTheme.colorScheme.onSecondaryContainer
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = container,
    ) {
        Text(
            text,
            modifier = Modifier.padding(14.dp),
            color = content,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}
