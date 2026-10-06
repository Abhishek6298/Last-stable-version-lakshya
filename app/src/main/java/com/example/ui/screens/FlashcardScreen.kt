@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
package com.example.ui.screens

import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import com.example.ui.components.AppSegmentedControl
import com.example.ui.components.FireworksProgressBar
import com.example.ui.components.NativeMarkdownText
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.FlashcardDeck
import com.example.data.FlashcardItem
import com.example.data.GeminiFlashcardGenerator
import com.example.data.GeneratedCard
import com.example.ui.AppViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

// System-inspired modern vibrant color palette
private val IosBlue = Color(0xFF007AFF)
private val IosGreen = Color(0xFF34C759)
private val IosRed = Color(0xFFFF3B30)
private val IosOrange = Color(0xFFFF9500)
private val IosPurple = Color(0xFFAF52DE)
private val IosPink = Color(0xFFFF2D55)
private val IosCyan = Color(0xFF30B0C7)

/**
 * High-tech iconic AI PDF Logo Badge
 */
@Composable
fun AiPdfLogoBadge(
    modifier: Modifier = Modifier,
    badgeSize: androidx.compose.ui.unit.Dp = 44.dp,
    showGlow: Boolean = true,
    isDark: Boolean = true
) {
    Box(
        modifier = modifier.size(badgeSize),
        contentAlignment = Alignment.Center
    ) {
        if (showGlow) {
            // Ambient glow halo
            Box(
                modifier = Modifier
                    .size(badgeSize * 1.15f)
                    .clip(RoundedCornerShape(badgeSize * 0.38f))
                    .background(
                        Brush.radialGradient(
                            listOf(
                                Color(0xFF8B5CF6).copy(alpha = 0.45f),
                                Color(0xFF10B981).copy(alpha = 0.2f),
                                Color.Transparent
                            )
                        )
                    )
            )
        }

        // Main Icon Container
        Box(
            modifier = Modifier
                .size(badgeSize)
                .clip(RoundedCornerShape(badgeSize * 0.32f))
                .background(
                    Brush.linearGradient(
                        listOf(
                            Color(0xFF7C3AED),
                            Color(0xFF4F46E5),
                            Color(0xFF059669)
                        )
                    )
                )
                .border(
                    BorderStroke(
                        1.2.dp,
                        Brush.linearGradient(
                            listOf(
                                Color(0xFFDDD6FE),
                                Color(0xFF8B5CF6),
                                Color(0xFF34D399)
                            )
                        )
                    ),
                    RoundedCornerShape(badgeSize * 0.32f)
                )
                .shadow(6.dp, RoundedCornerShape(badgeSize * 0.32f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.PictureAsPdf,
                contentDescription = "AI PDF",
                tint = Color.White,
                modifier = Modifier.size(badgeSize * 0.52f)
            )

            // Sparkle overlay badge in top-right corner
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 2.dp, y = (-2).dp)
                    .size(badgeSize * 0.42f)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFFF59E0B), Color(0xFFEF4444))
                        )
                    )
                    .border(0.8.dp, Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(badgeSize * 0.24f)
                )
            }
        }
    }
}

@Composable
fun FlashcardScreen(
    viewModel: AppViewModel,
    modifier: Modifier = Modifier,
    onNavigateBack: () -> Unit = {}
) {
    val isDarkMode by viewModel.isDarkMode.collectAsStateWithLifecycle()
    val isDark = isDarkMode
    val decks by viewModel.flashcardDecks.collectAsState()
    val allCards by viewModel.flashcardItems.collectAsState()

    var selectedDeckId by remember { mutableStateOf<Long?>(null) }
    var studyModeActive by remember { mutableStateOf(false) }
    var showNewDeckModal by remember { mutableStateOf(false) }
    var showPdfAiModal by remember { mutableStateOf(false) }

    // Intercept back presses when inside a deck, study mode, or dialog, or navigate back to dashboard
    BackHandler(enabled = true) {
        when {
            showPdfAiModal -> showPdfAiModal = false
            showNewDeckModal -> showNewDeckModal = false
            studyModeActive -> studyModeActive = false
            selectedDeckId != null -> selectedDeckId = null
            else -> onNavigateBack()
        }
    }

    val selectedDeck = remember(decks, selectedDeckId) {
        decks.find { it.id == selectedDeckId }
    }

    val deckCards = remember(allCards, selectedDeckId) {
        if (selectedDeckId == null) emptyList()
        else allCards.filter { it.deckId == selectedDeckId }
    }

    val todayStr = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }

    val dueCards = remember(deckCards, todayStr) {
        deckCards.filter { it.due == null || it.due <= todayStr }
    }

    val studiedTodayCount = remember(deckCards, todayStr) {
        deckCards.count { it.lastStudied == todayStr }
    }

    val masteredCount = remember(deckCards) {
        deckCards.count { it.interval > 21 }
    }

    val bgGradient = if (isDark) {
        Brush.verticalGradient(
            listOf(
                Color(0xFF090D16),
                Color(0xFF0D111F),
                Color(0xFF080B14)
            )
        )
    } else {
        Brush.verticalGradient(
            listOf(
                Color(0xFFF8FAFC),
                Color(0xFFF1F5F9),
                Color(0xFFE2E8F0)
            )
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        if (studyModeActive && selectedDeck != null) {
            StudyModeView(
                deck = selectedDeck,
                initialCards = if (dueCards.isNotEmpty()) dueCards else deckCards,
                onExitStudy = { studyModeActive = false },
                onUpdateCard = { updatedCard -> viewModel.updateFlashcardItem(updatedCard) },
                isDark = isDark
            )
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header & Decks Selector Bar
                TopDeckSelectorBar(
                    decks = decks,
                    allCards = allCards,
                    selectedDeckId = selectedDeckId,
                    todayStr = todayStr,
                    onSelectDeck = { selectedDeckId = it },
                    onOpenNewDeckModal = { showNewDeckModal = true },
                    onOpenPdfAiModal = { showPdfAiModal = true },
                    onImportPreMadeDecks = { viewModel.importAllPreMadeDecks() },
                    onNavigateBack = onNavigateBack,
                    isDark = isDark
                )

                AnimatedContent(
                    targetState = selectedDeck == null,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
                    },
                    label = "deckViewTransition"
                ) { isNoDeckSelected ->
                    if (isNoDeckSelected) {
                        AllDecksDashboardView(
                            decks = decks,
                            allCards = allCards,
                            todayStr = todayStr,
                            onSelectDeck = { selectedDeckId = it },
                            onNewDeckClick = { showNewDeckModal = true },
                            onOpenPdfAiModal = { showPdfAiModal = true },
                            onImportPreMadeDecks = { viewModel.importAllPreMadeDecks() },
                            isDark = isDark
                        )
                    } else {
                        selectedDeck?.let { deck ->
                            DeckDetailView(
                                deck = deck,
                                cards = deckCards,
                                dueCards = dueCards,
                                studiedTodayCount = studiedTodayCount,
                                masteredCount = masteredCount,
                                onStartStudy = { studyModeActive = true },
                                onAddCard = { front, back ->
                                    viewModel.addFlashcardItem(deck.id, front, back)
                                },
                                onUpdateCard = { updatedCard ->
                                    viewModel.updateFlashcardItem(updatedCard)
                                },
                                onDeleteCard = { cardId ->
                                    viewModel.deleteFlashcardItem(cardId)
                                },
                                onUpdateDeck = { id, name, icon ->
                                    viewModel.updateFlashcardDeck(id, name, icon, deck.category)
                                },
                                onDeleteDeck = {
                                    viewModel.deleteFlashcardDeck(deck.id)
                                    selectedDeckId = null
                                },
                                isDark = isDark
                            )
                        }
                    }
                }
            }
        }

        if (showNewDeckModal) {
            NewDeckModal(
                onDismiss = { showNewDeckModal = false },
                onCreateDeck = { name, icon ->
                    viewModel.addFlashcardDeck(name, icon)
                    showNewDeckModal = false
                },
                isDark = isDark
            )
        }

        if (showPdfAiModal) {
            PdfToFlashcardModal(
                existingDecks = decks,
                onDismiss = { showPdfAiModal = false },
                onCreateDeckWithCards = { name, category, icon, cards ->
                    viewModel.createDeckWithGeneratedCards(name, category, icon, cards)
                    showPdfAiModal = false
                },
                onAddCardsToDeck = { deckId, cards ->
                    viewModel.addGeneratedCardsToDeck(deckId, cards)
                    showPdfAiModal = false
                },
                isDark = isDark
            )
        }
    }
}

@Composable
fun TopDeckSelectorBar(
    decks: List<FlashcardDeck>,
    allCards: List<FlashcardItem>,
    selectedDeckId: Long?,
    todayStr: String,
    onSelectDeck: (Long?) -> Unit,
    onOpenNewDeckModal: () -> Unit,
    onOpenPdfAiModal: () -> Unit,
    onImportPreMadeDecks: () -> Unit,
    onNavigateBack: () -> Unit = {},
    isDark: Boolean
) {
    val barBg = if (isDark) Color(0xF00F1422) else Color(0xF0FFFFFF)
    val borderColor = if (isDark) Color(0x338B5CF6) else Color(0x1A000000)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                0.8.dp,
                borderColor,
                RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)
            ),
        color = barBg,
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    IconButton(
                        onClick = {
                            if (selectedDeckId != null) {
                                onSelectDeck(null)
                            } else {
                                onNavigateBack()
                            }
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (isDark) Color(0x1FFFFFFF) else Color(0x0A000000))
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = if (isDark) Color.White else Color(0xFF1E293B),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFF8B5CF6), Color(0xFF3B82F6))
                                )
                            )
                            .border(1.dp, Color(0xFFC4B5FD), RoundedCornerShape(12.dp))
                            .shadow(3.dp, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🎴", fontSize = 18.sp)
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Flashcard Studio",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Black,
                            color = if (isDark) Color.White else Color(0xFF1E293B),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF10B981))
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "SM-2 Repetition",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isDark) Color(0x99FFFFFF) else Color(0xFF64748B)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Enhanced AI PDF Pill Action Button
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.Transparent,
                        border = BorderStroke(
                            1.dp,
                            Brush.linearGradient(
                                listOf(Color(0xFF8B5CF6), Color(0xFF10B981))
                            )
                        ),
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onOpenPdfAiModal() }
                            .testTag("pdf_ai_button")
                    ) {
                        Row(
                            modifier = Modifier
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(
                                            Color(0xFF7C3AED).copy(alpha = 0.25f),
                                            Color(0xFF059669).copy(alpha = 0.2f)
                                        )
                                    )
                                )
                                .padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            AiPdfLogoBadge(badgeSize = 20.dp, showGlow = false, isDark = isDark)
                            Text(
                                text = "AI PDF",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isDark) Color(0xFFE2E8F0) else Color(0xFF1E293B)
                            )
                        }
                    }

                    // + Deck Button
                    Button(
                        onClick = onOpenNewDeckModal,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                        contentPadding = PaddingValues(horizontal = 9.dp, vertical = 5.dp),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
                        modifier = Modifier
                            .heightIn(min = 34.dp)
                            .testTag("new_deck_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(15.dp), tint = Color.White)
                        Spacer(modifier = Modifier.width(2.dp))
                        Text("DECK", fontSize = 11.sp, fontWeight = FontWeight.Black, color = Color.White)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Decks horizontal list chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                item {
                    val isAllSelected = selectedDeckId == null
                    val pillBg = if (isAllSelected) {
                        Brush.horizontalGradient(listOf(Color(0xFF7C3AED), Color(0xFF2563EB)))
                    } else {
                        Brush.horizontalGradient(
                            listOf(
                                if (isDark) Color(0x1AFFFFFF) else Color(0x0F000000),
                                if (isDark) Color(0x1AFFFFFF) else Color(0x0F000000)
                            )
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(pillBg)
                            .border(
                                1.dp,
                                if (isAllSelected) Color.Transparent else if (isDark) Color(0x33FFFFFF) else Color(0x1F000000),
                                CircleShape
                            )
                            .clickable { onSelectDeck(null) }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "✨ All Decks (${decks.size})",
                                fontSize = 13.sp,
                                fontWeight = if (isAllSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                color = if (isAllSelected) Color.White else if (isDark) Color.White else Color(0xFF3C3C43)
                            )
                        }
                    }
                }

                items(decks, key = { it.id }) { deck ->
                    val isSelected = deck.id == selectedDeckId
                    val deckCards = allCards.filter { it.deckId == deck.id }
                    val deckDue = deckCards.count { it.due == null || it.due <= todayStr }

                    val itemBg = if (isSelected) {
                        Brush.horizontalGradient(listOf(Color(0xFF3B82F6), Color(0xFF1D4ED8)))
                    } else {
                        Brush.horizontalGradient(
                            listOf(
                                if (isDark) Color(0x1FFFFFFF) else Color(0x0A000000),
                                if (isDark) Color(0x14FFFFFF) else Color(0x0A000000)
                            )
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(itemBg)
                            .border(
                                1.dp,
                                if (isSelected) Color.Transparent else if (isDark) Color(0x29FFFFFF) else Color(0x1A000000),
                                RoundedCornerShape(14.dp)
                            )
                            .clickable { onSelectDeck(deck.id) }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(deck.icon, fontSize = 15.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = deck.name,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else if (isDark) Color.White else Color(0xFF3C3C43)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "(${deckCards.size})",
                                fontSize = 11.sp,
                                color = if (isSelected) Color(0xCCFFFFFF) else if (isDark) Color(0x80FFFFFF) else Color(0x803C3C43)
                            )

                            if (deckDue > 0) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(if (isSelected) Color.White else IosRed)
                                        .padding(horizontal = 6.dp, vertical = 2.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "$deckDue",
                                        color = if (isSelected) IosRed else Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.ExtraBold
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

@Composable
fun AllDecksDashboardView(
    decks: List<FlashcardDeck>,
    allCards: List<FlashcardItem>,
    todayStr: String,
    onSelectDeck: (Long) -> Unit,
    onNewDeckClick: () -> Unit,
    onOpenPdfAiModal: () -> Unit,
    onImportPreMadeDecks: () -> Unit,
    isDark: Boolean
) {
    val totalCards = allCards.size
    val dueToday = allCards.count { it.due == null || it.due <= todayStr }
    val studiedToday = allCards.count { it.lastStudied == todayStr }
    val mastered = allCards.count { it.interval > 21 }

    var selectedCategoryFilter by remember { mutableStateOf("All") }

    val filteredDecks = remember(decks, selectedCategoryFilter) {
        if (selectedCategoryFilter == "All") {
            decks
        } else {
            decks.filter { deck ->
                deck.category.equals(selectedCategoryFilter, ignoreCase = true) ||
                deck.name.contains(selectedCategoryFilter, ignoreCase = true)
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // High-Tech AI PDF Studio Hero Banner
        item {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = if (isDark) Color(0xFF131828) else Color.White,
                shadowElevation = 6.dp,
                border = BorderStroke(
                    1.2.dp,
                    Brush.horizontalGradient(
                        listOf(
                            Color(0xFF8B5CF6).copy(alpha = 0.6f),
                            Color(0xFF3B82F6).copy(alpha = 0.5f),
                            Color(0xFF10B981).copy(alpha = 0.6f)
                        )
                    )
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .clickable { onOpenPdfAiModal() }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    if (isDark) Color(0xFF1E1B4B).copy(alpha = 0.45f) else Color(0xFFEEF2FF),
                                    if (isDark) Color(0xFF0F172A).copy(alpha = 0.45f) else Color(0xFFF8FAFC)
                                )
                            )
                        )
                        .padding(18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        AiPdfLogoBadge(badgeSize = 52.dp, isDark = isDark)

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "AI PDF Flashcard Studio",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                color = if (isDark) Color.White else Color(0xFF0F172A)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(
                                            Brush.horizontalGradient(
                                                listOf(Color(0xFF8B5CF6), Color(0xFF6366F1))
                                            )
                                        )
                                        .padding(horizontal = 7.dp, vertical = 2.dp)
                                ) {
                                    Text("LAKSHYA AI", fontSize = 9.sp, fontWeight = FontWeight.Black, color = Color.White)
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Auto-extract high-yield SM-2 decks",
                                    fontSize = 11.5.sp,
                                    color = if (isDark) Color(0xCCFFFFFF) else Color(0xFF475569),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Upload notes & coaching PDFs to generate flashcards instantly.",
                                fontSize = 11.sp,
                                color = if (isDark) Color(0x99FFFFFF) else Color(0xFF64748B),
                                lineHeight = 15.sp
                            )
                        }

                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Open AI PDF",
                            tint = if (isDark) Color(0xFFC4B5FD) else Color(0xFF6366F1),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // Quick Overview Header Stats Card
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDark) Color(0xFF131828) else Color.White
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        if (isDark) Color(0x33FFFFFF) else Color(0x1F000000),
                        RoundedCornerShape(24.dp)
                    )
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "STUDY METRICS",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF3B82F6),
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Overall Progress",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Black,
                                color = if (isDark) Color.White else Color(0xFF1E293B)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color(0xFF3B82F6).copy(alpha = 0.12f))
                                .border(0.8.dp, Color(0xFF3B82F6).copy(alpha = 0.3f), CircleShape)
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "${decks.size} Decks Active",
                                color = Color(0xFF3B82F6),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatCard(count = totalCards, label = "Total", icon = "📚", color = IosBlue, isDark = isDark, modifier = Modifier.weight(1f))
                        StatCard(count = dueToday, label = "Due Now", icon = "⏰", color = IosRed, isDark = isDark, modifier = Modifier.weight(1f))
                        StatCard(count = studiedToday, label = "Today", icon = "✅", color = IosGreen, isDark = isDark, modifier = Modifier.weight(1f))
                        StatCard(count = mastered, label = "Mastered", icon = "🌟", color = IosPurple, isDark = isDark, modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        item {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "YOUR DECKS",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isDark) Color(0x99FFFFFF) else Color(0xFF64748B),
                        letterSpacing = 0.8.sp
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        TextButton(onClick = onImportPreMadeDecks) {
                            Text("⚡ Starter Decks", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF8B5CF6))
                        }

                        TextButton(onClick = onNewDeckClick) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(15.dp), tint = IosBlue)
                                Text("New Deck", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = IosBlue)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Subject Category Filter Chips
                val filterCategories = listOf("All", "Physics", "Chemistry", "Biology", "Other")

                AppSegmentedControl(
                    items = filterCategories,
                    selectedItem = selectedCategoryFilter,
                    onItemSelected = { selectedCategoryFilter = it },
                    itemLabel = { it },
                    itemEmoji = {
                        when (it) {
                            "All" -> "✨"
                            "Physics" -> "⚡"
                            "Chemistry" -> "🧪"
                            "Biology" -> "🧬"
                            else -> "📖"
                        }
                    },
                    selectedColor = when (selectedCategoryFilter) {
                        "Physics" -> Color(0xFF2563EB)
                        "Chemistry" -> Color(0xFFD97706)
                        "Biology" -> Color(0xFF10B981)
                        "Other" -> Color(0xFF8B5CF6)
                        else -> Color(0xFF6D28D9)
                    },
                    selectedGradient = when (selectedCategoryFilter) {
                        "Physics" -> listOf(Color(0xFF3B82F6), Color(0xFF2563EB))
                        "Chemistry" -> listOf(Color(0xFFF59E0B), Color(0xFFD97706))
                        "Biology" -> listOf(Color(0xFF34D399), Color(0xFF10B981))
                        "Other" -> listOf(Color(0xFFA855F7), Color(0xFF8B5CF6))
                        else -> listOf(Color(0xFF7C3AED), Color(0xFF6D28D9))
                    },
                    isDark = isDark,
                    fontSize = 12.sp,
                    cornerRadius = 14.dp
                )
            }
        }

        if (filteredDecks.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDark) Color(0xFF171A28) else Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                        .border(1.dp, if (isDark) Color(0x29FFFFFF) else Color(0x1F000000), RoundedCornerShape(22.dp))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("🃏", fontSize = 48.sp)
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = if (selectedCategoryFilter == "All") "No Decks Created Yet" else "No $selectedCategoryFilter Decks Found",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color.White else Color(0xFF1C1C1E)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (selectedCategoryFilter == "All")
                                "Create your first flashcard deck or convert notes with AI PDF!"
                            else
                                "Tap '+ New Deck' and select $selectedCategoryFilter category to add one!",
                            fontSize = 13.sp,
                            color = if (isDark) Color(0x99FFFFFF) else Color(0x993C3C43),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = onOpenPdfAiModal,
                                shape = CircleShape,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6))
                            ) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("AI PDF Generator", fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick = onNewDeckClick,
                                shape = CircleShape,
                                colors = ButtonDefaults.buttonColors(containerColor = IosBlue),
                                modifier = Modifier.testTag("create_first_deck_button")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("New Deck", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        } else {
            items(filteredDecks, key = { it.id }) { deck ->
                val deckCards = allCards.filter { it.deckId == deck.id }
                val dueCardsCount = deckCards.count { it.due == null || it.due <= todayStr }
                val masteredInDeck = deckCards.count { it.interval > 21 }

                DeckTileCard(
                    deck = deck,
                    totalCards = deckCards.size,
                    dueCardsCount = dueCardsCount,
                    masteredCount = masteredInDeck,
                    onClick = { onSelectDeck(deck.id) },
                    isDark = isDark
                )
            }
        }

        item {
            com.example.ui.components.AppBrandingFooter(isDark = isDark)
        }
    }
}

@Composable
fun DeckTileCard(
    deck: FlashcardDeck,
    totalCards: Int,
    dueCardsCount: Int,
    masteredCount: Int,
    onClick: () -> Unit,
    isDark: Boolean
) {
    val categoryColor = when (deck.category) {
        "Physics" -> Color(0xFF3B82F6)
        "Chemistry" -> Color(0xFFF59E0B)
        "Biology" -> Color(0xFF10B981)
        else -> Color(0xFF8B5CF6)
    }

    val categoryGradient = when (deck.category) {
        "Physics" -> listOf(Color(0xFF3B82F6), Color(0xFF1D4ED8))
        "Chemistry" -> listOf(Color(0xFFF59E0B), Color(0xFFD97706))
        "Biology" -> listOf(Color(0xFF34D399), Color(0xFF059669))
        else -> listOf(Color(0xFFA855F7), Color(0xFF7C3AED))
    }

    val cardBg = if (isDark) Color(0xFF131826) else Color.White
    val borderCol = if (isDark) Color(0x33FFFFFF) else Color(0x1F000000)

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = cardBg,
        shadowElevation = 4.dp,
        modifier = Modifier
            .fillMaxWidth()
            .border(
                BorderStroke(
                    1.dp,
                    if (isDark) categoryColor.copy(alpha = 0.35f) else Color(0x1F000000)
                ),
                RoundedCornerShape(22.dp)
            )
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Category & Status Badge Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Category Chip
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(categoryColor.copy(alpha = 0.15f))
                        .border(0.8.dp, categoryColor.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = deck.category.uppercase(),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = categoryColor,
                        letterSpacing = 0.8.sp
                    )
                }

                if (dueCardsCount > 0) {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Brush.horizontalGradient(listOf(IosRed, Color(0xFFDC2626))))
                            .shadow(2.dp, CircleShape)
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(Color.White)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "$dueCardsCount Due",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(IosGreen.copy(alpha = 0.15f))
                            .border(0.8.dp, IosGreen.copy(alpha = 0.3f), CircleShape)
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Up to date ✓",
                            color = IosGreen,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Main Deck Info Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 3D Stack Icon Container
                Box(
                    modifier = Modifier.size(52.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Back card offset preview
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .offset(x = 4.dp, y = (-3).dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(categoryColor.copy(alpha = 0.2f))
                    )
                    // Front card icon container
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Brush.linearGradient(categoryGradient))
                            .shadow(4.dp, RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(deck.icon, fontSize = 24.sp)
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = deck.name,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isDark) Color.White else Color(0xFF1F2937),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "$totalCards Cards  •  $masteredCount Mastered",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isDark) Color(0x99FFFFFF) else Color(0xFF6B7280)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isDark) Color(0x18FFFFFF) else Color(0x0C000000)
                        ) {
                            Text("🧠 Active Recall", fontSize = 8.sp, fontWeight = FontWeight.SemiBold, color = if (isDark) Color(0x99FFFFFF) else Color(0xFF6B7280), modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp), softWrap = false)
                        }
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isDark) Color(0x18FFFFFF) else Color(0x0C000000)
                        ) {
                            Text("🔁 Spaced Repetition", fontSize = 8.sp, fontWeight = FontWeight.SemiBold, color = if (isDark) Color(0x99FFFFFF) else Color(0xFF6B7280), modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp), softWrap = false)
                        }
                    }
                }

                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Open Deck",
                    tint = if (isDark) Color(0x66FFFFFF) else Color(0xFF9CA3AF),
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Progress Bar & Percentage Readout
            val pct = if (totalCards > 0) ((masteredCount.toFloat() / totalCards) * 100).toInt() else 0
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Retention Rate",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isDark) Color(0x80FFFFFF) else Color(0xFF9CA3AF)
                )
                Text(
                    text = "$pct%",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = categoryColor
                )
            }
            Spacer(modifier = Modifier.height(3.dp))
            val masteredProg = if (totalCards > 0) masteredCount.toFloat() / totalCards else 0f
            FireworksProgressBar(
                progress = masteredProg,
                height = 6.dp,
                gradientColors = listOf(categoryColor, categoryColor.copy(alpha = 0.7f)),
                sparkColor = categoryColor,
                isDark = isDark
            )
        }
    }
}

@Composable
fun DeckDetailView(
    deck: FlashcardDeck,
    cards: List<FlashcardItem>,
    dueCards: List<FlashcardItem>,
    studiedTodayCount: Int,
    masteredCount: Int,
    onStartStudy: () -> Unit,
    onAddCard: (String, String) -> Unit,
    onUpdateCard: (FlashcardItem) -> Unit,
    onDeleteCard: (Long) -> Unit,
    onUpdateDeck: (Long, String, String) -> Unit,
    onDeleteDeck: () -> Unit,
    isDark: Boolean
) {
    var frontText by remember { mutableStateOf("") }
    var backText by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }
    var showEditDeckModal by remember { mutableStateOf(false) }

    val filteredCards = remember(cards, searchQuery) {
        if (searchQuery.isBlank()) cards
        else cards.filter {
            it.front.contains(searchQuery, ignoreCase = true) ||
            it.back.contains(searchQuery, ignoreCase = true)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Deck Hero Card
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDark) Color(0xFF161A2B) else Color.White
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        if (isDark) Color(0x33FFFFFF) else Color(0x1F000000),
                        RoundedCornerShape(24.dp)
                    )
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(
                                        Brush.linearGradient(
                                            listOf(IosBlue, Color(0xFF5856D6))
                                        )
                                    )
                                    .shadow(4.dp, RoundedCornerShape(16.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(deck.icon, fontSize = 28.sp)
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = deck.name,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isDark) Color.White else Color(0xFF1C1C1E)
                                )
                                Text(
                                    text = "${cards.size} total cards · ${dueCards.size} due for review",
                                    fontSize = 12.sp,
                                    color = if (isDark) Color(0x99FFFFFF) else Color(0x993C3C43)
                                )
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            IconButton(onClick = { showEditDeckModal = true }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit Deck", tint = IosOrange)
                            }

                            IconButton(onClick = onDeleteDeck) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete Deck", tint = IosRed)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Primary Action Button Banner
                    if (dueCards.isNotEmpty()) {
                        Button(
                            onClick = onStartStudy,
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(containerColor = IosBlue),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 46.dp)
                                .testTag("study_now_button"),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Start Reviewing (${dueCards.size} Due Now) →", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                        }
                    } else if (cards.isNotEmpty()) {
                        Button(
                            onClick = onStartStudy,
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(containerColor = IosGreen),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 46.dp)
                                .testTag("review_all_button")
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("All Caught Up! Practice All (${cards.size}) ✓", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }
            }
        }

        // Stats 4-Grid Card
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatCard(count = cards.size, label = "Total", icon = "🎴", color = IosBlue, isDark = isDark, modifier = Modifier.weight(1f))
                StatCard(count = dueCards.size, label = "Due Now", icon = "⏰", color = IosRed, isDark = isDark, modifier = Modifier.weight(1f))
                StatCard(count = studiedTodayCount, label = "Done Today", icon = "✅", color = IosGreen, isDark = isDark, modifier = Modifier.weight(1f))
                StatCard(count = masteredCount, label = "Mastered", icon = "🌟", color = IosPurple, isDark = isDark, modifier = Modifier.weight(1f))
            }
        }

        // Add Card Form
        item {
            val cardBg = if (isDark) Color(0xFF1A1D2B) else Color.White
            val borderCol = if (isDark) Color(0x33FFFFFF) else Color(0x1F000000)

            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, borderCol, RoundedCornerShape(20.dp))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "➕ ADD NEW FLASHCARD",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = IosBlue,
                            letterSpacing = 0.8.sp
                        )

                        Text(
                            text = "Type question & answer",
                            fontSize = 11.sp,
                            color = if (isDark) Color(0x80FFFFFF) else Color(0x803C3C43)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Column {
                            Text("FRONT (Question / Prompt)", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = IosBlue)
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = frontText,
                                onValueChange = { frontText = it },
                                placeholder = { Text("e.g. Speed of Light formula in vacuum", fontSize = 13.sp, fontFamily = FontFamily.Monospace) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("front_text_input"),
                                shape = RoundedCornerShape(12.dp),
                                textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace, fontSize = 13.sp)
                            )
                        }

                        Column {
                            Text("BACK (Answer / Solution)", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = IosGreen)
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = backText,
                                onValueChange = { backText = it },
                                placeholder = { Text("e.g. c = 3 x 10^8 m/s", fontSize = 13.sp, fontFamily = FontFamily.Monospace) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("back_text_input"),
                                shape = RoundedCornerShape(12.dp),
                                textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace, fontSize = 13.sp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    AlignRight {
                        Button(
                            onClick = {
                                if (frontText.isNotBlank() && backText.isNotBlank()) {
                                    onAddCard(frontText.trim(), backText.trim())
                                    frontText = ""
                                    backText = ""
                                }
                            },
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(containerColor = IosBlue),
                            enabled = frontText.isNotBlank() && backText.isNotBlank(),
                            modifier = Modifier.testTag("add_card_submit")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add Card to Deck", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }

        // Search & Filter Bar
        if (cards.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "CARDS IN DECK (${filteredCards.size}/${cards.size})",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isDark) Color(0x99FFFFFF) else Color(0x993C3C43),
                        letterSpacing = 0.5.sp
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search cards...", fontSize = 13.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = if (isDark) Color(0x80FFFFFF) else Color(0x803C3C43)) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear search", modifier = Modifier.size(18.dp))
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true
                )
            }
        }

        items(filteredCards, key = { it.id }) { card ->
            CardRowItem(
                card = card,
                onUpdateCard = onUpdateCard,
                onDelete = { onDeleteCard(card.id) },
                isDark = isDark
            )
        }

        item {
            com.example.ui.components.AppBrandingFooter(isDark = isDark)
        }
    }

    if (showEditDeckModal) {
        EditDeckModal(
            deck = deck,
            onDismiss = { showEditDeckModal = false },
            onSaveDeck = { name, icon ->
                onUpdateDeck(deck.id, name, icon)
                showEditDeckModal = false
            },
            isDark = isDark
        )
    }
}

@Composable
fun AlignRight(content: @Composable () -> Unit) {
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
        content()
    }
}

@Composable
fun StatCard(count: Int, label: String, icon: String, color: Color, isDark: Boolean, modifier: Modifier = Modifier) {
    val cardBg = if (isDark) Color(0xFF1B1E2E) else Color(0xFFF7F9FF)
    val borderCol = if (isDark) Color(0x29FFFFFF) else Color(0x1F000000)

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = cardBg,
        modifier = modifier
            .border(1.dp, borderCol, RoundedCornerShape(16.dp))
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = icon, fontSize = 15.sp)
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "$count",
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = color,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label.uppercase(),
                fontSize = 8.5.sp,
                fontWeight = FontWeight.Bold,
                color = if (isDark) Color(0x99FFFFFF) else Color(0x993C3C43),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun CardRowItem(
    card: FlashcardItem,
    onUpdateCard: (FlashcardItem) -> Unit,
    onDelete: () -> Unit,
    isDark: Boolean
) {
    val cardBg = if (isDark) Color(0xFF181B28) else Color.White
    val borderCol = if (isDark) Color(0x24FFFFFF) else Color(0x1A000000)
    var showEditCardModal by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = cardBg,
        shadowElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, borderCol, RoundedCornerShape(16.dp))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Q:", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = IosBlue)
                NativeMarkdownText(
                    text = card.front,
                    isDark = isDark,
                    fontSize = 13.sp,
                    textColor = if (isDark) Color.White else Color(0xFF1C1C1E)
                )
            }

            Box(
                modifier = Modifier
                    .padding(horizontal = 6.dp)
                    .width(1.dp)
                    .heightIn(min = 42.dp)
                    .background(if (isDark) Color(0x29FFFFFF) else Color(0x1F000000))
            )

            Column(modifier = Modifier.weight(1.2f)) {
                Text("A:", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = IosGreen)
                NativeMarkdownText(
                    text = card.back,
                    isDark = isDark,
                    fontSize = 13.sp,
                    textColor = if (isDark) Color.White else Color(0xFF1C1C1E)
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Status Badge
            val todayStr = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }
            val (badgeText, badgeColor) = when {
                card.reviews == 0 -> "New" to IosPurple
                card.due == null || card.due <= todayStr -> "Due" to IosRed
                card.interval > 21 -> "Mastered" to IosGreen
                else -> "+${card.interval}d" to IosCyan
            }

            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(badgeColor.copy(alpha = 0.15f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(badgeText, color = badgeColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.width(4.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                IconButton(onClick = { showEditCardModal = true }, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit Card", tint = IosOrange, modifier = Modifier.size(15.dp))
                }

                IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Delete Card", tint = IosRed, modifier = Modifier.size(15.dp))
                }
            }
        }
    }

    if (showEditCardModal) {
        EditCardModal(
            card = card,
            onDismiss = { showEditCardModal = false },
            onSaveCard = { front, back ->
                onUpdateCard(card.copy(front = front, back = back))
                showEditCardModal = false
            },
            isDark = isDark
        )
    }
}

@Composable
fun StudyModeView(
    deck: FlashcardDeck,
    initialCards: List<FlashcardItem>,
    onExitStudy: () -> Unit,
    onUpdateCard: (FlashcardItem) -> Unit,
    isDark: Boolean
) {
    var studyQueue by remember { mutableStateOf(initialCards) }
    var currentIndex by remember { mutableStateOf(0) }
    var isFlipped by remember { mutableStateOf(false) }
    var cardsReviewedCount by remember { mutableStateOf(0) }

    val currentCard = studyQueue.getOrNull(currentIndex)

    // Flip Animation
    val rotation by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
        label = "flipAnimation"
    )

    if (currentCard == null || currentIndex >= studyQueue.size) {
        StudyCompleteScreen(
            reviewedCount = cardsReviewedCount,
            onRestart = {
                studyQueue = initialCards
                currentIndex = 0
                isFlipped = false
                cardsReviewedCount = 0
            },
            onBackToDeck = onExitStudy,
            isDark = isDark
        )
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onExitStudy) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Exit Study", tint = IosBlue)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(deck.icon, fontSize = 18.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = deck.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color.White else Color(0xFF1C1C1E)
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(IosBlue.copy(alpha = 0.15f))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "${currentIndex + 1} / ${studyQueue.size}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = IosBlue
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Progress Bar
            val progressFraction = (currentIndex + 1).toFloat() / studyQueue.size.toFloat()
            FireworksProgressBar(
                progress = progressFraction,
                height = 8.dp,
                gradientColors = listOf(Color(0xFF38BDF8), Color(0xFF0284C7), Color(0xFF6366F1)),
                sparkColor = Color(0xFF38BDF8),
                isDark = isDark
            )

            Spacer(modifier = Modifier.height(20.dp))

            // 3D Flip Flashcard Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 280.dp)
                    .graphicsLayer {
                        rotationY = rotation
                        cameraDistance = 14f * density
                    }
                    .clickable { isFlipped = !isFlipped }
                    .shadow(12.dp, RoundedCornerShape(22.dp))
            ) {
                if (rotation <= 90f) {
                    // Front Face
                    val frontBg = if (isDark) Color(0xFF1C2030) else Color.White
                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = frontBg,
                        modifier = Modifier
                            .fillMaxSize()
                            .border(
                                2.dp,
                                Brush.linearGradient(listOf(IosBlue.copy(alpha = 0.6f), Color(0xFF5856D6))),
                                RoundedCornerShape(22.dp)
                            )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(IosBlue.copy(alpha = 0.12f))
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "❓ QUESTION",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = IosBlue,
                                        letterSpacing = 1.sp
                                    )
                                }

                                Text(
                                    text = "Tap to flip ↺",
                                    fontSize = 11.sp,
                                    color = if (isDark) Color(0x80FFFFFF) else Color(0x803C3C43)
                                )
                            }

                            NativeMarkdownText(
                                text = currentCard.front,
                                isDark = isDark,
                                fontSize = 17.sp,
                                textColor = if (isDark) Color.White else Color(0xFF1C1C1E),
                                modifier = Modifier
                                    .padding(vertical = 8.dp)
                                    .fillMaxWidth()
                            )

                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(IosBlue)
                                    .padding(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Reveal Answer",
                                        fontSize = 11.sp,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // Back Face (rotationY = 180f fix for un-mirrored display)
                    val backBg = if (isDark) Color(0xFF14223A) else Color(0xFFF0F6FF)
                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = backBg,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer { rotationY = 180f }
                            .border(
                                2.dp,
                                Brush.linearGradient(listOf(IosGreen, IosBlue)),
                                RoundedCornerShape(22.dp)
                            )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(IosGreen.copy(alpha = 0.15f))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "💡 ANSWER & EXPLANATION",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = IosGreen,
                                    letterSpacing = 1.sp
                                )
                            }

                            NativeMarkdownText(
                                text = currentCard.back,
                                isDark = isDark,
                                fontSize = 17.sp,
                                textColor = if (isDark) Color.White else Color(0xFF1C1C1E),
                                modifier = Modifier
                                    .padding(vertical = 8.dp)
                                    .fillMaxWidth()
                            )

                            Text(
                                text = "Select your recall accuracy below 👇",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color(0x99FFFFFF) else Color(0x993C3C43)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Rating Buttons (Shown when card is flipped or always accessible)
            if (isFlipped) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    RatingButton(
                        label = "Again",
                        sub = "< 1d",
                        color = IosRed,
                        onClick = {
                            val updated = calculateSm2(currentCard, 0)
                            onUpdateCard(updated)
                            cardsReviewedCount++

                            // Push card to end of queue to re-test
                            val newQueue = studyQueue.toMutableList()
                            newQueue.add(updated)
                            studyQueue = newQueue

                            currentIndex++
                            isFlipped = false
                        },
                        modifier = Modifier.weight(1f).testTag("rate_again")
                    )

                    RatingButton(
                        label = "Hard",
                        sub = "~${maxOf(1, Math.round(currentCard.interval * 1.2f))}d",
                        color = IosOrange,
                        onClick = {
                            val updated = calculateSm2(currentCard, 1)
                            onUpdateCard(updated)
                            cardsReviewedCount++
                            currentIndex++
                            isFlipped = false
                        },
                        modifier = Modifier.weight(1f).testTag("rate_hard")
                    )

                    RatingButton(
                        label = "Good",
                        sub = "~${if (currentCard.interval <= 1) 3 else Math.round(currentCard.interval * currentCard.ease)}d",
                        color = IosBlue,
                        onClick = {
                            val updated = calculateSm2(currentCard, 2)
                            onUpdateCard(updated)
                            cardsReviewedCount++
                            currentIndex++
                            isFlipped = false
                        },
                        modifier = Modifier.weight(1f).testTag("rate_good")
                    )

                    RatingButton(
                        label = "Easy",
                        sub = "~${if (currentCard.interval <= 1) 4 else Math.round(currentCard.interval * currentCard.ease * 1.3f)}d",
                        color = IosGreen,
                        onClick = {
                            val updated = calculateSm2(currentCard, 3)
                            onUpdateCard(updated)
                            cardsReviewedCount++
                            currentIndex++
                            isFlipped = false
                        },
                        modifier = Modifier.weight(1f).testTag("rate_easy")
                    )
                }
            } else {
                OutlinedButton(
                    onClick = { isFlipped = true },
                    shape = CircleShape,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Flip Card to Grade Answer", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
fun RatingButton(
    label: String,
    sub: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color.copy(alpha = 0.15f)),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, color.copy(alpha = 0.5f)),
        contentPadding = PaddingValues(vertical = 12.dp),
        modifier = modifier
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, color = color, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(modifier = Modifier.height(2.dp))
            Text(sub, color = color.copy(alpha = 0.9f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun StudyCompleteScreen(
    reviewedCount: Int,
    onRestart: () -> Unit,
    onBackToDeck: () -> Unit,
    isDark: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(IosGreen.copy(alpha = 0.15f))
                .border(2.dp, IosGreen, CircleShape)
                .shadow(8.dp, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Check, contentDescription = null, tint = IosGreen, modifier = Modifier.size(42.dp))
        }

        Spacer(modifier = Modifier.height(22.dp))

        Text(
            text = "Deck Mastered! 🎉",
            fontSize = 26.sp,
            fontWeight = FontWeight.ExtraBold,
            color = if (isDark) Color.White else Color(0xFF1C1C1E)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "You've successfully reviewed $reviewedCount flashcards today. Spaced repetition algorithm has rescheduled them for optimal memory retention!",
            fontSize = 14.sp,
            color = if (isDark) Color(0x99FFFFFF) else Color(0x993C3C43),
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )

        Spacer(modifier = Modifier.height(32.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = onRestart,
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = IosBlue),
                modifier = Modifier.heightIn(min = 44.dp)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Study Again", fontWeight = FontWeight.Bold)
            }

            OutlinedButton(
                onClick = onBackToDeck,
                shape = CircleShape,
                modifier = Modifier.heightIn(min = 44.dp)
            ) {
                Text("Back to Deck", fontWeight = FontWeight.Bold, color = if (isDark) Color.White else Color(0xFF1C1C1E))
            }
        }
    }
}

@Composable
fun NewDeckModal(
    onDismiss: () -> Unit,
    onCreateDeck: (String, String) -> Unit,
    isDark: Boolean
) {
    var deckName by remember { mutableStateOf("") }
    var selectedIcon by remember { mutableStateOf("🧬") }

    val emojis = listOf("🧬", "⚡", "🧪", "📖", "🔬", "🧮", "🌿", "🎯", "💡", "📐", "🩺", "🦴")

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = if (isDark) Color(0xFF1D2132) else Color.White,
            shadowElevation = 12.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(22.dp)) {
                Text(
                    text = "Create New Deck 🎴",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isDark) Color.White else Color(0xFF1C1C1E)
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = deckName,
                    onValueChange = { deckName = it },
                    placeholder = { Text("e.g. Physics Formulas, Organic Reactions...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("new_deck_name_input"),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "CHOOSE DECK ICON",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = IosBlue,
                    letterSpacing = 0.5.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(emojis, key = { it }) { emoji ->
                        val isSel = emoji == selectedIcon
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSel) IosBlue.copy(alpha = 0.2f) else Color.Transparent)
                                .border(1.5.dp, if (isSel) IosBlue else Color.Transparent, RoundedCornerShape(12.dp))
                                .clickable { selectedIcon = emoji },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(emoji, fontSize = 22.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = if (isDark) Color(0x99FFFFFF) else Color(0x993C3C43))
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            if (deckName.isNotBlank()) {
                                onCreateDeck(deckName.trim(), selectedIcon)
                            }
                        },
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = IosBlue),
                        enabled = deckName.isNotBlank(),
                        modifier = Modifier.testTag("confirm_create_deck")
                    ) {
                        Text("Create Deck →", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun EditDeckModal(
    deck: FlashcardDeck,
    onDismiss: () -> Unit,
    onSaveDeck: (String, String) -> Unit,
    isDark: Boolean
) {
    var deckName by remember { mutableStateOf(deck.name) }
    var selectedIcon by remember { mutableStateOf(deck.icon) }

    val emojis = listOf("🧬", "⚡", "🧪", "📖", "🔬", "🧮", "🌿", "🎯", "💡", "📐", "🩺", "🦴")

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = if (isDark) Color(0xFF1D2132) else Color.White,
            shadowElevation = 12.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(22.dp)) {
                Text(
                    text = "Edit Deck ✏️",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isDark) Color.White else Color(0xFF1C1C1E)
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = deckName,
                    onValueChange = { deckName = it },
                    label = { Text("Deck Name") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "CHOOSE DECK ICON",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = IosBlue,
                    letterSpacing = 0.5.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(emojis, key = { it }) { emoji ->
                        val isSel = emoji == selectedIcon
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSel) IosBlue.copy(alpha = 0.2f) else Color.Transparent)
                                .border(1.5.dp, if (isSel) IosBlue else Color.Transparent, RoundedCornerShape(12.dp))
                                .clickable { selectedIcon = emoji },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(emoji, fontSize = 22.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = if (isDark) Color(0x99FFFFFF) else Color(0x993C3C43))
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            if (deckName.isNotBlank()) {
                                onSaveDeck(deckName.trim(), selectedIcon)
                            }
                        },
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = IosBlue),
                        enabled = deckName.isNotBlank()
                    ) {
                        Text("Save Deck", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun EditCardModal(
    card: FlashcardItem,
    onDismiss: () -> Unit,
    onSaveCard: (String, String) -> Unit,
    isDark: Boolean
) {
    var frontText by remember { mutableStateOf(card.front) }
    var backText by remember { mutableStateOf(card.back) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = if (isDark) Color(0xFF1D2132) else Color.White,
            shadowElevation = 12.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(22.dp)) {
                Text(
                    text = "Edit Flashcard ✏️",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isDark) Color.White else Color(0xFF1C1C1E)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column {
                        Text("FRONT (Question / Prompt)", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = IosBlue)
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = frontText,
                            onValueChange = { frontText = it },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace, fontSize = 13.sp)
                        )
                    }

                    Column {
                        Text("BACK (Answer / Solution)", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = IosGreen)
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = backText,
                            onValueChange = { backText = it },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace, fontSize = 13.sp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = if (isDark) Color(0x99FFFFFF) else Color(0x993C3C43))
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            if (frontText.isNotBlank() && backText.isNotBlank()) {
                                onSaveCard(frontText.trim(), backText.trim())
                            }
                        },
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = IosBlue),
                        enabled = frontText.isNotBlank() && backText.isNotBlank()
                    ) {
                        Text("Save Card", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// SM-2 Spaced Repetition Logic
fun calculateSm2(
    card: FlashcardItem,
    q: Int // 0=Again, 1=Hard, 2=Good, 3=Easy
): FlashcardItem {
    var interval = card.interval
    var ease = card.ease
    var lapses = card.lapses

    when (q) {
        0 -> {
            interval = 1
            lapses += 1
            ease = maxOf(1.3f, ease - 0.2f)
        }
        1 -> {
            interval = maxOf(1, Math.round(interval * 1.2f))
            ease = maxOf(1.3f, ease - 0.15f)
        }
        2 -> {
            interval = if (interval <= 1) 3 else Math.round(interval * ease)
        }
        3 -> {
            interval = if (interval <= 1) 4 else Math.round(interval * ease * 1.3f)
            ease = minOf(3.0f, ease + 0.15f)
        }
    }

    val cal = Calendar.getInstance()
    cal.add(Calendar.DAY_OF_YEAR, interval)
    val nextDueStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.time)
    val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

    return card.copy(
        interval = interval,
        ease = ease,
        lapses = lapses,
        due = nextDueStr,
        lastStudied = todayStr,
        reviews = card.reviews + 1
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfToFlashcardModal(
    existingDecks: List<FlashcardDeck>,
    onDismiss: () -> Unit,
    onCreateDeckWithCards: (deckName: String, category: String, icon: String, cards: List<GeneratedCard>) -> Unit,
    onAddCardsToDeck: (deckId: Long, cards: List<GeneratedCard>) -> Unit,
    isDark: Boolean
) {
    val context = LocalContext.current
    var selectedPdfUri by remember { mutableStateOf<Uri?>(null) }
    var pdfFileName by remember { mutableStateOf<String?>(null) }
    var pdfFileSize by remember { mutableStateOf<String?>(null) }

    var targetMode by remember { mutableStateOf("new") } // "new" or "existing"
    var newDeckName by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Physics") }
    var selectedIcon by remember { mutableStateOf("📄") }
    var selectedExistingDeckId by remember { mutableStateOf<Long?>(existingDecks.firstOrNull()?.id) }

    var customApiKey by remember { mutableStateOf("") }
    var showApiKeyInput by remember { mutableStateOf(false) }

    var isGenerating by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var generatedCards by remember { mutableStateOf<List<GeneratedCard>>(emptyList()) }
    var selectedCardsMap by remember { mutableStateOf<Map<Int, Boolean>>(emptyMap()) }

    val scope = rememberCoroutineScope()

    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedPdfUri = it
            try {
                val cursor = context.contentResolver.query(it, null, null, null, null)
                cursor?.use { c ->
                    val nameIndex = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIndex = c.getColumnIndex(OpenableColumns.SIZE)
                    if (c.moveToFirst()) {
                        if (nameIndex != -1) {
                            val name = c.getString(nameIndex)
                            pdfFileName = name
                            if (newDeckName.isBlank()) {
                                newDeckName = name.replace(".pdf", "", ignoreCase = true).trim()
                            }
                        }
                        if (sizeIndex != -1) {
                            val bytes = c.getLong(sizeIndex)
                            pdfFileSize = "${String.format(java.util.Locale.US, "%.1f", bytes / (1024.0 * 1024.0))} MB"
                        }
                    }
                }
            } catch (e: Exception) {
                pdfFileName = "Selected_Document.pdf"
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = if (isDark) Color(0xFF181C2B) else Color.White,
            shadowElevation = 16.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(22.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        AiPdfLogoBadge(badgeSize = 46.dp, isDark = isDark)
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "PDF to Flashcards",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (isDark) Color.White else Color(0xFF1E293B)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(
                                            Brush.horizontalGradient(
                                                listOf(Color(0xFF8B5CF6), Color(0xFF6366F1))
                                            )
                                        )
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text("AI", fontSize = 9.sp, fontWeight = FontWeight.Black, color = Color.White)
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Extract high-yield study cards automatically",
                                fontSize = 11.sp,
                                color = if (isDark) Color(0xFFC4B5FD) else Color(0xFF7C3AED),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(if (isDark) Color(0x22FFFFFF) else Color(0x11000000))
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = if (isDark) Color.White else Color.Black, modifier = Modifier.size(18.dp))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (generatedCards.isEmpty()) {
                    // PDF Selection Area
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (selectedPdfUri != null) Color(0xFF8B5CF6).copy(alpha = 0.12f) else (if (isDark) Color(0xFF23283B) else Color(0xFFF2F4F8)),
                        border = BorderStroke(1.5.dp, if (selectedPdfUri != null) Color(0xFF8B5CF6) else (if (isDark) Color(0x1EFFFFFF) else Color(0x1F000000))),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { pdfPickerLauncher.launch("application/pdf") }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(if (selectedPdfUri != null) Color(0xFF8B5CF6) else Color(0xFF007AFF)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (selectedPdfUri != null) Icons.Default.PictureAsPdf else Icons.Default.UploadFile,
                                    contentDescription = null,
                                    tint = Color.White
                                )
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = pdfFileName ?: "Tap to select PDF file",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Color.White else Color(0xFF1C1C1E),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = pdfFileSize?.let { "Size: $it" } ?: "Supports NEET/JEE notes, PDFs, chapter modules",
                                    fontSize = 11.sp,
                                    color = if (isDark) Color(0x99FFFFFF) else Color(0x993C3C43)
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Surface(
                                shape = CircleShape,
                                color = Color(0xFF8B5CF6).copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = if (selectedPdfUri != null) "Change" else "Browse",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF8B5CF6),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Target Option: New Deck vs Existing Deck
                    Text(
                        text = "SAVE FLASHCARDS TO",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF8B5CF6),
                        letterSpacing = 0.5.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (targetMode == "new") Color(0xFF8B5CF6).copy(alpha = 0.15f) else Color.Transparent)
                                .border(1.5.dp, if (targetMode == "new") Color(0xFF8B5CF6) else (if (isDark) Color(0x1EFFFFFF) else Color(0x1F000000)), RoundedCornerShape(12.dp))
                                .clickable { targetMode = "new" }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("+ Create New Deck", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (targetMode == "new") Color(0xFF8B5CF6) else (if (isDark) Color.White else Color(0xFF1C1C1E)))
                        }

                        if (existingDecks.isNotEmpty()) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (targetMode == "existing") Color(0xFF8B5CF6).copy(alpha = 0.15f) else Color.Transparent)
                                    .border(1.5.dp, if (targetMode == "existing") Color(0xFF8B5CF6) else (if (isDark) Color(0x1EFFFFFF) else Color(0x1F000000)), RoundedCornerShape(12.dp))
                                    .clickable { targetMode = "existing" }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Add to Existing Deck", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (targetMode == "existing") Color(0xFF8B5CF6) else (if (isDark) Color.White else Color(0xFF1C1C1E)))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (targetMode == "new") {
                        OutlinedTextField(
                            value = newDeckName,
                            onValueChange = { newDeckName = it },
                            label = { Text("Deck Name") },
                            placeholder = { Text("e.g., Optics Formulas & Derivations") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "SELECT SUBJECT CATEGORY",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF8B5CF6)
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val categories = listOf("Physics" to "⚛️", "Chemistry" to "🧪", "Biology" to "🌿", "Other" to "📖")
                            categories.forEach { (cat, emoji) ->
                                val isSel = selectedCategory == cat
                                val catColor = when(cat) {
                                    "Physics" -> IosBlue
                                    "Chemistry" -> IosOrange
                                    "Biology" -> IosGreen
                                    else -> IosPurple
                                }
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isSel) catColor.copy(alpha = 0.2f) else Color.Transparent)
                                        .border(1.2.dp, if (isSel) catColor else (if (isDark) Color(0x1EFFFFFF) else Color(0x1F000000)), RoundedCornerShape(10.dp))
                                        .clickable {
                                            selectedCategory = cat
                                            selectedIcon = emoji
                                        }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(emoji, fontSize = 16.sp)
                                        Text(cat, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (isSel) catColor else (if (isDark) Color.White else Color(0xFF1C1C1E)))
                                    }
                                }
                            }
                        }
                    } else {
                        // Existing Decks List Selector
                        Text("Choose Deck:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (isDark) Color.White else Color.Black)
                        Spacer(modifier = Modifier.height(6.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(existingDecks, key = { it.id }) { deck ->
                                val isSel = selectedExistingDeckId == deck.id
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSel) Color(0xFF8B5CF6).copy(alpha = 0.2f) else (if (isDark) Color(0xFF23283B) else Color(0xFFF2F4F8)),
                                    border = BorderStroke(1.2.dp, if (isSel) Color(0xFF8B5CF6) else Color.Transparent),
                                    modifier = Modifier.clickable { selectedExistingDeckId = deck.id }
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                    ) {
                                        Text(deck.icon, fontSize = 14.sp)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(deck.name, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Optional API Key Toggle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showApiKeyInput = !showApiKeyInput },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "⚙️ Custom Lakshya AI Key (Optional)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color(0x99FFFFFF) else Color(0x993C3C43)
                        )
                        Icon(
                            imageVector = if (showApiKeyInput) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = if (isDark) Color(0x99FFFFFF) else Color(0x993C3C43)
                        )
                    }

                    if (showApiKeyInput) {
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = customApiKey,
                            onValueChange = { customApiKey = it },
                            placeholder = { Text("Paste Lakshya AI Key if needed") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Error Message
                    errorMessage?.let { err ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = IosRed.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, IosRed),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("❌", fontSize = 16.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(err, fontSize = 12.sp, color = IosRed, fontWeight = FontWeight.Medium)
                            }
                        }
                    }

                    // Generate Button / Loading State
                    if (isGenerating) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(color = Color(0xFF8B5CF6))
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = statusMessage,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF8B5CF6)
                            )
                        }
                    } else {
                        Button(
                            onClick = {
                                if (selectedPdfUri != null) {
                                    isGenerating = true
                                    statusMessage = "Analyzing PDF with LAKSHYA AI..."
                                    errorMessage = null
                                    scope.launch {
                                        val result = GeminiFlashcardGenerator.generateFlashcardsFromPdf(
                                            context = context,
                                            pdfUri = selectedPdfUri!!,
                                            customApiKey = customApiKey,
                                            subjectCategory = selectedCategory
                                        )
                                        isGenerating = false
                                        if (result.isSuccess) {
                                            val cards = result.getOrThrow()
                                            generatedCards = cards
                                            selectedCardsMap = cards.indices.associateWith { true }
                                        } else {
                                            errorMessage = result.exceptionOrNull()?.message ?: "Failed to generate flashcards."
                                        }
                                    }
                                }
                            },
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6)),
                            enabled = selectedPdfUri != null && !isGenerating,
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                        ) {
                            Text("✨ Extract Flashcards with LAKSHYA AI", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    // Preview Step: Show Extracted Flashcards
                    val selectedCount = selectedCardsMap.values.count { it }

                    Text(
                        text = "Extracted Flashcards (${generatedCards.size})",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color.White else Color(0xFF1C1C1E)
                    )
                    Text(
                        text = "Select cards to include in your deck:",
                        fontSize = 12.sp,
                        color = if (isDark) Color(0x99FFFFFF) else Color(0x993C3C43)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Box(modifier = Modifier.heightIn(max = 280.dp)) {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            itemsIndexed(generatedCards) { idx, card ->
                                val isChecked = selectedCardsMap[idx] ?: true
                                Card(
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isDark) Color(0xFF23283B) else Color(0xFFF7F8FC)
                                    ),
                                    border = BorderStroke(1.dp, if (isChecked) Color(0xFF8B5CF6) else Color.Transparent),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedCardsMap = selectedCardsMap.toMutableMap().apply {
                                                put(idx, !isChecked)
                                            }
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Checkbox(
                                            checked = isChecked,
                                            onCheckedChange = { checked ->
                                                selectedCardsMap = selectedCardsMap.toMutableMap().apply {
                                                    put(idx, checked)
                                                }
                                            },
                                            colors = CheckboxDefaults.colors(checkedColor = Color(0xFF8B5CF6))
                                        )

                                        Spacer(modifier = Modifier.width(8.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text("Q: ", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF8B5CF6))
                                                NativeMarkdownText(
                                                    text = card.front,
                                                    isDark = isDark,
                                                    fontSize = 13.sp,
                                                    textColor = if (isDark) Color.White else Color(0xFF1C1C1E)
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text("A: ", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = IosGreen)
                                                NativeMarkdownText(
                                                    text = card.back,
                                                    isDark = isDark,
                                                    fontSize = 12.sp,
                                                    textColor = if (isDark) Color(0xCCFFFFFF) else Color(0xFF475569)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { generatedCards = emptyList() }) {
                            Text("← Back", color = if (isDark) Color(0x99FFFFFF) else Color(0x993C3C43))
                        }

                        Button(
                            onClick = {
                                val finalCardsToImport = generatedCards.filterIndexed { index, _ ->
                                    selectedCardsMap[index] == true
                                }
                                if (finalCardsToImport.isNotEmpty()) {
                                    if (targetMode == "new") {
                                        val deckName = newDeckName.ifBlank { pdfFileName ?: "AI PDF Flashcards" }
                                        onCreateDeckWithCards(deckName, selectedCategory, selectedIcon, finalCardsToImport)
                                    } else if (selectedExistingDeckId != null) {
                                        onAddCardsToDeck(selectedExistingDeckId!!, finalCardsToImport)
                                    }
                                    Toast.makeText(context, "Added ${finalCardsToImport.size} flashcards to deck!", Toast.LENGTH_SHORT).show()
                                    onDismiss()
                                }
                            },
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6)),
                            enabled = selectedCount > 0
                        ) {
                            Text("📥 Save $selectedCount Flashcards", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
