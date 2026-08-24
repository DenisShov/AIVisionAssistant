package com.example.multimodalassistant.data.classifier

import android.content.Context
import android.graphics.Bitmap
import androidx.core.graphics.scale
import com.example.multimodalassistant.domain.model.ClassificationResult
import com.example.multimodalassistant.domain.repository.ImageClassifier
import com.google.ai.edge.litert.Accelerator
import com.google.ai.edge.litert.CompiledModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class LiteRtImageClassifier @Inject constructor(
    @ApplicationContext context: Context,
) : ImageClassifier {
    private val assets = context.applicationContext.assets
    private val labels by lazy {
        assets.open(LABELS_FILE).bufferedReader().use { it.readLines() }
    }

    private var compiledModel: CompiledModel? = null
    private var activeAccelerator = Accelerator.GPU

    @Synchronized
    override fun classify(bitmap: Bitmap): ClassificationResult {
        val model = compiledModel ?: createModel().also { compiledModel = it }
        return try {
            runInference(model, bitmap)
        } catch (error: Exception) {
            val fallback = when (activeAccelerator) {
                Accelerator.NPU -> Accelerator.GPU
                Accelerator.GPU -> Accelerator.CPU
                else -> throw error
            }
            compiledModel?.close()
            compiledModel = try {
                createModel(fallback)
            } catch (_: Exception) {
                if (fallback == Accelerator.GPU) createModel(Accelerator.CPU) else throw error
            }
            classify(bitmap)
        }
    }

    private fun runInference(model: CompiledModel, bitmap: Bitmap): ClassificationResult {
        val inputBuffers = model.createInputBuffers()
        val outputBuffers = model.createOutputBuffers()

        try {
            // The bundled official MobileNet V2 model has uint8 input/output tensors. LiteRT's
            // Kotlin API transports uint8 values through ByteArray, preserving their bit pattern.
            inputBuffers.first().writeInt8(bitmap.toRgbBytes())
            model.run(inputBuffers, outputBuffers)
            val scores = outputBuffers.first().readInt8().map {
                (it.toInt() and 0xFF) / 255f
            }

            val bestIndex = scores.indices.maxByOrNull(scores::get) ?: 0
            return ClassificationResult(
                label = labels.getOrElse(bestIndex) { "ImageNet class $bestIndex" },
                confidence = scores.getOrElse(bestIndex) { 0f }.coerceIn(0f, 1f),
                accelerator = activeAccelerator.name,
            )
        } finally {
            inputBuffers.forEach(AutoCloseable::close)
            outputBuffers.forEach(AutoCloseable::close)
        }
    }

    private fun createModel(): CompiledModel = try {
        createModel(Accelerator.NPU)
    } catch (_: Exception) {
        try {
            createModel(Accelerator.GPU)
        } catch (_: Exception) {
            createModel(Accelerator.CPU)
        }
    }

    private fun createModel(accelerator: Accelerator): CompiledModel {
        activeAccelerator = accelerator
        return if (accelerator == Accelerator.CPU) {
            CompiledModel.create(assets, MODEL_FILE, CompiledModel.Options.CPU)
        } else {
            CompiledModel.create(
                assets,
                MODEL_FILE,
                CompiledModel.Options(accelerator),
            )
        }
    }

    override fun close() {
        compiledModel?.close()
        compiledModel = null
    }

    private fun Bitmap.squarePixels(): IntArray {
        val side = minOf(width, height)
        val x = (width - side) / 2
        val y = (height - side) / 2
        val cropped = Bitmap.createBitmap(this, x, y, side, side)
        val scaled = cropped.scale(INPUT_SIZE, INPUT_SIZE)
        val pixels = IntArray(INPUT_SIZE * INPUT_SIZE)
        scaled.getPixels(pixels, 0, INPUT_SIZE, 0, 0, INPUT_SIZE, INPUT_SIZE)
        if (scaled !== cropped) scaled.recycle()
        if (cropped !== this) cropped.recycle()
        return pixels
    }

    private fun Bitmap.toRgbBytes(): ByteArray {
        val pixels = squarePixels()
        return ByteArray(pixels.size * CHANNELS).also { output ->
            var index = 0
            pixels.forEach { pixel ->
                output[index++] = (pixel shr 16 and 0xFF).toByte()
                output[index++] = (pixel shr 8 and 0xFF).toByte()
                output[index++] = (pixel and 0xFF).toByte()
            }
        }
    }

    private companion object {
        const val MODEL_FILE = "mobilenet_v2.tflite"
        const val LABELS_FILE = "labels.txt"
        const val INPUT_SIZE = 224
        const val CHANNELS = 3
    }
}
