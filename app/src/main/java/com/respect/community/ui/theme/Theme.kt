package com.respect.community.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val RespectDarkScheme = darkColorScheme(
    primary = RespectPurple,
    onPrimary = TextPrimary,
    primaryContainer = RespectPurpleDeep,
    onPrimaryContainer = TextPrimary,
    secondary = RespectPurpleBright,
    onSecondary = VoidBlack,
    background = VoidBlack,
    onBackground = TextPrimary,
    surface = RespectSurface,
    onSurface = TextPrimary,
    surfaceVariant = RespectSurfaceElevated,
    onSurfaceVariant = TextSecondary,
    error = LiveCrimson,
    onError = TextPrimary,
    outline = GlassBorder
)

val RespectTypography = Typography(
    headlineLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 25.sp,
        lineHeight = 32.sp,
        color = TextPrimary
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
        color = TextPrimary
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        color = TextPrimary
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 21.sp,
        color = TextPrimary
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 19.sp,
        color = TextSecondary
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        color = TextPrimary
    )
)

@Composable
fun RespectCommunityTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = RespectDarkScheme,
        typography = RespectTypography,
        content = content
    )
}
