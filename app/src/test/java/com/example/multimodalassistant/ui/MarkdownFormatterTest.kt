package com.example.multimodalassistant.ui

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MarkdownFormatterTest {
    @Test
    fun removesFormattingMarkersAndKeepsReadableText() {
        val formatted = "**Bold**, *italic*, `code`, and [a link](https://example.com)"
            .toMarkdownAnnotatedString(Color.Blue, Color.LightGray)

        assertEquals("Bold, italic, code, and a link", formatted.text)
        assertFalse(formatted.text.contains('*'))
        assertFalse(formatted.text.contains('`'))
        assertTrue(formatted.spanStyles.isNotEmpty())
    }

    @Test
    fun supportsCombinedBoldItalicFormatting() {
        val formatted = "***Important***".toMarkdownAnnotatedString(Color.Blue, Color.LightGray)

        assertEquals("Important", formatted.text)
        assertTrue(formatted.spanStyles.isNotEmpty())
    }
}
