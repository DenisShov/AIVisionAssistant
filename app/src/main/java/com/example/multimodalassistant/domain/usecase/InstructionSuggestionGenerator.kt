package com.example.multimodalassistant.domain.usecase

import com.example.multimodalassistant.domain.model.ClassificationResult
import com.example.multimodalassistant.domain.model.DetectedLanguage
import com.example.multimodalassistant.domain.model.OcrResult

object InstructionSuggestionGenerator {
    fun generate(
        ocrResult: OcrResult?,
        classification: ClassificationResult?,
        detectedLanguage: DetectedLanguage? = null,
    ): List<String> {
        val text = ocrResult?.fullText.orEmpty().trim()
        if (text.isBlank()) return imageSuggestions(classification)

        val normalized = text.lowercase()
        return buildList {
            val looksLikeInvoice = INVOICE_TERMS.any(normalized::contains)
            if (looksLikeInvoice) {
                add("Extract the invoice number, dates, parties, line items, and totals")
            } else if (RECEIPT_TERMS.any(normalized::contains)) {
                add("Extract purchased items, prices, tax, payment method, and total")
            }
            if (CONTACT_PATTERN.containsMatchIn(text)) {
                add("Extract all contact details")
            }
            if (DATE_PATTERN.containsMatchIn(text) || DATE_TERMS.any(normalized::contains)) {
                add("List all dates, deadlines, and related actions")
            }
            if (looksLikeTable(text)) {
                add("Convert the visible table into a clean Markdown table")
            }
            add("Summarize this document")
            if (detectedLanguage != null && !detectedLanguage.isEnglish) {
                add("Translate the ${detectedLanguage.displayName} text to English")
            } else if (detectedLanguage == null) {
                add("Translate the visible text to English")
            }
            add("Extract the key facts as a bullet list")
            if (text.length > LONG_DOCUMENT_CHARACTERS) {
                add("Create a short executive summary with action items")
            } else {
                add("Explain this document in simple language")
            }
        }.distinct().take(MAX_SUGGESTIONS)
    }

    private fun imageSuggestions(classification: ClassificationResult?): List<String> = buildList {
        add("Describe this image in detail")
        add("Identify the main objects and explain their purpose")
        classification
            ?.takeIf { it.confidence >= MIN_USEFUL_CONFIDENCE }
            ?.label
            ?.takeIf { it.isNotBlank() }
            ?.let { label -> add("Tell me about the $label visible in this image") }
        add("Point out anything important or unusual")
        add("Suggest practical next steps based on this image")
    }.distinct().take(MAX_SUGGESTIONS)

    private fun looksLikeTable(text: String): Boolean = text
        .lineSequence()
        .count { line -> NUMBER_PATTERN.findAll(line).count() >= 2 } >= 2

    private val INVOICE_TERMS = listOf("invoice", "faktura", "due date", "amount due")
    private val RECEIPT_TERMS = listOf("receipt", "subtotal", "total", "vat", "tax")
    private val DATE_TERMS = listOf("deadline", "due date", "expires", "valid until")
    private val CONTACT_PATTERN = Regex(
        "(?:[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}|(?:\\+?\\d[\\d ()-]{7,}\\d))",
        RegexOption.IGNORE_CASE,
    )
    private val DATE_PATTERN = Regex(
        "\\b(?:\\d{1,2}[./-]\\d{1,2}[./-]\\d{2,4}|\\d{4}-\\d{2}-\\d{2})\\b",
    )
    private val NUMBER_PATTERN = Regex("\\b\\d+(?:[.,]\\d+)?\\b")
    private const val LONG_DOCUMENT_CHARACTERS = 1_200
    private const val MIN_USEFUL_CONFIDENCE = 0.35f
    private const val MAX_SUGGESTIONS = 6
}
