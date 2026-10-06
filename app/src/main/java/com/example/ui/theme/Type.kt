package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.sp
import com.example.R

// Google Font Provider for Modern Rounded Display Typography (as in AirBeats / modern media apps)
val fontProvider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

// Primary Display Font: Modern Rounded Geometric Sans-Serif
val roundedGeometricFont = GoogleFont("Nunito")

// CookieRun-style Playful, Extra-Rounded, Bouncy Gaming Display Font
val cookieRunFont = GoogleFont("Fredoka")

val AppFontFamily = FontFamily(
    Font(googleFont = roundedGeometricFont, fontProvider = fontProvider, weight = FontWeight.Normal),
    Font(googleFont = roundedGeometricFont, fontProvider = fontProvider, weight = FontWeight.Medium),
    Font(googleFont = roundedGeometricFont, fontProvider = fontProvider, weight = FontWeight.SemiBold),
    Font(googleFont = roundedGeometricFont, fontProvider = fontProvider, weight = FontWeight.Bold),
    Font(googleFont = roundedGeometricFont, fontProvider = fontProvider, weight = FontWeight.ExtraBold),
    Font(googleFont = roundedGeometricFont, fontProvider = fontProvider, weight = FontWeight.Black)
)

// CookieRun Custom Font Family (Playful, energetic, rounded gaming style)
val CookieRunFontFamily = FontFamily(
    Font(googleFont = cookieRunFont, fontProvider = fontProvider, weight = FontWeight.Normal),
    Font(googleFont = cookieRunFont, fontProvider = fontProvider, weight = FontWeight.Medium),
    Font(googleFont = cookieRunFont, fontProvider = fontProvider, weight = FontWeight.SemiBold),
    Font(googleFont = cookieRunFont, fontProvider = fontProvider, weight = FontWeight.Bold),
    Font(googleFont = cookieRunFont, fontProvider = fontProvider, weight = FontWeight.ExtraBold),
    Font(googleFont = cookieRunFont, fontProvider = fontProvider, weight = FontWeight.Black)
)

// CookieRun Typography Preset Styles for Titles, Badges, Banners & Counters
object CookieRunTypography {
    val HeroTitle = TextStyle(
        fontFamily = CookieRunFontFamily,
        fontWeight = FontWeight.Black,
        fontSize = 32.sp,
        lineHeight = 38.sp,
        letterSpacing = (-0.4).sp
    )
    val BigHeading = TextStyle(
        fontFamily = CookieRunFontFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 24.sp,
        lineHeight = 30.sp,
        letterSpacing = (-0.2).sp
    )
    val CardHeader = TextStyle(
        fontFamily = CookieRunFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.sp
    )
    val BadgePill = TextStyle(
        fontFamily = CookieRunFontFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp
    )
    val CounterNumber = TextStyle(
        fontFamily = CookieRunFontFamily,
        fontWeight = FontWeight.Black,
        fontSize = 28.sp,
        lineHeight = 32.sp,
        letterSpacing = (-0.5).sp
    )
    val Subtitle = TextStyle(
        fontFamily = CookieRunFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    )
}

// High-Definition Modern Rounded Geometric Typography
val Typography = Typography(
    displayLarge = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Black,
        fontSize = 32.sp,
        lineHeight = 38.sp,
        letterSpacing = (-0.6).sp
    ),
    displayMedium = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        letterSpacing = (-0.5).sp
    ),
    displaySmall = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 30.sp,
        letterSpacing = (-0.3).sp
    ),
    headlineLarge = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = (-0.2).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
        letterSpacing = (-0.1).sp
    ),
    headlineSmall = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.sp
    ),
    titleLarge = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 17.sp,
        lineHeight = 23.sp,
        letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 21.sp,
        letterSpacing = 0.1.sp
    ),
    titleSmall = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.1.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.15.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 13.5.sp,
        lineHeight = 19.sp,
        letterSpacing = 0.15.sp
    ),
    bodySmall = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.2.sp
    ),
    labelLarge = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.3.sp
    ),
    labelMedium = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.5.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp
    ),
    labelSmall = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 10.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.6.sp
    )
)
