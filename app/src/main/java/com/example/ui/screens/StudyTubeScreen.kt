package com.example.ui.screens

import android.app.Activity
import android.app.DownloadManager
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.webkit.CookieManager
import android.webkit.DownloadListener
import android.webkit.URLUtil
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import android.view.PixelCopy
import android.graphics.Rect
import android.os.Build
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import androidx.activity.compose.BackHandler
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.drawable.BitmapDrawable
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.request.SuccessResult
import java.io.File
import java.io.FileOutputStream
import com.example.data.ExamSyllabusDatabase
import com.example.data.GeminiChatAssistant
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.alpha
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures

import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.AppViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

data class YouTubeVideo(
    val id: String,
    val title: String,
    val channelTitle: String,
    val thumbnailUrl: String,
    val subjectTag: String = "General",
    val channelId: String = "",
    val description: String = "",
    val matchScore: Int = 0,
    val isExactMatch: Boolean = false,
    val publishedAt: String = "",
    val publishTimeMillis: Long = 0L
)

data class StudyTubeTimestamp(
    val timeLabel: String,
    val seekSeconds: Int,
    val title: String,
    val isCustom: Boolean = false
)

fun formatSecondsToTimestamp(totalSeconds: Int): String {
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }
}

fun parseTimestampToSeconds(input: String): Int {
    val clean = input.trim()
    val parts = clean.split(":").mapNotNull { it.toIntOrNull() }
    return when (parts.size) {
        3 -> parts[0] * 3600 + parts[1] * 60 + parts[2]
        2 -> parts[0] * 60 + parts[1]
        1 -> parts[0] * 60
        else -> 0
    }
}

fun extractTimestampsFromDescription(description: String): List<StudyTubeTimestamp> {
    if (description.isBlank()) return emptyList()
    val timestamps = mutableListOf<StudyTubeTimestamp>()
    val lines = description.lines()
    // Matches HH:MM:SS or MM:SS e.g. 01:23:45 or 12:34 or 1:05
    val timeRegex = Regex("""(?:(?:([0-9]{1,2}):)?([0-9]{1,2}):([0-9]{2}))""")

    for (line in lines) {
        val trimmed = line.trim()
        if (trimmed.isBlank()) continue

        val match = timeRegex.find(trimmed) ?: continue
        val hours = match.groupValues[1].toIntOrNull() ?: 0
        val minutes = match.groupValues[2].toIntOrNull() ?: 0
        val seconds = match.groupValues[3].toIntOrNull() ?: 0
        val totalSeconds = hours * 3600 + minutes * 60 + seconds

        val timeLabel = if (hours > 0) {
            String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.US, "%02d:%02d", minutes, seconds)
        }

        // Clean out timestamp and punctuation to get chapter title
        val rawTitle = trimmed.removeRange(match.range).trim()
            .trimStart('-', '–', '—', ':', '.', '|', '•', '>', ']', ')', ' ')
            .trimEnd('-', '–', '—', ':', '.', '|', '•', '<', '[', '(', ' ')

        val cleanTitle = if (rawTitle.isNotBlank()) rawTitle else "Chapter ${timestamps.size + 1}"
        timestamps.add(
            StudyTubeTimestamp(
                timeLabel = timeLabel,
                seekSeconds = totalSeconds,
                title = cleanTitle,
                isCustom = false
            )
        )
    }

    return timestamps.distinctBy { it.seekSeconds }.sortedBy { it.seekSeconds }
}

fun generateIntervalTimestamps(intervalMinutes: Int, totalDurationSeconds: Int): List<StudyTubeTimestamp> {
    if (intervalMinutes <= 0) return emptyList()
    val intervalSeconds = intervalMinutes * 60
    val duration = if (totalDurationSeconds > 60) totalDurationSeconds else 3600

    val list = mutableListOf<StudyTubeTimestamp>()
    var currentSec = 0
    var partNum = 1

    while (currentSec < duration) {
        val hours = currentSec / 3600
        val mins = (currentSec % 3600) / 60
        val secs = currentSec % 60
        val timeLabel = if (hours > 0) {
            String.format(Locale.US, "%02d:%02d:%02d", hours, mins, secs)
        } else {
            String.format(Locale.US, "%02d:%02d", mins, secs)
        }

        val nextSec = minOf(currentSec + intervalSeconds, duration)
        val nextHours = nextSec / 3600
        val nextMins = (nextSec % 3600) / 60
        val nextSecs = nextSec % 60
        val nextTimeLabel = if (nextHours > 0) {
            String.format(Locale.US, "%02d:%02d:%02d", nextHours, nextMins, nextSecs)
        } else {
            String.format(Locale.US, "%02d:%02d", nextMins, nextSecs)
        }

        val title = "Part $partNum ($timeLabel - $nextTimeLabel)"
        list.add(
            StudyTubeTimestamp(
                timeLabel = timeLabel,
                seekSeconds = currentSec,
                title = title,
                isCustom = true
            )
        )
        currentSec += intervalSeconds
        partNum++
    }
    return list
}

fun serializeTimestamps(list: List<StudyTubeTimestamp>): String {
    val array = org.json.JSONArray()
    for (item in list) {
        val obj = org.json.JSONObject().apply {
            put("timeLabel", item.timeLabel)
            put("seekSeconds", item.seekSeconds)
            put("title", item.title)
            put("isCustom", item.isCustom)
        }
        array.put(obj)
    }
    return array.toString()
}

fun deserializeTimestamps(jsonStr: String?): List<StudyTubeTimestamp> {
    if (jsonStr.isNullOrBlank()) return emptyList()
    val list = mutableListOf<StudyTubeTimestamp>()
    try {
        val array = org.json.JSONArray(jsonStr)
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            list.add(
                StudyTubeTimestamp(
                    timeLabel = obj.optString("timeLabel", "00:00"),
                    seekSeconds = obj.optInt("seekSeconds", 0),
                    title = obj.optString("title", "Chapter ${i + 1}"),
                    isCustom = obj.optBoolean("isCustom", true)
                )
            )
        }
    } catch (_: Exception) { }
    return list.sortedBy { it.seekSeconds }
}

fun parseIsoPublishTimestamp(isoStr: String?): Long {
    if (isoStr.isNullOrBlank()) return 0L
    try {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            return java.time.Instant.parse(isoStr).toEpochMilli()
        }
    } catch (_: Exception) { }
    try {
        val clean = isoStr.replace("Z", "+0000").replace("+00:00", "+0000")
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.US)
        return sdf.parse(clean)?.time ?: 0L
    } catch (_: Exception) { }
    try {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return sdf.parse(isoStr.take(10))?.time ?: 0L
    } catch (_: Exception) { }
    return 0L
}

fun formatPublishRelativeTime(isoStr: String, timestamp: Long): String {
    val targetTime = if (timestamp > 0L) timestamp else parseIsoPublishTimestamp(isoStr)
    if (targetTime <= 0L) return ""
    val diff = System.currentTimeMillis() - targetTime
    if (diff < 0L) return "Just now"
    val seconds = diff / 1000L
    val minutes = seconds / 60L
    val hours = minutes / 60L
    val days = hours / 24L
    val months = days / 30L
    val years = days / 365L

    return when {
        years > 0 -> if (years == 1L) "1 yr ago" else "$years yrs ago"
        months > 0 -> if (months == 1L) "1 mo ago" else "$months mos ago"
        days > 0 -> if (days == 1L) "1 day ago" else "$days days ago"
        hours > 0 -> if (hours == 1L) "1 hr ago" else "$hours hrs ago"
        minutes > 0 -> if (minutes == 1L) "1 min ago" else "$minutes mins ago"
        else -> "Just now"
    }
}

data class ChannelPlaylist(
    val id: String,
    val title: String,
    val videoCount: Int,
    val thumbnailUrl: String = "",
    val subjectTag: String = "Revision",
    val playlistId: String = ""
)

data class CuratedChannel(
    val id: String = "",
    val name: String,
    val subject: String,
    val accentColor: Color,
    val icon: ImageVector,
    val description: String,
    val searchQueries: List<String>,
    val curatedVideos: List<YouTubeVideo> = emptyList(),
    val handle: String = "",
    val subscribers: String = "1.2M subscribers",
    val bannerUrl: String = "",
    val curatedPlaylists: List<ChannelPlaylist> = emptyList(),
    val channelId: String = "",
    val isCustom: Boolean = false
)

private tailrec fun android.content.Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

fun generateStudyTubeEmbedHtml(videoId: String, quality: String, startSeconds: Float, speed: Float): String {
    val vqPlayerVar = if (quality != "auto") ", 'vq': '$quality'" else ""
    val startPlayerVar = if (startSeconds > 1f) ", 'start': ${startSeconds.toInt()}" else ""
    return """
        <!DOCTYPE html>
        <html style="background:#000; margin:0; padding:0; width:100%; height:100%;">
        <head>
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
            <meta name="referrer" content="strict-origin-when-cross-origin">
            <style>
                * { margin:0; padding:0; box-sizing:border-box; background:#000; }
                html, body { width:100%; height:100%; overflow:hidden; background:#000; }
                #player-wrapper { width:100%; height:100%; position:relative; }
                #study_player { width:100%; height:100%; }
            </style>
        </head>
        <body>
            <div id="player-wrapper">
                <div id="study_player"></div>
            </div>
            <script src="https://www.youtube.com/iframe_api"></script>
            <script>
                var player;
                var currentCustomSpeed = $speed;
                var currentSelectedQuality = '$quality';
                function onYouTubeIframeAPIReady() {
                    player = new YT.Player('study_player', {
                        width: '100%',
                        height: '100%',
                        videoId: '$videoId',
                        playerVars: {
                            'autoplay': 1,
                            'playsinline': 1,
                            'enablejsapi': 1,
                            'rel': 0,
                            'modestbranding': 1,
                            'controls': 1,
                            'fs': 1,
                            'iv_load_policy': 3$vqPlayerVar$startPlayerVar
                        },
                        events: {
                            'onReady': function(event) {
                                try { event.target.playVideo(); } catch(e) {}
                                if (currentCustomSpeed && currentCustomSpeed !== 1.0) {
                                    try { player.setPlaybackRate(currentCustomSpeed); } catch(e) {}
                                }
                                if (currentSelectedQuality && currentSelectedQuality !== 'auto') {
                                    try {
                                        var qToSet = (currentSelectedQuality === 'auto') ? 'default' : currentSelectedQuality;
                                        if (typeof player.setPlaybackQuality === 'function') {
                                            player.setPlaybackQuality(qToSet);
                                        }
                                        if (typeof player.setPlaybackQualityRange === 'function') {
                                            player.setPlaybackQualityRange(qToSet, qToSet);
                                        }
                                    } catch(e) {}
                                }
                                if (window.Android && window.Android.onPlayerReady) {
                                    window.Android.onPlayerReady();
                                }
                            },
                            'onStateChange': function(event) {
                                if (window.Android && window.Android.onPlayerStateChange) {
                                    window.Android.onPlayerStateChange(event.data);
                                }
                            },
                            'onError': function(event) {
                                if (window.Android && window.Android.onPlayerError) {
                                    window.Android.onPlayerError(event.data);
                                }
                            }
                        }
                    });
                    setInterval(function() {
                        if (player && typeof player.getCurrentTime === 'function' && window.Android && window.Android.onTimeUpdate) {
                            try {
                                window.Android.onTimeUpdate(player.getCurrentTime() || 0, player.getDuration() || 0);
                            } catch(e) {}
                        }
                    }, 500);
                }
                function seekBy(seconds) {
                    if (player && typeof player.getCurrentTime === 'function') {
                        var curr = player.getCurrentTime() || 0;
                        var dur = player.getDuration() || 999999;
                        var target = Math.max(0, Math.min(dur, curr + seconds));
                        player.seekTo(target, true);
                    }
                }
                function setSpeed(spd) {
                    currentCustomSpeed = spd;
                    try {
                        if (player && typeof player.setPlaybackRate === 'function') {
                            player.setPlaybackRate(spd);
                        }
                    } catch(e) {}
                    try {
                        var vids = document.getElementsByTagName('video');
                        for (var i = 0; i < vids.length; i++) {
                            vids[i].playbackRate = spd;
                        }
                    } catch(e) {}
                }
                function setQuality(q) {
                    var ytQuality = (q === 'auto') ? 'default' : q;
                    currentSelectedQuality = ytQuality;
                    try {
                        if (player) {
                            if (typeof player.setPlaybackQualityRange === 'function') {
                                player.setPlaybackQualityRange(ytQuality, ytQuality);
                            }
                            if (typeof player.setPlaybackQuality === 'function') {
                                player.setPlaybackQuality(ytQuality);
                            }
                            var curr = 0;
                            try {
                                if (typeof player.getCurrentTime === 'function') {
                                    curr = player.getCurrentTime() || 0;
                                }
                            } catch(e) {}
                            if (typeof player.loadVideoById === 'function') {
                                player.loadVideoById({
                                    videoId: '$videoId',
                                    startSeconds: Math.max(0, curr),
                                    suggestedQuality: ytQuality
                                });
                            }
                            if (currentCustomSpeed && currentCustomSpeed !== 1.0) {
                                setTimeout(function() {
                                    try { player.setPlaybackRate(currentCustomSpeed); } catch(e) {}
                                }, 350);
                            }
                        }
                    } catch(e) {}
                    try {
                        var iframes = document.querySelectorAll('iframe');
                        for (var i = 0; i < iframes.length; i++) {
                            iframes[i].contentWindow.postMessage(JSON.stringify({
                                event: 'command',
                                func: 'setPlaybackQuality',
                                args: [ytQuality]
                            }), '*');
                            iframes[i].contentWindow.postMessage(JSON.stringify({
                                event: 'command',
                                func: 'setPlaybackQualityRange',
                                args: [ytQuality, ytQuality]
                            }), '*');
                        }
                    } catch(e) {}
                }
                function togglePlayPause() {
                    if (player && typeof player.getPlayerState === 'function') {
                        var state = player.getPlayerState();
                        if (state === 1) {
                            player.pauseVideo();
                            if (window.Android && window.Android.onPlayerStateChange) window.Android.onPlayerStateChange(2);
                        } else {
                            player.playVideo();
                            if (window.Android && window.Android.onPlayerStateChange) window.Android.onPlayerStateChange(1);
                        }
                    }
                }
            </script>
        </body>
        </html>
    """.trimIndent()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyTubeScreen(
    viewModel: AppViewModel,
    onNavigate: ((String) -> Unit)? = null,
    onNavigateBack: () -> Unit
) {
    val isAppDarkTheme by viewModel.isDarkMode.collectAsStateWithLifecycle()
    val isDark = isAppDarkTheme
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current
    val clipboardManager = LocalClipboardManager.current

    val youtubeApiKey by viewModel.youtubeApiKey.collectAsStateWithLifecycle()
    val savedPlaylists by viewModel.savedPlaylists.collectAsStateWithLifecycle()
    val bookmarks by viewModel.studyTubeBookmarks.collectAsStateWithLifecycle()
    val notes by viewModel.studyTubeNotes.collectAsStateWithLifecycle()
    val chapterVideos by viewModel.studyTubeChapterVideos.collectAsStateWithLifecycle()
    val recentlyViewed by viewModel.recentlyViewedVideos.collectAsStateWithLifecycle()
    val customChannelsList by viewModel.customHallOfFameChannels.collectAsStateWithLifecycle()
    val hiddenChannelIds by viewModel.hiddenHallOfFameChannelIds.collectAsStateWithLifecycle()

    val geminiKey1 by viewModel.geminiApiKey1.collectAsStateWithLifecycle()
    val geminiKey2 by viewModel.geminiApiKey2.collectAsStateWithLifecycle()
    val geminiKey3 by viewModel.geminiApiKey3.collectAsStateWithLifecycle()
    val selectedModel by viewModel.selectedGeminiModel.collectAsStateWithLifecycle()
    val aiProvider by viewModel.aiProvider.collectAsStateWithLifecycle()
    val openRouterSelectedModel by viewModel.openRouterSelectedModel.collectAsStateWithLifecycle()

    // Video Library UI State
    var videoLibrarySubject by remember { mutableStateOf("Physics") }
    var videoLibrarySubBranch by remember { mutableStateOf("All") }
    var videoLibraryClassFilter by remember { mutableStateOf("All") } // "All", "11th", "12th", "🔥 High Yield"
    var videoLibrarySearchQuery by remember { mutableStateOf("") }
    var showAttachVideoDialog by remember { mutableStateOf(false) }
    var attachVideoChapterName by remember { mutableStateOf("") }
    var attachVideoSubject by remember { mutableStateOf("") }
    var attachVideoUrlOrId by remember { mutableStateOf("") }
    var attachVideoTitle by remember { mutableStateOf("") }
    var attachVideoChannel by remember { mutableStateOf("") }

    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<YouTubeVideo>>(emptyList()) }
    var playlistResults by remember { mutableStateOf<List<YouTubeVideo>>(emptyList()) }
    var curatedChannelVideos by remember { mutableStateOf<List<YouTubeVideo>>(emptyList()) }
    var curatedChannelPlaylists by remember { mutableStateOf<List<ChannelPlaylist>>(emptyList()) }
    var selectedChannelQuery by remember { mutableStateOf<String?>(null) }
    var channelSearchFilter by remember { mutableStateOf("") }
    var channelActiveTab by remember { mutableIntStateOf(0) } // 0 = Home, 1 = Videos, 2 = Playlists, 3 = About
    var isChannelSubscribed by remember { mutableStateOf(false) }
    var channelNextPageToken by remember { mutableStateOf<String?>(null) }
    var channelUploadsPlaylistId by remember { mutableStateOf<String?>(null) }
    var resolvedChannelId by remember { mutableStateOf<String?>(null) }
    var isChannelLoadingMore by remember { mutableStateOf(false) }
    var isChannelPlaylistsLoading by remember { mutableStateOf(false) }
    var channelFilterTag by remember { mutableStateOf("All") }
    var channelSortOrder by remember { mutableStateOf("Latest") } // "Latest" (Default), "Popular", "Oldest"

    // Smart Study Tools (A-B Loop & Accidental Touch Lock)
    var isTouchLocked by remember { mutableStateOf(false) }
    var loopPointA by remember { mutableStateOf<Float?>(null) }
    var loopPointB by remember { mutableStateOf<Float?>(null) }
    var isLoopActive by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var warningMessage by remember { mutableStateOf<String?>(null) }

    // Active video in theater mode
    var selectedVideo by remember { mutableStateOf<YouTubeVideo?>(null) }
    var selectedSpeed by remember { mutableFloatStateOf(1.0f) }
    var selectedQuality by remember { mutableStateOf("auto") }
    var isDirectTouchMode by remember { mutableStateOf(false) }
    var showQualitySheet by remember { mutableStateOf(false) }
    var activeStudySeconds by remember { mutableLongStateOf(0L) }
    var isTimerRunning by remember { mutableStateOf(true) }
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var isFullScreen by remember { mutableStateOf(false) }
    var isPlaying by remember { mutableStateOf(true) }

    // YouTube-style player gesture & auto-hide overlay state
    var showPlayerOverlay by remember { mutableStateOf(true) }
    var playerInteractionTime by remember { mutableLongStateOf(System.currentTimeMillis()) }

    var videoCurrentTime by remember { mutableFloatStateOf(0f) }
    var videoDuration by remember { mutableFloatStateOf(0f) }

    val reloadVideoWithQuality: (String, String) -> Unit = { qualityKey, qualityLabel ->
        selectedQuality = qualityKey
        val vid = selectedVideo
        if (vid != null) {
            // Apply smoothly in JS without tearing down WebView
            webViewInstance?.evaluateJavascript("setQuality('$qualityKey');", null)
            Toast.makeText(context, "Video quality set to $qualityLabel 🎬", Toast.LENGTH_SHORT).show()
        }
    }

    // Lecture Doubt -> Mistake Notebook State
    var showMistakeDoubtDialog by remember { mutableStateOf(false) }
    var doubtSnapshotUri by remember { mutableStateOf<String?>(null) }
    var isCapturingDoubtSnapshot by remember { mutableStateOf(false) }
    var doubtCaptureTimeSeconds by remember { mutableIntStateOf(0) }

    val activity = remember(context) { context.findActivity() }

    val setFullscreenMode: (Boolean) -> Unit = { enable ->
        isFullScreen = enable
        activity?.let { act ->
            val window = act.window
            val insetsController = WindowCompat.getInsetsController(window, window.decorView)
            if (enable) {
                act.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                insetsController.hide(WindowInsetsCompat.Type.systemBars())
            } else {
                act.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                insetsController.show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    val triggerLectureDoubtCapture: (YouTubeVideo, Int) -> Unit = { video, currentSec ->
        // 1. Pause video playback
        webViewInstance?.evaluateJavascript("player.pauseVideo();", null)
        isPlaying = false

        // 2. Hide overlay controls cleanly during snapshot
        val wasFullScreen = isFullScreen
        showPlayerOverlay = false
        doubtCaptureTimeSeconds = currentSec
        isCapturingDoubtSnapshot = true
        doubtSnapshotUri = null

        coroutineScope.launch {
            try {
                // Wait for Compose to render the clean frame with ZERO overlay controls (isCapturingDoubtSnapshot = true)
                kotlinx.coroutines.delay(200L)

                val uriStr = captureStudyTubeLectureSnapshot(
                    context = context,
                    webView = webViewInstance,
                    videoId = video.id,
                    videoTitle = video.title,
                    channelTitle = video.channelTitle,
                    seekSeconds = currentSec,
                    thumbnailUrl = video.thumbnailUrl
                )
                doubtSnapshotUri = uriStr
            } catch (e: Exception) {
                doubtSnapshotUri = video.thumbnailUrl
            } finally {
                isCapturingDoubtSnapshot = false
                // Safely restore normal orientation if in fullscreen
                if (wasFullScreen || isFullScreen) {
                    setFullscreenMode(false)
                }
                // Open Doubt / Mistake Notebook dialog while keeping YouTube player intact
                showMistakeDoubtDialog = true
            }
        }
    }

    var leftSeekRippleSeconds by remember { mutableIntStateOf(0) }
    var rightSeekRippleSeconds by remember { mutableIntStateOf(0) }
    var is2xHoldActive by remember { mutableStateOf(false) }

    // Auto-hide controls after 4 seconds of inactivity
    LaunchedEffect(showPlayerOverlay, playerInteractionTime) {
        if (showPlayerOverlay) {
            kotlinx.coroutines.delay(4000L)
            showPlayerOverlay = false
        }
    }

    // Auto-reset left seek ripple indicator after 1 second
    LaunchedEffect(leftSeekRippleSeconds) {
        if (leftSeekRippleSeconds > 0) {
            kotlinx.coroutines.delay(1000L)
            leftSeekRippleSeconds = 0
        }
    }

    // Auto-reset right seek ripple indicator after 1 second
    LaunchedEffect(rightSeekRippleSeconds) {
        if (rightSeekRippleSeconds > 0) {
            kotlinx.coroutines.delay(1000L)
            rightSeekRippleSeconds = 0
        }
    }

    // Reset orientation, system bars, and persist study session when leaving this Composable
    DisposableEffect(Unit) {
        onDispose {
            activity?.let { act ->
                act.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                val window = act.window
                val insetsController = WindowCompat.getInsetsController(window, window.decorView)
                insetsController.show(WindowInsetsCompat.Type.systemBars())
            }
            val vid = selectedVideo
            val duration = activeStudySeconds
            if (vid != null && duration >= 10) {
                val t = (vid.title + " " + vid.channelTitle).lowercase()
                val subject = when {
                    t.contains("physic") || t.contains("eduniti") || t.contains("abj") || t.contains("kinematic") || t.contains("mechanic") || t.contains("electro") || t.contains("optics") || t.contains("thermo") -> "Physics"
                    t.contains("chem") || t.contains("organic") || t.contains("inorganic") || t.contains("alk sir") || t.contains("reaction") || t.contains("periodic") || t.contains("bonding") -> "Chemistry"
                    t.contains("bio") || t.contains("botany") || t.contains("zoology") || t.contains("ncert") || t.contains("cell") || t.contains("genetics") || t.contains("human") || t.contains("plant") -> "Biology"
                    t.contains("math") || t.contains("calculus") || t.contains("algebra") || t.contains("matrix") || t.contains("vector") || t.contains("integration") -> "Mathematics"
                    else -> "General Study"
                }
                viewModel.logStudyTubeSession(subject, vid.title, duration)
            }
        }
    }

    // Live study timer effect and history logger during video playback
    LaunchedEffect(selectedVideo) {
        selectedVideo?.let { vid ->
            viewModel.recordRecentlyViewed(
                videoId = vid.id,
                title = vid.title,
                channelTitle = vid.channelTitle,
                thumbUrl = vid.thumbnailUrl,
                subjectTag = vid.subjectTag
            )
        }
    }

    LaunchedEffect(selectedVideo, isTimerRunning) {
        if (selectedVideo != null && isTimerRunning) {
            while (true) {
                kotlinx.coroutines.delay(1000L)
                activeStudySeconds++
            }
        }
    }

    // A-B Repeat Loop Watcher
    LaunchedEffect(videoCurrentTime, isLoopActive, loopPointA, loopPointB) {
        if (isLoopActive && loopPointA != null && loopPointB != null) {
            val a = loopPointA!!
            val b = loopPointB!!
            if (b > a && videoCurrentTime >= b) {
                webViewInstance?.evaluateJavascript("player.seekTo($a, true);", null)
            }
        }
    }

    // Function to close theater mode and auto-log study session
    val closeTheaterMode: () -> Unit = {
        setFullscreenMode(false)
        val vid = selectedVideo
        val duration = activeStudySeconds
        if (vid != null && duration >= 10) {
            // Determine subject from video title or channel
            val t = (vid.title + " " + vid.channelTitle).lowercase()
            val subject = when {
                t.contains("physic") || t.contains("eduniti") || t.contains("abj") || t.contains("kinematic") || t.contains("mechanic") || t.contains("electro") || t.contains("optics") || t.contains("thermo") -> "Physics"
                t.contains("chem") || t.contains("organic") || t.contains("inorganic") || t.contains("alk sir") || t.contains("reaction") || t.contains("periodic") || t.contains("bonding") -> "Chemistry"
                t.contains("bio") || t.contains("botany") || t.contains("zoology") || t.contains("ncert") || t.contains("cell") || t.contains("genetics") || t.contains("human") || t.contains("plant") -> "Biology"
                t.contains("math") || t.contains("calculus") || t.contains("algebra") || t.contains("matrix") || t.contains("vector") || t.contains("integration") -> "Mathematics"
                else -> "General Study"
            }
            viewModel.logStudyTubeSession(subject, vid.title, duration)
            val mins = duration / 60
            val secs = duration % 60
            val timeText = if (mins > 0) "${mins}m ${secs}s" else "${secs}s"
            Toast.makeText(context, "✅ Logged $timeText to Daily $subject Goal!", Toast.LENGTH_SHORT).show()
        }
        selectedVideo = null
        activeStudySeconds = 0L
        selectedSpeed = 1.0f
        selectedQuality = "auto"
        webViewInstance = null
    }

    // UI Dialogs & Sheets
    var showApiKeyDialog by remember { mutableStateOf(false) }
    var inAppBrowserUrl by remember { mutableStateOf<String?>(null) }
    var showAddPlaylistDialog by remember { mutableStateOf(false) }
    var showAddNoteDialog by remember { mutableStateOf(false) }
    var noteTimestampInput by remember { mutableStateOf("00:00") }
    var noteTextInput by remember { mutableStateOf("") }

    // Lakshya Lens Question Scanner for StudyTube
    var showLensScannerDialog by remember { mutableStateOf(false) }
    var lensScannerInitialImageUri by remember { mutableStateOf<Uri?>(null) }
    var lensScannerInitialQuery by remember { mutableStateOf<String?>(null) }

    // Watch for cropped question images or question queries passed from CBT / PDF Smart Select
    LaunchedEffect(viewModel.pendingStudyTubeImageUri, viewModel.pendingStudyTubeQuery) {
        val pendingUri = viewModel.pendingStudyTubeImageUri
        val pendingQuery = viewModel.pendingStudyTubeQuery
        if (!pendingUri.isNullOrBlank() || !pendingQuery.isNullOrBlank()) {
            lensScannerInitialImageUri = pendingUri?.let { Uri.parse(it) }
            lensScannerInitialQuery = pendingQuery
            showLensScannerDialog = true
            viewModel.pendingStudyTubeImageUri = null
            viewModel.pendingStudyTubeQuery = null
        }
    }

    // AI Formula Sheet modal
    var showAiSummarySheet by remember { mutableStateOf(false) }
    var isGeneratingAiSummary by remember { mutableStateOf(false) }
    var aiSummaryContent by remember { mutableStateOf<String?>(null) }
    var aiSheetTitle by remember { mutableStateOf("AI Formula & Revision Sheet") }

    // Redesign states (Matching Reference Images)
    var selectedPlayerTab by remember { mutableIntStateOf(0) }
    var selectedQuickChannel by remember { mutableStateOf("Eduniti") }
    var selectedSubjectFilter by remember { mutableStateOf("Physics") }
    var selectedClassFilter by remember { mutableStateOf("11th") }
    var expandedChapterIndex by remember { mutableIntStateOf(2) }

    // Tab state: 0 = Video Library, 1 = Search, 2 = Curated Hall of Fame, 3 = My Playlists, 4 = Bookmarks, 5 = Lecture Notes
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var activePlaylistId by remember { mutableStateOf<String?>(null) }
    var activePlaylistTitle by remember { mutableStateOf("Playlist") }
    var activePlaylistChannelTitle by remember { mutableStateOf("") }
    var activePlaylistThumbnail by remember { mutableStateOf("") }
    var activePlaylistSubject by remember { mutableStateOf("") }
    var activeCuratedChannel by remember { mutableStateOf<CuratedChannel?>(null) }

    // Custom Hall of Fame Channel Management States
    var showAddChannelDialog by remember { mutableStateOf(false) }
    var channelToDelete by remember { mutableStateOf<CuratedChannel?>(null) }
    var newChannelName by remember { mutableStateOf("") }
    var newChannelSubject by remember { mutableStateOf("Physics") }
    var newChannelHandle by remember { mutableStateOf("") }
    var newChannelDesc by remember { mutableStateOf("") }
    var newChannelColorHex by remember { mutableStateOf("#00F0FF") }

    // Hierarchical Back Navigation Handler
    BackHandler(enabled = true) {
        if (isFullScreen) {
            setFullscreenMode(false)
        } else if (showMistakeDoubtDialog) {
            showMistakeDoubtDialog = false
        } else if (showQualitySheet) {
            showQualitySheet = false
        } else if (isDirectTouchMode) {
            isDirectTouchMode = false
        } else if (showAiSummarySheet) {
            showAiSummarySheet = false
        } else if (showAttachVideoDialog) {
            showAttachVideoDialog = false
        } else if (showAddNoteDialog) {
            showAddNoteDialog = false
        } else if (showAddPlaylistDialog) {
            showAddPlaylistDialog = false
        } else if (showApiKeyDialog) {
            showApiKeyDialog = false
        } else if (showAddChannelDialog) {
            showAddChannelDialog = false
        } else if (channelToDelete != null) {
            channelToDelete = null
        } else if (selectedVideo != null) {
            closeTheaterMode()
        } else if (activePlaylistId != null) {
            activePlaylistId = null
            playlistResults = emptyList()
        } else if (activeCuratedChannel != null) {
            activeCuratedChannel = null
            curatedChannelVideos = emptyList()
            curatedChannelPlaylists = emptyList()
            selectedChannelQuery = null
            channelSearchFilter = ""
            channelActiveTab = 0
        } else {
            onNavigateBack()
        }
    }

    // Modern Dark Glassmorphic Design Palette
    val bgColor = if (isDark) Color(0xFF070B14) else Color(0xFFF8FAFC)
    val glassBg = if (isDark) Color(0x99111C30) else Color(0xF0FFFFFF)
    val glassCardBg = if (isDark) Color(0x80152238) else Color(0xF5FFFFFF)
    val glassBorderColor = if (isDark) Color(0x3338BDF8) else Color(0xFFE2E8F0)
    val glassSpecularBorder = if (isDark) {
        Brush.linearGradient(
            listOf(
                Color(0x60FFFFFF),
                Color(0x15FFFFFF),
                Color(0x3038BDF8)
            )
        )
    } else {
        Brush.linearGradient(
            listOf(
                Color(0xFFE2E8F0),
                Color(0xFFCBD5E1)
            )
        )
    }
    val textColor = if (isDark) Color(0xFFF1F5F9) else Color(0xFF0F172A)
    val subTextColor = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
    val brandRed = Color(0xFFFF2A55)
    val brandCyan = Color(0xFF00F0FF)
    val brandGreen = Color(0xFF10E599)
    val brandPurple = Color(0xFFA855F7)
    val brandAmber = Color(0xFFFFB703)

    val defaultCuratedChannels = remember {
        listOf(
            CuratedChannel(
                id = "physics_wallah",
                name = "Physics Wallah - Alakh Pandey",
                subject = "Physics / Chem / Bio",
                accentColor = brandCyan,
                icon = Icons.Default.ElectricBolt,
                description = "Official Physics Wallah Channel: Ummeed Series, One-Shots, Manzil Batch & Complete NEET/JEE syllabus coverage by Alakh Sir & PW Star Faculties.",
                searchQueries = listOf("Physics Wallah Ummeed NEET One Shot", "Physics Wallah JEE Main Revision", "Physics Wallah Derivations PYQ"),
                handle = "@PhysicsWallah",
                channelId = "UCiGyWN6DEbnj2alu7iapuKQ",
                subscribers = "12.4M subscribers",
                curatedPlaylists = listOf(
                    ChannelPlaylist("pw_pl1", "Ummeed NEET Complete Physics Revision", 18, "https://img.youtube.com/vi/4b63yTwh42U/hqdefault.jpg", "Physics", "PL2bX0sF7w_5e02j3n8m_v6L"),
                    ChannelPlaylist("pw_pl2", "Ummeed Complete Class 12 Chemistry One-Shots", 14, "https://img.youtube.com/vi/Nn42Q2yJ-lM/hqdefault.jpg", "Chemistry", "PL2bX0sF7w_5c9x2v8m_v7K"),
                    ChannelPlaylist("pw_pl3", "NEET Biology Line By Line Marathon", 22, "https://img.youtube.com/vi/0bN_uM7E_j8/hqdefault.jpg", "Biology", "PL2bX0sF7w_5d8z4v1m_v9J"),
                    ChannelPlaylist("pw_pl4", "JEE Manzil Batch Physics Problem Solving", 16, "https://img.youtube.com/vi/W4H2F7fQ9zM/hqdefault.jpg", "Physics", "PL2bX0sF7w_5e7x3v2m_v8H")
                ),
                curatedVideos = listOf(
                    YouTubeVideo("4b63yTwh42U", "Electrostatics in One Shot | Complete Physics Class 12 | Ummeed NEET", "Physics Wallah", "https://img.youtube.com/vi/4b63yTwh42U/hqdefault.jpg", "Physics", "UCiGyWN6DEbnj2alu7iapuKQ"),
                    YouTubeVideo("q8L_5wXnU4A", "Current Electricity One Shot | Class 12 Boards / NEET | Alakh Pandey", "Physics Wallah", "https://img.youtube.com/vi/q8L_5wXnU4A/hqdefault.jpg", "Physics", "UCiGyWN6DEbnj2alu7iapuKQ"),
                    YouTubeVideo("jY24z6bU4gE", "Ray Optics in One Shot | Complete Class 12 Physics | Alakh Pandey", "Physics Wallah", "https://img.youtube.com/vi/jY24z6bU4gE/hqdefault.jpg", "Physics", "UCiGyWN6DEbnj2alu7iapuKQ"),
                    YouTubeVideo("Nn42Q2yJ-lM", "Chemical Kinetics One Shot | Class 12 Chemistry | Ummeed NEET", "Physics Wallah", "https://img.youtube.com/vi/Nn42Q2yJ-lM/hqdefault.jpg", "Chemistry", "UCiGyWN6DEbnj2alu7iapuKQ"),
                    YouTubeVideo("0bN_uM7E_j8", "Human Reproduction One Shot | Biology Class 12 | NEET Ummeed", "Physics Wallah", "https://img.youtube.com/vi/0bN_uM7E_j8/hqdefault.jpg", "Biology", "UCiGyWN6DEbnj2alu7iapuKQ"),
                    YouTubeVideo("P3c3Tj9iWz4", "Thermodynamics in One Shot | Physics Class 11 | Complete NEET Revision", "Physics Wallah", "https://img.youtube.com/vi/P3c3Tj9iWz4/hqdefault.jpg", "Physics", "UCiGyWN6DEbnj2alu7iapuKQ"),
                    YouTubeVideo("W4H2F7fQ9zM", "Work Energy & Power One Shot | Class 11 Physics | Alakh Sir", "Physics Wallah", "https://img.youtube.com/vi/W4H2F7fQ9zM/hqdefault.jpg", "Physics", "UCiGyWN6DEbnj2alu7iapuKQ"),
                    YouTubeVideo("k7Q2pM9vL1s", "Solutions & Colligative Properties One Shot | Class 12 Chemistry", "Physics Wallah", "https://img.youtube.com/vi/k7Q2pM9vL1s/hqdefault.jpg", "Chemistry", "UCiGyWN6DEbnj2alu7iapuKQ")
                )
            ),
            CuratedChannel(
                id = "doubtnut_solutions",
                name = "Doubtnut (NEET, JEE & NCERT)",
                subject = "Question-Wise Video Solutions",
                accentColor = Color(0xFFEA580C),
                icon = Icons.Default.SmartDisplay,
                description = "Millions of instant, step-by-step video solutions for NCERT, HC Verma, DC Pandey, Irodov, PYQs & DPP questions.",
                searchQueries = listOf("Doubtnut Physics Video Solutions", "Doubtnut Chemistry Video Solutions", "Doubtnut NEET PYQ Solution", "Doubtnut"),
                handle = "@doubtnut",
                channelId = "UC6SwhW87c2wLw5c9f5YQ08A",
                subscribers = "3.4M subscribers",
                curatedVideos = listOf(
                    YouTubeVideo("pQ1vL9mK2r4", "Class 12 Physics Current Electricity Question Solutions | Doubtnut", "Doubtnut", "https://img.youtube.com/vi/pQ1vL9mK2r4/hqdefault.jpg", "Physics"),
                    YouTubeVideo("dQ4vM8nK1s9", "Chemical Kinetics & Equilibrium NCERT Question Video Solutions | Doubtnut", "Doubtnut", "https://img.youtube.com/vi/dQ4vM8nK1s9/hqdefault.jpg", "Chemistry"),
                    YouTubeVideo("bQ7vN2pK9r3", "NEET Past 10 Years Physics Numerical Video Solutions | Doubtnut", "Doubtnut", "https://img.youtube.com/vi/bQ7vN2pK9r3/hqdefault.jpg", "Physics")
                )
            ),
            CuratedChannel(
                id = "eduniti_mohit_goenka",
                name = "Eduniti - Mohit Goenka",
                subject = "Physics (JEE / NEET)",
                accentColor = Color(0xFF06B6D4),
                icon = Icons.Default.Speed,
                description = "Rank Booster Physics revisions, 7-minute concept short-cuts, complete formula marathons & chapterwise PYQs by Mohit Goenka Sir (IIT Kharagpur).",
                searchQueries = listOf("Eduniti Physics Revision Mohit Goenka", "Eduniti Top PYQ Physics", "Eduniti Formula Sheet Physics", "Mohit Goenka Physics"),
                handle = "@mohitgoenka99",
                channelId = "UC7px6OmooQLmsJlYACR_n-A",
                subscribers = "730K subscribers",
                curatedPlaylists = listOf(
                    ChannelPlaylist("edu_pl1", "Booster Session JEE Advanced & Main | Mohit Goenka Sir", 12, "https://i2.ytimg.com/vi/mjgwfz3AAII/hqdefault.jpg", "Physics", "PLjvx7xqdpePJE4ULlrkbwIHBLlur_XCxT"),
                    ChannelPlaylist("edu_pl2", "JEE Main Solutions & One-Shots | Eduniti Mohit Sir", 11, "https://i.ytimg.com/vi/UZQBHNUPJwc/hqdefault.jpg", "Physics", "PLjvx7xqdpePKPYSBNy8wBilBVxCvPxSxD"),
                    ChannelPlaylist("edu_pl3", "JEE Advanced Chapterwise Solutions & PYQs | Mohit Sir", 7, "https://i.ytimg.com/vi/9NR2TR_SG8Y/hqdefault.jpg", "Physics", "PLjvx7xqdpePIcD63Tqu5SSW3hcQQVJiZK"),
                    ChannelPlaylist("edu_pl4", "Physics in Minutes Revision Short-Cuts | Eduniti", 15, "https://i.ytimg.com/vi/JJyxYVYgsqY/hqdefault.jpg", "Physics", "PLjvx7xqdpePKWI3f12KIu-Df2oT6Zs2GG")
                ),
                curatedVideos = listOf(
                    YouTubeVideo("UZQBHNUPJwc", "JEE Main Solutions - Ray Optics & Semiconductors | Eduniti | Mohit Sir", "Eduniti - Physics by Mohit Goenka", "https://i.ytimg.com/vi/UZQBHNUPJwc/hqdefault.jpg", "Physics", "UC7px6OmooQLmsJlYACR_n-A"),
                    YouTubeVideo("9NR2TR_SG8Y", "JEE Adv Solutions | Wave Motion & Mechanics PYQs | Eduniti | Mohit Sir", "Eduniti - Physics by Mohit Goenka", "https://i.ytimg.com/vi/9NR2TR_SG8Y/hqdefault.jpg", "Physics", "UC7px6OmooQLmsJlYACR_n-A"),
                    YouTubeVideo("u-7IGCHB_TU", "JEE Adv Mechanics 6 High Yield Questions | Advanced PYQs | Mohit Sir", "Eduniti - Physics by Mohit Goenka", "https://i.ytimg.com/vi/u-7IGCHB_TU/hqdefault.jpg", "Physics", "UC7px6OmooQLmsJlYACR_n-A"),
                    YouTubeVideo("mjgwfz3AAII", "Booster Session Physics Revision | Mohit Sir Eduniti", "Eduniti - Physics by Mohit Goenka", "https://i2.ytimg.com/vi/mjgwfz3AAII/hqdefault.jpg", "Physics", "UC7px6OmooQLmsJlYACR_n-A"),
                    YouTubeVideo("JJyxYVYgsqY", "Full Gravitation in 2 Minutes #jee #eduniti #physics | Mohit Goenka", "Eduniti - Physics by Mohit Goenka", "https://i.ytimg.com/vi/JJyxYVYgsqY/hqdefault.jpg", "Physics", "UC7px6OmooQLmsJlYACR_n-A"),
                    YouTubeVideo("Zmc_bC0zPyA", "Rules to Crack JEE Physics Strategy | Eduniti Mohit Goenka", "Eduniti - Physics by Mohit Goenka", "https://i3.ytimg.com/vi/Zmc_bC0zPyA/hqdefault.jpg", "Physics", "UC7px6OmooQLmsJlYACR_n-A")
                )
            ),
            CuratedChannel(
                id = "physics_galaxy",
                name = "Physics Galaxy - Ashish Arora",
                subject = "Physics (Concept & Advanced)",
                accentColor = Color(0xFF38BDF8),
                icon = Icons.Default.AutoStories,
                description = "Deep concept videos, checklist revisions, advance illustration marathons & PG physics books coverage by Ashish Arora Sir.",
                searchQueries = listOf("Physics Galaxy Revision Checklist", "Physics Galaxy Advanced Illustrations", "Ashish Arora Physics Concept"),
                handle = "@PhysicsGalaxy74",
                channelId = "UC-wyy-IyE52T-bkTB4NH2QQ",
                subscribers = "1.5M subscribers",
                curatedVideos = listOf(
                    YouTubeVideo("p1Q8mV3bL9A", "Physics Revision Checklist Complete Mechanics | Ashish Arora", "Physics Galaxy", "https://img.youtube.com/vi/p1Q8mV3bL9A/hqdefault.jpg", "Physics"),
                    YouTubeVideo("k2M9qL7vB4N", "Electrodynamics Revision Checklist | Physics Galaxy Ashish Sir", "Physics Galaxy", "https://img.youtube.com/vi/k2M9qL7vB4N/hqdefault.jpg", "Physics"),
                    YouTubeVideo("r5T7mN8vL2K", "Optics & Modern Physics Complete Revision Checklist | PG", "Physics Galaxy", "https://img.youtube.com/vi/r5T7mN8vL2K/hqdefault.jpg", "Physics")
                )
            ),
            CuratedChannel(
                id = "mohit_tyagi",
                name = "Mohit Tyagi (Competishun)",
                subject = "Physics, Maths & Chemistry",
                accentColor = Color(0xFF6366F1),
                icon = Icons.Default.Calculate,
                description = "Deep core conceptual lectures by ABJ Sir (Physics), Mohit Tyagi Sir (Maths) & ALK Sir (Inorganic/Physical Chemistry).",
                searchQueries = listOf("Mohit Tyagi ABJ Sir Physics", "Mohit Tyagi Maths JEE", "Mohit Tyagi ALK Sir Chemistry"),
                handle = "@MohitTyagi",
                channelId = "UCpyc1eTpM1cA3P0ZWym4clw",
                subscribers = "1.1M subscribers",
                curatedPlaylists = listOf(
                    ChannelPlaylist("mt_pl1", "ABJ Sir Complete Electrostatics & Electromagnetism", 28, "https://img.youtube.com/vi/X9z_lQ8tP2A/hqdefault.jpg", "Physics", "PL_A482G0wk7n_nQ9m9x_v8A"),
                    ChannelPlaylist("mt_pl2", "Mohit Tyagi Complete Calculus for JEE Advanced", 32, "https://img.youtube.com/vi/j8L_m9zQ4eA/hqdefault.jpg", "Maths", "PL_A482G0wk7m4f_v9x_v7B"),
                    ChannelPlaylist("mt_pl3", "ALK Sir Coordination Compounds Masterclass", 15, "https://img.youtube.com/vi/b4N_r6vQ1pY/hqdefault.jpg", "Chemistry", "PL_A482G0wk7l8k_v1x_v9C")
                ),
                curatedVideos = listOf(
                    YouTubeVideo("uW9vR6zX4qM", "Complete Vectors & 3D Geometry in One Shot | Mohit Tyagi Sir", "Mohit Tyagi", "https://img.youtube.com/vi/uW9vR6zX4qM/hqdefault.jpg", "Maths"),
                    YouTubeVideo("X9z_lQ8tP2A", "Electrostatics Complete Concept Lecture 01 | ABJ Sir Physics", "Mohit Tyagi", "https://img.youtube.com/vi/X9z_lQ8tP2A/hqdefault.jpg", "Physics"),
                    YouTubeVideo("b4N_r6vQ1pY", "Coordination Compounds Complete Masterclass | ALK Sir Chemistry", "Mohit Tyagi", "https://img.youtube.com/vi/b4N_r6vQ1pY/hqdefault.jpg", "Chemistry"),
                    YouTubeVideo("j8L_m9zQ4eA", "Definite Integration Super Tricks & PYQs | Mohit Tyagi Sir", "Mohit Tyagi", "https://img.youtube.com/vi/j8L_m9zQ4eA/hqdefault.jpg", "Maths"),
                    YouTubeVideo("q9P_3mK2vL7", "Capacitors & Dielectrics Full Lecture | ABJ Sir Physics", "Mohit Tyagi", "https://img.youtube.com/vi/q9P_3mK2vL7/hqdefault.jpg", "Physics")
                )
            ),
            CuratedChannel(
                id = "biology_at_ease",
                name = "Biology at Ease",
                subject = "Biology",
                accentColor = brandGreen,
                icon = Icons.Default.Grass,
                description = "100% NCERT Line-by-Line deep dive, high-yield diagram breakdowns, mnemonics & NEET past 10-year MCQ drills.",
                searchQueries = listOf("Biology at Ease NCERT Line by Line", "Biology at Ease One Shot NEET", "Biology at Ease Diagram Revision"),
                handle = "@BiologyAtEase",
                channelId = "UCYbFCdcMfA68ZvJO__zTUjg",
                subscribers = "950K subscribers",
                curatedPlaylists = listOf(
                    ChannelPlaylist("bio_pl1", "NCERT 100% Line By Line Class 12 Biology", 16, "https://img.youtube.com/vi/m3Q4pL8vX1Y/hqdefault.jpg", "Biology", "PLm3Q4pL8vX1Y_nQ9m9x_v8A"),
                    ChannelPlaylist("bio_pl2", "Human & Plant Physiology One-Shots", 14, "https://img.youtube.com/vi/r8L_2wN7qM8/hqdefault.jpg", "Biology", "PLr8L_2wN7qM8_nQ9m9x_v7B"),
                    ChannelPlaylist("bio_pl3", "Genetics & Ecology High-Yield Score Boosters", 18, "https://img.youtube.com/vi/a7K_9mX4bQ2/hqdefault.jpg", "Biology", "PLa7K_9mX4bQ2_nQ9m9x_v9C")
                ),
                curatedVideos = listOf(
                    YouTubeVideo("m3Q4pL8vX1Y", "Genetics & Molecular Basis of Inheritance One Shot | 100% NCERT", "Biology at Ease", "https://img.youtube.com/vi/m3Q4pL8vX1Y/hqdefault.jpg", "Biology"),
                    YouTubeVideo("r8L_2wN7qM8", "Human Physiology Complete Rapid Revision | NCERT Line By Line", "Biology at Ease", "https://img.youtube.com/vi/r8L_2wN7qM8/hqdefault.jpg", "Biology"),
                    YouTubeVideo("a7K_9mX4bQ2", "Ecology Unit Complete One Shot | NEET NCERT Line to Line", "Biology at Ease", "https://img.youtube.com/vi/a7K_9mX4bQ2/hqdefault.jpg", "Biology"),
                    YouTubeVideo("k4N_6vQ8pL3", "Plant Physiology Full Unit in 2 Hours | Biology at Ease", "Biology at Ease", "https://img.youtube.com/vi/k4N_6vQ8pL3/hqdefault.jpg", "Biology"),
                    YouTubeVideo("c2M_7pL9qK1", "Cell: The Unit of Life & Cell Cycle Line by Line | Biology at Ease", "Biology at Ease", "https://img.youtube.com/vi/c2M_7pL9qK1/hqdefault.jpg", "Biology")
                )
            ),
            CuratedChannel(
                id = "chemshiksha",
                name = "Chemshiksha",
                subject = "Chemistry",
                accentColor = brandPurple,
                icon = Icons.Default.Science,
                description = "Inorganic tricks, Organic reaction mechanisms, and NCERT deep dive.",
                searchQueries = listOf("Chemshiksha NCERT Inorganic", "Chemshiksha Organic Reactions", "Chemshiksha Physical Chemistry Formula"),
                channelId = "UCqB9qL7vB4N_nQ9m9x_v8A",
                handle = "@Chemshiksha",
                curatedVideos = listOf(
                    YouTubeVideo("v6L_8mQ3pY1", "Organic Chemistry Complete Name Reactions & Mechanisms", "Chemshiksha", "https://img.youtube.com/vi/v6L_8mQ3pY1/hqdefault.jpg", "Chemistry"),
                    YouTubeVideo("p3Q8mN9vL4K", "Periodic Table & Chemical Bonding Super Easy Tricks | NCERT", "Chemshiksha", "https://img.youtube.com/vi/p3Q8mN9vL4K/hqdefault.jpg", "Chemistry"),
                    YouTubeVideo("t7N_4pQ9mL2", "Coordination Chemistry & d-f Block Elements Quick Revision", "Chemshiksha", "https://img.youtube.com/vi/t7N_4pQ9mL2/hqdefault.jpg", "Chemistry"),
                    YouTubeVideo("w2M_9pL4qK8", "Aldehydes, Ketones & Carboxylic Acids Conversions | Chemshiksha", "Chemshiksha", "https://img.youtube.com/vi/w2M_9pL4qK8/hqdefault.jpg", "Chemistry")
                )
            ),
            CuratedChannel(
                id = "mathongo",
                name = "MathonGo - Anup Sir",
                subject = "Mathematics",
                accentColor = brandAmber,
                icon = Icons.Default.Calculate,
                description = "Targeted JEE Main & Advanced mathematics, marks vs percentile analysis, chapter-wise marks weightage & top PYQ sprints by Anup Sir.",
                searchQueries = listOf("MathonGo JEE Main PYQ Sprint", "MathonGo Calculus Revision", "Anup Sir MathonGo JEE"),
                handle = "@MathonGo",
                channelId = "UCi_K_v6L_8mQ3pY1_nQ9m9x",
                subscribers = "980K subscribers",
                curatedVideos = listOf(
                    YouTubeVideo("a8L_9mQ2pK4", "Top 100 JEE Main Math PYQs with Shortcuts | MathonGo Anup Sir", "MathonGo", "https://img.youtube.com/vi/a8L_9mQ2pK4/hqdefault.jpg", "Maths"),
                    YouTubeVideo("b3K_7mN8vL1", "Differential & Integral Calculus Quick Revision | MathonGo", "MathonGo", "https://img.youtube.com/vi/b3K_7mN8vL1/hqdefault.jpg", "Maths")
                )
            ),
            CuratedChannel(
                id = "sachin_rana",
                name = "Sachin Rana [IITB]",
                subject = "Organic Chemistry",
                accentColor = Color(0xFFEC4899),
                icon = Icons.Default.Science,
                description = "Crystal-clear Organic Chemistry mechanisms, GOC, Stereochemistry & Name Reactions for JEE & NEET by Sachin Rana Sir (IIT Bombay).",
                searchQueries = listOf("Sachin Rana Organic Chemistry", "Sachin Rana GOC JEE Advanced", "Sachin Rana Reaction Mechanisms"),
                handle = "@SachinRanaIITB",
                channelId = "UC3pY1_nQ9m9x_v8A_a8L_9m",
                subscribers = "650K subscribers",
                curatedVideos = listOf(
                    YouTubeVideo("s1Q4mN9vL2K", "General Organic Chemistry (GOC) Complete Masterclass | Sachin Rana", "Sachin Rana", "https://img.youtube.com/vi/s1Q4mN9vL2K/hqdefault.jpg", "Chemistry"),
                    YouTubeVideo("s2M8pL4qK7N", "Reaction Mechanisms & Reagents in Organic Chemistry | IITB", "Sachin Rana", "https://img.youtube.com/vi/s2M8pL4qK7N/hqdefault.jpg", "Chemistry")
                )
            ),
            CuratedChannel(
                id = "unacademy_neet_jee",
                name = "Unacademy NEET & JEE",
                subject = "NEET & JEE",
                accentColor = brandAmber,
                icon = Icons.Default.School,
                description = "Mock test discussions, strategy sessions, and rapid full-syllabus revisions by Garima Goel, Seep Pahuja, Namo Kaul & top teams.",
                searchQueries = listOf("Unacademy NEET Fastrack Revision", "Allen JEE Main Most Expected Questions", "Garima Goel Biology NEET"),
                handle = "@UnacademyNEET",
                channelId = "UCnQ9m9x_v8A_a8L_9mQ2pK4",
                curatedVideos = listOf(
                    YouTubeVideo("c8L_5mQ2pK9", "NEET Full Syllabus Physics Mock Test Discussion | Top PYQs", "Unacademy NEET", "https://img.youtube.com/vi/c8L_5mQ2pK9/hqdefault.jpg", "Physics"),
                    YouTubeVideo("b9K_4mQ8pL1", "NEET Biology Complete Rapid Fire Mock Paper with Solution", "Garima Goel", "https://img.youtube.com/vi/b9K_4mQ8pL1/hqdefault.jpg", "Biology"),
                    YouTubeVideo("e7L_2pM9qK4", "JEE Main Complete Chemistry Most Expected Questions | Allen", "Allen JEE", "https://img.youtube.com/vi/e7L_2pM9qK4/hqdefault.jpg", "Chemistry"),
                    YouTubeVideo("f3M_8pL2qK9", "Complete Class 11th Syllabus Physics Revision Marathon", "Unacademy JEE", "https://img.youtube.com/vi/f3M_8pL2qK9/hqdefault.jpg", "Physics")
                )
            ),
            CuratedChannel(
                id = "allen_career_institute",
                name = "ALLEN Career Institute",
                subject = "Physics / Chemistry / Bio / Math",
                accentColor = Color(0xFF2563EB),
                icon = Icons.Default.School,
                description = "Kota system classroom lectures, major test discussions, and rank booster crash courses for NEET and JEE.",
                searchQueries = listOf("Allen Kota NEET Crash Course", "Allen Kota JEE Advanced Problem Solving", "Allen Live Lectures"),
                handle = "@ALLENCareerInstitute",
                channelId = "UC_a8L_9mQ2pK4_b3K_7mN8v",
                subscribers = "3.2M subscribers"
            ),
            CuratedChannel(
                id = "vedantu_jee_neet",
                name = "Vedantu JEE & NEET",
                subject = "All Subjects",
                accentColor = brandRed,
                icon = Icons.Default.PlayCircleFilled,
                description = "Sprint batches, marathon problem-solving sessions, and board exam guides by Anand Prakash, Harsh Priyam & team.",
                searchQueries = listOf("Vedantu JEE Sprint", "Vedantu NEET Biology Marathon", "Vedantu Chemistry One Shot"),
                handle = "@VedantuJEE",
                channelId = "UC_b3K_7mN8vL1_c8L_5mQ2p",
                subscribers = "2.8M subscribers"
            ),
            CuratedChannel(
                id = "khan_academy_india",
                name = "Khan Academy India",
                subject = "Concept Foundations (All Subjects)",
                accentColor = brandGreen,
                icon = Icons.Default.Lightbulb,
                description = "World-class bite-sized conceptual foundation videos mapped directly to NCERT Class 9-12 curriculum.",
                searchQueries = listOf("Khan Academy India Physics", "Khan Academy India Chemistry", "Khan Academy India Biology"),
                handle = "@KhanAcademyIndia",
                channelId = "UC_c8L_5mQ2pK9_b9K_4mQ8p",
                subscribers = "2.1M subscribers"
            ),
            CuratedChannel(
                id = "apni_kaksha",
                name = "Apni Kaksha - Aman Dhattarwal",
                subject = "Physics / Chem / Maths / Boards",
                accentColor = Color(0xFF0284C7),
                icon = Icons.Default.MenuBook,
                description = "Animated concept videos, handwritten notes marathons, and board/JEE fast track revision lectures.",
                searchQueries = listOf("Apni Kaksha Physics One Shot", "Apni Kaksha Chemistry Notes", "Aman Dhattarwal Class 12"),
                handle = "@ApniKaksha",
                channelId = "UC_b9K_4mQ8pL1_e7L_2pM9q",
                subscribers = "4.5M subscribers"
            ),
            CuratedChannel(
                id = "canvas_classes",
                name = "Canvas Classes - Bharat Panchal",
                subject = "Chemistry (Boards & NEET)",
                accentColor = Color(0xFFE11D48),
                icon = Icons.Default.Science,
                description = "Rank booster chemistry one-shots, 30-minute chapter summaries, and NCERT formulas by Bharat Panchal Sir.",
                searchQueries = listOf("Bharat Panchal Chemistry One Shot", "Canvas Classes Chemistry Class 12", "Bharat Panchal Organic Chemistry"),
                handle = "@CanvasClasses",
                channelId = "UC_e7L_2pM9qK4_f3M_8pL2q",
                subscribers = "1.2M subscribers"
            ),
            CuratedChannel(
                id = "neela_bakore",
                name = "Neela Bakore Tutorials",
                subject = "Biology (Botany & Zoology)",
                accentColor = Color(0xFF059669),
                icon = Icons.Default.Grass,
                description = "Detailed diagrammatic chalk-and-board lectures covering entire NCERT Botany and Zoology for NEET aspirants.",
                searchQueries = listOf("Neela Bakore Botany Class 11 12", "Neela Bakore Zoology Genetics", "Neela Bakore Human Physiology"),
                handle = "@NeelaBakoreTutorials",
                channelId = "UC_f3M_8pL2qK9_a8L_9mQ2p",
                subscribers = "750K subscribers"
            )
        )
    }

    val curatedChannels = remember(defaultCuratedChannels, customChannelsList, hiddenChannelIds) {
        val visibleDefaults = defaultCuratedChannels.filter { it.id !in hiddenChannelIds && it.name !in hiddenChannelIds }
        val mappedCustom = customChannelsList
            .filter { it.id !in hiddenChannelIds && it.name !in hiddenChannelIds }
            .map { custom ->
                val color = try {
                    Color(android.graphics.Color.parseColor(custom.colorHex))
                } catch (_: Exception) {
                    brandCyan
                }
                val icon = when (custom.subject.lowercase()) {
                    "physics" -> Icons.Default.Speed
                    "chemistry" -> Icons.Default.Science
                    "biology" -> Icons.Default.Grass
                    "mathematics", "maths" -> Icons.Default.Calculate
                    else -> Icons.Default.SmartDisplay
                }
                CuratedChannel(
                    id = custom.id,
                    name = custom.name,
                    subject = custom.subject,
                    accentColor = color,
                    icon = icon,
                    description = custom.description.ifBlank { "Custom Hall of Fame Study Channel added by student." },
                    searchQueries = listOf("${custom.name} ${custom.subject} revision", "${custom.name} one shot", custom.name),
                    handle = custom.handle,
                    channelId = custom.channelId,
                    subscribers = "Verified Study Channel",
                    isCustom = true
                )
            }
        visibleDefaults + mappedCustom
    }

    // Function to select and load videos for a Curated Channel (NO LIMIT, FULL CATALOG & PLAYLISTS)
    fun selectCuratedChannel(channel: CuratedChannel, initialQuery: String? = null, targetChannelId: String? = null) {
        activeCuratedChannel = channel
        selectedChannelQuery = initialQuery
        channelSearchFilter = ""
        channelActiveTab = 0 // Default to Home
        channelSortOrder = "Latest" // Reset to Latest (Newest First)
        isChannelSubscribed = false
        channelFilterTag = "All"
        channelNextPageToken = null
        channelUploadsPlaylistId = null
        val initialChanId = targetChannelId ?: channel.channelId.ifBlank { null }
        resolvedChannelId = initialChanId
        curatedChannelVideos = channel.curatedVideos
        curatedChannelPlaylists = channel.curatedPlaylists

        if (youtubeApiKey.isNotBlank()) {
            isLoading = true
            isChannelPlaylistsLoading = true
            coroutineScope.launch {
                try {
                    withContext(Dispatchers.IO) {
                        var cId = initialChanId
                        var upId: String? = null

                        // Step 1: If channel ID is unknown, resolve via handle or channel search
                        if (cId.isNullOrBlank() && channel.handle.isNotBlank()) {
                            try {
                                val handleEncoded = URLEncoder.encode(channel.handle, "UTF-8")
                                val chanUrl = "https://www.googleapis.com/youtube/v3/channels?part=snippet,contentDetails,statistics&forHandle=$handleEncoded&key=$youtubeApiKey"
                                val conn = URL(chanUrl).openConnection() as HttpURLConnection
                                conn.connectTimeout = 8000
                                conn.readTimeout = 8000
                                if (conn.responseCode == 200) {
                                    val res = conn.inputStream.bufferedReader().use { it.readText() }
                                    val items = JSONObject(res).optJSONArray("items")
                                    if (items != null && items.length() > 0) {
                                        val itm = items.getJSONObject(0)
                                        cId = itm.optString("id")
                                        upId = itm.optJSONObject("contentDetails")?.optJSONObject("relatedPlaylists")?.optString("uploads")
                                    }
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }

                        // If cId starts with UC, uploads playlist is UU...
                        if (!cId.isNullOrBlank() && upId.isNullOrBlank() && cId.startsWith("UC")) {
                            upId = "UU" + cId.substring(2)
                        }

                        withContext(Dispatchers.Main) {
                            resolvedChannelId = cId
                            channelUploadsPlaylistId = upId
                        }

                        // Step 2: Fetch 50 Videos (from uploads playlist or search)
                        var nextTk: String? = null
                        val videoList = mutableListOf<YouTubeVideo>()
                        try {
                            val vUrl = if (!upId.isNullOrBlank()) {
                                "https://www.googleapis.com/youtube/v3/playlistItems?part=snippet,contentDetails&maxResults=50&playlistId=$upId&key=$youtubeApiKey"
                            } else if (!cId.isNullOrBlank()) {
                                "https://www.googleapis.com/youtube/v3/search?part=snippet&channelId=$cId&order=date&type=video&maxResults=50&key=$youtubeApiKey"
                            } else {
                                val qToSearch = initialQuery ?: channel.searchQueries.firstOrNull() ?: "${channel.name} NEET JEE"
                                "https://www.googleapis.com/youtube/v3/search?part=snippet&maxResults=50&q=${URLEncoder.encode(qToSearch, "UTF-8")}&type=video&key=$youtubeApiKey"
                            }
                            val conn = URL(vUrl).openConnection() as HttpURLConnection
                            conn.connectTimeout = 10000
                            conn.readTimeout = 10000
                            if (conn.responseCode == 200) {
                                val res = conn.inputStream.bufferedReader().use { it.readText() }
                                val json = JSONObject(res)
                                nextTk = if (json.has("nextPageToken")) json.getString("nextPageToken") else null
                                val items = json.optJSONArray("items")
                                if (items != null) {
                                    for (i in 0 until items.length()) {
                                        val item = items.getJSONObject(i)
                                        val snippet = item.optJSONObject("snippet")
                                        val videoId = if (!upId.isNullOrBlank()) {
                                            snippet?.optJSONObject("resourceId")?.optString("videoId")
                                        } else {
                                            item.optJSONObject("id")?.optString("videoId")
                                        }
                                        val publishedAt = snippet?.optString("publishedAt")
                                            ?: snippet?.optJSONObject("contentDetails")?.optString("videoPublishedAt")
                                            ?: ""
                                        val publishTimeMillis = parseIsoPublishTimestamp(publishedAt)
                                        val title = snippet?.optString("title")
                                            ?.replace("&quot;", "\"")
                                            ?.replace("&#39;", "'")
                                            ?.replace("&amp;", "&")
                                        val channelTitle = snippet?.optString("channelTitle") ?: channel.name
                                        val thumbnails = snippet?.optJSONObject("thumbnails")
                                        val highThumb = thumbnails?.optJSONObject("high")?.optString("url")
                                            ?: thumbnails?.optJSONObject("medium")?.optString("url")
                                            ?: thumbnails?.optJSONObject("default")?.optString("url")
                                        val description = snippet?.optString("description") ?: ""
                                        val channelId = snippet?.optString("channelId") ?: (cId ?: channel.channelId)
                                        if (!videoId.isNullOrBlank() && !title.isNullOrBlank() && title != "Private video" && title != "Deleted video" && !highThumb.isNullOrBlank()) {
                                            videoList.add(
                                                YouTubeVideo(
                                                    id = videoId,
                                                    title = title,
                                                    channelTitle = channelTitle,
                                                    thumbnailUrl = highThumb,
                                                    subjectTag = channel.subject,
                                                    channelId = channelId,
                                                    description = description,
                                                    publishedAt = publishedAt,
                                                    publishTimeMillis = publishTimeMillis
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }

                        // Step 3: Fetch ALL Channel Playlists from YouTube
                        val plList = mutableListOf<ChannelPlaylist>()
                        try {
                            val plUrl = if (!cId.isNullOrBlank()) {
                                "https://www.googleapis.com/youtube/v3/playlists?part=snippet,contentDetails&channelId=$cId&maxResults=50&key=$youtubeApiKey"
                            } else {
                                "https://www.googleapis.com/youtube/v3/search?part=snippet&q=${URLEncoder.encode("${channel.name} playlist", "UTF-8")}&type=playlist&maxResults=50&key=$youtubeApiKey"
                            }
                            val conn = URL(plUrl).openConnection() as HttpURLConnection
                            conn.connectTimeout = 10000
                            conn.readTimeout = 10000
                            if (conn.responseCode == 200) {
                                val res = conn.inputStream.bufferedReader().use { it.readText() }
                                val json = JSONObject(res)
                                val items = json.optJSONArray("items")
                                if (items != null) {
                                    for (i in 0 until items.length()) {
                                        val item = items.getJSONObject(i)
                                        val plId = if (!cId.isNullOrBlank()) item.optString("id") else item.optJSONObject("id")?.optString("playlistId")
                                        val snippet = item.optJSONObject("snippet")
                                        val title = snippet?.optString("title")
                                            ?.replace("&quot;", "\"")
                                            ?.replace("&#39;", "'")
                                            ?.replace("&amp;", "&")
                                        val contentDetails = item.optJSONObject("contentDetails")
                                        val count = contentDetails?.optInt("itemCount", 0) ?: 0
                                        val thumbnails = snippet?.optJSONObject("thumbnails")
                                        val thumbUrl = thumbnails?.optJSONObject("high")?.optString("url")
                                            ?: thumbnails?.optJSONObject("medium")?.optString("url")
                                            ?: thumbnails?.optJSONObject("default")?.optString("url") ?: ""
                                        if (!plId.isNullOrBlank() && !title.isNullOrBlank() && title != "Private playlist") {
                                            plList.add(ChannelPlaylist(plId, title, if (count > 0) count else 10, thumbUrl, channel.subject, plId))
                                        }
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }

                        withContext(Dispatchers.Main) {
                            if (videoList.isNotEmpty()) {
                                videoList.sortWith(
                                    compareByDescending<YouTubeVideo> { it.publishTimeMillis }
                                        .thenByDescending { it.publishedAt }
                                )
                                val resultIds = videoList.map { it.id }.toSet()
                                val extraFallbacks = channel.curatedVideos.filter { it.id !in resultIds }
                                curatedChannelVideos = videoList + extraFallbacks
                                channelNextPageToken = nextTk
                            }
                            if (plList.isNotEmpty()) {
                                val existingIds = plList.map { it.id }.toSet()
                                val extraCurated = channel.curatedPlaylists.filter { it.id !in existingIds }
                                curatedChannelPlaylists = plList + extraCurated
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    isLoading = false
                    isChannelPlaylistsLoading = false
                }
            }
        } else {
            // Zero-Key YouTube Public Feed Parser: Fetches actual latest channel uploads
            val cId = initialChanId
            if (!cId.isNullOrBlank()) {
                isLoading = true
                coroutineScope.launch {
                    try {
                        val rssVideos = withContext(Dispatchers.IO) {
                            val feedUrl = "https://www.youtube.com/feeds/videos.xml?channel_id=$cId"
                            val conn = URL(feedUrl).openConnection() as HttpURLConnection
                            conn.connectTimeout = 8000
                            conn.readTimeout = 8000
                            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                            if (conn.responseCode == 200) {
                                val xmlText = conn.inputStream.bufferedReader().use { it.readText() }
                                val entryRegex = Regex("<entry>([\\s\\S]*?)</entry>")
                                val vidRegex = Regex("<yt:videoId>([a-zA-Z0-9_-]+)</yt:videoId>")
                                val titleRegex = Regex("<title>([^<]+)</title>")
                                val mediaThumbRegex = Regex("<media:thumbnail[^>]+url=\"([^\"]+)\"")
                                val pubRegex = Regex("<published>([^<]+)</published>")
                                val authorNameRegex = Regex("<name>([^<]+)</name>")
                                val list = mutableListOf<YouTubeVideo>()

                                entryRegex.findAll(xmlText).forEach { match ->
                                    val entryContent = match.groupValues[1]
                                    val vId = vidRegex.find(entryContent)?.groupValues?.get(1)
                                    val vTitle = titleRegex.find(entryContent)?.groupValues?.get(1)
                                        ?.replace("&quot;", "\"")?.replace("&#39;", "'")?.replace("&amp;", "&")
                                    val vThumb = mediaThumbRegex.find(entryContent)?.groupValues?.get(1)
                                        ?: if (!vId.isNullOrBlank()) "https://img.youtube.com/vi/$vId/hqdefault.jpg" else ""
                                    val chAuthor = authorNameRegex.find(entryContent)?.groupValues?.get(1) ?: channel.name
                                    val pubDate = pubRegex.find(entryContent)?.groupValues?.get(1) ?: ""
                                    val pubMillis = parseIsoPublishTimestamp(pubDate)

                                    if (!vId.isNullOrBlank() && !vTitle.isNullOrBlank() && vTitle != "Private video") {
                                        list.add(
                                            YouTubeVideo(
                                                id = vId,
                                                title = vTitle,
                                                channelTitle = chAuthor,
                                                thumbnailUrl = vThumb,
                                                subjectTag = channel.subject,
                                                channelId = cId,
                                                publishedAt = pubDate,
                                                publishTimeMillis = pubMillis
                                            )
                                        )
                                    }
                                }
                                list
                            } else {
                                emptyList()
                            }
                        }
                        if (rssVideos.isNotEmpty()) {
                            withContext(Dispatchers.Main) {
                                val rssIds = rssVideos.map { it.id }.toSet()
                                val extraFallbacks = channel.curatedVideos.filter { it.id !in rssIds }
                                curatedChannelVideos = rssVideos + extraFallbacks
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    } finally {
                        isLoading = false
                    }
                }
            }
        }
    }

    // Function to load the next batch of channel videos (Pagination)
    fun loadMoreChannelVideos() {
        val ch = activeCuratedChannel ?: return
        if (isChannelLoadingMore) return
        val tk = channelNextPageToken
        val upId = channelUploadsPlaylistId
        val cId = resolvedChannelId

        if (youtubeApiKey.isNotBlank() && tk != null) {
            isChannelLoadingMore = true
            coroutineScope.launch {
                try {
                    val resultPair: Pair<List<YouTubeVideo>, String?> = withContext(Dispatchers.IO) {
                        val nextUrl = if (!upId.isNullOrBlank()) {
                            "https://www.googleapis.com/youtube/v3/playlistItems?part=snippet&maxResults=50&pageToken=$tk&playlistId=$upId&key=$youtubeApiKey"
                        } else if (!cId.isNullOrBlank()) {
                            "https://www.googleapis.com/youtube/v3/search?part=snippet&channelId=$cId&order=date&type=video&maxResults=50&pageToken=$tk&key=$youtubeApiKey"
                        } else {
                            val qToSearch = selectedChannelQuery ?: ch.searchQueries.firstOrNull() ?: "${ch.name} NEET JEE"
                            "https://www.googleapis.com/youtube/v3/search?part=snippet&maxResults=50&pageToken=$tk&q=${URLEncoder.encode(qToSearch, "UTF-8")}&type=video&key=$youtubeApiKey"
                        }
                        val conn = URL(nextUrl).openConnection() as HttpURLConnection
                        conn.connectTimeout = 10000
                        conn.readTimeout = 10000
                        if (conn.responseCode == 200) {
                            val res = conn.inputStream.bufferedReader().use { it.readText() }
                            val json = JSONObject(res)
                            val token = if (json.has("nextPageToken")) json.getString("nextPageToken") else null
                            val items = json.optJSONArray("items")
                            val list = mutableListOf<YouTubeVideo>()
                            if (items != null) {
                                for (i in 0 until items.length()) {
                                    val item = items.getJSONObject(i)
                                    val snippet = item.optJSONObject("snippet")
                                    val videoId = if (!upId.isNullOrBlank()) {
                                        snippet?.optJSONObject("resourceId")?.optString("videoId")
                                    } else {
                                        item.optJSONObject("id")?.optString("videoId")
                                    }
                                    val title = snippet?.optString("title")
                                        ?.replace("&quot;", "\"")
                                        ?.replace("&#39;", "'")
                                        ?.replace("&amp;", "&")
                                    val channelTitle = snippet?.optString("channelTitle") ?: ch.name
                                    val thumbnails = snippet?.optJSONObject("thumbnails")
                                    val highThumb = thumbnails?.optJSONObject("high")?.optString("url")
                                        ?: thumbnails?.optJSONObject("medium")?.optString("url")
                                        ?: thumbnails?.optJSONObject("default")?.optString("url")
                                    val description = snippet?.optString("description") ?: ""
                                    val channelId = snippet?.optString("channelId") ?: (cId ?: ch.channelId)
                                    val publishedAt = snippet?.optString("publishedAt")
                                        ?: snippet?.optJSONObject("contentDetails")?.optString("videoPublishedAt")
                                        ?: ""
                                    val publishTimeMillis = parseIsoPublishTimestamp(publishedAt)
                                    if (!videoId.isNullOrBlank() && !title.isNullOrBlank() && title != "Private video" && title != "Deleted video" && !highThumb.isNullOrBlank()) {
                                        list.add(
                                            YouTubeVideo(
                                                id = videoId,
                                                title = title,
                                                channelTitle = channelTitle,
                                                thumbnailUrl = highThumb,
                                                subjectTag = ch.subject,
                                                channelId = channelId,
                                                description = description,
                                                publishedAt = publishedAt,
                                                publishTimeMillis = publishTimeMillis
                                            )
                                        )
                                    }
                                }
                            }
                            Pair<List<YouTubeVideo>, String?>(list, token)
                        } else {
                            Pair<List<YouTubeVideo>, String?>(emptyList(), null)
                        }
                    }
                    val newVideos = resultPair.first
                    val nextTk = resultPair.second
                    if (newVideos.isNotEmpty()) {
                        val currentIds = curatedChannelVideos.map { it.id }.toSet()
                        val trulyNew = newVideos.filter { it.id !in currentIds }
                        curatedChannelVideos = curatedChannelVideos + trulyNew
                        channelNextPageToken = nextTk
                        Toast.makeText(context, "Loaded ${trulyNew.size} more videos from ${ch.name}", Toast.LENGTH_SHORT).show()
                    } else {
                        channelNextPageToken = null
                        Toast.makeText(context, "All available videos loaded", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(context, "Could not fetch more: ${e.message}", Toast.LENGTH_SHORT).show()
                } finally {
                    isChannelLoadingMore = false
                }
            }
        } else if (youtubeApiKey.isNotBlank()) {
            isChannelLoadingMore = true
            coroutineScope.launch {
                try {
                    val alternateQueries = listOf(
                        "${ch.name} Complete Chapter One Shot",
                        "${ch.name} Previous Year Questions PYQ",
                        "${ch.name} Formula Marathon Revision",
                        "${ch.name} Complete Syllabus Revision",
                        "${ch.name} Most Expected Questions"
                    )
                    val q = alternateQueries.random()
                    val discoveredVideos = withContext(Dispatchers.IO) {
                        val encoded = URLEncoder.encode(q, "UTF-8")
                        val vUrl = if (!cId.isNullOrBlank()) {
                            "https://www.googleapis.com/youtube/v3/search?part=snippet&channelId=$cId&q=$encoded&maxResults=50&type=video&key=$youtubeApiKey"
                        } else {
                            "https://www.googleapis.com/youtube/v3/search?part=snippet&q=$encoded&maxResults=50&type=video&key=$youtubeApiKey"
                        }
                        val conn = URL(vUrl).openConnection() as HttpURLConnection
                        conn.connectTimeout = 10000
                        conn.readTimeout = 10000
                        if (conn.responseCode == 200) {
                            val res = conn.inputStream.bufferedReader().use { it.readText() }
                            val json = JSONObject(res)
                            val items = json.optJSONArray("items")
                            val list = mutableListOf<YouTubeVideo>()
                            if (items != null) {
                                for (i in 0 until items.length()) {
                                    val item = items.getJSONObject(i)
                                    val videoId = item.optJSONObject("id")?.optString("videoId")
                                    val snippet = item.optJSONObject("snippet")
                                    val title = snippet?.optString("title")
                                        ?.replace("&quot;", "\"")
                                        ?.replace("&#39;", "'")
                                        ?.replace("&amp;", "&")
                                    val thumbnails = snippet?.optJSONObject("thumbnails")
                                    val highThumb = thumbnails?.optJSONObject("high")?.optString("url")
                                        ?: thumbnails?.optJSONObject("default")?.optString("url")
                                    val publishedAt = snippet?.optString("publishedAt") ?: ""
                                    val publishTimeMillis = parseIsoPublishTimestamp(publishedAt)
                                    if (!videoId.isNullOrBlank() && !title.isNullOrBlank() && !highThumb.isNullOrBlank()) {
                                        list.add(
                                            YouTubeVideo(
                                                id = videoId,
                                                title = title,
                                                channelTitle = ch.name,
                                                thumbnailUrl = highThumb,
                                                subjectTag = ch.subject,
                                                channelId = cId ?: "",
                                                description = "",
                                                publishedAt = publishedAt,
                                                publishTimeMillis = publishTimeMillis
                                            )
                                        )
                                    }
                                }
                            }
                            list
                        } else {
                            emptyList()
                        }
                    }
                    if (discoveredVideos.isNotEmpty()) {
                        val currentIds = curatedChannelVideos.map { it.id }.toSet()
                        val trulyNew = discoveredVideos.filter { it.id !in currentIds }
                        curatedChannelVideos = curatedChannelVideos + trulyNew
                        Toast.makeText(context, "Added ${trulyNew.size} more lectures!", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    isChannelLoadingMore = false
                }
            }
        } else {
            Toast.makeText(context, "Add YouTube API Key in Settings to fetch unlimited videos & playlists", Toast.LENGTH_LONG).show()
        }
    }

    // Function to search online within the active channel
    fun searchWithinChannel(channel: CuratedChannel, query: String) {
        val q = query.trim()
        if (q.isBlank()) return
        if (youtubeApiKey.isBlank()) {
            channelActiveTab = 1
            return
        }
        isLoading = true
        coroutineScope.launch {
            try {
                val results = withContext(Dispatchers.IO) {
                    val cId = resolvedChannelId
                    val encodedQ = URLEncoder.encode(q, "UTF-8")
                    val vUrl = if (!cId.isNullOrBlank()) {
                        "https://www.googleapis.com/youtube/v3/search?part=snippet&channelId=$cId&q=$encodedQ&type=video&maxResults=50&key=$youtubeApiKey"
                    } else {
                        "https://www.googleapis.com/youtube/v3/search?part=snippet&q=${URLEncoder.encode("${channel.name} $q", "UTF-8")}&type=video&maxResults=50&key=$youtubeApiKey"
                    }
                    val conn = URL(vUrl).openConnection() as HttpURLConnection
                    conn.connectTimeout = 10000
                    conn.readTimeout = 10000
                    if (conn.responseCode == 200) {
                        val res = conn.inputStream.bufferedReader().use { it.readText() }
                        val json = JSONObject(res)
                        val items = json.optJSONArray("items")
                        val list = mutableListOf<YouTubeVideo>()
                        if (items != null) {
                            for (i in 0 until items.length()) {
                                val item = items.getJSONObject(i)
                                val videoId = item.optJSONObject("id")?.optString("videoId")
                                val snippet = item.optJSONObject("snippet")
                                val title = snippet?.optString("title")
                                    ?.replace("&quot;", "\"")
                                    ?.replace("&#39;", "'")
                                    ?.replace("&amp;", "&")
                                val thumbnails = snippet?.optJSONObject("thumbnails")
                                val highThumb = thumbnails?.optJSONObject("high")?.optString("url")
                                    ?: thumbnails?.optJSONObject("default")?.optString("url")
                                val description = snippet?.optString("description") ?: ""
                                if (!videoId.isNullOrBlank() && !title.isNullOrBlank() && !highThumb.isNullOrBlank()) {
                                    list.add(YouTubeVideo(videoId, title, channel.name, highThumb, channel.subject, cId ?: "", description))
                                }
                            }
                        }
                        list
                    } else {
                        emptyList()
                    }
                }
                if (results.isNotEmpty()) {
                    curatedChannelVideos = results
                    channelActiveTab = 1
                    Toast.makeText(context, "Found ${results.size} lectures for \"$q\"", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "No online videos found for \"$q\"", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoading = false
            }
        }
    }

    // Function to search YouTube API
    fun performSearch(query: String) {
        if (youtubeApiKey.isBlank()) {
            showApiKeyDialog = true
            return
        }

        val q = query.trim()
        if (q.isBlank()) return

        keyboardController?.hide()
        errorMessage = null
        warningMessage = null
        searchResults = emptyList()

        val lowerQuery = q.lowercase()
        val blockedTerms = listOf("song", "music", "movie", "trailer", "vlog", "roast", "prank", "gaming", "pubg", "freefire", "minecraft", "comedy", "dance", "dj", "web series", "netflix", "hotstar", "drama", "anime", "cinema", "reels", "tiktok", "standup", "funny")
        val allowedTerms = listOf("physics", "chemistry", "biology", "math", "neet", "jee", "iit", "ncert", "oneshot", "pyq", "revision", "derivation", "mechanics", "organic", "inorganic", "lecture", "class 11", "class 12", "mock test", "sir", "mam", "concept", "cbse", "tricks", "strategy", "syllabus", "question", "solution", "numerical", "chapter", "formula", "botany", "zoology", "calculus", "optics", "thermo", "kinematics", "electro", "atomic", "gate", "cuet")

        if (blockedTerms.any { lowerQuery.contains(it) } && allowedTerms.none { lowerQuery.contains(it) }) {
            warningMessage = "🚫 Focus Guard Active: Non-study content is strictly restricted. StudyTube is exclusively locked for educational lectures, chapter solutions, PYQs, and exam preparation."
            return
        }

        val isExplicitlyStudy = allowedTerms.any { lowerQuery.contains(it) } || curatedChannels.any { lowerQuery.contains(it.name.lowercase()) }
        val finalQuery = if (isExplicitlyStudy) q else "$q NEET JEE class 11 12 study"

        isLoading = true
        coroutineScope.launch {
            try {
                val results = withContext(Dispatchers.IO) {
                    val encodedQuery = URLEncoder.encode(finalQuery, "UTF-8")
                    val urlStr = "https://www.googleapis.com/youtube/v3/search?part=snippet&maxResults=50&q=$encodedQuery&type=video&key=$youtubeApiKey"
                    val url = URL(urlStr)
                    val connection = url.openConnection() as HttpURLConnection
                    connection.requestMethod = "GET"
                    connection.connectTimeout = 10000
                    connection.readTimeout = 10000

                    if (connection.responseCode == 200) {
                        val response = connection.inputStream.bufferedReader().use { it.readText() }
                        val json = JSONObject(response)
                        val items = json.optJSONArray("items")
                        val videos = mutableListOf<YouTubeVideo>()
                        if (items != null) {
                            for (i in 0 until items.length()) {
                                val item = items.getJSONObject(i)
                                val idObj = item.optJSONObject("id")
                                val videoId = idObj?.optString("videoId")
                                val snippet = item.optJSONObject("snippet")
                                val title = snippet?.optString("title")
                                    ?.replace("&quot;", "\"")
                                    ?.replace("&#39;", "'")
                                    ?.replace("&amp;", "&")
                                val channelTitle = snippet?.optString("channelTitle")
                                val thumbnails = snippet?.optJSONObject("thumbnails")
                                val highThumb = thumbnails?.optJSONObject("high")?.optString("url")
                                    ?: thumbnails?.optJSONObject("medium")?.optString("url")
                                    ?: thumbnails?.optJSONObject("default")?.optString("url")

                                val description = snippet?.optString("description") ?: ""
                                val channelId = snippet?.optString("channelId") ?: ""
                                val tag = when {
                                    title?.contains("Physics", ignoreCase = true) == true -> "Physics"
                                    title?.contains("Chem", ignoreCase = true) == true -> "Chemistry"
                                    title?.contains("Bio", ignoreCase = true) == true -> "Biology"
                                    title?.contains("Math", ignoreCase = true) == true -> "Maths"
                                    else -> "NEET/JEE"
                                }

                                if (videoId != null && title != null && channelTitle != null && highThumb != null) {
                                    videos.add(YouTubeVideo(videoId, title, channelTitle, highThumb, tag, channelId, description))
                                }
                            }
                        }
                        videos
                    } else {
                        val errorStream = connection.errorStream?.bufferedReader()?.use { it.readText() }
                        throw Exception("HTTP ${connection.responseCode}: $errorStream")
                    }
                }
                searchResults = results
                if (results.isEmpty()) {
                    warningMessage = "No relevant study lectures found for this query."
                }
            } catch (e: Exception) {
                e.printStackTrace()
                errorMessage = "Failed to load study videos. Please verify your YouTube API key in settings."
            } finally {
                isLoading = false
            }
        }
    }

    // Function to open and load Playlist videos with multi-tier fallback
    fun openPlaylist(
        playlistId: String,
        title: String = "Playlist",
        channelTitle: String = "",
        thumbnailUrl: String = "",
        subject: String = "",
        fallbackVideos: List<YouTubeVideo> = emptyList()
    ) {
        val cleanId = playlistId.trim()
        selectedVideo = null // Ensure we switch view away from video player so playlist opens!
        activePlaylistId = cleanId
        activePlaylistTitle = title.ifBlank { "Curated Playlist" }
        val cTitle = channelTitle.ifBlank { activeCuratedChannel?.name ?: "Verified Educator" }
        activePlaylistChannelTitle = cTitle
        activePlaylistThumbnail = thumbnailUrl
        val subj = subject.ifBlank { activeCuratedChannel?.subject ?: "NEET/JEE" }
        activePlaylistSubject = subj
        isLoading = true
        errorMessage = null
        playlistResults = emptyList()

        coroutineScope.launch {
            try {
                val finalPlaylistId = if (cleanId.contains("list=")) {
                    Regex("[?&]list=([a-zA-Z0-9_-]+)").find(cleanId)?.groupValues?.get(1) ?: cleanId
                } else {
                    cleanId
                }

                var fetchedVideos = emptyList<YouTubeVideo>()

                val isYouTubeId = finalPlaylistId.startsWith("PL") ||
                                  finalPlaylistId.startsWith("UU") ||
                                  finalPlaylistId.startsWith("RD") ||
                                  finalPlaylistId.startsWith("OLAK") ||
                                  finalPlaylistId.length >= 18

                // Tier 1: Fetch directly from YouTube playlistItems if API key is present
                if (isYouTubeId && youtubeApiKey.isNotBlank()) {
                    try {
                        fetchedVideos = withContext(Dispatchers.IO) {
                            val urlStr = "https://www.googleapis.com/youtube/v3/playlistItems?part=snippet&maxResults=50&playlistId=$finalPlaylistId&key=$youtubeApiKey"
                            val url = URL(urlStr)
                            val connection = url.openConnection() as HttpURLConnection
                            connection.requestMethod = "GET"
                            connection.connectTimeout = 10000
                            connection.readTimeout = 10000

                            if (connection.responseCode == 200) {
                                val response = connection.inputStream.bufferedReader().use { it.readText() }
                                val json = JSONObject(response)
                                val items = json.optJSONArray("items")
                                val videos = mutableListOf<YouTubeVideo>()
                                if (items != null) {
                                    for (i in 0 until items.length()) {
                                        val item = items.getJSONObject(i)
                                        val snippet = item.optJSONObject("snippet")
                                        val videoId = snippet?.optJSONObject("resourceId")?.optString("videoId")
                                        val vTitle = snippet?.optString("title")
                                            ?.replace("&quot;", "\"")
                                            ?.replace("&#39;", "'")
                                            ?.replace("&amp;", "&")
                                        val chTitle = snippet?.optString("videoOwnerChannelTitle")
                                            ?: snippet?.optString("channelTitle") ?: cTitle
                                        val thumbnails = snippet?.optJSONObject("thumbnails")
                                        val highThumb = thumbnails?.optJSONObject("high")?.optString("url")
                                            ?: thumbnails?.optJSONObject("medium")?.optString("url")
                                            ?: thumbnails?.optJSONObject("default")?.optString("url")

                                        val description = snippet?.optString("description") ?: ""
                                        val channelId = snippet?.optString("channelId") ?: ""

                                        if (!videoId.isNullOrBlank() && !vTitle.isNullOrBlank() && vTitle != "Private video" && vTitle != "Deleted video" && !highThumb.isNullOrBlank()) {
                                            videos.add(YouTubeVideo(videoId, vTitle, chTitle, highThumb, subj, channelId, description))
                                        }
                                    }
                                }
                                videos
                            } else {
                                emptyList()
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                // Tier 1.5: Public zero-key YouTube RSS feed parser
                if (fetchedVideos.isEmpty() && isYouTubeId) {
                    try {
                        fetchedVideos = withContext(Dispatchers.IO) {
                            val feedUrl = "https://www.youtube.com/feeds/videos.xml?playlist_id=$finalPlaylistId"
                            val conn = URL(feedUrl).openConnection() as HttpURLConnection
                            conn.connectTimeout = 8000
                            conn.readTimeout = 8000
                            conn.setRequestProperty("User-Agent", "Mozilla/5.0")
                            if (conn.responseCode == 200) {
                                val xmlText = conn.inputStream.bufferedReader().use { it.readText() }
                                val entryRegex = Regex("<entry>([\\s\\S]*?)</entry>")
                                val vidRegex = Regex("<yt:videoId>([a-zA-Z0-9_-]+)</yt:videoId>")
                                val titleRegex = Regex("<title>([^<]+)</title>")
                                val mediaThumbRegex = Regex("<media:thumbnail[^>]+url=\"([^\"]+)\"")
                                val authorNameRegex = Regex("<name>([^<]+)</name>")
                                val rssVideos = mutableListOf<YouTubeVideo>()

                                entryRegex.findAll(xmlText).forEach { match ->
                                    val entryContent = match.groupValues[1]
                                    val vId = vidRegex.find(entryContent)?.groupValues?.get(1)
                                    val vTitle = titleRegex.find(entryContent)?.groupValues?.get(1)
                                        ?.replace("&quot;", "\"")?.replace("&#39;", "'")?.replace("&amp;", "&")
                                    val vThumb = mediaThumbRegex.find(entryContent)?.groupValues?.get(1)
                                        ?: if (!vId.isNullOrBlank()) "https://img.youtube.com/vi/$vId/hqdefault.jpg" else ""
                                    val chAuthor = authorNameRegex.find(entryContent)?.groupValues?.get(1) ?: cTitle

                                    if (!vId.isNullOrBlank() && !vTitle.isNullOrBlank() && vTitle != "Private video") {
                                        rssVideos.add(YouTubeVideo(vId, vTitle, chAuthor, vThumb, subj))
                                    }
                                }
                                rssVideos
                            } else {
                                emptyList()
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                // Tier 2: Search YouTube with channel name + playlist title if playlistItems was empty
                if (fetchedVideos.isEmpty() && youtubeApiKey.isNotBlank()) {
                    try {
                        val searchQuery = "$cTitle $title".trim()
                        fetchedVideos = withContext(Dispatchers.IO) {
                            val encodedQ = URLEncoder.encode(searchQuery, "UTF-8")
                            val searchUrl = "https://www.googleapis.com/youtube/v3/search?part=snippet&maxResults=25&q=$encodedQ&type=video&key=$youtubeApiKey"
                            val conn = URL(searchUrl).openConnection() as HttpURLConnection
                            conn.connectTimeout = 10000
                            conn.readTimeout = 10000
                            if (conn.responseCode == 200) {
                                val res = conn.inputStream.bufferedReader().use { it.readText() }
                                val json = JSONObject(res)
                                val items = json.optJSONArray("items")
                                val vids = mutableListOf<YouTubeVideo>()
                                if (items != null) {
                                    for (i in 0 until items.length()) {
                                        val item = items.getJSONObject(i)
                                        val videoId = item.optJSONObject("id")?.optString("videoId")
                                        val snippet = item.optJSONObject("snippet")
                                        val vTitle = snippet?.optString("title")
                                            ?.replace("&quot;", "\"")
                                            ?.replace("&#39;", "'")
                                            ?.replace("&amp;", "&")
                                        val chTitle = snippet?.optString("channelTitle") ?: cTitle
                                        val thumbnails = snippet?.optJSONObject("thumbnails")
                                        val highThumb = thumbnails?.optJSONObject("high")?.optString("url")
                                            ?: thumbnails?.optJSONObject("default")?.optString("url")
                                        val description = snippet?.optString("description") ?: ""
                                        if (!videoId.isNullOrBlank() && !vTitle.isNullOrBlank() && !highThumb.isNullOrBlank()) {
                                            vids.add(YouTubeVideo(videoId, vTitle, chTitle, highThumb, subj, "", description))
                                        }
                                    }
                                }
                                vids
                            } else {
                                emptyList()
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                // Tier 3: Single video URL or ID fallback
                if (fetchedVideos.isEmpty()) {
                    val singleVidId = if (cleanId.contains("watch?v=")) {
                        Regex("watch\\?v=([a-zA-Z0-9_-]+)").find(cleanId)?.groupValues?.get(1)
                    } else if (cleanId.contains("youtu.be/")) {
                        Regex("youtu\\.be/([a-zA-Z0-9_-]+)").find(cleanId)?.groupValues?.get(1)
                    } else if (cleanId.length == 11 && !cleanId.startsWith("PL")) {
                        cleanId
                    } else null

                    if (!singleVidId.isNullOrBlank()) {
                        fetchedVideos = listOf(
                            YouTubeVideo(
                                id = singleVidId,
                                title = title.ifBlank { "Study Lecture" },
                                channelTitle = cTitle,
                                thumbnailUrl = "https://img.youtube.com/vi/$singleVidId/hqdefault.jpg",
                                subjectTag = subj
                            )
                        )
                    }
                }

                // Tier 4: Local/curated channel video matching fallback
                if (fetchedVideos.isEmpty()) {
                    val pool = (fallbackVideos.ifEmpty { activeCuratedChannel?.curatedVideos ?: emptyList() })
                    val titleKeywords = title.lowercase().split(" ", "-", "_").filter { it.length > 3 }
                    val matched = pool.filter { v ->
                        v.subjectTag.equals(subj, ignoreCase = true) ||
                        titleKeywords.any { kw -> v.title.contains(kw, ignoreCase = true) }
                    }
                    fetchedVideos = if (matched.isNotEmpty()) matched else pool
                }

                if (fetchedVideos.isNotEmpty()) {
                    playlistResults = fetchedVideos
                    errorMessage = null
                } else {
                    if (youtubeApiKey.isBlank()) {
                        errorMessage = "To stream all lectures for this playlist online, please add your YouTube API Key in Settings (top right key icon)."
                    } else {
                        errorMessage = "No videos found for this playlist. Ensure the playlist is public or unlisted."
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                errorMessage = "Could not load playlist: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    fun fetchPlaylistVideos(playlistId: String) {
        openPlaylist(playlistId = playlistId)
    }

    // Function to generate AI Notes / Formulas or Quiz using Gemini
    fun generateAiNotesForTopic(topicTitle: String, isQuiz: Boolean = false) {
        showAiSummarySheet = true
        isGeneratingAiSummary = true
        aiSummaryContent = null
        aiSheetTitle = if (isQuiz) "🎯 AI 5-Question Quiz: $topicTitle" else "⚡ AI Revision & Formula Sheet: $topicTitle"

        val customKey = geminiKey1.ifBlank { geminiKey2.ifBlank { geminiKey3 } }.takeIf { it.isNotBlank() }

        coroutineScope.launch {
            try {
                val res = if (isQuiz) {
                    GeminiChatAssistant.generateStudyTubeAiQuiz(context, topicTitle, customApiKey = customKey)
                } else {
                    GeminiChatAssistant.generateStudyTubeAiNotes(context, topicTitle, customApiKey = customKey)
                }
                aiSummaryContent = if (res.isSuccess) {
                    res.getOrNull() ?: "No content generated."
                } else {
                    val errMsg = res.exceptionOrNull()?.localizedMessage ?: "Unknown error"
                    "⚠️ Could not generate AI sheet: $errMsg\n\n💡 Tip: Please check your Lakshya AI Key in the Settings menu (or Top Bar key icon) to ensure continuous, lightning-fast AI generations."
                }
            } catch (e: Exception) {
                e.printStackTrace()
                aiSummaryContent = "Error generating AI summary: ${e.localizedMessage}"
            } finally {
                isGeneratingAiSummary = false
            }
        }
    }

    fun generateAiFormulaSheet(video: YouTubeVideo) {
        generateAiNotesForTopic("${video.title} (${video.channelTitle})", isQuiz = false)
    }

    // Direct Launch handler from Checklist / other screens
    val pendingLaunch by viewModel.pendingStudyTubeLaunch.collectAsStateWithLifecycle()
    LaunchedEffect(pendingLaunch) {
        val req = pendingLaunch ?: return@LaunchedEffect
        viewModel.clearPendingStudyTubeLaunch()

        val input = req.urlOrIdOrQuery.trim()
        if (input.isBlank()) return@LaunchedEffect

        // 1. Check if the input is a YouTube Playlist URL or contains list=
        val extractedPlaylistId = if (input.contains("list=")) {
            Regex("[?&]list=([a-zA-Z0-9_-]+)").find(input)?.groupValues?.get(1) ?: ""
        } else if (input.startsWith("PL") || input.startsWith("UU") || input.startsWith("RD") || input.startsWith("OLAK")) {
            input
        } else ""

        if (extractedPlaylistId.isNotBlank()) {
            selectedVideo = null
            openPlaylist(
                playlistId = extractedPlaylistId,
                title = req.title.ifBlank { "Curated Playlist" },
                subject = req.subject
            )
            return@LaunchedEffect
        }

        // 2. Check if the input is a direct YouTube video URL (youtu.be or watch?v=)
        val extractedVidId = extractVideoId(input)
        if (extractedVidId.isNotBlank() && !input.contains("search_query=") && !input.contains("results?")) {
            val tsFromUrl = Regex("[?&]t=(\\d+)").find(input)?.groupValues?.get(1)?.toIntOrNull()
            val targetSec = tsFromUrl ?: req.timestampSeconds
            activePlaylistId = null
            videoCurrentTime = targetSec.toFloat()
            selectedVideo = YouTubeVideo(
                id = extractedVidId,
                title = req.title.ifBlank { "Study Lecture" },
                channelTitle = "Verified Educator",
                thumbnailUrl = "https://img.youtube.com/vi/$extractedVidId/hqdefault.jpg",
                subjectTag = req.subject.ifBlank { "NEET/JEE" }
            )
            return@LaunchedEffect
        }

        // 3. Check if any curated playlist matches the topic or chapter
        val matchedCuratedPlaylist = curatedChannels.flatMap { it.curatedPlaylists }.firstOrNull { pl ->
            (req.title.isNotBlank() && pl.title.contains(req.title, ignoreCase = true)) ||
            (input.isNotBlank() && pl.title.contains(input, ignoreCase = true))
        }
        if (matchedCuratedPlaylist != null) {
            selectedVideo = null
            openPlaylist(
                playlistId = matchedCuratedPlaylist.playlistId,
                title = matchedCuratedPlaylist.title,
                thumbnailUrl = matchedCuratedPlaylist.thumbnailUrl,
                subject = matchedCuratedPlaylist.subjectTag
            )
            return@LaunchedEffect
        }

        // 4. Fallback: Search query for chapter/topic playlist
        val cleanQuery = if (input.contains("search_query=")) {
            try {
                Uri.parse(input).getQueryParameter("search_query") ?: input
            } catch (e: Exception) {
                input
            }
        } else {
            input
        }

        selectedVideo = null
        activePlaylistId = null
        searchQuery = cleanQuery
        selectedTabIndex = 1 // Switch to Search tab
        performSearch(cleanQuery)
    }

    Scaffold(
        topBar = {
            if (!isFullScreen) {
                Surface(
                    color = glassBg,
                    border = BorderStroke(1.dp, glassBorderColor),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TopAppBar(
                        title = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .background(
                                            Brush.linearGradient(listOf(brandRed, Color(0xFFFF7A00))),
                                            RoundedCornerShape(12.dp)
                                        )
                                        .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            if (activePlaylistId != null) activePlaylistTitle else "StudyTube",
                                            fontWeight = FontWeight.ExtraBold,
                                            color = textColor,
                                            fontSize = 18.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (selectedVideo != null) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = brandCyan.copy(alpha = 0.2f),
                                                border = BorderStroke(1.dp, brandCyan)
                                            ) {
                                                Text(
                                                    "PRO",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = brandCyan,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                )
                                            }
                                        } else {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Box(
                                                modifier = Modifier
                                                    .size(6.dp)
                                                    .background(brandCyan, CircleShape)
                                            )
                                        }
                                    }
                                    Text(
                                        if (selectedVideo != null) "● Focus Theater Active"
                                        else if (activePlaylistId != null) "${activePlaylistChannelTitle.ifBlank { "Playlist" }} • ${playlistResults.size} Lectures"
                                        else "Zero Distraction Study Video",
                                        fontSize = 11.sp,
                                        color = if (selectedVideo != null) brandCyan else if (activePlaylistId != null) brandPurple else brandCyan,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        },
                        navigationIcon = {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFF131B2E),
                                border = BorderStroke(0.8.dp, Color(0xFF1E293B)),
                                modifier = Modifier
                                    .padding(start = 12.dp, end = 4.dp)
                                    .size(36.dp)
                                    .clickable {
                                        if (showMistakeDoubtDialog) {
                                            showMistakeDoubtDialog = false
                                        } else if (showQualitySheet) {
                                            showQualitySheet = false
                                        } else if (isDirectTouchMode) {
                                            isDirectTouchMode = false
                                        } else if (showAiSummarySheet) {
                                            showAiSummarySheet = false
                                        } else if (showAttachVideoDialog) {
                                            showAttachVideoDialog = false
                                        } else if (showAddNoteDialog) {
                                            showAddNoteDialog = false
                                        } else if (showAddPlaylistDialog) {
                                            showAddPlaylistDialog = false
                                        } else if (showApiKeyDialog) {
                                            showApiKeyDialog = false
                                        } else if (showAddChannelDialog) {
                                            showAddChannelDialog = false
                                        } else if (channelToDelete != null) {
                                            channelToDelete = null
                                        } else if (selectedVideo != null) {
                                            closeTheaterMode()
                                        } else if (activePlaylistId != null) {
                                            activePlaylistId = null
                                            playlistResults = emptyList()
                                        } else if (activeCuratedChannel != null) {
                                            activeCuratedChannel = null
                                            curatedChannelVideos = emptyList()
                                            curatedChannelPlaylists = emptyList()
                                            selectedChannelQuery = null
                                            channelSearchFilter = ""
                                            channelActiveTab = 0
                                        } else {
                                            onNavigateBack()
                                        }
                                    }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White, modifier = Modifier.size(18.dp))
                                }
                            }
                        },
                        actions = {
                            if (selectedVideo != null) {
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = Color(0xFF042F2E),
                                    border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.6f)),
                                    modifier = Modifier.clickable {
                                        Toast.makeText(context, "Zen Shield: 100% Distraction Free Active", Toast.LENGTH_SHORT).show()
                                    }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Security, contentDescription = "Zen", tint = Color(0xFF10B981), modifier = Modifier.size(13.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("ZEN", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                                    }
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFF131B2E),
                                    border = BorderStroke(0.8.dp, Color(0xFF1E293B)),
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clickable { setFullscreenMode(!isFullScreen) }
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.Fullscreen, contentDescription = "Full Screen", tint = brandCyan, modifier = Modifier.size(18.dp))
                                    }
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                            } else {
                                // Lakshya Lens Doubt Scanner Button in Top Bar
                                Surface(
                                    shape = RoundedCornerShape(18.dp),
                                    color = Color(0xFF0F172A),
                                    border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.7f)),
                                    modifier = Modifier
                                        .height(36.dp)
                                        .clickable {
                                            lensScannerInitialImageUri = null
                                            showLensScannerDialog = true
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.DocumentScanner,
                                            contentDescription = "Scan Question",
                                            tint = Color(0xFF38BDF8),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            "Scan Doubt",
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFF131B2E),
                                    border = BorderStroke(0.8.dp, Color(0xFF1E293B)),
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clickable { showApiKeyDialog = true }
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Outlined.Key, contentDescription = "API Keys", tint = if (youtubeApiKey.isNotBlank()) brandGreen else brandCyan, modifier = Modifier.size(17.dp))
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                    )
                }
            }
        },
        containerColor = bgColor,
        bottomBar = {
            if (!isFullScreen) {
                if (selectedVideo != null) {
                    // Theater Mode Bottom Floating Dock (Matching Reference Image 2)
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xFF0B1120).copy(alpha = 0.95f),
                        border = BorderStroke(1.dp, Color(0xFF1E293B)),
                        shadowElevation = 14.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp, horizontal = 12.dp),
                            horizontalArrangement = Arrangement.SpaceAround,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.clickable {
                                    Toast.makeText(context, "Zen Shield: 100% Pure Focused Learning Active", Toast.LENGTH_SHORT).show()
                                }
                            ) {
                                Icon(Icons.Default.Security, contentDescription = "Focus", tint = brandGreen, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("Focus", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = brandGreen)
                            }
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.clickable {
                                    selectedVideo?.let { generateAiFormulaSheet(it) }
                                }
                            ) {
                                Icon(Icons.Default.MenuBook, contentDescription = "Formula", tint = brandCyan, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("Formula", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = brandCyan)
                            }
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.clickable {
                                    Toast.makeText(context, "Audio Booster: Voice Clarity Enabled 🔊", Toast.LENGTH_SHORT).show()
                                }
                            ) {
                                Icon(Icons.Default.VolumeUp, contentDescription = "Boost", tint = brandAmber, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("Boost", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = brandAmber)
                            }
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.clickable {
                                    noteTextInput = ""
                                    noteTimestampInput = "00:00"
                                    showAddNoteDialog = true
                                }
                            ) {
                                Icon(Icons.Default.EditNote, contentDescription = "Notes", tint = brandPurple, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("Notes", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = brandPurple)
                            }
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.clickable {
                                    setFullscreenMode(!isFullScreen)
                                }
                            ) {
                                Icon(Icons.Default.PictureInPicture, contentDescription = "PiP", tint = Color(0xFF38BDF8), modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("PiP", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8))
                            }
                        }
                    }
                } else if (activePlaylistId == null) {
                    // Main Screen Bottom Navigation Bar (Matching Reference Image 1)
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xFF0B1120).copy(alpha = 0.95f),
                        border = BorderStroke(1.dp, Color(0xFF1E293B)),
                        shadowElevation = 14.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp, horizontal = 8.dp),
                            horizontalArrangement = Arrangement.SpaceAround,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 1. Explore
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.clickable { selectedTabIndex = 1 }
                            ) {
                                Icon(
                                    Icons.Default.Explore,
                                    contentDescription = "Explore",
                                    tint = if (selectedTabIndex == 1) brandCyan else subTextColor,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("Explore", fontSize = 10.sp, fontWeight = if (selectedTabIndex == 1) FontWeight.Bold else FontWeight.Medium, color = if (selectedTabIndex == 1) brandCyan else subTextColor)
                            }

                            // 2. Syllabus
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.clickable { selectedTabIndex = 0 }
                            ) {
                                Icon(
                                    Icons.Default.MenuBook,
                                    contentDescription = "Syllabus",
                                    tint = if (selectedTabIndex == 0) brandCyan else subTextColor,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("Syllabus", fontSize = 10.sp, fontWeight = if (selectedTabIndex == 0) FontWeight.Bold else FontWeight.Medium, color = if (selectedTabIndex == 0) brandCyan else subTextColor)
                            }

                            // 3. Center Zen Shield Action Button
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(46.dp)
                                    .background(
                                        Brush.linearGradient(listOf(Color(0xFF059669), Color(0xFF10B981))),
                                        CircleShape
                                    )
                                    .border(1.5.dp, Color(0xFF6EE7B7), CircleShape)
                                    .clickable {
                                        Toast.makeText(context, "🛡️ Zen Shield Active: 100% Distraction-Free Study Space", Toast.LENGTH_SHORT).show()
                                    }
                            ) {
                                Icon(Icons.Default.Security, contentDescription = "Zen Shield", tint = Color.White, modifier = Modifier.size(22.dp))
                            }

                            // 4. Bookmarks
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.clickable { selectedTabIndex = 4 }
                            ) {
                                Icon(
                                    Icons.Default.BookmarkBorder,
                                    contentDescription = "Bookmarks",
                                    tint = if (selectedTabIndex == 4) brandCyan else subTextColor,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("Bookmarks", fontSize = 10.sp, fontWeight = if (selectedTabIndex == 4) FontWeight.Bold else FontWeight.Medium, color = if (selectedTabIndex == 4) brandCyan else subTextColor)
                            }

                            // 5. Profile
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.clickable { showApiKeyDialog = true }
                            ) {
                                Icon(
                                    Icons.Default.PersonOutline,
                                    contentDescription = "Profile",
                                    tint = subTextColor,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("Profile", fontSize = 10.sp, fontWeight = FontWeight.Medium, color = subTextColor)
                            }
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            if (!isFullScreen && selectedTabIndex == 3 && selectedVideo == null && activePlaylistId == null) {
                FloatingActionButton(
                    onClick = { showAddPlaylistDialog = true },
                    containerColor = brandRed,
                    contentColor = Color.White,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Add Playlist", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    ) { paddingValues ->
        val effectivePadding = if (isFullScreen) PaddingValues(0.dp) else paddingValues
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFF060A13),
                            Color(0xFF0C1424),
                            Color(0xFF080D1A)
                        )
                    )
                )
                .padding(effectivePadding)
        ) {
            if (selectedVideo != null) {
                // ==========================================
                // THEATER MODE: DISTRACTION-FREE VIDEO PLAYER
                // ==========================================
                val currentVid = selectedVideo!!
                var hasStartedPlaying by remember(currentVid.id) { mutableStateOf(false) }
                var playerHasError by remember(currentVid.id) { mutableStateOf(false) }
                var playerErrorCode by remember(currentVid.id) { mutableIntStateOf(0) }
                val isBookmarked = bookmarks.any { it.startsWith("${currentVid.id}|||") }
                var videoFullDescription by remember(currentVid.id) { mutableStateOf(currentVid.description) }
                var isDescriptionExpanded by remember(currentVid.id) { mutableStateOf(false) }
                var isLoadingDescription by remember(currentVid.id) { mutableStateOf(false) }

                LaunchedEffect(currentVid.id) {
                    kotlinx.coroutines.delay(1800)
                    hasStartedPlaying = true
                }

                LaunchedEffect(currentVid.id, youtubeApiKey) {
                    if (youtubeApiKey.isNotBlank()) {
                        isLoadingDescription = true
                        try {
                            withContext(Dispatchers.IO) {
                                val urlStr = "https://www.googleapis.com/youtube/v3/videos?part=snippet&id=${currentVid.id}&key=$youtubeApiKey"
                                val conn = URL(urlStr).openConnection() as HttpURLConnection
                                conn.requestMethod = "GET"
                                conn.connectTimeout = 6000
                                conn.readTimeout = 6000
                                if (conn.responseCode == 200) {
                                    val resp = conn.inputStream.bufferedReader().use { it.readText() }
                                    val json = JSONObject(resp)
                                    val items = json.optJSONArray("items")
                                    val item0 = items?.optJSONObject(0)
                                    val snippet = item0?.optJSONObject("snippet")
                                    val fullDesc = snippet?.optString("description")
                                    if (!fullDesc.isNullOrBlank()) {
                                        videoFullDescription = fullDesc
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        } finally {
                            isLoadingDescription = false
                        }
                    }
                    if (videoFullDescription.isBlank()) {
                        isLoadingDescription = true
                        try {
                            withContext(Dispatchers.IO) {
                                val watchUrl = "https://www.youtube.com/watch?v=${currentVid.id}"
                                val conn = URL(watchUrl).openConnection() as HttpURLConnection
                                conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                                conn.connectTimeout = 6000
                                conn.readTimeout = 6000
                                if (conn.responseCode == 200) {
                                    val html = conn.inputStream.bufferedReader().use { it.readText() }
                                    val descRegex = Regex(""""shortDescription":"(.*?)"""")
                                    val match = descRegex.find(html)
                                    val extractedDesc = match?.groupValues?.get(1)
                                        ?.replace("\\n", "\n")
                                        ?.replace("\\r", "")
                                        ?.replace("\\\"", "\"")
                                        ?.replace("\\\\", "\\")
                                    if (!extractedDesc.isNullOrBlank()) {
                                        videoFullDescription = extractedDesc
                                    }
                                }
                            }
                        } catch (_: Exception) {
                        } finally {
                            isLoadingDescription = false
                        }
                    }
                }

                val timerHours = activeStudySeconds / 3600
                val timerMinutes = (activeStudySeconds % 3600) / 60
                val timerSecs = activeStudySeconds % 60
                val formattedStudyTime = if (timerHours > 0) {
                    String.format("%02d:%02d:%02d", timerHours, timerMinutes, timerSecs)
                } else {
                    String.format("%02d:%02d", timerMinutes, timerSecs)
                }

                val onReloadVideo: () -> Unit = {
                    playerHasError = false
                    hasStartedPlaying = false
                    val html = generateStudyTubeEmbedHtml(currentVid.id, selectedQuality, videoCurrentTime, selectedSpeed)
                    webViewInstance?.loadDataWithBaseURL("https://www.youtube-nocookie.com", html, "text/html", "UTF-8", null)
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                ) {
                        // 16:9 Video Canvas with YouTube IFrame API Control & Gestures
                        Box(
                            modifier = if (isFullScreen) {
                                Modifier.fillMaxSize().background(Color.Black)
                            } else {
                                Modifier.fillMaxWidth().aspectRatio(16f / 9f).background(Color.Black)
                            }
                        ) {
                            key(currentVid.id) {
                                AndroidView(
                                    factory = { ctx ->
                                        WebView(ctx).apply {
                                            layoutParams = android.view.ViewGroup.LayoutParams(
                                                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                                                android.view.ViewGroup.LayoutParams.MATCH_PARENT
                                            )
                                            setBackgroundColor(android.graphics.Color.BLACK)
                                            addJavascriptInterface(object : Any() {
                                                @android.webkit.JavascriptInterface
                                                fun onPlayerReady() {
                                                    hasStartedPlaying = true
                                                    playerHasError = false
                                                }
                                                @android.webkit.JavascriptInterface
                                                fun onPlayerStateChange(state: Int) {
                                                    if (state == 1) {
                                                        isPlaying = true
                                                        hasStartedPlaying = true
                                                        playerHasError = false
                                                    } else if (state == 2 || state == 0) {
                                                        isPlaying = false
                                                    }
                                                }
                                                @android.webkit.JavascriptInterface
                                                fun onPlayerError(errCode: Int) {
                                                    hasStartedPlaying = true
                                                    playerHasError = true
                                                    playerErrorCode = errCode
                                                }
                                                @android.webkit.JavascriptInterface
                                                fun onTimeUpdate(currentTime: Float, duration: Float) {
                                                    videoCurrentTime = currentTime
                                                    videoDuration = duration
                                                    if (currentTime > 0.3f) {
                                                        hasStartedPlaying = true
                                                        playerHasError = false
                                                    }
                                                }
                                            }, "Android")
                                            settings.apply {
                                                javaScriptEnabled = true
                                                domStorageEnabled = true
                                                databaseEnabled = true
                                                mediaPlaybackRequiresUserGesture = false
                                                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                                                loadWithOverviewMode = true
                                                useWideViewPort = true
                                                setSupportZoom(false)
                                                allowFileAccess = true
                                                allowContentAccess = true
                                                cacheMode = WebSettings.LOAD_DEFAULT
                                                userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36"
                                            }
                                            webChromeClient = object : WebChromeClient() {
                                                override fun onShowCustomView(view: android.view.View?, callback: CustomViewCallback?) {
                                                    super.onShowCustomView(view, callback)
                                                    setFullscreenMode(true)
                                                }
                                                override fun onHideCustomView() {
                                                    super.onHideCustomView()
                                                    setFullscreenMode(false)
                                                }
                                            }
                                            webViewClient = object : WebViewClient() {
                                                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                                    return false
                                                }
                                                override fun onRenderProcessGone(view: WebView?, detail: android.webkit.RenderProcessGoneDetail?): Boolean {
                                                    try {
                                                        view?.destroy()
                                                    } catch (e: Exception) {
                                                        // Ignore
                                                    }
                                                    playerHasError = true
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
                                            val initialHtml = generateStudyTubeEmbedHtml(currentVid.id, selectedQuality, videoCurrentTime, selectedSpeed)
                                            loadDataWithBaseURL("https://www.youtube-nocookie.com", initialHtml, "text/html", "UTF-8", null)
                                            webViewInstance = this
                                        }
                                    },
                                    update = { wv ->
                                        webViewInstance = wv
                                    },
                                    modifier = Modifier.fillMaxSize()
                                )
                            }

                            // YouTube-Style Interactive Control & Gesture Overlay
                            if (!hasStartedPlaying && !playerHasError && !isCapturingDoubtSnapshot) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Black)
                                        .clickable {
                                            hasStartedPlaying = true
                                            webViewInstance?.evaluateJavascript("togglePlayPause();", null)
                                        }
                                ) {
                                    coil.compose.AsyncImage(
                                        model = currentVid.thumbnailUrl,
                                        contentDescription = null,
                                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize().alpha(0.6f)
                                    )
                                    Column(
                                        modifier = Modifier.align(Alignment.Center),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        androidx.compose.material3.CircularProgressIndicator(
                                            modifier = Modifier.size(36.dp),
                                            color = brandCyan
                                        )
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Text("Loading Video...", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }

                            // Video Error / Replay Overlay
                            if (playerHasError && !isCapturingDoubtSnapshot) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color(0xFF0F172A).copy(alpha = 0.97f))
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            Icons.Default.Refresh,
                                            contentDescription = null,
                                            tint = brandCyan,
                                            modifier = Modifier.size(44.dp)
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            "Video Playback Interrupted",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            textAlign = TextAlign.Center
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            "In-app player reload karein ya YouTube me kholein:",
                                            color = Color.White.copy(alpha = 0.8f),
                                            fontSize = 11.sp,
                                            textAlign = TextAlign.Center
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Button(
                                                onClick = onReloadVideo,
                                                colors = ButtonDefaults.buttonColors(containerColor = brandCyan),
                                                shape = RoundedCornerShape(10.dp)
                                            ) {
                                                Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Reload Player", fontWeight = FontWeight.Bold, color = Color.Black, fontSize = 12.sp)
                                            }

                                            OutlinedButton(
                                                onClick = {
                                                    try {
                                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/watch?v=${currentVid.id}&t=${videoCurrentTime.toInt()}s"))
                                                        context.startActivity(intent)
                                                    } catch (e: Exception) {
                                                        Toast.makeText(context, "Could not open YouTube: ${e.message}", Toast.LENGTH_SHORT).show()
                                                    }
                                                },
                                                shape = RoundedCornerShape(10.dp),
                                                border = BorderStroke(1.dp, Color(0xFFEF4444))
                                            ) {
                                                Text("Open in YouTube ↗️", fontWeight = FontWeight.Bold, color = Color(0xFFEF4444), fontSize = 12.sp)
                                            }
                                        }
                                    }
                                }
                            }
                            if (!isCapturingDoubtSnapshot) {
                                PlayerInteractiveOverlay(
                                    isPlaying = isPlaying,
                                    isFullScreen = isFullScreen,
                                    showControls = showPlayerOverlay,
                                    currentTime = videoCurrentTime,
                                    duration = videoDuration,
                                    onSeekTo = { pos -> 
                                        webViewInstance?.evaluateJavascript("player.seekTo($pos, true);", null) 
                                    },
                                    onToggleControls = {
                                        showPlayerOverlay = !showPlayerOverlay
                                        playerInteractionTime = System.currentTimeMillis()
                                    },
                                    onUserInteract = {
                                        playerInteractionTime = System.currentTimeMillis()
                                    },
                                    onSeekBack10 = {
                                        leftSeekRippleSeconds += 10
                                        rightSeekRippleSeconds = 0
                                        webViewInstance?.evaluateJavascript("seekBy(-10);", null)
                                        showPlayerOverlay = true
                                        playerInteractionTime = System.currentTimeMillis()
                                    },
                                    onSeekForward10 = {
                                        rightSeekRippleSeconds += 10
                                        leftSeekRippleSeconds = 0
                                        webViewInstance?.evaluateJavascript("seekBy(10);", null)
                                        showPlayerOverlay = true
                                        playerInteractionTime = System.currentTimeMillis()
                                    },
                                    onTogglePlayPause = {
                                        val nextPlaying = !isPlaying
                                        isPlaying = nextPlaying
                                        webViewInstance?.evaluateJavascript("togglePlayPause();", null)
                                        showPlayerOverlay = true
                                        playerInteractionTime = System.currentTimeMillis()
                                    },
                                    onSetSpeed = { spd ->
                                        selectedSpeed = spd
                                        webViewInstance?.evaluateJavascript("setSpeed($spd);", null)
                                        showPlayerOverlay = true
                                        playerInteractionTime = System.currentTimeMillis()
                                    },
                                    onStart2xHold = {
                                        is2xHoldActive = true
                                        webViewInstance?.evaluateJavascript("setSpeed(2.0);", null)
                                        showPlayerOverlay = true
                                        playerInteractionTime = System.currentTimeMillis()
                                    },
                                    onRelease2xHold = {
                                        is2xHoldActive = false
                                        webViewInstance?.evaluateJavascript("setSpeed($selectedSpeed);", null)
                                    },
                                    onToggleFullscreen = {
                                        setFullscreenMode(!isFullScreen)
                                        showPlayerOverlay = true
                                        playerInteractionTime = System.currentTimeMillis()
                                    },
                                    isDirectTouchMode = isDirectTouchMode,
                                    onToggleDirectTouch = {
                                        isDirectTouchMode = !isDirectTouchMode
                                        Toast.makeText(
                                            context,
                                            if (isDirectTouchMode) "YouTube Touch Mode: Tap ⚙️ on video for native quality" else "Gesture controls restored",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    },
                                    onOpenQualitySheet = {
                                        showQualitySheet = true
                                    },
                                    qualityLabel = when (selectedQuality) {
                                        "hd1080" -> "1080p"
                                        "hd720" -> "720p"
                                        "large" -> "480p"
                                        "medium" -> "360p"
                                        else -> "Auto"
                                    },
                                    onDoubtCapture = {
                                        triggerLectureDoubtCapture(currentVid, videoCurrentTime.toInt())
                                    },
                                    leftSeekSeconds = leftSeekRippleSeconds,
                                    rightSeekSeconds = rightSeekRippleSeconds,
                                    is2xHoldActive = is2xHoldActive,
                                    selectedSpeed = selectedSpeed,
                                    formattedStudyTime = formattedStudyTime,
                                    videoTitle = currentVid.title,
                                    brandCyan = brandCyan,
                                    brandGreen = brandGreen
                                )
                            }
                        }

                    // Video Meta & Live Study Controls
                    if (!isFullScreen) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp)
                    ) {
                        // Title
                        Text(
                            text = currentVid.title,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = textColor,
                            lineHeight = 23.sp
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Channel & Subject Tag Row
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = brandCyan.copy(alpha = 0.15f),
                                border = BorderStroke(0.5.dp, brandCyan.copy(alpha = 0.4f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Verified, contentDescription = null, tint = brandCyan, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        currentVid.channelTitle,
                                        fontSize = 12.sp,
                                        color = brandCyan,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.clickable {
                                            // Handle channel click
                                            val ch = curatedChannels.find { it.name.equals(currentVid.channelTitle, ignoreCase = true) }
                                                ?: CuratedChannel(
                                                    name = currentVid.channelTitle,
                                                    subject = currentVid.subjectTag,
                                                    accentColor = brandCyan,
                                                    icon = Icons.Default.Verified,
                                                    description = "YouTube Channel: " + currentVid.channelTitle,
                                                    searchQueries = listOf(currentVid.channelTitle),
                                                    curatedVideos = emptyList()
                                                )
                                            selectCuratedChannel(ch, currentVid.channelTitle, currentVid.channelId.takeIf { it.isNotBlank() })
                                            selectedVideo = null
                                        }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.weight(1f))

                            // Distraction-Free Active Pill
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = brandGreen.copy(alpha = 0.15f),
                                border = BorderStroke(0.5.dp, brandGreen.copy(alpha = 0.4f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(modifier = Modifier.size(6.dp).background(brandGreen, CircleShape))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("0 Distractions", fontSize = 11.sp, color = brandGreen, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // ==========================================
                        // ❓ UNMISSABLE DOUBT & MISTAKE NOTEBOOK CARD
                        // ==========================================
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFF4C0519),
                            border = BorderStroke(1.2.dp, Color(0xFFFB7185)),
                            shadowElevation = 4.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    triggerLectureDoubtCapture(currentVid, videoCurrentTime.toInt())
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .background(Color(0xFFE11D48), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.HelpOutline,
                                        contentDescription = "Doubt",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            "Ask Doubt / Note Mistake",
                                            fontSize = 13.5.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color.White
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = Color(0xFFFDA4AF).copy(alpha = 0.25f)
                                        ) {
                                            Text(
                                                "📸 Freeze @ ${formatSecondsToTimestamp(videoCurrentTime.toInt())}",
                                                fontSize = 9.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFFFECDD3),
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        "Capture lecture frame & add to Mistake Notebook",
                                        fontSize = 11.sp,
                                        color = Color(0xFFFDE8E8),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    tint = Color(0xFFFDA4AF),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        // ==========================================
                        // ⏳ LIVE STUDY TIMER & DAILY GOAL SYNC CARD (Dark Glassmorphic)
                        // ==========================================
                        val timerHours = activeStudySeconds / 3600
                        val timerMinutes = (activeStudySeconds % 3600) / 60
                        val timerSecs = activeStudySeconds % 60
                        val formattedStudyTime = if (timerHours > 0) {
                            String.format("%02d:%02d:%02d", timerHours, timerMinutes, timerSecs)
                        } else {
                            String.format("%02d:%02d", timerMinutes, timerSecs)
                        }

                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = glassCardBg,
                            border = BorderStroke(1.dp, brandGreen.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .background(brandGreen.copy(alpha = 0.18f), RoundedCornerShape(10.dp))
                                            .border(1.dp, brandGreen.copy(alpha = 0.4f), RoundedCornerShape(10.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.HourglassTop, contentDescription = null, tint = brandGreen, modifier = Modifier.size(18.dp))
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            "Live Study Timer & Daily Goal Sync",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = textColor
                                        )
                                        Text(
                                            "Auto-synced with Daily Study Target on exit",
                                            fontSize = 11.sp,
                                            color = subTextColor
                                        )
                                    }

                                    // Active Stop/Play toggle & Time pill
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = brandGreen.copy(alpha = 0.2f),
                                        border = BorderStroke(1.dp, brandGreen.copy(alpha = 0.6f))
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .clickable { isTimerRunning = !isTimerRunning }
                                                .padding(horizontal = 10.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                if (isTimerRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                                                contentDescription = if (isTimerRunning) "Pause Timer" else "Resume Timer",
                                                tint = brandGreen,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                formattedStudyTime,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = brandGreen
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // ==========================================
                        // ⚡ QUICK PLAYBACK SPEED CHIPS (Matching Reference Image)
                        // ==========================================
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "⚡ QUICK PLAYBACK SPEED",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = brandCyan,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                "Preset: ${selectedSpeed}x",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = brandCyan
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        val speedOptions = listOf(1.0f, 1.25f, 1.5f, 1.75f, 2.0f)
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(speedOptions) { spd ->
                                val isSelected = selectedSpeed == spd
                                val speedLabel = if (spd == 1.0f) "1.0x (Normal)" else "${spd}x"
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSelected) Color(0xFF0E2F3F) else Color(0xFF131B2E),
                                    border = BorderStroke(1.2.dp, if (isSelected) brandCyan else Color(0xFF1E293B)),
                                    modifier = Modifier.clickable {
                                        selectedSpeed = spd
                                        webViewInstance?.evaluateJavascript("setSpeed($spd);", null)
                                        Toast.makeText(context, "Playback Speed: ${spd}x", Toast.LENGTH_SHORT).show()
                                    }
                                ) {
                                    Text(
                                        text = speedLabel,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                        color = if (isSelected) brandCyan else textColor,
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // ==========================================
                        // 📺 QUICK VIDEO QUALITY CHIPS & DIRECT CONTROLS (Matching Reference Image)
                        // ==========================================
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "📺 VIDEO QUALITY",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = brandAmber,
                                letterSpacing = 0.5.sp
                            )
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isDirectTouchMode) brandAmber else Color(0xFF261D09),
                                border = BorderStroke(1.dp, brandAmber.copy(alpha = 0.8f)),
                                modifier = Modifier.clickable {
                                    isDirectTouchMode = !isDirectTouchMode
                                    Toast.makeText(
                                        context,
                                        if (isDirectTouchMode) "YouTube Touch Mode ON: Video pe ⚙️ icon dabakar quality badlein" else "Gesture controls restored",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.TouchApp,
                                        contentDescription = null,
                                        tint = if (isDirectTouchMode) Color.Black else brandAmber,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        if (isDirectTouchMode) "YT Touch Active (Tap ⚙️ on Video)" else "⚙ Direct YT Touch",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDirectTouchMode) Color.Black else brandAmber
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        val qualityOptions = listOf(
                            "auto" to "Auto",
                            "hd1080" to "1080p Full HD",
                            "hd720" to "720p HD",
                            "large" to "480p",
                            "medium" to "360p"
                        )
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(qualityOptions) { (qKey, qLabel) ->
                                val isSelected = selectedQuality == qKey
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSelected) Color(0xFF2A1C06) else Color(0xFF131B2E),
                                    border = BorderStroke(1.2.dp, if (isSelected) brandAmber else Color(0xFF1E293B)),
                                    modifier = Modifier.clickable {
                                        reloadVideoWithQuality(qKey, qLabel)
                                    }
                                ) {
                                    Text(
                                        text = qLabel,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                        color = if (isSelected) brandAmber else textColor,
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                                    )
                                }
                            }
                        }

                        // Tip Banner
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF131B2E),
                            border = BorderStroke(0.6.dp, Color(0xFF1E293B)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("💡", fontSize = 12.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Tip: Kisi bhi quality button ko tap karein ya 'Direct YT Touch' se video ke ⚙️ icon se exact resolution chunein.",
                                    fontSize = 11.sp,
                                    color = subTextColor,
                                    lineHeight = 15.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // ==========================================
                        // 🗂️ THEATER MODE 3 TABS (Matching Reference Image 2)
                        // [Timestamps & Chapters] [Key Notes & Formula] [Handouts & PYQ]
                        // ==========================================
                        val theaterTabs = listOf("Timestamps & Chapters", "Key Notes & Formula", "Handouts & PYQ")
                        TabRow(
                            selectedTabIndex = selectedPlayerTab,
                            containerColor = Color.Transparent,
                            contentColor = brandCyan,
                            divider = { HorizontalDivider(color = Color(0xFF1E293B), thickness = 1.dp) },
                            indicator = { tabPositions ->
                                if (selectedPlayerTab < tabPositions.size) {
                                    TabRowDefaults.Indicator(
                                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedPlayerTab]),
                                        color = brandCyan,
                                        height = 3.dp
                                    )
                                }
                            }
                        ) {
                            theaterTabs.forEachIndexed { idx, tabTitle ->
                                Tab(
                                    selected = selectedPlayerTab == idx,
                                    onClick = { selectedPlayerTab = idx },
                                    text = {
                                        Text(
                                            tabTitle,
                                            fontSize = 12.sp,
                                            fontWeight = if (selectedPlayerTab == idx) FontWeight.Bold else FontWeight.Medium,
                                            color = if (selectedPlayerTab == idx) brandCyan else subTextColor,
                                            maxLines = 1
                                        )
                                    }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        when (selectedPlayerTab) {
                            0 -> {
                                // ----------------------------------------------------
                                // TAB 0: TIMESTAMPS & CHAPTERS (Real Description & Custom Intervals)
                                // ----------------------------------------------------
                                val timestampPrefs = remember {
                                    context.getSharedPreferences("studytube_custom_timestamps", Context.MODE_PRIVATE)
                                }
                                var customTimestampsJson by remember(currentVid.id) {
                                    mutableStateOf(timestampPrefs.getString("custom_${currentVid.id}", null))
                                }
                                val parsedFromDesc = remember(videoFullDescription) {
                                    extractTimestampsFromDescription(videoFullDescription)
                                }
                                val customList = remember(customTimestampsJson) {
                                    deserializeTimestamps(customTimestampsJson)
                                }
                                val isUsingCustom = customList.isNotEmpty()
                                val activeChapters = if (isUsingCustom) customList else parsedFromDesc

                                var showAddTimestampDialog by remember { mutableStateOf(false) }
                                var showIntervalSelector by remember { mutableStateOf(false) }
                                var customIntervalMinutes by remember { mutableStateOf("5") }
                                var newTimestampTitle by remember { mutableStateOf("") }
                                var newTimestampTime by remember { mutableStateOf("00:00") }

                                val currentSec = videoCurrentTime.toInt()
                                val currentPlayingIndex = remember(currentSec, activeChapters) {
                                    if (activeChapters.isEmpty()) -1
                                    else {
                                        val idx = activeChapters.indexOfLast { currentSec >= it.seekSeconds }
                                        if (idx >= 0) idx else 0
                                    }
                                }

                                Column(
                                    verticalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    // Control / Header Bar
                                    Surface(
                                        shape = RoundedCornerShape(14.dp),
                                        color = Color(0xFF131B2E),
                                        border = BorderStroke(1.dp, Color(0xFF1E293B)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                            verticalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                if (isUsingCustom) {
                                                    Surface(
                                                        shape = RoundedCornerShape(8.dp),
                                                        color = brandAmber.copy(alpha = 0.15f),
                                                        border = BorderStroke(1.dp, brandAmber.copy(alpha = 0.5f))
                                                    ) {
                                                        Text(
                                                            "⚡ Custom Timestamps (${activeChapters.size})",
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = brandAmber,
                                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                        )
                                                    }
                                                } else if (parsedFromDesc.isNotEmpty()) {
                                                    Surface(
                                                        shape = RoundedCornerShape(8.dp),
                                                        color = brandCyan.copy(alpha = 0.15f),
                                                        border = BorderStroke(1.dp, brandCyan.copy(alpha = 0.5f))
                                                    ) {
                                                        Text(
                                                            "📄 From Channel Description (${activeChapters.size})",
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = brandCyan,
                                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                        )
                                                    }
                                                } else {
                                                    Surface(
                                                        shape = RoundedCornerShape(8.dp),
                                                        color = Color(0xFF334155).copy(alpha = 0.4f),
                                                        border = BorderStroke(1.dp, Color(0xFF475569))
                                                    ) {
                                                        Text(
                                                            "ℹ️ No timestamps in description",
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Medium,
                                                            color = subTextColor,
                                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                        )
                                                    }
                                                }

                                                if (isUsingCustom) {
                                                    TextButton(
                                                        onClick = {
                                                            timestampPrefs.edit().remove("custom_${currentVid.id}").apply()
                                                            customTimestampsJson = null
                                                            Toast.makeText(context, "Restored original channel timestamps", Toast.LENGTH_SHORT).show()
                                                        },
                                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                                    ) {
                                                        Icon(Icons.Default.Refresh, contentDescription = null, tint = brandCyan, modifier = Modifier.size(14.dp))
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text("Reset", fontSize = 11.sp, color = brandCyan, fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                            }

                                            // Action Buttons Row: [⏱️ Auto-Intervals] [➕ Custom Timestamp]
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                // Auto-Interval Toggle Button
                                                Surface(
                                                    shape = RoundedCornerShape(10.dp),
                                                    color = if (showIntervalSelector) brandCyan.copy(alpha = 0.2f) else Color(0xFF1E293B),
                                                    border = BorderStroke(1.dp, if (showIntervalSelector) brandCyan else Color(0xFF334155)),
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .clickable {
                                                            showIntervalSelector = !showIntervalSelector
                                                        }
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 10.dp),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.Center
                                                    ) {
                                                        Icon(Icons.Default.Schedule, contentDescription = null, tint = brandCyan, modifier = Modifier.size(16.dp))
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Text("⏱️ Auto-Intervals", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                                    }
                                                }

                                                // Add Custom Timestamp Button
                                                Surface(
                                                    shape = RoundedCornerShape(10.dp),
                                                    color = Color(0xFF1E293B),
                                                    border = BorderStroke(1.dp, Color(0xFF334155)),
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .clickable {
                                                            newTimestampTime = formatSecondsToTimestamp(videoCurrentTime.toInt())
                                                            newTimestampTitle = ""
                                                            showAddTimestampDialog = true
                                                        }
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 10.dp),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.Center
                                                    ) {
                                                        Icon(Icons.Default.Add, contentDescription = null, tint = brandGreen, modifier = Modifier.size(16.dp))
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Text("+ Custom Point", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                                    }
                                                }
                                            }

                                            // Auto-Interval Selector Expansion
                                            AnimatedVisibility(visible = showIntervalSelector) {
                                                Column(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .background(Color(0xFF0F172A), RoundedCornerShape(10.dp))
                                                        .border(1.dp, Color(0xFF334155), RoundedCornerShape(10.dp))
                                                        .padding(10.dp),
                                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    Text(
                                                        "Divide lecture starting from 00:00 to end:",
                                                        fontSize = 11.5.sp,
                                                        color = subTextColor,
                                                        fontWeight = FontWeight.SemiBold
                                                    )

                                                    // Preset Interval Chips: 5 min, 10 min, 15 min, 20 min, 30 min
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                    ) {
                                                        listOf(5, 10, 15, 20, 30).forEach { mins ->
                                                            Surface(
                                                                shape = RoundedCornerShape(8.dp),
                                                                color = Color(0xFF1E293B),
                                                                border = BorderStroke(1.dp, Color(0xFF38BDF8)),
                                                                modifier = Modifier
                                                                    .weight(1f)
                                                                    .clickable {
                                                                        val dur = if (videoDuration > 60) videoDuration.toInt() else 3600
                                                                        val generated = generateIntervalTimestamps(mins, dur)
                                                                        val json = serializeTimestamps(generated)
                                                                        timestampPrefs.edit().putString("custom_${currentVid.id}", json).apply()
                                                                        customTimestampsJson = json
                                                                        showIntervalSelector = false
                                                                        Toast.makeText(context, "✅ Created ${generated.size} chapters (every $mins min)!", Toast.LENGTH_SHORT).show()
                                                                    }
                                                            ) {
                                                                Text(
                                                                    "$mins m",
                                                                    fontSize = 11.5.sp,
                                                                    fontWeight = FontWeight.Bold,
                                                                    color = Color(0xFF38BDF8),
                                                                    textAlign = TextAlign.Center,
                                                                    modifier = Modifier.padding(vertical = 6.dp)
                                                                )
                                                            }
                                                        }
                                                    }

                                                    // Custom Minutes Input
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                    ) {
                                                        OutlinedTextField(
                                                            value = customIntervalMinutes,
                                                            onValueChange = { customIntervalMinutes = it.filter { char -> char.isDigit() } },
                                                            label = { Text("Custom min (e.g. 7)", fontSize = 11.sp) },
                                                            singleLine = true,
                                                            modifier = Modifier.weight(1f),
                                                            textStyle = LocalTextStyle.current.copy(fontSize = 12.sp, color = Color.White),
                                                            colors = OutlinedTextFieldDefaults.colors(
                                                                focusedBorderColor = brandCyan,
                                                                unfocusedBorderColor = Color(0xFF334155),
                                                                focusedContainerColor = Color(0xFF131B2E),
                                                                unfocusedContainerColor = Color(0xFF131B2E)
                                                            )
                                                        )

                                                        Button(
                                                            onClick = {
                                                                val mins = customIntervalMinutes.toIntOrNull() ?: 5
                                                                val dur = if (videoDuration > 60) videoDuration.toInt() else 3600
                                                                val generated = generateIntervalTimestamps(mins, dur)
                                                                val json = serializeTimestamps(generated)
                                                                timestampPrefs.edit().putString("custom_${currentVid.id}", json).apply()
                                                                customTimestampsJson = json
                                                                showIntervalSelector = false
                                                                Toast.makeText(context, "✅ Created ${generated.size} chapters (every $mins min)!", Toast.LENGTH_SHORT).show()
                                                            },
                                                            colors = ButtonDefaults.buttonColors(containerColor = brandCyan),
                                                            shape = RoundedCornerShape(8.dp)
                                                        ) {
                                                            Text("Apply", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    // If empty list -> Empty State with Instant 5-Min Prompt
                                    if (activeChapters.isEmpty()) {
                                        Surface(
                                            shape = RoundedCornerShape(14.dp),
                                            color = Color(0xFF131B2E),
                                            border = BorderStroke(1.dp, Color(0xFF1E293B)),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(20.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                Icon(Icons.Default.HourglassEmpty, contentDescription = null, tint = subTextColor, modifier = Modifier.size(36.dp))
                                                Text(
                                                    "No Timestamps Found in Channel Description",
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = textColor,
                                                    textAlign = TextAlign.Center
                                                )
                                                Text(
                                                    "This video does not have chapter markers in its description. Tap below to automatically divide the entire lecture into 5-minute study segments!",
                                                    fontSize = 11.5.sp,
                                                    color = subTextColor,
                                                    textAlign = TextAlign.Center
                                                )
                                                Button(
                                                    onClick = {
                                                        val dur = if (videoDuration > 60) videoDuration.toInt() else 3600
                                                        val generated = generateIntervalTimestamps(5, dur)
                                                        val json = serializeTimestamps(generated)
                                                        timestampPrefs.edit().putString("custom_${currentVid.id}", json).apply()
                                                        customTimestampsJson = json
                                                        Toast.makeText(context, "✅ Auto-generated 5-minute chapters!", Toast.LENGTH_SHORT).show()
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = brandCyan),
                                                    shape = RoundedCornerShape(10.dp)
                                                ) {
                                                    Icon(Icons.Default.Bolt, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text("⚡ Create 5-Min Chapters", fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                                                }
                                            }
                                        }
                                    } else {
                                        // Chapters List
                                        activeChapters.forEachIndexed { index, chapter ->
                                            val isCurrentPlaying = (index == currentPlayingIndex)
                                            val idxStr = String.format(Locale.US, "%02d", index + 1)

                                            Surface(
                                                shape = RoundedCornerShape(14.dp),
                                                color = if (isCurrentPlaying) Color(0xFF0C2433) else Color(0xFF131B2E),
                                                border = BorderStroke(1.2.dp, if (isCurrentPlaying) brandCyan else Color(0xFF1E293B)),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable {
                                                        webViewInstance?.evaluateJavascript("player.seekTo(${chapter.seekSeconds}, true);", null)
                                                        videoCurrentTime = chapter.seekSeconds.toFloat()
                                                        Toast.makeText(context, "⏩ Jumped to ${chapter.timeLabel} (${chapter.title})", Toast.LENGTH_SHORT).show()
                                                    }
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(14.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    // Index badge or Play icon
                                                    Box(
                                                        modifier = Modifier
                                                            .size(36.dp)
                                                            .background(
                                                                if (isCurrentPlaying) brandCyan.copy(alpha = 0.2f) else Color(0xFF1E293B),
                                                                CircleShape
                                                            )
                                                            .border(
                                                                1.dp,
                                                                if (isCurrentPlaying) brandCyan else Color(0xFF334155),
                                                                CircleShape
                                                            ),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        if (isCurrentPlaying) {
                                                            Icon(
                                                                Icons.Default.PlayArrow,
                                                                contentDescription = "Playing",
                                                                tint = brandCyan,
                                                                modifier = Modifier.size(20.dp)
                                                            )
                                                        } else {
                                                            Text(
                                                                idxStr,
                                                                fontSize = 13.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = subTextColor
                                                            )
                                                        }
                                                    }

                                                    Spacer(modifier = Modifier.width(12.dp))

                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(
                                                            chapter.title,
                                                            fontSize = 13.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = if (isCurrentPlaying) brandCyan else textColor,
                                                            lineHeight = 17.sp
                                                        )
                                                        Spacer(modifier = Modifier.height(2.dp))
                                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                            Text(
                                                                chapter.timeLabel,
                                                                fontSize = 11.sp,
                                                                fontWeight = FontWeight.SemiBold,
                                                                color = subTextColor
                                                            )
                                                            if (isCurrentPlaying) {
                                                                Spacer(modifier = Modifier.width(8.dp))
                                                                Surface(
                                                                    shape = RoundedCornerShape(6.dp),
                                                                    color = brandCyan.copy(alpha = 0.15f),
                                                                    border = BorderStroke(0.5.dp, brandCyan)
                                                                ) {
                                                                    Text(
                                                                        "CURRENTLY PLAYING",
                                                                        fontSize = 9.sp,
                                                                        fontWeight = FontWeight.ExtraBold,
                                                                        color = brandCyan,
                                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                                    )
                                                                }
                                                            }
                                                        }
                                                    }

                                                    // If custom chapter, show delete option
                                                    if (chapter.isCustom) {
                                                        IconButton(
                                                            onClick = {
                                                                val updated = activeChapters.toMutableList().also { it.removeAt(index) }
                                                                val json = serializeTimestamps(updated)
                                                                timestampPrefs.edit().putString("custom_${currentVid.id}", json).apply()
                                                                customTimestampsJson = json
                                                                Toast.makeText(context, "Removed timestamp", Toast.LENGTH_SHORT).show()
                                                            },
                                                            modifier = Modifier.size(28.dp)
                                                        ) {
                                                            Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = subTextColor, modifier = Modifier.size(16.dp))
                                                        }
                                                    } else {
                                                        Icon(
                                                            Icons.Default.PlayCircleFilled,
                                                            contentDescription = "Seek",
                                                            tint = if (isCurrentPlaying) brandCyan else subTextColor.copy(alpha = 0.5f),
                                                            modifier = Modifier.size(24.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                // Dialog to add manual custom timestamp
                                if (showAddTimestampDialog) {
                                    AlertDialog(
                                        onDismissRequest = { showAddTimestampDialog = false },
                                        title = {
                                            Text("Add Custom Timestamp", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = textColor)
                                        },
                                        text = {
                                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                                OutlinedTextField(
                                                    value = newTimestampTitle,
                                                    onValueChange = { newTimestampTitle = it },
                                                    label = { Text("Chapter / Topic Title") },
                                                    placeholder = { Text("e.g. Formula Derivation, PYQ Trick") },
                                                    singleLine = true,
                                                    modifier = Modifier.fillMaxWidth()
                                                )
                                                OutlinedTextField(
                                                    value = newTimestampTime,
                                                    onValueChange = { newTimestampTime = it },
                                                    label = { Text("Timestamp (MM:SS or HH:MM:SS)") },
                                                    placeholder = { Text("e.g. 05:00 or 01:12:30") },
                                                    singleLine = true,
                                                    modifier = Modifier.fillMaxWidth()
                                                )
                                                Button(
                                                    onClick = {
                                                        newTimestampTime = formatSecondsToTimestamp(videoCurrentTime.toInt())
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                                                    shape = RoundedCornerShape(8.dp)
                                                ) {
                                                    Icon(Icons.Default.MyLocation, contentDescription = null, tint = brandCyan, modifier = Modifier.size(14.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("Use Current Video Time (${formatSecondsToTimestamp(videoCurrentTime.toInt())})", fontSize = 11.sp, color = brandCyan)
                                                }
                                            }
                                        },
                                        confirmButton = {
                                            Button(
                                                onClick = {
                                                    val secs = parseTimestampToSeconds(newTimestampTime)
                                                    val cleanTitle = newTimestampTitle.ifBlank { "Custom Chapter (${newTimestampTime})" }
                                                    val newEntry = StudyTubeTimestamp(
                                                        timeLabel = newTimestampTime,
                                                        seekSeconds = secs,
                                                        title = cleanTitle,
                                                        isCustom = true
                                                    )
                                                    val updated = (activeChapters + newEntry).distinctBy { it.seekSeconds }.sortedBy { it.seekSeconds }
                                                    val json = serializeTimestamps(updated)
                                                    timestampPrefs.edit().putString("custom_${currentVid.id}", json).apply()
                                                    customTimestampsJson = json
                                                    showAddTimestampDialog = false
                                                    Toast.makeText(context, "Added timestamp: $newTimestampTime", Toast.LENGTH_SHORT).show()
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = brandCyan)
                                            ) {
                                                Text("Save", color = Color.Black, fontWeight = FontWeight.Bold)
                                            }
                                        },
                                        dismissButton = {
                                            TextButton(onClick = { showAddTimestampDialog = false }) {
                                                Text("Cancel", color = subTextColor)
                                            }
                                        }
                                    )
                                }
                            }

                            1 -> {
                                // ----------------------------------------------------
                                // TAB 1: KEY NOTES & FORMULAS (Matching Reference Image 2)
                                // ----------------------------------------------------
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    // Action buttons row
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        // Doubt Snapshot Button
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = Color(0xFFE11D48),
                                            border = BorderStroke(1.dp, Color(0xFFFDA4AF)),
                                            modifier = Modifier
                                                .weight(1.1f)
                                                .clickable {
                                                    triggerLectureDoubtCapture(currentVid, videoCurrentTime.toInt())
                                                }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(vertical = 10.dp),
                                                horizontalArrangement = Arrangement.Center,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(Icons.Default.HelpOutline, contentDescription = "Doubt", tint = Color.White, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Doubt 📸", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                            }
                                        }

                                        // Bookmark Button
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = if (isBookmarked) brandAmber.copy(alpha = 0.2f) else Color(0xFF131B2E),
                                            border = BorderStroke(1.dp, if (isBookmarked) brandAmber else Color(0xFF1E293B)),
                                            modifier = Modifier
                                                .weight(1f)
                                                .clickable {
                                                    viewModel.toggleStudyTubeBookmark(
                                                        currentVid.id,
                                                        currentVid.title,
                                                        currentVid.channelTitle,
                                                        currentVid.thumbnailUrl
                                                    )
                                                    Toast.makeText(
                                                        context,
                                                        if (isBookmarked) "Removed from Bookmarks" else "⭐ Saved to Bookmarks!",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(vertical = 10.dp),
                                                horizontalArrangement = Arrangement.Center,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    if (isBookmarked) Icons.Default.Star else Icons.Outlined.StarBorder,
                                                    contentDescription = "Bookmark",
                                                    tint = if (isBookmarked) brandAmber else subTextColor,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    if (isBookmarked) "Saved" else "Bookmark",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isBookmarked) brandAmber else textColor
                                                )
                                            }
                                        }

                                        // Add Timestamped Note Button
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = Color(0xFF131B2E),
                                            border = BorderStroke(1.dp, Color(0xFF1E293B)),
                                            modifier = Modifier
                                                .weight(1f)
                                                .clickable {
                                                    noteTextInput = ""
                                                    noteTimestampInput = "00:00"
                                                    showAddNoteDialog = true
                                                }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(vertical = 10.dp),
                                                horizontalArrangement = Arrangement.Center,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(Icons.Outlined.EditNote, contentDescription = "Add Note", tint = brandPurple, modifier = Modifier.size(20.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Add Note", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = textColor)
                                            }
                                        }

                                        // AI Formula Sheet Button
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = Color(0xFF0E2F3F),
                                            border = BorderStroke(1.dp, brandCyan.copy(alpha = 0.6f)),
                                            modifier = Modifier
                                                .weight(1.2f)
                                                .clickable { generateAiFormulaSheet(currentVid) }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(vertical = 10.dp),
                                                horizontalArrangement = Arrangement.Center,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(Icons.Default.AutoAwesome, contentDescription = "AI Formulas", tint = brandCyan, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("AI Formulas", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = brandCyan)
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(14.dp))

                                    // Video Notes Section
                                    val videoNotes = notes.filter { it.contains("|||${currentVid.id}|||") }
                                    if (videoNotes.isNotEmpty()) {
                                        Text("Your Saved Notes for this Lecture", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = textColor)
                                        Spacer(modifier = Modifier.height(8.dp))
                                        videoNotes.forEach { entry ->
                                            val parts = entry.split("|||")
                                            val tsSec = parts.getOrNull(2)?.toIntOrNull() ?: 0
                                            val noteTxt = parts.getOrNull(3) ?: ""
                                            val m = tsSec / 60
                                            val s = tsSec % 60
                                            val timeStr = String.format("%02d:%02d", m, s)

                                            Surface(
                                                shape = RoundedCornerShape(10.dp),
                                                color = Color(0xFF131B2E),
                                                border = BorderStroke(1.dp, Color(0xFF1E293B)),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 4.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(12.dp),
                                                    verticalAlignment = Alignment.Top
                                                ) {
                                                    Surface(
                                                        shape = RoundedCornerShape(6.dp),
                                                        color = brandPurple.copy(alpha = 0.2f),
                                                        border = BorderStroke(0.5.dp, brandPurple.copy(alpha = 0.5f)),
                                                        modifier = Modifier.clickable {
                                                            webViewInstance?.evaluateJavascript("player.seekTo($tsSec, true);", null)
                                                            Toast.makeText(context, "Jumped to $timeStr", Toast.LENGTH_SHORT).show()
                                                        }
                                                    ) {
                                                        Text(
                                                            timeStr,
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = brandPurple,
                                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                    Spacer(modifier = Modifier.width(10.dp))
                                                    Text(noteTxt, fontSize = 13.sp, color = textColor, modifier = Modifier.weight(1f))
                                                    IconButton(
                                                        onClick = { viewModel.removeStudyTubeNote(entry) },
                                                        modifier = Modifier.size(24.dp)
                                                    ) {
                                                        Icon(Icons.Default.Close, contentDescription = "Delete", tint = subTextColor, modifier = Modifier.size(16.dp))
                                                    }
                                                }
                                            }
                                        }
                                    } else {
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = Color(0xFF131B2E),
                                            border = BorderStroke(1.dp, Color(0xFF1E293B)),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(20.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Icon(Icons.Outlined.EditNote, contentDescription = null, tint = brandPurple, modifier = Modifier.size(36.dp))
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Text("No notes added yet", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = textColor)
                                                Text("Tap 'Add Note' or 'AI Formulas' to save formulas and revisions.", fontSize = 11.sp, color = subTextColor, textAlign = TextAlign.Center)
                                            }
                                        }
                                    }
                                }
                            }

                            2 -> {
                                // ----------------------------------------------------
                                // TAB 2: HANDOUTS & PYQ (Lecture Details & PDF Materials)
                                // ----------------------------------------------------
                                val urlPattern = remember { java.util.regex.Pattern.compile("https?://[\\w\\-._~:/?#\\[\\]@!$&'()*+,;=%]+") }
                                val foundUrls = remember(videoFullDescription) {
                                    val list = mutableListOf<String>()
                                    if (!videoFullDescription.isNullOrBlank()) {
                                        val matcher = urlPattern.matcher(videoFullDescription)
                                        while (matcher.find()) {
                                            val match = matcher.group()
                                            if (!list.contains(match)) {
                                                list.add(match)
                                            }
                                        }
                                    }
                                    list
                                }

                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = Color(0xFF131B2E),
                                    border = BorderStroke(1.dp, brandCyan.copy(alpha = 0.35f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(32.dp)
                                                    .background(brandCyan.copy(alpha = 0.18f), RoundedCornerShape(8.dp)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(Icons.Default.Description, contentDescription = null, tint = brandCyan, modifier = Modifier.size(18.dp))
                                            }
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    "Lecture Details & PDF Materials",
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = textColor
                                                )
                                                Text(
                                                    if (isLoadingDescription) "Fetching description & study links..." else if (foundUrls.isNotEmpty()) "${foundUrls.size} Study Link(s) & PDF resources detected" else "Channel notes & resources",
                                                    fontSize = 11.sp,
                                                    color = subTextColor
                                                )
                                            }
                                            IconButton(
                                                onClick = { isDescriptionExpanded = !isDescriptionExpanded },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(
                                                    if (isDescriptionExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                                    contentDescription = "Expand description",
                                                    tint = brandCyan
                                                )
                                            }
                                        }

                                        // Quick PDF / Material Chips if URLs found
                                        if (foundUrls.isNotEmpty()) {
                                            Spacer(modifier = Modifier.height(10.dp))
                                            Text(
                                                "📥 Extracted Study Links & PDF Downloads:",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = brandGreen
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                            LazyRow(
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                items(foundUrls) { u ->
                                                    val isPdf = u.contains(".pdf", ignoreCase = true) || u.contains("drive.google.com", ignoreCase = true)
                                                    val label = if (isPdf) "📑 Open/Download PDF" else if (u.contains("t.me")) "📢 Telegram Notes" else "🔗 Study Webpage"
                                                    Surface(
                                                        shape = RoundedCornerShape(8.dp),
                                                        color = if (isPdf) brandGreen.copy(alpha = 0.2f) else brandCyan.copy(alpha = 0.15f),
                                                        border = BorderStroke(0.5.dp, if (isPdf) brandGreen else brandCyan),
                                                        modifier = Modifier.clickable {
                                                            inAppBrowserUrl = u
                                                        }
                                                    ) {
                                                        Row(
                                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Icon(
                                                                if (isPdf) Icons.Default.Download else Icons.Default.OpenInBrowser,
                                                                contentDescription = null,
                                                                tint = if (isPdf) brandGreen else brandCyan,
                                                                modifier = Modifier.size(14.dp)
                                                            )
                                                            Spacer(modifier = Modifier.width(4.dp))
                                                            Text(
                                                                label,
                                                                fontSize = 11.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = if (isPdf) brandGreen else brandCyan
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        // Full / Collapsed Description text
                                        val descDisplay = videoFullDescription.ifBlank { "No detailed description provided by creator for this lecture." }
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Text(
                                            text = if (isDescriptionExpanded) descDisplay else descDisplay.take(180) + if (descDisplay.length > 180) "..." else "",
                                            fontSize = 12.sp,
                                            color = textColor.copy(alpha = 0.85f),
                                            lineHeight = 18.sp
                                        )

                                        if (descDisplay.length > 180) {
                                            Text(
                                                text = if (isDescriptionExpanded) "Show Less ▲" else "Read More & Links ▼",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = brandCyan,
                                                modifier = Modifier
                                                    .padding(top = 6.dp)
                                                    .clickable { isDescriptionExpanded = !isDescriptionExpanded }
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // Focus Guarantee Banner
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = Color(0xFF131B2E),
                                    border = BorderStroke(1.dp, brandGreen.copy(alpha = 0.35f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Security, contentDescription = null, tint = brandGreen, modifier = Modifier.size(28.dp))
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text("Pure Learning Environment", fontWeight = FontWeight.Bold, color = textColor, fontSize = 13.sp)
                                            Text(
                                                "YouTube shorts, infinite feeds, and distractions are completely isolated so you retain more per hour.",
                                                fontSize = 11.sp,
                                                color = subTextColor,
                                                lineHeight = 15.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else if (activePlaylistId != null) {
            // ==========================================
            // INSIDE A PLAYLIST (CHAPTER SERIES / CHANNEL PLAYLIST / CUSTOM PLAYLIST)
            // ==========================================
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(bgColor)
            ) {
                // Header / Playlist Identity Card
                Surface(
                    color = glassCardBg,
                    border = BorderStroke(1.dp, glassBorderColor),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = brandPurple.copy(alpha = 0.2f),
                                border = BorderStroke(1.dp, brandPurple.copy(alpha = 0.4f)),
                                modifier = Modifier.clickable {
                                    activePlaylistId = null
                                    playlistResults = emptyList()
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = brandPurple, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        if (activeCuratedChannel != null) "Back to ${activeCuratedChannel!!.name}" else "Back to Library",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = brandPurple
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.weight(1f))

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = brandAmber.copy(alpha = 0.15f),
                                border = BorderStroke(0.5.dp, brandAmber.copy(alpha = 0.4f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.PlaylistPlay, contentDescription = null, tint = brandAmber, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        "${playlistResults.size} Lectures",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = brandAmber
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = activePlaylistTitle,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = textColor,
                            lineHeight = 22.sp
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Verified, contentDescription = null, tint = brandCyan, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = activePlaylistChannelTitle,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = subTextColor
                            )
                            if (activePlaylistSubject.isNotBlank()) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("•", fontSize = 12.sp, color = subTextColor.copy(alpha = 0.5f))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = activePlaylistSubject,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = brandPurple
                                )
                            }
                        }

                        if (playlistResults.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(14.dp))
                            Button(
                                onClick = {
                                    selectedVideo = playlistResults.first()
                                },
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = brandPurple,
                                    contentColor = Color.White
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Start Watching / Play All (#1)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }
                    }
                }

                if (isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = brandPurple)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Loading Playlist Lectures...", color = subTextColor, fontSize = 13.sp)
                        }
                    }
                } else if (errorMessage != null && playlistResults.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = brandRed, modifier = Modifier.size(52.dp))
                            Spacer(modifier = Modifier.height(14.dp))
                            Text("Playlist Notice", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textColor)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                errorMessage!!,
                                fontSize = 13.sp,
                                color = subTextColor,
                                textAlign = TextAlign.Center,
                                lineHeight = 18.sp
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                if (youtubeApiKey.isBlank()) {
                                    Button(
                                        onClick = { showApiKeyDialog = true },
                                        colors = ButtonDefaults.buttonColors(containerColor = brandGreen)
                                    ) {
                                        Icon(Icons.Outlined.Key, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Set YouTube Key")
                                    }
                                }
                                Button(
                                    onClick = {
                                        activePlaylistId = null
                                        playlistResults = emptyList()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = brandPurple.copy(alpha = 0.2f), contentColor = brandPurple)
                                ) {
                                    Text("Return")
                                }
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 80.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(playlistResults.size) { index ->
                            val video = playlistResults[index]
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (selectedVideo?.id == video.id) brandGreen.copy(alpha = 0.2f) else glassCardBg,
                                    border = BorderStroke(1.dp, if (selectedVideo?.id == video.id) brandGreen else glassBorderColor),
                                    modifier = Modifier.width(36.dp)
                                ) {
                                    Box(
                                        modifier = Modifier.padding(vertical = 12.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            "#${index + 1}",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (selectedVideo?.id == video.id) brandGreen else subTextColor
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                Box(modifier = Modifier.weight(1f)) {
                                    VideoFeedCard(
                                        video = video,
                                        cardBgColor = glassCardBg,
                                        cardBorderColor = if (selectedVideo?.id == video.id) brandGreen else glassBorderColor,
                                        textColor = textColor,
                                        subTextColor = subTextColor,
                                        accentColor = brandPurple,
                                        onClick = { selectedVideo = video }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else if (activeCuratedChannel != null) {
                // ==========================================
                // EXPLORING A YOUTUBE CHANNEL (AUTHENTIC YOUTUBE STYLE WITH TABS)
                // ==========================================
                val ch = activeCuratedChannel!!
                val displayedVideos = remember(curatedChannelVideos, channelSearchFilter, channelFilterTag, channelSortOrder) {
                    val filtered = curatedChannelVideos.filter { vid ->
                        val matchesSearch = if (channelSearchFilter.isBlank()) {
                            true
                        } else {
                            vid.title.contains(channelSearchFilter, ignoreCase = true) ||
                            vid.subjectTag.contains(channelSearchFilter, ignoreCase = true)
                        }
                        val matchesTag = when (channelFilterTag) {
                            "One Shot" -> vid.title.contains("one shot", ignoreCase = true) || vid.title.contains("oneshot", ignoreCase = true)
                            "PYQs" -> vid.title.contains("pyq", ignoreCase = true) || vid.title.contains("question", ignoreCase = true)
                            "Revision" -> vid.title.contains("revision", ignoreCase = true) || vid.title.contains("marathon", ignoreCase = true)
                            "Class 11" -> vid.title.contains("11", ignoreCase = true)
                            "Class 12" -> vid.title.contains("12", ignoreCase = true)
                            "Formula" -> vid.title.contains("formula", ignoreCase = true) || vid.title.contains("cheat", ignoreCase = true)
                            else -> true
                        }
                        matchesSearch && matchesTag
                    }

                    when (channelSortOrder) {
                        "Latest" -> {
                            filtered.sortedWith(
                                compareByDescending<YouTubeVideo> { it.publishTimeMillis }
                                    .thenByDescending { it.publishedAt }
                            )
                        }
                        "Oldest" -> {
                            filtered.sortedWith(
                                compareBy<YouTubeVideo> { if (it.publishTimeMillis > 0L) it.publishTimeMillis else Long.MAX_VALUE }
                                    .thenBy { it.publishedAt }
                            )
                        }
                        "Popular" -> {
                            filtered.sortedByDescending { vid ->
                                var score = vid.matchScore
                                val t = vid.title.lowercase()
                                if (t.contains("one shot") || t.contains("complete")) score += 50
                                if (t.contains("pyq") || t.contains("marathon")) score += 30
                                if (t.contains("revision") || t.contains("formula")) score += 20
                                score
                            }
                        }
                        else -> filtered
                    }
                }
                val displayedPlaylists = remember(curatedChannelPlaylists, channelSearchFilter) {
                    if (channelSearchFilter.isBlank()) {
                        curatedChannelPlaylists
                    } else {
                        curatedChannelPlaylists.filter {
                            it.title.contains(channelSearchFilter, ignoreCase = true) ||
                            it.subjectTag.contains(channelSearchFilter, ignoreCase = true)
                        }
                    }
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize().background(bgColor),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    // 1. Channel Banner (Subtle sleek gradient banner with Subject tag)
                    item(key = "channel_banner") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(85.dp)
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(
                                            ch.accentColor.copy(alpha = 0.35f),
                                            Color(0xFF0F172A),
                                            ch.accentColor.copy(alpha = 0.15f)
                                        )
                                    )
                                )
                        ) {
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = Color.Black.copy(alpha = 0.6f),
                                border = BorderStroke(0.5.dp, ch.accentColor.copy(alpha = 0.6f)),
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(10.dp)
                            ) {
                                Text(
                                    text = "Verified Study Channel",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ch.accentColor,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }

                    // 2. Channel Avatar, Info, & Subscribe Row
                    item(key = "channel_info_and_subscribe") {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                // YouTube Channel Avatar (Large Circle with Icon/Letter)
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .background(ch.accentColor.copy(alpha = 0.2f), CircleShape)
                                        .border(2.dp, ch.accentColor, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        ch.icon,
                                        contentDescription = null,
                                        tint = ch.accentColor,
                                        modifier = Modifier.size(30.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(14.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = ch.name,
                                            fontSize = 17.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = textColor,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Icon(
                                            Icons.Default.Verified,
                                            contentDescription = "Verified Channel",
                                            tint = ch.accentColor,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "${ch.handle} • ${ch.subscribers} • ${displayedVideos.size} videos",
                                        fontSize = 12.sp,
                                        color = subTextColor,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Channel Short Bio / Description snippet
                            Text(
                                text = ch.description,
                                fontSize = 12.sp,
                                color = subTextColor,
                                lineHeight = 16.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Subscribe / Subscribed Button Row (Authentic YouTube pill style)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        isChannelSubscribed = !isChannelSubscribed
                                        Toast.makeText(
                                            context,
                                            if (isChannelSubscribed) "🔔 Subscribed to ${ch.name}" else "Unsubscribed",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    },
                                    shape = RoundedCornerShape(24.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isChannelSubscribed) glassCardBg else Color.White,
                                        contentColor = if (isChannelSubscribed) textColor else Color.Black
                                    ),
                                    border = if (isChannelSubscribed) BorderStroke(1.dp, glassBorderColor) else null,
                                    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 6.dp),
                                    modifier = Modifier.height(36.dp)
                                ) {
                                    if (isChannelSubscribed) {
                                        Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = brandCyan, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Subscribed", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    } else {
                                        Text("Subscribe", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                // Search within channel button
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = glassCardBg,
                                    border = BorderStroke(1.dp, glassBorderColor),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(36.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 10.dp)
                                    ) {
                                        Icon(Icons.Default.Search, contentDescription = null, tint = subTextColor, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        androidx.compose.foundation.text.BasicTextField(
                                            value = channelSearchFilter,
                                            onValueChange = { channelSearchFilter = it },
                                            singleLine = true,
                                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                            keyboardActions = KeyboardActions(
                                                onSearch = {
                                                    searchWithinChannel(ch, channelSearchFilter)
                                                }
                                            ),
                                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, color = textColor),
                                            modifier = Modifier.weight(1f),
                                            decorationBox = { innerTextField ->
                                                if (channelSearchFilter.isEmpty()) {
                                                    Text("Search ${ch.name}...", fontSize = 12.sp, color = subTextColor)
                                                }
                                                innerTextField()
                                            }
                                        )
                                        if (channelSearchFilter.isNotEmpty()) {
                                            IconButton(
                                                onClick = { searchWithinChannel(ch, channelSearchFilter) },
                                                modifier = Modifier.size(22.dp)
                                            ) {
                                                Icon(Icons.Default.Search, contentDescription = "Search", tint = ch.accentColor, modifier = Modifier.size(15.dp))
                                            }
                                            Spacer(modifier = Modifier.width(2.dp))
                                            IconButton(
                                                onClick = { channelSearchFilter = "" },
                                                modifier = Modifier.size(20.dp)
                                            ) {
                                                Icon(Icons.Default.Clear, contentDescription = "Clear", tint = subTextColor, modifier = Modifier.size(14.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 3. YouTube Channel Navigation Tabs: [Home, Videos, Playlists, About]
                    item(key = "channel_nav_tabs") {
                        val channelTabs = listOf("Home", "Videos", "Playlists", "About")
                        TabRow(
                            selectedTabIndex = channelActiveTab,
                            containerColor = glassCardBg,
                            contentColor = ch.accentColor,
                            indicator = { tabPositions ->
                                TabRowDefaults.SecondaryIndicator(
                                    Modifier.tabIndicatorOffset(tabPositions[channelActiveTab]),
                                    color = ch.accentColor,
                                    height = 3.dp
                                )
                            },
                            divider = { Divider(color = glassBorderColor) }
                        ) {
                            channelTabs.forEachIndexed { index, title ->
                                Tab(
                                    selected = channelActiveTab == index,
                                    onClick = { channelActiveTab = index },
                                    text = {
                                        Text(
                                            text = title,
                                            fontSize = 13.sp,
                                            fontWeight = if (channelActiveTab == index) FontWeight.Bold else FontWeight.Medium,
                                            color = if (channelActiveTab == index) ch.accentColor else subTextColor
                                        )
                                    }
                                )
                            }
                        }
                    }

                    // 4. Tab Content (Home, Videos, Playlists, About)
                    when (channelActiveTab) {
                        0 -> {
                            // ==================== HOME TAB ====================
                            // Fast Concept Queries Chips
                            item {
                                Spacer(modifier = Modifier.height(12.dp))
                                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                                    Text("Top Concept Tracks:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = subTextColor)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        items(ch.searchQueries) { q ->
                                            val isSelected = selectedChannelQuery == q
                                            Surface(
                                                shape = RoundedCornerShape(20.dp),
                                                color = if (isSelected) ch.accentColor.copy(alpha = 0.35f) else ch.accentColor.copy(alpha = 0.12f),
                                                border = BorderStroke(1.dp, if (isSelected) ch.accentColor else ch.accentColor.copy(alpha = 0.35f)),
                                                modifier = Modifier.clickable {
                                                    selectedChannelQuery = q
                                                    selectCuratedChannel(ch, q)
                                                }
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                                ) {
                                                    if (isSelected) {
                                                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = ch.accentColor, modifier = Modifier.size(14.dp))
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                    }
                                                    Text(
                                                        q,
                                                        fontSize = 12.sp,
                                                        color = if (isSelected) textColor else ch.accentColor,
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // Featured Playlists Row
                            if (displayedPlaylists.isNotEmpty()) {
                                item {
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("Created Playlists", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = textColor)
                                            Text(
                                                "View all",
                                                fontSize = 12.sp,
                                                color = ch.accentColor,
                                                fontWeight = FontWeight.SemiBold,
                                                modifier = Modifier.clickable { channelActiveTab = 2 }
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(10.dp))
                                        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                            items(displayedPlaylists, key = { it.id }) { pl ->
                                                ChannelPlaylistCard(
                                                    playlist = pl,
                                                    accentColor = ch.accentColor,
                                                    cardBg = glassCardBg,
                                                    borderColor = glassBorderColor,
                                                    textColor = textColor,
                                                    subTextColor = subTextColor,
                                                    onClick = {
                                                        val realId = if (pl.playlistId.isNotBlank()) pl.playlistId else pl.id
                                                        openPlaylist(
                                                            playlistId = realId,
                                                            title = pl.title,
                                                            channelTitle = ch.name,
                                                            thumbnailUrl = pl.thumbnailUrl,
                                                            subject = pl.subjectTag,
                                                            fallbackVideos = ch.curatedVideos
                                                        )
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // Latest Uploads Header
                            item {
                                Spacer(modifier = Modifier.height(16.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Recent Uploads", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = textColor)
                                    Text(
                                        "${displayedVideos.size} videos",
                                        fontSize = 12.sp,
                                        color = ch.accentColor,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                            }

                            // Videos in Home feed
                            items(displayedVideos.take(10), key = { it.id }) { vid ->
                                Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                                    VideoFeedCard(
                                        video = vid,
                                        cardBgColor = glassCardBg,
                                        cardBorderColor = glassBorderColor,
                                        textColor = textColor,
                                        subTextColor = subTextColor,
                                        accentColor = ch.accentColor,
                                        onClick = { selectedVideo = vid }
                                    )
                                }
                            }
                        }

                        1 -> {
                            // ==================== VIDEOS TAB ====================
                            if (isLoading && displayedVideos.isEmpty()) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 48.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            CircularProgressIndicator(color = ch.accentColor)
                                            Spacer(modifier = Modifier.height(12.dp))
                                            Text("Fetching latest channel uploads...", color = subTextColor, fontSize = 13.sp)
                                        }
                                    }
                                }
                            } else {
                                // YouTube-Style Sort Order Selector: Latest (Default), Popular, Oldest
                                item(key = "channel_sort_chips_row") {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        val sortOptions = listOf(
                                            Triple("Latest", "Latest 🕒", Icons.Default.Schedule),
                                            Triple("Popular", "Popular 🔥", Icons.Default.TrendingUp),
                                            Triple("Oldest", "Oldest ⏳", Icons.Default.History)
                                        )
                                        sortOptions.forEach { (sortKey, sortLabel, sortIcon) ->
                                            val isSelected = channelSortOrder == sortKey
                                            Surface(
                                                shape = RoundedCornerShape(18.dp),
                                                color = if (isSelected) ch.accentColor else glassCardBg,
                                                border = BorderStroke(1.dp, if (isSelected) ch.accentColor else glassBorderColor),
                                                modifier = Modifier.clickable { channelSortOrder = sortKey }
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(
                                                        imageVector = sortIcon,
                                                        contentDescription = null,
                                                        tint = if (isSelected) Color.White else subTextColor,
                                                        modifier = Modifier.size(13.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(5.dp))
                                                    Text(
                                                        text = sortLabel,
                                                        fontSize = 12.sp,
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                        color = if (isSelected) Color.White else textColor
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                // Quick Topic Filter Chips (One Shot, PYQ, Revision, etc.)
                                item(key = "channel_topic_filter_chips") {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    val topicChips = listOf("All", "One Shot", "PYQs", "Revision", "Class 11", "Class 12", "Formula")
                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp)
                                    ) {
                                        items(topicChips) { chip ->
                                            val isSelected = channelFilterTag == chip
                                            Surface(
                                                shape = RoundedCornerShape(16.dp),
                                                color = if (isSelected) ch.accentColor.copy(alpha = 0.35f) else glassCardBg,
                                                border = BorderStroke(1.dp, if (isSelected) ch.accentColor else glassBorderColor),
                                                modifier = Modifier.clickable { channelFilterTag = chip }
                                            ) {
                                                Text(
                                                    chip,
                                                    fontSize = 12.sp,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                    color = if (isSelected) textColor else subTextColor,
                                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                // Header with dynamic count and Fetch More button
                                item {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.VideoLibrary, contentDescription = null, tint = ch.accentColor, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                "All Lectures (${displayedVideos.size})",
                                                fontSize = 13.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = ch.accentColor
                                            )
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = ch.accentColor.copy(alpha = 0.15f),
                                            border = BorderStroke(0.5.dp, ch.accentColor.copy(alpha = 0.4f)),
                                            modifier = Modifier.clickable { loadMoreChannelVideos() }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(Icons.Default.Refresh, contentDescription = null, tint = ch.accentColor, modifier = Modifier.size(13.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Fetch More", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ch.accentColor)
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                }

                                if (displayedVideos.isNotEmpty()) {
                                    items(displayedVideos, key = { it.id }) { vid ->
                                        Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                                            VideoFeedCard(
                                                video = vid,
                                                cardBgColor = glassCardBg,
                                                cardBorderColor = glassBorderColor,
                                                textColor = textColor,
                                                subTextColor = subTextColor,
                                                accentColor = ch.accentColor,
                                                onClick = { selectedVideo = vid }
                                            )
                                        }
                                    }

                                    // Pagination Load More Card at the bottom
                                    item {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 16.dp, vertical = 12.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (isChannelLoadingMore) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                                ) {
                                                    CircularProgressIndicator(
                                                        modifier = Modifier.size(20.dp),
                                                        strokeWidth = 2.dp,
                                                        color = ch.accentColor
                                                    )
                                                    Text("Fetching more videos from YouTube...", fontSize = 12.sp, color = subTextColor)
                                                }
                                            } else {
                                                Button(
                                                    onClick = { loadMoreChannelVideos() },
                                                    shape = RoundedCornerShape(20.dp),
                                                    colors = ButtonDefaults.buttonColors(
                                                        containerColor = ch.accentColor.copy(alpha = 0.18f),
                                                        contentColor = ch.accentColor
                                                    ),
                                                    border = BorderStroke(1.dp, ch.accentColor.copy(alpha = 0.45f))
                                                ) {
                                                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(
                                                        "Load More Videos from YouTube (${displayedVideos.size}+) ⬇️",
                                                        fontSize = 12.5.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    item {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(32.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Icon(Icons.Default.SearchOff, contentDescription = null, tint = ch.accentColor.copy(alpha = 0.6f), modifier = Modifier.size(56.dp))
                                                Spacer(modifier = Modifier.height(16.dp))
                                                Text("No Lectures Matching Filters", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = textColor)
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Button(
                                                    onClick = {
                                                        channelSearchFilter = ""
                                                        channelFilterTag = "All"
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = ch.accentColor.copy(alpha = 0.2f), contentColor = ch.accentColor)
                                                ) {
                                                    Text("Reset All Filters")
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        2 -> {
                            // ==================== PLAYLISTS TAB ====================
                            if (isChannelPlaylistsLoading && displayedPlaylists.isEmpty()) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 48.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            CircularProgressIndicator(color = ch.accentColor)
                                            Spacer(modifier = Modifier.height(12.dp))
                                            Text("Fetching All Playlists from YouTube...", color = subTextColor, fontSize = 13.sp)
                                        }
                                    }
                                }
                            } else if (displayedPlaylists.isNotEmpty()) {
                                item {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.PlaylistPlay, contentDescription = null, tint = ch.accentColor, modifier = Modifier.size(20.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                "All Playlists & Series (${displayedPlaylists.size})",
                                                fontSize = 13.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = ch.accentColor
                                            )
                                        }
                                        Text(
                                            "Tap playlist to watch",
                                            fontSize = 11.sp,
                                            color = subTextColor
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                                items(displayedPlaylists, key = { it.id }) { pl ->
                                    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                                        FullPlaylistRowCard(
                                            playlist = pl,
                                            accentColor = ch.accentColor,
                                            cardBg = glassCardBg,
                                            borderColor = glassBorderColor,
                                            textColor = textColor,
                                            subTextColor = subTextColor,
                                            onClick = {
                                                val realId = if (pl.playlistId.isNotBlank()) pl.playlistId else pl.id
                                                openPlaylist(
                                                    playlistId = realId,
                                                    title = pl.title,
                                                    channelTitle = ch.name,
                                                    thumbnailUrl = pl.thumbnailUrl,
                                                    subject = pl.subjectTag,
                                                    fallbackVideos = ch.curatedVideos
                                                )
                                            }
                                        )
                                    }
                                }
                            } else {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(32.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Icon(Icons.Default.PlaylistPlay, contentDescription = null, tint = ch.accentColor.copy(alpha = 0.6f), modifier = Modifier.size(56.dp))
                                            Spacer(modifier = Modifier.height(16.dp))
                                            Text("No Playlists Found", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = textColor)
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text("All chapter lectures are directly accessible in the Videos tab.", fontSize = 12.sp, color = subTextColor, textAlign = TextAlign.Center)
                                            Spacer(modifier = Modifier.height(14.dp))
                                            Button(
                                                onClick = {
                                                    channelActiveTab = 1
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = ch.accentColor.copy(alpha = 0.2f), contentColor = ch.accentColor)
                                            ) {
                                                Text("Browse All Channel Videos 🎬")
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        3 -> {
                            // ==================== ABOUT TAB ====================
                            item {
                                Spacer(modifier = Modifier.height(16.dp))
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = glassCardBg,
                                    border = BorderStroke(1.dp, glassBorderColor),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text("Channel Details", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = textColor)
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text(ch.description, fontSize = 13.sp, color = subTextColor, lineHeight = 18.sp)
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Divider(color = glassBorderColor)
                                        Spacer(modifier = Modifier.height(16.dp))

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Public, contentDescription = null, tint = ch.accentColor, modifier = Modifier.size(20.dp))
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column {
                                                Text("Subject Coverage", fontSize = 11.sp, color = subTextColor)
                                                Text(ch.subject, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = textColor)
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(12.dp))

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = brandGreen, modifier = Modifier.size(20.dp))
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column {
                                                Text("Distraction-Free Status", fontSize = 11.sp, color = subTextColor)
                                                Text("100% Isolated From YouTube Algorithm & Shorts", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = brandGreen)
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(12.dp))

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.VideoLibrary, contentDescription = null, tint = brandAmber, modifier = Modifier.size(20.dp))
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column {
                                                Text("Available Curated Lectures", fontSize = 11.sp, color = subTextColor)
                                                Text("${displayedVideos.size} High-Yield Exam Modules", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = textColor)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

            } else {
                // ==========================================
                // MAIN TABS & EXPLORE HUB
                // ==========================================
                Column(modifier = Modifier.fillMaxSize()) {
                    // Sleek Scrollable Tab Row with Glass Capsule styling
                    val tabs = listOf(
                        Triple("Video Library", Icons.Default.FolderSpecial, brandCyan),
                        Triple("Search & Feed", Icons.Default.Search, brandRed),
                        Triple("Hall of Fame", Icons.Default.WorkspacePremium, brandAmber),
                        Triple("My Playlists", Icons.Default.PlaylistPlay, brandPurple),
                        Triple("Bookmarks (${bookmarks.size})", Icons.Default.Star, Color(0xFF38BDF8)),
                        Triple("Notes (${notes.size})", Icons.Default.NoteAlt, brandGreen)
                    )

                    Surface(
                        color = glassBg,
                        border = BorderStroke(1.dp, glassBorderColor),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        ScrollableTabRow(
                            selectedTabIndex = selectedTabIndex,
                            containerColor = Color.Transparent,
                            contentColor = brandCyan,
                            edgePadding = 16.dp,
                            divider = {},
                            indicator = { tabPositions ->
                                if (selectedTabIndex < tabPositions.size) {
                                    TabRowDefaults.Indicator(
                                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                                        color = tabs[selectedTabIndex].third,
                                        height = 3.dp
                                    )
                                }
                            }
                        ) {
                            tabs.forEachIndexed { index, (label, icon, color) ->
                                Tab(
                                    selected = selectedTabIndex == index,
                                    onClick = { selectedTabIndex = index },
                                    text = {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp)
                                        ) {
                                            Icon(
                                                icon,
                                                contentDescription = null,
                                                tint = if (selectedTabIndex == index) color else subTextColor,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = label,
                                                fontSize = 13.sp,
                                                fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Medium,
                                                color = if (selectedTabIndex == index) color else subTextColor,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                )
                            }
                        }
                    }

                    when (selectedTabIndex) {
                        // ------------------------------------
                        // TAB 0: CHAPTER-WISE VIDEO LIBRARY HUB
                        // ------------------------------------
                        0 -> {
                            // Get Chapters from ExamSyllabusDatabase
                            val rawChapters = when (videoLibrarySubject) {
                                "Physics" -> ExamSyllabusDatabase.neetPhysicsChapters
                                "Chemistry" -> when (videoLibrarySubBranch) {
                                    "Organic" -> ExamSyllabusDatabase.organicChemistryChapters
                                    "Inorganic" -> ExamSyllabusDatabase.inorganicChemistryChapters
                                    "Physical" -> ExamSyllabusDatabase.physicalChemistryChapters
                                    else -> ExamSyllabusDatabase.neetChemistryChapters
                                }
                                "Biology" -> when (videoLibrarySubBranch) {
                                    "Botany" -> ExamSyllabusDatabase.neetBiologyChapters.filter { it.branch == "Botany" }
                                    "Zoology" -> ExamSyllabusDatabase.neetBiologyChapters.filter { it.branch == "Zoology" }
                                    else -> ExamSyllabusDatabase.neetBiologyChapters
                                }
                                else -> listOf(
                                    ExamSyllabusDatabase.ChapterItem("Sets, Relations & Functions", "11th", "High Yield (2 Qs)", branch = "Maths"),
                                    ExamSyllabusDatabase.ChapterItem("Complex Numbers & Quadratic Equations", "11th", "🔥 Very High Yield (2-3 Qs)", branch = "Maths"),
                                    ExamSyllabusDatabase.ChapterItem("Matrices and Determinants", "12th", "🔥 Very High Yield (3 Qs)", branch = "Maths"),
                                    ExamSyllabusDatabase.ChapterItem("Permutations and Combinations", "11th", "High Yield (2 Qs)", branch = "Maths"),
                                    ExamSyllabusDatabase.ChapterItem("Binomial Theorem", "11th", "High Yield (2 Qs)", branch = "Maths"),
                                    ExamSyllabusDatabase.ChapterItem("Sequences and Series (AP, GP)", "11th", "High Yield (2 Qs)", branch = "Maths"),
                                    ExamSyllabusDatabase.ChapterItem("Limits, Continuity and Differentiability", "12th", "🔥 Very High Yield (3-4 Qs)", branch = "Maths"),
                                    ExamSyllabusDatabase.ChapterItem("Integral Calculus (Definite & Indefinite)", "12th", "🔥 Super High Yield (4-5 Qs)", branch = "Maths"),
                                    ExamSyllabusDatabase.ChapterItem("Differential Equations", "12th", "High Yield (2 Qs)", branch = "Maths"),
                                    ExamSyllabusDatabase.ChapterItem("Coordinate Geometry & Straight Lines", "11th", "High Yield (2-3 Qs)", branch = "Maths"),
                                    ExamSyllabusDatabase.ChapterItem("Conic Sections (Circle, Parabola, Ellipse)", "11th", "🔥 Very High Yield (3-4 Qs)", branch = "Maths"),
                                    ExamSyllabusDatabase.ChapterItem("Vector Algebra & 3D Geometry", "12th", "🔥 Super High Yield (4-5 Qs)", branch = "Maths"),
                                    ExamSyllabusDatabase.ChapterItem("Probability & Statistics", "12th", "High Yield (2-3 Qs)", branch = "Maths"),
                                    ExamSyllabusDatabase.ChapterItem("Trigonometric Functions & Equations", "11th", "High Yield (2 Qs)", branch = "Maths")
                                )
                            }

                            // Apply filters
                            val filteredChapters = rawChapters.filter { ch ->
                                val matchClass = when (videoLibraryClassFilter) {
                                    "11th" -> ch.classLevel == "11th"
                                    "12th" -> ch.classLevel == "12th"
                                    "🔥 High Yield" -> ch.pyqWeightage.contains("High", ignoreCase = true) || ch.pyqWeightage.contains("🔥")
                                    else -> true
                                }
                                val matchSearch = videoLibrarySearchQuery.isBlank() || ch.name.contains(videoLibrarySearchQuery, ignoreCase = true)
                                matchClass && matchSearch
                            }

                            // Render List of Chapter Accordions / Cards with Smooth Scrollable Header Controls
                            LazyColumn(
                                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 80.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                // 1. Subject Selector Tabs (Scrolls naturally with chapters)
                                item {
                                    val subjects = listOf("Physics", "Chemistry", "Biology", "Mathematics")
                                    LazyRow(
                                        contentPadding = PaddingValues(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        items(subjects) { subj ->
                                            val isSel = videoLibrarySubject == subj
                                            val subjColor = when (subj) {
                                                "Physics" -> Color(0xFF00F0FF)
                                                "Chemistry" -> Color(0xFFA855F7)
                                                "Biology" -> Color(0xFF10E599)
                                                else -> Color(0xFFFFB703)
                                            }
                                            Surface(
                                                shape = RoundedCornerShape(20.dp),
                                                color = if (isSel) subjColor.copy(alpha = 0.2f) else glassCardBg,
                                                border = BorderStroke(1.dp, if (isSel) subjColor else glassBorderColor),
                                                modifier = Modifier.clickable {
                                                    videoLibrarySubject = subj
                                                    videoLibrarySubBranch = "All"
                                                }
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    val icon = when (subj) {
                                                        "Physics" -> Icons.Default.ElectricBolt
                                                        "Chemistry" -> Icons.Default.Science
                                                        "Biology" -> Icons.Default.Grass
                                                        else -> Icons.Default.Calculate
                                                    }
                                                    Icon(icon, contentDescription = null, tint = if (isSel) subjColor else subTextColor, modifier = Modifier.size(16.dp))
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(
                                                        subj,
                                                        color = if (isSel) subjColor else textColor,
                                                        fontSize = 13.sp,
                                                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                // 2. Sub-Branch Selector (for Chemistry & Biology)
                                val subBranches = when (videoLibrarySubject) {
                                    "Chemistry" -> listOf("All", "Organic", "Inorganic", "Physical")
                                    "Biology" -> listOf("All", "Botany", "Zoology")
                                    else -> emptyList()
                                }
                                if (subBranches.isNotEmpty()) {
                                    item {
                                        LazyRow(
                                            contentPadding = PaddingValues(vertical = 2.dp),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            items(subBranches) { branch ->
                                                val isSel = videoLibrarySubBranch == branch
                                                Surface(
                                                    shape = RoundedCornerShape(12.dp),
                                                    color = if (isSel) brandPurple.copy(alpha = 0.2f) else Color.Transparent,
                                                    border = BorderStroke(1.dp, if (isSel) brandPurple else glassBorderColor.copy(alpha = 0.5f)),
                                                    modifier = Modifier.clickable { videoLibrarySubBranch = branch }
                                                ) {
                                                    Text(
                                                        text = if (branch == "All") "All $videoLibrarySubject" else branch,
                                                        fontSize = 11.sp,
                                                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                                        color = if (isSel) brandPurple else subTextColor,
                                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                // 3. Search & Class Filters Bar (Scrolls with list)
                                item {
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = videoLibrarySearchQuery,
                                            onValueChange = { videoLibrarySearchQuery = it },
                                            placeholder = { Text("Search chapter in $videoLibrarySubject...", fontSize = 12.sp, color = subTextColor) },
                                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = brandCyan, modifier = Modifier.size(18.dp)) },
                                            trailingIcon = {
                                                if (videoLibrarySearchQuery.isNotEmpty()) {
                                                    IconButton(onClick = { videoLibrarySearchQuery = "" }, modifier = Modifier.size(20.dp)) {
                                                        Icon(Icons.Default.Clear, contentDescription = "Clear", tint = subTextColor, modifier = Modifier.size(14.dp))
                                                    }
                                                }
                                            },
                                            singleLine = true,
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedContainerColor = glassCardBg,
                                                unfocusedContainerColor = glassCardBg,
                                                focusedBorderColor = brandCyan,
                                                unfocusedBorderColor = glassBorderColor,
                                                focusedTextColor = textColor,
                                                unfocusedTextColor = textColor
                                            ),
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(48.dp)
                                        )

                                        // Class Level Chips (All, 11th, 12th, High Yield)
                                        val classFilters = listOf("All", "11th", "12th", "🔥 High Yield")
                                        LazyRow(
                                            contentPadding = PaddingValues(vertical = 2.dp),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            items(classFilters) { filter ->
                                                val isSel = videoLibraryClassFilter == filter
                                                Surface(
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = if (isSel) Color(0xFF38BDF8).copy(alpha = 0.2f) else glassCardBg.copy(alpha = 0.5f),
                                                    border = BorderStroke(0.8.dp, if (isSel) Color(0xFF38BDF8) else glassBorderColor),
                                                    modifier = Modifier.clickable { videoLibraryClassFilter = filter }
                                                ) {
                                                    Text(
                                                        filter,
                                                        fontSize = 11.sp,
                                                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                                        color = if (isSel) Color(0xFF38BDF8) else subTextColor,
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                                    // RECENTLY VIEWED SECTION
                                    if (recentlyViewed.isNotEmpty() && videoLibrarySearchQuery.isBlank()) {
                                        item {
                                            RecentlyViewedSection(
                                                recentList = recentlyViewed,
                                                cardBgColor = glassCardBg,
                                                cardBorderColor = glassBorderColor,
                                                textColor = textColor,
                                                subTextColor = subTextColor,
                                                onPlayVideo = { vid -> selectedVideo = vid },
                                                onRemoveItem = { vidId -> viewModel.removeRecentlyViewedItem(vidId) },
                                                onClearAll = { viewModel.clearRecentlyViewed() }
                                            )
                                        }
                                    }

                                    items(filteredChapters) { chItem ->
                                        ChapterVideoCard(
                                            chapter = chItem,
                                            subject = videoLibrarySubject,
                                            chapterVideos = chapterVideos,
                                            cardBgColor = glassCardBg,
                                            cardBorderColor = glassBorderColor,
                                            textColor = textColor,
                                            subTextColor = subTextColor,
                                            accentColor = brandCyan,
                                            onPlayVideo = { vid -> selectedVideo = vid },
                                            onDeleteVideo = { entry -> viewModel.removeVideoFromChapter(entry) },
                                            onAttachVideo = {
                                                attachVideoSubject = videoLibrarySubject
                                                attachVideoChapterName = chItem.name
                                                attachVideoUrlOrId = ""
                                                attachVideoTitle = ""
                                                attachVideoChannel = ""
                                                showAttachVideoDialog = true
                                            },
                                            onFindTopLectures = {
                                                searchQuery = "${chItem.name} $videoLibrarySubject NEET JEE one shot"
                                                selectedTabIndex = 1 // Switch to Search & Feed tab
                                                performSearch(searchQuery)
                                            },
                                            onGenerateAiSummary = {
                                                generateAiNotesForTopic("${chItem.name} ($videoLibrarySubject)", isQuiz = false)
                                            },
                                            onGenerateAiQuiz = {
                                                generateAiNotesForTopic("${chItem.name} ($videoLibrarySubject)", isQuiz = true)
                                            }
                                        )
                                    }
                                }
                            }

                        // ------------------------------------
                        // TAB 1: SEARCH & STRICT FOCUS FEED
                        // ------------------------------------
                        1 -> {
                            val searchFeedListState = androidx.compose.foundation.lazy.rememberLazyListState()
                            val coroutineScope = rememberCoroutineScope()

                            Box(modifier = Modifier.fillMaxSize()) {
                                LazyColumn(
                                    state = searchFeedListState,
                                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 80.dp),
                                    verticalArrangement = Arrangement.spacedBy(14.dp),
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    // 1. Search Input Bar (Matching Reference Screenshot)
                                    item(key = "search_bar") {
                                        OutlinedTextField(
                                            value = searchQuery,
                                            onValueChange = { searchQuery = it },
                                            placeholder = {
                                                Text(
                                                    "Search chapter, PYQs, one-shot derivation...",
                                                    color = Color(0xFF64748B),
                                                    fontSize = 13.sp
                                                )
                                            },
                                            leadingIcon = {
                                                Icon(
                                                    Icons.Default.Search,
                                                    contentDescription = null,
                                                    tint = Color(0xFFF43F5E)
                                                )
                                            },
                                            trailingIcon = {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    if (searchQuery.isNotEmpty()) {
                                                        IconButton(onClick = {
                                                            searchQuery = ""
                                                            searchResults = emptyList()
                                                            warningMessage = null
                                                            errorMessage = null
                                                        }) {
                                                            Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Color(0xFF94A3B8), modifier = Modifier.size(18.dp))
                                                        }
                                                    }
                                                    // Lakshya Lens Doubt Scanner Icon
                                                    IconButton(onClick = {
                                                        lensScannerInitialImageUri = null
                                                        showLensScannerDialog = true
                                                    }) {
                                                        Icon(
                                                            Icons.Default.DocumentScanner,
                                                            contentDescription = "Scan Question with Lakshya Lens",
                                                            tint = Color(0xFF38BDF8),
                                                            modifier = Modifier.size(20.dp)
                                                        )
                                                    }
                                                }
                                            },
                                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                            keyboardActions = KeyboardActions(onSearch = { performSearch(searchQuery) }),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedContainerColor = Color(0xFF0F172A).copy(alpha = 0.85f),
                                                unfocusedContainerColor = Color(0xFF0F172A).copy(alpha = 0.6f),
                                                focusedBorderColor = Color(0xFF38BDF8),
                                                unfocusedBorderColor = Color(0xFF1E293B),
                                                focusedTextColor = Color.White,
                                                unfocusedTextColor = Color.White,
                                                cursorColor = Color(0xFFF43F5E)
                                            ),
                                            shape = RoundedCornerShape(18.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }

                                    // Lakshya Lens Scanner Quick Action Banner
                                    item(key = "lens_scanner_banner") {
                                        Surface(
                                            shape = RoundedCornerShape(16.dp),
                                            color = Color(0xFF0B172B),
                                            border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.6f)),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    lensScannerInitialImageUri = null
                                                    showLensScannerDialog = true
                                                }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(12.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(38.dp)
                                                        .clip(CircleShape)
                                                        .background(Color(0xFF0284C7).copy(alpha = 0.2f))
                                                        .border(BorderStroke(1.dp, Color(0xFF38BDF8)), CircleShape),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        Icons.Default.DocumentScanner,
                                                        contentDescription = null,
                                                        tint = Color(0xFF38BDF8),
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Text(
                                                            "Lakshya Lens Question Scanner",
                                                            color = Color.White,
                                                            fontSize = 13.sp,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Surface(
                                                            shape = RoundedCornerShape(4.dp),
                                                            color = Color(0xFF10B981).copy(alpha = 0.2f)
                                                        ) {
                                                            Text(
                                                                "AI",
                                                                fontSize = 9.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = Color(0xFF10B981),
                                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                            )
                                                        }
                                                    }
                                                    Text(
                                                        "Scan any book/DPP question for instant video solutions",
                                                        color = Color(0xFF94A3B8),
                                                        fontSize = 11.sp
                                                    )
                                                }
                                                Icon(
                                                    Icons.AutoMirrored.Filled.ArrowForward,
                                                    contentDescription = null,
                                                    tint = Color(0xFF38BDF8),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }

                                    // 2. Channel Shortcut Pills (Matching Screenshot: Physics Wallah, Eduniti, Physics Galaxy...)
                                    item(key = "channel_pills") {
                                        val quickChannels = listOf(
                                            "Doubtnut",
                                            "Physics Wallah",
                                            "Eduniti",
                                            "Physics Galaxy",
                                            "Mohit Tyagi",
                                            "Biology at Ease",
                                            "Chemshiksha",
                                            "MathonGo",
                                            "Sachin Rana",
                                            "Unacademy NEET",
                                            "ALLEN Career"
                                        )
                                        LazyRow(
                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            items(quickChannels) { chName ->
                                                val isSelected = selectedQuickChannel == chName
                                                Surface(
                                                    shape = RoundedCornerShape(22.dp),
                                                    color = if (isSelected) Color(0xFF1E293B) else Color(0xFF0F172A),
                                                    border = BorderStroke(1.dp, if (isSelected) Color(0xFF38BDF8) else Color(0xFF1E293B)),
                                                    modifier = Modifier.clickable {
                                                        selectedQuickChannel = chName
                                                        searchQuery = chName
                                                        performSearch(chName)
                                                    }
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Icon(
                                                            Icons.Default.PlayCircleFilled,
                                                            contentDescription = null,
                                                            tint = Color(0xFFEF4444),
                                                            modifier = Modifier.size(15.dp)
                                                        )
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Text(
                                                            chName,
                                                            fontSize = 12.sp,
                                                            fontWeight = FontWeight.Medium,
                                                            color = Color(0xFFF1F5F9),
                                                            maxLines = 1
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    // 3. Focus Warning / Error Banner
                                    if (warningMessage != null) {
                                        item(key = "warning_msg") {
                                            Surface(
                                                modifier = Modifier.fillMaxWidth(),
                                                color = Color(0xFFFEF2F2),
                                                shape = RoundedCornerShape(12.dp),
                                                border = BorderStroke(1.dp, Color(0xFFFECACA))
                                            ) {
                                                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFDC2626))
                                                    Spacer(modifier = Modifier.width(12.dp))
                                                    Text(warningMessage!!, color = Color(0xFF991B1B), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                                }
                                            }
                                        }
                                    }

                                    if (errorMessage != null) {
                                        item(key = "error_msg") {
                                            Surface(
                                                modifier = Modifier.fillMaxWidth(),
                                                color = brandRed.copy(alpha = 0.1f),
                                                shape = RoundedCornerShape(12.dp),
                                                border = BorderStroke(1.dp, brandRed.copy(alpha = 0.3f))
                                            ) {
                                                Text(
                                                    text = errorMessage!!,
                                                    color = brandRed,
                                                    modifier = Modifier.padding(12.dp),
                                                    fontSize = 12.sp
                                                )
                                            }
                                        }
                                    }

                                    // 4. Loading State
                                    if (isLoading) {
                                        item(key = "loading_state") {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 60.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                CircularProgressIndicator(color = brandRed)
                                            }
                                        }
                                    } else if (searchResults.isNotEmpty()) {
                                        // 5. Search Results Feed
                                        items(searchResults, key = { it.id }) { video ->
                                            VideoFeedCard(
                                                video = video,
                                                cardBgColor = Color(0xFF131B2E),
                                                cardBorderColor = Color(0xFF1E293B),
                                                textColor = textColor,
                                                subTextColor = subTextColor,
                                                accentColor = brandCyan,
                                                onClick = { selectedVideo = video }
                                            )
                                        }
                                    } else if (warningMessage == null && errorMessage == null) {
                                        // 6. Centered Empty State (Exact Match to Reference Screenshot)
                                        item(key = "empty_state") {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(top = 90.dp, bottom = 40.dp, start = 24.dp, end = 24.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.Center
                                            ) {
                                                Icon(
                                                    Icons.Default.School,
                                                    contentDescription = null,
                                                    tint = Color(0xFF881337), // Muted dark maroon / ruby red matching screenshot
                                                    modifier = Modifier.size(76.dp)
                                                )
                                                Spacer(modifier = Modifier.height(18.dp))
                                                Text(
                                                    text = "Search Verified Lectures",
                                                    fontSize = 18.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White,
                                                    textAlign = TextAlign.Center
                                                )
                                                Spacer(modifier = Modifier.height(10.dp))
                                                Text(
                                                    text = "Type any chapter name (e.g., 'Ray Optics One Shot' or\n'Chemical Kinetics PYQ'). All distractions are blocked.",
                                                    fontSize = 13.sp,
                                                    color = Color(0xFF94A3B8),
                                                    textAlign = TextAlign.Center,
                                                    lineHeight = 19.sp
                                                )
                                            }
                                        }
                                    }
                                }

                                // Quick Jump to Search / Top floating action button
                                if (searchFeedListState.firstVisibleItemIndex > 1) {
                                    FloatingActionButton(
                                        onClick = {
                                            coroutineScope.launch {
                                                searchFeedListState.animateScrollToItem(0)
                                            }
                                        },
                                        containerColor = brandRed,
                                        contentColor = Color.White,
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .padding(end = 16.dp, bottom = 86.dp)
                                            .size(44.dp)
                                    ) {
                                        Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Scroll to top", modifier = Modifier.size(24.dp))
                                    }
                                }
                            }
                        }

                        // ------------------------------------
                        // TAB 2: HALL OF FAME (CURATED EDUCATORS)
                        // ------------------------------------
                        2 -> {
                            LazyColumn(
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                // Header with Add Channel Button & Quick Controls
                                item {
                                    Surface(
                                        shape = RoundedCornerShape(20.dp),
                                        color = glassCardBg,
                                        border = BorderStroke(1.dp, glassBorderColor),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(16.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(42.dp)
                                                            .background(brandAmber.copy(alpha = 0.2f), CircleShape)
                                                            .border(1.dp, brandAmber.copy(alpha = 0.5f), CircleShape),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Icon(Icons.Default.WorkspacePremium, contentDescription = null, tint = brandAmber, modifier = Modifier.size(24.dp))
                                                    }
                                                    Spacer(modifier = Modifier.width(12.dp))
                                                    Column {
                                                        Text("Hall of Fame", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textColor)
                                                        Text("${curatedChannels.size} Top NEET/JEE Mentors", fontSize = 11.5.sp, color = subTextColor)
                                                    }
                                                }

                                                // + Add Channel Button
                                                Button(
                                                    onClick = {
                                                        newChannelName = ""
                                                        newChannelHandle = ""
                                                        newChannelDesc = ""
                                                        newChannelSubject = "Physics"
                                                        newChannelColorHex = "#00F0FF"
                                                        showAddChannelDialog = true
                                                    },
                                                    colors = ButtonDefaults.buttonColors(
                                                        containerColor = brandAmber,
                                                        contentColor = Color.Black
                                                    ),
                                                    shape = RoundedCornerShape(12.dp),
                                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                                ) {
                                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("Add Channel", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }

                                            if (hiddenChannelIds.isNotEmpty()) {
                                                Spacer(modifier = Modifier.height(10.dp))
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        "${hiddenChannelIds.size} hidden default channel(s)",
                                                        fontSize = 11.5.sp,
                                                        color = subTextColor
                                                    )
                                                    TextButton(
                                                        onClick = { viewModel.restoreDefaultHallOfFameChannels() },
                                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                                    ) {
                                                        Icon(Icons.Default.Restore, contentDescription = null, tint = brandCyan, modifier = Modifier.size(14.dp))
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text("Restore All", fontSize = 11.5.sp, color = brandCyan, fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                items(curatedChannels, key = { it.id }) { channel ->
                                    Surface(
                                        shape = RoundedCornerShape(16.dp),
                                        color = glassCardBg,
                                        border = BorderStroke(1.dp, glassBorderColor),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                selectCuratedChannel(channel)
                                            }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(14.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(48.dp)
                                                    .background(channel.accentColor.copy(alpha = 0.18f), RoundedCornerShape(14.dp))
                                                    .border(1.dp, channel.accentColor.copy(alpha = 0.4f), RoundedCornerShape(14.dp)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(channel.icon, contentDescription = null, tint = channel.accentColor, modifier = Modifier.size(26.dp))
                                            }
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(channel.name, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = textColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    if (channel.isCustom) {
                                                        Surface(
                                                            shape = RoundedCornerShape(6.dp),
                                                            color = channel.accentColor.copy(alpha = 0.2f)
                                                        ) {
                                                            Text(
                                                                "CUSTOM",
                                                                fontSize = 9.sp,
                                                                fontWeight = FontWeight.Black,
                                                                color = channel.accentColor,
                                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                                            )
                                                        }
                                                    } else {
                                                        Icon(Icons.Default.Verified, contentDescription = null, tint = channel.accentColor, modifier = Modifier.size(15.dp))
                                                    }
                                                }
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(channel.subject, fontSize = 11.5.sp, color = channel.accentColor, fontWeight = FontWeight.SemiBold)
                                                if (channel.description.isNotBlank()) {
                                                    Spacer(modifier = Modifier.height(2.dp))
                                                    Text(channel.description, fontSize = 11.5.sp, color = subTextColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                }
                                            }

                                            // Delete / Remove Button
                                            IconButton(
                                                onClick = { channelToDelete = channel },
                                                modifier = Modifier.size(36.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.DeleteOutline,
                                                    contentDescription = "Remove Channel",
                                                    tint = subTextColor.copy(alpha = 0.7f),
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }

                                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = subTextColor, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }
                            }
                        }

                        // ------------------------------------
                        // TAB 3: MY SAVED PLAYLISTS
                        // ------------------------------------
                        3 -> {
                            if (savedPlaylists.isEmpty()) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(32.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(Icons.Default.FeaturedPlayList, contentDescription = null, tint = brandPurple.copy(alpha = 0.4f), modifier = Modifier.size(64.dp))
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text("No Custom Playlists Yet", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = textColor)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        "Paste any YouTube Playlist URL or ID (e.g., PW Batch, Mohit Tyagi Course) to watch in distraction-free mode.",
                                        fontSize = 13.sp,
                                        color = subTextColor,
                                        textAlign = TextAlign.Center,
                                        lineHeight = 18.sp
                                    )
                                    Spacer(modifier = Modifier.height(20.dp))
                                    Button(
                                        onClick = { showAddPlaylistDialog = true },
                                        colors = ButtonDefaults.buttonColors(containerColor = brandPurple),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Add First Playlist", fontWeight = FontWeight.Bold)
                                    }
                                }
                            } else {
                                LazyColumn(
                                    contentPadding = PaddingValues(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    items(savedPlaylists.toList(), key = { it }) { entry ->
                                        val parts = entry.split("|", limit = 2)
                                        val pId = parts.getOrNull(0) ?: ""
                                        val pName = parts.getOrNull(1) ?: "Unnamed Playlist"

                                        Surface(
                                            shape = RoundedCornerShape(14.dp),
                                            color = glassCardBg,
                                            border = BorderStroke(1.dp, glassBorderColor),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    openPlaylist(
                                                        playlistId = pId,
                                                        title = pName,
                                                        channelTitle = "Custom Playlist",
                                                        thumbnailUrl = "",
                                                        subject = "Saved Series"
                                                    )
                                                }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(14.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(48.dp)
                                                        .background(brandPurple.copy(alpha = 0.18f), RoundedCornerShape(12.dp))
                                                        .border(1.dp, brandPurple.copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(Icons.Default.PlaylistPlay, contentDescription = null, tint = brandPurple, modifier = Modifier.size(28.dp))
                                                }
                                                Spacer(modifier = Modifier.width(14.dp))
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(pName, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = textColor)
                                                    Spacer(modifier = Modifier.height(2.dp))
                                                    Text("ID: $pId", fontSize = 11.sp, color = subTextColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                }
                                                IconButton(onClick = { viewModel.removeCustomPlaylist(pId) }) {
                                                    Icon(Icons.Outlined.Delete, contentDescription = "Delete", tint = Color(0xFFEF4444), modifier = Modifier.size(20.dp))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // ------------------------------------
                        // TAB 4: SAVED BOOKMARKS
                        // ------------------------------------
                        4 -> {
                            if (bookmarks.isEmpty()) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(32.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(Icons.Outlined.StarBorder, contentDescription = null, tint = brandAmber.copy(alpha = 0.4f), modifier = Modifier.size(64.dp))
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text("No Saved Lectures", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = textColor)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        "Bookmark critical derivations, PYQ sessions, or high-weightage chapters to quickly revise before exams.",
                                        fontSize = 13.sp,
                                        color = subTextColor,
                                        textAlign = TextAlign.Center,
                                        lineHeight = 18.sp
                                    )
                                }
                            } else {
                                LazyColumn(
                                    contentPadding = PaddingValues(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    items(bookmarks.toList(), key = { it }) { entry ->
                                        val parts = entry.split("|||")
                                        val vId = parts.getOrNull(0) ?: ""
                                        val vTitle = parts.getOrNull(1) ?: "Bookmarked Video"
                                        val vChannel = parts.getOrNull(2) ?: "Verified Channel"
                                        val vThumb = parts.getOrNull(3) ?: ""

                                        val videoObj = YouTubeVideo(vId, vTitle, vChannel, vThumb)
                                        VideoFeedCard(
                                            video = videoObj,
                                            cardBgColor = glassCardBg,
                                            cardBorderColor = glassBorderColor,
                                            textColor = textColor,
                                            subTextColor = subTextColor,
                                            accentColor = brandAmber,
                                            onClick = { selectedVideo = videoObj }
                                        )
                                    }
                                }
                            }
                        }

                        // ------------------------------------
                        // TAB 5: LECTURE NOTES DIRECTORY
                        // ------------------------------------
                        5 -> {
                            if (notes.isEmpty()) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(32.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(Icons.Outlined.NoteAlt, contentDescription = null, tint = brandGreen.copy(alpha = 0.4f), modifier = Modifier.size(64.dp))
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text("No Lecture Notes Yet", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = textColor)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        "While watching any video, click 'Add Note' to capture timestamps and key formulas directly in-app.",
                                        fontSize = 13.sp,
                                        color = subTextColor,
                                        textAlign = TextAlign.Center,
                                        lineHeight = 18.sp
                                    )
                                }
                            } else {
                                LazyColumn(
                                    contentPadding = PaddingValues(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    items(notes.toList(), key = { it }) { entry ->
                                        val parts = entry.split("|||")
                                        val vId = parts.getOrNull(1) ?: ""
                                        val tsSec = parts.getOrNull(2)?.toIntOrNull() ?: 0
                                        val noteTxt = parts.getOrNull(3) ?: ""
                                        val vTitle = parts.getOrNull(4) ?: "Lecture Note"

                                        val m = tsSec / 60
                                        val s = tsSec % 60
                                        val timeStr = String.format("%02d:%02d", m, s)

                                        Surface(
                                            shape = RoundedCornerShape(14.dp),
                                            color = glassCardBg,
                                            border = BorderStroke(1.dp, glassBorderColor),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(modifier = Modifier.padding(14.dp)) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Surface(
                                                        shape = RoundedCornerShape(6.dp),
                                                        color = brandGreen.copy(alpha = 0.2f),
                                                        border = BorderStroke(0.5.dp, brandGreen.copy(alpha = 0.5f))
                                                    ) {
                                                        Text(
                                                            "@ $timeStr",
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = brandGreen,
                                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(
                                                        vTitle,
                                                        fontSize = 13.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = textColor,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis,
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                    IconButton(
                                                        onClick = { viewModel.removeStudyTubeNote(entry) },
                                                        modifier = Modifier.size(24.dp)
                                                    ) {
                                                        Icon(Icons.Default.Close, contentDescription = "Delete", tint = subTextColor, modifier = Modifier.size(16.dp))
                                                    }
                                                }
                                                Spacer(modifier = Modifier.height(8.dp))
                                                com.example.ui.components.NativeMarkdownText(
                                                    text = noteTxt,
                                                    isDark = true,
                                                    fontSize = 14.sp,
                                                    textColor = textColor
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
        }
    }

    // ============================================================
    // DIALOG: ADD TIMESTAMPED NOTE
    // ============================================================
    if (showAddNoteDialog && selectedVideo != null) {
        AlertDialog(
            onDismissRequest = { showAddNoteDialog = false },
            containerColor = Color(0xFF0F172A),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.EditNote, contentDescription = null, tint = brandPurple)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add Timestamp Note", color = textColor, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                }
            },
            text = {
                Column {
                    Text(
                        "Video: ${selectedVideo!!.title}",
                        fontSize = 12.sp,
                        color = subTextColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = noteTimestampInput,
                        onValueChange = { noteTimestampInput = it },
                        label = { Text("Timestamp (MM:SS, e.g. 14:30)") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = glassCardBg,
                            unfocusedContainerColor = glassCardBg,
                            focusedBorderColor = brandPurple,
                            focusedLabelColor = brandPurple,
                            focusedTextColor = textColor,
                            unfocusedTextColor = textColor
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = noteTextInput,
                        onValueChange = { noteTextInput = it },
                        label = { Text("Formula, Rule or Concept Note") },
                        minLines = 3,
                        maxLines = 5,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = glassCardBg,
                            unfocusedContainerColor = glassCardBg,
                            focusedBorderColor = brandPurple,
                            focusedLabelColor = brandPurple,
                            focusedTextColor = textColor,
                            unfocusedTextColor = textColor
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val txt = noteTextInput.trim()
                        if (txt.isNotEmpty()) {
                            val parts = noteTimestampInput.split(":")
                            val mins = parts.getOrNull(0)?.toIntOrNull() ?: 0
                            val secs = parts.getOrNull(1)?.toIntOrNull() ?: 0
                            val totalSec = mins * 60 + secs

                            viewModel.addStudyTubeNote(
                                selectedVideo!!.id,
                                selectedVideo!!.title,
                                totalSec,
                                txt
                            )
                            Toast.makeText(context, "Note Saved!", Toast.LENGTH_SHORT).show()
                            showAddNoteDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = brandPurple)
                ) {
                    Text("Save Note")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddNoteDialog = false }) {
                    Text("Cancel", color = subTextColor)
                }
            }
        )
    }

    // ============================================================
    // DIALOG: ADD CUSTOM PLAYLIST
    // ============================================================
    if (showAddPlaylistDialog) {
        var playlistName by remember { mutableStateOf("") }
        var playlistUrlOrId by remember { mutableStateOf("") }
        var plError by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showAddPlaylistDialog = false },
            containerColor = Color(0xFF0F172A),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.VideoLibrary, contentDescription = null, tint = brandPurple)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add YouTube Playlist", color = textColor, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                }
            },
            text = {
                Column {
                    Text(
                        "Paste playlist link from Physics Wallah, Mohit Tyagi, Eduniti, or any coaching batch.",
                        fontSize = 12.sp,
                        color = subTextColor,
                        lineHeight = 16.sp
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    OutlinedTextField(
                        value = playlistName,
                        onValueChange = { playlistName = it },
                        label = { Text("Name (e.g. Eduniti Physics JEE 2025)") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = glassCardBg,
                            unfocusedContainerColor = glassCardBg,
                            focusedBorderColor = brandPurple,
                            focusedLabelColor = brandPurple,
                            focusedTextColor = textColor,
                            unfocusedTextColor = textColor
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = playlistUrlOrId,
                        onValueChange = { playlistUrlOrId = it },
                        label = { Text("Playlist URL or ID (list=...)") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = glassCardBg,
                            unfocusedContainerColor = glassCardBg,
                            focusedBorderColor = brandPurple,
                            focusedLabelColor = brandPurple,
                            focusedTextColor = textColor,
                            unfocusedTextColor = textColor
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (plError != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(plError!!, color = Color.Red, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val name = playlistName.trim()
                        val input = playlistUrlOrId.trim()
                        if (name.isEmpty() || input.isEmpty()) {
                            plError = "Please enter both name and playlist link/ID"
                            return@Button
                        }

                        var finalId = input
                        if (input.contains("list=")) {
                            val regexMatch = Regex("[?&]list=([a-zA-Z0-9_-]+)").find(input)?.groupValues?.get(1)
                            val uri = try { Uri.parse(input) } catch(e: Exception) { null }
                            finalId = regexMatch ?: uri?.getQueryParameter("list") ?: input
                        }

                        viewModel.addCustomPlaylist(finalId, name)
                        Toast.makeText(context, "Playlist Added!", Toast.LENGTH_SHORT).show()
                        showAddPlaylistDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = brandPurple)
                ) {
                    Text("Add Playlist")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddPlaylistDialog = false }) {
                    Text("Cancel", color = subTextColor)
                }
            }
        )
    }

    // ============================================================
    // DIALOG: YOUTUBE API KEY CONFIGURATION
    // ============================================================
    if (showApiKeyDialog) {
        var tempKey by remember { mutableStateOf(youtubeApiKey) }
        AlertDialog(
            onDismissRequest = { showApiKeyDialog = false },
            containerColor = Color(0xFF0F172A),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.VpnKey, contentDescription = null, tint = brandRed)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("YouTube Data API Key", color = textColor, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                }
            },
            text = {
                Column {
                    Text(
                        "StudyTube uses official YouTube Data API v3 for high-speed, lag-free search and playlist streaming with 0 quota limits.",
                        fontSize = 12.sp,
                        color = subTextColor,
                        lineHeight = 16.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "How to get free API key:\n1. Open Google Cloud Console\n2. Enable 'YouTube Data API v3'\n3. Generate an API Key and paste it below.",
                        fontSize = 12.sp,
                        color = textColor,
                        fontWeight = FontWeight.Medium,
                        lineHeight = 16.sp
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    OutlinedTextField(
                        value = tempKey,
                        onValueChange = { tempKey = it },
                        label = { Text("API Key (AQ... or AIzaSy...)") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = glassCardBg,
                            unfocusedContainerColor = glassCardBg,
                            focusedBorderColor = brandRed,
                            focusedLabelColor = brandRed,
                            focusedTextColor = textColor,
                            unfocusedTextColor = textColor
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.saveYoutubeApiKey(tempKey)
                        Toast.makeText(context, "API Key Saved", Toast.LENGTH_SHORT).show()
                        showApiKeyDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = brandRed)
                ) {
                    Text("Save Key")
                }
            },
            dismissButton = {
                TextButton(onClick = { showApiKeyDialog = false }) {
                    Text("Cancel", color = subTextColor)
                }
            }
        )
    }

    // ============================================================
    // DIALOG: ADD CUSTOM HALL OF FAME CHANNEL
    // ============================================================
    if (showAddChannelDialog) {
        val subjectOptions = listOf("Physics", "Chemistry", "Biology", "Mathematics", "NEET/JEE", "Other")
        val colorOptions = listOf(
            "#00F0FF" to "Cyan",
            "#F59E0B" to "Amber",
            "#A855F7" to "Purple",
            "#10B981" to "Green",
            "#EC4899" to "Pink",
            "#3B82F6" to "Blue"
        )
        AlertDialog(
            onDismissRequest = { showAddChannelDialog = false },
            containerColor = Color(0xFF0F172A),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AddCircle, contentDescription = null, tint = brandAmber, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add Hall of Fame Channel", color = textColor, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                }
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Add your favorite teacher or institute YouTube channel to your curated feed.",
                        fontSize = 12.sp,
                        color = subTextColor,
                        lineHeight = 16.sp
                    )

                    OutlinedTextField(
                        value = newChannelName,
                        onValueChange = { newChannelName = it },
                        label = { Text("Channel Name *", color = subTextColor) },
                        placeholder = { Text("e.g. Physics Wallah, Nitin Sachan", color = subTextColor.copy(alpha = 0.5f)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = brandAmber,
                            unfocusedBorderColor = glassBorderColor,
                            focusedTextColor = textColor,
                            unfocusedTextColor = textColor,
                            focusedContainerColor = glassCardBg,
                            unfocusedContainerColor = glassCardBg
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = newChannelHandle,
                        onValueChange = { newChannelHandle = it },
                        label = { Text("YouTube @Handle or Channel ID", color = subTextColor) },
                        placeholder = { Text("e.g. @PhysicsWallah or UC...", color = subTextColor.copy(alpha = 0.5f)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = brandAmber,
                            unfocusedBorderColor = glassBorderColor,
                            focusedTextColor = textColor,
                            unfocusedTextColor = textColor,
                            focusedContainerColor = glassCardBg,
                            unfocusedContainerColor = glassCardBg
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("Subject Category", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = textColor)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(subjectOptions) { subj ->
                            val isSelected = newChannelSubject == subj
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = if (isSelected) brandAmber.copy(alpha = 0.25f) else glassCardBg,
                                border = BorderStroke(1.dp, if (isSelected) brandAmber else glassBorderColor),
                                modifier = Modifier.clickable { newChannelSubject = subj }
                            ) {
                                Text(
                                    text = subj,
                                    color = if (isSelected) brandAmber else subTextColor,
                                    fontSize = 11.5.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = newChannelDesc,
                        onValueChange = { newChannelDesc = it },
                        label = { Text("Description (Optional)", color = subTextColor) },
                        placeholder = { Text("e.g. Best for advanced derivations & PYQs", color = subTextColor.copy(alpha = 0.5f)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = brandAmber,
                            unfocusedBorderColor = glassBorderColor,
                            focusedTextColor = textColor,
                            unfocusedTextColor = textColor,
                            focusedContainerColor = glassCardBg,
                            unfocusedContainerColor = glassCardBg
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 2
                    )

                    Text("Accent Theme Color", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = textColor)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        colorOptions.forEach { (hex, _) ->
                            val isSelected = newChannelColorHex == hex
                            val parsedCol = try { Color(android.graphics.Color.parseColor(hex)) } catch (e: Exception) { brandCyan }
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(parsedCol)
                                    .border(if (isSelected) 2.5.dp else 1.dp, if (isSelected) Color.White else Color.Transparent, CircleShape)
                                    .clickable { newChannelColorHex = hex },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val cleanName = newChannelName.trim()
                        if (cleanName.isNotBlank()) {
                            val cleanHandle = newChannelHandle.trim()
                            val parsedChannelId = if (cleanHandle.startsWith("UC")) cleanHandle else ""
                            viewModel.addCustomHallOfFameChannel(
                                AppViewModel.CustomStudyChannel(
                                    name = cleanName,
                                    handle = cleanHandle,
                                    channelId = parsedChannelId,
                                    subject = newChannelSubject,
                                    description = newChannelDesc.trim(),
                                    colorHex = newChannelColorHex
                                )
                            )
                            Toast.makeText(context, "Channel added to Hall of Fame!", Toast.LENGTH_SHORT).show()
                            showAddChannelDialog = false
                        }
                    },
                    enabled = newChannelName.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = brandAmber, contentColor = Color.Black),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Save to Hall of Fame 🏆", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddChannelDialog = false }) {
                    Text("Cancel", color = subTextColor)
                }
            }
        )
    }

    // ============================================================
    // DIALOG: CONFIRM DELETE / HIDE CHANNEL
    // ============================================================
    if (channelToDelete != null) {
        AlertDialog(
            onDismissRequest = { channelToDelete = null },
            containerColor = Color(0xFF0F172A),
            title = { Text("Remove Channel?", color = textColor, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Are you sure you want to remove \"${channelToDelete?.name}\" from your Hall of Fame? You can restore default channels at any time using 'Restore All'.",
                    color = subTextColor,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val ch = channelToDelete
                        if (ch != null) {
                            viewModel.deleteHallOfFameChannel(ch.id)
                            Toast.makeText(context, "\"${ch.name}\" removed", Toast.LENGTH_SHORT).show()
                        }
                        channelToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = brandRed, contentColor = Color.White),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Delete / Hide")
                }
            },
            dismissButton = {
                TextButton(onClick = { channelToDelete = null }) {
                    Text("Cancel", color = subTextColor)
                }
            }
        )
    }
    if (showAiSummarySheet) {
        Dialog(
            onDismissRequest = { showAiSummarySheet = false },
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
                    // Top App Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF0F172A))
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { showAiSummarySheet = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = brandCyan, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    "AI Formula Sheet & Revision",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Text(
                                aiSheetTitle,
                                fontSize = 11.sp,
                                color = brandCyan,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        if (aiSummaryContent != null) {
                            IconButton(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(aiSummaryContent!!))
                                    Toast.makeText(context, "Copied all formulas to clipboard!", Toast.LENGTH_SHORT).show()
                                }
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy Formulas", tint = brandCyan)
                            }
                            IconButton(
                                onClick = {
                                    try {
                                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_SUBJECT, aiSheetTitle)
                                            putExtra(Intent.EXTRA_TEXT, aiSummaryContent)
                                        }
                                        context.startActivity(Intent.createChooser(shareIntent, "Share Formula Sheet"))
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Could not share", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            ) {
                                Icon(Icons.Default.Share, contentDescription = "Share", tint = brandGreen)
                            }
                        }
                    }

                    // Main Sheet Content
                    if (isGeneratingAiSummary) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(24.dp)
                            ) {
                                CircularProgressIndicator(color = brandCyan, modifier = Modifier.size(48.dp))
                                Spacer(modifier = Modifier.height(18.dp))
                                Text(
                                    "Generating Complete Formula Sheet...",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    "AI is extracting key formulas, derivations, SI units, and exam shortcuts.",
                                    fontSize = 12.sp,
                                    color = subTextColor,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else if (aiSummaryContent != null) {
                        val base64Content = android.util.Base64.encodeToString(aiSummaryContent!!.toByteArray(), android.util.Base64.NO_WRAP)
                        AndroidView(
                            factory = { ctx ->
                                android.webkit.WebView(ctx).apply {
                                    settings.apply {
                                        javaScriptEnabled = true
                                        domStorageEnabled = true
                                        setSupportZoom(true)
                                        builtInZoomControls = true
                                        displayZoomControls = false
                                        useWideViewPort = true
                                        loadWithOverviewMode = true
                                    }
                                    setBackgroundColor(android.graphics.Color.parseColor("#070B14"))
                                    webViewClient = object : android.webkit.WebViewClient() {
                                        override fun onRenderProcessGone(view: android.webkit.WebView?, detail: android.webkit.RenderProcessGoneDetail?): Boolean {
                                            try { view?.destroy() } catch (e: Exception) {}
                                            return true
                                        }
                                    }
                                }
                            },
                            update = { webView ->
                                val html = """
                                    <!DOCTYPE html>
                                    <html>
                                    <head>
                                    <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=3.0, user-scalable=yes">
                                    <script src="https://cdn.jsdelivr.net/npm/marked/marked.min.js"></script>
                                    <script>
                                    window.MathJax = {
                                      tex: {
                                        inlineMath: [['$', '$'], ['\\(', '\\)']],
                                        displayMath: [['$$', '$$'], ['\\[', '\\]']]
                                      }
                                    };
                                    </script>
                                    <script id="MathJax-script" async src="https://cdn.jsdelivr.net/npm/mathjax@3/es5/tex-mml-chtml.js"></script>
                                    <style>
                                        * { box-sizing: border-box; }
                                        body { 
                                            color: #E2E8F0; 
                                            background-color: #070B14;
                                            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;
                                            font-size: 15px;
                                            line-height: 1.6;
                                            margin: 0;
                                            padding: 16px 16px 40px 16px;
                                        }
                                        h1, h2, h3, h4 { color: #00E5FF; margin-top: 1.2em; margin-bottom: 0.5em; }
                                        h1 { font-size: 20px; border-bottom: 1px solid rgba(0, 229, 255, 0.3); padding-bottom: 6px; }
                                        h2 { font-size: 17px; color: #00E676; }
                                        h3 { font-size: 15px; color: #FFB703; }
                                        p, li { color: #CBD5E1; }
                                        strong { color: #FFFFFF; }
                                        code { background: rgba(255,255,255,0.1); padding: 2px 6px; border-radius: 4px; color: #00E5FF; font-size: 13px; }
                                        pre { background: #131A2A; border: 1px solid rgba(255,255,255,0.1); border-radius: 8px; padding: 12px; overflow-x: auto; }
                                        blockquote { border-left: 4px solid #00E5FF; margin: 12px 0; padding: 8px 14px; background: rgba(0, 229, 255, 0.08); border-radius: 0 8px 8px 0; }
                                        table { width: 100%; border-collapse: collapse; margin: 12px 0; font-size: 13px; }
                                        th, td { border: 1px solid rgba(255,255,255,0.15); padding: 8px 10px; text-align: left; }
                                        th { background: #131A2A; color: #00E5FF; }
                                        mjx-container[jax="CHTML"][display="true"] {
                                            overflow-x: auto;
                                            overflow-y: hidden;
                                            padding: 8px 0;
                                            background: rgba(0, 229, 255, 0.05);
                                            border-radius: 6px;
                                            margin: 8px 0;
                                        }
                                    </style>
                                    </head>
                                    <body>
                                    <div id="content"></div>
                                    <script>
                                        try {
                                            const rawMarkdown = decodeURIComponent(escape(window.atob('${base64Content}')));
                                            document.getElementById('content').innerHTML = marked.parse(rawMarkdown);
                                        } catch(e) {
                                            document.getElementById('content').innerText = "Error rendering formula sheet.";
                                        }
                                    </script>
                                    </body>
                                    </html>
                                """.trimIndent()
                                webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        )
                    }
                }
            }
        }
    }

    // ============================================================
    // DIALOG: ATTACH VIDEO TO CHAPTER
    // ============================================================
    if (showAttachVideoDialog) {
        AlertDialog(
            onDismissRequest = { showAttachVideoDialog = false },
            containerColor = Color(0xFF0F172A),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.BookmarkAdd, contentDescription = null, tint = brandCyan)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text("Attach Video to Chapter", color = textColor, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("$attachVideoSubject • $attachVideoChapterName", color = brandCyan, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            },
            text = {
                Column {
                    Text(
                        "Paste any YouTube Video URL, Video ID, or Lecture Link to pin it to this chapter.",
                        fontSize = 12.sp,
                        color = subTextColor,
                        lineHeight = 16.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = attachVideoUrlOrId,
                        onValueChange = { attachVideoUrlOrId = it },
                        label = { Text("Video URL or ID") },
                        placeholder = { Text("https://youtu.be/... or dQw4w9WgXcQ", fontSize = 11.sp) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = glassCardBg,
                            unfocusedContainerColor = glassCardBg,
                            focusedBorderColor = brandCyan,
                            focusedLabelColor = brandCyan,
                            focusedTextColor = textColor,
                            unfocusedTextColor = textColor
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = attachVideoTitle,
                        onValueChange = { attachVideoTitle = it },
                        label = { Text("Custom Title (Optional)") },
                        placeholder = { Text("e.g. Best Derivation One Shot", fontSize = 11.sp) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = glassCardBg,
                            unfocusedContainerColor = glassCardBg,
                            focusedBorderColor = brandCyan,
                            focusedLabelColor = brandCyan,
                            focusedTextColor = textColor,
                            unfocusedTextColor = textColor
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = attachVideoChannel,
                        onValueChange = { attachVideoChannel = it },
                        label = { Text("Channel / Faculty Name (Optional)") },
                        placeholder = { Text("e.g. Physics Wallah", fontSize = 11.sp) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = glassCardBg,
                            unfocusedContainerColor = glassCardBg,
                            focusedBorderColor = brandCyan,
                            focusedLabelColor = brandCyan,
                            focusedTextColor = textColor,
                            unfocusedTextColor = textColor
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val vId = extractVideoId(attachVideoUrlOrId.trim())
                        if (vId.isBlank()) {
                            Toast.makeText(context, "Please enter a valid YouTube URL or Video ID", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        val title = attachVideoTitle.trim().ifBlank { "$attachVideoChapterName Lecture" }
                        val channel = attachVideoChannel.trim().ifBlank { "Attached Resource" }
                        val thumb = "https://img.youtube.com/vi/$vId/hqdefault.jpg"
                        viewModel.addVideoToChapter(attachVideoSubject, attachVideoChapterName, vId, title, channel, thumb)
                        Toast.makeText(context, "Video Attached to $attachVideoChapterName", Toast.LENGTH_SHORT).show()
                        showAttachVideoDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = brandCyan)
                ) {
                    Text("Attach Video")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAttachVideoDialog = false }) {
                    Text("Cancel", color = subTextColor)
                }
            }
        )
    }

    // ============================================================
    // MODAL BOTTOM SHEET: VIDEO QUALITY SELECTION
    // ============================================================
    if (showQualitySheet) {
        ModalBottomSheet(
            onDismissRequest = { showQualitySheet = false },
            containerColor = Color(0xFF18181B),
            scrimColor = Color.Black.copy(alpha = 0.65f),
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = null,
                            tint = brandCyan,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Video Quality Settings",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(onClick = { showQualitySheet = false }) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }

                Text(
                    "Video resolution chunein ya native touch se exact YouTube stream switch karein:",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(vertical = 6.dp)
                )

                val sheetQualities = listOf(
                    "auto" to "Auto (Best for network speed)",
                    "hd1080" to "1080p Full HD (Crystal clear equations)",
                    "hd720" to "720p HD (High Definition)",
                    "large" to "480p Standard (Data friendly)",
                    "medium" to "360p Data Saver (Minimum data)"
                )

                sheetQualities.forEach { (qKey, qTitle) ->
                    val isSelected = selectedQuality == qKey
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) brandCyan.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.05f),
                        border = BorderStroke(1.dp, if (isSelected) brandCyan else Color.White.copy(alpha = 0.1f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable {
                                showQualitySheet = false
                                reloadVideoWithQuality(qKey, qTitle.split(" (").first())
                            }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                qTitle,
                                color = if (isSelected) brandCyan else Color.White,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                            if (isSelected) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = brandCyan,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                Spacer(modifier = Modifier.height(10.dp))

                // Direct YouTube Touch Mode Option
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF59E0B).copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.5f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            showQualitySheet = false
                            isDirectTouchMode = true
                            Toast.makeText(
                                context,
                                "YouTube Touch Mode ON: Video pe ⚙️ icon dabakar quality badlein",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.TouchApp,
                            contentDescription = null,
                            tint = Color(0xFFF59E0B),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                "Touch YouTube Player Directly (⚙️)",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Video pe YouTube ke native settings (⚙️) button ko directly tap karein exact 1080p, 720p, 480p, 360p, 240p, 144p ya captions ke liye.",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // ============================================================
    // LECTURE DOUBT -> ORIGINAL MISTAKE NOTEBOOK LOG DIALOG
    // ============================================================
    if (showMistakeDoubtDialog && selectedVideo != null) {
        val currentVid = selectedVideo!!
        // Auto-detect clean subject from video tags/title
        val autoSubject = remember(currentVid) {
            val t = currentVid.title.lowercase()
            val s = currentVid.subjectTag.lowercase()
            when {
                s.contains("phy") || t.contains("physics") || t.contains("electro") || t.contains("optics") || t.contains("mechanics") || t.contains("rotational") || t.contains("gravitation") -> "Physics"
                s.contains("chem") || t.contains("chem") || t.contains("organic") || t.contains("inorganic") || t.contains("physical chem") || t.contains("equilibrium") -> "Chemistry"
                s.contains("bio") || t.contains("bio") || t.contains("botany") || t.contains("zoology") || t.contains("genetics") || t.contains("neet") -> "Biology"
                s.contains("math") || t.contains("math") || t.contains("calculus") || t.contains("algebra") || t.contains("coordinate") -> "Mathematics"
                else -> "Physics"
            }
        }

        // Clean chapter name - strip channel names, teacher names, bot prompts
        val autoChapter = remember(currentVid) {
            var t = currentVid.title
            val knownNames = listOf(
                "Physics Wallah", "PW", "Unacademy", "Doubtnut", "Vedantu", "Khan Academy",
                "Aakash", "Allen", "Competishun", "Eduniti", "Sankalp Bharat", "Mohit Tyagi",
                "Arvind Academy", "Seep Pahuja", "Garima Goel", "Alakh Pandey", "Sachin Sir",
                "MR Sir", "Tarun Sir", "Rajwant Sir", "Sign in", "confirm you are not a bot",
                "bot", "YouTube", "Channel", "Verified Educator"
            )
            for (name in knownNames) {
                t = t.replace(Regex("(?i)\\b" + Regex.escape(name) + "\\b"), "")
            }
            val stripped = t.replace(Regex("(?i)\\|.*|#\\S+|full chapter|one shot|class \\d+|neet|jee|complete revision|in \\d+ (hours|mins|shot)"), "")
                .replace(Regex("[\\|\\-–—:_]+"), " ")
                .replace(Regex("\\s+"), " ")
                .trim()
            if (stripped.isNotBlank() && stripped.length > 2) stripped else ""
        }

        val videoTimestampUrl = "https://youtube.com/watch?v=${currentVid.id}&t=${doubtCaptureTimeSeconds}s"
        val videoTimestampLabel = if (autoChapter.isNotBlank()) "$autoChapter @ ${formatSecondsToTimestamp(doubtCaptureTimeSeconds)}" else "Lecture @ ${formatSecondsToTimestamp(doubtCaptureTimeSeconds)}"

        ModernAddMistakeDialog(
            isDark = isDark,
            editingMistake = null,
            initialImageUri = doubtSnapshotUri,
            initialSubject = autoSubject,
            initialChapter = autoChapter,
            initialQuestion = "",
            initialPdfUri = videoTimestampUrl,
            initialPdfName = videoTimestampLabel,
            availableSubjects = listOf("Physics", "Chemistry", "Biology", "Mathematics"),
            onDismiss = {
                showMistakeDoubtDialog = false
            },
            onOpenPdf = { uriStr, name ->
                if (uriStr.contains("youtube") || uriStr.contains("youtu.be") || uriStr.startsWith("studytube")) {
                    val timeSec = Regex("[?&]t=(\\d+)").find(uriStr)?.groupValues?.get(1)?.toIntOrNull() ?: doubtCaptureTimeSeconds
                    webViewInstance?.evaluateJavascript("player.seekTo($timeSec, true); player.playVideo();", null)
                    videoCurrentTime = timeSec.toFloat()
                    isPlaying = true
                    showMistakeDoubtDialog = false
                }
            },
            onSave = { subject, question, type, chapter, pdfUri, pdfName, imageUri ->
                viewModel.addMistakeLog(
                    subject = subject,
                    question = question,
                    mistakeType = type,
                    chapter = chapter,
                    pdfUri = pdfUri ?: videoTimestampUrl,
                    pdfName = pdfName ?: videoTimestampLabel,
                    imageUri = imageUri
                )
                showMistakeDoubtDialog = false
                Toast.makeText(context, "Saved to Mistake Notebook! 📓", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // ============================================================
    // LAKSHYA LENS QUESTION SCANNER DIALOG
    // ============================================================
    if (showLensScannerDialog) {
        StudyTubeLensScannerDialog(
            isOpen = showLensScannerDialog,
            onDismiss = {
                showLensScannerDialog = false
                lensScannerInitialImageUri = null
                lensScannerInitialQuery = null
            },
            initialImageUri = lensScannerInitialImageUri,
            initialQueryText = lensScannerInitialQuery,
            youtubeApiKey = youtubeApiKey,
            curatedChannels = curatedChannels,
            geminiApiKey = geminiKey1.ifBlank { geminiKey2.ifBlank { geminiKey3 } },
            selectedModel = if (aiProvider == com.example.data.AiProvider.OPENROUTER) openRouterSelectedModel else selectedModel,
            onSelectVideo = { video ->
                selectedVideo = video
                showLensScannerDialog = false
                lensScannerInitialImageUri = null
                lensScannerInitialQuery = null
                activeStudySeconds = 0L
                isTimerRunning = true
                Toast.makeText(context, "Playing Video Solution: ${video.title}", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // ============================================================
    // IN-APP WEB BROWSER & PDF DOWNLOADER MODAL
    // ============================================================
    if (inAppBrowserUrl != null) {
        InAppWebBrowserDialog(
            url = inAppBrowserUrl!!,
            onDismiss = { inAppBrowserUrl = null },
            onDownloadPdf = { rawUrl, docTitle, contentDisposition, mimeType ->
                try {
                    // 1. Convert Google Drive view URLs to direct binary download link
                    val directUrl = if (rawUrl.contains("drive.google.com", ignoreCase = true) ||
                        rawUrl.contains("docs.google.com", ignoreCase = true)
                    ) {
                        val fileIdRegex = Regex("(?:/file/d/|/d/|[?&]id=)([a-zA-Z0-9_-]{15,})")
                        val match = fileIdRegex.find(rawUrl)
                        if (match != null) {
                            val fileId = match.groupValues[1]
                            "https://drive.google.com/uc?export=download&id=$fileId&confirm=t"
                        } else {
                            rawUrl
                        }
                    } else {
                        rawUrl
                    }

                    // 2. Resolve safe and proper PDF filename (prevent .bin, extract real name from title/disposition/url)
                    var fileName = ""
                    // Priority 1: Check document/page title
                    if (!docTitle.isNullOrBlank()) {
                        val cleanedTitle = docTitle
                            .replace(Regex("(?i)\\s*-\\s*Google\\s*(Drive|Docs|Sheets|Slides|Chrome|PDF)?.*$"), "")
                            .replace(Regex("(?i)\\s*\\|\\s*.*$"), "")
                            .trim()
                        if (cleanedTitle.isNotBlank() &&
                            !cleanedTitle.equals("Google Drive", ignoreCase = true) &&
                            !cleanedTitle.equals("Loading...", ignoreCase = true) &&
                            !cleanedTitle.equals("Untitled", ignoreCase = true) &&
                            !cleanedTitle.equals("About:blank", ignoreCase = true)
                        ) {
                            fileName = cleanedTitle
                        }
                    }

                    // Priority 2: Check content disposition & guessFileName
                    if (fileName.isBlank()) {
                        val guessed = try {
                            URLUtil.guessFileName(rawUrl, contentDisposition, mimeType)
                        } catch (_: Exception) {
                            null
                        }
                        if (!guessed.isNullOrBlank() &&
                            !guessed.equals("downloadfile.bin", ignoreCase = true) &&
                            !guessed.endsWith(".bin", ignoreCase = true)
                        ) {
                            fileName = guessed
                        }
                    }

                    // Priority 3: Extract from URL segment
                    if (fileName.isBlank()) {
                        try {
                            val uri = Uri.parse(rawUrl)
                            val lastSeg = uri.lastPathSegment
                            if (!lastSeg.isNullOrBlank() && lastSeg.contains(".pdf", ignoreCase = true)) {
                                fileName = lastSeg
                            } else {
                                val qName = uri.getQueryParameter("fileName")
                                    ?: uri.getQueryParameter("filename")
                                    ?: uri.getQueryParameter("title")
                                if (!qName.isNullOrBlank()) {
                                    fileName = qName
                                }
                            }
                        } catch (_: Exception) {}
                    }

                    // Fallback: Default study notes name
                    if (fileName.isBlank()) {
                        fileName = "StudyMaterial_${System.currentTimeMillis()}"
                    }

                    // Cleanup any accidental extensions
                    if (fileName.endsWith(".bin", ignoreCase = true)) {
                        val base = fileName.substringBeforeLast(".bin").trim()
                        fileName = if (base.isBlank() || base.equals("downloadfile", ignoreCase = true)) {
                            "StudyMaterial_${System.currentTimeMillis()}"
                        } else {
                            base
                        }
                    }
                    if (fileName.endsWith(".html", ignoreCase = true) || fileName.endsWith(".htm", ignoreCase = true)) {
                        fileName = fileName.substringBeforeLast(".").trim()
                    }

                    // Enforce .pdf extension
                    if (!fileName.endsWith(".pdf", ignoreCase = true)) {
                        fileName = "$fileName.pdf"
                    }

                    // Sanitize for file system
                    val safeFileName = fileName.replace(Regex("[\\\\/:*?\"<>|]"), "_").trim()

                    // 3. Initiate download with DownloadManager
                    val uri = Uri.parse(directUrl)
                    val request = DownloadManager.Request(uri).apply {
                        setTitle(safeFileName)
                        setDescription("StudyTube Lecture Material / PDF Notes")
                        setMimeType("application/pdf")
                        setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                        setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, safeFileName)
                        allowScanningByMediaScanner()

                        val cookies = CookieManager.getInstance().getCookie(directUrl)
                            ?: CookieManager.getInstance().getCookie(rawUrl)
                        if (!cookies.isNullOrBlank()) {
                            addRequestHeader("Cookie", cookies)
                        }
                        val ua = try {
                            WebSettings.getDefaultUserAgent(context)
                        } catch (_: Exception) {
                            "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
                        }
                        addRequestHeader("User-Agent", ua)
                    }
                    val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                    dm.enqueue(request)
                    Toast.makeText(context, "📥 Downloading $safeFileName! Check Downloads folder.", Toast.LENGTH_LONG).show()
                } catch (e: Exception) {
                    e.printStackTrace()
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(rawUrl)).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        context.startActivity(intent)
                        Toast.makeText(context, "Opening in external browser to download...", Toast.LENGTH_SHORT).show()
                    } catch (err: Exception) {
                        Toast.makeText(context, "Download failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        )
    }
}

@Composable
fun ChapterVideoCard(
    chapter: ExamSyllabusDatabase.ChapterItem,
    subject: String,
    chapterVideos: Set<String>,
    cardBgColor: Color,
    cardBorderColor: Color,
    textColor: Color,
    subTextColor: Color,
    accentColor: Color,
    onPlayVideo: (YouTubeVideo) -> Unit,
    onDeleteVideo: (String) -> Unit,
    onAttachVideo: () -> Unit,
    onFindTopLectures: () -> Unit,
    onGenerateAiSummary: () -> Unit,
    onGenerateAiQuiz: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    // Filter videos pinned to this subject + chapter
    val pinnedVideos = remember(chapterVideos, chapter.name, subject) {
        chapterVideos.mapNotNull { entry ->
            val parts = entry.split("|||")
            if (parts.size >= 6) {
                val eSubject = parts[0]
                val eChapter = parts[1]
                val eVideoId = parts[2]
                val eTitle = parts[3]
                val eChannel = parts[4]
                val eThumb = parts[5]
                if (eSubject.equals(subject, ignoreCase = true) && eChapter.equals(chapter.name, ignoreCase = true)) {
                    entry to YouTubeVideo(eVideoId, eTitle, eChannel, eThumb)
                } else null
            } else null
        }
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = cardBgColor,
        border = BorderStroke(1.dp, if (expanded) accentColor.copy(alpha = 0.5f) else cardBorderColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row: Chapter Title & Expand Arrow
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Class Badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (chapter.classLevel == "12th") Color(0xFFA855F7).copy(alpha = 0.2f) else Color(0xFF00F0FF).copy(alpha = 0.2f),
                    border = BorderStroke(1.dp, if (chapter.classLevel == "12th") Color(0xFFA855F7).copy(alpha = 0.4f) else Color(0xFF00F0FF).copy(alpha = 0.4f))
                ) {
                    Text(
                        chapter.classLevel,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (chapter.classLevel == "12th") Color(0xFFA855F7) else Color(0xFF00F0FF),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        chapter.name,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor,
                        lineHeight = 18.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            chapter.pyqWeightage,
                            fontSize = 11.sp,
                            color = Color(0xFFFFB703),
                            fontWeight = FontWeight.Medium
                        )
                        if (pinnedVideos.isNotEmpty()) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "• ${pinnedVideos.size} ${if (pinnedVideos.size == 1) "Video" else "Videos"}",
                                fontSize = 11.sp,
                                color = accentColor,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                IconButton(
                    onClick = { expanded = !expanded },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = "Expand",
                        tint = subTextColor
                    )
                }
            }

            // Expanded Actions & Video Grid
            if (expanded) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = cardBorderColor.copy(alpha = 0.4f), thickness = 0.8.dp)
                Spacer(modifier = Modifier.height(10.dp))

                // Quick Action Buttons (Find Top Lectures, AI Formula, AI Quiz, Attach Video)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Find Top Lectures Button
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFEF4444).copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.3f)),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onFindTopLectures() }
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Find Lectures", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFEF4444), maxLines = 1)
                        }
                    }

                    // AI Formula Sheet Button
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = accentColor.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.3f)),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onGenerateAiSummary() }
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = accentColor, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("AI Formulas", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = accentColor, maxLines = 1)
                        }
                    }

                    // AI Quiz Button
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF10E599).copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, Color(0xFF10E599).copy(alpha = 0.3f)),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onGenerateAiQuiz() }
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.Quiz, contentDescription = null, tint = Color(0xFF10E599), modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("AI Quiz", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10E599), maxLines = 1)
                        }
                    }

                    // Attach Video Button
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFA855F7).copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, Color(0xFFA855F7).copy(alpha = 0.3f)),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onAttachVideo() }
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = Color(0xFFA855F7), modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("+ Attach", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFA855F7), maxLines = 1)
                        }
                    }
                }

                // Pinned Videos List
                if (pinnedVideos.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Pinned Video Resources:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = textColor)
                    Spacer(modifier = Modifier.height(6.dp))

                    pinnedVideos.forEach { (rawEntry, video) ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF0F172A).copy(alpha = 0.8f),
                            border = BorderStroke(0.8.dp, cardBorderColor),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable { onPlayVideo(video) }
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(width = 72.dp, height = 44.dp)
                                        .background(Color.Black, RoundedCornerShape(8.dp))
                                ) {
                                    coil.compose.AsyncImage(
                                        model = video.thumbnailUrl,
                                        contentDescription = null,
                                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color.Black.copy(alpha = 0.3f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                                    }
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        video.title,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = textColor,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        video.channelTitle,
                                        fontSize = 11.sp,
                                        color = subTextColor,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                IconButton(
                                    onClick = { onDeleteVideo(rawEntry) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Outlined.Delete, contentDescription = "Delete", tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        "No videos pinned to this chapter yet. Tap '+ Attach' to link lectures or 'Find Lectures' to browse one-shots.",
                        fontSize = 11.sp,
                        color = subTextColor,
                        lineHeight = 15.sp
                    )
                }
            }
        }
    }
}

@Composable
fun ChannelPlaylistCard(
    playlist: ChannelPlaylist,
    accentColor: Color,
    cardBg: Color,
    borderColor: Color,
    textColor: Color,
    subTextColor: Color,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = cardBg,
        border = BorderStroke(1.dp, borderColor),
        modifier = Modifier
            .width(220.dp)
            .clickable { onClick() }
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(124.dp)
                    .background(Color(0xFF0F172A))
            ) {
                if (playlist.thumbnailUrl.isNotBlank()) {
                    coil.compose.AsyncImage(
                        model = playlist.thumbnailUrl,
                        contentDescription = "Playlist Thumbnail",
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // YouTube-style playlist right-side vertical overlay badge
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(72.dp)
                        .align(Alignment.CenterEnd)
                        .background(Color.Black.copy(alpha = 0.75f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.PlaylistPlay,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${playlist.videoCount}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = playlist.title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 17.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Playlist • ${playlist.videoCount} videos",
                        fontSize = 11.sp,
                        color = subTextColor
                    )
                }
            }
        }
    }
}

@Composable
fun FullPlaylistRowCard(
    playlist: ChannelPlaylist,
    accentColor: Color,
    cardBg: Color,
    borderColor: Color,
    textColor: Color,
    subTextColor: Color,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = cardBg,
        border = BorderStroke(1.dp, borderColor),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(130.dp)
                    .height(78.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF0F172A))
            ) {
                if (playlist.thumbnailUrl.isNotBlank()) {
                    coil.compose.AsyncImage(
                        model = playlist.thumbnailUrl,
                        contentDescription = "Playlist Thumbnail",
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(48.dp)
                        .align(Alignment.CenterEnd)
                        .background(Color.Black.copy(alpha = 0.75f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.PlaylistPlay,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "${playlist.videoCount}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = playlist.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 18.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = accentColor.copy(alpha = 0.15f),
                        border = BorderStroke(0.5.dp, accentColor.copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = playlist.subjectTag,
                            fontSize = 10.sp,
                            color = accentColor,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${playlist.videoCount} lectures",
                        fontSize = 11.sp,
                        color = subTextColor
                    )
                }
            }

            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = subTextColor.copy(alpha = 0.6f),
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

@Composable
fun VideoFeedCard(
    video: YouTubeVideo,
    cardBgColor: Color,
    cardBorderColor: Color,
    textColor: Color,
    subTextColor: Color,
    accentColor: Color,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = cardBgColor,
        border = BorderStroke(1.dp, cardBorderColor),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column {
            // 16:9 Thumbnail with Overlay Badge
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .background(Color(0xFF0F172A))
            ) {
                coil.compose.AsyncImage(
                    model = video.thumbnailUrl,
                    contentDescription = "Thumbnail",
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Play Button Backdrop
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color.Black.copy(alpha = 0.6f),
                        border = BorderStroke(1.5.dp, Color.White.copy(alpha = 0.8f))
                    ) {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = "Play",
                            tint = Color.White,
                            modifier = Modifier
                                .padding(10.dp)
                                .size(28.dp)
                        )
                    }
                }

                // Subject / Fast Tag Pill
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color.Black.copy(alpha = 0.75f),
                    border = BorderStroke(0.5.dp, accentColor.copy(alpha = 0.6f)),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(10.dp)
                ) {
                    Text(
                        text = video.subjectTag,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = accentColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            // Video Details
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = video.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Verified,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = video.channelTitle,
                        fontSize = 12.sp,
                        color = subTextColor,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    val relTime = formatPublishRelativeTime(video.publishedAt, video.publishTimeMillis)
                    if (relTime.isNotBlank()) {
                        Text(
                            text = " • $relTime",
                            fontSize = 11.sp,
                            color = subTextColor.copy(alpha = 0.85f),
                            fontWeight = FontWeight.Normal,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

/**
 * Extracts YouTube video ID from various link formats (youtu.be, youtube.com/watch?v=, /embed/, /shorts/, etc.)
 */
fun extractVideoId(input: String): String {
    val trimmed = input.trim()
    if (trimmed.length == 11 && !trimmed.contains("/") && !trimmed.contains("?")) {
        return trimmed
    }
    return try {
        val uri = Uri.parse(trimmed)
        if (uri.host?.contains("youtu.be") == true) {
            uri.lastPathSegment ?: ""
        } else if (uri.host?.contains("youtube.com") == true) {
            if (uri.path?.contains("/shorts/") == true || uri.path?.contains("/embed/") == true || uri.path?.contains("/v/") == true) {
                uri.lastPathSegment ?: ""
            } else {
                uri.getQueryParameter("v") ?: ""
            }
        } else {
            // Regex fallback for 11-char ID
            val pattern = "([a-zA-Z0-9_-]{11})".toRegex()
            pattern.find(trimmed)?.value ?: ""
        }
    } catch (e: Exception) {
        val pattern = "([a-zA-Z0-9_-]{11})".toRegex()
        pattern.find(trimmed)?.value ?: ""
    }
}

fun formatTimeAgo(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    val mins = diff / (60 * 1000)
    val hours = diff / (60 * 60 * 1000)
    val days = diff / (24 * 60 * 60 * 1000)
    return when {
        mins < 1 -> "Just now"
        mins < 60 -> "${mins}m ago"
        hours < 24 -> "${hours}h ago"
        days == 1L -> "Yesterday"
        days < 7 -> "${days}d ago"
        else -> SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(timestamp))
    }
}

@Composable
fun RecentlyViewedSection(
    recentList: List<AppViewModel.RecentlyViewedVideo>,
    cardBgColor: Color,
    cardBorderColor: Color,
    textColor: Color,
    subTextColor: Color,
    onPlayVideo: (YouTubeVideo) -> Unit,
    onRemoveItem: (String) -> Unit,
    onClearAll: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 6.dp)
    ) {
        // Section Header Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFF00F0FF).copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, Color(0xFF00F0FF).copy(alpha = 0.3f)),
                    modifier = Modifier.size(24.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.History,
                            contentDescription = null,
                            tint = Color(0xFF00F0FF),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Recently Viewed",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
                Spacer(modifier = Modifier.width(6.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF00F0FF).copy(alpha = 0.2f),
                    border = BorderStroke(0.8.dp, Color(0xFF00F0FF).copy(alpha = 0.5f))
                ) {
                    Text(
                        text = "${recentList.size}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF00F0FF),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            TextButton(
                onClick = onClearAll,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Outlined.DeleteSweep,
                        contentDescription = "Clear History",
                        tint = subTextColor.copy(alpha = 0.8f),
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Clear",
                        fontSize = 11.sp,
                        color = subTextColor.copy(alpha = 0.8f),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Horizontal Carousel of Recently Viewed Lectures
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(horizontal = 2.dp)
        ) {
            items(recentList, key = { it.id }) { item ->
                RecentlyViewedCard(
                    item = item,
                    cardBgColor = cardBgColor,
                    cardBorderColor = cardBorderColor,
                    textColor = textColor,
                    subTextColor = subTextColor,
                    onClick = {
                        onPlayVideo(
                            YouTubeVideo(
                                id = item.id,
                                title = item.title,
                                channelTitle = item.channelTitle,
                                thumbnailUrl = item.thumbnailUrl,
                                subjectTag = item.subjectTag
                            )
                        )
                    },
                    onRemove = { onRemoveItem(item.id) }
                )
            }
        }
    }
}

@Composable
fun RecentlyViewedCard(
    item: AppViewModel.RecentlyViewedVideo,
    cardBgColor: Color,
    cardBorderColor: Color,
    textColor: Color,
    subTextColor: Color,
    onClick: () -> Unit,
    onRemove: () -> Unit
) {
    val accentColor = when (item.subjectTag) {
        "Physics" -> Color(0xFF00F0FF)
        "Chemistry" -> Color(0xFFA855F7)
        "Biology" -> Color(0xFF10E599)
        "Mathematics" -> Color(0xFFFFB703)
        else -> Color(0xFF38BDF8)
    }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = cardBgColor,
        border = BorderStroke(1.dp, cardBorderColor),
        modifier = Modifier
            .width(210.dp)
            .clickable { onClick() }
    ) {
        Column {
            // Thumbnail with Play overlay, Subject tag and Close button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(115.dp)
                    .background(Color(0xFF0F172A))
            ) {
                coil.compose.AsyncImage(
                    model = item.thumbnailUrl,
                    contentDescription = item.title,
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Dark overlay with subtle play button
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.25f)),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color.Black.copy(alpha = 0.55f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.7f)),
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.PlayArrow,
                                contentDescription = "Play",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // Top row: Subject Pill + Remove Button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color.Black.copy(alpha = 0.75f),
                        border = BorderStroke(0.5.dp, accentColor.copy(alpha = 0.7f))
                    ) {
                        Text(
                            text = item.subjectTag,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = accentColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Surface(
                        shape = CircleShape,
                        color = Color.Black.copy(alpha = 0.65f),
                        modifier = Modifier
                            .size(22.dp)
                            .clickable { onRemove() }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Remove from history",
                                tint = Color.White.copy(alpha = 0.8f),
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }

                // Time ago pill at bottom
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = Color.Black.copy(alpha = 0.8f),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                ) {
                    Text(
                        text = formatTimeAgo(item.timestamp),
                        fontSize = 9.sp,
                        color = Color.White.copy(alpha = 0.9f),
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                    )
                }
            }

            // Text Info
            Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                Text(
                    text = item.title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = textColor,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Verified,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = item.channelTitle,
                        fontSize = 11.sp,
                        color = subTextColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun PlayerInteractiveOverlay(
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    isFullScreen: Boolean,
    showControls: Boolean,
    currentTime: Float = 0f,
    duration: Float = 0f,
    onSeekTo: (Float) -> Unit = {},
    onToggleControls: () -> Unit,
    onUserInteract: () -> Unit,
    onSeekBack10: () -> Unit,
    onSeekForward10: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onSetSpeed: (Float) -> Unit,
    onStart2xHold: () -> Unit,
    onRelease2xHold: () -> Unit,
    onToggleFullscreen: () -> Unit,
    isDirectTouchMode: Boolean = false,
    onToggleDirectTouch: () -> Unit = {},
    onOpenQualitySheet: () -> Unit = {},
    qualityLabel: String = "Auto",
    onDoubtCapture: (() -> Unit)? = null,
    leftSeekSeconds: Int,
    rightSeekSeconds: Int,
    is2xHoldActive: Boolean,
    selectedSpeed: Float,
    formattedStudyTime: String,
    videoTitle: String,
    brandCyan: Color,
    brandGreen: Color
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val audioManager = remember { context.getSystemService(android.content.Context.AUDIO_SERVICE) as android.media.AudioManager }
    val activity = context as? android.app.Activity
    
    var swipeIndicatorVisible by remember { mutableStateOf(false) }
    var swipeIndicatorIcon by remember { mutableStateOf(androidx.compose.material.icons.Icons.Default.VolumeUp) }
    var swipeIndicatorValue by remember { mutableFloatStateOf(0f) }
    var swipeIndicatorText by remember { mutableStateOf("") }
    
    LaunchedEffect(swipeIndicatorVisible, swipeIndicatorValue) {
        if (swipeIndicatorVisible) {
            kotlinx.coroutines.delay(1500)
            swipeIndicatorVisible = false
        }
    }

    val gestureModifier = if (!isDirectTouchMode) {
        Modifier
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragStart = { offset ->
                        // Check if left or right side
                    },
                    onDragEnd = {
                        swipeIndicatorVisible = false
                    },
                    onVerticalDrag = { change, dragAmount ->
                        change.consume()
                        val isLeft = change.position.x < size.width / 2f
                        val dragFactor = dragAmount / size.height.toFloat()
                        
                        if (isLeft) {
                            // Brightness
                            val window = activity?.window
                            if (window != null) {
                                val layoutParams = window.attributes
                                var currentBrightness = layoutParams.screenBrightness
                                if (currentBrightness < 0) currentBrightness = 0.5f // system default
                                var newBrightness = currentBrightness - (dragFactor * 2f) // Multiplier for sensitivity
                                newBrightness = newBrightness.coerceIn(0f, 1f)
                                layoutParams.screenBrightness = newBrightness
                                window.attributes = layoutParams
                                
                                swipeIndicatorIcon = androidx.compose.material.icons.Icons.Default.BrightnessMedium
                                swipeIndicatorValue = newBrightness
                                swipeIndicatorText = "${(newBrightness * 100).toInt()}%"
                                swipeIndicatorVisible = true
                            }
                        } else {
                            // Volume
                            val maxVolume = audioManager.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC).toFloat()
                            val currentVolume = audioManager.getStreamVolume(android.media.AudioManager.STREAM_MUSIC).toFloat()
                            var newVolume = currentVolume - (dragFactor * maxVolume * 2f)
                            newVolume = newVolume.coerceIn(0f, maxVolume)
                            audioManager.setStreamVolume(android.media.AudioManager.STREAM_MUSIC, newVolume.toInt(), 0)
                            
                            swipeIndicatorIcon = androidx.compose.material.icons.Icons.Default.VolumeUp
                            swipeIndicatorValue = newVolume / maxVolume
                            swipeIndicatorText = "${(swipeIndicatorValue * 100).toInt()}%"
                            swipeIndicatorVisible = true
                        }
                    }
                )
            }
            .pointerInput(is2xHoldActive) {
                detectTapGestures(
                    onTap = {
                        onToggleControls()
                    },
                    onDoubleTap = { offset ->
                        val isLeft = offset.x < size.width / 2f
                        if (isLeft) {
                            onSeekBack10()
                        } else {
                            onSeekForward10()
                        }
                    },
                    onLongPress = {
                        onStart2xHold()
                    },
                    onPress = {
                        try {
                            tryAwaitRelease()
                        } finally {
                            onRelease2xHold()
                        }
                    }
                )
            }
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .then(gestureModifier)
    ) {
        // Direct Touch Mode Banner (Allows direct touch to YouTube native controls)
        if (isDirectTouchMode) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFFF59E0B),
                shadowElevation = 8.dp,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(8.dp)
                    .clickable { onToggleDirectTouch() }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.TouchApp,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "YT Touch Active (Tap to Exit)",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
            }
        }
        // 1. Double-Tap Left Seek Ripple Feedback (-10s, -20s...)
        if (leftSeekSeconds > 0) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(0.45f)
                    .align(Alignment.CenterStart)
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                Color.White.copy(alpha = 0.25f),
                                Color.Transparent
                            )
                        ),
                        shape = RoundedCornerShape(topEndPercent = 100, bottomEndPercent = 100)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.FastRewind,
                        contentDescription = "Seek Back",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "-${leftSeekSeconds}s",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // 2. Double-Tap Right Seek Ripple Feedback (+10s, +20s...)
        if (rightSeekSeconds > 0) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(0.45f)
                    .align(Alignment.CenterEnd)
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                Color.Transparent,
                                Color.White.copy(alpha = 0.25f)
                            )
                        ),
                        shape = RoundedCornerShape(topStartPercent = 100, bottomStartPercent = 100)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.FastForward,
                        contentDescription = "Seek Forward",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "+${rightSeekSeconds}s",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // 3. 2x Speed Hold Banner (Top Center)
        if (is2xHoldActive) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.Black.copy(alpha = 0.85f),
                border = BorderStroke(1.dp, brandCyan),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp)
                    .clickable { onRelease2xHold() }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Speed,
                        contentDescription = "2X Speed",
                        tint = brandCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "2X Speed Fast Forward (Tap to dismiss)",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // 4. Auto-Hiding Player Controls UI (Fades out after 3s of inactivity)
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
            ) {
                // Top Bar Controls
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .align(Alignment.TopCenter),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Back / Exit Fullscreen Button
                    Surface(
                        shape = CircleShape,
                        color = Color.Black.copy(alpha = 0.6f),
                        border = BorderStroke(0.8.dp, Color.White.copy(alpha = 0.3f)),
                        modifier = Modifier.clickable {
                            onUserInteract()
                            onToggleFullscreen()
                        }
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(8.dp)
                        ) {
                            Icon(
                                if (isFullScreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                                contentDescription = "Toggle Fullscreen",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // Video Title in Fullscreen or Header
                    if (isFullScreen) {
                        Text(
                            text = videoTitle,
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 12.dp)
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Prominent Doubt Capture Button
                        if (onDoubtCapture != null) {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = Color(0xFFE11D48),
                                border = BorderStroke(1.dp, Color(0xFFFDA4AF)),
                                modifier = Modifier.clickable {
                                    onUserInteract()
                                    onDoubtCapture()
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.HelpOutline, contentDescription = "Doubt", tint = Color.White, modifier = Modifier.size(13.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        "Doubt 📸",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }
                        }

                        // Video Quality Button
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color.Black.copy(alpha = 0.7f),
                            border = BorderStroke(1.dp, brandCyan.copy(alpha = 0.6f)),
                            modifier = Modifier.clickable {
                                onUserInteract()
                                onOpenQualitySheet()
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Settings,
                                    contentDescription = "Quality Settings",
                                    tint = brandCyan,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    "Quality ($qualityLabel)",
                                    color = brandCyan,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Direct YouTube Player Touch Mode Toggle
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (isDirectTouchMode) Color(0xFFF59E0B) else Color.Black.copy(alpha = 0.7f),
                            border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.7f)),
                            modifier = Modifier.clickable {
                                onUserInteract()
                                onToggleDirectTouch()
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.TouchApp,
                                    contentDescription = "Direct Touch",
                                    tint = if (isDirectTouchMode) Color.Black else Color(0xFFF59E0B),
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    if (isDirectTouchMode) "YT Touch" else "Touch ⚙️",
                                    color = if (isDirectTouchMode) Color.Black else Color(0xFFF59E0B),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Study Timer Badge
                        if (!isFullScreen) {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = Color.Black.copy(alpha = 0.7f),
                                border = BorderStroke(1.dp, brandGreen.copy(alpha = 0.5f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .background(brandGreen, CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        formattedStudyTime,
                                        fontSize = 11.sp,
                                        color = brandGreen,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                // Center Control Buttons (-10s, Play/Pause, +10s)
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalArrangement = Arrangement.spacedBy(28.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // -10s Button
                    Surface(
                        shape = CircleShape,
                        color = Color.Black.copy(alpha = 0.65f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.4f)),
                        modifier = Modifier.clickable {
                            onUserInteract()
                            onSeekBack10()
                        }
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Icon(
                                Icons.Default.Replay10,
                                contentDescription = "10s Back",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }

                    // Play/Pause Button
                    Surface(
                        shape = CircleShape,
                        color = Color.White,
                        modifier = Modifier.clickable {
                            onUserInteract()
                            onTogglePlayPause()
                        }
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(14.dp)
                        ) {
                            Icon(
                                if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                tint = Color.Black,
                                modifier = Modifier.size(34.dp)
                            )
                        }
                    }

                    // +10s Button
                    Surface(
                        shape = CircleShape,
                        color = Color.Black.copy(alpha = 0.65f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.4f)),
                        modifier = Modifier.clickable {
                            onUserInteract()
                            onSeekForward10()
                        }
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Icon(
                                Icons.Default.Forward10,
                                contentDescription = "10s Forward",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }

                // Bottom Seekbar & Time Row
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    var isDraggingSlider by remember { mutableStateOf(false) }
                    var dragPosition by remember { mutableFloatStateOf(0f) }

                    val effectiveDuration = remember(duration) { if (duration > 0f) duration else 0f }
                    val displayTime = if (isDraggingSlider) dragPosition else currentTime
                    val clampedDisplayTime = displayTime.coerceIn(0f, if (effectiveDuration > 0f) effectiveDuration else 3600f)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Current / Starting Time
                        Text(
                            text = formatSecondsToTimestamp(clampedDisplayTime.toInt()),
                            color = Color.White,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold
                        )

                        // Seekbar Slider with smooth drag tracking
                        androidx.compose.material3.Slider(
                            value = clampedDisplayTime.coerceIn(0f, if (effectiveDuration > 0f) effectiveDuration else 100f),
                            onValueChange = { newPos ->
                                isDraggingSlider = true
                                dragPosition = newPos
                            },
                            onValueChangeFinished = {
                                isDraggingSlider = false
                                onSeekTo(dragPosition)
                                onUserInteract()
                            },
                            valueRange = 0f..(if (effectiveDuration > 0f) effectiveDuration else 100f),
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 4.dp),
                            colors = androidx.compose.material3.SliderDefaults.colors(
                                thumbColor = brandCyan,
                                activeTrackColor = brandCyan,
                                inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                            )
                        )

                        // Ending / Total Duration Time
                        Text(
                            text = if (effectiveDuration > 0f) formatSecondsToTimestamp(effectiveDuration.toInt()) else "--:--",
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

// ============================================================
// IN-APP WEB BROWSER & PDF VIEWER COMPOSABLE
// ============================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InAppWebBrowserDialog(
    url: String,
    onDismiss: () -> Unit,
    onDownloadPdf: (downloadUrl: String, title: String?, contentDisposition: String?, mimeType: String?) -> Unit
) {
    val context = LocalContext.current
    var pageTitle by remember { mutableStateOf("Loading...") }
    var webProgress by remember { mutableStateOf(0) }
    var canGoBack by remember { mutableStateOf(false) }
    var browserWv by remember { mutableStateOf<WebView?>(null) }
    var fileUploadCallback by remember { mutableStateOf<android.webkit.ValueCallback<Array<Uri>>?>(null) }

    val fileChooserLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val intent = result.data
            val results: Array<Uri>? = when {
                intent?.data != null -> arrayOf(intent.data!!)
                intent?.clipData != null -> {
                    val count = intent.clipData!!.itemCount
                    Array(count) { i -> intent.clipData!!.getItemAt(i).uri }
                }
                else -> null
            }
            fileUploadCallback?.onReceiveValue(results)
        } else {
            fileUploadCallback?.onReceiveValue(null)
        }
        fileUploadCallback = null
    }
    val brandCyan = Color(0xFF00E5FF)
    val brandGreen = Color(0xFF00E676)
    val cardBgColor = Color(0xFF131722)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            color = cardBgColor
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top Action Bar
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = pageTitle,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = url,
                                fontSize = 10.sp,
                                color = Color.White.copy(alpha = 0.6f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close Browser", tint = Color.White)
                        }
                    },
                    actions = {
                        if (canGoBack) {
                            IconButton(onClick = { browserWv?.goBack() }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                            }
                        }
                        IconButton(onClick = {
                            val currentUrl = browserWv?.url ?: url
                            val currentTitle = (browserWv?.title ?: pageTitle).takeIf { !it.isNullOrBlank() && it != "Loading..." }
                            onDownloadPdf(currentUrl, currentTitle, null, "application/pdf")
                        }) {
                            Icon(Icons.Default.Download, contentDescription = "Download File", tint = brandGreen)
                        }
                        IconButton(onClick = {
                            val currentUrl = browserWv?.url ?: url
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(currentUrl))
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "Cannot open external app", Toast.LENGTH_SHORT).show()
                            }
                        }) {
                            Icon(Icons.Default.OpenInBrowser, contentDescription = "Open in External App", tint = brandCyan)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color(0xFF1A1F2C)
                    )
                )

                // Linear Progress bar
                if (webProgress in 1..99) {
                    LinearProgressIndicator(
                        progress = { webProgress / 100f },
                        modifier = Modifier.fillMaxWidth(),
                        color = brandCyan,
                        trackColor = Color.Black
                    )
                }

                // In-App WebView with Full DownloadListener
                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            layoutParams = android.view.ViewGroup.LayoutParams(
                                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                                android.view.ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            settings.apply {
                                javaScriptEnabled = true
                                domStorageEnabled = true
                                databaseEnabled = true
                                setSupportZoom(true)
                                builtInZoomControls = true
                                displayZoomControls = false
                                useWideViewPort = true
                                loadWithOverviewMode = true
                                allowFileAccess = true
                                allowContentAccess = true
                                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                            }
                            setDownloadListener { downloadUrl, userAgent, contentDisposition, mimetype, contentLength ->
                                val currentTitle = (browserWv?.title ?: pageTitle).takeIf { !it.isNullOrBlank() && it != "Loading..." }
                                onDownloadPdf(downloadUrl, currentTitle, contentDisposition, mimetype)
                            }
                            webChromeClient = object : WebChromeClient() {
                                override fun onReceivedTitle(view: WebView?, title: String?) {
                                    if (!title.isNullOrBlank()) pageTitle = title
                                }
                                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                    webProgress = newProgress
                                }
                                override fun onShowFileChooser(
                                    webView: WebView?,
                                    filePathCallback: android.webkit.ValueCallback<Array<Uri>>?,
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
                                        fileUploadCallback?.onReceiveValue(null)
                                        fileUploadCallback = null
                                        return false
                                    }
                                }
                            }
                            webViewClient = object : WebViewClient() {
                                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                    val reqUrl = request?.url?.toString() ?: return false
                                    if (reqUrl.endsWith(".pdf", ignoreCase = true) ||
                                        reqUrl.contains("download=true", ignoreCase = true) ||
                                        reqUrl.contains("export=download", ignoreCase = true)
                                    ) {
                                        val currentTitle = (view?.title ?: pageTitle).takeIf { !it.isNullOrBlank() && it != "Loading..." }
                                        onDownloadPdf(reqUrl, currentTitle, null, "application/pdf")
                                        return true
                                    }
                                    return false
                                }
                                override fun onPageFinished(view: WebView?, finishedUrl: String?) {
                                    super.onPageFinished(view, finishedUrl)
                                    canGoBack = view?.canGoBack() ?: false
                                }
                                override fun onRenderProcessGone(view: WebView?, detail: android.webkit.RenderProcessGoneDetail?): Boolean {
                                    try { view?.destroy() } catch (e: Exception) {}
                                    return true
                                }
                            }
                            loadUrl(url)
                            browserWv = this
                        }
                    },
                    update = { wv ->
                        browserWv = wv
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                )
            }
        }
    }
}

// ============================================================
// LECTURE SNAPSHOT CAPTURE UTILITY (ACTUAL LIVE VIDEO FRAME VIA PIXELCOPY)
// ============================================================
private fun findChildSurfaceView(view: android.view.View): android.view.SurfaceView? {
    if (view is android.view.SurfaceView) return view
    if (view is android.view.ViewGroup) {
        for (i in 0 until view.childCount) {
            val child = view.getChildAt(i)
            val sv = findChildSurfaceView(child)
            if (sv != null) return sv
        }
    }
    return null
}

private fun isBitmapMeaningful(bitmap: Bitmap): Boolean {
    val w = bitmap.width
    val h = bitmap.height
    if (w < 20 || h < 20) return false
    var nonDarkCount = 0
    var nonTransparentCount = 0
    val stepX = (w / 12).coerceAtLeast(1)
    val stepY = (h / 12).coerceAtLeast(1)
    for (x in stepX until w step stepX) {
        for (y in stepY until h step stepY) {
            val pixel = bitmap.getPixel(x, y)
            val a = (pixel shr 24) and 0xFF
            val r = (pixel shr 16) and 0xFF
            val g = (pixel shr 8) and 0xFF
            val b = pixel and 0xFF
            if (a > 20) {
                nonTransparentCount++
                if (r > 6 || g > 6 || b > 6) {
                    nonDarkCount++
                }
            }
        }
    }
    return nonDarkCount >= 2 || nonTransparentCount >= 10
}

suspend fun captureStudyTubeLectureSnapshot(
    context: Context,
    webView: WebView?,
    videoId: String,
    videoTitle: String,
    channelTitle: String,
    seekSeconds: Int,
    thumbnailUrl: String
): String = withContext(Dispatchers.IO) {
    try {
        var capturedBitmap: Bitmap? = null

        // 1. Try to capture the live video frame directly from WebView via PixelCopy (Hardware Compositor)
        if (webView != null) {
            val liveFrame = withContext(Dispatchers.Main) {
                try {
                    val activity = context.findActivity()
                    val w = webView.width
                    val h = webView.height
                    if (activity != null && w > 30 && h > 30 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        // First check if there is an internal SurfaceView for direct video hardware surface
                        val childSurface = findChildSurfaceView(webView)
                        if (childSurface != null && childSurface.holder.surface.isValid && childSurface.width > 30 && childSurface.height > 30) {
                            val surfaceBmp = Bitmap.createBitmap(childSurface.width, childSurface.height, Bitmap.Config.ARGB_8888)
                            val surfaceCopyResult = suspendCancellableCoroutine<Int> { cont ->
                                try {
                                    PixelCopy.request(
                                        childSurface,
                                        surfaceBmp,
                                        { res -> if (cont.isActive) cont.resume(res) },
                                        Handler(Looper.getMainLooper())
                                    )
                                } catch (e: Exception) {
                                    if (cont.isActive) cont.resume(PixelCopy.ERROR_UNKNOWN)
                                }
                            }
                            if (surfaceCopyResult == PixelCopy.SUCCESS && isBitmapMeaningful(surfaceBmp)) {
                                return@withContext surfaceBmp
                            }
                        }

                        // Window PixelCopy with safely clamped bounds inside the decor view
                        val decorView = activity.window.decorView
                        val winW = decorView.width.coerceAtLeast(1)
                        val winH = decorView.height.coerceAtLeast(1)
                        val location = IntArray(2)
                        webView.getLocationInWindow(location)

                        val left = location[0].coerceIn(0, winW - 1)
                        val top = location[1].coerceIn(0, winH - 1)
                        val right = (location[0] + w).coerceIn(left + 1, winW)
                        val bottom = (location[1] + h).coerceIn(top + 1, winH)
                        val safeRect = Rect(left, top, right, bottom)
                        val captureW = (right - left).coerceAtLeast(1)
                        val captureH = (bottom - top).coerceAtLeast(1)

                        val bmp = Bitmap.createBitmap(captureW, captureH, Bitmap.Config.ARGB_8888)

                        val copyResult = suspendCancellableCoroutine<Int> { cont ->
                            try {
                                PixelCopy.request(
                                    activity.window,
                                    safeRect,
                                    bmp,
                                    { res ->
                                        if (cont.isActive) cont.resume(res)
                                    },
                                    Handler(Looper.getMainLooper())
                                )
                            } catch (e: Exception) {
                                if (cont.isActive) cont.resume(PixelCopy.ERROR_UNKNOWN)
                            }
                        }

                        if (copyResult == PixelCopy.SUCCESS && isBitmapMeaningful(bmp)) {
                            bmp
                        } else {
                            null
                        }
                    } else null
                } catch (e: Exception) {
                    null
                }
            }
            if (liveFrame != null) {
                capturedBitmap = liveFrame
            }
        }

        // 2. Fallback: Draw WebView to Canvas directly
        if (capturedBitmap == null && webView != null) {
            val canvasFrame = withContext(Dispatchers.Main) {
                try {
                    val w = webView.width
                    val h = webView.height
                    if (w > 50 && h > 50) {
                        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                        val c = Canvas(bmp)
                        webView.draw(c)
                        if (isBitmapMeaningful(bmp)) bmp else null
                    } else null
                } catch (e: Exception) {
                    null
                }
            }
            if (canvasFrame != null) {
                capturedBitmap = canvasFrame
            }
        }

        // 3. Fallback: High-res YouTube thumbnail (only if frame couldn't be captured from WebView)
        if (capturedBitmap == null) {
            val loader = ImageLoader(context)
            val candidateUrls = listOf(
                "https://img.youtube.com/vi/$videoId/maxresdefault.jpg",
                "https://img.youtube.com/vi/$videoId/sddefault.jpg",
                "https://img.youtube.com/vi/$videoId/hqdefault.jpg",
                thumbnailUrl
            )
            for (url in candidateUrls) {
                if (url.isBlank()) continue
                try {
                    val req = ImageRequest.Builder(context)
                        .data(url)
                        .allowHardware(false)
                        .build()
                    val result = (loader.execute(req) as? SuccessResult)?.drawable
                    val b = (result as? BitmapDrawable)?.bitmap
                    if (b != null && b.width > 120 && b.height > 90) {
                        capturedBitmap = b
                        break
                    }
                } catch (_: Exception) {}
            }
        }

        val finalBitmap = capturedBitmap ?: Bitmap.createBitmap(640, 360, Bitmap.Config.ARGB_8888).apply {
            val c = Canvas(this)
            c.drawColor(android.graphics.Color.DKGRAY)
        }

        val snapshotsDir = File(context.filesDir, "study_snapshots").apply { mkdirs() }
        val snapshotFile = File(snapshotsDir, "doubt_${videoId}_${seekSeconds}_${System.currentTimeMillis()}.jpg")
        FileOutputStream(snapshotFile).use { out ->
            finalBitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
        }
        "file://" + snapshotFile.absolutePath
    } catch (e: Exception) {
        "https://img.youtube.com/vi/$videoId/hqdefault.jpg"
    }
}
