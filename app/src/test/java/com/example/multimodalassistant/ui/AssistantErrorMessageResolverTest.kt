package com.example.multimodalassistant.ui

import com.example.multimodalassistant.domain.model.AssistantConfiguration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AssistantErrorMessageResolverTest {
    @Test
    fun returnsNestedCauseMessageForRegularFailure() {
        val resolver = resolver(isDebugBuild = true)

        val message = resolver.resolve(IllegalStateException("Request failed"))

        assertEquals("Request failed", message)
    }

    @Test
    fun explainsDebugTokenRegistrationForInvalidDebugAppCheckToken() {
        val resolver = resolver(isDebugBuild = true)

        val message = resolver.resolve(
            IllegalStateException("Firebase App Check token is invalid."),
        )

        assertTrue(message.contains("Manage debug tokens"))
    }

    @Test
    fun explainsPlayIntegrityForInvalidReleaseAppCheckToken() {
        val resolver = resolver(isDebugBuild = false)

        val message = resolver.resolve(
            IllegalStateException("App attestation failed"),
        )

        assertTrue(message.contains("Play Integrity"))
    }

    private fun resolver(isDebugBuild: Boolean) = AssistantErrorMessageResolver(
        AssistantConfiguration(
            isFirebaseConfigured = true,
            isDebugBuild = isDebugBuild,
        ),
    )
}
