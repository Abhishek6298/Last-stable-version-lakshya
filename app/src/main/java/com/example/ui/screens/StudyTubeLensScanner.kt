package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import com.example.ui.components.NativeMarkdownText
import com.example.ui.components.parseMarkdown
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.data.GeminiChatAssistant
import com.example.data.ScannedQuestionVideoSolutionResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Unescapes HTML and Unicode sequences commonly found in YouTube web responses.
 */
fun unescapeYouTubeHtml(text: String): String {
    var s = text
        .replace("\\u0026", "&")
        .replace("\\\"", "\"")
        .replace("&#39;", "'")
        .replace("&amp;", "&")
        .replace("&quot;", "\"")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
    val unicodeRegex = Regex("""\\u([0-9a-fA-F]{4})""")
    s = unicodeRegex.replace(s) { match ->
        try {
            match.groupValues[1].toInt(16).toChar().toString()
        } catch (e: Exception) {
            match.value
        }
    }
    return s.trim()
}

/**
 * Sanitizes scraped channel names, eliminating raw JSON leftovers like navigationEndpoint or tracking params.
 */
fun cleanScrapedChannel(raw: String): String {
    var s = unescapeYouTubeHtml(raw).trim()
    if (s.contains("\",\"")) s = s.substringBefore("\",\"")
    if (s.contains("navigationEndpoint")) s = s.substringBefore("navigationEndpoint")
    if (s.contains("clickTrackingParams")) s = s.substringBefore("clickTrackingParams")
    if (s.contains("\"")) s = s.substringBefore("\"")
    s = s.replace(Regex("""[\"\}\{\]\[\\]+"""), "").trim()
    if (s.length > 35) s = s.take(35).trim()
    return s.ifBlank { "YouTube Educator" }
}

/**
 * Sanitizes scraped video titles, eliminating raw JSON leftovers.
 */
fun cleanScrapedTitle(raw: String): String {
    var s = unescapeYouTubeHtml(raw).trim()
    if (s.contains("\",\"") && (s.contains("navigationEndpoint") || s.contains("clickTrackingParams"))) {
        s = s.substringBefore("\",\"")
    }
    return s.replace(Regex("""[\"\}\{\]\[\\]+"""), "").trim()
}

/**
 * Assigns a polished, distinctive accent color to any YouTube channel.
 */
fun getChannelThemeColor(channelName: String): Color {
    val nameLower = channelName.lowercase()
    return when {
        nameLower.contains("doubtnut") -> Color(0xFFEA580C)
        nameLower.contains("physics wallah") || nameLower.contains("pw") || nameLower.contains("alakh") -> Color(0xFF8B5CF6)
        nameLower.contains("unacademy") -> Color(0xFF10B981)
        nameLower.contains("vedantu") -> Color(0xFF0284C7)
        nameLower.contains("eduniti") -> Color(0xFF06B6D4)
        nameLower.contains("khan") -> Color(0xFF14B8A6)
        nameLower.contains("allen") -> Color(0xFFF59E0B)
        nameLower.contains("aakash") -> Color(0xFFF97316)
        nameLower.contains("galaxy") -> Color(0xFFEC4899)
        nameLower.contains("tyagi") || nameLower.contains("competishun") -> Color(0xFF6366F1)
        nameLower.contains("sachin") -> Color(0xFF3B82F6)
        nameLower.contains("bakore") -> Color(0xFF10B981)
        else -> {
            val hash = Math.abs(channelName.hashCode())
            val palette = listOf(
                Color(0xFF6366F1), Color(0xFF3B82F6), Color(0xFF10B981),
                Color(0xFF8B5CF6), Color(0xFFF59E0B), Color(0xFF06B6D4),
                Color(0xFFEC4899), Color(0xFF14B8A6), Color(0xFFF43F5E)
            )
            palette[hash % palette.size]
        }
    }
}

/**
 * Multi-tier Video Solution Finder:
 * Discovers video solutions across ALL YouTube channels (Doubtnut, Physics Wallah,
 * Khan Academy, Unacademy, Vedantu, Allen, and all individual teachers/creators).
 * Uses QuestionVideoSolutionMatcher to extract clean stems, build high-precision queries,
 * and re-rank solutions by exact question text and numerical match scores.
 */
suspend fun fetchLensVideoSolutions(
    queries: List<String>,
    apiKey: String,
    curatedChannels: List<CuratedChannel>,
    questionSnippet: String = "",
    subject: String = "",
    chapter: String = "",
    topic: String = ""
): List<YouTubeVideo> = withContext(Dispatchers.IO) {
    val gathered = mutableListOf<YouTubeVideo>()
    val seenIds = mutableSetOf<String>()

    val cleanInfo = com.example.util.QuestionVideoSolutionMatcher.extractCleanQuestionInfo(questionSnippet)
    val targetQueries = com.example.util.QuestionVideoSolutionMatcher.generateHighPrecisionVideoQueries(
        info = cleanInfo,
        subject = subject,
        chapter = chapter,
        topic = topic,
        customQueries = queries
    )

    // Tier 1: Query YouTube API for targeted search queries (captures real channel names for ALL channels)
    for (q in targetQueries) {
        val encodedQuery = URLEncoder.encode(q, "UTF-8")
        if (apiKey.isNotBlank()) {
            try {
                val urlStr = "https://www.googleapis.com/youtube/v3/search?part=snippet&maxResults=20&q=$encodedQuery&type=video&key=$apiKey"
                val connection = URL(urlStr).openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 8000
                connection.readTimeout = 8000
                if (connection.responseCode == 200) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(response)
                    val items = json.optJSONArray("items")
                    if (items != null) {
                        for (i in 0 until items.length()) {
                            val item = items.getJSONObject(i)
                            val videoId = item.optJSONObject("id")?.optString("videoId")
                            val snippet = item.optJSONObject("snippet")
                            val rawTitle = snippet?.optString("title") ?: ""
                            val title = unescapeYouTubeHtml(rawTitle)
                            val rawChannel = snippet?.optString("channelTitle")?.trim() ?: ""
                            val channelTitle = rawChannel.ifBlank { "YouTube Educator" }
                            val thumbnails = snippet?.optJSONObject("thumbnails")
                            val highThumb = thumbnails?.optJSONObject("high")?.optString("url")
                                ?: thumbnails?.optJSONObject("medium")?.optString("url")
                                ?: thumbnails?.optJSONObject("default")?.optString("url")
                            val description = snippet?.optString("description") ?: ""
                            val channelId = snippet?.optString("channelId") ?: ""

                            val tag = when {
                                channelTitle.contains("Doubtnut", ignoreCase = true) -> "Doubtnut"
                                channelTitle.contains("Physics Wallah", ignoreCase = true) -> "Physics Wallah"
                                title.contains("Physics", ignoreCase = true) -> "Physics"
                                title.contains("Chem", ignoreCase = true) -> "Chemistry"
                                title.contains("Bio", ignoreCase = true) -> "Biology"
                                title.contains("Math", ignoreCase = true) -> "Maths"
                                else -> channelTitle
                            }
                            if (!videoId.isNullOrBlank() && title.isNotBlank() && !highThumb.isNullOrBlank()) {
                                if (seenIds.add(videoId)) {
                                    gathered.add(
                                        YouTubeVideo(
                                            id = videoId,
                                            title = title,
                                            channelTitle = channelTitle,
                                            thumbnailUrl = highThumb,
                                            subjectTag = tag,
                                            channelId = channelId,
                                            description = description
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Tier 2: Public YouTube search scrape fallback (extracts real channel names from ANY channel across YouTube!)
        if (gathered.size < 35) {
            try {
                val scrapeUrl = "https://www.youtube.com/results?search_query=$encodedQuery"
                val conn = URL(scrapeUrl).openConnection() as HttpURLConnection
                conn.setRequestProperty(
                    "User-Agent",
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
                )
                conn.connectTimeout = 7000
                conn.readTimeout = 7000
                if (conn.responseCode == 200) {
                    val html = conn.inputStream.bufferedReader().use { it.readText() }
                    val videoBlocks = html.split("\"videoRenderer\":{")
                    for (block in videoBlocks.drop(1)) {
                        val idMatch = Regex(""""videoId":"([a-zA-Z0-9_-]{11})"""").find(block) ?: continue
                        val vidId = idMatch.groupValues[1]

                        val titleMatch = Regex(""""title":\{"runs":\[\{"text":"([^"\\]*(?:\\.[^"\\]*)*)"""").find(block)
                            ?: Regex(""""title":\{"simpleText":"([^"\\]*(?:\\.[^"\\]*)*)"""").find(block)
                            ?: Regex(""""title":\{"runs":\[\{"text":"(.*?)"\}""").find(block)
                        val rawTitle = titleMatch?.groupValues?.get(1) ?: continue
                        val cleanTitle = cleanScrapedTitle(rawTitle)

                        val channelMatch = Regex(""""(?:ownerText|longBylineText|shortBylineText)":\{"runs":\[\{"text":"([^"\\]*(?:\\.[^"\\]*)*)"""").find(block)
                            ?: Regex(""""(?:ownerText|longBylineText|shortBylineText)":\{"simpleText":"([^"\\]*(?:\\.[^"\\]*)*)"""").find(block)
                            ?: Regex(""""(?:ownerText|longBylineText|shortBylineText)":\{"runs":\[\{"text":"(.*?)"\}""").find(block)
                        val rawChannelName = channelMatch?.groupValues?.get(1)?.let { cleanScrapedChannel(it) }?.trim()

                        val realChannel = if (!rawChannelName.isNullOrBlank() && !rawChannelName.equals("YouTube Educator", ignoreCase = true)) {
                            rawChannelName
                        } else {
                            when {
                                cleanTitle.contains("doubtnut", ignoreCase = true) -> "Doubtnut"
                                cleanTitle.contains("physics wallah", ignoreCase = true) || cleanTitle.contains("alakh", ignoreCase = true) -> "Physics Wallah"
                                cleanTitle.contains("unacademy", ignoreCase = true) -> "Unacademy NEET"
                                cleanTitle.contains("vedantu", ignoreCase = true) -> "Vedantu"
                                cleanTitle.contains("eduniti", ignoreCase = true) -> "Eduniti"
                                cleanTitle.contains("khan academy", ignoreCase = true) -> "Khan Academy"
                                cleanTitle.contains("mohit tyagi", ignoreCase = true) -> "Mohit Tyagi"
                                cleanTitle.contains("allen", ignoreCase = true) -> "ALLEN Career Institute"
                                cleanTitle.contains("aakash", ignoreCase = true) -> "Aakash BYJU'S"
                                else -> "YouTube Educator"
                            }
                        }

                        val detectedTag = when {
                            cleanTitle.contains("Physics", ignoreCase = true) -> "Physics"
                            cleanTitle.contains("Chem", ignoreCase = true) -> "Chemistry"
                            cleanTitle.contains("Bio", ignoreCase = true) -> "Biology"
                            cleanTitle.contains("Math", ignoreCase = true) -> "Maths"
                            else -> realChannel
                        }

                        if (seenIds.add(vidId)) {
                            gathered.add(
                                YouTubeVideo(
                                    id = vidId,
                                    title = cleanTitle,
                                    channelTitle = realChannel,
                                    thumbnailUrl = "https://i.ytimg.com/vi/$vidId/hqdefault.jpg",
                                    subjectTag = detectedTag,
                                    description = "Video solution from $realChannel"
                                )
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Tier 3: Match from Curated Channel Video Catalogs
    val keywords = queries.joinToString(" ").lowercase()
        .split(" ")
        .map { it.trim() }
        .filter { it.length >= 3 && it !in listOf("video", "solution", "neet", "questions", "chapter", "lecture") }

    for (ch in curatedChannels) {
        for (v in ch.curatedVideos) {
            val titleLower = v.title.lowercase()
            val matchScore = keywords.count { titleLower.contains(it) }
            if (matchScore >= 1 && seenIds.add(v.id)) {
                gathered.add(v)
            }
        }
    }

    // High-precision re-ranking with Doubtnut & numerical question token scoring
    com.example.util.QuestionVideoSolutionMatcher.scoreAndRankVideoSolutions(
        videos = gathered,
        info = cleanInfo,
        subject = subject
    )
}

/**
 * Full-screen Lakshya Lens Question Scanner for StudyTube.
 * Allows student to scan question from Camera, Gallery, or PDF Selector,
 * analyzes mathematical/conceptual content with Gemini Vision,
 * and fetches multiple relevant video solutions from Physics Wallah, Unacademy, and top educators.
 */
@Composable
fun StudyTubeLensScannerDialog(
    isOpen: Boolean,
    onDismiss: () -> Unit,
    initialImageUri: Uri? = null,
    initialQueryText: String? = null,
    initialBitmap: Bitmap? = null,
    youtubeApiKey: String = "",
    curatedChannels: List<CuratedChannel> = emptyList(),
    geminiApiKey: String = "",
    selectedModel: String = com.example.data.GeminiModelManager.DEFAULT_MODEL,
    onSelectVideo: (YouTubeVideo) -> Unit
) {
    if (!isOpen) return

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current

    // Local Scanner State
    var currentImageUri by remember { mutableStateOf<Uri?>(initialImageUri) }
    var currentBitmap by remember { mutableStateOf<Bitmap?>(initialBitmap) }
    var isScanning by remember { mutableStateOf(false) }
    var scanStatusMessage by remember { mutableStateOf("Ready to scan question") }
    var analysisResult by remember { mutableStateOf<ScannedQuestionVideoSolutionResult?>(null) }
    var videoSolutions by remember { mutableStateOf<List<YouTubeVideo>>(emptyList()) }
    var selectedFilterTab by remember { mutableStateOf("All Solutions") }
    var showManualInput by remember { mutableStateOf(initialImageUri == null && initialBitmap == null && !initialQueryText.isNullOrBlank()) }
    var manualInputText by remember { mutableStateOf(initialQueryText ?: "") }
    var isExplanationExpanded by remember { mutableStateOf(true) }

    // Core Scan Execution Function
    val executeScan: (Uri?, Bitmap?, String?) -> Unit = { targetUri, targetBmp, manualQuery ->
        isScanning = true
        scanStatusMessage = "🔍 Reading question with Lakshya Lens AI..."
        analysisResult = null
        videoSolutions = emptyList()

        coroutineScope.launch {
            try {
                val imageBytes: ByteArray? = withContext(Dispatchers.IO) {
                    if (targetBmp != null) {
                        val stream = ByteArrayOutputStream()
                        targetBmp.compress(Bitmap.CompressFormat.JPEG, 90, stream)
                        stream.toByteArray()
                    } else if (targetUri != null) {
                        try {
                            val scheme = targetUri.scheme?.lowercase()
                            val original: Bitmap? = if (scheme == "http" || scheme == "https") {
                                val url = URL(targetUri.toString())
                                val conn = url.openConnection() as HttpURLConnection
                                conn.connectTimeout = 10000
                                conn.readTimeout = 10000
                                conn.doInput = true
                                conn.connect()
                                conn.inputStream.use { BitmapFactory.decodeStream(it) }
                            } else if (scheme == "file" && targetUri.path != null) {
                                File(targetUri.path!!).inputStream().use { BitmapFactory.decodeStream(it) }
                            } else {
                                try {
                                    context.contentResolver.openInputStream(targetUri)?.use { BitmapFactory.decodeStream(it) }
                                } catch (_: Exception) {
                                    targetUri.path?.let { File(it) }?.takeIf { it.exists() }?.inputStream()?.use { BitmapFactory.decodeStream(it) }
                                }
                            }
                            if (original != null) {
                                val maxDim = 1600
                                val width = original.width
                                val height = original.height
                                val scaled = if (width > maxDim || height > maxDim) {
                                    val ratio = minOf(maxDim.toFloat() / width, maxDim.toFloat() / height)
                                    Bitmap.createScaledBitmap(original, (width * ratio).toInt(), (height * ratio).toInt(), true)
                                } else {
                                    original
                                }
                                val stream = ByteArrayOutputStream()
                                scaled.compress(Bitmap.CompressFormat.JPEG, 90, stream)
                                stream.toByteArray()
                            } else null
                        } catch (e: Exception) {
                            e.printStackTrace()
                            null
                        }
                    } else null
                }

                val resolvedGeminiKey = geminiApiKey.takeIf { it.isNotBlank() }
                    ?: youtubeApiKey.takeIf { it.startsWith("AIza") || it.startsWith("AQ") }

                val scanResult: Result<ScannedQuestionVideoSolutionResult> = if (imageBytes != null && imageBytes.isNotEmpty()) {
                    scanStatusMessage = "✨ Abhi Magic 🪄 scanning question with Lakshya Vision..."
                    GeminiChatAssistant.scanQuestionForVideoSolutions(
                        context = context,
                        imageBytes = imageBytes,
                        contextText = manualQuery,
                        customApiKey = resolvedGeminiKey,
                        targetModel = selectedModel
                    )
                } else if (!manualQuery.isNullOrBlank()) {
                    scanStatusMessage = "✨ Abhi Magic 🪄 searching question results..."
                    GeminiChatAssistant.analyzeQuestionTextForVideoSolutions(
                        context = context,
                        questionText = manualQuery,
                        customApiKey = resolvedGeminiKey,
                        targetModel = selectedModel
                    )
                } else {
                    Result.failure(Exception("Please provide a question photo or enter question text."))
                }

                if (scanResult.isSuccess) {
                    val result = scanResult.getOrThrow()
                    analysisResult = result
                    scanStatusMessage = "🎥 Finding video solutions from Doubtnut, Physics Wallah & top educators..."
                    val videos = fetchLensVideoSolutions(
                        queries = result.searchQueries,
                        apiKey = youtubeApiKey,
                        curatedChannels = curatedChannels,
                        questionSnippet = result.questionText,
                        subject = result.subject,
                        chapter = result.chapter,
                        topic = result.topic
                    )
                    videoSolutions = videos
                    scanStatusMessage = if (videos.isNotEmpty()) {
                        "Found ${videos.size} video solutions!"
                    } else {
                        "Question parsed. Ready to search related lectures."
                    }
                } else {
                    val err = scanResult.exceptionOrNull()?.message ?: "Could not parse question"
                    scanStatusMessage = if (err.contains("Key is missing", ignoreCase = true) || err.contains("API key", ignoreCase = true)) {
                        "🔑 Lakshya AI Key required. Please check Lakshya AI Key in Settings or enter question text below."
                    } else {
                        "⚠️ $err. You can also paste or edit question text below."
                    }
                    showManualInput = true
                }
            } catch (e: Exception) {
                e.printStackTrace()
                scanStatusMessage = "Error: ${e.localizedMessage ?: "Scanning failed"}"
                showManualInput = true
            } finally {
                isScanning = false
            }
        }
    }

    // Auto-trigger scan if opened with an initial image URI, bitmap, or question text (e.g. from CBT or PDF Smart Select)
    LaunchedEffect(initialImageUri, initialBitmap, initialQueryText) {
        if (analysisResult == null) {
            if (initialImageUri != null) {
                currentImageUri = initialImageUri
                currentBitmap = null
                executeScan(initialImageUri, null, initialQueryText)
            } else if (initialBitmap != null) {
                currentImageUri = null
                currentBitmap = initialBitmap
                executeScan(null, initialBitmap, initialQueryText)
            } else if (!initialQueryText.isNullOrBlank()) {
                manualInputText = initialQueryText
                showManualInput = true
                executeScan(null, null, initialQueryText)
            }
        }
    }

    // Gallery and Camera Launchers with Permission & FileProvider handling
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }
    var pendingActionAfterPermission by remember { mutableStateOf<(() -> Unit)?>(null) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            currentImageUri = uri
            currentBitmap = null
            showManualInput = false
            executeScan(uri, null, null)
        }
    }

    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        if (success && pendingCameraUri != null) {
            val uri = pendingCameraUri!!
            currentImageUri = uri
            currentBitmap = null
            showManualInput = false
            executeScan(uri, null, null)
        }
    }

    val cameraPreviewLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bmp: Bitmap? ->
        if (bmp != null) {
            currentBitmap = bmp
            currentImageUri = null
            showManualInput = false
            executeScan(null, bmp, null)
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            pendingActionAfterPermission?.invoke()
        } else {
            Toast.makeText(
                context,
                "Camera permission is required to take photos. You can also select an image from Gallery.",
                Toast.LENGTH_LONG
            ).show()
        }
        pendingActionAfterPermission = null
    }

    fun launchCameraSafely() {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasPermission) {
            pendingActionAfterPermission = {
                launchCameraSafely()
            }
            try {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            } catch (e: Exception) {
                Toast.makeText(context, "Could not open permission prompt: ${e.message}", Toast.LENGTH_SHORT).show()
            }
            return
        }

        try {
            val cacheDir = context.cacheDir
            val imagesDir = File(cacheDir, "lens_camera_photos").apply { if (!exists()) mkdirs() }
            val photoFile = File(imagesDir, "lens_${System.currentTimeMillis()}.jpg")
            val photoUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                photoFile
            )
            pendingCameraUri = photoUri
            takePictureLauncher.launch(photoUri)
        } catch (e: Exception) {
            try {
                cameraPreviewLauncher.launch(null)
            } catch (ex: Exception) {
                Toast.makeText(
                    context,
                    "Camera unavailable on this device. Please use Gallery.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    fun launchGallerySafely() {
        try {
            galleryLauncher.launch("image/*")
        } catch (e: Exception) {
            Toast.makeText(context, "Could not open gallery: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // Lakshya Lens Glowing Laser Animation
    val infiniteTransition = rememberInfiniteTransition(label = "lens_laser")
    val laserY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laser_y"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF070B14)),
            color = Color(0xFF070B14)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                // 1. Futuristic Lakshya Lens Top App Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF0F172A))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Lens Icon with Google 4-color styling
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.sweepGradient(
                                    listOf(
                                        Color(0xFF4285F4), // Google Blue
                                        Color(0xFFEA4335), // Google Red
                                        Color(0xFFFBBC05), // Google Yellow
                                        Color(0xFF34A853), // Google Green
                                        Color(0xFF4285F4)
                                    )
                                )
                            )
                            .padding(2.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .background(Color(0xFF0B132B)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.DocumentScanner,
                                contentDescription = null,
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "Lakshya Lens",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFF10B981).copy(alpha = 0.2f),
                                border = BorderStroke(0.8.dp, Color(0xFF10B981))
                            ) {
                                Text(
                                    "AI SCANNER",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF10B981),
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            "Scan Question • Instant Video Solutions",
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp
                        )
                    }

                    // Quick Action: Camera or Gallery
                    IconButton(onClick = { launchCameraSafely() }) {
                        Icon(Icons.Default.CameraAlt, contentDescription = "Camera", tint = Color(0xFF38BDF8))
                    }
                    IconButton(onClick = { launchGallerySafely() }) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = "Gallery", tint = Color(0xFFF59E0B))
                    }
                }

                // Discovered Channels across all of YouTube, dynamically grouped & sorted by video count
                val distinctDiscoveredChannels = remember(videoSolutions) {
                    videoSolutions
                        .groupBy { it.channelTitle }
                        .map { (channel, vids) -> channel to vids.size }
                        .sortedByDescending { it.second }
                }

                val exactMatchCount = remember(videoSolutions) {
                    videoSolutions.count { it.isExactMatch || it.matchScore >= 160 }
                }

                // Provider video counts and active filtered list
                val doubtnutCount = remember(videoSolutions) {
                    videoSolutions.count {
                        it.channelTitle.contains("Doubtnut", ignoreCase = true) ||
                        it.title.contains("Doubtnut", ignoreCase = true) ||
                        it.subjectTag.equals("Doubtnut", ignoreCase = true)
                    }
                }
                val pwCount = remember(videoSolutions) {
                    videoSolutions.count {
                        it.channelTitle.contains("Physics Wallah", ignoreCase = true) ||
                        it.title.contains("Physics Wallah", ignoreCase = true) ||
                        it.title.contains("PW", ignoreCase = true) ||
                        it.title.contains("Alakh", ignoreCase = true)
                    }
                }

                val displayedVideos = remember(videoSolutions, selectedFilterTab) {
                    when {
                        selectedFilterTab == "🎯 Exact Matches" || selectedFilterTab == "Exact Matches" -> {
                            val exacts = videoSolutions.filter { it.isExactMatch || it.matchScore >= 160 }
                            if (exacts.isNotEmpty()) exacts else videoSolutions
                        }
                        selectedFilterTab == "All Solutions" || selectedFilterTab == "All Channels" -> videoSolutions
                        selectedFilterTab == "Doubtnut" -> videoSolutions.filter {
                            it.channelTitle.contains("Doubtnut", ignoreCase = true) ||
                            it.title.contains("Doubtnut", ignoreCase = true) ||
                            it.subjectTag.equals("Doubtnut", ignoreCase = true)
                        }
                        selectedFilterTab == "Physics Wallah" -> videoSolutions.filter {
                            it.channelTitle.contains("Physics Wallah", ignoreCase = true) ||
                            it.title.contains("Physics Wallah", ignoreCase = true) ||
                            it.title.contains("PW", ignoreCase = true) ||
                            it.title.contains("Alakh", ignoreCase = true)
                        }
                        selectedFilterTab == "Unacademy" -> videoSolutions.filter {
                            it.channelTitle.contains("Unacademy", ignoreCase = true) ||
                            it.title.contains("Unacademy", ignoreCase = true)
                        }
                        selectedFilterTab == "Vedantu" -> videoSolutions.filter {
                            it.channelTitle.contains("Vedantu", ignoreCase = true) ||
                            it.title.contains("Vedantu", ignoreCase = true)
                        }
                        // Direct match on any discovered YouTube channel
                        videoSolutions.any { it.channelTitle.equals(selectedFilterTab, ignoreCase = true) } -> {
                            videoSolutions.filter { it.channelTitle.equals(selectedFilterTab, ignoreCase = true) }
                        }
                        else -> {
                            val qLower = selectedFilterTab.lowercase()
                            val keywords = qLower.split(" ").filter { it.length > 3 && it !in listOf("video", "solution", "neet", "questions") }
                            val filtered = videoSolutions.filter { v ->
                                keywords.any { v.title.lowercase().contains(it) } ||
                                keywords.any { v.channelTitle.lowercase().contains(it) }
                            }
                            if (filtered.isNotEmpty()) filtered else videoSolutions
                        }
                    }
                }

                // Main Scrollable Content Area
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    // 2. Viewfinder & Image Preview Area
                    item {
                        if (currentImageUri != null || currentBitmap != null) {
                            // Active Image Viewfinder with Laser Scanner Overlay
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 160.dp, max = 240.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color(0xFF0F172A))
                                    .border(BorderStroke(1.2.dp, Color(0xFF38BDF8).copy(alpha = 0.6f)), RoundedCornerShape(16.dp))
                            ) {
                                if (currentBitmap != null) {
                                    Image(
                                        bitmap = currentBitmap!!.asImageBitmap(),
                                        contentDescription = "Scanned Question",
                                        contentScale = ContentScale.Fit,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else if (currentImageUri != null) {
                                    AsyncImage(
                                        model = currentImageUri,
                                        contentDescription = "Scanned Question",
                                        contentScale = ContentScale.Fit,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }

                                // Lakshya Lens 4-Corner Viewfinder Brackets
                                Canvas(modifier = Modifier.fillMaxSize().padding(10.dp)) {
                                    val len = 24.dp.toPx()
                                    val stroke = 3.5.dp.toPx()
                                    val bracketColor = Color(0xFF38BDF8)

                                    // Top-Left
                                    drawLine(bracketColor, Offset(0f, 0f), Offset(len, 0f), stroke)
                                    drawLine(bracketColor, Offset(0f, 0f), Offset(0f, len), stroke)
                                    // Top-Right
                                    drawLine(bracketColor, Offset(size.width, 0f), Offset(size.width - len, 0f), stroke)
                                    drawLine(bracketColor, Offset(size.width, 0f), Offset(size.width, len), stroke)
                                    // Bottom-Left
                                    drawLine(bracketColor, Offset(0f, size.height), Offset(len, size.height), stroke)
                                    drawLine(bracketColor, Offset(0f, size.height), Offset(0f, size.height - len), stroke)
                                    // Bottom-Right
                                    drawLine(bracketColor, Offset(size.width, size.height), Offset(size.width - len, size.height), stroke)
                                    drawLine(bracketColor, Offset(size.width, size.height), Offset(size.width, size.height - len), stroke)
                                }

                                // Animated Laser Line during scanning
                                if (isScanning) {
                                    Canvas(modifier = Modifier.fillMaxSize()) {
                                        val curY = size.height * laserY
                                        drawRect(
                                            brush = Brush.verticalGradient(
                                                colors = listOf(
                                                    Color.Transparent,
                                                    Color(0xFF38BDF8).copy(alpha = 0.35f),
                                                    Color(0xFF38BDF8),
                                                    Color(0xFF38BDF8).copy(alpha = 0.35f),
                                                    Color.Transparent
                                                ),
                                                startY = curY - 20.dp.toPx(),
                                                endY = curY + 20.dp.toPx()
                                            ),
                                            topLeft = Offset(0f, curY - 20.dp.toPx()),
                                            size = Size(size.width, 40.dp.toPx())
                                        )
                                        drawLine(
                                            color = Color(0xFFFFFFFF),
                                            start = Offset(0f, curY),
                                            end = Offset(size.width, curY),
                                            strokeWidth = 2.5.dp.toPx()
                                        )
                                    }
                                }

                                // Quick Controls Bar on Image
                                Row(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFF0F172A).copy(alpha = 0.85f),
                                        border = BorderStroke(0.8.dp, Color(0xFF334155)),
                                        modifier = Modifier.clickable {
                                            executeScan(currentImageUri, currentBitmap, null)
                                        }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.Refresh, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(13.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Rescan", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                        }
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFF0F172A).copy(alpha = 0.85f),
                                        border = BorderStroke(0.8.dp, Color(0xFF334155)),
                                        modifier = Modifier.clickable { launchCameraSafely() }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.CameraAlt, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(13.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Retake", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                        }
                                    }
                                }
                            }
                        } else {
                            // Empty State: Prominent Lakshya Lens Scanner Card
                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = Color(0xFF0F172A),
                                border = BorderStroke(1.2.dp, Color(0xFF38BDF8).copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(20.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(68.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF0284C7).copy(alpha = 0.15f))
                                            .border(BorderStroke(1.5.dp, Color(0xFF38BDF8)), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.CenterFocusWeak,
                                            contentDescription = null,
                                            tint = Color(0xFF38BDF8),
                                            modifier = Modifier.size(34.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    Text(
                                        "Scan Any Question for Video Solutions",
                                        color = Color.White,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )

                                    Spacer(modifier = Modifier.height(6.dp))

                                    Text(
                                        "Point camera at books, test papers, or DPPs. Lakshya Lens AI extracts formulas and instantly pulls video solutions from Physics Wallah, Unacademy, and top educators.",
                                        color = Color(0xFF94A3B8),
                                        fontSize = 12.sp,
                                        lineHeight = 17.sp,
                                        modifier = Modifier.padding(horizontal = 8.dp)
                                    )

                                    Spacer(modifier = Modifier.height(18.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Button(
                                            onClick = { launchCameraSafely() },
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(12.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
                                        ) {
                                            Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Take Photo", fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                                        }

                                        OutlinedButton(
                                            onClick = { launchGallerySafely() },
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(12.dp),
                                            border = BorderStroke(1.dp, Color(0xFFF59E0B)),
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFF59E0B))
                                        ) {
                                            Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Gallery", fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    TextButton(onClick = { showManualInput = !showManualInput }) {
                                        Icon(Icons.Default.Edit, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            if (showManualInput) "Hide text input" else "Or type question statement manually",
                                            color = Color(0xFF94A3B8),
                                            fontSize = 11.5.sp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Optional Manual Question Input Section
                    if (showManualInput) {
                        item {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = Color(0xFF0F172A),
                                border = BorderStroke(1.dp, Color(0xFF334155)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text("Type or Paste Question Text", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    OutlinedTextField(
                                        value = manualInputText,
                                        onValueChange = { manualInputText = it },
                                        placeholder = { Text("e.g. A particle of mass m executes SHM with amplitude A. Find kinetic energy at x = A/2...", color = Color(0xFF64748B), fontSize = 12.sp) },
                                        modifier = Modifier.fillMaxWidth(),
                                        maxLines = 4,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedContainerColor = Color(0xFF070B14),
                                            unfocusedContainerColor = Color(0xFF070B14),
                                            focusedBorderColor = Color(0xFF38BDF8),
                                            unfocusedBorderColor = Color(0xFF1E293B),
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White
                                        ),
                                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                        keyboardActions = KeyboardActions(onSearch = {
                                            if (manualInputText.isNotBlank()) {
                                                executeScan(null, null, manualInputText)
                                            }
                                        })
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Button(
                                        onClick = {
                                            if (manualInputText.isNotBlank()) {
                                                executeScan(null, null, manualInputText)
                                            }
                                        },
                                        modifier = Modifier.align(Alignment.End),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1))
                                    ) {
                                        Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(15.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Find Video Solutions", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    // 3. Scanning Status & Progress Indicator
                    item {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isScanning) Color(0xFF0C243B) else Color(0xFF0F172A),
                            border = BorderStroke(1.dp, if (isScanning) Color(0xFF38BDF8) else Color(0xFF1E293B)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (isScanning) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = Color(0xFF38BDF8)
                                    )
                                } else {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = if (analysisResult != null) Color(0xFF10B981) else Color(0xFF64748B),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    scanStatusMessage,
                                    color = if (isScanning) Color(0xFF38BDF8) else Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    // 4. Detected Question & AI Concept Analysis Box
                    analysisResult?.let { result ->
                        item {
                            val subjectColor = when (result.subject.lowercase()) {
                                "physics" -> Color(0xFF38BDF8)
                                "chemistry" -> Color(0xFFF59E0B)
                                "biology" -> Color(0xFF10B981)
                                "mathematics" -> Color(0xFFA855F7)
                                else -> Color(0xFF6366F1)
                            }

                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = Color(0xFF0F172A),
                                border = BorderStroke(1.2.dp, subjectColor.copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    // Subject & Chapter Header Row
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = subjectColor.copy(alpha = 0.2f),
                                                border = BorderStroke(0.8.dp, subjectColor)
                                            ) {
                                                Text(
                                                    result.subject.uppercase(),
                                                    color = subjectColor,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                result.chapter,
                                                color = Color.White,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }

                                        IconButton(
                                            onClick = {
                                                clipboardManager.setText(AnnotatedString("${result.questionText}\n\nSolution Concept: ${result.coreConcept}"))
                                                Toast.makeText(context, "Copied question & concept!", Toast.LENGTH_SHORT).show()
                                            },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = Color(0xFF94A3B8), modifier = Modifier.size(16.dp))
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Topic pill
                                    if (result.topic.isNotBlank()) {
                                        Text(
                                            "Topic: ${result.topic}",
                                            color = Color(0xFF94A3B8),
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                    }

                                    // Verbatim Question Text Box with Math & Markdown rendering
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = Color(0xFF070B14),
                                        border = BorderStroke(0.8.dp, Color(0xFF1E293B)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        NativeMarkdownText(
                                            text = result.questionText,
                                            isDark = true,
                                            fontSize = 12.5.sp,
                                            textColor = Color(0xFFE2E8F0),
                                            modifier = Modifier.padding(10.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 5. Video Solutions Section Header & Query/Provider Filter Chips
                    item {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.VideoLibrary, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        "Video Solutions (${videoSolutions.size})",
                                        color = Color.White,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    if (distinctDiscoveredChannels.isNotEmpty()) "${distinctDiscoveredChannels.size} Channels Found" else "Tap to watch in StudyTube",
                                    color = Color(0xFF38BDF8),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Provider Tabs across ALL YouTube Channels + Topic Search Pills
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // 1. Exact Match Priority Filter Tab
                                if (exactMatchCount > 0) {
                                    item {
                                        val isExactSelected = selectedFilterTab == "🎯 Exact Matches" || selectedFilterTab == "Exact Matches"
                                        FilterChip(
                                            selected = isExactSelected,
                                            onClick = { selectedFilterTab = "🎯 Exact Matches" },
                                            label = {
                                                Text(
                                                    "🎯 Exact Matches ($exactMatchCount)",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.ExtraBold
                                                )
                                            },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = Color(0xFF10B981),
                                                selectedLabelColor = Color.White,
                                                containerColor = Color(0xFF064E3B).copy(alpha = 0.6f),
                                                labelColor = Color(0xFF34D399)
                                            ),
                                            border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.6f))
                                        )
                                    }
                                }

                                item {
                                    val isAllSelected = selectedFilterTab == "All Solutions" || selectedFilterTab == "All Channels"
                                    FilterChip(
                                        selected = isAllSelected,
                                        onClick = { selectedFilterTab = "All Channels" },
                                        label = { Text("🌐 All Channels (${videoSolutions.size})", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = Color(0xFF6366F1),
                                            selectedLabelColor = Color.White,
                                            containerColor = Color(0xFF0F172A),
                                            labelColor = Color(0xFF94A3B8)
                                        )
                                    )
                                }

                                // Discovered Channels across all of YouTube
                                items(distinctDiscoveredChannels) { (chName, count) ->
                                    val isSelected = selectedFilterTab.equals(chName, ignoreCase = true)
                                    val accentColor = getChannelThemeColor(chName)
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { selectedFilterTab = chName },
                                        label = {
                                            Text(
                                                "$chName ($count)",
                                                fontSize = 11.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                            )
                                        },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = accentColor,
                                            selectedLabelColor = Color.White,
                                            containerColor = Color(0xFF0F172A),
                                            labelColor = accentColor
                                        )
                                    )
                                }

                                // Quick fetch options if Doubtnut or PW wasn't found in initial top results
                                if (distinctDiscoveredChannels.none { it.first.contains("doubtnut", ignoreCase = true) }) {
                                    item {
                                        FilterChip(
                                            selected = selectedFilterTab == "Doubtnut",
                                            onClick = {
                                                selectedFilterTab = "Doubtnut"
                                                if (doubtnutCount == 0 && analysisResult != null) {
                                                    coroutineScope.launch {
                                                        isScanning = true
                                                        scanStatusMessage = "🔍 Searching Doubtnut question database..."
                                                        val dVids = fetchLensVideoSolutions(
                                                            queries = listOf(
                                                                "${analysisResult!!.questionText.take(70)} Doubtnut",
                                                                "Doubtnut ${analysisResult!!.subject} ${analysisResult!!.topic}",
                                                                "Doubtnut ${analysisResult!!.chapter} question solution"
                                                            ),
                                                            apiKey = youtubeApiKey,
                                                            curatedChannels = curatedChannels,
                                                            questionSnippet = analysisResult!!.questionText,
                                                            subject = analysisResult!!.subject,
                                                            chapter = analysisResult!!.chapter,
                                                            topic = analysisResult!!.topic
                                                        )
                                                        videoSolutions = (dVids + videoSolutions).distinctBy { it.id }
                                                        isScanning = false
                                                    }
                                                }
                                            },
                                            label = {
                                                Text(
                                                    "🟠 + Doubtnut",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = Color(0xFFEA580C),
                                                selectedLabelColor = Color.White,
                                                containerColor = Color(0xFF0F172A),
                                                labelColor = Color(0xFFFB923C)
                                            )
                                        )
                                    }
                                }

                                if (distinctDiscoveredChannels.none { it.first.contains("physics wallah", ignoreCase = true) || it.first.contains("pw", ignoreCase = true) }) {
                                    item {
                                        FilterChip(
                                            selected = selectedFilterTab == "Physics Wallah",
                                            onClick = {
                                                selectedFilterTab = "Physics Wallah"
                                                if (pwCount == 0 && analysisResult != null) {
                                                    coroutineScope.launch {
                                                        isScanning = true
                                                        scanStatusMessage = "🔍 Searching Physics Wallah solutions..."
                                                        val pwVids = fetchLensVideoSolutions(
                                                            queries = listOf(
                                                                "Physics Wallah ${analysisResult!!.chapter} ${analysisResult!!.topic} question solution",
                                                                "PW ${analysisResult!!.subject} ${analysisResult!!.topic}"
                                                            ),
                                                            apiKey = youtubeApiKey,
                                                            curatedChannels = curatedChannels,
                                                            questionSnippet = analysisResult!!.questionText,
                                                            subject = analysisResult!!.subject,
                                                            chapter = analysisResult!!.chapter,
                                                            topic = analysisResult!!.topic
                                                        )
                                                        videoSolutions = (pwVids + videoSolutions).distinctBy { it.id }
                                                        isScanning = false
                                                    }
                                                }
                                            },
                                            label = {
                                                Text(
                                                    "🟣 + PW",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = Color(0xFF8B5CF6),
                                                selectedLabelColor = Color.White,
                                                containerColor = Color(0xFF0F172A),
                                                labelColor = Color(0xFFC084FC)
                                            )
                                        )
                                    }
                                }

                                // Query pills generated by Gemini Vision
                                analysisResult?.searchQueries?.let { queries ->
                                    items(queries) { q ->
                                        val shortQ = q.take(28) + if (q.length > 28) "..." else ""
                                        FilterChip(
                                            selected = selectedFilterTab == q,
                                            onClick = {
                                                selectedFilterTab = q
                                                coroutineScope.launch {
                                                    isScanning = true
                                                    scanStatusMessage = "Fetching solutions for '$shortQ'..."
                                                    val vids = fetchLensVideoSolutions(
                                                        queries = listOf(q),
                                                        apiKey = youtubeApiKey,
                                                        curatedChannels = curatedChannels,
                                                        questionSnippet = analysisResult?.questionText ?: "",
                                                        subject = analysisResult?.subject ?: "",
                                                        chapter = analysisResult?.chapter ?: "",
                                                        topic = analysisResult?.topic ?: ""
                                                    )
                                                    videoSolutions = (vids + videoSolutions).distinctBy { it.id }
                                                    isScanning = false
                                                }
                                            },
                                            label = { Text(shortQ, fontSize = 11.sp) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = Color(0xFF0284C7),
                                                selectedLabelColor = Color.White,
                                                containerColor = Color(0xFF0F172A),
                                                labelColor = Color(0xFF94A3B8)
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 6. Filtered Video Solutions List
                    if (displayedVideos.isEmpty() && !isScanning && analysisResult != null) {
                        item {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFF0F172A),
                                border = BorderStroke(1.dp, Color(0xFF1E293B)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(20.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(Icons.Default.SearchOff, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(32.dp))
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        if (selectedFilterTab == "Doubtnut") "No Doubtnut videos loaded yet" else "No direct video matches found",
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        if (selectedFilterTab == "Doubtnut") "Tap below to search Doubtnut's vast question database specifically for this numerical." else "Try switching tabs or typing key concept keywords manually.",
                                        color = Color(0xFF94A3B8),
                                        fontSize = 11.sp,
                                        textAlign = TextAlign.Center
                                    )

                                    if (selectedFilterTab == "Doubtnut") {
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Button(
                                            onClick = {
                                                coroutineScope.launch {
                                                    isScanning = true
                                                    scanStatusMessage = "🔍 Fetching Doubtnut solutions..."
                                                    val dVids = fetchLensVideoSolutions(
                                                        queries = listOf(
                                                            "${analysisResult!!.questionText.take(70)} Doubtnut",
                                                            "Doubtnut ${analysisResult!!.subject} ${analysisResult!!.topic}"
                                                        ),
                                                        apiKey = youtubeApiKey,
                                                        curatedChannels = curatedChannels,
                                                        questionSnippet = analysisResult!!.questionText,
                                                        subject = analysisResult!!.subject,
                                                        chapter = analysisResult!!.chapter,
                                                        topic = analysisResult!!.topic
                                                    )
                                                    videoSolutions = (dVids + videoSolutions).distinctBy { it.id }
                                                    isScanning = false
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEA580C)),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text("⚡ Search Doubtnut Specifically", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        items(displayedVideos) { video ->
                            StudyTubeVideoSolutionCard(
                                video = video,
                                onPlay = {
                                    onSelectVideo(video)
                                    onDismiss()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Polished Video Solution Card with high-res thumbnail, channel badge,
 * exact question match indicator, and direct one-click StudyTube distraction-free playback.
 */
@Composable
fun StudyTubeVideoSolutionCard(
    video: YouTubeVideo,
    onPlay: () -> Unit
) {
    val isExact = video.isExactMatch || video.matchScore >= 160
    val isDoubtnut = video.channelTitle.contains("Doubtnut", ignoreCase = true) ||
                     video.title.contains("Doubtnut", ignoreCase = true) ||
                     video.subjectTag.equals("Doubtnut", ignoreCase = true)

    val badgeLabel = if (video.channelTitle.isNotBlank() && !video.channelTitle.equals("YouTube Educator", ignoreCase = true)) {
        video.channelTitle.uppercase().take(22)
    } else {
        "VIDEO SOLUTION"
    }
    val badgeColor = if (isExact) Color(0xFF10B981) else getChannelThemeColor(video.channelTitle)
    val cardBorderColor = when {
        isExact -> Color(0xFF10B981).copy(alpha = 0.85f)
        isDoubtnut -> Color(0xFFEA580C).copy(alpha = 0.6f)
        else -> badgeColor.copy(alpha = 0.35f)
    }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (isExact) Color(0xFF06281E) else Color(0xFF0F172A),
        border = BorderStroke(if (isExact) 1.5.dp else 1.dp, cardBorderColor),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onPlay() }
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            // Prominent Top Badge for Exact Matches
            if (isExact) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFF10B981).copy(alpha = 0.25f),
                        border = BorderStroke(0.8.dp, Color(0xFF10B981))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "🎯 EXACT QUESTION MATCH",
                                color = Color(0xFF34D399),
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }

                    Text(
                        "Verified Solution",
                        color = Color(0xFF6EE7B7),
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Thumbnail with Play overlay
                Box(
                    modifier = Modifier
                        .size(width = 110.dp, height = 70.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF070B14))
                ) {
                    AsyncImage(
                        model = video.thumbnailUrl,
                        contentDescription = video.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    // Play Icon Overlay
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.25f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFEF4444).copy(alpha = 0.9f),
                            modifier = Modifier.size(26.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.PlayArrow, contentDescription = "Play", tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Details Column
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        video.title,
                        color = Color.White,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 16.sp
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.AccountCircle,
                            contentDescription = null,
                            tint = badgeColor,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            video.channelTitle,
                            color = badgeColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = badgeColor.copy(alpha = 0.18f)
                        ) {
                            Text(
                                badgeLabel,
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = badgeColor,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }

                        Text(
                            "▶ Play in StudyTube",
                            color = Color(0xFFEF4444),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
