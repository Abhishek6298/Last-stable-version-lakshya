package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

/**
 * Hardware-Accelerated Gliding Pill Segmented Control.
 * Bypasses layout & measurement phase during animation frames using lambda-based IntOffset,
 * eliminating frame drops and ensuring butter-smooth section shifting.
 */
@Composable
fun <T> AppSegmentedControl(
    items: List<T>,
    selectedItem: T,
    onItemSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
    itemLabel: (T) -> String = { it.toString() },
    itemIcon: ((T) -> ImageVector?)? = null,
    itemEmoji: ((T) -> String?)? = null,
    itemBadge: ((T) -> String?)? = null,
    selectedColor: Color = Color(0xFF6D28D9),
    selectedGradient: List<Color>? = listOf(Color(0xFF7C3AED), Color(0xFF6D28D9)),
    isDark: Boolean = true,
    fontSize: TextUnit = 13.sp,
    contentPadding: PaddingValues = PaddingValues(vertical = 9.dp, horizontal = 8.dp),
    cornerRadius: Dp = 18.dp
) {
    if (items.isEmpty()) return

    val containerBg = if (isDark) Color(0xFF161224) else Color(0xFFF1F5F9)
    val outerBorderColor = if (isDark) Color(0x38818CF8) else Color(0xFFCBD5E1)
    val dividerColor = if (isDark) Color(0x22FFFFFF) else Color(0x20000000)
    val unselectedTextColor = if (isDark) Color(0xCCFFFFFF) else Color(0xFF64748B)

    val selectedIndex = remember(items, selectedItem) {
        val idx = items.indexOf(selectedItem)
        if (idx >= 0) idx else 0
    }

    // High performance spring specs: responsive, crisp, zero lag
    val animatedIndex by animateFloatAsState(
        targetValue = selectedIndex.toFloat(),
        animationSpec = spring(
            dampingRatio = 0.82f,
            stiffness = 900f
        ),
        label = "pillSlideAnimation"
    )

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(cornerRadius),
        color = containerBg,
        border = BorderStroke(1.dp, outerBorderColor)
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .padding(3.5.dp)
        ) {
            val density = LocalDensity.current
            val totalWidthPx = with(density) { maxWidth.toPx() }
            val itemCount = items.size
            val itemWidthPx = if (itemCount > 0) totalWidthPx / itemCount else 0f
            val itemWidthDp = with(density) { itemWidthPx.toDp() }

            // 1. Static Dividers between unselected tabs
            Row(
                modifier = Modifier.matchParentSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (index in 0 until itemCount) {
                    if (index > 0) {
                        val isNearSelected = index == selectedIndex || index == selectedIndex + 1
                        Box(
                            modifier = Modifier.width(itemWidthDp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (!isNearSelected) {
                                Box(
                                    modifier = Modifier
                                        .width(1.dp)
                                        .height(14.dp)
                                        .background(dividerColor)
                                )
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.width(itemWidthDp))
                    }
                }
            }

            // 2. Hardware Accelerated Sliding Active Capsule
            // Using offset lambda avoids re-measurement/re-layout on every animation frame
            Box(
                modifier = Modifier
                    .width(itemWidthDp)
                    .offset {
                        IntOffset(
                            x = (itemWidthPx * animatedIndex).roundToInt(),
                            y = 0
                        )
                    }
                    .clip(RoundedCornerShape(cornerRadius - 3.dp))
                    .then(
                        if (selectedGradient != null) {
                            Modifier.background(Brush.horizontalGradient(selectedGradient))
                        } else {
                            Modifier.background(selectedColor)
                        }
                    )
                    .border(
                        0.5.dp,
                        Color.White.copy(alpha = 0.25f),
                        RoundedCornerShape(cornerRadius - 3.dp)
                    )
                    .padding(contentPadding)
            ) {
                Text(
                    text = " ",
                    fontSize = fontSize,
                    maxLines = 1
                )
            }

            // 3. Touch Targets & Labels
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEachIndexed { index, item ->
                    val isSelected = index == selectedIndex

                    val animatedTextColor by animateColorAsState(
                        targetValue = if (isSelected) Color.White else unselectedTextColor,
                        animationSpec = tween(durationMillis = 150, easing = FastOutSlowInEasing),
                        label = "textColorAnim"
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(cornerRadius - 3.dp))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple(bounded = true, color = Color.White)
                            ) {
                                if (item != selectedItem) {
                                    onItemSelected(item)
                                }
                            }
                            .padding(contentPadding),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            val icon = itemIcon?.invoke(item)
                            val emoji = itemEmoji?.invoke(item)
                            val badge = itemBadge?.invoke(item)

                            if (emoji != null) {
                                Text(
                                    text = emoji,
                                    fontSize = if (items.size >= 4) (fontSize.value - 1f).sp else fontSize,
                                    modifier = Modifier.padding(end = if (items.size >= 4) 2.5.dp else 4.dp)
                                )
                            } else if (icon != null) {
                                androidx.compose.material3.Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = animatedTextColor,
                                    modifier = Modifier
                                        .size(if (items.size >= 4) 14.dp else 16.dp)
                                        .padding(end = if (items.size >= 4) 2.5.dp else 4.dp)
                                )
                            }

                            Text(
                                text = itemLabel(item),
                                color = animatedTextColor,
                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                                fontSize = if (items.size >= 4) (fontSize.value - 1.2f).sp else fontSize,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center
                            )

                            if (badge != null) {
                                Spacer(modifier = Modifier.width(if (items.size >= 4) 2.5.dp else 4.dp))
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (isSelected) Color.White.copy(alpha = 0.28f) else if (isDark) Color(0x28FFFFFF) else Color(0xFFE2E8F0)
                                ) {
                                    Text(
                                        text = badge,
                                        color = animatedTextColor,
                                        fontSize = if (items.size >= 4) 9.sp else 10.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 0.5.dp)
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

/**
 * Scrollable variant of AppSegmentedControl with optimized animation
 */
@Composable
fun <T> AppScrollableSegmentedControl(
    items: List<T>,
    selectedItem: T,
    onItemSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
    itemLabel: (T) -> String = { it.toString() },
    itemEmoji: ((T) -> String?)? = null,
    itemBadge: ((T) -> String?)? = null,
    selectedColor: Color = Color(0xFF6D28D9),
    selectedGradient: List<Color>? = listOf(Color(0xFF7C3AED), Color(0xFF6D28D9)),
    isDark: Boolean = true,
    fontSize: TextUnit = 13.sp,
    cornerRadius: Dp = 18.dp
) {
    if (items.isEmpty()) return

    val containerBg = if (isDark) Color(0xFF161224) else Color(0xFFF1F5F9)
    val outerBorderColor = if (isDark) Color(0x38818CF8) else Color(0xFFCBD5E1)
    val dividerColor = if (isDark) Color(0x28FFFFFF) else Color(0x28000000)
    val unselectedTextColor = if (isDark) Color(0xCCFFFFFF) else Color(0xFF64748B)

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(cornerRadius),
        color = containerBg,
        border = BorderStroke(1.dp, outerBorderColor)
    ) {
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(3.5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val selectedIndex = items.indexOf(selectedItem)
            items.forEachIndexed { index, item ->
                val isSelected = item == selectedItem
                val isPrevUnselected = index > 0 && (index - 1) != selectedIndex && index != selectedIndex

                if (isPrevUnselected) {
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(16.dp)
                            .background(dividerColor)
                    )
                }

                val animatedBgColor by animateColorAsState(
                    targetValue = if (isSelected) selectedColor else Color.Transparent,
                    animationSpec = tween(durationMillis = 150, easing = FastOutSlowInEasing),
                    label = "scrollSegmentBg"
                )

                val itemModifier = Modifier
                    .clip(RoundedCornerShape(cornerRadius - 3.dp))
                    .then(
                        if (isSelected && selectedGradient != null) {
                            Modifier.background(Brush.horizontalGradient(selectedGradient))
                        } else {
                            Modifier.background(animatedBgColor)
                        }
                    )
                    .then(
                        if (isSelected) {
                            Modifier.border(
                                0.5.dp,
                                Color.White.copy(alpha = 0.25f),
                                RoundedCornerShape(cornerRadius - 3.dp)
                            )
                        } else {
                            Modifier
                        }
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(bounded = true, color = Color.White)
                    ) {
                        if (item != selectedItem) {
                            onItemSelected(item)
                        }
                    }
                    .padding(vertical = 9.dp, horizontal = 16.dp)

                Box(
                    modifier = itemModifier,
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        val emoji = itemEmoji?.invoke(item)
                        val badge = itemBadge?.invoke(item)

                        if (emoji != null) {
                            Text(
                                text = emoji,
                                fontSize = fontSize,
                                modifier = Modifier.padding(end = 6.dp)
                            )
                        }

                        Text(
                            text = itemLabel(item),
                            color = if (isSelected) Color.White else unselectedTextColor,
                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                            fontSize = fontSize,
                            maxLines = 1
                        )

                        if (badge != null) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) Color.White.copy(alpha = 0.25f) else if (isDark) Color(0x22FFFFFF) else Color(0xFFE2E8F0)
                            ) {
                                Text(
                                    text = badge,
                                    color = if (isSelected) Color.White else unselectedTextColor,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

