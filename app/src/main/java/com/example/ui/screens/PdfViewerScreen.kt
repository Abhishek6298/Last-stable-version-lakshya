package com.example.ui.screens

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures

import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.AddPhotoAlternate

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.AppViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.io.IOException

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.window.Popup
import androidx.core.content.FileProvider

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfViewerScreen(
    viewModel: AppViewModel,
    onNavigate: ((String) -> Unit)? = null,
    onNavigateBack: (() -> Unit)? = null
) {
    val isAppDarkTheme by viewModel.isDarkMode.collectAsStateWithLifecycle()
    val globalPdfUri by viewModel.currentPdfReaderUri.collectAsStateWithLifecycle()
    val globalPdfTitle by viewModel.currentPdfReaderTitle.collectAsStateWithLifecycle()

    var pdfUriString by rememberSaveable { mutableStateOf<String?>(null) }
    var pdfTitle by rememberSaveable { mutableStateOf<String?>("PDF Document") }
    var isPdfDarkMode by rememberSaveable { mutableStateOf(isAppDarkTheme) }
    var isNightMode by rememberSaveable { mutableStateOf(false) }
    var isFullScreen by rememberSaveable { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var queuedMistakeUris by remember { mutableStateOf<List<String>>(emptyList()) }

    // Gesture state for swipe-to-dismiss / swipe-back
    var dragAmountTotal by remember { mutableStateOf(0f) }
    var isDraggingBack by remember { mutableStateOf(false) }
    var dragSideIsRight by remember { mutableStateOf(false) }
    var showFullscreenControlsTemporarily by remember { mutableStateOf(false) }

    val context = androidx.compose.ui.platform.LocalContext.current
    val activity = remember(context) {
        var ctx = context
        while (ctx is android.content.ContextWrapper) {
            if (ctx is android.app.Activity) return@remember ctx
            ctx = ctx.baseContext
        }
        null
    }

    DisposableEffect(isFullScreen, activity) {
        val window = activity?.window
        if (window != null) {
            val insetsController = androidx.core.view.WindowCompat.getInsetsController(window, window.decorView)
            if (isFullScreen) {
                insetsController.systemBarsBehavior = androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                insetsController.hide(androidx.core.view.WindowInsetsCompat.Type.statusBars())
            } else {
                insetsController.show(androidx.core.view.WindowInsetsCompat.Type.statusBars())
            }
        }
        onDispose {
            activity?.window?.let { win ->
                val insetsController = androidx.core.view.WindowCompat.getInsetsController(win, win.decorView)
                insetsController.show(androidx.core.view.WindowInsetsCompat.Type.statusBars())
            }
        }
    }

    val onGestureBack: () -> Unit = remember(isFullScreen, onNavigateBack) {
        {
            if (isFullScreen) {
                isFullScreen = false
                showFullscreenControlsTemporarily = false
            } else if (onNavigateBack != null) {
                onNavigateBack()
            }
        }
    }

    // Auto-dismiss temporary controls after 3 seconds in fullscreen mode
    LaunchedEffect(showFullscreenControlsTemporarily) {
        if (showFullscreenControlsTemporarily) {
            delay(3000)
            showFullscreenControlsTemporarily = false
        }
    }

    // Intercept back presses
    BackHandler {
        when {
            showMenu -> showMenu = false
            isFullScreen -> {
                isFullScreen = false
                showFullscreenControlsTemporarily = false
            }
            onNavigateBack != null -> onNavigateBack()
        }
    }

    // Sync with global ViewModel PDF state if triggered from anywhere else in app
    LaunchedEffect(globalPdfUri) {
        if (globalPdfUri != null) {
            pdfUriString = globalPdfUri.toString()
            pdfTitle = globalPdfTitle ?: "PDF Document"
        }
    }

    var scale by remember { mutableStateOf(1f) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }

    fun resetZoom() {
        scale = 1f
        offsetX = 0f
        offsetY = 0f
    }
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            if (uri != null) {
                try {
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                pdfUriString = uri.toString()
                pdfTitle = uri.lastPathSegment ?: "Document.pdf"
                viewModel.openPdfInReader(uri, pdfTitle)
            }
        }
    )

    fun openWithExternalApp() {
        val currentUriStr = pdfUriString
        if (currentUriStr.isNullOrBlank()) {
            Toast.makeText(context, "No PDF currently loaded", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            val uri = Uri.parse(currentUriStr)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(intent, "Open PDF with..."))
        } catch (e: Exception) {
            Toast.makeText(context, "No external PDF app found or cannot open file: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    Scaffold(
        contentWindowInsets = if (isFullScreen) WindowInsets(0, 0, 0, 0) else ScaffoldDefaults.contentWindowInsets,
        topBar = {
            if (!isFullScreen) {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = pdfTitle ?: "PDF Reader",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (pdfUriString != null) {
                                Text(
                                    text = "In-App Reader • Tap Fullscreen for distraction-free reading",
                                    fontSize = 11.sp,
                                    color = if (isAppDarkTheme) Color(0x99FFFFFF) else Color(0xFF64748B)
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        if (onNavigateBack != null) {
                            IconButton(onClick = onNavigateBack) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = if (isAppDarkTheme) Color(0xFF0F172A) else Color.White
                    ),
                    actions = {
                        // Night Mode (Sepia / Warm)
                        IconButton(onClick = { isNightMode = !isNightMode }) {
                            Icon(
                                imageVector = if (isNightMode) Icons.Default.Brightness2 else Icons.Default.Brightness4,
                                contentDescription = "Night Mode",
                                tint = if (isNightMode) Color(0xFFFFB300) else LocalContentColor.current
                            )
                        }
                        // Dark Mode Invert
                        IconButton(onClick = { isPdfDarkMode = !isPdfDarkMode }) {
                            Icon(
                                imageVector = if (isPdfDarkMode) Icons.Default.Brightness7 else Icons.Default.Brightness4,
                                contentDescription = "Dark Mode Invert",
                                tint = if (isPdfDarkMode) Color(0xFF38BDF8) else LocalContentColor.current
                            )
                        }
                        // Fullscreen Mode Toggle
                        if (pdfUriString != null) {
                            IconButton(onClick = { isFullScreen = true }) {
                                Icon(
                                    imageVector = Icons.Default.Fullscreen,
                                    contentDescription = "Full Screen",
                                    tint = Color(0xFF10B981)
                                )
                            }
                        }
                        // Open With / Share Menu
                        Box {
                            IconButton(onClick = { showMenu = !showMenu }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "Options Menu")
                            }
                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false },
                                modifier = Modifier.background(if (isAppDarkTheme) Color(0xFF1E293B) else Color.White)
                            ) {
                                DropdownMenuItem(
                                    text = { Text("📂 Open Another PDF", color = if (isAppDarkTheme) Color.White else Color.Black) },
                                    leadingIcon = { Icon(Icons.Default.FolderOpen, contentDescription = null, tint = Color(0xFF6366F1)) },
                                    onClick = {
                                        showMenu = false
                                        launcher.launch(arrayOf("application/pdf"))
                                    }
                                )
                                if (pdfUriString != null) {
                                    DropdownMenuItem(
                                        text = { Text("🚀 Open With (External App)", color = if (isAppDarkTheme) Color.White else Color.Black, fontWeight = FontWeight.Bold) },
                                        leadingIcon = { Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, tint = Color(0xFF10B981)) },
                                        onClick = {
                                            showMenu = false
                                            openWithExternalApp()
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("⛶ Fullscreen Mode", color = if (isAppDarkTheme) Color.White else Color.Black) },
                                        leadingIcon = { Icon(Icons.Default.Fullscreen, contentDescription = null, tint = Color(0xFFF59E0B)) },
                                        onClick = {
                                            showMenu = false
                                            isFullScreen = true
                                        }
                                    )
                                }
                            }
                        }
                    }
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(if (isFullScreen) PaddingValues(0.dp) else paddingValues)
                .background(if (isAppDarkTheme) Color(0xFF090D16) else Color(0xFFF1F5F9))
        ) {
            if (pdfUriString == null) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(90.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF6366F1).copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PictureAsPdf,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = Color(0xFF6366F1)
                        )
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        "No PDF Selected",
                        color = if (isAppDarkTheme) Color.White else Color.Black,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "PDFs from Books, DPPs, Test Papers & Downloads will automatically open here in high definition.",
                        color = if (isAppDarkTheme) Color.LightGray else Color.DarkGray,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { launcher.launch(arrayOf("application/pdf")) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(48.dp)
                    ) {
                        Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Select PDF from Device", fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                val uri = try { Uri.parse(pdfUriString) } catch(e: Exception) { null }
                if (uri != null) {
                    PdfRendererView(
                        uri = uri,
                        isDarkMode = isPdfDarkMode,
                        isNightMode = isNightMode,
                        appDarkTheme = isAppDarkTheme,
                        onTap = {
                            if (isFullScreen) {
                                showFullscreenControlsTemporarily = !showFullscreenControlsTemporarily
                            }
                        },
                        onDoubleTap = {
                            isFullScreen = !isFullScreen
                            showFullscreenControlsTemporarily = false
                        },
                        onSmartSelect = { action, croppedUri ->
                            if (action == "mistake") {
                                val allUris = queuedMistakeUris + croppedUri.toString()
                                viewModel.pendingMistakeImageUri = allUris.joinToString("||")
                                queuedMistakeUris = emptyList()
                                onNavigate?.invoke("mistakes")
                            } else if (action == "ai") {
                                viewModel.pendingAiBotImageUri = croppedUri.toString()
                                val detectedSubject = viewModel.currentPdfReaderSubject.value ?: viewModel.detectSubjectFromPdf(pdfTitle)
                                viewModel.pendingAiBotSubject = detectedSubject
                                onNavigate?.invoke("chat")
                            } else if (action == "video_solutions" || action == "studytube") {
                                viewModel.pendingStudyTubeImageUri = croppedUri.toString()
                                onNavigate?.invoke("studytube")
                            }
                        },
                        onQueueMistakeImage = { croppedUri ->
                            queuedMistakeUris = queuedMistakeUris + croppedUri.toString()
                        },
                        queuedCount = queuedMistakeUris.size
                    )
                }
            }

            // 1. Left Edge Gesture Detector: Inward swipe from left to go back / exit fullscreen
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(44.dp)
                    .align(Alignment.CenterStart)
                    .pointerInput(isFullScreen) {
                        detectHorizontalDragGestures(
                            onDragStart = {
                                isDraggingBack = true
                                dragSideIsRight = false
                                dragAmountTotal = 0f
                            },
                            onDragEnd = {
                                if (dragAmountTotal > 45.dp.toPx()) {
                                    onGestureBack()
                                }
                                isDraggingBack = false
                                dragAmountTotal = 0f
                            },
                            onDragCancel = {
                                isDraggingBack = false
                                dragAmountTotal = 0f
                            },
                            onHorizontalDrag = { change, dragAmount ->
                                change.consume()
                                dragAmountTotal = (dragAmountTotal + dragAmount).coerceAtLeast(0f)
                            }
                        )
                    }
            )

            // 2. Right Edge Gesture Detector: Inward swipe from right to go back / exit fullscreen
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(44.dp)
                    .align(Alignment.CenterEnd)
                    .pointerInput(isFullScreen) {
                        detectHorizontalDragGestures(
                            onDragStart = {
                                isDraggingBack = true
                                dragSideIsRight = true
                                dragAmountTotal = 0f
                            },
                            onDragEnd = {
                                if (dragAmountTotal < -45.dp.toPx()) {
                                    onGestureBack()
                                }
                                isDraggingBack = false
                                dragAmountTotal = 0f
                            },
                            onDragCancel = {
                                isDraggingBack = false
                                dragAmountTotal = 0f
                            },
                            onHorizontalDrag = { change, dragAmount ->
                                change.consume()
                                dragAmountTotal = (dragAmountTotal + dragAmount).coerceAtMost(0f)
                            }
                        )
                    }
            )

            // 3. Top Edge Pull-Down Detector: Pull down from top in fullscreen to exit
            if (isFullScreen) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .align(Alignment.TopCenter)
                        .pointerInput(Unit) {
                            detectVerticalDragGestures(
                                onDragStart = {
                                    isDraggingBack = true
                                    dragAmountTotal = 0f
                                },
                                onDragEnd = {
                                    if (dragAmountTotal > 45.dp.toPx()) {
                                        onGestureBack()
                                    }
                                    isDraggingBack = false
                                    dragAmountTotal = 0f
                                },
                                onDragCancel = {
                                    isDraggingBack = false
                                    dragAmountTotal = 0f
                                },
                                onVerticalDrag = { change, dragAmount ->
                                    change.consume()
                                    dragAmountTotal = (dragAmountTotal + dragAmount).coerceAtLeast(0f)
                                }
                            )
                        }
                )
            }

            // 4. Live Visual Feedback Pill during swipe gesture
            AnimatedVisibility(
                visible = isDraggingBack && kotlin.math.abs(dragAmountTotal) > 12f,
                enter = fadeIn(animationSpec = tween(80)),
                exit = fadeOut(animationSpec = tween(120)),
                modifier = Modifier
                    .align(if (dragSideIsRight) Alignment.CenterEnd else Alignment.CenterStart)
                    .padding(horizontal = 16.dp)
            ) {
                val scaleProgress = (kotlin.math.abs(dragAmountTotal) / 120f).coerceIn(0.7f, 1.25f)
                Surface(
                    shape = RoundedCornerShape(28.dp),
                    color = Color(0xFF0F172A).copy(alpha = 0.94f),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF6366F1)),
                    shadowElevation = 12.dp,
                    modifier = Modifier.graphicsLayer {
                        scaleX = scaleProgress
                        scaleY = scaleProgress
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = if (dragSideIsRight) Icons.AutoMirrored.Filled.ArrowForward else Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Gesture Back",
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = if (isFullScreen) "Piche (Exit Fullscreen)" else "Piche",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // 5. Temporary Non-Intrusive Bottom Controls (ONLY shown briefly when tapping screen in fullscreen)
            AnimatedVisibility(
                visible = isFullScreen && showFullscreenControlsTemporarily && pdfUriString != null,
                enter = fadeIn(tween(150)) + slideInVertically { it / 2 },
                exit = fadeOut(tween(150)) + slideOutVertically { it / 2 },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color(0xFF0F172A).copy(alpha = 0.92f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155).copy(alpha = 0.8f)),
                    shadowElevation = 10.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Exit Fullscreen Button
                        IconButton(
                            onClick = {
                                isFullScreen = false
                                showFullscreenControlsTemporarily = false
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FullscreenExit,
                                contentDescription = "Exit Fullscreen",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Open with external app
                        IconButton(
                            onClick = { openWithExternalApp() },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                contentDescription = "Open With External App",
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Dark mode toggle
                        IconButton(
                            onClick = { isPdfDarkMode = !isPdfDarkMode },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = if (isPdfDarkMode) Icons.Default.Brightness7 else Icons.Default.Brightness4,
                                contentDescription = "Dark Mode",
                                tint = if (isPdfDarkMode) Color(0xFF38BDF8) else Color.LightGray,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Night mode toggle
                        IconButton(
                            onClick = { isNightMode = !isNightMode },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = if (isNightMode) Icons.Default.Brightness2 else Icons.Default.Brightness4,
                                contentDescription = "Night Mode",
                                tint = if (isNightMode) Color(0xFFFFB300) else Color.LightGray,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PdfRendererView(
    uri: Uri,
    isDarkMode: Boolean,
    isNightMode: Boolean,
    appDarkTheme: Boolean,
    onTap: (() -> Unit)? = null,
    onDoubleTap: (() -> Unit)? = null,
    onSmartSelect: ((String, Uri) -> Unit)? = null,
    onQueueMistakeImage: ((Uri) -> Unit)? = null,
    queuedCount: Int = 0
) {
    var scale by remember { mutableStateOf(1f) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }

    fun resetZoom() {
        scale = 1f
        offsetX = 0f
        offsetY = 0f
    }
    val context = LocalContext.current
    var pdfRenderer by remember { mutableStateOf<PdfRenderer?>(null) }
    var pageCount by remember { mutableStateOf(0) }
    var fileDescriptor by remember { mutableStateOf<ParcelFileDescriptor?>(null) }
    var tempFileToDelete by remember { mutableStateOf<File?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    DisposableEffect(uri) {
        var localTempFile: File? = null
        try {
            val fd: ParcelFileDescriptor? = when {
                uri.scheme == "file" -> {
                    val path = uri.path
                    val file = if (path != null) File(path) else null
                    if (file != null && file.exists()) {
                        ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                    } else {
                        context.contentResolver.openFileDescriptor(uri, "r")
                    }
                }
                else -> {
                    try {
                        context.contentResolver.openFileDescriptor(uri, "r")
                    } catch (e: Exception) {
                        // Fallback: copy from inputStream into cache to bypass permission quirks
                        try {
                            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                                val cachePdf = File(context.cacheDir, "temp_view_${System.currentTimeMillis()}.pdf")
                                cachePdf.outputStream().use { outputStream ->
                                    inputStream.copyTo(outputStream)
                                }
                                localTempFile = cachePdf
                                ParcelFileDescriptor.open(cachePdf, ParcelFileDescriptor.MODE_READ_ONLY)
                            }
                        } catch (streamEx: Exception) {
                            null
                        }
                    }
                }
            }

            if (fd != null) {
                fileDescriptor = fd
                tempFileToDelete = localTempFile
                val renderer = PdfRenderer(fd)
                pdfRenderer = renderer
                pageCount = renderer.pageCount
                errorMessage = null
            } else {
                errorMessage = "Could not open PDF document."
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
            // Fallback attempt via inputStream
            try {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    val cachePdf = File(context.cacheDir, "temp_view_${System.currentTimeMillis()}.pdf")
                    cachePdf.outputStream().use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                    val fd = ParcelFileDescriptor.open(cachePdf, ParcelFileDescriptor.MODE_READ_ONLY)
                    fileDescriptor = fd
                    tempFileToDelete = cachePdf
                    val renderer = PdfRenderer(fd)
                    pdfRenderer = renderer
                    pageCount = renderer.pageCount
                    errorMessage = null
                }
            } catch (fallbackEx: Exception) {
                errorMessage = "Permission denied. Please select or attach the file again."
            }
        } catch (e: IOException) {
            e.printStackTrace()
            errorMessage = "File not found or unreadable."
        } catch (e: Exception) {
            e.printStackTrace()
            errorMessage = e.localizedMessage ?: "Failed to open PDF."
        }
        onDispose {
            try {
                pdfRenderer?.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            try {
                fileDescriptor?.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            try {
                tempFileToDelete?.delete()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    if (errorMessage != null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Error loading PDF: $errorMessage", color = MaterialTheme.colorScheme.error)
        }
    } else if (pageCount > 0 && pdfRenderer != null) {
        val renderMutex = remember { Mutex() }
        val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
        val cacheSize = (maxMemory / 8).coerceAtLeast(16 * 1024)
        val pageCache = remember(pdfRenderer) {
            object : android.util.LruCache<Int, Bitmap>(cacheSize) {
                override fun sizeOf(key: Int, value: Bitmap): Int {
                    return value.byteCount / 1024
                }
            }
        }
        var containerSize by remember { mutableStateOf(IntSize.Zero) }

        val transformableState = rememberTransformableState { zoomChange, panChange, rotationChange ->
            scale = (scale * zoomChange).coerceIn(1f, 5f)
            if (scale > 1f) {
                val maxX = (containerSize.width * (scale - 1)) / 2
                val maxY = (containerSize.height * (scale - 1)) / 2
                offsetX = (offsetX + panChange.x).coerceIn(-maxX, maxX)
                offsetY = (offsetY + panChange.y).coerceIn(-maxY, maxY)
            } else {
                offsetX = 0f
                offsetY = 0f
            }
        }

        val isZoomed = scale > 1.01f

        Box(
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged { containerSize = it }
                .pointerInput(isZoomed) {
                    if (isZoomed) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            val maxX = (containerSize.width * (scale - 1)) / 2
                            val maxY = (containerSize.height * (scale - 1)) / 2
                            offsetX = (offsetX + dragAmount.x).coerceIn(-maxX, maxX)
                            offsetY = (offsetY + dragAmount.y).coerceIn(-maxY, maxY)
                        }
                    }
                }
                .transformable(state = transformableState)
        ) {
            LazyColumn(
                userScrollEnabled = !isZoomed,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offsetX,
                        translationY = offsetY
                    ),
                horizontalAlignment = Alignment.CenterHorizontally,
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(pageCount, key = { it }) { index ->
                    PdfPageItem(
                        pdfRenderer = pdfRenderer!!, 
                        renderMutex = renderMutex,
                        pageCache = pageCache,
                        pageIndex = index, 
                        isDarkMode = isDarkMode, 
                        isNightMode = isNightMode,
                        appDarkTheme = appDarkTheme,
                        onTap = onTap,
                        onDoubleTap = {
                            if (scale > 1.05f) {
                                resetZoom()
                            } else {
                                scale = 2.0f
                            }
                        },
                        onSmartSelect = onSmartSelect,
                        onQueueMistakeImage = onQueueMistakeImage,
                        queuedCount = queuedCount
                    )
                }
            }
        }
    } else {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Color(0xFF6366F1))
        }
    }
}

@Composable
fun PdfPageItem(
    pdfRenderer: PdfRenderer,
    renderMutex: Mutex,
    pageCache: android.util.LruCache<Int, Bitmap>,
    pageIndex: Int,
    isDarkMode: Boolean,
    isNightMode: Boolean,
    appDarkTheme: Boolean,
    onTap: (() -> Unit)? = null,
    onDoubleTap: (() -> Unit)? = null,
    onSmartSelect: ((String, Uri) -> Unit)? = null,
    onQueueMistakeImage: ((Uri) -> Unit)? = null,
    queuedCount: Int = 0
) {
    val cachedBitmap = remember(pageIndex) { pageCache.get(pageIndex) }
    var bitmap by remember(pageIndex) { mutableStateOf<Bitmap?>(if (cachedBitmap != null && !cachedBitmap.isRecycled) cachedBitmap else null) }
    var hasError by remember(pageIndex) { mutableStateOf(false) }

    LaunchedEffect(pageIndex) {
        if (bitmap != null && !bitmap!!.isRecycled) return@LaunchedEffect
        withContext(Dispatchers.IO) {
            renderMutex.withLock {
                if (!isActive) return@withLock
                val inMemory = pageCache.get(pageIndex)
                if (inMemory != null && !inMemory.isRecycled) {
                    withContext(Dispatchers.Main) { bitmap = inMemory }
                    return@withLock
                }
                try {
                    val page = pdfRenderer.openPage(pageIndex)
                    val density = 1.5f
                    val width = (page.width * density).toInt()
                    val height = (page.height * density).toInt()

                    val renderBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                    renderBitmap.eraseColor(android.graphics.Color.WHITE)
                    
                    page.render(renderBitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    page.close()
                    
                    pageCache.put(pageIndex, renderBitmap)
                    withContext(Dispatchers.Main) {
                        bitmap = renderBitmap
                    }
                } catch (e: OutOfMemoryError) {
                    pageCache.evictAll()
                    e.printStackTrace()
                    hasError = true
                } catch (e: Exception) {
                    e.printStackTrace()
                    hasError = true
                }
            }
        }
    }

    // Color matrix logic
    val colorFilter = remember(isDarkMode, isNightMode) {
        val baseMatrix = android.graphics.ColorMatrix()
        
        if (isDarkMode) {
            // Invert colors
            baseMatrix.postConcat(
                android.graphics.ColorMatrix(
                    floatArrayOf(
                        -1f, 0f, 0f, 0f, 255f, // Red
                        0f, -1f, 0f, 0f, 255f, // Green
                        0f, 0f, -1f, 0f, 255f, // Blue
                        0f, 0f, 0f, 1f, 0f     // Alpha
                    )
                )
            )
        }
        
        if (isNightMode) {
            // Warm/Yellow tint for night reading
            baseMatrix.postConcat(
                android.graphics.ColorMatrix(
                    floatArrayOf(
                        1.0f, 0f, 0f, 0f, 0f,
                        0f, 0.9f, 0f, 0f, 0f,
                        0f, 0f, 0.75f, 0f, 0f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
            )
        }
        
        if (isDarkMode || isNightMode) ColorFilter.colorMatrix(ColorMatrix(baseMatrix.array)) else null
    }

    var isSmartSelectMode by remember { mutableStateOf(false) }
    var cropRect by remember { mutableStateOf<Rect?>(null) }
    var showCropMenu by remember { mutableStateOf(false) }
    var imageSize by remember { mutableStateOf(IntSize.Zero) }
    var isPressed by remember { mutableStateOf(false) }

    BackHandler(enabled = isSmartSelectMode || showCropMenu) {
        showCropMenu = false
        isSmartSelectMode = false
        cropRect = null
    }

    val context = LocalContext.current

    LaunchedEffect(isPressed) {
        if (isPressed) {
            kotlinx.coroutines.delay(3000)
            isSmartSelectMode = true
            Toast.makeText(context, "Smart Select: Draw a box to capture.", Toast.LENGTH_SHORT).show()
        }
    }

    fun cropAndSave(b: Bitmap): Uri? {
        if (cropRect == null || imageSize.width == 0 || imageSize.height == 0) return null
        return try {
            val scaleX = b.width.toFloat() / imageSize.width.toFloat()
            val scaleY = b.height.toFloat() / imageSize.height.toFloat()

            val left = (minOf(cropRect!!.left, cropRect!!.right) * scaleX).toInt().coerceIn(0, b.width - 1)
            val top = (minOf(cropRect!!.top, cropRect!!.bottom) * scaleY).toInt().coerceIn(0, b.height - 1)
            val right = (maxOf(cropRect!!.left, cropRect!!.right) * scaleX).toInt().coerceIn(0, b.width)
            val bottom = (maxOf(cropRect!!.top, cropRect!!.bottom) * scaleY).toInt().coerceIn(0, b.height)

            val width = maxOf(1, right - left)
            val height = maxOf(1, bottom - top)

            val cropped = Bitmap.createBitmap(b, left, top, minOf(width, b.width - left), minOf(height, b.height - top))
            val tempFile = File(context.cacheDir, "smart_select_${System.currentTimeMillis()}.png")
            tempFile.outputStream().use { out ->
                cropped.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", tempFile)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    val pageAspectRatio = remember(bitmap) {
        if (bitmap != null && bitmap!!.height > 0) {
            bitmap!!.width.toFloat() / bitmap!!.height.toFloat()
        } else {
            0.707f
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(pageAspectRatio)
            .clip(RoundedCornerShape(8.dp))
            .background(if (appDarkTheme) Color(0xFF1E1E1E) else Color.White),
        contentAlignment = Alignment.Center
    ) {
        if (hasError) {
            Text("Failed to render page", color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp))
        } else if (bitmap == null) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(16.dp)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(28.dp),
                    color = Color(0xFF6366F1),
                    strokeWidth = 2.5.dp
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Page ${pageIndex + 1}",
                    fontSize = 12.sp,
                    color = if (appDarkTheme) Color.Gray else Color.LightGray
                )
            }
        } else {
            bitmap?.let { b ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .onSizeChanged { imageSize = it }
                        .pointerInput(isSmartSelectMode) {
                            if (isSmartSelectMode) {

                                var activeHandle: String? = null
                                detectDragGestures(
                                    onDragStart = { offset ->
                                        showCropMenu = false
                                        if (cropRect != null) {
                                            val r = cropRect!!
                                            val normalized = Rect(
                                                left = minOf(r.left, r.right),
                                                top = minOf(r.top, r.bottom),
                                                right = maxOf(r.left, r.right),
                                                bottom = maxOf(r.top, r.bottom)
                                            )
                                            val touchRadius = 60f
                                            if (offset.x in (normalized.left - touchRadius)..(normalized.left + touchRadius) && offset.y in (normalized.top - touchRadius)..(normalized.top + touchRadius)) {
                                                activeHandle = "topLeft"
                                            } else if (offset.x in (normalized.right - touchRadius)..(normalized.right + touchRadius) && offset.y in (normalized.top - touchRadius)..(normalized.top + touchRadius)) {
                                                activeHandle = "topRight"
                                            } else if (offset.x in (normalized.left - touchRadius)..(normalized.left + touchRadius) && offset.y in (normalized.bottom - touchRadius)..(normalized.bottom + touchRadius)) {
                                                activeHandle = "bottomLeft"
                                            } else if (offset.x in (normalized.right - touchRadius)..(normalized.right + touchRadius) && offset.y in (normalized.bottom - touchRadius)..(normalized.bottom + touchRadius)) {
                                                activeHandle = "bottomRight"
                                            } else if (normalized.contains(offset)) {
                                                activeHandle = "center"
                                            } else {
                                                activeHandle = "new"
                                                cropRect = Rect(offset, offset)
                                            }
                                        } else {
                                            activeHandle = "new"
                                            cropRect = Rect(offset, offset)
                                        }
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        val r = cropRect ?: return@detectDragGestures
                                        val normalized = Rect(
                                            left = minOf(r.left, r.right),
                                            top = minOf(r.top, r.bottom),
                                            right = maxOf(r.left, r.right),
                                            bottom = maxOf(r.top, r.bottom)
                                        )
                                        when (activeHandle) {
                                            "topLeft" -> cropRect = Rect(change.position, normalized.bottomRight)
                                            "topRight" -> cropRect = Rect(Offset(normalized.left, change.position.y), Offset(change.position.x, normalized.bottom))
                                            "bottomLeft" -> cropRect = Rect(Offset(change.position.x, normalized.top), Offset(normalized.right, change.position.y))
                                            "bottomRight" -> cropRect = Rect(normalized.topLeft, change.position)
                                            "center" -> cropRect = Rect(normalized.left + dragAmount.x, normalized.top + dragAmount.y, normalized.right + dragAmount.x, normalized.bottom + dragAmount.y)
                                            "new" -> cropRect = Rect(r.topLeft, change.position)
                                        }
                                    },
                                    onDragEnd = {
                                        showCropMenu = true
                                    },
                                    onDragCancel = {
                                        if (activeHandle == "new") {
                                            cropRect = null
                                            isSmartSelectMode = false
                                        }
                                        showCropMenu = true
                                    }
                                )
                            } else {
                                detectTapGestures(
                                    onPress = {
                                        isPressed = true
                                        tryAwaitRelease()
                                        isPressed = false
                                    },
                                    onTap = { onTap?.invoke() },
                                    onDoubleTap = { onDoubleTap?.invoke() }
                                )
                            }
                        }
                ) {
                    Image(
                        bitmap = b.asImageBitmap(),
                        contentDescription = "Page ${pageIndex + 1}",
                        modifier = Modifier.fillMaxWidth(),
                        colorFilter = colorFilter
                    )

                    if (isSmartSelectMode) {
                        Canvas(modifier = Modifier.matchParentSize().graphicsLayer(alpha = 0.99f)) {
                            drawRect(Color.Black.copy(alpha = 0.5f))
                            cropRect?.let { rect ->
                                val normalizedRect = Rect(
                                    left = minOf(rect.left, rect.right),
                                    top = minOf(rect.top, rect.bottom),
                                    right = maxOf(rect.left, rect.right),
                                    bottom = maxOf(rect.top, rect.bottom)
                                )
                                drawRect(
                                    color = Color.Transparent,
                                    topLeft = normalizedRect.topLeft,
                                    size = normalizedRect.size,
                                    blendMode = BlendMode.Clear
                                )
                                drawRect(
                                    color = Color(0xFF6366F1),
                                    topLeft = normalizedRect.topLeft,
                                    size = normalizedRect.size,
                                    style = Stroke(width = 2.dp.toPx())
                                )
                                drawCircle(Color.White, radius = 6.dp.toPx(), center = normalizedRect.topLeft)
                                drawCircle(Color(0xFF6366F1), radius = 6.dp.toPx(), center = normalizedRect.topLeft, style = Stroke(2.dp.toPx()))
                                drawCircle(Color.White, radius = 6.dp.toPx(), center = normalizedRect.topRight)
                                drawCircle(Color(0xFF6366F1), radius = 6.dp.toPx(), center = normalizedRect.topRight, style = Stroke(2.dp.toPx()))
                                drawCircle(Color.White, radius = 6.dp.toPx(), center = normalizedRect.bottomLeft)
                                drawCircle(Color(0xFF6366F1), radius = 6.dp.toPx(), center = normalizedRect.bottomLeft, style = Stroke(2.dp.toPx()))
                                drawCircle(Color.White, radius = 6.dp.toPx(), center = normalizedRect.bottomRight)
                                drawCircle(Color(0xFF6366F1), radius = 6.dp.toPx(), center = normalizedRect.bottomRight, style = Stroke(2.dp.toPx()))

                            }
                        }
                    }

                    if (showCropMenu && cropRect != null) {
                        val normalizedRect = Rect(
                            left = minOf(cropRect!!.left, cropRect!!.right),
                            top = minOf(cropRect!!.top, cropRect!!.bottom),
                            right = maxOf(cropRect!!.left, cropRect!!.right),
                            bottom = maxOf(cropRect!!.top, cropRect!!.bottom)
                        )
                        Box(
                            modifier = Modifier.offset { 
                                IntOffset(
                                    x = maxOf(0, normalizedRect.left.toInt() - 20).coerceAtMost(maxOf(0, imageSize.width - 200)),
                                    y = (normalizedRect.bottom + 10).toInt().coerceAtMost(maxOf(0, imageSize.height - 60))
                                )
                            }
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFF1E293B),
                                shadowElevation = 8.dp,
                                border = BorderStroke(1.dp, Color(0xFF334155))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .padding(horizontal = 4.dp, vertical = 4.dp)
                                        .horizontalScroll(rememberScrollState()),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    // 1. AI Study Bot Option
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0x2E38BDF8),
                                        border = BorderStroke(1.dp, Color(0x5938BDF8))
                                    ) {
                                        TextButton(
                                            onClick = { 
                                                cropAndSave(b)?.let { uri ->
                                                    onSmartSelect?.invoke("ai", uri)
                                                }
                                                isSmartSelectMode = false
                                                showCropMenu = false
                                            },
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Icon(Icons.Default.AutoAwesome, null, tint = Color(0xFF38BDF8), modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text("AI Study Bot", color = Color(0xFF38BDF8), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    // 2. Mistake Log Option
                                    TextButton(
                                        onClick = { 
                                            cropAndSave(b)?.let { uri ->
                                                onSmartSelect?.invoke("mistake", uri)
                                            }
                                            isSmartSelectMode = false
                                            showCropMenu = false
                                        },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Icon(Icons.Default.Book, null, tint = Color(0xFF818CF8), modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text(if (queuedCount > 0) "Save All ($queuedCount+1)" else "Mistake Log", color = Color(0xFFA5B4FC), fontSize = 12.sp)
                                    }

                                    // 3. Video Solutions Option
                                    TextButton(
                                        onClick = { 
                                            cropAndSave(b)?.let { uri ->
                                                onSmartSelect?.invoke("video_solutions", uri)
                                            }
                                            isSmartSelectMode = false
                                            showCropMenu = false
                                        },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Icon(Icons.Default.PlayCircleFilled, null, tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Video Solutions", color = Color(0xFFFCA5A5), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }

                                    // 4. Add Another Option
                                    TextButton(
                                        onClick = { 
                                            cropAndSave(b)?.let { uri ->
                                                onQueueMistakeImage?.invoke(uri)
                                            }
                                            isSmartSelectMode = false
                                            showCropMenu = false
                                            cropRect = null
                                        },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Icon(Icons.Default.AddPhotoAlternate, null, tint = Color(0xFFF59E0B), modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Add Another", color = Color(0xFFFCD34D), fontSize = 12.sp)
                                    }

                                    // Close
                                    IconButton(
                                        onClick = { 
                                            isSmartSelectMode = false
                                            cropRect = null
                                            showCropMenu = false 
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.Close, null, tint = Color(0xFF94A3B8), modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            } ?: CircularProgressIndicator(color = Color(0xFF6366F1))
        }
    }
}
