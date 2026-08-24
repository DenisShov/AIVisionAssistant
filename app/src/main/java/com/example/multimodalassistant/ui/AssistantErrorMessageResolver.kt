package com.example.multimodalassistant.ui

import com.example.multimodalassistant.domain.model.AssistantConfiguration
import javax.inject.Inject

class AssistantErrorMessageResolver @Inject constructor(
    private val configuration: AssistantConfiguration,
) {
    fun resolve(error: Throwable): String {
        val messages = generateSequence(error) { it.cause }
            .mapNotNull { it.localizedMessage }
            .toList()
        val hasInvalidAppCheckToken = messages.any { message ->
            message.contains("App Check token is invalid", ignoreCase = true) ||
                message.contains("App attestation failed", ignoreCase = true)
        }

        if (!hasInvalidAppCheckToken) {
            return messages.firstOrNull { it.isNotBlank() } ?: DEFAULT_ERROR
        }

        return if (configuration.isDebugBuild) DEBUG_APP_CHECK_ERROR else PLAY_INTEGRITY_ERROR
    }

    private companion object {
        const val DEFAULT_ERROR = "The multimodal request failed."
        const val DEBUG_APP_CHECK_ERROR =
            "Firebase rejected this device's debug App Check token. In Logcat, search for " +
                "DebugAppCheckProvider, then register that secret under Firebase Console > " +
                "App Check > Apps > Manage debug tokens. If you changed Firebase projects, " +
                "clear this app's data first to generate a new secret."
        const val PLAY_INTEGRITY_ERROR =
            "Firebase rejected Play Integrity. Install the release from a Google Play testing " +
                "track and register the Play app-signing SHA-256 certificate in Firebase App Check."
    }
}
