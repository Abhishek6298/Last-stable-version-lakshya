package com.example.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max
import kotlin.math.min

/**
 * High-performance, lag-free Square Image Cropper & Rotator for AI Study Bot.
 * 
 * Strict specifications:
 * - Square crop frame (1:1 aspect ratio) with smooth 120 FPS hardware-accelerated touch handling.
 * - Safely padded strictly below status bar (statusBars) and above navigation bar (navigationBars).
 * - 90-degree lossless clockwise rotation.
 * - Top header controls: Cancel button, Tick (confirm) button right beside cancel, and Rotate button. Zero bottom bar clutter.
 */
@Composable
fun ImageCropperDialog(
    imageUri: Uri,
    onCropSuccess: (Uri) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val view = LocalView.current

    var rotationDegrees by remember { mutableIntStateOf(0) }
    var loadedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isProcessing by remember { mutableStateOf(false) }

    // Efficiently decode bitmap on background thread with downsampling to prevent memory lag
    LaunchedEffect(imageUri) {
        withContext(Dispatchers.IO) {
            try {
                // 1. Measure dimensions
                val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                var stream = context.contentResolver.openInputStream(imageUri)
                BitmapFactory.decodeStream(stream, null, options)
                stream?.close()

                // 2. Downsample large camera photos to ~2048 max dimension for instantaneous 120 FPS rendering
                var sampleSize = 1
                val maxDim = 2048
                var w = options.outWidth
                var h = options.outHeight
                while (w / 2 >= maxDim || h / 2 >= maxDim) {
                    w /= 2
                    h /= 2
                    sampleSize *= 2
                }

                val decodeOptions = BitmapFactory.Options().apply {
                    inSampleSize = sampleSize
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                }
                stream = context.contentResolver.openInputStream(imageUri)
                val bmp = BitmapFactory.decodeStream(stream, null, decodeOptions)
                stream?.close()
                loadedBitmap = bmp
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    Dialog(
        onDismissRequest = {
            if (!isProcessing) onDismiss()
        },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        // Configure edge-to-edge window so insets work accurately
        DisposableEffect(view) {
            var parent = view.parent
            while (parent != null && parent !is DialogWindowProvider) {
                parent = parent.parent
            }
            val dialogWindow = (parent as? DialogWindowProvider)?.window
            dialogWindow?.let { win ->
                win.setLayout(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT
                )
                WindowCompat.setDecorFitsSystemWindows(win, false)
                ViewCompat.setOnApplyWindowInsetsListener(win.decorView) { _, insets ->
                    ViewCompat.dispatchApplyWindowInsets(view, insets)
                    insets
                }
                view.requestApplyInsets()
            }
            onDispose {}
        }

        // Apply rotation to bitmap
        val rawBmp = loadedBitmap
        val rotatedBmp = remember(rawBmp, rotationDegrees) {
            if (rawBmp == null || rotationDegrees == 0) rawBmp else {
                val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
                Bitmap.createBitmap(rawBmp, 0, 0, rawBmp.width, rawBmp.height, matrix, true)
            }
        }

        var containerSize by remember { mutableStateOf(IntSize.Zero) }
        var cropRectPx by remember { mutableStateOf<Rect?>(null) }
        var activeHandle by remember { mutableStateOf<String?>(null) }

        val density = LocalDensity.current
        val handleTouchRadiusPx = with(density) { 42.dp.toPx() }
        val minSidePx = with(density) { 64.dp.toPx() }

        // Calculate aspect ratio fit of image in container
        val imgDrawRect = remember(rotatedBmp, containerSize) {
            if (rotatedBmp == null || containerSize.width <= 0 || containerSize.height <= 0) {
                Rect.Zero
            } else {
                val containerW = containerSize.width.toFloat()
                val containerH = containerSize.height.toFloat()
                val bmpW = rotatedBmp.width.toFloat()
                val bmpH = rotatedBmp.height.toFloat()

                val scale = min(containerW / bmpW, containerH / bmpH)
                val drawnW = bmpW * scale
                val drawnH = bmpH * scale
                val left = (containerW - drawnW) / 2f
                val top = (containerH - drawnH) / 2f

                Rect(left, top, left + drawnW, top + drawnH)
            }
        }

        // Initialize crop rect to a centered SQUARE on layout or rotation
        LaunchedEffect(imgDrawRect, rotationDegrees) {
            if (imgDrawRect.width > 0 && imgDrawRect.height > 0) {
                val side = min(imgDrawRect.width, imgDrawRect.height) * 0.85f
                val cX = imgDrawRect.center.x
                val cY = imgDrawRect.center.y
                cropRectPx = Rect(
                    left = cX - side / 2f,
                    top = cY - side / 2f,
                    right = cX + side / 2f,
                    bottom = cY + side / 2f
                )
            }
        }

        // Execution logic for Square Crop & Save
        val executeCrop: () -> Unit = {
            val crop = cropRectPx
            val bmp = rotatedBmp
            if (crop != null && bmp != null && imgDrawRect.width > 0 && imgDrawRect.height > 0 && !isProcessing) {
                isProcessing = true
                coroutineScope.launch(Dispatchers.IO) {
                    try {
                        val bmpW = bmp.width.toFloat()
                        val scale = bmpW / imgDrawRect.width

                        val pixelLeft = ((crop.left - imgDrawRect.left) * scale).toInt().coerceIn(0, bmp.width - 1)
                        val pixelTop = ((crop.top - imgDrawRect.top) * scale).toInt().coerceIn(0, bmp.height - 1)
                        val pixelSide = (crop.width * scale).toInt().coerceIn(1, min(bmp.width - pixelLeft, bmp.height - pixelTop))

                        val croppedBmp = Bitmap.createBitmap(bmp, pixelLeft, pixelTop, pixelSide, pixelSide)

                        val outFile = File(context.cacheDir, "cropped_question_${System.currentTimeMillis()}.jpg")
                        val fos = FileOutputStream(outFile)
                        croppedBmp.compress(Bitmap.CompressFormat.JPEG, 92, fos)
                        fos.flush()
                        fos.close()
                        croppedBmp.recycle()

                        val resultUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", outFile)
                        withContext(Dispatchers.Main) {
                            isProcessing = false
                            onCropSuccess(resultUri)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        withContext(Dispatchers.Main) {
                            isProcessing = false
                            onDismiss()
                        }
                    }
                }
            }
        }

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFF090D16)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
            ) {
                // ==================== 1. TOP HEADER BAR ====================
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left side: Cancel Button + Correct Tick Button side-by-side
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Cancel Icon Button
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(Color(0x22FFFFFF))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Cancel",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        // Correct Tick Icon Button (at the side of Cancel icon)
                        IconButton(
                            onClick = executeCrop,
                            enabled = !isProcessing && rotatedBmp != null,
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(if (!isProcessing && rotatedBmp != null) Color(0xFF10B981) else Color(0x3310B981))
                        ) {
                            if (isProcessing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Confirm Crop",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }

                    Text(
                        text = "Crop Question",
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )

                    // Right side: Rotate Button (90° Clockwise)
                    IconButton(
                        onClick = { rotationDegrees = (rotationDegrees + 90) % 360 },
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Color(0x22FFFFFF))
                    ) {
                        Icon(
                            imageVector = Icons.Default.RotateRight,
                            contentDescription = "Rotate 90°",
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                // ==================== 2. CENTER IMAGE & SQUARE CROP FRAME ====================
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (rotatedBmp == null) {
                        CircularProgressIndicator(color = Color(0xFF10B981))
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .onSizeChanged { containerSize = it }
                                .pointerInput(imgDrawRect) {
                                    detectDragGestures(
                                        onDragStart = { offset ->
                                            val current = cropRectPx ?: return@detectDragGestures
                                            val l = current.left
                                            val t = current.top
                                            val r = current.right
                                            val b = current.bottom

                                            // Determine which handle or center is touched
                                            activeHandle = when {
                                                (offset - Offset(l, t)).getDistance() <= handleTouchRadiusPx -> "TL"
                                                (offset - Offset(r, t)).getDistance() <= handleTouchRadiusPx -> "TR"
                                                (offset - Offset(l, b)).getDistance() <= handleTouchRadiusPx -> "BL"
                                                (offset - Offset(r, b)).getDistance() <= handleTouchRadiusPx -> "BR"
                                                kotlin.math.abs(offset.y - t) <= handleTouchRadiusPx && offset.x in l..r -> "T"
                                                kotlin.math.abs(offset.y - b) <= handleTouchRadiusPx && offset.x in l..r -> "B"
                                                kotlin.math.abs(offset.x - l) <= handleTouchRadiusPx && offset.y in t..b -> "L"
                                                kotlin.math.abs(offset.x - r) <= handleTouchRadiusPx && offset.y in t..b -> "R"
                                                current.contains(offset) -> "CENTER"
                                                else -> null
                                            }
                                        },
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            val current = cropRectPx ?: return@detectDragGestures

                                            val minX = imgDrawRect.left
                                            val maxX = imgDrawRect.right
                                            val minY = imgDrawRect.top
                                            val maxY = imgDrawRect.bottom

                                            when (activeHandle) {
                                                "BR" -> {
                                                    // Resize square anchored at Top-Left
                                                    val delta = (dragAmount.x + dragAmount.y) / 2f
                                                    val maxSide = min(maxX - current.left, maxY - current.top)
                                                    val newSide = (current.width + delta).coerceIn(minSidePx, maxSide)
                                                    cropRectPx = Rect(current.left, current.top, current.left + newSide, current.top + newSide)
                                                }
                                                "TL" -> {
                                                    // Resize square anchored at Bottom-Right
                                                    val delta = (-dragAmount.x - dragAmount.y) / 2f
                                                    val maxSide = min(current.right - minX, current.bottom - minY)
                                                    val newSide = (current.width + delta).coerceIn(minSidePx, maxSide)
                                                    cropRectPx = Rect(current.right - newSide, current.bottom - newSide, current.right, current.bottom)
                                                }
                                                "TR" -> {
                                                    // Resize square anchored at Bottom-Left
                                                    val delta = (dragAmount.x - dragAmount.y) / 2f
                                                    val maxSide = min(maxX - current.left, current.bottom - minY)
                                                    val newSide = (current.width + delta).coerceIn(minSidePx, maxSide)
                                                    cropRectPx = Rect(current.left, current.bottom - newSide, current.left + newSide, current.bottom)
                                                }
                                                "BL" -> {
                                                    // Resize square anchored at Top-Right
                                                    val delta = (-dragAmount.x + dragAmount.y) / 2f
                                                    val maxSide = min(current.right - minX, maxY - current.top)
                                                    val newSide = (current.width + delta).coerceIn(minSidePx, maxSide)
                                                    cropRectPx = Rect(current.right - newSide, current.top, current.right, current.top + newSide)
                                                }
                                                "T" -> {
                                                    // Resize square symmetrically from Top
                                                    val delta = dragAmount.y
                                                    val newSide = (current.height - delta).coerceIn(minSidePx, current.bottom - minY)
                                                    val cX = current.center.x
                                                    var newL = cX - newSide / 2f
                                                    var newR = cX + newSide / 2f
                                                    if (newL < minX) { newL = minX; newR = minX + newSide }
                                                    if (newR > maxX) { newR = maxX; newL = maxX - newSide }
                                                    cropRectPx = Rect(newL, current.bottom - newSide, newR, current.bottom)
                                                }
                                                "B" -> {
                                                    // Resize square symmetrically from Bottom
                                                    val delta = dragAmount.y
                                                    val newSide = (current.height + delta).coerceIn(minSidePx, maxY - current.top)
                                                    val cX = current.center.x
                                                    var newL = cX - newSide / 2f
                                                    var newR = cX + newSide / 2f
                                                    if (newL < minX) { newL = minX; newR = minX + newSide }
                                                    if (newR > maxX) { newR = maxX; newL = maxX - newSide }
                                                    cropRectPx = Rect(newL, current.top, newR, current.top + newSide)
                                                }
                                                "L" -> {
                                                    // Resize square symmetrically from Left
                                                    val delta = dragAmount.x
                                                    val newSide = (current.width - delta).coerceIn(minSidePx, current.right - minX)
                                                    val cY = current.center.y
                                                    var newT = cY - newSide / 2f
                                                    var newB = cY + newSide / 2f
                                                    if (newT < minY) { newT = minY; newB = minY + newSide }
                                                    if (newB > maxY) { newB = maxY; newT = maxY - newSide }
                                                    cropRectPx = Rect(current.right - newSide, newT, current.right, newB)
                                                }
                                                "R" -> {
                                                    // Resize square symmetrically from Right
                                                    val delta = dragAmount.x
                                                    val newSide = (current.width + delta).coerceIn(minSidePx, maxX - current.left)
                                                    val cY = current.center.y
                                                    var newT = cY - newSide / 2f
                                                    var newB = cY + newSide / 2f
                                                    if (newT < minY) { newT = minY; newB = minY + newSide }
                                                    if (newB > maxY) { newB = maxY; newT = maxY - newSide }
                                                    cropRectPx = Rect(current.left, newT, current.left + newSide, newB)
                                                }
                                                "CENTER" -> {
                                                    // Smooth panning of the square box
                                                    val curW = current.width
                                                    val curH = current.height
                                                    val newL = (current.left + dragAmount.x).coerceIn(minX, maxX - curW)
                                                    val newT = (current.top + dragAmount.y).coerceIn(minY, maxY - curH)
                                                    cropRectPx = Rect(newL, newT, newL + curW, newT + curH)
                                                }
                                            }
                                        },
                                        onDragEnd = { activeHandle = null },
                                        onDragCancel = { activeHandle = null }
                                    )
                                }
                        ) {
                            val imgBitmap = remember(rotatedBmp) { rotatedBmp.asImageBitmap() }

                            Canvas(modifier = Modifier.fillMaxSize()) {
                                if (imgDrawRect.width <= 0 || imgDrawRect.height <= 0) return@Canvas

                                // 1. Draw scaled & rotated image
                                drawImage(
                                    image = imgBitmap,
                                    dstOffset = androidx.compose.ui.unit.IntOffset(imgDrawRect.left.toInt(), imgDrawRect.top.toInt()),
                                    dstSize = IntSize(imgDrawRect.width.toInt(), imgDrawRect.height.toInt())
                                )

                                val crop = cropRectPx ?: return@Canvas

                                // 2. Dark semi-transparent scrim outside square crop
                                val scrimColor = Color(0xB3000000)

                                // Top scrim
                                drawRect(scrimColor, topLeft = Offset(imgDrawRect.left, imgDrawRect.top), size = Size(imgDrawRect.width, max(0f, crop.top - imgDrawRect.top)))
                                // Bottom scrim
                                drawRect(scrimColor, topLeft = Offset(imgDrawRect.left, crop.bottom), size = Size(imgDrawRect.width, max(0f, imgDrawRect.bottom - crop.bottom)))
                                // Left scrim
                                drawRect(scrimColor, topLeft = Offset(imgDrawRect.left, crop.top), size = Size(max(0f, crop.left - imgDrawRect.left), crop.height))
                                // Right scrim
                                drawRect(scrimColor, topLeft = Offset(crop.right, crop.top), size = Size(max(0f, imgDrawRect.right - crop.right), crop.height))

                                // 3. Crisp white 1:1 square border
                                drawRect(
                                    color = Color.White,
                                    topLeft = Offset(crop.left, crop.top),
                                    size = Size(crop.width, crop.height),
                                    style = Stroke(width = 2.dp.toPx())
                                )

                                // 4. Rule-of-thirds grid lines (subtle guide)
                                val thirdW = crop.width / 3f
                                val thirdH = crop.height / 3f
                                val gridColor = Color(0x55FFFFFF)
                                val gridStroke = 1.dp.toPx()

                                drawLine(gridColor, Offset(crop.left + thirdW, crop.top), Offset(crop.left + thirdW, crop.bottom), strokeWidth = gridStroke)
                                drawLine(gridColor, Offset(crop.left + 2 * thirdW, crop.top), Offset(crop.left + 2 * thirdW, crop.bottom), strokeWidth = gridStroke)
                                drawLine(gridColor, Offset(crop.left, crop.top + thirdH), Offset(crop.right, crop.top + thirdH), strokeWidth = gridStroke)
                                drawLine(gridColor, Offset(crop.left, crop.top + 2 * thirdH), Offset(crop.right, crop.top + 2 * thirdH), strokeWidth = gridStroke)

                                // 5. Prominent neon cyan corner handles
                                val cornerLen = 22.dp.toPx()
                                val cornerStroke = 4.dp.toPx()
                                val cornerColor = Color(0xFF38BDF8)

                                // Top-Left
                                drawLine(cornerColor, Offset(crop.left - 1, crop.top), Offset(crop.left + cornerLen, crop.top), strokeWidth = cornerStroke)
                                drawLine(cornerColor, Offset(crop.left, crop.top - 1), Offset(crop.left, crop.top + cornerLen), strokeWidth = cornerStroke)

                                // Top-Right
                                drawLine(cornerColor, Offset(crop.right + 1, crop.top), Offset(crop.right - cornerLen, crop.top), strokeWidth = cornerStroke)
                                drawLine(cornerColor, Offset(crop.right, crop.top - 1), Offset(crop.right, crop.top + cornerLen), strokeWidth = cornerStroke)

                                // Bottom-Left
                                drawLine(cornerColor, Offset(crop.left - 1, crop.bottom), Offset(crop.left + cornerLen, crop.bottom), strokeWidth = cornerStroke)
                                drawLine(cornerColor, Offset(crop.left, crop.bottom + 1), Offset(crop.left, crop.bottom - cornerLen), strokeWidth = cornerStroke)

                                // Bottom-Right
                                drawLine(cornerColor, Offset(crop.right + 1, crop.bottom), Offset(crop.right - cornerLen, crop.bottom), strokeWidth = cornerStroke)
                                drawLine(cornerColor, Offset(crop.right, crop.bottom + 1), Offset(crop.right, crop.bottom - cornerLen), strokeWidth = cornerStroke)
                            }
                        }
                    }
                }
            }
        }
    }
}
