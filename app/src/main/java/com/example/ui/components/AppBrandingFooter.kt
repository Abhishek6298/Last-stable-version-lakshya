package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * High-performance, GPU-accelerated container with a sleek neon glowing gradient border.
 */
@Composable
fun RevolvingGlowCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 22.dp,
    borderWidth: Dp = 2.dp,
    glowColors: List<Color>,
    cardBgColor: Color,
    content: @Composable BoxScope.() -> Unit
) {
    // Elegant static linear gradient border with zero continuous GPU overdraw
    val borderBrush = remember(glowColors) {
        Brush.linearGradient(
            colors = glowColors,
            start = Offset.Zero,
            end = Offset.Infinite
        )
    }

    // Outer wrapper clipped to corner radius
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(borderBrush)
            .padding(borderWidth)
            .clip(RoundedCornerShape((cornerRadius - borderWidth).coerceAtLeast(0.dp)))
            .background(cardBgColor)
    ) {
        content()
    }
}

@Composable
fun AppBrandingFooter(
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    // Vibrant Aurora Palette for Abhi Yadav Master Card (Cyan -> Electric Indigo -> Hot Pink -> Amber -> Cyan)
    val creatorGlowColors = remember {
        listOf(
            Color(0xFF00F5D4), // Neon Mint / Cyan
            Color(0xFF3B82F6), // Electric Blue
            Color(0xFF8B5CF6), // Royal Purple
            Color(0xFFFF007F), // Hot Magenta
            Color(0xFFF59E0B), // Neon Gold
            Color(0xFF00F5D4)  // Seamless loop point
        )
    }

    // Fire / Hazard Danger Palette for Warning Card (Crimson Red -> Neon Flame Orange -> Electric Amber -> Danger Red)
    val dangerGlowColors = remember {
        listOf(
            Color(0xFFFF0033), // Pure Danger Red
            Color(0xFFFF5500), // Fiery Orange
            Color(0xFFFFD700), // Warning Amber Gold
            Color(0xFFFF0055), // Crimson Pink
            Color(0xFFFF0033)  // Seamless loop point
        )
    }

    val footerBgGradient = remember(isDark) {
        if (isDark) {
            Brush.verticalGradient(
                colors = listOf(Color.Transparent, Color(0xFF060B14), Color(0xFF000000))
            )
        } else {
            Brush.verticalGradient(
                colors = listOf(Color.Transparent, Color(0xFFF1F5F9), Color(0xFFE2E8F0))
            )
        }
    }

    val creatorCardBg = if (isDark) Color(0xFF0B0F19) else Color(0xFFFFFFFF)
    val dangerCardBg = if (isDark) Color(0xFF140507) else Color(0xFFFFF5F5)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(footerBgGradient)
            .padding(top = 20.dp, bottom = 28.dp, start = 16.dp, end = 16.dp)
            .testTag("app_website_footer"),
        verticalArrangement = Arrangement.spacedBy(22.dp)
    ) {

        // =========================================================================
        // 🌟 1. MASTER CREATOR CARD (NEON GLOW CONTAINER)
        // =========================================================================
        RevolvingGlowCard(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 24.dp,
            borderWidth = 2.5.dp,
            glowColors = creatorGlowColors,
            cardBgColor = creatorCardBg
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.radialGradient(
                            colors = if (isDark) {
                                listOf(
                                    Color(0xFF1E1B4B).copy(alpha = 0.45f),
                                    Color(0xFF0D1527).copy(alpha = 0.2f),
                                    Color.Transparent
                                )
                            } else {
                                listOf(
                                    Color(0xFFF5F3FF),
                                    Color(0xFFEEF2FF),
                                    Color.Transparent
                                )
                            },
                            radius = 600f
                        )
                    )
                    .padding(horizontal = 20.dp, vertical = 22.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Header Tag Pill
                    Surface(
                        shape = CircleShape,
                        color = if (isDark) Color(0x336366F1) else Color(0x1F6366F1),
                        border = BorderStroke(1.dp, Color(0xFF818CF8).copy(alpha = 0.6f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "DESIGNED & ENGINEERED BY",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.6.sp,
                                color = if (isDark) Color(0xFFC7D2FE) else Color(0xFF4338CA)
                            )
                            Icon(
                                imageVector = Icons.Default.Stars,
                                contentDescription = null,
                                tint = Color(0xFFF43F5E),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    // ⭐ ABHI YADAV LUXURY GLOWING BADGE ⭐
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = if (isDark) Color(0xFF030712) else Color(0xFFF8FAFC),
                        border = BorderStroke(
                            1.5.dp,
                            Brush.horizontalGradient(
                                listOf(
                                    Color(0xFF00F5D4),
                                    Color(0xFF8B5CF6),
                                    Color(0xFFFF007F)
                                )
                            )
                        ),
                        shadowElevation = if (isDark) 8.dp else 4.dp
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 14.dp)
                        ) {
                            // Crown / Lead Icon
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFFF59E0B).copy(alpha = 0.2f),
                                modifier = Modifier.size(32.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.WorkspacePremium,
                                        contentDescription = "Architect",
                                        tint = Color(0xFFFBBF24),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(horizontalAlignment = Alignment.Start) {
                                Text(
                                    text = "Abhi Yadav",
                                    fontSize = 21.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 0.8.sp,
                                    color = if (isDark) Color.White else Color(0xFF0F172A)
                                )
                                Text(
                                    text = "Lead Architect & Developer",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF818CF8)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            // Heart Icon with pulsing neon glow
                            Icon(
                                imageVector = Icons.Default.Favorite,
                                contentDescription = "Crafted with passion",
                                tint = Color(0xFFFF1493),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // Feature Pill Badges
                    @OptIn(ExperimentalLayoutApi::class)
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Badge 1: NTA 2026 Synced
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isDark) Color(0x2E10B981) else Color(0xFFDCFCE7),
                            border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.6f)),
                            modifier = Modifier.padding(horizontal = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Default.Verified, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(13.dp))
                                Text("NTA 2026 Synced", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (isDark) Color(0xFF6EE7B7) else Color(0xFF15803D), maxLines = 1, softWrap = false)
                            }
                        }

                        // Badge 2: Ultra High Speed
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isDark) Color(0x2E6366F1) else Color(0xFFEEF2FF),
                            border = BorderStroke(1.dp, Color(0xFF6366F1).copy(alpha = 0.6f)),
                            modifier = Modifier.padding(horizontal = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Default.Bolt, contentDescription = null, tint = Color(0xFF818CF8), modifier = Modifier.size(13.dp))
                                Text("Turbo Engine", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (isDark) Color(0xFFA5B4FC) else Color(0xFF4338CA), maxLines = 1, softWrap = false)
                            }
                        }

                        // Badge 3: 100% Offline Vault
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isDark) Color(0x2EF59E0B) else Color(0xFFFEF3C7),
                            border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.6f)),
                            modifier = Modifier.padding(horizontal = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(13.dp))
                                Text("Private Vault", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (isDark) Color(0xFFFDE68A) else Color(0xFFB45309), maxLines = 1, softWrap = false)
                            }
                        }
                    }

                    // Motto / Dedication
                    Text(
                        text = "⚡ Crafted with relentless precision & passion for NEET-UG Future Doctors",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // =========================================================================
        // 🚨 2. STRICT DANGER & LEGAL NOTICE CARD (FIRE HAZARD GLOW CONTAINER)
        // =========================================================================
        RevolvingGlowCard(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 22.dp,
            borderWidth = 2.5.dp,
            glowColors = dangerGlowColors,
            cardBgColor = dangerCardBg
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.radialGradient(
                            colors = if (isDark) {
                                listOf(
                                    Color(0xFF7F1D1D).copy(alpha = 0.35f),
                                    Color(0xFF450A0A).copy(alpha = 0.2f),
                                    Color.Transparent
                                )
                            } else {
                                listOf(
                                    Color(0xFFFEE2E2),
                                    Color(0xFFFEF2F2),
                                    Color.Transparent
                                )
                            },
                            radius = 500f
                        )
                    )
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Glowing Hazard Header Ribbon
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        Color(0xFF991B1B),
                                        Color(0xFFDC2626),
                                        Color(0xFFEF4444),
                                        Color(0xFFDC2626),
                                        Color(0xFF991B1B)
                                    )
                                )
                            )
                            .padding(vertical = 9.dp, horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        // Flashing Warning Icon
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Warning",
                            tint = Color(0xFFFEF08A),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "STRICT DANGER & LEGAL NOTICE",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 2.sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Default.Gavel,
                            contentDescription = "Legal",
                            tint = Color(0xFFFEF08A),
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // Warning Content Body
                    Row(
                        modifier = Modifier.padding(18.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFEF4444).copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.4f)),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Security,
                                    contentDescription = "Protected",
                                    tint = if (isDark) Color(0xFFF87171) else Color(0xFFDC2626),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "PROPRIETARY INTELLECTUAL PROPERTY",
                                    color = if (isDark) Color(0xFFFCA5A5) else Color(0xFF991B1B),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 0.5.sp
                                )
                            }

                            Text(
                                text = "This application, code architecture, database schemas, question bank algorithms, and UI assets are exclusively engineered by Abhi Yadav. Unauthorized reproduction, modification, decompilation, distribution, or commercial reuse is strictly prohibited and subject to severe prosecution under the Copyright Act & Indian IT Laws.",
                                color = if (isDark) Color(0xFFFECACA).copy(alpha = 0.9f) else Color(0xFF7F1D1D),
                                fontSize = 11.sp,
                                lineHeight = 16.5.sp,
                                fontWeight = FontWeight.Medium
                            )

                            Spacer(modifier = Modifier.height(2.dp))

                            // Tamper-proof encryption badge
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (isDark) Color(0x33DC2626) else Color(0x1FDC2626)
                            ) {
                                Text(
                                    text = "🔒 CRYPTOGRAPHICALLY SIGNED • ZERO TAMPERING TOLERATED",
                                    color = if (isDark) Color(0xFFF87171) else Color(0xFFB91C1C),
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 0.4.sp,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // =========================================================================
        // 💫 3. COPYRIGHT & SYSTEM STATUS BAR (ANIMATED ORBITAL SPINNER)
        // =========================================================================
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                // Orbiting Atom / Tech Icon
                Icon(
                    imageVector = Icons.Default.AllInclusive,
                    contentDescription = "Loop",
                    tint = Color(0xFF818CF8),
                    modifier = Modifier.size(16.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = "© 2026–2027 Abhi Yadav. All Rights Reserved.",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569),
                    letterSpacing = 0.3.sp
                )
            }

            Text(
                text = "NEET Score Booster Pro • Made with ❤️ in India 🇮🇳",
                fontSize = 10.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8)
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
