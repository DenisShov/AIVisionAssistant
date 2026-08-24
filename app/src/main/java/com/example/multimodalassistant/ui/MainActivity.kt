package com.example.multimodalassistant.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.example.multimodalassistant.data.scanner.DocumentScannerManager
import com.example.multimodalassistant.domain.repository.SpeechInput
import com.example.multimodalassistant.ui.compose.AssistantScreen
import com.example.multimodalassistant.ui.theme.AIVisionAssistantTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel: AssistantViewModel by viewModels()

    @Inject
    lateinit var speechInput: SpeechInput

    @Inject
    lateinit var scannerManager: DocumentScannerManager

    private val scannerLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult(),
    ) { result ->
        lifecycleScope.launch {
            runCatching { scannerManager.decodeResult(result.resultCode, result.data) }
                .onSuccess { bitmap ->
                    if (bitmap != null) viewModel.setImage(bitmap) else viewModel.setScanning(false)
                }
                .onFailure { error ->
                    viewModel.showError(error.localizedMessage ?: "The scanned image could not be opened.")
                }
        }
    }

    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri == null) {
            viewModel.setImporting(false)
            return@registerForActivityResult
        }
        lifecycleScope.launch {
            runCatching { scannerManager.decodeUri(uri) }
                .onSuccess(viewModel::setImage)
                .onFailure { error ->
                    viewModel.showError(error.localizedMessage ?: "The selected image could not be opened.")
                }
        }
    }

    private val microphonePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            speechInput.startListening()
        } else {
            viewModel.showError("Microphone permission is required to record speech.")
        }
    }

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) {
        // ML Kit owns the scanner camera UI and can still start if this app-level permission is denied.
        launchScanner()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            AIVisionAssistantTheme {
                val state by viewModel.uiState.collectAsStateWithLifecycle()
                val speechState by speechInput.state.collectAsStateWithLifecycle()
                AssistantScreen(
                    state = state,
                    speechState = speechState,
                    onScan = ::requestScan,
                    onPickImage = ::pickImage,
                    onStartSpeech = ::requestSpeech,
                    onStopSpeech = speechInput::stopListening,
                    onToggleOcrBoxes = viewModel::toggleOcrBoxes,
                    onProcess = viewModel::process,
                )
            }
        }
    }

    private fun requestSpeech() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            speechInput.startListening()
        } else {
            microphonePermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    private fun requestScan() {
        if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            launchScanner()
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun pickImage() {
        viewModel.setImporting(true)
        galleryLauncher.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
        )
    }

    private fun launchScanner() {
        viewModel.setScanning(true)
        scannerManager.startScanIntent()
            .addOnSuccessListener(this) { sender ->
                scannerLauncher.launch(IntentSenderRequest.Builder(sender).build())
            }
            .addOnFailureListener(this) { error ->
                viewModel.showError(error.localizedMessage ?: "ML Kit document scanner is unavailable.")
            }
    }

    override fun onDestroy() {
        speechInput.close()
        super.onDestroy()
    }
}
