package com.example.ui

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.example.data.AppDatabase
import com.example.data.Note
import com.example.ui.theme.MyApplicationTheme
import com.example.widget.WidgetUpdateHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class QuickNoteActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val noteId = intent.getIntExtra("EXTRA_NOTE_ID", -1)
        val initialTitle = intent.getStringExtra("EXTRA_NOTE_TITLE") ?: ""
        val initialContent = intent.getStringExtra("EXTRA_NOTE_CONTENT") ?: ""

        setContent {
            val isDark = true
            MyApplicationTheme(darkTheme = isDark) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0x99000000))
                        .clickable(onClick = { finish() })
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = false) {}
                            .padding(horizontal = 8.dp),
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF0F172A)
                        ),
                        border = BorderStroke(1.5.dp, Color(0xFF6366F1).copy(alpha = 0.6f))
                    ) {
                        QuickNoteComposerContent(
                            initialNoteId = noteId,
                            initialTitle = initialTitle,
                            initialContent = initialContent,
                            onSave = { title, content, color ->
                                saveNoteAndSyncWidget(noteId, title, content, color)
                            },
                            onDismiss = { finish() }
                        )
                    }
                }
            }
        }
    }

    private fun saveNoteAndSyncWidget(existingId: Int, title: String, content: String, color: Long) {
        if (title.isBlank() && content.isBlank()) {
            Toast.makeText(this, "Please enter some text or title", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(this@QuickNoteActivity)
            val noteToSave = if (existingId != -1) {
                Note(id = existingId, title = title.trim(), content = content.trim(), timestamp = System.currentTimeMillis(), color = color)
            } else {
                Note(title = title.trim(), content = content.trim(), timestamp = System.currentTimeMillis(), color = color)
            }
            db.appDao().insertNote(noteToSave)

            // Put this note at the front of the widget
            val prefs = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
            prefs.edit().putInt("widget_notes_current_index", 0).apply()

            // Update all widgets immediately
            WidgetUpdateHelper.updateAllWidgets(this@QuickNoteActivity)

            withContext(Dispatchers.Main) {
                Toast.makeText(this@QuickNoteActivity, "Note saved & widget updated! 📝⚡", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }
}

@Composable
fun QuickNoteComposerContent(
    initialNoteId: Int,
    initialTitle: String,
    initialContent: String,
    onSave: (title: String, content: String, color: Long) -> Unit,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf(initialTitle) }
    var content by remember { mutableStateOf(initialContent) }
    val colorOptions = listOf(
        0xFF818CF8, // Indigo
        0xFF38BDF8, // Sky
        0xFF34D399, // Emerald
        0xFFFBBF24, // Amber
        0xFFF472B6, // Pink
        0xFFA78BFA  // Purple
    )
    var selectedColor by remember { mutableStateOf(colorOptions[0]) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = null,
                    tint = Color(0xFF818CF8),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = if (initialNoteId != -1) "Edit Quick Note" else "New Quick Note",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                    Text(
                        text = "Instant 2-way sync with Home Screen Widget",
                        fontSize = 10.5.sp,
                        color = Color(0xFF94A3B8)
                    )
                }
            }
            IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF94A3B8), modifier = Modifier.size(18.dp))
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Title Field
        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            placeholder = { Text("Title (e.g. Optics Formula, Target, Reminder)", fontSize = 13.sp, color = Color(0xFF64748B)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(selectedColor),
                unfocusedBorderColor = Color(0xFF334155),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            )
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Content Field (Multi-line)
        OutlinedTextField(
            value = content,
            onValueChange = { content = it },
            placeholder = { Text("Write your quick note, high-yield formula, or revision sticky here...", fontSize = 13.sp, color = Color(0xFF64748B)) },
            minLines = 4,
            maxLines = 8,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(selectedColor),
                unfocusedBorderColor = Color(0xFF334155),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            )
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Color Picker Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Card Color:", fontSize = 11.5.sp, color = Color(0xFF94A3B8), fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                colorOptions.forEach { col ->
                    val isSelected = selectedColor == col
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(Color(col))
                            .border(
                                width = if (isSelected) 2.5.dp else 0.dp,
                                color = if (isSelected) Color.White else Color.Transparent,
                                shape = CircleShape
                            )
                            .clickable { selectedColor = col }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp),
                border = BorderStroke(1.dp, Color(0xFF334155))
            ) {
                Text("Cancel", color = Color(0xFF94A3B8), fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = { onSave(title, content, selectedColor) },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                modifier = Modifier
                    .weight(1.3f)
                    .height(44.dp)
            ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Save Note ✓", fontWeight = FontWeight.Black, color = Color.White)
            }
        }
    }
}
