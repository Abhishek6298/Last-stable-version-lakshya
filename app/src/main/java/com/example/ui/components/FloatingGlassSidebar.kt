package com.example.ui.components

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.GeminiChatAssistant
import com.example.data.StudyPortal
import com.example.data.StudyWebVerificationResult
import com.example.ui.AppViewModel
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

data class SidebarNavItem(
    val route: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val isActionPortal: Boolean = false
)

/**
 * Modern Floating Acrylic Glass Sidebar with Edge-Swipe gesture support.
 * Distraction-free: No intrusive floating buttons over study content.
 * Simply swipe right from the left edge to open, and swipe left or tap anywhere to close.
 */
@Composable
fun FloatingGlassSidebar(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    isTimerRunning: Boolean,
    isDark: Boolean,
    viewModel: AppViewModel,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }
    var showBrowserSelector by remember { mutableStateOf(false) }
    var showProfileDialog by remember { mutableStateOf(false) }
    val studyPortals by viewModel.studyPortals.collectAsStateWithLifecycle()
    var showAddEditPortalDialog by remember { mutableStateOf(false) }
    var editingPortal by remember { mutableStateOf<StudyPortal?>(null) }
    var portalToDelete by remember { mutableStateOf<StudyPortal?>(null) }

    val isSidebarOpenByVm by viewModel.isSidebarOpen.collectAsState()

    LaunchedEffect(isSidebarOpenByVm) {
        if (isExpanded != isSidebarOpenByVm) {
            isExpanded = isSidebarOpenByVm
        }
    }

    LaunchedEffect(isExpanded) {
        if (isExpanded != viewModel.isSidebarOpen.value) {
            if (isExpanded) viewModel.openSidebar() else viewModel.closeSidebar()
        }
    }

    // Collect aspirant profile details to showcase in the sidebar header
    val candidateName by viewModel.userName.collectAsState()
    val candidateClass by viewModel.userClass.collectAsState()
    val candidateAvatarUri by viewModel.userAvatarUri.collectAsState()
    val appPassword by viewModel.appPassword.collectAsState()
    val habitPassword by viewModel.habitPassword.collectAsState()
    val securityQuestion by viewModel.securityQuestion.collectAsState()
    val securityAnswer by viewModel.securityAnswer.collectAsState()
    val geminiApiKey1 by viewModel.geminiApiKey1.collectAsState()
    val geminiApiKey2 by viewModel.geminiApiKey2.collectAsState()
    val geminiApiKey3 by viewModel.geminiApiKey3.collectAsState()
    val selectedGeminiModel by viewModel.selectedGeminiModel.collectAsState()
    val aiProvider by viewModel.aiProvider.collectAsState()
    val neetTargetMillis by viewModel.neetTargetMillis.collectAsState()
    val studyLogs by viewModel.studyLogs.collectAsState()
    val mockTests by viewModel.mockTests.collectAsState()
    val dailyPractices by viewModel.dailyPractices.collectAsState()
    val completedTopics by viewModel.completedTopics.collectAsState()
    val notificationTimes by viewModel.notificationTimes.collectAsState()

    val primaryAccent = Color(0xFF6366F1)

    // Categorized Direct Navigation Items (Clean, flat, 1-tap navigation)
    val workspaceItems = remember(isTimerRunning) {
        listOf(
            SidebarNavItem("dashboard", "Dashboard", "Daily Overview & Streak", Icons.Default.Dashboard),
            SidebarNavItem("monthly", "Monthly Analytics", "Past Months & Performance", Icons.Default.CalendarMonth),
            SidebarNavItem("timer", "Focus Timer", "Pomodoro & Deep Work", Icons.Default.Timer),
            SidebarNavItem("targets", "Goal Tracker", "Subject Goals & Daily Plan", Icons.Default.Checklist),
            SidebarNavItem("habits", "Habits & Garden", "Forest Focus & Study Streak", Icons.Default.Park)
        )
    }

    val practiceItems = remember {
        listOf(
            SidebarNavItem("ai_test", "AI CBT Test Series", "NTA Pattern & AI Solutions", Icons.Default.AutoAwesome),
            SidebarNavItem("weak_topic_test", "Weak Topic Tests", "AI Diagnostics & Targeted Prep", Icons.Default.Troubleshoot),
            SidebarNavItem("mistakes", "Mistakes Notebook", "Revision Bank & Analytics", Icons.Default.Error),
            SidebarNavItem("mocks", "Mock Test Analytics", "Scores, Accuracy & Percentiles", Icons.Default.Analytics),
            SidebarNavItem("studytube", "StudyTube", "Distraction-Free Video Feed", Icons.Default.PlayCircle)
        )
    }

    val academicItems = remember {
        listOf(
            SidebarNavItem("books", "NCERT Library", "Physics, Chem, Bio Textbooks", Icons.AutoMirrored.Filled.MenuBook),
            SidebarNavItem("chat", "AI Assistant", "Doubt Solver & 24/7 Mentor", Icons.AutoMirrored.Filled.Chat),
            SidebarNavItem("flashcards", "NCERT Flashcards", "Spaced Repetition Active Recall", Icons.Default.Style),
            SidebarNavItem("gmc_cutoffs", "College Cutoffs", "Rank & State Quota Finder", Icons.Default.School),
            SidebarNavItem("testbook", "Web Portals", "PW Thor Live & Testbook", Icons.Default.Public, isActionPortal = true)
        )
    }

    val sidebarBg = remember(isDark) {
        if (isDark) {
            Brush.linearGradient(
                listOf(
                    Color(0xD61E293B),
                    Color(0xE3111827),
                    Color(0xEE0F172A),
                    Color(0xF5080D1A)
                )
            )
        } else {
            Brush.linearGradient(
                listOf(
                    Color(0xEBFFFFFF),
                    Color(0xCCF8FAFC),
                    Color(0xB8F1F5F9),
                    Color(0xD9E2E8F0)
                )
            )
        }
    }

    val sidebarBorder = remember(isDark) {
        if (isDark) {
            Brush.linearGradient(
                listOf(
                    Color(0x73FFFFFF),
                    Color(0x33818CF8),
                    Color(0x14FFFFFF),
                    Color(0x2E38BDF8)
                )
            )
        } else {
            Brush.linearGradient(
                listOf(
                    Color(0xF5FFFFFF),
                    Color(0x666366F1),
                    Color(0x4D38BDF8),
                    Color(0xB3FFFFFF)
                )
            )
        }
    }

    // Portal Selector Dialog
    if (showBrowserSelector) {
        Dialog(onDismissRequest = { showBrowserSelector = false }) {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = if (isDark) Color(0xFF0F172A) else Color.White,
                border = BorderStroke(1.dp, if (isDark) Color(0x4D6366F1) else Color(0xFFCBD5E1)),
                shadowElevation = 24.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 580.dp)
                    .padding(horizontal = 4.dp, vertical = 12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Dialog Header
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
                                    "Online Study Portals",
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
                        // Close Dialog (X)
                        IconButton(
                            onClick = { showBrowserSelector = false },
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

                    // Fixed Height Sleek "+ Add Custom Study Portal" Action Button
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

                    Spacer(modifier = Modifier.height(12.dp))

                    // List of Dynamic Study Portals
                    if (studyPortals.isEmpty()) {
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
                                Text("No study portals configured yet.", fontSize = 12.sp, color = Color.Gray)
                                Spacer(modifier = Modifier.height(8.dp))
                                TextButton(onClick = { viewModel.resetStudyPortalsToDefault() }) {
                                    Text("Restore Default Portals (PW Thor & Testbook)", fontSize = 11.5.sp)
                                }
                            }
                        }
                    } else {
                        studyPortals.forEach { portal ->
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (isDark) Color(0x1F6366F1) else Color(0xFFF8FAFC),
                                border = BorderStroke(
                                    1.dp,
                                    if (isDark) Color(0x33818CF8) else Color(0xFFE2E8F0)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.5.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            showBrowserSelector = false
                                            isExpanded = false
                                            val encoded = java.net.URLEncoder.encode(portal.url, "UTF-8")
                                            onNavigate("testbook?url=$encoded")
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

                    // Footer Options: Restore Defaults
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

    // Add / Edit Portal Dialog
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
    if (portalToDelete != null) {
        val target = portalToDelete!!
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
                    "Are you sure you want to remove this study web portal from your quick launcher?",
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

    Box(modifier = modifier.fillMaxSize()) {
        // Dimmed Scrim backdrop when sidebar is open with spring fade
        AnimatedVisibility(
            visible = isExpanded,
            enter = fadeIn(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)),
            exit = fadeOut(animationSpec = tween(180))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = if (isDark) 0.65f else 0.45f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { isExpanded = false }
                    .pointerInput(Unit) {
                        var totalDragX = 0f
                        detectHorizontalDragGestures(
                            onDragStart = { totalDragX = 0f },
                            onHorizontalDrag = { change, dragAmount ->
                                totalDragX += dragAmount
                                if (totalDragX < -18f) {
                                    isExpanded = false
                                    change.consume()
                                }
                            }
                        )
                    }
            )
        }

        // 1. COLLAPSED: Distraction-Free Edge-Swipe Detector on left screen edge (Only on dashboard, never covering top app bar or bottom navigation)
        if (!isExpanded && currentRoute.split("?")[0] == "dashboard") {
            Box(
                modifier = Modifier
                    .padding(top = 96.dp, bottom = 96.dp)
                    .fillMaxHeight()
                    .width(24.dp)
                    .align(Alignment.CenterStart)
                    .pointerInput(Unit) {
                        var totalDragX = 0f
                        detectHorizontalDragGestures(
                            onDragStart = { totalDragX = 0f },
                            onHorizontalDrag = { change, dragAmount ->
                                totalDragX += dragAmount
                                if (totalDragX > 16f) {
                                    isExpanded = true
                                    change.consume()
                                }
                            }
                        )
                    }
            )
        }

        // 2. EXPANDED FLOATING ACRYLIC GLASS SIDEBAR (Fast, Crisp Transition)
        AnimatedVisibility(
            visible = isExpanded,
            enter = slideInHorizontally(
                initialOffsetX = { -it },
                animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing)
            ) + fadeIn(animationSpec = tween(durationMillis = 140)),
            exit = slideOutHorizontally(
                targetOffsetX = { -it },
                animationSpec = tween(durationMillis = 140, easing = FastOutLinearInEasing)
            ) + fadeOut(animationSpec = tween(durationMillis = 100)),
            modifier = Modifier.align(Alignment.CenterStart)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxHeight()
                    .widthIn(min = 285.dp, max = 325.dp)
                    .padding(vertical = 16.dp, horizontal = 12.dp)
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .pointerInput(Unit) {
                        var totalDragX = 0f
                        detectHorizontalDragGestures(
                            onDragStart = { totalDragX = 0f },
                            onHorizontalDrag = { change, dragAmount ->
                                totalDragX += dragAmount
                                if (totalDragX < -22f) {
                                    isExpanded = false
                                    change.consume()
                                }
                            }
                        )
                    },
                shape = RoundedCornerShape(28.dp),
                color = Color.Transparent,
                shadowElevation = 18.dp
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(sidebarBg)
                        .border(1.5.dp, sidebarBorder, RoundedCornerShape(28.dp))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        // SIDEBAR HEADER WITH COLLAPSE TOGGLE
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(
                                            Brush.linearGradient(
                                                listOf(
                                                    if (isDark) Color(0xFF6366F1) else Color(0xFF818CF8),
                                                    if (isDark) Color(0xFF4F46E5) else Color(0xFF6366F1)
                                                )
                                            )
                                        )
                                        .border(
                                            1.dp,
                                            Color.White.copy(alpha = 0.4f),
                                            RoundedCornerShape(14.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.School,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Column {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            "NEET & JEE PREP",
                                            fontWeight = FontWeight.Black,
                                            fontSize = 14.5.sp,
                                            color = if (isDark) Color.White else Color(0xFF0F172A)
                                        )
                                    }
                                    Text(
                                        "AIR TARGET 1 • PRO SUITE",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDark) Color(0xFF818CF8) else Color(0xFF4F46E5),
                                        letterSpacing = 0.5.sp
                                    )
                                }
                            }

                            // Collapse button
                            Surface(
                                shape = CircleShape,
                                color = if (isDark) Color(0x330F172A) else Color(0x80FFFFFF),
                                border = BorderStroke(1.dp, if (isDark) Color(0x33FFFFFF) else Color(0x4DCBD5E1)),
                                shadowElevation = 2.dp,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clickable { isExpanded = false }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                        contentDescription = "Collapse Sidebar",
                                        tint = if (isDark) Color(0xFFF1F5F9) else Color(0xFF1E293B),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Candidate Identity Mini-Card (Liquid Glass Pill)
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (isDark) Color(0x221E293B) else Color(0x99FFFFFF),
                            border = BorderStroke(
                                1.dp,
                                if (isDark) Color(0x33818CF8) else Color(0x4D6366F1)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    isExpanded = false
                                    showProfileDialog = true
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(
                                            Brush.linearGradient(
                                                listOf(Color(0xFF818CF8), Color(0xFF6366F1))
                                            )
                                        )
                                        .border(1.5.dp, Color.White.copy(alpha = 0.6f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (!candidateAvatarUri.isNullOrBlank()) {
                                        AsyncImage(
                                            model = candidateAvatarUri,
                                            contentDescription = "Candidate Avatar",
                                            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else {
                                        Text(
                                            text = candidateName.take(1).uppercase(),
                                            color = Color.White,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 15.sp
                                        )
                                    }
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                                    ) {
                                        Text(
                                            text = candidateName,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isDark) Color.White else Color(0xFF0F172A),
                                            maxLines = 1,
                                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f, fill = false)
                                        )
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(Color(0xFF10B981).copy(alpha = 0.2f))
                                                .padding(horizontal = 4.dp, vertical = 1.dp)
                                        ) {
                                            Text("PRO", fontSize = 7.5.sp, fontWeight = FontWeight.Black, color = Color(0xFF10B981))
                                        }
                                    }
                                    Text(
                                        text = candidateClass,
                                        fontSize = 10.sp,
                                        color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                                        maxLines = 1
                                    )
                                }

                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = if (isDark) Color(0xFF818CF8) else Color(0xFF6366F1),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 10.dp),
                            color = if (isDark) Color(0x22FFFFFF) else Color(0x40CBD5E1)
                        )

                        // NAVIGATION ITEMS LIST (Categorized & Scrollable)
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            // 1. CORE WORKSPACE
                            Text(
                                text = "CORE WORKSPACE",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                color = if (isDark) Color(0x99818CF8) else Color(0xFF6366F1),
                                letterSpacing = 1.1.sp,
                                modifier = Modifier.padding(start = 6.dp, top = 4.dp, bottom = 4.dp)
                            )
                            workspaceItems.forEach { item ->
                                val isSelected = currentRoute.split("?")[0] == item.route
                                FloatingSidebarNavItemRow(
                                    item = item,
                                    isSelected = isSelected,
                                    isDark = isDark,
                                    onClick = {
                                        if (item.isActionPortal) {
                                            showBrowserSelector = true
                                        } else {
                                            isExpanded = false
                                            if (!isSelected) {
                                                onNavigate(item.route)
                                            }
                                        }
                                    }
                                )
                            }

                            // 2. EXAM & PRACTICE
                            Text(
                                text = "EXAM & PRACTICE",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                color = if (isDark) Color(0x99818CF8) else Color(0xFF6366F1),
                                letterSpacing = 1.1.sp,
                                modifier = Modifier.padding(start = 6.dp, top = 8.dp, bottom = 4.dp)
                            )
                            practiceItems.forEach { item ->
                                val isSelected = currentRoute.split("?")[0] == item.route
                                FloatingSidebarNavItemRow(
                                    item = item,
                                    isSelected = isSelected,
                                    isDark = isDark,
                                    onClick = {
                                        if (item.isActionPortal) {
                                            showBrowserSelector = true
                                        } else {
                                            isExpanded = false
                                            if (!isSelected) {
                                                onNavigate(item.route)
                                            }
                                        }
                                    }
                                )
                            }

                            // 3. ACADEMIC & TOOLS
                            Text(
                                text = "ACADEMIC & TOOLS",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                color = if (isDark) Color(0x99818CF8) else Color(0xFF6366F1),
                                letterSpacing = 1.1.sp,
                                modifier = Modifier.padding(start = 6.dp, top = 8.dp, bottom = 4.dp)
                            )
                            academicItems.forEach { item ->
                                val isSelected = currentRoute.split("?")[0] == item.route
                                FloatingSidebarNavItemRow(
                                    item = item,
                                    isSelected = isSelected,
                                    isDark = isDark,
                                    onClick = {
                                        if (item.isActionPortal) {
                                            showBrowserSelector = true
                                        } else {
                                            isExpanded = false
                                            if (!isSelected) {
                                                onNavigate(item.route)
                                            }
                                        }
                                    }
                                )
                            }
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 8.dp),
                            color = if (isDark) Color(0x22FFFFFF) else Color(0x40CBD5E1)
                        )

                        // FOOTER ACTIONS: Modern Theme Toggle Capsule & Session Badge
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isDark) Color(0x1FFFFFFF) else Color(0x99FFFFFF),
                                border = BorderStroke(
                                    1.dp,
                                    if (isDark) Color(0x26FFFFFF) else Color(0x4D6366F1)
                                ),
                                modifier = Modifier.clickable {
                                    viewModel.setDarkMode(!isDark)
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isDark) Icons.Default.LightMode else Icons.Default.DarkMode,
                                        contentDescription = "Theme Toggle",
                                        tint = if (isDark) Color(0xFFFBBF24) else Color(0xFF6366F1),
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Text(
                                        if (isDark) "Light Mode" else "Dark Mode",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDark) Color.White else Color(0xFF0F172A)
                                    )
                                }
                            }

                            if (isTimerRunning) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFFEF4444).copy(alpha = 0.15f),
                                    border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.4f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFFEF4444))
                                        )
                                        Text(
                                            "Timer On",
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFEF4444)
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

    if (showProfileDialog) {
        val sidebarContext = androidx.compose.ui.platform.LocalContext.current
        DashboardProfileDialog(
            appPassword = appPassword,
            habitPassword = habitPassword,
            securityQuestion = securityQuestion,
            securityAnswer = securityAnswer,
            onSetSecurityLock = { pass, q, a -> viewModel.setSecurityLock(pass, q, a) },
            onSetAppPassword = { pass -> viewModel.setAppPassword(pass) },
            onSetHabitPassword = { pass -> viewModel.setHabitPassword(pass) },
            isDark = isDark,
            userName = candidateName,
            userClass = candidateClass,
            userAvatarUri = candidateAvatarUri,
            geminiApiKey1 = geminiApiKey1,
            geminiApiKey2 = geminiApiKey2,
            geminiApiKey3 = geminiApiKey3,
            selectedGeminiModel = selectedGeminiModel,
            neetTargetMillis = neetTargetMillis,
            logs = studyLogs,
            tests = mockTests,
            dailyPractices = dailyPractices,
            completedTopics = completedTopics,
            notificationTimes = notificationTimes,
            currentAiProvider = aiProvider,
            onUpdateNotificationTime = { type, hour -> viewModel.updateNotificationTime(type, hour, sidebarContext) },
            onSaveProfile = { name, uClass, avatarUri, k1, k2, k3, model ->
                viewModel.setUserProfile(name, uClass, avatarUri)
                viewModel.saveCustomApiKeys(k1, k2, k3)
                viewModel.setSelectedGeminiModel(model, switchProvider = false)
            },
            onSetDarkMode = { dark -> viewModel.setDarkMode(dark) },
            onResetAllData = { viewModel.resetAllData() },
            onSelectOpenRouterModel = { viewModel.setOpenRouterSelectedModel(it) },
            onSelectGroqModel = { viewModel.setGroqSelectedModel(it) },
            onSelectAiProvider = { viewModel.setAiProvider(it) },
            onDismissRequest = { showProfileDialog = false }
        )
    }
}

@Composable
private fun FloatingSidebarNavItemRow(
    item: SidebarNavItem,
    isSelected: Boolean,
    isDark: Boolean,
    onClick: () -> Unit
) {
    val activeBrush = remember(isDark) {
        if (isDark) {
            Brush.horizontalGradient(
                listOf(
                    Color(0x596366F1),
                    Color(0x338B5CF6),
                    Color(0x1F38BDF8)
                )
            )
        } else {
            Brush.horizontalGradient(
                listOf(
                    Color(0x386366F1),
                    Color(0x248B5CF6),
                    Color(0x1438BDF8)
                )
            )
        }
    }

    val activeBorder = if (isDark) Color(0xFF818CF8).copy(alpha = 0.6f) else Color(0xFF6366F1).copy(alpha = 0.45f)
    val idleBg = if (isDark) Color.Transparent else Color(0x33FFFFFF)
    val idleBorder = if (isDark) Color.Transparent else Color(0x1FCBD5E1)

    val textPrimary = remember(isSelected, isDark) {
        if (isSelected) {
            if (isDark) Color.White else Color(0xFF4338CA)
        } else {
            if (isDark) Color(0xFFF1F5F9) else Color(0xFF0F172A)
        }
    }
    val textSecondary = remember(isDark) {
        if (isDark) Color(0x99FFFFFF) else Color(0xFF64748B)
    }

    val rowShape = remember { RoundedCornerShape(14.dp) }

    Surface(
        shape = rowShape,
        color = Color.Transparent,
        border = BorderStroke(
            1.dp,
            if (isSelected) activeBorder else idleBorder
        ),
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isSelected) Modifier.background(activeBrush, rowShape)
                else Modifier.background(idleBg, rowShape)
            )
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left indicator glow strip for selected item
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .width(3.5.dp)
                        .height(20.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color(0xFF6366F1))
                )
                Spacer(modifier = Modifier.width(6.dp))
            } else {
                Spacer(modifier = Modifier.width(4.dp))
            }

            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (isSelected) {
                            Brush.linearGradient(
                                listOf(Color(0xFF6366F1), Color(0xFF8B5CF6))
                            )
                        } else if (isDark) {
                            Brush.linearGradient(
                                listOf(Color(0x22FFFFFF), Color(0x14FFFFFF))
                            )
                        } else {
                            Brush.linearGradient(
                                listOf(Color(0x80FFFFFF), Color(0x50F1F5F9))
                            )
                        }
                    )
                    .border(
                        0.8.dp,
                        if (isSelected) Color(0x66FFFFFF) else if (isDark) Color(0x1AFFFFFF) else Color(0x33CBD5E1),
                        RoundedCornerShape(10.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = item.title,
                    tint = if (isSelected) Color.White else if (isDark) Color(0xFF94A3B8) else Color(0xFF475569),
                    modifier = Modifier.size(17.dp)
                )
            }

            Spacer(modifier = Modifier.width(9.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    fontSize = 12.sp,
                    fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                    color = textPrimary
                )
                Text(
                    text = item.subtitle,
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = textSecondary,
                    maxLines = 1
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditStudyPortalDialog(
    initialPortal: StudyPortal?,
    isDark: Boolean,
    onDismiss: () -> Unit,
    onSavePortal: (StudyPortal) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var portalUrl by remember(initialPortal) { mutableStateOf(initialPortal?.url ?: "") }
    var portalName by remember(initialPortal) { mutableStateOf(initialPortal?.name ?: "") }
    var portalTag by remember(initialPortal) { mutableStateOf(initialPortal?.tag ?: "Live Batches") }
    var portalEmoji by remember(initialPortal) { mutableStateOf(initialPortal?.iconEmoji ?: "🌐") }

    var isVerifying by remember { mutableStateOf(false) }
    var verificationResult by remember { mutableStateOf<StudyWebVerificationResult?>(null) }
    var verifyError by remember { mutableStateOf<String?>(null) }

    val popularTags = listOf("Live Batches", "Test Series", "Lectures", "Notes", "Self Study", "Reference")
    val popularEmojis = listOf("⚡", "📝", "📚", "🔬", "🧬", "🏛️", "🎯", "🌐", "💻", "📖")

    fun triggerVerification(andSave: Boolean = false) {
        val clean = portalUrl.trim()
        if (clean.isBlank()) {
            verifyError = "Please enter a website URL or link"
            return
        }
        val normalizedUrl = if (!clean.startsWith("http://") && !clean.startsWith("https://")) {
            "https://$clean"
        } else clean

        isVerifying = true
        verifyError = null
        verificationResult = null

        coroutineScope.launch {
            try {
                val res = GeminiChatAssistant.verifyStudyPortalWithGemini(
                    context = context,
                    rawUrl = normalizedUrl,
                    titleHint = portalName.takeIf { it.isNotBlank() }
                )
                isVerifying = false
                res.fold(
                    onSuccess = { result ->
                        verificationResult = result
                        if (result.isStudyWeb) {
                            if (portalName.isBlank() && result.suggestedTitle.isNotBlank()) {
                                portalName = result.suggestedTitle
                            }
                            if (portalEmoji == "🌐" && result.suggestedEmoji.isNotBlank()) {
                                portalEmoji = result.suggestedEmoji
                            }
                            if (portalTag == "Live Batches" && result.suggestedTag.isNotBlank() && result.suggestedTag != "Study Portal") {
                                portalTag = result.suggestedTag
                            }
                            if (andSave) {
                                val savedPortal = StudyPortal(
                                    id = initialPortal?.id ?: java.util.UUID.randomUUID().toString(),
                                    name = portalName.ifBlank { result.suggestedTitle.ifBlank { "Study Web Portal" } },
                                    url = normalizedUrl,
                                    tag = portalTag,
                                    description = normalizedUrl.removePrefix("https://").removePrefix("http://"),
                                    iconEmoji = portalEmoji,
                                    isDefault = initialPortal?.isDefault ?: false,
                                    isVerified = true,
                                    aiVerificationReason = result.reason
                                )
                                onSavePortal(savedPortal)
                                Toast.makeText(context, "🎉 Study Portal verified and saved!", Toast.LENGTH_SHORT).show()
                                onDismiss()
                            }
                        } else {
                            // Blocked!
                            Toast.makeText(context, "🚫 Access Denied: Non-academic website blocked.", Toast.LENGTH_LONG).show()
                        }
                    },
                    onFailure = { err ->
                        verifyError = err.localizedMessage ?: "Verification failed. Please check internet connection."
                    }
                )
            } catch (e: Exception) {
                isVerifying = false
                verifyError = e.localizedMessage ?: "Verification error"
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = if (isDark) Color(0xFF0F172A) else Color.White,
            border = BorderStroke(1.dp, if (isDark) Color(0x4D6366F1) else Color(0xFFCBD5E1)),
            shadowElevation = 24.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
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
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(if (isDark) Color(0x336366F1) else Color(0xFFEEF2FF)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Shield,
                                contentDescription = null,
                                tint = if (isDark) Color(0xFF818CF8) else Color(0xFF4F46E5),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                if (initialPortal != null) "Edit Study Web Portal" else "Add Study Web Portal",
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp,
                                color = if (isDark) Color.White else Color(0xFF0F172A)
                            )
                            Text(
                                "AI-Verified Academic Security Gate",
                                fontSize = 11.sp,
                                color = if (isDark) Color(0x99FFFFFF) else Color(0xFF64748B)
                            )
                        }
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(30.dp)) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close",
                            tint = if (isDark) Color(0x99FFFFFF) else Color(0xFF64748B)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // URL Input Field
                OutlinedTextField(
                    value = portalUrl,
                    onValueChange = {
                        portalUrl = it
                        verificationResult = null
                        verifyError = null
                    },
                    label = { Text("Website Address / URL *", fontSize = 12.sp) },
                    placeholder = { Text("e.g. allen.in or unacademy.com", fontSize = 12.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    trailingIcon = {
                        if (portalUrl.isNotBlank()) {
                            IconButton(onClick = {
                                portalUrl = ""
                                verificationResult = null
                                verifyError = null
                            }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                )

                // Quick URL chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val quickLinks = listOf(
                        "pwthor.live" to "PW Thor",
                        "testbook.com" to "Testbook",
                        "allen.in" to "Allen",
                        "marks.app" to "Marks"
                    )
                    quickLinks.forEach { (domain, label) ->
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isDark) Color(0x226366F1) else Color(0xFFEEF2FF),
                            border = BorderStroke(0.5.dp, if (isDark) Color(0x44818CF8) else Color(0xFFC7D2FE)),
                            modifier = Modifier.clickable {
                                portalUrl = "https://$domain"
                                verificationResult = null
                                verifyError = null
                            }
                        ) {
                            Text(
                                label,
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isDark) Color(0xFFA5B4FC) else Color(0xFF4F46E5),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Portal Name Field (Optional / Auto-suggested)
                OutlinedTextField(
                    value = portalName,
                    onValueChange = { portalName = it },
                    label = { Text("Portal Title (Optional)", fontSize = 12.sp) },
                    placeholder = { Text("Auto-filled by AI if empty", fontSize = 12.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Emoji Icon Selector
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "Portal Icon",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color(0xCCFFFFFF) else Color(0xFF334155)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        popularEmojis.take(7).forEach { emoji ->
                            val isSelected = portalEmoji == emoji
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) {
                                    if (isDark) Color(0xFF4F46E5) else Color(0xFF6366F1)
                                } else {
                                    if (isDark) Color(0x22FFFFFF) else Color(0xFFF1F5F9)
                                },
                                border = if (isSelected) BorderStroke(1.5.dp, Color(0xFF818CF8)) else null,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clickable { portalEmoji = emoji }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(emoji, fontSize = 16.sp)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Tag / Category selector
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "Category Tag",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color(0xCCFFFFFF) else Color(0xFF334155)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        popularTags.take(3).forEach { tag ->
                            val isSelected = portalTag == tag
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) {
                                    Color(0xFF6366F1).copy(alpha = if (isDark) 0.3f else 0.15f)
                                } else {
                                    if (isDark) Color(0x1AFFFFFF) else Color(0xFFF1F5F9)
                                },
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) Color(0xFF818CF8) else Color.Transparent
                                ),
                                modifier = Modifier.clickable { portalTag = tag }
                            ) {
                                Text(
                                    tag,
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) {
                                        if (isDark) Color(0xFFA5B4FC) else Color(0xFF4F46E5)
                                    } else {
                                        if (isDark) Color(0x99FFFFFF) else Color(0xFF64748B)
                                    },
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // AI Verification Status Banner
                AnimatedVisibility(
                    visible = isVerifying || verificationResult != null || verifyError != null,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                        if (isVerifying) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isDark) Color(0x266366F1) else Color(0xFFEEF2FF),
                                border = BorderStroke(1.dp, if (isDark) Color(0x4D818CF8) else Color(0xFFC7D2FE)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp,
                                        color = if (isDark) Color(0xFF818CF8) else Color(0xFF4F46E5)
                                    )
                                    Text(
                                        "🤖 Lakshya AI is verifying academic study authenticity...",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = if (isDark) Color(0xFFA5B4FC) else Color(0xFF4338CA)
                                    )
                                }
                            }
                        } else if (verificationResult != null) {
                            val res = verificationResult!!
                            if (res.isStudyWeb) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isDark) Color(0x2610B981) else Color(0xFFECFDF5),
                                    border = BorderStroke(1.dp, if (isDark) Color(0x4D34D399) else Color(0xFFA7F3D0)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                tint = Color(0xFF10B981),
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Text(
                                                "Approved Academic Study Portal",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                color = if (isDark) Color(0xFF34D399) else Color(0xFF065F46)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            res.reason,
                                            fontSize = 11.sp,
                                            color = if (isDark) Color(0xCCFFFFFF) else Color(0xFF047857)
                                        )
                                    }
                                }
                            } else {
                                // Blocked!
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isDark) Color(0x26EF4444) else Color(0xFFFEF2F2),
                                    border = BorderStroke(1.dp, if (isDark) Color(0x4DF87171) else Color(0xFFFECACA)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Block,
                                                contentDescription = null,
                                                tint = Color(0xFFEF4444),
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Text(
                                                "Access Denied: Blocked Platform",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                color = if (isDark) Color(0xFFF87171) else Color(0xFF991B1B)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            res.reason,
                                            fontSize = 11.sp,
                                            color = if (isDark) Color(0xCCFFFFFF) else Color(0xFFB91C1C)
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            "🛡️ Non-educational / entertainment sites are blocked to keep your study focus intact.",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (isDark) Color(0x99FFFFFF) else Color(0xFF7F1D1D)
                                        )
                                    }
                                }
                            }
                        } else if (verifyError != null) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isDark) Color(0x26F59E0B) else Color(0xFFFFFBEB),
                                border = BorderStroke(1.dp, if (isDark) Color(0x4DFBBF24) else Color(0xFFFDE68A)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = Color(0xFFF59E0B),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        verifyError!!,
                                        fontSize = 11.5.sp,
                                        color = if (isDark) Color(0xFFFCD34D) else Color(0xFF92400E)
                                    )
                                }
                            }
                        }
                    }
                }

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cancel")
                    }

                    if (verificationResult != null && !verificationResult!!.isStudyWeb) {
                        // Blocked Button State
                        Button(
                            onClick = { /* Disabled */ },
                            enabled = false,
                            modifier = Modifier.weight(1.5f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                disabledContainerColor = Color(0xFFEF4444).copy(alpha = 0.3f),
                                disabledContentColor = Color.White
                            )
                        ) {
                            Icon(Icons.Default.Block, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Blocked by AI", fontSize = 13.sp)
                        }
                    } else if (verificationResult != null && verificationResult!!.isStudyWeb) {
                        // Approved Save Button
                        Button(
                            onClick = {
                                val clean = portalUrl.trim()
                                val normalizedUrl = if (!clean.startsWith("http://") && !clean.startsWith("https://")) {
                                    "https://$clean"
                                } else clean
                                val savedPortal = StudyPortal(
                                    id = initialPortal?.id ?: java.util.UUID.randomUUID().toString(),
                                    name = portalName.ifBlank { verificationResult?.suggestedTitle?.ifBlank { "Study Web Portal" } ?: "Study Web Portal" },
                                    url = normalizedUrl,
                                    tag = portalTag,
                                    description = normalizedUrl.removePrefix("https://").removePrefix("http://"),
                                    iconEmoji = portalEmoji,
                                    isDefault = initialPortal?.isDefault ?: false,
                                    isVerified = true,
                                    aiVerificationReason = verificationResult?.reason ?: "Verified by Lakshya AI"
                                )
                                onSavePortal(savedPortal)
                                Toast.makeText(context, "🎉 Study Portal Saved!", Toast.LENGTH_SHORT).show()
                                onDismiss()
                            },
                            modifier = Modifier.weight(1.5f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF10B981)
                            )
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (initialPortal != null) "Update Portal" else "Save Portal", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        // Verify & Add Button
                        Button(
                            onClick = { triggerVerification(andSave = true) },
                            enabled = portalUrl.isNotBlank() && !isVerifying,
                            modifier = Modifier.weight(1.5f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isDark) Color(0xFF6366F1) else Color(0xFF4F46E5)
                            )
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (initialPortal != null) "Verify & Update" else "Verify & Add", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
