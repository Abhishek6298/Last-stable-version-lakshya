package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Super smooth, GPU-accelerated Liquid Glass Mosaic Backdrop.
 * Features an atmospheric multi-point luminous ambient glow mesh.
 * When frosted GlassCards slide over these soft liquid orbs, they refract
 * vivid cyan, indigo, and violet tones like true frosted liquid glass.
 */
@Composable
fun AppAtmosphericBackground(
    isDark: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val backgroundColor = remember(isDark) {
        if (isDark) Color(0xFF0E1621) else Color(0xFFF8FAFC)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        content()
    }
}

