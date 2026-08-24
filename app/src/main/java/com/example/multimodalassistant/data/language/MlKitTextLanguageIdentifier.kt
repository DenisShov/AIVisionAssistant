package com.example.multimodalassistant.data.language

import com.example.multimodalassistant.domain.model.DetectedLanguage
import com.example.multimodalassistant.domain.repository.TextLanguageIdentifier
import com.google.mlkit.nl.languageid.LanguageIdentification
import kotlinx.coroutines.tasks.await
import java.util.Locale
import javax.inject.Inject

class MlKitTextLanguageIdentifier @Inject constructor() : TextLanguageIdentifier {
    private val identifier = LanguageIdentification.getClient()

    override suspend fun identify(text: String): DetectedLanguage? {
        if (text.count(Char::isLetter) < MIN_LETTERS) return null

        val candidate = identifier
            .identifyPossibleLanguages(text.take(MAX_INPUT_CHARACTERS))
            .await()
            .asSequence()
            .filterNot { it.languageTag == UNDETERMINED_LANGUAGE_TAG }
            .maxByOrNull { it.confidence }
            ?.takeIf { it.confidence >= MIN_CONFIDENCE }
            ?: return null

        val displayName = Locale.forLanguageTag(candidate.languageTag)
            .getDisplayLanguage(Locale.ENGLISH)
            .takeIf(String::isNotBlank)
            ?.replaceFirstChar { it.titlecase() }
            ?: candidate.languageTag

        return DetectedLanguage(
            languageTag = candidate.languageTag,
            displayName = displayName,
            confidence = candidate.confidence.coerceIn(0f, 1f),
        )
    }

    override fun close() {
        identifier.close()
    }

    private companion object {
        const val UNDETERMINED_LANGUAGE_TAG = "und"
        const val MIN_LETTERS = 12
        const val MIN_CONFIDENCE = 0.5f
        const val MAX_INPUT_CHARACTERS = 10_000
    }
}
