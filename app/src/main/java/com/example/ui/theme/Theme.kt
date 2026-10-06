package com.example.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF5288C1), // Telegram light blue / cyan primary
    onPrimary = Color.White,
    primaryContainer = Color(0xFF1E2C3D),
    onPrimaryContainer = Color(0xFFE2EDF8),
    secondary = Color(0xFF38BDF8), // Sky 400
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF162534),
    onSecondaryContainer = Color.White,
    tertiary = Color(0xFF229ED9), // Telegram accent
    background = Color(0xFF0E1621), // Telegram Dark Blue canvas
    onBackground = Color(0xFFF1F5F9),
    surface = Color(0xFF17212B), // Telegram dark surface
    onSurface = Color(0xFFF1F5F9),
    surfaceVariant = Color(0xFF242F3D),
    onSurfaceVariant = Color(0xFFCBD5E1),
    outline = Color(0x335288C1)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF2481CC), // Telegram Sky Blue
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE3EFF9),
    onPrimaryContainer = Color(0xFF0F365A),
    secondary = Color(0xFF0284C7),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD6E8F7),
    onSecondaryContainer = Color(0xFF0C4A6E),
    tertiary = Color(0xFF0EA5E9),
    background = Color(0xFFE5EFF9), // Telegram light sky blue canvas
    onBackground = Color(0xFF0F172A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFEAF2F8),
    onSurfaceVariant = Color(0xFF334155),
    outline = Color(0xFFCBDDF0)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = false,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            var context = view.context
            while (context is android.content.ContextWrapper) {
                if (context is Activity) break
                context = context.baseContext
            }
            val activity = context as? Activity
            if (activity != null) {
                val window = activity.window
                WindowCompat.setDecorFitsSystemWindows(window, false)
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
                WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
