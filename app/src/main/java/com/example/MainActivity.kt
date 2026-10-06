package com.example

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.content.pm.PackageManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.AppViewModel
import com.example.ui.components.AppAtmosphericBackground
import com.example.ui.components.FloatingGlassSidebar
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.workers.NotificationScheduler

class MainActivity : ComponentActivity() {
    private val viewModel: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            val webViewCacheDir = java.io.File(cacheDir, "WebView/Default/HTTP Cache/Code Cache/js")
            if (!webViewCacheDir.exists()) {
                webViewCacheDir.mkdirs()
            }
        } catch (e: Exception) {
            // Ignore directory creation failure
        }
        
        NotificationScheduler.scheduleAllReminders(this)
        handlePdfIntent(intent)

        // Configure hardware-accelerated, leak-free Coil image caching
        try {
            val imageLoader = coil.ImageLoader.Builder(this)
                .memoryCache {
                    coil.memory.MemoryCache.Builder(this)
                        .maxSizePercent(0.25)
                        .build()
                }
                .diskCache {
                    coil.disk.DiskCache.Builder()
                        .directory(cacheDir.resolve("image_cache"))
                        .maxSizeBytes(100L * 1024 * 1024)
                        .build()
                }
                .crossfade(true)
                .respectCacheHeaders(false)
                .build()
            coil.Coil.setImageLoader(imageLoader)
        } catch (_: Exception) {}
        
        enableEdgeToEdge()
        setContent {
            val isDarkMode by viewModel.isDarkMode.collectAsStateWithLifecycle()
            val isDirectPdfIntent = (intent?.action == Intent.ACTION_VIEW || intent?.action == Intent.ACTION_EDIT || intent?.action == Intent.ACTION_SEND) && 
                (intent?.data != null || (intent?.clipData != null && intent?.clipData!!.itemCount > 0) || intent?.hasExtra(Intent.EXTRA_STREAM) == true)
            var showSplash by remember { mutableStateOf(!isDirectPdfIntent) }

            MyApplicationTheme(darkTheme = isDarkMode) {
                Crossfade(
                    targetState = showSplash,
                    animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
                    label = "splashTransition"
                ) { isSplash ->
                    if (isSplash) {
                        com.example.ui.components.AppSplashScreen(
                            isDark = isDarkMode,
                            onSplashFinished = { showSplash = false }
                        )
                    } else {
                        MainScreen(viewModel)
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handlePdfIntent(intent)
    }

    private fun handlePdfIntent(intent: Intent?) {
        if (intent == null) return
        val action = intent.action
        var uri = intent.data
        val type = intent.type

        if (uri == null && intent.clipData != null && intent.clipData!!.itemCount > 0) {
            uri = intent.clipData!!.getItemAt(0).uri
        }

        if (uri == null && (action == Intent.ACTION_SEND)) {
            val streamUri = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(Intent.EXTRA_STREAM) as? Uri
            }
            if (streamUri != null) {
                uri = streamUri
            }
        }

        val isPdf = (type?.contains("pdf", ignoreCase = true) == true) ||
                (uri != null && (
                    uri.path?.endsWith(".pdf", ignoreCase = true) == true ||
                    uri.toString().endsWith(".pdf", ignoreCase = true) ||
                    try { contentResolver.getType(uri)?.contains("pdf", ignoreCase = true) == true } catch (_: Exception) { false }
                ))

        if ((action == Intent.ACTION_VIEW || action == Intent.ACTION_EDIT || action == Intent.ACTION_SEND) && (isPdf || uri != null)) {
            if (uri != null) {
                try {
                    contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (_: Exception) {
                }

                var title = "PDF Document"
                try {
                    if (uri.scheme == "content") {
                        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                            val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                            if (nameIndex != -1 && cursor.moveToFirst()) {
                                val resolvedName = cursor.getString(nameIndex)
                                if (!resolvedName.isNullOrBlank()) {
                                    title = resolvedName
                                }
                            }
                        }
                    } else if (uri.scheme == "file") {
                        title = uri.lastPathSegment ?: "PDF Document"
                    }
                } catch (_: Exception) {
                    title = uri.lastPathSegment ?: "PDF Document"
                }

                viewModel.openPdfInReader(uri, title)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.syncTimerFromClock()
        com.example.widget.WidgetUpdateHelper.updateAllWidgets(this)
    }
}

@Composable
fun MainScreen(viewModel: AppViewModel) {
    val isDarkMode by viewModel.isDarkMode.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted -> hasNotificationPermission = isGranted }
    )

    LaunchedEffect(Unit) {
        if (!hasNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: "dashboard"
    
    val isTimerRunning by viewModel.timerIsRunning.collectAsStateWithLifecycle()
    val isFullScreenTimer by viewModel.isFullScreenTimer.collectAsStateWithLifecycle()
    val isFullScreenBrowser by viewModel.isFullScreenBrowser.collectAsStateWithLifecycle()
    val pdfOpenTrigger by viewModel.pdfOpenTrigger.collectAsStateWithLifecycle()

    val onNavigateToRoute: (String) -> Unit = remember(navController) {
        { targetRoute ->
            val currentBaseRoute = navController.currentBackStackEntry?.destination?.route?.split("?")?.get(0)
            val targetBaseRoute = targetRoute.split("?")[0]
            if (currentBaseRoute != targetBaseRoute) {
                if (targetBaseRoute == "dashboard") {
                    navController.popBackStack("dashboard", false)
                } else {
                    val popped = try {
                        navController.popBackStack(targetRoute, false)
                    } catch (e: Exception) {
                        false
                    }
                    if (!popped) {
                        navController.navigate(targetRoute) {
                            launchSingleTop = true
                        }
                    }
                }
            }
        }
    }

    // Auto-navigate to PDF Viewer when a document is opened externally or internally
    LaunchedEffect(pdfOpenTrigger) {
        if (pdfOpenTrigger != null && currentRoute != "pdf_viewer") {
            onNavigateToRoute("pdf_viewer")
        }
    }

    // Auto-navigate to target screen when opened from Home Screen Widget
    val activity = context as? ComponentActivity
    LaunchedEffect(activity?.intent) {
        val navigateTo = activity?.intent?.getStringExtra("NAVIGATE_TO")
        if (!navigateTo.isNullOrBlank()) {
            val route = when (navigateTo) {
                "AI_CBT_SCREEN" -> "ai_test"
                "AI_CHAT_SCREEN" -> "chat"
                "STUDY_TIMER_SCREEN" -> "timer"
                "TIMETABLE_SCREEN" -> "targets"
                "MISTAKE_NOTEBOOK_SCREEN" -> "mistakes"
                "STUDY_ROOM_SCREEN" -> "dashboard"
                else -> null
            }
            if (route != null) {
                onNavigateToRoute(route)
                activity.intent.removeExtra("NAVIGATE_TO")
            }
        }
    }

    val onSafeNavigateBack: () -> Unit = remember(navController) {
        {
            val popped = try {
                navController.popBackStack()
            } catch (e: Exception) {
                false
            }
            if (!popped) {
                // If back stack has no earlier entry, safely return to dashboard
                navController.navigate("dashboard") {
                    popUpTo("dashboard") { inclusive = false }
                    launchSingleTop = true
                }
            }
        }
    }

    AppAtmosphericBackground(isDark = isDarkMode) {
        Box(modifier = Modifier.fillMaxSize()) {
            NavHost(
                navController = navController,
                startDestination = "dashboard",
                modifier = Modifier.fillMaxSize(),
                enterTransition = {
                    fadeIn(animationSpec = tween(150, easing = FastOutSlowInEasing))
                },
                exitTransition = {
                    fadeOut(animationSpec = tween(90, easing = FastOutLinearInEasing))
                },
                popEnterTransition = {
                    fadeIn(animationSpec = tween(150, easing = FastOutSlowInEasing))
                },
                popExitTransition = {
                    fadeOut(animationSpec = tween(90, easing = FastOutLinearInEasing))
                }
            ) {
                composable("dashboard") { DashboardScreen(viewModel, onNavigate = onNavigateToRoute) }
                composable("timer") { TimerScreen(viewModel, onNavigateBack = onSafeNavigateBack) }
                composable("targets") { TargetsScreen(viewModel, onNavigate = onNavigateToRoute, onNavigateBack = onSafeNavigateBack) }
                composable("habits") { com.example.ui.screens.HabitTrackerScreen(viewModel, onNavigateBack = onSafeNavigateBack) }
                composable("mocks") { MockTestScreen(viewModel, onNavigate = onNavigateToRoute, onNavigateBack = onSafeNavigateBack) }
                composable("gmc_cutoffs") { GmcCutoffScreen(viewModel, onNavigateBack = onSafeNavigateBack) }
                composable("flashcards") { FlashcardScreen(viewModel, onNavigateBack = onSafeNavigateBack) }
                composable("books") { BooksScreen(viewModel, onNavigate = onNavigateToRoute, onNavigateBack = onSafeNavigateBack) }
                composable("chat") { ChatScreen(viewModel, onNavigateBack = onSafeNavigateBack) }
                composable("pdf_viewer") { com.example.ui.screens.PdfViewerScreen(viewModel, onNavigate = onNavigateToRoute, onNavigateBack = onSafeNavigateBack) }
                composable("mistakes") { com.example.ui.screens.MistakesScreen(viewModel, onNavigate = onNavigateToRoute, onNavigateBack = onSafeNavigateBack) }
                composable("studytube") { com.example.ui.screens.StudyTubeScreen(viewModel, onNavigate = onNavigateToRoute, onNavigateBack = onSafeNavigateBack) }
                composable("ai_test") { com.example.ui.screens.AiTestScreen(viewModel, onNavigateBack = onSafeNavigateBack, onNavigate = onNavigateToRoute) }
                composable("custom_test") { com.example.ui.screens.CustomTestScreen(viewModel, onNavigate = onNavigateToRoute, onNavigateBack = onSafeNavigateBack) }
                composable("weak_topic_test") { com.example.ui.screens.WeakTopicTestScreen(viewModel, onNavigateBack = onSafeNavigateBack, onNavigate = onNavigateToRoute) }
                composable("monthly") { com.example.ui.screens.MonthlyScreen(viewModel, onNavigateBack = onSafeNavigateBack) }
                composable(
                    route = "testbook?url={url}",
                    arguments = listOf(androidx.navigation.navArgument("url") { 
                        type = androidx.navigation.NavType.StringType
                        defaultValue = "https://testbook.com/test-series" 
                    })
                ) { backStackEntry ->
                    val encodedUrl = backStackEntry.arguments?.getString("url") ?: "https://testbook.com/test-series"
                    val url = try {
                        java.net.URLDecoder.decode(encodedUrl, "UTF-8")
                    } catch (_: Exception) {
                        encodedUrl
                    }
                    TestbookWebScreen(
                        viewModel,
                        initialUrl = url,
                        onBackToApp = onSafeNavigateBack,
                        onNavigate = onNavigateToRoute
                    )
                }
            }

            // Distraction-Free Floating Glass Sidebar with Collapsible Floating Arrow Handle
            if (!isFullScreenTimer && !isFullScreenBrowser && currentRoute != "pdf_viewer") {
                FloatingGlassSidebar(
                    currentRoute = currentRoute,
                    onNavigate = onNavigateToRoute,
                    isTimerRunning = isTimerRunning,
                    isDark = isDarkMode,
                    viewModel = viewModel
                )
            }
        }
    }
}

