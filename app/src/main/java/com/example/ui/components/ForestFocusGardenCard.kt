package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.MusicOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.StudyLog
import com.example.util.AmbientStudySoundManager
import kotlin.math.cos
import kotlin.math.sin

data class PlantType(
    val id: String,
    val name: String,
    val emoji: String,
    val primaryColor: Color,
    val secondaryColor: Color,
    val description: String
)

val AVAILABLE_PLANTS = listOf(
    PlantType("pine", "Nordic Pine", "🌲", Color(0xFF10B981), Color(0xFF047857), "Deep endurance & relentless focus"),
    PlantType("sakura", "Zen Sakura", "🌸", Color(0xFFF472B6), Color(0xFFDB2777), "Calm memorization & revision"),
    PlantType("bamboo", "Green Bamboo", "🎋", Color(0xFF34D399), Color(0xFF059669), "Fast problem solving & logic"),
    PlantType("sunflower", "Radiant Bloom", "🌻", Color(0xFFFBBF24), Color(0xFFD97706), "High energy morning sessions"),
    PlantType("biomushroom", "Night Glow", "🍄", Color(0xFF818CF8), Color(0xFF6366F1), "Late night quiet hustle")
)

val MOTIVATIONAL_STUDY_QUOTES = listOf(
    "“The pain of discipline is far less than the pain of regret.”",
    "“One focused hour today is worth ten distracted hours tomorrow.”",
    "“Every single concept you master brings you closer to your dream rank.”",
    "“Your future self is thanking you for not giving up right now.”",
    "“Discipline is choosing between what you want now, and what you want most.”",
    "“Small daily improvements over time lead to stunning results.”"
)

@Composable
fun ForestFocusGardenCard(
    secondsElapsed: Int,
    isRunning: Boolean,
    studyLogs: List<StudyLog>,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    var selectedPlantId by remember { mutableStateOf("pine") }
    val selectedPlant = AVAILABLE_PLANTS.find { it.id == selectedPlantId } ?: AVAILABLE_PLANTS.first()
    
    // Ambient sound states
    var isSoundActive by remember { mutableStateOf(AmbientStudySoundManager.isSoundPlaying()) }
    var currentSoundName by remember { mutableStateOf(AmbientStudySoundManager.getCurrentSound()) }
    var soundVolume by remember { mutableStateOf(AmbientStudySoundManager.getVolume()) }
    var showSoundControls by remember { mutableStateOf(false) }

    // Growth Stage (0 to 4) based on seconds:
    // 0: < 180s (3m) -> Seed
    // 1: 180s - 600s (10m) -> Sprout
    // 2: 600s - 1500s (25m) -> Sapling
    // 3: 1500s - 2700s (45m) -> Blooming Tree
    // 4: > 2700s -> Ancient Wonder Tree
    val stage = when {
        secondsElapsed < 180 -> 0
        secondsElapsed < 600 -> 1
        secondsElapsed < 1500 -> 2
        secondsElapsed < 2700 -> 3
        else -> 4
    }

    val stageTitles = listOf(
        "🌰 Seed Resting in Soil",
        "🌱 Tender Sprout Awakening",
        "🌿 Growing Strong Sapling",
        "🌳 Fully Bloomed Focus Tree",
        "🌲✨ Ancient Radiant Wonder"
    )

    val stageProgress = when (stage) {
        0 -> secondsElapsed / 180f
        1 -> (secondsElapsed - 180) / 420f
        2 -> (secondsElapsed - 600) / 900f
        3 -> (secondsElapsed - 1500) / 1200f
        else -> 1f
    }.coerceIn(0f, 1f)

    // Calculate today's trees planted
    val todayStart = remember {
        val cal = java.util.Calendar.getInstance()
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        cal.timeInMillis
    }
    val todayLogs = studyLogs.filter { it.timestamp >= todayStart }
    val treesGrownToday = todayLogs.count { it.durationSeconds >= 600 } + if (stage >= 2) 1 else 0
    val totalFocusMinutesToday = (todayLogs.sumOf { it.durationSeconds } + secondsElapsed) / 60

    // Rotating Quote based on minute
    val quoteIndex = remember(secondsElapsed / 120) {
        (secondsElapsed / 120) % MOTIVATIONAL_STUDY_QUOTES.size
    }

    val cardBg = if (isDark) Color(0xFF0F172A) else Color.White
    val cardBorder = if (isDark) Color(0x3338BDF8) else Color(0xFFE2E8F0)
    val textColor = if (isDark) Color.White else Color(0xFF0F172A)
    val subTextColor = if (isDark) Color(0x99FFFFFF) else Color(0xFF64748B)

    // Breathing and swaying animation
    val infiniteTransition = rememberInfiniteTransition(label = "TreeSway")
    val swayAngleState = infiniteTransition.animateFloat(
        initialValue = -3f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sway"
    )
    val pulseScaleState = infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        modifier = modifier
            .fillMaxWidth()
            .border(1.2.dp, cardBorder, RoundedCornerShape(28.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(if (isDark) Color(0x2610B981) else Color(0xFFDCFCE7)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🌿", fontSize = 18.sp)
                    }
                    Column {
                        Text(
                            text = "Forest Focus Garden",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = textColor
                        )
                        Text(
                            text = if (isRunning) "Absorbing focus energy..." else "Pick your tree & start focus",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isRunning) Color(0xFF10B981) else subTextColor,
                            fontWeight = if (isRunning) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }
                }

                // Ambient Audio Quick Toggle
                IconButton(
                    onClick = {
                        if (isSoundActive) {
                            AmbientStudySoundManager.stopSound()
                            isSoundActive = false
                        } else {
                            AmbientStudySoundManager.startSound(currentSoundName, soundVolume)
                            isSoundActive = true
                        }
                    },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(if (isSoundActive) Color(0xFF10B981) else if (isDark) Color(0x1AFFFFFF) else Color(0xFFF1F5F9))
                        .border(1.dp, if (isSoundActive) Color(0xFF34D399) else cardBorder, CircleShape)
                ) {
                    Icon(
                        imageVector = if (isSoundActive) Icons.Default.MusicNote else Icons.Default.MusicOff,
                        contentDescription = "Ambient Lo-Fi",
                        tint = if (isSoundActive) Color.White else subTextColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Interactive Growing Canvas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.verticalGradient(
                            if (isDark) listOf(Color(0xFF0B132B), Color(0xFF1C2541), Color(0xFF0F172A))
                            else listOf(Color(0xFFE0F2FE), Color(0xFFF0FDF4), Color(0xFFDCFCE7))
                        )
                    )
                    .border(1.dp, if (isDark) Color(0x1FFFFFFF) else Color(0x3310B981), RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    val groundY = h * 0.82f

                    // Draw Ground soil / hill
                    val groundPath = Path().apply {
                        moveTo(0f, groundY + 10f)
                        quadraticTo(w * 0.5f, groundY - 8f, w, groundY + 10f)
                        lineTo(w, h)
                        lineTo(0f, h)
                        close()
                    }
                    drawPath(
                        path = groundPath,
                        brush = Brush.verticalGradient(
                            listOf(
                                if (isDark) Color(0xFF1E293B) else Color(0xFF86EFAC),
                                if (isDark) Color(0xFF0F172A) else Color(0xFF4ADE80)
                            )
                        )
                    )

                    // Center Tree Coordinates
                    val centerX = w * 0.5f
                    val currentSway = if (isRunning) swayAngleState.value else 0f
                    val currentScale = if (isRunning) pulseScaleState.value else 1f

                    when (stage) {
                        0 -> {
                            // Seed stage
                            drawCircle(
                                color = if (isDark) Color(0xFFD97706) else Color(0xFF92400E),
                                radius = 7f * currentScale,
                                center = Offset(centerX, groundY - 2f)
                            )
                            // Soft soil glow
                            drawCircle(
                                color = selectedPlant.primaryColor.copy(alpha = 0.25f),
                                radius = 18f * currentScale,
                                center = Offset(centerX, groundY - 2f)
                            )
                        }
                        1 -> {
                            // Sprout stage (Tiny stem + 2 tiny leaves)
                            val stemTopY = groundY - 24f
                            drawLine(
                                color = selectedPlant.secondaryColor,
                                start = Offset(centerX, groundY),
                                end = Offset(centerX + currentSway, stemTopY),
                                strokeWidth = 5f
                            )
                            // Left leaf
                            drawOval(
                                color = selectedPlant.primaryColor,
                                topLeft = Offset(centerX - 14f + currentSway, stemTopY - 4f),
                                size = Size(14f, 8f)
                            )
                            // Right leaf
                            drawOval(
                                color = selectedPlant.primaryColor,
                                topLeft = Offset(centerX + currentSway, stemTopY - 8f),
                                size = Size(14f, 8f)
                            )
                        }
                        2 -> {
                            // Young Sapling
                            val trunkHeight = 55f
                            val topY = groundY - trunkHeight
                            // Trunk
                            drawLine(
                                color = if (selectedPlant.id == "sakura") Color(0xFF78350F) else Color(0xFF573312),
                                start = Offset(centerX, groundY),
                                end = Offset(centerX + currentSway * 1.5f, topY),
                                strokeWidth = 8f
                            )
                            // Foliage Cluster
                            drawCircle(
                                brush = Brush.radialGradient(
                                    listOf(selectedPlant.primaryColor, selectedPlant.secondaryColor),
                                    center = Offset(centerX + currentSway * 1.5f, topY - 15f),
                                    radius = 32f
                                ),
                                radius = 28f * currentScale,
                                center = Offset(centerX + currentSway * 1.5f, topY - 15f)
                            )
                        }
                        3, 4 -> {
                            // Full Bloomed / Ancient Tree
                            val trunkHeight = if (stage == 4) 80f else 70f
                            val topY = groundY - trunkHeight
                            
                            // Glowing aura for Stage 4
                            if (stage == 4) {
                                drawCircle(
                                    color = Color(0xFFFBBF24).copy(alpha = 0.22f),
                                    radius = 65f * currentScale,
                                    center = Offset(centerX + currentSway * 2f, topY - 25f)
                                )
                            }

                            // Strong Trunk with branches
                            val trunkColor = if (selectedPlant.id == "sakura") Color(0xFF451A03) else Color(0xFF3E2723)
                            drawLine(
                                color = trunkColor,
                                start = Offset(centerX, groundY),
                                end = Offset(centerX + currentSway * 2f, topY),
                                strokeWidth = if (stage == 4) 14f else 11f
                            )
                            // Left branch
                            drawLine(
                                color = trunkColor,
                                start = Offset(centerX + currentSway, topY + 25f),
                                end = Offset(centerX - 24f + currentSway, topY + 5f),
                                strokeWidth = 6f
                            )
                            // Right branch
                            drawLine(
                                color = trunkColor,
                                start = Offset(centerX + currentSway, topY + 30f),
                                end = Offset(centerX + 26f + currentSway, topY + 10f),
                                strokeWidth = 6f
                            )

                            // Triple Foliage Spheres
                            val foliageRadius = if (stage == 4) 42f else 35f
                            // Center canopy
                            drawCircle(
                                brush = Brush.radialGradient(
                                    listOf(
                                        if (stage == 4) Color(0xFFFDE047) else selectedPlant.primaryColor,
                                        selectedPlant.secondaryColor
                                    ),
                                    center = Offset(centerX + currentSway * 2f, topY - 25f),
                                    radius = foliageRadius + 10f
                                ),
                                radius = foliageRadius * currentScale,
                                center = Offset(centerX + currentSway * 2f, topY - 25f)
                            )
                            // Left canopy
                            drawCircle(
                                color = selectedPlant.primaryColor.copy(alpha = 0.9f),
                                radius = (foliageRadius * 0.75f) * currentScale,
                                center = Offset(centerX - 30f + currentSway * 1.5f, topY - 10f)
                            )
                            // Right canopy
                            drawCircle(
                                color = selectedPlant.primaryColor.copy(alpha = 0.9f),
                                radius = (foliageRadius * 0.75f) * currentScale,
                                center = Offset(centerX + 30f + currentSway * 1.5f, topY - 10f)
                            )

                            // Floating Golden Sparkles if Max Stage
                            if (stage == 4) {
                                for (i in 0..5) {
                                    val angle = (i * 60) * Math.PI / 180.0
                                    val sparkX = centerX + currentSway * 2f + (cos(angle) * 55.0).toFloat()
                                    val sparkY = topY - 25f + (sin(angle) * 45.0).toFloat()
                                    drawCircle(
                                        color = Color.White,
                                        radius = 2.5f * currentScale,
                                        center = Offset(sparkX, sparkY)
                                    )
                                }
                            }
                        }
                    }
                }

                // Stage Floating Badge
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isDark) Color(0xCC0F172A) else Color(0xEEFFFFFF))
                        .border(1.dp, if (isDark) Color(0x33FFFFFF) else Color(0x1F000000), RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = stageTitles[stage],
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                }

                // Plant Emojis Badge
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                        .clip(CircleShape)
                        .background(if (isDark) Color(0xCC0F172A) else Color(0xEEFFFFFF))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "${selectedPlant.emoji} ${selectedPlant.name}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Stage Progress Bar
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (stage == 4) "🌟 Max Tree Level Achieved!" else "Next Stage Progress",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = textColor
                    )
                    Text(
                        text = "${(stageProgress * 100).toInt()}%",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF10B981)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                FireworksProgressBar(
                    progress = stageProgress,
                    height = 8.dp,
                    gradientColors = listOf(Color(0xFF10B981), Color(0xFF34D399), Color(0xFF00F5A0)),
                    sparkColor = Color(0xFF34D399),
                    isDark = isDark
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Plant Species Selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Plant Species",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = subTextColor
                )
                Text(
                    text = if (isRunning) "Locked during active session" else "Tap to choose",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isRunning) Color(0xFFF59E0B) else subTextColor
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AVAILABLE_PLANTS.forEach { plant ->
                    val isSelected = plant.id == selectedPlantId
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                if (isSelected) plant.primaryColor.copy(alpha = if (isDark) 0.3f else 0.15f)
                                else if (isDark) Color(0x0DFFFFFF) else Color(0xFFF8FAFC)
                            )
                            .border(
                                width = if (isSelected) 1.8.dp else 1.dp,
                                color = if (isSelected) plant.primaryColor else if (isDark) Color(0x1AFFFFFF) else Color(0xFFE2E8F0),
                                shape = RoundedCornerShape(14.dp)
                            )
                            .clickable(enabled = !isRunning) {
                                selectedPlantId = plant.id
                            }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(plant.emoji, fontSize = 20.sp)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = plant.name.split(" ").last(),
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) textColor else subTextColor
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Sound Controls Expandable Section
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (isDark) Color(0x1AFFFFFF) else Color(0xFFF8FAFC))
                    .border(1.dp, cardBorder, RoundedCornerShape(16.dp))
                    .clickable { showSoundControls = !showSoundControls }
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = when (currentSoundName) {
                            "Rain" -> "🌧️"
                            "Brown Noise" -> "☕"
                            "Ocean Waves" -> "🌊"
                            else -> "🔔"
                        },
                        fontSize = 16.sp
                    )
                    Column {
                        Text(
                            text = "Lo-Fi Study Ambience: $currentSoundName",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = textColor
                        )
                        Text(
                            text = if (isSoundActive) "Playing in background (Offline synth)" else "Tap to adjust & change sound",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isSoundActive) Color(0xFF10B981) else subTextColor
                        )
                    }
                }
                Text(
                    text = if (showSoundControls) "▲" else "▼",
                    fontSize = 12.sp,
                    color = subTextColor
                )
            }

            if (showSoundControls) {
                Spacer(modifier = Modifier.height(12.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isDark) Color(0x1A0F172A) else Color(0xFFF1F5F9))
                        .padding(14.dp)
                ) {
                    Text(
                        text = "Choose Focus Ambience",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val sounds = listOf("Rain", "Brown Noise", "Ocean Waves", "Zen Alpha (432Hz)")
                        sounds.forEach { sName ->
                            val isSel = sName == currentSoundName
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSel) Color(0xFF10B981) else if (isDark) Color(0x26FFFFFF) else Color.White)
                                    .clickable {
                                        currentSoundName = sName
                                        if (isSoundActive) {
                                            AmbientStudySoundManager.startSound(sName, soundVolume)
                                        }
                                    }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = sName.split(" ").first(),
                                    fontSize = 11.sp,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSel) Color.White else textColor
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Volume Slider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = "Volume",
                            tint = subTextColor,
                            modifier = Modifier.size(18.dp)
                        )
                        Slider(
                            value = soundVolume,
                            onValueChange = {
                                soundVolume = it
                                AmbientStudySoundManager.setVolume(it)
                            },
                            valueRange = 0f..1f,
                            modifier = Modifier.weight(1f),
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFF10B981),
                                activeTrackColor = Color(0xFF10B981),
                                inactiveTrackColor = if (isDark) Color(0x26FFFFFF) else Color(0xFFCBD5E1)
                            )
                        )
                        Text(
                            text = "${(soundVolume * 100).toInt()}%",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = textColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Forest Stats Bar (Today's Harvest)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (isDark) Color(0x1F10B981) else Color(0xFFECFDF5))
                    .border(1.dp, if (isDark) Color(0x3310B981) else Color(0x6634D399), RoundedCornerShape(16.dp))
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "🌳 $treesGrownToday Trees",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = if (isDark) Color(0xFF34D399) else Color(0xFF065F46)
                    )
                    Text(
                        text = "Grown Today",
                        fontSize = 11.sp,
                        color = subTextColor
                    )
                }

                Box(modifier = Modifier.height(24.dp).width(1.dp).background(cardBorder))

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "⏱️ ${totalFocusMinutesToday}m Focus",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = if (isDark) Color(0xFF38BDF8) else Color(0xFF0369A1)
                    )
                    Text(
                        text = "Absorbed Energy",
                        fontSize = 11.sp,
                        color = subTextColor
                    )
                }

                Box(modifier = Modifier.height(24.dp).width(1.dp).background(cardBorder))

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "✨ 100%",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = if (isDark) Color(0xFFFBBF24) else Color(0xFFB45309)
                    )
                    Text(
                        text = "Forest Health",
                        fontSize = 11.sp,
                        color = subTextColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Motivational Study Quote
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (isDark) Color(0x0DFFFFFF) else Color(0xFFF8FAFC))
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = MOTIVATIONAL_STUDY_QUOTES[quoteIndex],
                    fontSize = 12.sp,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    color = subTextColor,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
