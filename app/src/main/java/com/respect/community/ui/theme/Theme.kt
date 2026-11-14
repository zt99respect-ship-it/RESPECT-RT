package com.respect.community.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

private val RespectDarkColorScheme = darkColorScheme(
    primary = RespectPurple,
    onPrimary = TextPrimary,
    primaryContainer = RespectPurpleDeep,
    onPrimaryContainer = TextPrimary,
    secondary = CyanAccent,
    onSecondary = VoidBlack,
    tertiary = KickEmerald,
    background = VoidBlack,
    onBackground = TextPrimary,
    surface = RespectSurface,
    onSurface = TextPrimary,
    surfaceVariant = DeepObsidian,
    onSurfaceVariant = TextSecondary,
    outline = GlassBorder
)

private val RespectTypography = Typography(
    headlineMedium = TextStyle(
        fontSize = 21.sp,
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = (-0.3).sp,
        color = TextPrimary
    ),
    titleMedium = TextStyle(
        fontSize = 15.sp,
        fontWeight = FontWeight.Bold,
        color = TextPrimary
    ),
    bodyLarge = TextStyle(
        fontSize = 14.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 21.sp,
        color = TextPrimary
    ),
    bodyMedium = TextStyle(
        fontSize = 13.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 19.sp,
        color = TextSecondary
    ),
    labelMedium = TextStyle(
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        color = TextSecondary
    )
)

@Composable
fun RespectCommunityTheme(content: @Composable () -> Unit) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            window.statusBarColor = VoidBlack.toArgb()
            window.navigationBarColor = VoidBlack.toArgb()
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = false
            controller.isAppearanceLightNavigationBars = false
        }
    }

    MaterialTheme(
        colorScheme = RespectDarkColorScheme,
        typography = RespectTypography,
        content = content
    )
}
