package com.example.ui.components

import android.net.Uri
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.ChatMessageEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Isolated, high-performance Chat Message Bubble.
 * Declared as a standalone @Composable to enable Compose smart-recomposition skipping
 * and 120 FPS butter-smooth scrolling in LazyColumn.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChatMessageBubble(
    msg: ChatMessageEntity,
    isDark: Boolean,
    isStreaming: Boolean,
    chatType: String,
    activeModelLabel: String,
    isSpeaking: Boolean,
    onToggleSpeak: () -> Unit,
    onCopy: () -> Unit,
    onAddToMistakes: () -> Unit,
    onSimplify: () -> Unit,
    onPracticeSimilar: () -> Unit,
    onStepBreakdown: () -> Unit,
    onImageClick: (String) -> Unit,
    onLongClick: () -> Unit,
    renderTable: @Composable (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isUser = msg.sender == "user"
    val bubbleBg = if (isUser) {
        if (isDark) Color(0xFF1E293B) else Color(0xFFE0E7FF)
    } else {
        if (isDark) Color(0xFF0F172A) else Color.White
    }
    val bubbleTextColor = if (isDark) Color.White else Color(0xFF0F172A)
    val timeColor = if (isDark) Color(0x99FFFFFF) else Color(0x800F172A)
    val subTextColor = if (isDark) Color(0x99FFFFFF) else Color(0xFF64748B)

    val displayTime = remember(msg.time, msg.timestamp) {
        if (msg.time.isNotBlank() && (msg.time.contains("AM", ignoreCase = true) || msg.time.contains("PM", ignoreCase = true) || msg.time.contains(":"))) {
            msg.time
        } else {
            try {
                SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(msg.timestamp))
            } catch (_: Exception) {
                "12:00 PM"
            }
        }
    }

    val bubbleShape = remember(isUser) {
        RoundedCornerShape(
            topStart = 18.dp,
            topEnd = 18.dp,
            bottomStart = if (isUser) 18.dp else 4.dp,
            bottomEnd = if (isUser) 4.dp else 18.dp
        )
    }

    val borderStroke = remember(isUser, isDark, chatType) {
        BorderStroke(
            1.dp,
            if (isUser) {
                Color(0xFF6366F1).copy(alpha = 0.4f)
            } else {
                if (chatType == "error") Color(0xFFFB7185).copy(alpha = 0.35f) else Color(0xFF38BDF8).copy(alpha = 0.35f)
            }
        )
    }

    Column(
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp)
    ) {
        Surface(
            shape = bubbleShape,
            color = bubbleBg,
            border = borderStroke,
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .clip(bubbleShape)
                .combinedClickable(
                    onClick = { /* Instant click */ },
                    onLongClick = onLongClick
                )
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                // Header for AI message
                if (!isUser) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f, fill = false),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = if (chatType == "error") "⚠️ Error Analysis" else "🤖 LAKSHYA AI Solution",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = if (chatType == "error") Color(0xFFFB7185) else Color(0xFF38BDF8),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFF6366F1).copy(alpha = 0.15f),
                                border = BorderStroke(0.5.dp, Color(0xFF818CF8).copy(alpha = 0.3f))
                            ) {
                                Text(
                                    text = activeModelLabel,
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF818CF8),
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                    maxLines = 1,
                                    overflow = TextOverflow.Visible
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = displayTime,
                            fontSize = 9.5.sp,
                            color = timeColor,
                            maxLines = 1
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Attachment rendering
                if (msg.mediaUri != null && msg.mediaType == "image") {
                    val uri = remember(msg.mediaUri) { Uri.parse(msg.mediaUri) }
                    AsyncImage(
                        model = uri,
                        contentDescription = "Attached Image",
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 160.dp, max = 280.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onImageClick(msg.mediaUri) },
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                } else if (msg.mediaUri != null) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isDark) Color(0x22FFFFFF) else Color(0x11000000),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(8.dp)
                        ) {
                            Icon(
                                Icons.Default.PictureAsPdf,
                                contentDescription = null,
                                tint = Color(0xFFEF4444),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = msg.mediaName ?: "Attached Document.pdf",
                                color = bubbleTextColor,
                                fontSize = 11.5.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Message content (Table or Markdown Text)
                if (msg.mediaType == "table" || msg.text.startsWith("[TABLE:")) {
                    renderTable(msg.text)
                } else if (msg.text.isNotBlank()) {
                    ChatMessageRenderer(
                        text = msg.text,
                        isDark = isDark,
                        textColor = bubbleTextColor,
                        isStreaming = isStreaming
                    )
                }

                // Bottom Quick Action bar for AI response
                if (!isUser) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 1. Copy
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isDark) Color(0x18FFFFFF) else Color(0xFFF1F5F9),
                            border = BorderStroke(0.7.dp, if (isDark) Color(0x22FFFFFF) else Color(0xFFE2E8F0)),
                            modifier = Modifier.clickable { onCopy() }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = subTextColor, modifier = Modifier.size(11.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("Copy", fontSize = 10.sp, color = subTextColor, fontWeight = FontWeight.Bold)
                            }
                        }

                        // 2. Audio Readout (TTS)
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSpeaking) Color(0xFF10B981).copy(alpha = 0.2f) else if (isDark) Color(0x18FFFFFF) else Color(0xFFF1F5F9),
                            border = BorderStroke(0.7.dp, if (isSpeaking) Color(0xFF10B981) else if (isDark) Color(0x22FFFFFF) else Color(0xFFE2E8F0)),
                            modifier = Modifier.clickable { onToggleSpeak() }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(if (isSpeaking) "⏹️" else "🔊", fontSize = 10.sp)
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = if (isSpeaking) "Stop Audio" else "Read Aloud",
                                    fontSize = 10.sp,
                                    color = if (isSpeaking) Color(0xFF10B981) else subTextColor,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // 3. Log Mistake
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFF43F5E).copy(alpha = 0.15f),
                            border = BorderStroke(0.7.dp, Color(0xFFF43F5E).copy(alpha = 0.3f)),
                            modifier = Modifier.clickable { onAddToMistakes() }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("📓", fontSize = 10.sp)
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("Log Mistake", fontSize = 10.sp, color = Color(0xFFFB7185), fontWeight = FontWeight.Bold)
                            }
                        }

                        // 4. Simplify
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF6366F1).copy(alpha = 0.15f),
                            border = BorderStroke(0.7.dp, Color(0xFF6366F1).copy(alpha = 0.3f)),
                            modifier = Modifier.clickable { onSimplify() }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("⚡", fontSize = 10.sp)
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("Simplify", fontSize = 10.sp, color = Color(0xFFA5B4FC), fontWeight = FontWeight.Bold)
                            }
                        }

                        // 5. Practice Similar
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF0284C7).copy(alpha = 0.15f),
                            border = BorderStroke(0.7.dp, Color(0xFF0284C7).copy(alpha = 0.3f)),
                            modifier = Modifier.clickable { onPracticeSimilar() }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("🎯", fontSize = 10.sp)
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("Practice Similar", fontSize = 10.sp, color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold)
                            }
                        }

                        // 6. Step Breakdown
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF8B5CF6).copy(alpha = 0.15f),
                            border = BorderStroke(0.7.dp, Color(0xFF8B5CF6).copy(alpha = 0.3f)),
                            modifier = Modifier.clickable { onStepBreakdown() }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("🔍", fontSize = 10.sp)
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("Step Breakdown", fontSize = 10.sp, color = Color(0xFFC4B5FD), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // User footer (timestamp + read checkmarks)
                if (isUser) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.End)
                            .padding(top = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = displayTime,
                            color = timeColor,
                            fontSize = 10.sp
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            Icons.Default.DoneAll,
                            contentDescription = "Delivered",
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
            }
        }
    }
}
