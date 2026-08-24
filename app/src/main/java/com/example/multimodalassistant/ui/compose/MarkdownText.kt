package com.example.multimodalassistant.ui.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import androidx.compose.ui.unit.sp

@Composable
internal fun MarkdownText(markdown: String) {
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
    primaryColor: Color,
    codeBackground: Color,
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
    primaryColor: Color,
    codeBackground: Color,
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
    primaryColor: Color,
    codeBackground: Color,
): AnnotatedString = buildAnnotatedString {
    appendMarkdown(this@toMarkdownAnnotatedString, primaryColor, codeBackground)
}

private fun AnnotatedString.Builder.appendMarkdown(
    source: String,
    primaryColor: Color,
    codeBackground: Color,
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

