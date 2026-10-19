package com.saeed.mirror.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** Same palette as the iOS app (system dark colors + one amber accent). */
object MirrorColors {
    val Accent = Color(0xFFFF9F0A)
    val Background = Color(0xFF000000)
    val Card = Color(0xFF1C1C1E)          // secondarySystemBackground
    val Fill = Color(0xFF2C2C2E)          // tertiarySystemBackground
    val Separator = Color(0xFF38383A)
    val Secondary = Color(0xFF8E8E93)
    val Tertiary = Color(0xFF636366)
    val Green = Color(0xFF30D158)
    val Red = Color(0xFFFF453A)
}

private val scheme = darkColorScheme(
    primary = MirrorColors.Accent,
    onPrimary = Color.Black,
    background = MirrorColors.Background,
    onBackground = Color.White,
    surface = MirrorColors.Background,
    onSurface = Color.White,
    surfaceVariant = MirrorColors.Card,
    onSurfaceVariant = MirrorColors.Secondary,
    surfaceContainer = Color(0xFF161618),
    secondaryContainer = MirrorColors.Fill,
    onSecondaryContainer = Color.White,
    outline = MirrorColors.Separator
)

private val typography = Typography(
    displaySmall = TextStyle(fontSize = 34.sp, lineHeight = 41.sp, fontWeight = FontWeight.Bold),
    titleLarge = TextStyle(fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.Bold),
    titleMedium = TextStyle(fontSize = 17.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 17.sp, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontSize = 15.sp, lineHeight = 20.sp),
    labelLarge = TextStyle(fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold),
    bodySmall = TextStyle(fontSize = 13.sp, lineHeight = 18.sp),
    labelSmall = TextStyle(fontSize = 11.sp, lineHeight = 13.sp, fontWeight = FontWeight.Medium)
)

@Composable
fun MirrorTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = scheme, typography = typography, content = content)
}
