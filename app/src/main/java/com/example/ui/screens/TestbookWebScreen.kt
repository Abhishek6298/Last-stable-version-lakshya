package com.example.ui.screens

import android.app.Activity
import android.app.DownloadManager
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.view.ViewGroup
import android.webkit.*
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import com.example.ui.components.FireworksProgressBar
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.FileProvider
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.GeminiChatAssistant
import com.example.data.StudyPortal
import com.example.data.StudyPortalGuard
import com.example.data.TestbookScorecardResult
import com.example.ui.AppViewModel
import com.example.ui.components.AddEditStudyPortalDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

private const val DESKTOP_USER_AGENT =
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36"
private const val MOBILE_USER_AGENT =
    "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Mobile Safari/537.36"

data class DownloadedPdfRecord(
    val id: String = UUID.randomUUID().toString(),
    val fileName: String,
    val filePath: String,
    val fileSizeFormatted: String,
    val dateAdded: String = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date()),
    val uriString: String? = null
)

enum class ActiveDownloadState {
    DOWNLOADING,
    COMPLETED,
    FAILED
}

data class ActiveDownloadInfo(
    val fileName: String,
    val progress: Float, // 0f to 1f
    val downloadedBytes: Long,
    val totalBytes: Long,
    val state: ActiveDownloadState,
    val filePath: String? = null,
    val fileUri: Uri? = null,
    val errorMessage: String? = null
)

@android.annotation.SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TestbookWebScreen(
    viewModel: AppViewModel,
    initialUrl: String = "https://testbook.com/test-series",
    onBackToApp: () -> Unit,
    onNavigate: ((String) -> Unit)? = null
) {
    val isDark by viewModel.isDarkMode.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val activity = remember(context) { context.findActivity() }

    val studyPortals by viewModel.studyPortals.collectAsStateWithLifecycle()
    var showPortalSelectorDialog by remember { mutableStateOf(false) }
    var showAddEditPortalDialog by remember { mutableStateOf(false) }
    var editingPortal by remember { mutableStateOf<StudyPortal?>(null) }
    var portalToDelete by remember { mutableStateOf<StudyPortal?>(null) }

    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var currentUrl by remember { mutableStateOf(initialUrl) }
    var webProgress by remember { mutableFloatStateOf(0f) }
    var canGoBack by remember { mutableStateOf(false) }
    var canGoForward by remember { mutableStateOf(false) }

    // HTML5 File Chooser / Document Upload Handler for web portals (NotebookLM, PW, Google Drive, Testbook)
    var fileUploadCallback by remember { mutableStateOf<ValueCallback<Array<Uri>>?>(null) }

    val fileChooserLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val intent = result.data
            val results: Array<Uri>? = when {
                intent?.data != null -> arrayOf(intent.data!!)
                intent?.clipData != null -> {
                    val count = intent.clipData!!.itemCount
                    val uris = Array(count) { i -> intent.clipData!!.getItemAt(i).uri }
                    uris
                }
                else -> null
            }
            fileUploadCallback?.onReceiveValue(results)
        } else {
            fileUploadCallback?.onReceiveValue(null)
        }
        fileUploadCallback = null
    }

    // Default to mobile view so touch interactions, taps and responsive UI work properly
    var isDesktopMode by remember { mutableStateOf(false) }

    // Website Dark Mode toggle (Default: false so sites like PW Thor load with their original native theme)
    var isWebDarkMode by remember { mutableStateOf(false) }

    // Clean, non-destructive JavaScript dark styling for sites that don't have their own dark mode
    val darkThemeJs = """
        (function() {
            var cssId = 'neet-master-dark-theme-style';
            var existing = document.getElementById(cssId);
            if (existing) {
                existing.remove();
            }
            var style = document.createElement('style');
            style.id = cssId;
            style.type = 'text/css';
            style.innerHTML = `
                html {
                    filter: invert(1) hue-rotate(180deg) !important;
                    background-color: #fff !important; /* Ensure background inverts to dark */
                }
                img, picture, video, iframe, canvas, svg {
                    filter: invert(1) hue-rotate(180deg) !important;
                }
            `;
            document.head.appendChild(style);
        })();
    """.trimIndent()

    val removeDarkThemeJs = """
        (function() {
            var cssId = 'neet-master-dark-theme-style';
            var existing = document.getElementById(cssId);
            if (existing) {
                existing.remove();
            }
        })();
    """.trimIndent()

    fun applyWebTheme(wv: WebView?, enableDark: Boolean) {
        if (wv == null) return
        if (enableDark) {
            wv.evaluateJavascript(darkThemeJs, null)
        } else {
            wv.evaluateJavascript(removeDarkThemeJs, null)
        }
    }

    LaunchedEffect(isWebDarkMode, webViewInstance) {
        applyWebTheme(webViewInstance, isWebDarkMode)
    }

    // Clean, automatic script to remove any website-generated "Exit Fullscreen" or "Normal" overlays / bars
    val removeExitFullscreenMenuJs = """
        (function() {
            function removeExitFsElements() {
                var selectors = [
                    '[class*="exit-fullscreen" i]',
                    '[class*="fullscreen-exit" i]',
                    '[id*="exit-fullscreen" i]',
                    '[id*="fullscreen-exit" i]',
                    '[class*="exit_fullscreen" i]',
                    '[id*="exit_fullscreen" i]',
                    '[class*="btn-fullscreen-exit" i]',
                    '[class*="normal-mode" i]',
                    '[class*="normal-screen" i]'
                ];
                selectors.forEach(function(sel) {
                    try {
                        document.querySelectorAll(sel).forEach(function(el) {
                            el.style.setProperty('display', 'none', 'important');
                        });
                    } catch (e) {}
                });

                var candidates = document.querySelectorAll('button, a, div, span, p');
                for (var i = 0; i < candidates.length; i++) {
                    var el = candidates[i];
                    if (el.children.length <= 2) {
                        var txt = (el.innerText || el.textContent || '').trim();
                        if (/^(normal|exit\s*full\s*screen|exit\s*fullscreen|normal\s*screen)$/i.test(txt)) {
                            var style = window.getComputedStyle(el);
                            if (style.position === 'fixed' || style.position === 'absolute' || el.closest('[class*="fullscreen" i], [class*="bar" i], [class*="header" i], [class*="menu" i]')) {
                                el.style.setProperty('display', 'none', 'important');
                            }
                        }
                    }
                }
            }
            removeExitFsElements();
            if (!window.__exitFsObserver) {
                window.__exitFsObserver = new MutationObserver(function() {
                    removeExitFsElements();
                });
                if (document.body) {
                    window.__exitFsObserver.observe(document.body, { childList: true, subtree: true });
                }
            }
        })();
    """.trimIndent()

    // Fullscreen Mode state - When enabled, hides top navigation, search bar, and system status bar
    var isFullScreen by remember { mutableStateOf(false) }

    LaunchedEffect(isFullScreen, webViewInstance) {
        if (isFullScreen) {
            webViewInstance?.evaluateJavascript(removeExitFullscreenMenuJs, null)
        }
    }

    // Immersive Status Bar Control: hides status bar on fullscreen and restores it on exit/dispose.
    // Swiping down from top transiently reveals the status bar without disrupting the fullscreen web page.
    DisposableEffect(isFullScreen, activity) {
        val window = activity?.window
        if (window != null) {
            val insetsController = WindowCompat.getInsetsController(window, window.decorView)
            if (isFullScreen) {
                insetsController.systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                insetsController.hide(WindowInsetsCompat.Type.statusBars())
            } else {
                insetsController.show(WindowInsetsCompat.Type.statusBars())
                insetsController.systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_DEFAULT
            }
        }
        viewModel.setIsFullScreenBrowser(isFullScreen)

        onDispose {
            val w = activity?.window
            if (w != null) {
                val insetsController = WindowCompat.getInsetsController(w, w.decorView)
                insetsController.show(WindowInsetsCompat.Type.statusBars())
                insetsController.systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_DEFAULT
            }
            viewModel.setIsFullScreenBrowser(false)
        }
    }

    // Synchronized Study Focus Timer States (Zero-Distraction Mode)
    val timerIsRunning by viewModel.timerIsRunning.collectAsStateWithLifecycle()
    val timerSecondsElapsed by viewModel.timerSecondsElapsed.collectAsStateWithLifecycle()
    val timerSubject by viewModel.timerSubject.collectAsStateWithLifecycle()
    val timerChapter by viewModel.timerChapter.collectAsStateWithLifecycle()
    var showFocusTimerSheet by remember { mutableStateOf(false) }
    var isStealthTimerMode by remember { mutableStateOf(false) }
    var showCustomChapterDialog by remember { mutableStateOf(false) }
    var customChapterInput by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.syncTimerFromClock()
    }

    fun formatFocusTimer(seconds: Int): String {
        val hrs = seconds / 3600
        val mins = (seconds % 3600) / 60
        val secs = seconds % 60
        return if (hrs > 0) {
            String.format(Locale.getDefault(), "%02d:%02d:%02d", hrs, mins, secs)
        } else {
            String.format(Locale.getDefault(), "%02d:%02d", mins, secs)
        }
    }

    // Focus Shield Distraction Block State (YouTube, Dailymotion, Social Media restriction)
    var blockedDistractionResult by remember { mutableStateOf<StudyPortalGuard.BlockResult?>(null) }

    // AI Scanner States
    var isScanning by remember { mutableStateOf(false) }
    var scanStatusMessage by remember { mutableStateOf("") }
    var scannedResult by remember { mutableStateOf<TestbookScorecardResult?>(null) }
    var showReviewDialog by remember { mutableStateOf(false) }
    var showScanOptionsSheet by remember { mutableStateOf(false) }

    // Download States
    var activeDownload by remember { mutableStateOf<ActiveDownloadInfo?>(null) }
    var downloadedPdfs by remember { mutableStateOf<List<DownloadedPdfRecord>>(emptyList()) }
    var showDownloadsSheet by remember { mutableStateOf(false) }


    // Intercept hardware/system back button so user NEVER accidentally jumps to Dashboard from website
    BackHandler(enabled = true) {
        when {
            showCustomChapterDialog -> showCustomChapterDialog = false
            showDownloadsSheet -> showDownloadsSheet = false
            showFocusTimerSheet -> showFocusTimerSheet = false
            showScanOptionsSheet -> showScanOptionsSheet = false
            showPortalSelectorDialog -> showPortalSelectorDialog = false
            showAddEditPortalDialog -> showAddEditPortalDialog = false
            showReviewDialog -> showReviewDialog = false
            portalToDelete != null -> portalToDelete = null
            isFullScreen -> {
                // Exit fullscreen mode, stay on current website!
                isFullScreen = false
            }
            webViewInstance?.canGoBack() == true -> {
                // Go back within the website history!
                webViewInstance?.goBack()
            }
            else -> onBackToApp()
        }
    }


    // Helper to format bytes
    fun formatBytes(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        val gb = mb / 1024.0
        return when {
            gb >= 1.0 -> String.format(Locale.getDefault(), "%.1f GB", gb)
            mb >= 1.0 -> String.format(Locale.getDefault(), "%.1f MB", mb)
            kb >= 1.0 -> String.format(Locale.getDefault(), "%.1f KB", kb)
            else -> "$bytes B"
        }
    }

    // Helper to open PDF
    fun openPdfFile(file: File) {
        try {
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            viewModel.openPdfInReader(uri, file.name)
            if (onNavigate != null) {
                onNavigate("pdf_viewer")
            } else {
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "application/pdf")
                    flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(Intent.createChooser(intent, "Open PDF with"))
            }
        } catch (e: Exception) {
            Toast.makeText(context, "No PDF viewer app found. Please install a PDF reader.", Toast.LENGTH_LONG).show()
        }
    }

    // Helper to share PDF
    fun sharePdfFile(file: File) {
        try {
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share Testbook PDF"))
        } catch (e: Exception) {
            Toast.makeText(context, "Unable to share file: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // Load existing downloaded PDFs across Downloads/Testbook_PDFs/ and device directories
    fun refreshDownloadedPdfs() {
        coroutineScope.launch(Dispatchers.IO) {
            val candidateDirs = listOfNotNull(
                File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "Testbook_PDFs"),
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "Lakshya_Reports"),
                File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "Testbook_PDFs"),
                context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
                File(context.filesDir, "Testbook_PDFs"),
                context.filesDir
            )

            val list = mutableListOf<DownloadedPdfRecord>()
            candidateDirs.forEach { dir ->
                if (dir.exists() && dir.isDirectory) {
                    dir.listFiles { f -> f.isFile && f.name.endsWith(".pdf", ignoreCase = true) }?.forEach { f ->
                        list.add(
                            DownloadedPdfRecord(
                                fileName = f.name,
                                filePath = f.absolutePath,
                                fileSizeFormatted = formatBytes(f.length()),
                                dateAdded = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(f.lastModified()))
                            )
                        )
                    }
                }
            }
            val distinctList = list.distinctBy { it.filePath }.sortedByDescending { File(it.filePath).lastModified() }
            withContext(Dispatchers.Main) {
                downloadedPdfs = distinctList
            }
        }
    }

    LaunchedEffect(Unit) {
        refreshDownloadedPdfs()
    }

    LaunchedEffect(showDownloadsSheet) {
        if (showDownloadsSheet) {
            refreshDownloadedPdfs()
        }
    }

    // PDF Download Execution
    fun startPdfDownload(
        url: String,
        userAgent: String,
        contentDisposition: String?,
        mimetype: String?,
        contentLength: Long
    ) {
        var rawFileName = URLUtil.guessFileName(url, contentDisposition, mimetype)
        if (rawFileName.isNullOrBlank() || rawFileName == "downloadfile.bin") {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            rawFileName = "Testbook_NEET_Paper_$timestamp.pdf"
        }
        if (!rawFileName.endsWith(".pdf", ignoreCase = true) && (mimetype?.contains("pdf", ignoreCase = true) == true || url.contains(".pdf", ignoreCase = true))) {
            rawFileName += ".pdf"
        }
        val targetFileName = rawFileName

        activeDownload = ActiveDownloadInfo(
            fileName = targetFileName,
            progress = 0.05f,
            downloadedBytes = 0L,
            totalBytes = if (contentLength > 0) contentLength else 0L,
            state = ActiveDownloadState.DOWNLOADING
        )

        Toast.makeText(context, "📥 Downloading $targetFileName...", Toast.LENGTH_SHORT).show()

        coroutineScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    // Safe target file creation across scoped storage / internal / external files
                    val targetFile: File = run {
                        var f: File? = null
                        try {
                            val pubDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "Testbook_PDFs")
                            if (!pubDir.exists()) pubDir.mkdirs()
                            val candidate = File(pubDir, targetFileName)
                            if (candidate.createNewFile() || candidate.exists()) {
                                f = candidate
                            }
                        } catch (_: Exception) {}

                        if (f == null) {
                            try {
                                val extDir = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "Testbook_PDFs").apply { mkdirs() }
                                f = File(extDir, targetFileName)
                            } catch (_: Exception) {
                                val intDir = File(context.filesDir, "Testbook_PDFs").apply { mkdirs() }
                                f = File(intDir, targetFileName)
                            }
                        }
                        f
                    }

                    var curUrl = url
                    var connection: HttpURLConnection? = null
                    var redirectCount = 0

                    while (redirectCount < 5) {
                        val conn = URL(curUrl).openConnection() as HttpURLConnection
                        conn.instanceFollowRedirects = true
                        conn.requestMethod = "GET"
                        conn.setRequestProperty("User-Agent", userAgent)
                        val cookies = CookieManager.getInstance().getCookie(curUrl) ?: CookieManager.getInstance().getCookie(url)
                        if (!cookies.isNullOrEmpty()) {
                            conn.setRequestProperty("Cookie", cookies)
                        }
                        conn.connectTimeout = 15000
                        conn.readTimeout = 30000
                        conn.connect()
                        val code = conn.responseCode
                        if (code in 301..308) {
                            val loc = conn.getHeaderField("Location")
                            conn.disconnect()
                            if (!loc.isNullOrBlank()) {
                                curUrl = if (loc.startsWith("http")) loc else URL(URL(curUrl), loc).toString()
                                redirectCount++
                                continue
                            }
                        }
                        connection = conn
                        break
                    }

                    val finalConn = connection ?: throw Exception("Failed to connect")

                    if (finalConn.responseCode in 200..299) {
                        val fileLength = finalConn.contentLengthLong.takeIf { it > 0 } ?: contentLength
                        val input = finalConn.inputStream
                        val output = FileOutputStream(targetFile)
                        val data = ByteArray(8192)
                        var total: Long = 0
                        var count: Int

                        while (input.read(data).also { count = it } != -1) {
                            total += count.toLong()
                            output.write(data, 0, count)

                            val progressVal = if (fileLength > 0) {
                                (total.toFloat() / fileLength).coerceIn(0f, 1f)
                            } else {
                                0.5f
                            }

                            withContext(Dispatchers.Main) {
                                activeDownload = ActiveDownloadInfo(
                                    fileName = targetFileName,
                                    progress = progressVal,
                                    downloadedBytes = total,
                                    totalBytes = fileLength,
                                    state = ActiveDownloadState.DOWNLOADING,
                                    filePath = targetFile.absolutePath
                                )
                            }
                        }

                        output.flush()
                        output.close()
                        input.close()
                        finalConn.disconnect()

                        // Notify MediaScanner so the PDF is immediately visible in Android's Files/Downloads app
                        MediaScannerConnection.scanFile(
                            context,
                            arrayOf(targetFile.absolutePath),
                            arrayOf(mimetype ?: "application/pdf"),
                            null
                        )

                        val newRecord = DownloadedPdfRecord(
                            fileName = targetFileName,
                            filePath = targetFile.absolutePath,
                            fileSizeFormatted = formatBytes(targetFile.length())
                        )

                        withContext(Dispatchers.Main) {
                            activeDownload = ActiveDownloadInfo(
                                fileName = targetFileName,
                                progress = 1f,
                                downloadedBytes = total,
                                totalBytes = total,
                                state = ActiveDownloadState.COMPLETED,
                                filePath = targetFile.absolutePath
                            )
                            downloadedPdfs = (listOf(newRecord) + downloadedPdfs).distinctBy { it.filePath }
                            Toast.makeText(context, "✅ Download Complete: $targetFileName", Toast.LENGTH_LONG).show()
                        }
                    } else {
                        // Attempt DownloadManager fallback for protected / session downloads
                        try {
                            val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                            val dmReq = DownloadManager.Request(Uri.parse(url)).apply {
                                setTitle(targetFileName)
                                setDescription("Downloading study material...")
                                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                                setMimeType(mimetype ?: "application/pdf")
                                addRequestHeader("User-Agent", userAgent)
                                val c = CookieManager.getInstance().getCookie(url)
                                if (!c.isNullOrBlank()) {
                                    addRequestHeader("Cookie", c)
                                }
                                try {
                                    setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "Testbook_PDFs/$targetFileName")
                                } catch (_: Exception) {
                                    setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, targetFileName)
                                }
                            }
                            dm.enqueue(dmReq)
                            withContext(Dispatchers.Main) {
                                activeDownload = ActiveDownloadInfo(
                                    fileName = targetFileName,
                                    progress = 1f,
                                    downloadedBytes = 0L,
                                    totalBytes = 0L,
                                    state = ActiveDownloadState.COMPLETED,
                                    filePath = targetFile.absolutePath
                                )
                                Toast.makeText(context, "📥 Download queued in Download Manager: $targetFileName", Toast.LENGTH_LONG).show()
                            }
                        } catch (dmEx: Exception) {
                            throw Exception("Server returned HTTP ${finalConn.responseCode}")
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    // Final fallback to DownloadManager if direct connection errored
                    var dmSuccess = false
                    try {
                        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                        val dmReq = DownloadManager.Request(Uri.parse(url)).apply {
                            setTitle(targetFileName)
                            setDescription("Downloading file...")
                            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                            setMimeType(mimetype ?: "application/pdf")
                            addRequestHeader("User-Agent", userAgent)
                            val c = CookieManager.getInstance().getCookie(url)
                            if (!c.isNullOrBlank()) {
                                addRequestHeader("Cookie", c)
                            }
                            try {
                                setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "Testbook_PDFs/$targetFileName")
                            } catch (_: Exception) {
                                setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, targetFileName)
                            }
                        }
                        dm.enqueue(dmReq)
                        dmSuccess = true
                    } catch (_: Exception) {}

                    withContext(Dispatchers.Main) {
                        if (dmSuccess) {
                            activeDownload = ActiveDownloadInfo(
                                fileName = targetFileName,
                                progress = 1f,
                                downloadedBytes = 0L,
                                totalBytes = 0L,
                                state = ActiveDownloadState.COMPLETED,
                                filePath = null
                            )
                            Toast.makeText(context, "📥 Download queued in Download Manager!", Toast.LENGTH_SHORT).show()
                        } else {
                            activeDownload = activeDownload?.copy(
                                state = ActiveDownloadState.FAILED,
                                errorMessage = e.localizedMessage ?: "Download failed"
                            )
                            Toast.makeText(context, "Download failed: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        }
    }

    // Image Picker Launcher for gallery scorecards
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            isScanning = true
            scanStatusMessage = "Analyzing Scorecard from top to bottom with Lakshya AI Vision..."
            coroutineScope.launch {
                val res = GeminiChatAssistant.scanTestbookScorecardWithGeminiVision(
                    context = context,
                    imageBytes = null,
                    imageUriStr = uri.toString()
                )
                isScanning = false
                if (res.isSuccess) {
                    scannedResult = res.getOrNull()
                    showReviewDialog = true
                } else {
                    Toast.makeText(
                        context,
                        "Scan Failed: ${res.exceptionOrNull()?.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    // Wrong Question Extraction States
    var extractedMistakesResult by remember { mutableStateOf<com.example.data.TestbookMistakeExtractionResult?>(null) }
    var showMistakesImportDialog by remember { mutableStateOf(false) }

    val batchQuestionPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            isScanning = true
            scanStatusMessage = "Analyzing ${uris.size} question screenshot(s) with Lakshya AI Vision..."
            coroutineScope.launch {
                val bytesList = withContext(Dispatchers.IO) {
                    uris.mapNotNull { uri ->
                        try {
                            context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                        } catch (e: Exception) {
                            null
                        }
                    }
                }

                if (bytesList.isEmpty()) {
                    isScanning = false
                    Toast.makeText(context, "Could not read selected screenshots", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                val res = GeminiChatAssistant.extractMistakesFromTestbookScreenshots(
                    context = context,
                    imagesBytesList = bytesList
                )
                isScanning = false
                if (res.isSuccess) {
                    val data = res.getOrNull()
                    if (data != null && data.questions.isNotEmpty()) {
                        extractedMistakesResult = data
                        showMistakesImportDialog = true
                    } else {
                        Toast.makeText(context, "No incorrect questions could be extracted", Toast.LENGTH_LONG).show()
                    }
                } else {
                    Toast.makeText(context, "Mistake Extraction Failed: ${res.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // Helper to capture Visible Viewport
    fun captureVisibleBitmap(): Bitmap? {
        val wv = webViewInstance ?: return null
        return try {
            val width = wv.width
            val height = wv.height
            if (width <= 0 || height <= 0) return null
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            wv.draw(canvas)
            bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun executeSingleScreenMistakeExtraction() {
        val bmp = captureVisibleBitmap()
        if (bmp == null) {
            Toast.makeText(context, "Please navigate to the incorrect question first", Toast.LENGTH_SHORT).show()
            return
        }
        isScanning = true
        scanStatusMessage = "Extracting current question, mistake type & options with Lakshya AI Vision..."

        coroutineScope.launch {
            val bytes = withContext(Dispatchers.Default) {
                val stream = ByteArrayOutputStream()
                bmp.compress(Bitmap.CompressFormat.JPEG, 85, stream)
                stream.toByteArray()
            }

            val res = GeminiChatAssistant.extractMistakesFromTestbookScreenshots(
                context = context,
                imagesBytesList = listOf(bytes)
            )
            isScanning = false
            if (res.isSuccess) {
                val data = res.getOrNull()
                if (data != null && data.questions.isNotEmpty()) {
                    extractedMistakesResult = data
                    showMistakesImportDialog = true
                } else {
                    Toast.makeText(context, "No question found on current display", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(context, "Mistake Extraction Failed: ${res.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    // Helper to capture Entire Webpage from Top to Bottom
    fun captureFullPageBitmap(): Bitmap? {
        val wv = webViewInstance ?: return null
        return try {
            val width = wv.width
            if (width <= 0) return null

            val density = wv.context.resources.displayMetrics.density
            val contentHeightPx = (wv.contentHeight * density).toInt()
            val targetHeight = if (contentHeightPx > wv.height) {
                contentHeightPx.coerceIn(wv.height, 6500)
            } else {
                wv.height
            }

            val bitmap = Bitmap.createBitmap(width, targetHeight, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            wv.draw(canvas)
            bitmap
        } catch (e: OutOfMemoryError) {
            captureVisibleBitmap()
        } catch (e: Exception) {
            captureVisibleBitmap()
        }
    }

    fun executeScan(fullPage: Boolean) {
        val bmp = if (fullPage) captureFullPageBitmap() else captureVisibleBitmap()
        if (bmp == null) {
            Toast.makeText(context, "Please wait for page to render or upload screenshot directly", Toast.LENGTH_SHORT).show()
            return
        }

        isScanning = true
        scanStatusMessage = if (fullPage) {
            "Scanning entire page top-to-bottom with Lakshya AI Vision OCR..."
        } else {
            "Scanning current view with Lakshya AI Vision OCR..."
        }

        coroutineScope.launch {
            val bytes = withContext(Dispatchers.Default) {
                val stream = ByteArrayOutputStream()
                bmp.compress(Bitmap.CompressFormat.JPEG, 85, stream)
                stream.toByteArray()
            }

            val res = GeminiChatAssistant.scanTestbookScorecardWithGeminiVision(
                context = context,
                imageBytes = bytes,
                imageUriStr = null
            )
            isScanning = false
            if (res.isSuccess) {
                scannedResult = res.getOrNull()
                showReviewDialog = true
            } else {
                Toast.makeText(
                    context,
                    "OCR Scan Error: ${res.exceptionOrNull()?.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            if (!isFullScreen) {
                // Sleek, Compact Top Navigation Toolbar with settled AI Scanner Button
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = if (isDark) Color(0xFF0F172A) else Color.White,
                    shadowElevation = 6.dp
                ) {
                    Column(modifier = Modifier.statusBarsPadding()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Back to App
                            IconButton(onClick = onBackToApp, modifier = Modifier.size(36.dp)) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back to App",
                                    tint = if (isDark) Color.White else Color(0xFF1E293B)
                                )
                            }

                            // Web Back / Forward
                            IconButton(
                                onClick = { webViewInstance?.goBack() },
                                enabled = canGoBack,
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(
                                    Icons.Default.ChevronLeft,
                                    contentDescription = "Web Back",
                                    tint = if (canGoBack) (if (isDark) Color.White else Color(0xFF1E293B)) else Color.Gray.copy(alpha = 0.3f)
                                )
                            }

                            IconButton(
                                onClick = { webViewInstance?.goForward() },
                                enabled = canGoForward,
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(
                                    Icons.Default.ChevronRight,
                                    contentDescription = "Web Forward",
                                    tint = if (canGoForward) (if (isDark) Color.White else Color(0xFF1E293B)) else Color.Gray.copy(alpha = 0.3f)
                                )
                            }

                            IconButton(
                                onClick = { webViewInstance?.reload() },
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(
                                    Icons.Default.Refresh,
                                    contentDescription = "Reload",
                                    tint = if (isDark) Color.White else Color(0xFF1E293B)
                                )
                            }

                            Spacer(modifier = Modifier.width(4.dp))

                            // URL Indicator Box (Interactive Portal Selector Pill)
                            val activePortal = remember(currentUrl, studyPortals) {
                                studyPortals.find { p ->
                                    val domain = p.url.removePrefix("https://").removePrefix("http://").trimEnd('/').split('/')[0]
                                    currentUrl.contains(domain, ignoreCase = true)
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9))
                                    .clickable { showPortalSelectorDialog = true }
                                    .padding(horizontal = 8.dp, vertical = 5.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (activePortal != null) {
                                        Text(activePortal.iconEmoji.ifBlank { "🌐" }, fontSize = 12.sp)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = activePortal.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.5.sp,
                                            color = if (isDark) Color.White else Color(0xFF1E293B),
                                            maxLines = 1
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(
                                            text = "• " + currentUrl.removePrefix("https://").removePrefix("http://").take(18) + (if (currentUrl.length > 22) "..." else ""),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                                            maxLines = 1,
                                            fontSize = 10.sp
                                        )
                                    } else {
                                        Icon(
                                            Icons.Default.Lock,
                                            contentDescription = "Secure",
                                            tint = if (isDark) Color(0xFF10B981) else Color(0xFF059669),
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = currentUrl.removePrefix("https://").removePrefix("http://").take(26) + if (currentUrl.length > 26) "..." else "",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                                            maxLines = 1,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.width(4.dp))

                            // Synced Study Focus Timer Capsule (Zero-Distraction, Live Sync with Room Study Logs)
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (timerIsRunning) {
                                    if (isDark) Color(0xFF1E293B) else Color(0xFFEEF2FF)
                                } else {
                                    if (isDark) Color(0xFF1E293B).copy(alpha = 0.6f) else Color(0xFFF1F5F9)
                                },
                                border = BorderStroke(
                                    1.2.dp,
                                    if (timerIsRunning) {
                                        Brush.horizontalGradient(listOf(Color(0xFF10B981), Color(0xFF6366F1)))
                                    } else {
                                        Brush.horizontalGradient(listOf(if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1), if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1)))
                                    }
                                ),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { showFocusTimerSheet = true }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    if (timerIsRunning) {
                                        val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                                        val pulseAlpha by infiniteTransition.animateFloat(
                                            initialValue = 0.35f,
                                            targetValue = 1f,
                                            animationSpec = infiniteRepeatable(
                                                animation = tween(800, easing = LinearEasing),
                                                repeatMode = RepeatMode.Reverse
                                            ),
                                            label = "pulse_alpha"
                                        )
                                        Box(
                                            modifier = Modifier
                                                .size(7.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFF10B981).copy(alpha = pulseAlpha))
                                        )
                                    } else {
                                        Icon(
                                            Icons.Default.Timer,
                                            contentDescription = "Focus Timer",
                                            tint = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                                            modifier = Modifier.size(13.dp)
                                        )
                                    }

                                    Text(
                                        text = if (isStealthTimerMode && timerIsRunning) "● Active" else formatFocusTimer(timerSecondsElapsed),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (timerIsRunning) (if (isDark) Color(0xFF34D399) else Color(0xFF059669)) else (if (isDark) Color.White else Color(0xFF334155))
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(4.dp))

                            // Web Dark Mode Switcher Button
                            IconButton(
                                onClick = {
                                    isWebDarkMode = !isWebDarkMode
                                    applyWebTheme(webViewInstance, isWebDarkMode)
                                    Toast.makeText(
                                        context,
                                        if (isWebDarkMode) "🌙 Website Dark Mode: ON" else "☀️ Website Light Mode: ON",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                },
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(
                                    imageVector = if (isWebDarkMode) Icons.Default.DarkMode else Icons.Default.LightMode,
                                    contentDescription = "Website Dark Mode",
                                    tint = if (isWebDarkMode) Color(0xFFFBBF24) else (if (isDark) Color.White else Color(0xFF1E293B))
                                )
                            }

                            Spacer(modifier = Modifier.width(2.dp))

                            // Fullscreen Button (Hide Search bar / Top bar during test)
                            IconButton(
                                onClick = {
                                    isFullScreen = true
                                    Toast.makeText(context, "⛶ Fullscreen Mode Enabled! Press or swipe Back to exit.", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(
                                    Icons.Default.Fullscreen,
                                    contentDescription = "Fullscreen Test Mode",
                                    tint = Color(0xFF6366F1)
                                )
                            }

                            Spacer(modifier = Modifier.width(2.dp))

                            // Downloaded PDFs Library Button
                            IconButton(
                                onClick = { showDownloadsSheet = true },
                                modifier = Modifier.size(34.dp)
                            ) {
                                BadgedBox(
                                    badge = {
                                        if (downloadedPdfs.isNotEmpty()) {
                                            Badge(
                                                containerColor = Color(0xFF10B981),
                                                contentColor = Color.White
                                            ) {
                                                Text("${downloadedPdfs.size}", fontSize = 9.sp)
                                            }
                                        }
                                    }
                                ) {
                                    Icon(
                                        Icons.Default.Download,
                                        contentDescription = "Downloaded PDFs",
                                        tint = if (isDark) Color.White else Color(0xFF1E293B)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(2.dp))

                            // SETTLED HOT AI SCANNER BUTTON IN TOP BAR (Zero Distraction on Web Page)
                            Button(
                                onClick = { showScanOptionsSheet = true },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isDark) Color(0xFF1E1B4B) else Color(0xFFEEF2FF)
                                ),
                                shape = RoundedCornerShape(10.dp),
                                border = ButtonDefaults.outlinedButtonBorder().copy(
                                    brush = Brush.linearGradient(
                                        listOf(Color(0xFF818CF8), Color(0xFFF472B6), Color(0xFF38BDF8))
                                    ),
                                    width = 1.2.dp
                                ),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                modifier = Modifier.heightIn(min = 34.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("⚡", fontSize = 12.sp)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        "Scan OCR",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black,
                                        color = if (isDark) Color.White else Color(0xFF4F46E5),
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(2.dp))

                            // More / Desktop Mode Menu
                            var showMenu by remember { mutableStateOf(false) }
                            IconButton(onClick = { showMenu = true }, modifier = Modifier.size(34.dp)) {
                                Icon(
                                    Icons.Default.MoreVert,
                                    contentDescription = "More Options",
                                    tint = if (isDark) Color.White else Color(0xFF1E293B)
                                )
                            }

                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false },
                                modifier = Modifier.background(if (isDark) Color(0xFF1E293B) else Color.White)
                            ) {
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                "📥 Downloaded PDFs Storage",
                                                color = if (isDark) Color.White else Color.Black,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp
                                            )
                                        }
                                    },
                                    onClick = {
                                        showDownloadsSheet = true
                                        showMenu = false
                                    }
                                )

                                HorizontalDivider(color = if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0))



                                 // Web Dark Mode Switcher in Menu
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                if (isWebDarkMode) "🌙 Website Dark Theme (ON)" else "☀️ Website Light Theme (ON)",
                                                color = if (isDark) Color.White else Color.Black,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp
                                            )
                                        }
                                    },
                                    onClick = {
                                        isWebDarkMode = !isWebDarkMode
                                        applyWebTheme(webViewInstance, isWebDarkMode)
                                        showMenu = false
                                        Toast.makeText(
                                            context,
                                            if (isWebDarkMode) "🌙 Dark Mode applied to Website" else "☀️ Light Mode applied to Website",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                )

                                HorizontalDivider(color = if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0))

                                // Desktop Mode Switcher
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                if (isDesktopMode) "💻 Desktop Mode (Laptop View: ON)" else "📱 Mobile Mode (ON)",
                                                color = if (isDark) Color.White else Color.Black,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp
                                            )
                                        }
                                    },
                                    onClick = {
                                        isDesktopMode = !isDesktopMode
                                        val currentUa = webViewInstance?.settings?.userAgentString ?: ""; val cleanUa = currentUa.replace("; wv", "").replace("Version/4.0 ", ""); webViewInstance?.settings?.userAgentString = if (isDesktopMode) DESKTOP_USER_AGENT else (if (cleanUa.contains("Mozilla")) cleanUa else currentUa)
                                        webViewInstance?.reload()
                                        showMenu = false
                                        Toast.makeText(
                                            context,
                                            if (isDesktopMode) "Switched to Laptop / Desktop View" else "Switched to Mobile View",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                )

                                HorizontalDivider(color = if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0))

                                // Saved Portals Header
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                "🌐 SAVED STUDY PORTALS",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Black,
                                                color = if (isDark) Color(0xFFA5B4FC) else Color(0xFF4F46E5),
                                                letterSpacing = 0.5.sp
                                            )
                                            Text(
                                                "Manage ⚙️",
                                                fontSize = 10.sp,
                                                color = if (isDark) Color(0xFF818CF8) else Color(0xFF6366F1),
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    },
                                    onClick = {
                                        showPortalSelectorDialog = true
                                        showMenu = false
                                    }
                                )

                                studyPortals.forEach { portal ->
                                    DropdownMenuItem(
                                        text = {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Text(portal.iconEmoji.ifBlank { "🌐" }, fontSize = 13.sp)
                                                Text(
                                                    portal.name,
                                                    color = if (isDark) Color.White else Color.Black,
                                                    fontWeight = FontWeight.SemiBold,
                                                    fontSize = 12.5.sp,
                                                    maxLines = 1
                                                )
                                                Surface(
                                                    color = Color(0xFF6366F1).copy(alpha = 0.15f),
                                                    shape = RoundedCornerShape(3.dp)
                                                ) {
                                                    Text(
                                                        portal.tag,
                                                        fontSize = 8.sp,
                                                        color = if (isDark) Color(0xFFA5B4FC) else Color(0xFF4F46E5),
                                                        fontWeight = FontWeight.Bold,
                                                        modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp)
                                                    )
                                                }
                                            }
                                        },
                                        onClick = {
                                            webViewInstance?.loadUrl(portal.url)
                                            showMenu = false
                                        }
                                    )
                                }

                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            "➕ Add / Manage Study Portals...",
                                            color = if (isDark) Color(0xFF818CF8) else Color(0xFF4F46E5),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                    },
                                    onClick = {
                                        showPortalSelectorDialog = true
                                        showMenu = false
                                    }
                                )
                                HorizontalDivider(color = if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0))
                                DropdownMenuItem(
                                    text = { Text("🔄 Hard Reload Page", color = if (isDark) Color(0xFF818CF8) else Color(0xFF4F46E5), fontWeight = FontWeight.SemiBold) },
                                    onClick = {
                                        webViewInstance?.reload()
                                        showMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("🧹 Clear Cache & Fix Blank Screen", color = Color(0xFFEF4444), fontWeight = FontWeight.SemiBold) },
                                    onClick = {
                                        webViewInstance?.clearCache(true)
                                        webViewInstance?.clearHistory()
                                        webViewInstance?.reload()
                                        showMenu = false
                                        Toast.makeText(context, "Browser cache cleared & refreshed!", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }
                        }

                        // Web Loading Progress Bar
                        if (webProgress > 0f && webProgress < 1f) {
                            FireworksProgressBar(
                                progress = webProgress,
                                height = 3.dp,
                                gradientColors = listOf(Color(0xFF6366F1), Color(0xFF818CF8), Color(0xFFC084FC)),
                                sparkColor = Color(0xFF818CF8),
                                isDark = isDark,
                                trackColor = Color.Transparent
                            )
                        }

                        // Quick Study Portal Selector Strip (1-tap instant portal launch)
                        if (!isFullScreen && studyPortals.isNotEmpty()) {
                            LazyRow(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(if (isDark) Color(0xFF0F172A) else Color(0xFFF1F5F9))
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                items(studyPortals) { portal ->
                                    val portalDomain = portal.url.removePrefix("https://").removePrefix("http://").trimEnd('/').split('/')[0]
                                    val isCurrentPortal = currentUrl.contains(portalDomain, ignoreCase = true)
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isCurrentPortal) {
                                            Color(0xFF6366F1).copy(alpha = if (isDark) 0.35f else 0.2f)
                                        } else if (isDark) {
                                            Color(0xFF1E293B)
                                        } else {
                                            Color.White
                                        },
                                        border = BorderStroke(
                                            if (isCurrentPortal) 1.5.dp else 0.8.dp,
                                            if (isCurrentPortal) Color(0xFF818CF8) else if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)
                                        ),
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable {
                                                if (!isCurrentPortal) {
                                                    webViewInstance?.loadUrl(portal.url)
                                                }
                                            }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(portal.iconEmoji.ifBlank { "🌐" }, fontSize = 11.sp)
                                            Text(
                                                text = portal.name,
                                                fontSize = 11.sp,
                                                fontWeight = if (isCurrentPortal) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isCurrentPortal) {
                                                    if (isDark) Color(0xFFA5B4FC) else Color(0xFF4338CA)
                                                } else {
                                                    if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569)
                                                }
                                            )
                                            if (isCurrentPortal) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(5.dp)
                                                        .clip(CircleShape)
                                                        .background(Color(0xFF10B981))
                                                )
                                            }
                                        }
                                    }
                                }
                                item {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isDark) Color(0x1F6366F1) else Color(0xFFEEF2FF),
                                        border = BorderStroke(0.8.dp, Color(0xFF6366F1).copy(alpha = 0.5f)),
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable { showPortalSelectorDialog = true }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Add,
                                                contentDescription = "Manage Portals",
                                                tint = Color(0xFF6366F1),
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Text(
                                                "Portals",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF6366F1)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(if (isFullScreen) PaddingValues(0.dp) else paddingValues)
        ) {
            // Fullscreen Android WebView configured with responsive touch and SPA compatibility
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    WebView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        isFocusable = true
                        isFocusableInTouchMode = true
                        isClickable = true
                        setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null)

                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            javaScriptCanOpenWindowsAutomatically = false
                            setSupportZoom(true)
                            builtInZoomControls = true
                            displayZoomControls = false
                            loadWithOverviewMode = true
                            useWideViewPort = true
                            allowFileAccess = true
                            allowContentAccess = true
                            databaseEnabled = true
                            cacheMode = WebSettings.LOAD_DEFAULT
                            val defaultUa = userAgentString
                            val cleanMobileUa = defaultUa.replace("; wv", "").replace("Version/4.0 ", "")
                            userAgentString = if (isDesktopMode) DESKTOP_USER_AGENT else cleanMobileUa
                            setSupportMultipleWindows(true)
                            mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                            mediaPlaybackRequiresUserGesture = false
                            setGeolocationEnabled(false)
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                safeBrowsingEnabled = true
                            }
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                isAlgorithmicDarkeningAllowed = false
                            }
                        }

                        val cookieMgr = CookieManager.getInstance()
                        cookieMgr.setAcceptCookie(true)
                        cookieMgr.setAcceptThirdPartyCookies(this, true)
                        cookieMgr.flush()

                        addJavascriptInterface(BlobDownloaderInterface { base64Data, mimeType ->
                            val pureBase64 = base64Data.replaceFirst("^data:.*;base64,".toRegex(), "")
                            val bytes = android.util.Base64.decode(pureBase64, android.util.Base64.DEFAULT)
                            val timestamp = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault()).format(java.util.Date())
                            val targetFileName = "PW_Blob_Download_$timestamp.pdf"
                            
                            coroutineScope.launch {
                                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                    activeDownload = ActiveDownloadInfo(
                                        fileName = targetFileName,
                                        progress = 0.5f,
                                        downloadedBytes = 0L,
                                        totalBytes = bytes.size.toLong(),
                                        state = ActiveDownloadState.DOWNLOADING
                                    )
                                    android.widget.Toast.makeText(context, "📥 Processing Blob PDF...", android.widget.Toast.LENGTH_SHORT).show()
                                }
                                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                    try {
                                        val targetFile: java.io.File = run {
                                            var f: java.io.File? = null
                                            try {
                                                val pubDir = java.io.File(
                                                    android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS),
                                                    "Testbook_PDFs"
                                                )
                                                if (!pubDir.exists()) pubDir.mkdirs()
                                                val candidate = java.io.File(pubDir, targetFileName)
                                                if (candidate.createNewFile() || candidate.exists()) {
                                                    f = candidate
                                                }
                                            } catch (_: Exception) {}

                                            if (f == null) {
                                                try {
                                                    val extDir = java.io.File(context.getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS), "Testbook_PDFs").apply { mkdirs() }
                                                    f = java.io.File(extDir, targetFileName)
                                                } catch (_: Exception) {
                                                    val intDir = java.io.File(context.filesDir, "Testbook_PDFs").apply { mkdirs() }
                                                    f = java.io.File(intDir, targetFileName)
                                                }
                                            }
                                            f
                                        }

                                        val output = java.io.FileOutputStream(targetFile)
                                        output.write(bytes)
                                        output.flush()
                                        output.close()
                                        
                                        android.media.MediaScannerConnection.scanFile(
                                            context,
                                            arrayOf(targetFile.absolutePath),
                                            arrayOf(mimeType.ifBlank { "application/pdf" }),
                                            null
                                        )
                                        
                                        val newRecord = DownloadedPdfRecord(
                                            fileName = targetFileName,
                                            filePath = targetFile.absolutePath,
                                            fileSizeFormatted = formatBytes(targetFile.length())
                                        )
                                        
                                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                            activeDownload = ActiveDownloadInfo(
                                                fileName = targetFileName,
                                                progress = 1f,
                                                downloadedBytes = bytes.size.toLong(),
                                                totalBytes = bytes.size.toLong(),
                                                state = ActiveDownloadState.COMPLETED,
                                                filePath = targetFile.absolutePath
                                            )
                                            downloadedPdfs = (listOf(newRecord) + downloadedPdfs).distinctBy { it.filePath }
                                            android.widget.Toast.makeText(context, "✅ Download Complete: $targetFileName", android.widget.Toast.LENGTH_LONG).show()
                                        }
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                            activeDownload = activeDownload?.copy(
                                                state = ActiveDownloadState.FAILED,
                                                errorMessage = e.localizedMessage ?: "Download failed"
                                            )
                                            android.widget.Toast.makeText(context, "❌ Blob Download Error: ${e.localizedMessage}", android.widget.Toast.LENGTH_LONG).show()
                                        }
                                    }
                                }
                            }
                        }, "AndroidBlobDownloader")

                        // Automatic Testbook / PW PDF Download Interception
                        setDownloadListener { url, userAgent, contentDisposition, mimetype, contentLength ->
                            if (url.startsWith("blob:")) {
                                val script = """
                                    javascript:(function() {
                                        var xhr = new XMLHttpRequest();
                                        xhr.open('GET', '$url', true);
                                        xhr.responseType = 'blob';
                                        xhr.onload = function() {
                                            if (this.status == 200) {
                                                var blob = this.response;
                                                var reader = new FileReader();
                                                reader.readAsDataURL(blob);
                                                reader.onloadend = function() {
                                                    AndroidBlobDownloader.getBase64FromBlobData(reader.result, '$mimetype');
                                                }
                                            }
                                        };
                                        xhr.send();
                                    })();
                                """.trimIndent()
                                evaluateJavascript(script, null)
                            } else {
                                startPdfDownload(url, userAgent, contentDisposition, mimetype, contentLength)
                            }
                        }

                        webViewClient = object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(
                                view: WebView?,
                                request: WebResourceRequest?
                            ): Boolean {
                                val url = request?.url?.toString() ?: ""
                                val scheme = request?.url?.scheme ?: ""

                                // 1. Strict Focus Shield: Check and block YouTube, Dailymotion, Social & Entertainment platforms
                                val guardCheck = StudyPortalGuard.checkUrl(url)
                                if (guardCheck.isBlocked) {
                                    blockedDistractionResult = guardCheck
                                    Toast.makeText(context, guardCheck.blockReason, Toast.LENGTH_LONG).show()
                                    return true
                                }

                                // Prevent bouncing out to Play Store or external intents unless requested
                                if (scheme == "intent" || scheme == "market" || scheme.startsWith("testbook")) {
                                    try {
                                        val intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME)
                                        if (intent != null) {
                                            val fallbackUrl = intent.getStringExtra("browser_fallback_url")
                                            if (fallbackUrl != null) {
                                                val fallbackGuard = StudyPortalGuard.checkUrl(fallbackUrl)
                                                if (fallbackGuard.isBlocked) {
                                                    blockedDistractionResult = fallbackGuard
                                                    Toast.makeText(context, fallbackGuard.blockReason, Toast.LENGTH_LONG).show()
                                                    return true
                                                }
                                                view?.loadUrl(fallbackUrl)
                                                return true
                                            }
                                        }
                                    } catch (e: Exception) {
                                        // Ignore
                                    }
                                    return true
                                }
                                // If user clicked a direct PDF link or download endpoint
                                if (url.endsWith(".pdf", ignoreCase = true) || url.contains(".pdf?") || url.contains("/download")) {
                                    val userAgent = view?.settings?.userAgentString ?: (if (isDesktopMode) DESKTOP_USER_AGENT else MOBILE_USER_AGENT)
                                    startPdfDownload(url, userAgent, null, "application/pdf", -1L)
                                    return true
                                }
                                if (url.startsWith("http://") || url.startsWith("https://")) {
                                    blockedDistractionResult = null
                                    return false
                                }
                                return true
                            }

                            override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                                super.onPageStarted(view, url, favicon)
                                if (url != null) {
                                    val guardCheck = StudyPortalGuard.checkUrl(url)
                                    if (guardCheck.isBlocked) {
                                        view?.stopLoading()
                                        view?.loadUrl("about:blank")
                                        blockedDistractionResult = guardCheck
                                        Toast.makeText(context, guardCheck.blockReason, Toast.LENGTH_LONG).show()
                                        return
                                    }
                                }
                                blockedDistractionResult = null
                            }

                            override fun onReceivedSslError(
                                view: WebView?,
                                handler: SslErrorHandler?,
                                error: android.net.http.SslError?
                            ) {
                                // Default behavior is to cancel the connection for SSL errors to prevent MITM
                                super.onReceivedSslError(view, handler, error)
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                if (url != null) {
                                    val guardCheck = StudyPortalGuard.checkUrl(url)
                                    if (guardCheck.isBlocked) {
                                        view?.stopLoading()
                                        view?.loadUrl("about:blank")
                                        blockedDistractionResult = guardCheck
                                        return
                                    }
                                }
                                currentUrl = url ?: currentUrl
                                canGoBack = view?.canGoBack() ?: false
                                canGoForward = view?.canGoForward() ?: false
                                if (isWebDarkMode) {
                                    applyWebTheme(view, true)
                                }
                                if (isFullScreen) {
                                    view?.evaluateJavascript(removeExitFullscreenMenuJs, null)
                                }
                            }

                            override fun onRenderProcessGone(view: WebView?, detail: android.webkit.RenderProcessGoneDetail?): Boolean {
                                try { view?.destroy() } catch (e: Exception) {}
                                return true
                            }

                            override fun onReceivedError(
                                view: WebView?,
                                request: WebResourceRequest?,
                                error: android.webkit.WebResourceError?
                            ) {
                                super.onReceivedError(view, request, error)
                            }
                        }

                        webChromeClient = object : WebChromeClient() {
                            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                super.onProgressChanged(view, newProgress)
                                webProgress = newProgress / 100f
                            }

                            override fun onShowFileChooser(
                                webView: WebView?,
                                filePathCallback: ValueCallback<Array<Uri>>?,
                                fileChooserParams: FileChooserParams?
                            ): Boolean {
                                fileUploadCallback?.onReceiveValue(null)
                                fileUploadCallback = filePathCallback

                                try {
                                    val intent = fileChooserParams?.createIntent() ?: Intent(Intent.ACTION_GET_CONTENT).apply {
                                        addCategory(Intent.CATEGORY_OPENABLE)
                                        type = "*/*"
                                    }
                                    if (fileChooserParams?.mode == FileChooserParams.MODE_OPEN_MULTIPLE) {
                                        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                                    }
                                    fileChooserLauncher.launch(intent)
                                    return true
                                } catch (e: Exception) {
                                    try {
                                        val fallbackIntent = Intent(Intent.ACTION_GET_CONTENT).apply {
                                            addCategory(Intent.CATEGORY_OPENABLE)
                                            type = "*/*"
                                            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                                        }
                                        fileChooserLauncher.launch(fallbackIntent)
                                        return true
                                    } catch (ex: Exception) {
                                        fileUploadCallback?.onReceiveValue(null)
                                        fileUploadCallback = null
                                        return false
                                    }
                                }
                            }

                            override fun onCreateWindow(
                                view: WebView?,
                                isDialog: Boolean,
                                isUserGesture: Boolean,
                                resultMsg: android.os.Message?
                            ): Boolean {
                                val transport = resultMsg?.obj as? WebView.WebViewTransport
                                val tempWebView = WebView(view?.context ?: return false).apply {
                                    settings.apply {
                                        javaScriptEnabled = true
                                        domStorageEnabled = true
                                        databaseEnabled = true
                                        allowFileAccess = true
                                        allowContentAccess = true
                                        mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                                        javaScriptCanOpenWindowsAutomatically = false
                                        userAgentString = view.settings.userAgentString
                                    }
                                    CookieManager.getInstance().setAcceptCookie(true)
                                    CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                                    webChromeClient = object : WebChromeClient() {
                                        override fun onShowFileChooser(
                                            wView: WebView?,
                                            fPathCallback: ValueCallback<Array<Uri>>?,
                                            fChooserParams: FileChooserParams?
                                        ): Boolean {
                                            fileUploadCallback?.onReceiveValue(null)
                                            fileUploadCallback = fPathCallback
                                            try {
                                                val intent = fChooserParams?.createIntent() ?: Intent(Intent.ACTION_GET_CONTENT).apply {
                                                    addCategory(Intent.CATEGORY_OPENABLE)
                                                    type = "*/*"
                                                }
                                                if (fChooserParams?.mode == FileChooserParams.MODE_OPEN_MULTIPLE) {
                                                    intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                                                }
                                                fileChooserLauncher.launch(intent)
                                                return true
                                            } catch (e: Exception) {
                                                fileUploadCallback?.onReceiveValue(null)
                                                fileUploadCallback = null
                                                return false
                                            }
                                        }

                                        override fun onPermissionRequest(request: PermissionRequest?) {
                                            request?.grant(request.resources)
                                        }
                                    }
                                    webViewClient = object : WebViewClient() {
                                        override fun shouldOverrideUrlLoading(
                                            innerView: WebView?,
                                            request: WebResourceRequest?
                                        ): Boolean {
                                            val targetUrl = request?.url?.toString() ?: ""
                                            if (targetUrl.isNotBlank()) {
                                                if (!StudyPortalGuard.isAllowedSchemeAndUrl(targetUrl)) {
                                                    val guardCheck = StudyPortalGuard.checkUrl(targetUrl)
                                                    if (guardCheck.isBlocked) {
                                                        blockedDistractionResult = guardCheck
                                                        Toast.makeText(context, guardCheck.blockReason, Toast.LENGTH_LONG).show()
                                                    }
                                                    return true
                                                }
                                                view.loadUrl(targetUrl)
                                            }
                                            return true
                                        }

                                        override fun onRenderProcessGone(innerView: WebView?, detail: android.webkit.RenderProcessGoneDetail?): Boolean {
                                            try { innerView?.destroy() } catch (e: Exception) {}
                                            return true
                                        }
                                    }
                                }
                                transport?.webView = tempWebView
                                resultMsg?.sendToTarget()
                                return true
                            }

                            override fun onJsAlert(view: WebView?, url: String?, message: String?, result: JsResult?): Boolean {
                                Toast.makeText(context, message ?: "", Toast.LENGTH_SHORT).show()
                                result?.confirm()
                                return true
                            }

                            override fun onJsConfirm(view: WebView?, url: String?, message: String?, result: JsResult?): Boolean {
                                result?.confirm()
                                return true
                            }

                            override fun onPermissionRequest(request: PermissionRequest?) {
                                request?.grant(request.resources)
                            }
                        }

                        loadUrl(currentUrl)
                        webViewInstance = this
                    }
                },
                update = { wv ->
                    webViewInstance = wv
                }
            )

            // Focus Shield Distraction Blocked Overlay (YouTube / Dailymotion / Video Streaming Block)
            AnimatedVisibility(
                visible = blockedDistractionResult != null,
                enter = fadeIn() + scaleIn(initialScale = 0.95f),
                exit = fadeOut() + scaleOut(targetScale = 0.95f)
            ) {
                val blockInfo = blockedDistractionResult
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(if (isDark) Color(0xF5090D16) else Color(0xF5F8FAFC))
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = if (isDark) Color(0xFF1E293B) else Color.White,
                        border = BorderStroke(1.5.dp, if (isDark) Color(0x66EF4444) else Color(0xFFFECACA)),
                        shadowElevation = 24.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 480.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Shield Icon with Glow
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFEF4444).copy(alpha = 0.15f))
                                    .border(1.5.dp, Color(0xFFEF4444).copy(alpha = 0.4f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Block,
                                    contentDescription = "Blocked",
                                    tint = Color(0xFFEF4444),
                                    modifier = Modifier.size(38.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFEF4444).copy(alpha = 0.12f)
                            ) {
                                Text(
                                    "🔒 STUDY FOCUS SHIELD ACTIVE",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.sp,
                                    color = Color(0xFFEF4444),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                "Distraction Platform Blocked",
                                fontWeight = FontWeight.Black,
                                fontSize = 19.sp,
                                color = if (isDark) Color.White else Color(0xFF0F172A),
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                "${blockInfo?.platformName ?: "Video / Entertainment Site"} is restricted in Study Mode to protect your NEET / JEE preparation focus.",
                                fontSize = 13.sp,
                                color = if (isDark) Color(0xCCFFFFFF) else Color(0xFF475569),
                                textAlign = TextAlign.Center,
                                lineHeight = 18.sp
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isDark) Color(0x33000000) else Color(0xFFF1F5F9),
                                border = BorderStroke(1.dp, if (isDark) Color(0x33FFFFFF) else Color(0xFFE2E8F0)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text("🎯", fontSize = 20.sp)
                                    Column {
                                        Text(
                                            "AIR 1 Zero-Distraction Guard",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = if (isDark) Color(0xFFA5B4FC) else Color(0xFF4F46E5)
                                        )
                                        Text(
                                            "YouTube, Dailymotion, social media & video streaming are strictly blocked. Only genuine educational portals are allowed.",
                                            fontSize = 10.5.sp,
                                            color = if (isDark) Color(0x99FFFFFF) else Color(0xFF64748B),
                                            lineHeight = 15.sp
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            // Action Buttons
                            Button(
                                onClick = {
                                    blockedDistractionResult = null
                                    val safeUrl = studyPortals.firstOrNull()?.url ?: "https://pwthor.live"
                                    webViewInstance?.loadUrl(safeUrl)
                                },
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(46.dp)
                            ) {
                                Icon(Icons.Default.Public, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Return to PW Thor / Study Portal",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.5.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedButton(
                                onClick = {
                                    blockedDistractionResult = null
                                    showPortalSelectorDialog = true
                                },
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, if (isDark) Color(0x66818CF8) else Color(0xFF6366F1)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp)
                            ) {
                                Icon(
                                    Icons.Default.Apps,
                                    contentDescription = null,
                                    tint = if (isDark) Color(0xFFA5B4FC) else Color(0xFF4F46E5),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    "Switch to Other Study Portals",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.5.sp,
                                    color = if (isDark) Color(0xFFA5B4FC) else Color(0xFF4F46E5)
                                )
                            }
                        }
                    }
                }
            }

            // Scanning AI Animation Overlay
            AnimatedVisibility(
                visible = isScanning,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.82f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        CircularProgressIndicator(
                            color = Color(0xFF818CF8),
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "⚡ LAKSHYA AI VISION TOP-TO-BOTTOM OCR",
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            scanStatusMessage,
                            color = Color(0xFFCBD5E1),
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // Floating Live Download Progress / Completion HUD
            AnimatedVisibility(
                visible = activeDownload != null,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 16.dp, vertical = 20.dp)
            ) {
                activeDownload?.let { download ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        color = if (isDark) Color(0xF20F172A) else Color(0xF8FFFFFF),
                        border = BorderStroke(
                            1.5.dp,
                            Brush.horizontalGradient(
                                listOf(
                                    when (download.state) {
                                        ActiveDownloadState.COMPLETED -> Color(0xFF10B981)
                                        ActiveDownloadState.FAILED -> Color(0xFFEF4444)
                                        ActiveDownloadState.DOWNLOADING -> Color(0xFF818CF8)
                                    },
                                    when (download.state) {
                                        ActiveDownloadState.COMPLETED -> Color(0xFF059669)
                                        ActiveDownloadState.FAILED -> Color(0xFFDC2626)
                                        ActiveDownloadState.DOWNLOADING -> Color(0xFFC084FC)
                                    }
                                )
                            )
                        ),
                        shadowElevation = 18.dp
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(
                                            when (download.state) {
                                                ActiveDownloadState.COMPLETED -> Color(0x2210B981)
                                                ActiveDownloadState.FAILED -> Color(0x22EF4444)
                                                ActiveDownloadState.DOWNLOADING -> Color(0x226366F1)
                                            }
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        when (download.state) {
                                            ActiveDownloadState.COMPLETED -> "✅"
                                            ActiveDownloadState.FAILED -> "❌"
                                            ActiveDownloadState.DOWNLOADING -> "📥"
                                        },
                                        fontSize = 18.sp
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = download.fileName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        color = if (isDark) Color.White else Color(0xFF0F172A)
                                    )
                                    Text(
                                        text = when (download.state) {
                                            ActiveDownloadState.COMPLETED -> "Saved to Device Storage (Downloads/Testbook_PDFs/)"
                                            ActiveDownloadState.FAILED -> "Download failed: ${download.errorMessage ?: "Network error"}"
                                            ActiveDownloadState.DOWNLOADING -> "${(download.progress * 100).toInt()}% • ${formatBytes(download.downloadedBytes)} / ${if (download.totalBytes > 0) formatBytes(download.totalBytes) else "..."}"
                                        },
                                        fontSize = 11.5.sp,
                                        color = when (download.state) {
                                            ActiveDownloadState.COMPLETED -> Color(0xFF10B981)
                                            ActiveDownloadState.FAILED -> Color(0xFFEF4444)
                                            ActiveDownloadState.DOWNLOADING -> if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                                        }
                                    )
                                }

                                IconButton(
                                    onClick = { activeDownload = null },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Close",
                                        tint = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            if (download.state == ActiveDownloadState.DOWNLOADING) {
                                Spacer(modifier = Modifier.height(4.dp))
                                FireworksProgressBar(
                                    progress = download.progress,
                                    height = 6.dp,
                                    gradientColors = listOf(Color(0xFF8B5CF6), Color(0xFFA78BFA), Color(0xFFC084FC)),
                                    sparkColor = Color(0xFFA78BFA),
                                    isDark = isDark
                                )
                            }

                            if (download.state == ActiveDownloadState.COMPLETED && download.filePath != null) {
                                val file = File(download.filePath)
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = { openPdfFile(file) },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                                    ) {
                                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Open PDF", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }

                                    OutlinedButton(
                                        onClick = { sharePdfFile(file) },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(1.dp, Color(0xFF10B981))
                                    ) {
                                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFF10B981))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Share", color = Color(0xFF10B981), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }

        }
    }

    // Modal Sheet for Scan Option Selection
    if (showScanOptionsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showScanOptionsSheet = false },
            containerColor = if (isDark) Color(0xFF0F172A) else Color.White,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 36.dp)
            ) {
                Text(
                    "⚡ Scorecard OCR Scanner Options",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = if (isDark) Color.White else Color(0xFF0F172A)
                )
                Text(
                    "Extract marks, accuracy, percentiles & weak topics using Lakshya AI Vision",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Option 1: Top-to-Bottom Full Document Scan
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            showScanOptionsSheet = false
                            executeScan(fullPage = true)
                        },
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDark) Color(0xFF1E293B) else Color(0xFFF8FAFC)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = Brush.linearGradient(listOf(Color(0xFF818CF8), Color(0xFFF472B6)))
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF6366F1).copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("📜", fontSize = 20.sp)
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    "Full Page Scan (Top-to-Bottom)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.5.sp,
                                    color = if (isDark) Color.White else Color(0xFF0F172A),
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color(0xFF10B981).copy(alpha = 0.18f)
                                ) {
                                    Text(
                                        "BEST ⚡",
                                        fontSize = 8.5.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color(0xFF10B981),
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                                        maxLines = 1
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                "Captures the entire scrollable scorecard & subject breakdown",
                                fontSize = 11.sp,
                                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                            )
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.Gray)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Option 2: Visible Screen Only
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            showScanOptionsSheet = false
                            executeScan(fullPage = false)
                        },
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDark) Color(0xFF1E293B) else Color(0xFFF8FAFC)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = Brush.linearGradient(listOf(Color(0xFF38BDF8), Color(0xFF818CF8)))
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF38BDF8).copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("📱", fontSize = 20.sp)
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Current Visible Screen Only",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = if (isDark) Color.White else Color(0xFF0F172A)
                            )
                            Text(
                                "Fast scan of what is currently visible on your display",
                                fontSize = 11.sp,
                                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                            )
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.Gray)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Option 3: Upload from Gallery
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            showScanOptionsSheet = false
                            imagePickerLauncher.launch("image/*")
                        },
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDark) Color(0xFF1E293B) else Color(0xFFF8FAFC)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = Brush.linearGradient(listOf(Color(0xFFF472B6), Color(0xFFEC4899)))
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFEC4899).copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🖼️", fontSize = 20.sp)
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Upload Scorecard Image / Photo",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = if (isDark) Color.White else Color(0xFF0F172A)
                            )
                            Text(
                                "Select a screenshot from your phone gallery or PC photo",
                                fontSize = 11.sp,
                                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                            )
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.Gray)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = if (isDark) Color(0x22FFFFFF) else Color(0xFFE2E8F0))
                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    "🎯 Mistake Notebook OCR Tools",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Black,
                    color = if (isDark) Color(0xFFC7D2FE) else Color(0xFF4338CA)
                )
                Text(
                    "Auto-extract incorrect test questions directly into your Mistake Notebook",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Mistake Option 1: Capture Current Screen Question
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            showScanOptionsSheet = false
                            executeSingleScreenMistakeExtraction()
                        },
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDark) Color(0xFF1E293B) else Color(0xFFF8FAFC)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = Brush.linearGradient(listOf(Color(0xFFE11D48), Color(0xFFF43F5E)))
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFF43F5E).copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("📸", fontSize = 20.sp)
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "Capture Current Wrong Question",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = if (isDark) Color.White else Color(0xFF0F172A)
                                )
                            }
                            Text(
                                "Auto-detects question, wrong vs correct mark & saves to subject notebook",
                                fontSize = 11.sp,
                                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                            )
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.Gray)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Mistake Option 2: Batch Multiple Screenshots from Gallery
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            showScanOptionsSheet = false
                            batchQuestionPickerLauncher.launch("image/*")
                        },
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDark) Color(0xFF1E293B) else Color(0xFFF8FAFC)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = Brush.linearGradient(listOf(Color(0xFFA855F7), Color(0xFF6366F1)))
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFA855F7).copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("📚", fontSize = 20.sp)
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "Batch Import Multiple Question Photos",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = if (isDark) Color.White else Color(0xFF0F172A)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color(0xFFA855F7).copy(alpha = 0.2f))
                                        .padding(horizontal = 4.dp, vertical = 1.dp)
                                ) {
                                    Text("BATCH ⚡", fontSize = 8.sp, fontWeight = FontWeight.Black, color = Color(0xFFA855F7))
                                }
                            }
                            Text(
                                "Select multiple test screenshots to extract & categorize all errors at once",
                                fontSize = 11.sp,
                                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                            )
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.Gray)
                    }
                }
            }
        }
    }

    // Modal Sheet for Downloaded PDFs Library
    if (showDownloadsSheet) {
        var pdfSearchQuery by remember { mutableStateOf("") }
        var pdfToDelete by remember { mutableStateOf<DownloadedPdfRecord?>(null) }

        val filteredPdfs = remember(downloadedPdfs, pdfSearchQuery) {
            if (pdfSearchQuery.isBlank()) {
                downloadedPdfs
            } else {
                downloadedPdfs.filter { it.fileName.contains(pdfSearchQuery.trim(), ignoreCase = true) }
            }
        }

        ModalBottomSheet(
            onDismissRequest = { showDownloadsSheet = false },
            containerColor = if (isDark) Color(0xFF0F172A) else Color.White,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.85f)
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 24.dp)
            ) {
                // Sheet Top Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF10B981).copy(alpha = 0.18f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("📥", fontSize = 20.sp)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "Downloaded Materials",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Black,
                                    color = if (isDark) Color.White else Color(0xFF0F172A)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    color = Color(0xFF10B981).copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        "${downloadedPdfs.size} PDFs",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color(0xFF10B981),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                "Offline question papers, answer keys & lecture notes",
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 11.5.sp,
                                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = {
                                refreshDownloadedPdfs()
                                Toast.makeText(context, "Storage refreshed! 🔄", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = "Refresh",
                                tint = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                            )
                        }
                        IconButton(
                            onClick = { showDownloadsSheet = false },
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Close",
                                tint = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Search Bar & External Storage Action Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = pdfSearchQuery,
                        onValueChange = { pdfSearchQuery = it },
                        placeholder = { Text("Search downloaded papers...", fontSize = 12.sp) },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                        },
                        trailingIcon = {
                            if (pdfSearchQuery.isNotEmpty()) {
                                IconButton(onClick = { pdfSearchQuery = "" }, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF10B981),
                            unfocusedBorderColor = if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1)
                        ),
                        modifier = Modifier.weight(1f),
                        textStyle = MaterialTheme.typography.bodySmall
                    )

                    OutlinedButton(
                        onClick = {
                            try {
                                val intent = Intent(DownloadManager.ACTION_VIEW_DOWNLOADS).apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                }
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                try {
                                    val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                                        type = "*/*"
                                        addCategory(Intent.CATEGORY_OPENABLE)
                                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                    }
                                    context.startActivity(intent)
                                } catch (_: Exception) {
                                    Toast.makeText(context, "Check device Downloads folder in Files app", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp)
                    ) {
                        Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFF10B981))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Device", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Downloaded PDFs List or Empty State
                if (downloadedPdfs.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Surface(
                                color = if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9),
                                shape = CircleShape,
                                modifier = Modifier.size(72.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text("📄", fontSize = 34.sp)
                                }
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                "No Downloaded PDFs Yet",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = if (isDark) Color.White else Color(0xFF0F172A)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                "Click the download button or export question papers on any test portal (PW, Testbook, Allen, NTA, Unacademy) to save and read papers offline here.",
                                textAlign = TextAlign.Center,
                                fontSize = 12.sp,
                                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                                lineHeight = 18.sp
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { refreshDownloadedPdfs() },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Scan Device Downloads", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                } else if (filteredPdfs.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No files matching \"$pdfSearchQuery\"",
                            fontSize = 13.sp,
                            color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(filteredPdfs, key = { it.filePath }) { record ->
                            val file = File(record.filePath)
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isDark) Color(0xFF1E293B) else Color(0xFFF8FAFC)
                                ),
                                border = BorderStroke(
                                    1.dp,
                                    if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)
                                )
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(42.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(
                                                    Brush.linearGradient(
                                                        listOf(Color(0xFFEF4444), Color(0xFFDC2626))
                                                    )
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                Icons.Default.PictureAsPdf,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(12.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = record.fileName,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.5.sp,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis,
                                                color = if (isDark) Color.White else Color(0xFF0F172A)
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = record.fileSizeFormatted,
                                                    fontSize = 11.5.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = Color(0xFF10B981)
                                                )
                                                Text(
                                                    text = " • ${record.dateAdded}",
                                                    fontSize = 11.sp,
                                                    color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Action Buttons Row
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Open in PDF Reader
                                        Button(
                                            onClick = {
                                                showDownloadsSheet = false
                                                openPdfFile(file)
                                            },
                                            shape = RoundedCornerShape(10.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                            modifier = Modifier.weight(1.1f)
                                        ) {
                                            Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(15.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Read PDF", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                                        }

                                        // Start AI CBT Test from this PDF
                                        OutlinedButton(
                                            onClick = {
                                                showDownloadsSheet = false
                                                try {
                                                    val uri = FileProvider.getUriForFile(
                                                        context,
                                                        "${context.packageName}.fileprovider",
                                                        file
                                                    )
                                                    viewModel.extractTestFromDocumentOrImage(
                                                        context = context,
                                                        exam = viewModel.activeExamGoal.value,
                                                        uri = uri,
                                                        fileName = file.name,
                                                        institute = "Downloaded Paper",
                                                        startPage = 1,
                                                        endPage = null
                                                    )
                                                    if (onNavigate != null) {
                                                        onNavigate("ai_test")
                                                    } else {
                                                        Toast.makeText(context, "Extracting CBT test from ${file.name}...", Toast.LENGTH_SHORT).show()
                                                    }
                                                } catch (e: Exception) {
                                                    Toast.makeText(context, "Unable to load into CBT: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                                                }
                                            },
                                            shape = RoundedCornerShape(10.dp),
                                            border = BorderStroke(1.dp, Color(0xFF6366F1)),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                                            modifier = Modifier.weight(1.2f)
                                        ) {
                                            Text("⚡", fontSize = 12.sp)
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("CBT Test", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF6366F1))
                                        }

                                        // Share Button
                                        IconButton(
                                            onClick = { sharePdfFile(file) },
                                            modifier = Modifier.size(34.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Share,
                                                contentDescription = "Share",
                                                tint = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }

                                        // Delete Button
                                        IconButton(
                                            onClick = { pdfToDelete = record },
                                            modifier = Modifier.size(34.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.DeleteOutline,
                                                contentDescription = "Delete",
                                                tint = Color(0xFFEF4444),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Delete Confirmation Dialog
        if (pdfToDelete != null) {
            val targetRecord = pdfToDelete!!
            AlertDialog(
                onDismissRequest = { pdfToDelete = null },
                title = { Text("Delete Downloaded File?", fontWeight = FontWeight.Bold) },
                text = { Text("Are you sure you want to delete \"${targetRecord.fileName}\" from your device?") },
                confirmButton = {
                    Button(
                        onClick = {
                            try {
                                val f = File(targetRecord.filePath)
                                if (f.exists()) f.delete()
                                downloadedPdfs = downloadedPdfs.filter { it.filePath != targetRecord.filePath }
                                Toast.makeText(context, "Deleted ${targetRecord.fileName}", Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                Toast.makeText(context, "Delete failed: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                            }
                            pdfToDelete = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                    ) {
                        Text("Delete", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { pdfToDelete = null }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }

    // Review & Auto-Sync Dialog
    if (showReviewDialog && scannedResult != null) {
        val result = scannedResult!!
        var editableTitle by remember { mutableStateOf(result.testTitle) }
        var editableScore by remember { mutableStateOf(result.totalScore.toString()) }
        var editableMaxScore by remember { mutableStateOf(result.maxScore.toString()) }
        var editablePhysics by remember { mutableStateOf(result.physicsScore.toString()) }
        var editablePhysicsMax by remember { mutableStateOf(result.physicsMax.toString()) }
        var editableChemistry by remember { mutableStateOf(result.chemistryScore.toString()) }
        var editableChemistryMax by remember { mutableStateOf(result.chemistryMax.toString()) }
        var editableBiology by remember { mutableStateOf(result.biologyScore.toString()) }
        var editableBiologyMax by remember { mutableStateOf(result.biologyMax.toString()) }
        var editableNegative by remember { mutableStateOf(result.negativeMarks.toString()) }

        AlertDialog(
            onDismissRequest = { showReviewDialog = false },
            containerColor = if (isDark) Color(0xFF0F172A) else Color.White,
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth(0.95f),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("⚡ ", fontSize = 20.sp)
                    Column {
                        Text(
                            "AI Scorecard Verified",
                            fontWeight = FontWeight.Black,
                            fontSize = 17.sp,
                            color = if (isDark) Color.White else Color(0xFF0F172A)
                        )
                        Text(
                            "Top-to-Bottom OCR (Max: ${result.maxScore} Marks)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF818CF8)
                        )
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    // Safe Testing Badge Notice
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF6366F1).copy(alpha = 0.12f))
                            .border(1.dp, Color(0xFF6366F1).copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                            .padding(10.dp)
                    ) {
                        Text(
                            "🧪 Scanned exact max marks (${result.maxScore}M). You can adjust total/subject max marks below if needed.",
                            fontSize = 10.5.sp,
                            color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569),
                            lineHeight = 14.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Editable Test Title
                    OutlinedTextField(
                        value = editableTitle,
                        onValueChange = { editableTitle = it },
                        label = { Text("Test Title") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Total Score and Max Marks
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = editableScore,
                            onValueChange = { editableScore = it },
                            label = { Text("Score Obtained") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                        OutlinedTextField(
                            value = editableMaxScore,
                            onValueChange = { editableMaxScore = it },
                            label = { Text("Total Max Marks") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Negative Marks
                    OutlinedTextField(
                        value = editableNegative,
                        onValueChange = { editableNegative = it },
                        label = { Text("Negative Marks Deducted / Incorrect Qs") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        "Subject Breakdown (Obtained / Max):",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569)
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    // Subject Breakdown with dynamic Max
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedTextField(
                            value = editablePhysics,
                            onValueChange = { editablePhysics = it },
                            label = { Text("Phy (/$editablePhysicsMax)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                        OutlinedTextField(
                            value = editableChemistry,
                            onValueChange = { editableChemistry = it },
                            label = { Text("Chem (/$editableChemistryMax)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                        OutlinedTextField(
                            value = editableBiology,
                            onValueChange = { editableBiology = it },
                            label = { Text("Bio (/$editableBiologyMax)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    if (result.rankOrPercentile.isNotBlank()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            "🏆 ${result.rankOrPercentile}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = Color(0xFF10B981)
                        )
                    }

                    if (result.weakTopics.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "⚠️ Identified Weak Topics:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = Color(0xFFF59E0B)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        result.weakTopics.forEach { topic ->
                            Text("• $topic", fontSize = 10.5.sp, color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569))
                        }
                    }

                    if (result.mistakesIdentified.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            "📝 Mistakes Log for Error Notebook:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = Color(0xFFEC4899)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        result.mistakesIdentified.take(4).forEach { mistake ->
                            Text("• $mistake", fontSize = 10.5.sp, color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569))
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val parsedTotal = editableScore.toIntOrNull() ?: result.totalScore
                        val parsedMax = editableMaxScore.toIntOrNull() ?: result.maxScore
                        val parsedPhy = editablePhysics.toIntOrNull() ?: result.physicsScore
                        val parsedPhyMax = editablePhysicsMax.toIntOrNull() ?: result.physicsMax
                        val parsedChem = editableChemistry.toIntOrNull() ?: result.chemistryScore
                        val parsedChemMax = editableChemistryMax.toIntOrNull() ?: result.chemistryMax
                        val parsedBio = editableBiology.toIntOrNull() ?: result.biologyScore
                        val parsedBioMax = editableBiologyMax.toIntOrNull() ?: result.biologyMax
                        val parsedNeg = editableNegative.toIntOrNull() ?: result.negativeMarks

                        val updatedResult = result.copy(
                            testTitle = editableTitle,
                            totalScore = parsedTotal,
                            maxScore = parsedMax,
                            physicsScore = parsedPhy,
                            physicsMax = parsedPhyMax,
                            chemistryScore = parsedChem,
                            chemistryMax = parsedChemMax,
                            biologyScore = parsedBio,
                            biologyMax = parsedBioMax,
                            negativeMarks = parsedNeg
                        )

                        viewModel.syncTestbookScorecardToApp(updatedResult) { msg ->
                            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                        }
                        showReviewDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF6366F1)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("🚀 Auto-Apply to App Data", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showReviewDialog = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            }
        )
    }

    // Mistake Extraction Review & Import Dialog
    if (showMistakesImportDialog && extractedMistakesResult != null) {
        val resultData = extractedMistakesResult!!
        BatchMistakesImportDialog(
            result = resultData,
            isDark = isDark,
            onDismiss = {
                showMistakesImportDialog = false
                extractedMistakesResult = null
            },
            onImportConfirmed = { selectedQuestions ->
                val mistakeLogs = selectedQuestions.map { q ->
                    val content = buildString {
                        if (q.chapter.isNotBlank() && q.chapter != "General") {
                            append("[${q.chapter}] ")
                        }
                        append(q.questionText)
                        if (q.userWrongOption.isNotBlank() || q.correctOption.isNotBlank()) {
                            append("\n❌ My Mark: ${q.userWrongOption} | ✅ Key: ${q.correctOption}")
                        }
                        if (q.keyConceptMissed.isNotBlank()) {
                            append("\n💡 Key Concept: ${q.keyConceptMissed}")
                        }
                        if (q.stepByStepSolution.isNotBlank()) {
                            append("\n📝 Solution: ${q.stepByStepSolution}")
                        }
                    }
                    com.example.data.MistakeLog(
                        subject = q.subject,
                        question = content,
                        mistakeType = q.mistakeType
                    )
                }

                viewModel.addBatchMistakeLogs(mistakeLogs)
                showMistakesImportDialog = false
                extractedMistakesResult = null
                Toast.makeText(
                    context,
                    "🎉 Imported ${mistakeLogs.size} incorrect questions into Mistake Notebook!",
                    Toast.LENGTH_LONG
                ).show()
            }
        )
    }

    // Portal Quick Switcher & Manager Dialog
    if (showPortalSelectorDialog) {
        androidx.compose.ui.window.Dialog(onDismissRequest = { showPortalSelectorDialog = false }) {
            var portalSearchQuery by remember { mutableStateOf("") }
            val filteredPortals = remember(studyPortals, portalSearchQuery) {
                if (portalSearchQuery.isBlank()) studyPortals
                else studyPortals.filter {
                    it.name.contains(portalSearchQuery, ignoreCase = true) ||
                    it.tag.contains(portalSearchQuery, ignoreCase = true) ||
                    it.description.contains(portalSearchQuery, ignoreCase = true) ||
                    it.url.contains(portalSearchQuery, ignoreCase = true)
                }
            }

            Surface(
                shape = RoundedCornerShape(22.dp),
                color = if (isDark) Color(0xFF0F172A) else Color.White,
                border = BorderStroke(1.dp, if (isDark) Color(0x4D6366F1) else Color(0xFFCBD5E1)),
                shadowElevation = 24.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 600.dp)
                    .padding(horizontal = 4.dp, vertical = 12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(if (isDark) Color(0x336366F1) else Color(0xFFEEF2FF)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Public,
                                    contentDescription = null,
                                    tint = if (isDark) Color(0xFF818CF8) else Color(0xFF4F46E5),
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                            Column {
                                Text(
                                    "Switch Study Portal",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 15.5.sp,
                                    color = if (isDark) Color.White else Color(0xFF0F172A)
                                )
                                Text(
                                    "AI-Verified Academic Web Portals",
                                    fontSize = 10.5.sp,
                                    color = if (isDark) Color(0x99FFFFFF) else Color(0xFF64748B)
                                )
                            }
                        }
                        IconButton(
                            onClick = { showPortalSelectorDialog = false },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Close",
                                tint = if (isDark) Color(0x99FFFFFF) else Color(0xFF64748B),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Fixed Height Sleek "+ Add Custom Study Web Portal" Action Button
                    Button(
                        onClick = {
                            editingPortal = null
                            showAddEditPortalDialog = true
                        },
                        shape = RoundedCornerShape(13.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        Color(0xFF4F46E5),
                                        Color(0xFF6366F1),
                                        Color(0xFF8B5CF6)
                                    )
                                ),
                                shape = RoundedCornerShape(13.dp)
                            )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Default.AddCircle,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(17.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Add Custom Study Portal",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color.White.copy(alpha = 0.22f)
                            ) {
                                Text(
                                    "AI Verified",
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Portal Search Field
                    OutlinedTextField(
                        value = portalSearchQuery,
                        onValueChange = { portalSearchQuery = it },
                        placeholder = { Text("Search portals (PW, NTA, Testbook...)", fontSize = 12.sp) },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = "Search", modifier = Modifier.size(16.dp), tint = Color(0xFF6366F1))
                        },
                        trailingIcon = {
                            if (portalSearchQuery.isNotEmpty()) {
                                IconButton(onClick = { portalSearchQuery = "" }, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear", modifier = Modifier.size(14.dp))
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Portals List
                    if (filteredPortals.isEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isDark) Color(0x1AFFFFFF) else Color(0xFFF8FAFC),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    if (portalSearchQuery.isBlank()) "No study portals configured yet."
                                    else "No portals match '$portalSearchQuery'",
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                TextButton(onClick = { viewModel.resetStudyPortalsToDefault() }) {
                                    Text("Restore Default Portals (PW Thor & Testbook)", fontSize = 11.5.sp)
                                }
                            }
                        }
                    } else {
                        filteredPortals.forEach { portal ->
                            val isCurrentPortal = currentUrl.contains(
                                portal.url.removePrefix("https://").removePrefix("http://").trimEnd('/').split('/')[0],
                                ignoreCase = true
                            )
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (isCurrentPortal) {
                                    Color(0xFF6366F1).copy(alpha = if (isDark) 0.25f else 0.15f)
                                } else if (isDark) {
                                    Color(0x1F6366F1)
                                } else {
                                    Color(0xFFF8FAFC)
                                },
                                border = BorderStroke(
                                    if (isCurrentPortal) 1.5.dp else 1.dp,
                                    if (isCurrentPortal) Color(0xFF818CF8) else if (isDark) Color(0x33818CF8) else Color(0xFFE2E8F0)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.5.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            showPortalSelectorDialog = false
                                            webViewInstance?.loadUrl(portal.url)
                                        }
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Portal Emoji Badge
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (isDark) Color(0x336366F1) else Color(0xFFEEF2FF))
                                            .border(0.8.dp, if (isDark) Color(0x4D818CF8) else Color(0xFFC7D2FE), RoundedCornerShape(10.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(portal.iconEmoji.ifBlank { "🌐" }, fontSize = 18.sp)
                                    }

                                    Spacer(modifier = Modifier.width(10.dp))

                                    // Portal Details (Expanded width)
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                                        ) {
                                            Text(
                                                portal.name,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.5.sp,
                                                color = if (isDark) Color.White else Color(0xFF0F172A),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f, fill = false)
                                            )
                                            Surface(
                                                color = Color(0xFF6366F1).copy(alpha = if (isDark) 0.25f else 0.12f),
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text(
                                                    portal.tag,
                                                    fontSize = 8.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isDark) Color(0xFFA5B4FC) else Color(0xFF4F46E5),
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(2.dp))

                                        Text(
                                            portal.url.removePrefix("https://").removePrefix("http://").trimEnd('/'),
                                            fontSize = 10.sp,
                                            color = if (isDark) Color(0x99FFFFFF) else Color(0xFF64748B),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )

                                        if (portal.isVerified) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.padding(top = 1.dp)
                                            ) {
                                                Text(
                                                    "🛡️ AI Verified",
                                                    fontSize = 8.5.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = Color(0xFF10B981)
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(4.dp))

                                    // Action Buttons: Edit & Delete (Compact)
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        IconButton(
                                            onClick = {
                                                editingPortal = portal
                                                showAddEditPortalDialog = true
                                            },
                                            modifier = Modifier.size(26.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Edit,
                                                contentDescription = "Edit Portal",
                                                tint = if (isDark) Color(0xFFA5B4FC) else Color(0xFF6366F1),
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }

                                        IconButton(
                                            onClick = {
                                                portalToDelete = portal
                                            },
                                            modifier = Modifier.size(26.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.DeleteOutline,
                                                contentDescription = "Delete Portal",
                                                tint = Color(0xFFEF4444).copy(alpha = 0.85f),
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }

                                        Icon(
                                            Icons.Default.ChevronRight,
                                            contentDescription = "Open Portal",
                                            tint = if (isDark) Color(0xFF818CF8) else Color(0xFF6366F1),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        TextButton(
                            onClick = {
                                viewModel.resetStudyPortalsToDefault()
                            }
                        ) {
                            Icon(
                                Icons.Default.Restore,
                                contentDescription = null,
                                modifier = Modifier.size(13.dp),
                                tint = if (isDark) Color(0x99FFFFFF) else Color(0xFF64748B)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "Restore Default Portals",
                                fontSize = 10.5.sp,
                                color = if (isDark) Color(0x99FFFFFF) else Color(0xFF64748B)
                            )
                        }
                    }
                }
            }
        }
    }

    // Add / Edit Study Portal Dialog
    if (showAddEditPortalDialog) {
        AddEditStudyPortalDialog(
            initialPortal = editingPortal,
            isDark = isDark,
            onDismiss = {
                showAddEditPortalDialog = false
                editingPortal = null
            },
            onSavePortal = { portal ->
                if (editingPortal != null) {
                    viewModel.updateStudyPortal(portal)
                } else {
                    viewModel.addStudyPortal(portal)
                }
                showAddEditPortalDialog = false
                editingPortal = null
            }
        )
    }

    // Delete Confirmation Dialog
    portalToDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { portalToDelete = null },
            title = {
                Text(
                    "Delete '${target.name}'?",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Text(
                    "Are you sure you want to remove this study web portal from your saved list?",
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteStudyPortal(target.id)
                        portalToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Text("Delete", color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { portalToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Synchronized Focus Timer & Zero-Distraction Study Log Sheet
    if (showFocusTimerSheet) {
        ModalBottomSheet(
            onDismissRequest = { showFocusTimerSheet = false },
            containerColor = if (isDark) Color(0xFF0F172A) else Color.White,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                    .navigationBarsPadding(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF6366F1).copy(alpha = 0.15f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Timer,
                                    contentDescription = null,
                                    tint = Color(0xFF818CF8),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                "Focus Timer & Study Log",
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp,
                                color = if (isDark) Color.White else Color(0xFF0F172A)
                            )
                            Text(
                                "Zero-Distraction • Live Sync with Room Logs",
                                fontSize = 11.sp,
                                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                            )
                        }
                    }

                    IconButton(
                        onClick = { showFocusTimerSheet = false },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close",
                            tint = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Big Digital Timer Card
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (isDark) Color(0xFF1E293B) else Color(0xFFF8FAFC),
                    border = BorderStroke(
                        1.5.dp,
                        if (timerIsRunning) {
                            Brush.horizontalGradient(listOf(Color(0xFF10B981), Color(0xFF6366F1)))
                        } else {
                            Brush.horizontalGradient(listOf(if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0), if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)))
                        }
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 20.dp, horizontal = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Current Active Subject Tag
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (timerIsRunning) Color(0xFF10B981).copy(alpha = 0.15f) else Color(0xFF6366F1).copy(alpha = 0.15f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(if (timerIsRunning) Color(0xFF10B981) else Color(0xFF6366F1))
                                )
                                Text(
                                    text = "$timerSubject • ${timerChapter.ifBlank { "Testbook Practice" }}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (timerIsRunning) (if (isDark) Color(0xFF34D399) else Color(0xFF059669)) else (if (isDark) Color(0xFFA5B4FC) else Color(0xFF4F46E5))
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Large Digital Time Display
                        Text(
                            text = formatFocusTimer(timerSecondsElapsed),
                            fontSize = 44.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 2.sp,
                            color = if (timerIsRunning) (if (isDark) Color.White else Color(0xFF0F172A)) else (if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B))
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = if (timerIsRunning) "🔥 Active Study Session in Progress" else "⏸️ Timer Paused / Ready",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (timerIsRunning) Color(0xFF10B981) else if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Subject Fast Switcher Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "SELECT SUBJECT TO LOG",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Black,
                        color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                        letterSpacing = 0.5.sp
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                val subjectList = listOf("Physics", "Chemistry", "Biology", "Mock Test", "General Practice")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    subjectList.forEach { subj ->
                        val isSelected = timerSubject == subj || (subj == "General Practice" && timerSubject.contains("Practice", true))
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) Color(0xFF6366F1) else if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9),
                            border = BorderStroke(1.dp, if (isSelected) Color(0xFF818CF8) else if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1)),
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    viewModel.setTimerSubject(subj)
                                }
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = when (subj) {
                                        "Physics" -> "Phy"
                                        "Chemistry" -> "Chem"
                                        "Biology" -> "Bio"
                                        "Mock Test" -> "Mock"
                                        else -> "Gen"
                                    },
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else if (isDark) Color.White else Color(0xFF334155)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Chapter Selector Title Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "SELECT CHAPTER / TOPIC (${timerSubject.uppercase()})",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Black,
                        color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                        letterSpacing = 0.5.sp
                    )

                    // Type Custom Chapter Button
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF6366F1).copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, Color(0xFF6366F1).copy(alpha = 0.4f)),
                        modifier = Modifier.clickable {
                            customChapterInput = timerChapter
                            showCustomChapterDialog = true
                        }
                    ) {
                        Text(
                            "✏️ Custom Chapter",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF818CF8),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.5.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Chapter Search & Filter Field
                var chapterSearchQuery by remember { mutableStateOf("") }

                OutlinedTextField(
                    value = chapterSearchQuery,
                    onValueChange = { chapterSearchQuery = it },
                    placeholder = {
                        Text(
                            "Search or filter $timerSubject chapters...",
                            fontSize = 11.5.sp,
                            color = if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8)
                        )
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = "Search",
                            tint = if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8),
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF6366F1),
                        unfocusedBorderColor = if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0),
                        focusedContainerColor = if (isDark) Color(0xFF1E293B) else Color(0xFFF8FAFC),
                        unfocusedContainerColor = if (isDark) Color(0xFF1E293B) else Color(0xFFF8FAFC)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 42.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Get Chapters list for current timerSubject
                val rawChapterList = remember(timerSubject) {
                    when {
                        timerSubject.contains("Physics", ignoreCase = true) -> com.example.data.ExamSyllabusDatabase.neetPhysicsChapters.map { it.name }
                        timerSubject.contains("Chemistry", ignoreCase = true) -> com.example.data.ExamSyllabusDatabase.neetChemistryChapters.map { it.name }
                        timerSubject.contains("Biology", ignoreCase = true) || timerSubject.contains("Botany", ignoreCase = true) || timerSubject.contains("Zoology", ignoreCase = true) -> com.example.data.ExamSyllabusDatabase.neetBiologyChapters.map { it.name }
                        timerSubject.contains("Mock", ignoreCase = true) -> listOf("Full Syllabus Mock 720", "Part Test 11th", "Part Test 12th", "PYQ Past 10 Years", "Sectional Speed Test")
                        else -> listOf("General Practice", "NCERT Line-by-Line", "Formula Sheet Practice", "DPQ & Worksheets", "Mistake Notebook Review")
                    }
                }

                val filteredChapters = remember(rawChapterList, chapterSearchQuery) {
                    if (chapterSearchQuery.isBlank()) {
                        rawChapterList
                    } else {
                        rawChapterList.filter { it.contains(chapterSearchQuery, ignoreCase = true) }
                    }
                }

                // Horizontally Scrollable Chapter Chips
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(filteredChapters) { chName ->
                        val isChSelected = timerChapter.equals(chName, ignoreCase = true)
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isChSelected) Color(0xFF10B981) else if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9),
                            border = BorderStroke(
                                1.dp,
                                if (isChSelected) Color(0xFF34D399) else if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1)
                            ),
                            modifier = Modifier.clickable {
                                viewModel.setTimerChapter(chName)
                            }
                        ) {
                            Text(
                                text = chName,
                                fontSize = 11.5.sp,
                                fontWeight = if (isChSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isChSelected) Color.White else if (isDark) Color.White else Color(0xFF334155),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Action Buttons (Play/Pause & Save to Study Log)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Play / Pause Button
                    Button(
                        onClick = { viewModel.toggleTimer() },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (timerIsRunning) Color(0xFFE11D48) else Color(0xFF10B981)
                        ),
                        contentPadding = PaddingValues(vertical = 12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = if (timerIsRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = if (timerIsRunning) "Pause Timer" else "Start Timer",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }

                    // Stop & Save to Study Logs Button
                    Button(
                        onClick = {
                            if (timerSecondsElapsed > 0) {
                                viewModel.stopAndSaveTimer()
                                showFocusTimerSheet = false
                                Toast.makeText(context, "🎉 Study session logged to Study Logs & Analytics!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Timer is at 00:00", Toast.LENGTH_SHORT).show()
                            }
                        },
                        enabled = timerSecondsElapsed > 0,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF6366F1),
                            disabledContainerColor = if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0)
                        ),
                        contentPadding = PaddingValues(vertical = 12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                Icons.Default.Save,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                "Stop & Log",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Distraction-Free Stealth Mode Toggle Card
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isDark) Color(0xFF1E293B).copy(alpha = 0.5f) else Color(0xFFF1F5F9),
                    border = BorderStroke(1.dp, if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "🛡️ Stealth / Silent Focus Mode",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color.White else Color(0xFF0F172A)
                            )
                            Text(
                                "Hides ticking seconds in top bar to avoid test anxiety",
                                fontSize = 10.5.sp,
                                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                            )
                        }
                        Switch(
                            checked = isStealthTimerMode,
                            onCheckedChange = { isStealthTimerMode = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFF6366F1)
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Fullscreen Focus / Pomodoro link
                if (onNavigate != null) {
                    TextButton(
                        onClick = {
                            showFocusTimerSheet = false
                            onNavigate("timer")
                        }
                    ) {
                        Text(
                            "Open Full Forest Pomodoro Timer Screen →",
                            fontSize = 11.5.sp,
                            color = Color(0xFF818CF8),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }

    // Custom Chapter Input Dialog
    if (showCustomChapterDialog) {
        AlertDialog(
            onDismissRequest = { showCustomChapterDialog = false },
            title = {
                Text(
                    "Custom Chapter for $timerSubject",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = if (isDark) Color.White else Color(0xFF0F172A)
                )
            },
            text = {
                Column {
                    Text(
                        "Type the exact chapter name or topic you are studying/practicing on this website:",
                        fontSize = 12.sp,
                        color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = customChapterInput,
                        onValueChange = { customChapterInput = it },
                        placeholder = { Text("e.g., Work Energy Power Practice") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val finalChap = customChapterInput.trim().ifBlank { "General Practice" }
                        viewModel.setTimerChapter(finalChap)
                        showCustomChapterDialog = false
                        Toast.makeText(context, "Chapter set to: $finalChap", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1))
                ) {
                    Text("Save Chapter", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomChapterDialog = false }) {
                    Text("Cancel")
                }
            },
            containerColor = if (isDark) Color(0xFF1E293B) else Color.White
        )
    }
}


class BlobDownloaderInterface(
    private val onDownloadReady: (String, String) -> Unit
) {
    @android.webkit.JavascriptInterface
    fun getBase64FromBlobData(base64Data: String, mimeType: String) {
        onDownloadReady(base64Data, mimeType)
    }
}
