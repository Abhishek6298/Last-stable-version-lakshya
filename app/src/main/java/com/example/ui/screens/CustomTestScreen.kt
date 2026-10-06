package com.example.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.AiSavedTest
import com.example.data.ExamCategory
import com.example.ui.AppViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomTestScreen(
    viewModel: AppViewModel,
    onNavigate: (String) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isDark = MaterialTheme.colorScheme.background.red < 0.5f
    val textColor = if (isDark) Color.White else Color(0xFF0F172A)
    val subTextColor = if (isDark) Color(0x99FFFFFF) else Color(0xFF64748B)

    val savedTests by viewModel.aiSavedTests.collectAsStateWithLifecycle()

    var selectedCategory by remember { mutableStateOf("All") }
    var searchQuery by remember { mutableStateOf("") }
    var testToEdit by remember { mutableStateOf<AiSavedTest?>(null) }
    var testToDelete by remember { mutableStateOf<AiSavedTest?>(null) }
    var showNewOcrDialog by remember { mutableStateOf(false) }

    // Multi-category extractor
    val categories = remember(savedTests) {
        val distinctCats = savedTests.map { it.institute.ifBlank { "General" }.trim() }
            .distinct()
            .sorted()
        listOf("All") + distinctCats
    }

    val filteredTests = remember(savedTests, selectedCategory, searchQuery) {
        savedTests.filter { test ->
            val cat = test.institute.ifBlank { "General" }.trim()
            val categoryMatches = selectedCategory == "All" || cat.equals(selectedCategory, ignoreCase = true)
            val searchMatches = searchQuery.isBlank() ||
                test.title.contains(searchQuery, ignoreCase = true) ||
                cat.contains(searchQuery, ignoreCase = true)
            categoryMatches && searchMatches
        }
    }

    Scaffold(
        topBar = {
            Surface(
                color = if (isDark) Color(0xFF0F172A) else Color.White,
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth().statusBarsPadding()
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = onNavigateBack,
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isDark) Color(0x1AFFFFFF) else Color(0xFFF1F5F9))
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = textColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        "Custom Test",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 18.sp,
                                        color = textColor
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Surface(
                                        color = Color(0xFFF59E0B).copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(6.dp),
                                        border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.35f))
                                    ) {
                                        Text(
                                            "OCR CBT",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color(0xFFF59E0B),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Text(
                                    "${savedTests.size} tests • Organized by Category",
                                    fontSize = 11.5.sp,
                                    color = subTextColor
                                )
                            }
                        }

                        // Create New OCR Test Action Button
                        Button(
                            onClick = { showNewOcrDialog = true },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B)),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("New Test", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Search Field
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search tests by name or category...", fontSize = 12.5.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = subTextColor, modifier = Modifier.size(18.dp)) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear", tint = subTextColor, modifier = Modifier.size(16.dp))
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFF59E0B),
                            unfocusedBorderColor = if (isDark) Color(0x22FFFFFF) else Color(0xFFE2E8F0)
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Category Tabs / Filter Chips
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(categories) { cat ->
                            val isSel = selectedCategory == cat
                            val count = if (cat == "All") savedTests.size else savedTests.count { it.institute.ifBlank { "General" }.trim().equals(cat, ignoreCase = true) }
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSel) Color(0xFFF59E0B) else if (isDark) Color(0x14FFFFFF) else Color(0xFFF1F5F9),
                                border = BorderStroke(1.dp, if (isSel) Color(0xFFF59E0B) else if (isDark) Color(0x22FFFFFF) else Color(0xFFCBD5E1)),
                                modifier = Modifier.clickable { selectedCategory = cat }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = cat,
                                        fontSize = 11.5.sp,
                                        fontWeight = if (isSel) FontWeight.ExtraBold else FontWeight.Medium,
                                        color = if (isSel) Color.White else textColor
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Surface(
                                        shape = CircleShape,
                                        color = if (isSel) Color.White.copy(alpha = 0.25f) else Color(0xFFF59E0B).copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            "$count",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Black,
                                            color = if (isSel) Color.White else Color(0xFFF59E0B),
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        containerColor = if (isDark) Color(0xFF0B0F19) else Color(0xFFF8FAFC),
        modifier = modifier
    ) { paddingValues ->
        if (filteredTests.isEmpty()) {
            // Empty State
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0xFF131A2E) else Color.White),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, if (isDark) Color(0x22FFFFFF) else Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFF59E0B).copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🪄", fontSize = 28.sp)
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            if (savedTests.isEmpty()) "No Custom Tests Yet" else "No Tests in '$selectedCategory'",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp,
                            color = textColor
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            if (savedTests.isEmpty())
                                "Scan any PDF test paper or photo with Abhi Magic 🪄 OCR. It will generate a live CBT test with customizable timer, KaTeX, and chemistry reaction support!"
                            else "No tests matched the category or search criteria. Switch category or create a new test.",
                            fontSize = 12.sp,
                            color = subTextColor,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            lineHeight = 16.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { showNewOcrDialog = true },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B))
                        ) {
                            Icon(Icons.Default.CloudUpload, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Create / Scan OCR Test", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 12.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredTests, key = { it.id }) { test ->
                    CustomTestCardItem(
                        test = test,
                        onAttempt = {
                            viewModel.reattemptSavedAiTest(test)
                            onNavigate("ai_test")
                        },
                        onReview = {
                            viewModel.loadSavedAiTest(test)
                            onNavigate("ai_test")
                        },
                        onEdit = { testToEdit = test },
                        onDelete = { testToDelete = test },
                        isDark = isDark
                    )
                }
            }
        }
    }

    // ----------------------------------------------------
    // EDIT TEST NAME & CATEGORY DIALOG
    // ----------------------------------------------------
    if (testToEdit != null) {
        val currentTest = testToEdit!!
        var editTitle by remember(currentTest) { mutableStateOf(currentTest.title) }
        var editCategory by remember(currentTest) { mutableStateOf(currentTest.institute.ifBlank { "General" }) }

        val categoryPresets = listOf("Physics", "Chemistry", "Biology", "Full Syllabus", "Allen", "PW", "Aakash", "General")

        Dialog(onDismissRequest = { testToEdit = null }) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = if (isDark) Color(0xFF131A2E) else Color.White,
                border = BorderStroke(1.dp, if (isDark) Color(0x33FFFFFF) else Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth().padding(8.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Edit, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Edit Test Details", fontWeight = FontWeight.Black, fontSize = 16.sp, color = textColor)
                        }
                        IconButton(onClick = { testToEdit = null }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = subTextColor, modifier = Modifier.size(18.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text("Test Name", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = textColor)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = editTitle,
                        onValueChange = { editTitle = it },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFFF59E0B))
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Category / Coaching Tag", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = textColor)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = editCategory,
                        onValueChange = { editCategory = it },
                        placeholder = { Text("e.g. Physics, Allen, Organic Chemistry", fontSize = 12.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFFF59E0B))
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Quick Categories:", fontSize = 10.5.sp, color = subTextColor)
                    Spacer(modifier = Modifier.height(4.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(categoryPresets) { preset ->
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (editCategory == preset) Color(0xFFF59E0B).copy(alpha = 0.2f) else if (isDark) Color(0x14FFFFFF) else Color(0xFFF1F5F9),
                                border = BorderStroke(1.dp, if (editCategory == preset) Color(0xFFF59E0B) else Color.Transparent),
                                modifier = Modifier.clickable { editCategory = preset }
                            ) {
                                Text(
                                    preset,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (editCategory == preset) Color(0xFFF59E0B) else textColor,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { testToEdit = null },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel", fontSize = 12.sp, color = textColor)
                        }
                        Button(
                            onClick = {
                                if (editTitle.isNotBlank()) {
                                    viewModel.updateAiSavedTest(currentTest, editTitle, editCategory)
                                    testToEdit = null
                                    Toast.makeText(context, "Test updated successfully", Toast.LENGTH_SHORT).show()
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B)),
                            modifier = Modifier.weight(1.2f)
                        ) {
                            Text("Save Changes", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
                        }
                    }
                }
            }
        }
    }

    // ----------------------------------------------------
    // DELETE CONFIRMATION DIALOG
    // ----------------------------------------------------
    if (testToDelete != null) {
        val test = testToDelete!!
        AlertDialog(
            onDismissRequest = { testToDelete = null },
            title = { Text("Delete Custom Test", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = { Text("Are you sure you want to delete \"${test.title}\"? This cannot be undone.", fontSize = 13.sp) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteSavedAiTest(test.id)
                        testToDelete = null
                        Toast.makeText(context, "Test deleted", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Text("Delete", color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { testToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // ----------------------------------------------------
    // NEW OCR TEST LAUNCH DIALOG
    // ----------------------------------------------------
    if (showNewOcrDialog) {
        NewOcrTestCreatorDialog(
            viewModel = viewModel,
            onDismiss = { showNewOcrDialog = false },
            onLaunched = {
                showNewOcrDialog = false
                onNavigate("ai_test")
            },
            isDark = isDark
        )
    }
}

// ----------------------------------------------------
// CUSTOM TEST CARD ITEM
// ----------------------------------------------------
@Composable
fun CustomTestCardItem(
    test: AiSavedTest,
    onAttempt: () -> Unit,
    onReview: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    isDark: Boolean
) {
    val textColor = if (isDark) Color.White else Color(0xFF0F172A)
    val subTextColor = if (isDark) Color(0x99FFFFFF) else Color(0xFF64748B)
    val dateStr = SimpleDateFormat("dd MMM, h:mm a", Locale.getDefault()).format(Date(test.timestamp))
    val category = test.institute.ifBlank { "General" }
    val isAttempted = test.score > 0 || test.timeTakenSeconds > 0

    Card(
        colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0xFF131A2E) else Color.White),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, if (isDark) Color(0x22FFFFFF) else Color(0xFFE2E8F0)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row: Category Badge + Edit + Delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Surface(
                    color = Color(0xFFF59E0B).copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(0.8.dp, Color(0xFFF59E0B).copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🏷️", fontSize = 10.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            category,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFFF59E0B)
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onEdit, modifier = Modifier.size(30.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Test", tint = Color(0xFF6366F1), modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(30.dp)) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete Test", tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Test Title
            Text(
                test.title,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 15.sp,
                color = textColor,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Meta Info: Questions, Marks, Date
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    "📝 ${test.totalQuestions} Questions",
                    fontSize = 11.sp,
                    color = subTextColor,
                    fontWeight = FontWeight.Medium
                )
                Text("•", fontSize = 10.sp, color = subTextColor)
                Text(
                    "🎯 ${test.maxScore} Marks",
                    fontSize = 11.sp,
                    color = subTextColor,
                    fontWeight = FontWeight.Medium
                )
                Text("•", fontSize = 10.sp, color = subTextColor)
                Text(
                    dateStr,
                    fontSize = 10.5.sp,
                    color = subTextColor
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Score / Attempt Status Banner
            if (isAttempted) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF10B981).copy(alpha = 0.12f),
                    border = BorderStroke(0.8.dp, Color(0xFF10B981).copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Score: ${test.score} / ${test.maxScore}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF10B981)
                        )
                        Text(
                            "Accuracy: ${String.format(Locale.getDefault(), "%.0f%%", test.accuracyPct)}",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF10B981)
                        )
                    }
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF6366F1).copy(alpha = 0.1f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "⚡ Ready to Attempt (Timer & CBT active)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF6366F1),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onAttempt,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B)),
                    modifier = Modifier.weight(1f).heightIn(min = 40.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (isAttempted) "Re-Attempt 🔁" else "Start CBT Test 🚀", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
                }

                if (isAttempted) {
                    OutlinedButton(
                        onClick = onReview,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Color(0xFF6366F1).copy(alpha = 0.6f)),
                        modifier = Modifier.weight(0.9f).heightIn(min = 40.dp)
                    ) {
                        Icon(Icons.Default.Assignment, contentDescription = null, tint = Color(0xFF6366F1), modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Solutions 📋", fontWeight = FontWeight.Bold, fontSize = 11.5.sp, color = Color(0xFF6366F1))
                    }
                }
            }
        }
    }
}

// ----------------------------------------------------
// NEW OCR TEST CREATOR DIALOG (WITH CUSTOM TIMER)
// ----------------------------------------------------
@Composable
fun NewOcrTestCreatorDialog(
    viewModel: AppViewModel,
    onDismiss: () -> Unit,
    onLaunched: () -> Unit,
    isDark: Boolean
) {
    val context = LocalContext.current
    val textColor = if (isDark) Color.White else Color(0xFF0F172A)
    val subTextColor = if (isDark) Color(0x99FFFFFF) else Color(0xFF64748B)

    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var selectedName by remember { mutableStateOf<String?>(null) }
    var categoryText by remember { mutableStateOf("Physics") }
    var durationMode by remember { mutableStateOf("60") } // "30", "60", "180", "200", "CUSTOM", "AUTO"
    var customMinutesInput by remember { mutableStateOf("60") }

    val docPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            selectedUri = uri
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            val name = cursor?.use {
                if (it.moveToFirst()) {
                    val idx = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0) it.getString(idx) else null
                } else null
            } ?: "Test_Paper.pdf"
            selectedName = name
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = if (isDark) Color(0xFF131A2E) else Color.White,
            border = BorderStroke(1.dp, if (isDark) Color(0x33FFFFFF) else Color(0xFFE2E8F0)),
            modifier = Modifier.fillMaxWidth().padding(8.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🪄", fontSize = 20.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("New OCR Custom Test", fontWeight = FontWeight.Black, fontSize = 16.sp, color = textColor)
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = subTextColor, modifier = Modifier.size(18.dp))
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Select File Button
                Surface(
                    color = if (selectedUri != null) Color(0xFFF59E0B).copy(alpha = 0.12f) else if (isDark) Color(0x14FFFFFF) else Color(0xFFF8FAFC),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, if (selectedUri != null) Color(0xFFF59E0B) else Color(0xFFCBD5E1)),
                    modifier = Modifier.fillMaxWidth().clickable { docPicker.launch("*/*") }
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            if (selectedUri != null) Icons.Default.Description else Icons.Default.CloudUpload,
                            contentDescription = null,
                            tint = Color(0xFFF59E0B),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                selectedName ?: "Tap to Select PDF or Photo Paper",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.5.sp,
                                color = textColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                if (selectedUri != null) "Ready for OCR CBT Extraction" else "Supports PDF, JPG, PNG",
                                fontSize = 10.sp,
                                color = subTextColor
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Category
                Text("Category / Subject", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = textColor)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = categoryText,
                    onValueChange = { categoryText = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFFF59E0B))
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Custom Timer Duration Selection
                Text("⏱️ Custom Test Timer", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = textColor)
                Spacer(modifier = Modifier.height(4.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val opts = listOf(
                        "30" to "30m",
                        "60" to "1h",
                        "180" to "3h",
                        "200" to "200m (NEET)",
                        "CUSTOM" to "Custom ⚙️",
                        "AUTO" to "Auto"
                    )
                    items(opts) { (m, label) ->
                        val isSel = durationMode == m
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSel) Color(0xFF6366F1) else if (isDark) Color(0x14FFFFFF) else Color(0xFFF1F5F9),
                            border = BorderStroke(1.dp, if (isSel) Color(0xFF6366F1) else Color.Transparent),
                            modifier = Modifier.clickable { durationMode = m }
                        ) {
                            Text(
                                label,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSel) Color.White else textColor,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                            )
                        }
                    }
                }

                if (durationMode == "CUSTOM") {
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = customMinutesInput,
                        onValueChange = { if (it.all { ch -> ch.isDigit() } && it.length <= 4) customMinutesInput = it },
                        label = { Text("Duration in Minutes", fontSize = 11.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF6366F1))
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                Button(
                    onClick = {
                        if (selectedUri != null) {
                            val customMins = when (durationMode) {
                                "30" -> 30
                                "60" -> 60
                                "180" -> 180
                                "200" -> 200
                                "CUSTOM" -> customMinutesInput.toIntOrNull()?.coerceIn(5, 360) ?: 60
                                else -> null
                            }
                            viewModel.extractTestFromDocumentOrImage(
                                context = context,
                                exam = ExamCategory.NEET,
                                uri = selectedUri!!,
                                fileName = selectedName ?: "Test_Paper.pdf",
                                institute = categoryText.ifBlank { "General" },
                                customDurationMinutes = customMins
                            )
                            onLaunched()
                        } else {
                            Toast.makeText(context, "Please select a test paper first", Toast.LENGTH_SHORT).show()
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B)),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 44.dp)
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Extract & Launch CBT Test", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}
