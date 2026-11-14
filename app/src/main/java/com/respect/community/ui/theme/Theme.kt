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
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.respect.community.R

val CairoFontFamily = FontFamily(
    Font(R.font.cairo_regular, FontWeight.Normal),
    Font(R.font.cairo_regular, FontWeight.Medium),
    Font(R.font.cairo_bold, FontWeight.SemiBold),
    Font(R.font.cairo_bold, FontWeight.Bold),
    Font(R.font.cairo_bold, FontWeight.ExtraBold)
)

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
    displayLarge = TextStyle(fontFamily = CairoFontFamily, fontSize = 32.sp, fontWeight = FontWeight.Bold, color = TextPrimary),
    displayMedium = TextStyle(fontFamily = CairoFontFamily, fontSize = 26.sp, fontWeight = FontWeight.Bold, color = TextPrimary),
    headlineLarge = TextStyle(fontFamily = CairoFontFamily, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TextPrimary),
    headlineMedium = TextStyle(fontFamily = CairoFontFamily, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary),
    headlineSmall = TextStyle(fontFamily = CairoFontFamily, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary),
    titleLarge = TextStyle(fontFamily = CairoFontFamily, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = TextPrimary),
    titleMedium = TextStyle(fontFamily = CairoFontFamily, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary),
    titleSmall = TextStyle(fontFamily = CairoFontFamily, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary),
    bodyLarge = TextStyle(fontFamily = CairoFontFamily, fontSize = 14.sp, fontWeight = FontWeight.Normal, lineHeight = 22.sp, color = TextPrimary),
    bodyMedium = TextStyle(fontFamily = CairoFontFamily, fontSize = 13.sp, fontWeight = FontWeight.Normal, lineHeight = 20.sp, color = TextSecondary),
    bodySmall = TextStyle(fontFamily = CairoFontFamily, fontSize = 12.sp, fontWeight = FontWeight.Normal, lineHeight = 18.sp, color = TextMuted),
    labelLarge = TextStyle(fontFamily = CairoFontFamily, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary),
    labelMedium = TextStyle(fontFamily = CairoFontFamily, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextSecondary),
    labelSmall = TextStyle(fontFamily = CairoFontFamily, fontSize = 11.sp, fontWeight = FontWeight.Normal, color = TextMuted)
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
