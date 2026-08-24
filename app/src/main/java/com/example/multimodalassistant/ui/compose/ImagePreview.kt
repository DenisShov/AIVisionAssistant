package com.example.multimodalassistant.ui.compose

import android.graphics.Bitmap
import android.graphics.Paint as AndroidPaint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.multimodalassistant.domain.model.OcrResult
import kotlin.math.min

@Composable
internal fun ImagePreview(
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
                topLeft = Offset(left, top),
                size = Size(width, height),
            )
            drawRect(
                color = boxColor,
                topLeft = Offset(left, top),
                size = Size(width, height),
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

