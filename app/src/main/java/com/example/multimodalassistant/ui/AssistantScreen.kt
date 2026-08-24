package com.example.multimodalassistant.ui

import android.graphics.Paint as AndroidPaint
import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.multimodalassistant.data.speech.SpeechState
import com.example.multimodalassistant.domain.model.DetectedLanguage
import com.example.multimodalassistant.domain.model.OcrResult
import kotlin.math.min

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssistantScreen(
    state: AssistantUiState,
    speechState: SpeechState,
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
        if (speechState is SpeechState.Success) {
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
                    classification = state.classification,
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

@Composable
private fun LanguageResultPanel(
    isIdentifying: Boolean,
    detectedLanguage: DetectedLanguage?,
    attempted: Boolean,
    error: String?,
) {
    when {
        isIdentifying -> {
            Row(
                modifier = Modifier.padding(top = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(8.dp))
                Text(
                    "Identifying language on device…",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }

        detectedLanguage != null -> {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.secondaryContainer,
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Default.Language, contentDescription = null)
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            "Detected language",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            "${detectedLanguage.displayName} " +
                                "(${(detectedLanguage.confidence * 100).toInt()}%) · On device",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
        }

        error != null -> {
            Text(
                "Language identification unavailable: $error",
                modifier = Modifier.padding(top = 8.dp),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }

        attempted -> {
            Text(
                "Language could not be identified confidently.",
                modifier = Modifier.padding(top = 8.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun InstructionSuggestions(
    suggestions: List<String>,
    enabled: Boolean,
    onSuggestionSelected: (String) -> Unit,
) {
    if (suggestions.isEmpty()) return

    Column(
        modifier = Modifier.padding(top = 10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            "Suggested for this image",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            suggestions.forEach { suggestion ->
                SuggestionChip(
                    onClick = { onSuggestionSelected(suggestion) },
                    enabled = enabled,
                    label = { Text(suggestion) },
                )
            }
        }
        Text(
            "Tap one or more suggestions, then edit the instructions above if needed.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun MarkdownText(markdown: String) {
    val lines = remember(markdown) { markdown.lines() }
    val primary = MaterialTheme.colorScheme.primary
    val codeBackground = MaterialTheme.colorScheme.surfaceContainerHighest

    SelectionContainer {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            var index = 0
            while (index < lines.size) {
                val line = lines[index]
                if (line.trimStart().startsWith("```")) {
                    val codeLines = mutableListOf<String>()
                    index++
                    while (index < lines.size && !lines[index].trimStart().startsWith("```")) {
                        codeLines += lines[index]
                        index++
                    }
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        color = codeBackground,
                    ) {
                        Text(
                            text = codeLines.joinToString("\n"),
                            modifier = Modifier.padding(12.dp),
                            fontFamily = FontFamily.Monospace,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                } else {
                    MarkdownLine(
                        line = line,
                        primaryColor = primary,
                        codeBackground = codeBackground,
                    )
                }
                index++
            }
        }
    }
}

@Composable
private fun MarkdownLine(
    line: String,
    primaryColor: androidx.compose.ui.graphics.Color,
    codeBackground: androidx.compose.ui.graphics.Color,
) {
    val trimmed = line.trimStart()
    val headingLevel = trimmed.takeWhile { it == '#' }.length
        .takeIf { it in 1..6 && trimmed.getOrNull(it) == ' ' }
    val bulletPrefix = when {
        trimmed.startsWith("- ") -> "- "
        trimmed.startsWith("* ") -> "* "
        trimmed.startsWith("+ ") -> "+ "
        else -> null
    }
    val orderedMatch = ORDERED_LIST_PATTERN.find(trimmed)

    when {
        line.isBlank() -> Spacer(Modifier.height(3.dp))

        headingLevel != null -> {
            val headingText = trimmed.drop(headingLevel + 1)
            Text(
                text = headingText.toMarkdownAnnotatedString(primaryColor, codeBackground),
                style = when (headingLevel) {
                    1 -> MaterialTheme.typography.headlineSmall
                    2 -> MaterialTheme.typography.titleLarge
                    else -> MaterialTheme.typography.titleMedium
                },
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 5.dp),
            )
        }

        bulletPrefix != null -> {
            MarkdownListItem(
                marker = "•",
                text = trimmed.removePrefix(bulletPrefix),
                primaryColor = primaryColor,
                codeBackground = codeBackground,
            )
        }

        orderedMatch != null -> {
            MarkdownListItem(
                marker = "${orderedMatch.groupValues[1]}.",
                text = trimmed.drop(orderedMatch.value.length),
                primaryColor = primaryColor,
                codeBackground = codeBackground,
            )
        }

        trimmed.startsWith("> ") -> {
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(color = primaryColor, fontWeight = FontWeight.Bold)) {
                        append("│ ")
                    }
                    appendMarkdown(trimmed.removePrefix("> "), primaryColor, codeBackground)
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyLarge,
            )
        }

        trimmed == "---" || trimmed == "***" || trimmed == "___" -> {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp),
                color = MaterialTheme.colorScheme.outlineVariant,
            ) {}
        }

        else -> {
            Text(
                text = line.toMarkdownAnnotatedString(primaryColor, codeBackground),
                style = MaterialTheme.typography.bodyLarge.withReadableLineHeight(),
            )
        }
    }
}

@Composable
private fun MarkdownListItem(
    marker: String,
    text: String,
    primaryColor: androidx.compose.ui.graphics.Color,
    codeBackground: androidx.compose.ui.graphics.Color,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            marker,
            modifier = Modifier.width(28.dp),
            color = primaryColor,
            fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.bodyLarge,
        )
        Text(
            text = text.toMarkdownAnnotatedString(primaryColor, codeBackground),
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge.withReadableLineHeight(),
        )
    }
}

internal fun String.toMarkdownAnnotatedString(
    primaryColor: androidx.compose.ui.graphics.Color,
    codeBackground: androidx.compose.ui.graphics.Color,
): AnnotatedString = buildAnnotatedString {
    appendMarkdown(this@toMarkdownAnnotatedString, primaryColor, codeBackground)
}

private fun AnnotatedString.Builder.appendMarkdown(
    source: String,
    primaryColor: androidx.compose.ui.graphics.Color,
    codeBackground: androidx.compose.ui.graphics.Color,
) {
    var index = 0
    while (index < source.length) {
        when {
            source[index] == '\\' && index + 1 < source.length -> {
                append(source[index + 1])
                index += 2
            }

            source.startsWith("***", index) || source.startsWith("___", index) -> {
                val delimiter = source.substring(index, index + 3)
                val end = source.indexOf(delimiter, index + 3)
                if (end < 0) {
                    append(delimiter)
                    index += 3
                } else {
                    withStyle(
                        SpanStyle(
                            fontWeight = FontWeight.Bold,
                            fontStyle = FontStyle.Italic,
                        ),
                    ) {
                        appendMarkdown(source.substring(index + 3, end), primaryColor, codeBackground)
                    }
                    index = end + 3
                }
            }

            source.startsWith("**", index) || source.startsWith("__", index) -> {
                val delimiter = source.substring(index, index + 2)
                val end = source.indexOf(delimiter, index + 2)
                if (end < 0) {
                    append(delimiter)
                    index += 2
                } else {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        appendMarkdown(source.substring(index + 2, end), primaryColor, codeBackground)
                    }
                    index = end + 2
                }
            }

            source.startsWith("~~", index) -> {
                val end = source.indexOf("~~", index + 2)
                if (end < 0) {
                    append("~~")
                    index += 2
                } else {
                    withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) {
                        appendMarkdown(source.substring(index + 2, end), primaryColor, codeBackground)
                    }
                    index = end + 2
                }
            }

            source[index] == '`' -> {
                val end = source.indexOf('`', index + 1)
                if (end < 0) {
                    append('`')
                    index++
                } else {
                    withStyle(
                        SpanStyle(
                            fontFamily = FontFamily.Monospace,
                            background = codeBackground,
                        ),
                    ) {
                        append(source.substring(index + 1, end))
                    }
                    index = end + 1
                }
            }

            source[index] == '[' -> {
                val labelEnd = source.indexOf("](", index + 1)
                val urlEnd = if (labelEnd >= 0) source.indexOf(')', labelEnd + 2) else -1
                if (labelEnd < 0 || urlEnd < 0) {
                    append('[')
                    index++
                } else {
                    withStyle(
                        SpanStyle(
                            color = primaryColor,
                            textDecoration = TextDecoration.Underline,
                        ),
                    ) {
                        append(source.substring(index + 1, labelEnd))
                    }
                    index = urlEnd + 1
                }
            }

            source[index] == '*' || source[index] == '_' -> {
                val delimiter = source[index]
                val end = source.indexOf(delimiter, index + 1)
                if (end <= index + 1) {
                    append(delimiter)
                    index++
                } else {
                    withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                        appendMarkdown(source.substring(index + 1, end), primaryColor, codeBackground)
                    }
                    index = end + 1
                }
            }

            else -> {
                append(source[index])
                index++
            }
        }
    }
}

private fun TextStyle.withReadableLineHeight(): TextStyle = copy(lineHeight = 24.sp)

private val ORDERED_LIST_PATTERN = Regex("^(\\d+)[.)]\\s+")

@Composable
private fun LiteRtResultPanel(
    isClassifying: Boolean,
    classification: com.example.multimodalassistant.domain.model.ClassificationResult?,
    error: String?,
) {
    when {
        isClassifying -> {
            Row(
                modifier = Modifier.padding(top = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(8.dp))
                Text(
                    "Classifying image locally with LiteRT…",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }

        classification != null -> {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            "LiteRT on-device result",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(classification.summary, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }

        error != null -> {
            Text(
                "LiteRT unavailable: $error",
                modifier = Modifier.padding(top = 10.dp),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
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
    onToggleOcrBoxes: () -> Unit,
) {
    var showFullScreen by remember(bitmap) { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .clickable(enabled = bitmap != null) { showFullScreen = true },
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
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp),
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.62f),
                ) {
                    Icon(
                        Icons.Default.Fullscreen,
                        contentDescription = "Open image full screen",
                        modifier = Modifier.padding(8.dp),
                        tint = Color.White,
                    )
                }
            }
        }
    }

    if (showFullScreen && bitmap != null) {
        FullScreenImageViewer(
            bitmap = bitmap,
            ocrResult = ocrResult,
            showOcrBoxes = showOcrBoxes,
            onToggleOcrBoxes = onToggleOcrBoxes,
            onDismiss = { showFullScreen = false },
        )
    }
}

@Composable
private fun FullScreenImageViewer(
    bitmap: Bitmap,
    ocrResult: OcrResult?,
    showOcrBoxes: Boolean,
    onToggleOcrBoxes: () -> Unit,
    onDismiss: () -> Unit,
) {
    var scale by remember(bitmap) { mutableFloatStateOf(1f) }
    var offset by remember(bitmap) { mutableStateOf(Offset.Zero) }
    var viewportSize by remember { mutableStateOf(IntSize.Zero) }

    val updateTransform: (Offset, Offset, Float) -> Unit = { centroid, panChange, zoomChange ->
        val previousScale = scale
        val newScale = (scale * zoomChange).coerceIn(MIN_IMAGE_SCALE, MAX_IMAGE_SCALE)
        val zoomRatio = newScale / previousScale
        val viewportCenter = Offset(
            x = viewportSize.width / 2f,
            y = viewportSize.height / 2f,
        )
        val focalPoint = centroid - viewportCenter
        val zoomedOffset = Offset(
            x = offset.x * zoomRatio + focalPoint.x * (1f - zoomRatio),
            y = offset.y * zoomRatio + focalPoint.y * (1f - zoomRatio),
        )
        val maxOffsetX = viewportSize.width * (newScale - 1f) / 2f
        val maxOffsetY = viewportSize.height * (newScale - 1f) / 2f
        scale = newScale
        offset = Offset(
            x = (zoomedOffset.x + panChange.x).coerceIn(-maxOffsetX, maxOffsetX),
            y = (zoomedOffset.y + panChange.y).coerceIn(-maxOffsetY, maxOffsetY),
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.Black,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clipToBounds(),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .onSizeChanged { viewportSize = it }
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            translationX = offset.x
                            translationY = offset.y
                        }
                        .pointerInput(bitmap) {
                            detectTransformGestures { centroid, pan, zoom, _ ->
                                updateTransform(centroid, pan, zoom)
                            }
                        },
                ) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Full-screen scanned document",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize(),
                    )
                    if (showOcrBoxes && ocrResult != null) {
                        OcrBoundingBoxOverlay(bitmap, ocrResult)
                    }
                }

                Surface(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .statusBarsPadding(),
                    color = Color.Black.copy(alpha = 0.68f),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 6.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(onClick = onDismiss) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Close full-screen image",
                                tint = Color.White,
                            )
                        }
                        Text(
                            "Image preview",
                            modifier = Modifier.weight(1f),
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        if (ocrResult?.hasText == true) {
                            TextButton(onClick = onToggleOcrBoxes) {
                                Text(
                                    if (showOcrBoxes) "Hide OCR" else "Show OCR",
                                    color = Color.White,
                                )
                            }
                        }
                        IconButton(
                            onClick = {
                                scale = MIN_IMAGE_SCALE
                                offset = Offset.Zero
                            },
                            enabled = scale > MIN_IMAGE_SCALE || offset != Offset.Zero,
                        ) {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = "Reset zoom",
                                tint = if (scale > MIN_IMAGE_SCALE || offset != Offset.Zero) {
                                    Color.White
                                } else {
                                    Color.Gray
                                },
                            )
                        }
                    }
                }

                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(bottom = 20.dp),
                    shape = RoundedCornerShape(50),
                    color = Color.Black.copy(alpha = 0.68f),
                ) {
                    Text(
                        "Pinch to zoom · drag to move",
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        color = Color.White,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}

private const val MIN_IMAGE_SCALE = 1f
private const val MAX_IMAGE_SCALE = 6f

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
