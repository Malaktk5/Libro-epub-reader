package app.libro.reader.ui

import android.app.Activity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.os.LocaleListCompat
import androidx.core.view.WindowCompat
import app.libro.reader.data.AppThemes
import app.libro.reader.data.Langs

data class ThemeInfo(val dark: Boolean, val sepia: Boolean)

val LocalThemeInfo = compositionLocalOf { ThemeInfo(dark = false, sepia = false) }

private val Teal = Color(0xFF2F5D62)

private val LightScheme = lightColorScheme(
    primary = Teal,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD3E6E8),
    onPrimaryContainer = Color(0xFF0E2A2D),
    secondary = Color(0xFFB4642F),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF5DFCF),
    onSecondaryContainer = Color(0xFF3A1D08),
    background = Color(0xFFFBF8F3),
    onBackground = Color(0xFF1C1B19),
    surface = Color(0xFFFBF8F3),
    onSurface = Color(0xFF1C1B19),
    surfaceVariant = Color(0xFFECE6DA),
    onSurfaceVariant = Color(0xFF4C4A44),
    surfaceContainer = Color(0xFFF3EEE4),
    surfaceContainerHigh = Color(0xFFEDE7DB),
    surfaceContainerHighest = Color(0xFFE6DFD1),
    outline = Color(0xFF7D7A71),
    outlineVariant = Color(0xFFD3CDBF),
)

private val DarkScheme = darkColorScheme(
    primary = Color(0xFF8FCBD2),
    onPrimary = Color(0xFF00363B),
    primaryContainer = Color(0xFF1F4A4F),
    onPrimaryContainer = Color(0xFFCFEBEE),
    secondary = Color(0xFFEBB08A),
    onSecondary = Color(0xFF482000),
    secondaryContainer = Color(0xFF633418),
    onSecondaryContainer = Color(0xFFFFDBC7),
    background = Color(0xFF121413),
    onBackground = Color(0xFFE4E3DE),
    surface = Color(0xFF121413),
    onSurface = Color(0xFFE4E3DE),
    surfaceVariant = Color(0xFF2A2D2C),
    onSurfaceVariant = Color(0xFFC3C6C2),
    surfaceContainer = Color(0xFF1B1E1D),
    surfaceContainerHigh = Color(0xFF242726),
    surfaceContainerHighest = Color(0xFF2E3130),
    outline = Color(0xFF8D918D),
    outlineVariant = Color(0xFF3D403F),
)

private val SepiaScheme = lightColorScheme(
    primary = Color(0xFF7A4A22),
    onPrimary = Color(0xFFFFF8EC),
    primaryContainer = Color(0xFFEBD3AE),
    onPrimaryContainer = Color(0xFF331B05),
    secondary = Color(0xFF5E6B3D),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDDE3BF),
    onSecondaryContainer = Color(0xFF1B2208),
    background = Color(0xFFF4ECD8),
    onBackground = Color(0xFF3F3121),
    surface = Color(0xFFF4ECD8),
    onSurface = Color(0xFF3F3121),
    surfaceVariant = Color(0xFFE6DABD),
    onSurfaceVariant = Color(0xFF5A4A35),
    surfaceContainer = Color(0xFFEDE2C8),
    surfaceContainerHigh = Color(0xFFE7DBBD),
    surfaceContainerHighest = Color(0xFFE0D2B1),
    outline = Color(0xFF857559),
    outlineVariant = Color(0xFFD2C4A2),
)

private val UiFont = FontFamily.SansSerif
private val Display = FontFamily.Serif

private val LibroTypography = Typography(
    headlineLarge = TextStyle(fontFamily = Display, fontWeight = FontWeight.SemiBold, fontSize = 34.sp, lineHeight = 40.sp),
    headlineMedium = TextStyle(fontFamily = Display, fontWeight = FontWeight.SemiBold, fontSize = 28.sp, lineHeight = 34.sp),
    titleLarge = TextStyle(fontFamily = Display, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 28.sp),
    titleMedium = TextStyle(fontFamily = UiFont, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp),
    titleSmall = TextStyle(fontFamily = UiFont, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp),
    bodyLarge = TextStyle(fontFamily = UiFont, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = UiFont, fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontFamily = UiFont, fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge = TextStyle(fontFamily = UiFont, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp),
    labelMedium = TextStyle(fontFamily = UiFont, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp),
    labelSmall = TextStyle(fontFamily = UiFont, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 16.sp),
)

@Composable
fun LibroTheme(appTheme: String, content: @Composable () -> Unit) {
    val systemDark = isSystemInDarkTheme()
    val sepia = appTheme == AppThemes.SEPIA
    val dark = when (appTheme) {
        AppThemes.DARK -> true
        AppThemes.LIGHT, AppThemes.SEPIA -> false
        else -> systemDark
    }
    val scheme = when {
        sepia -> SepiaScheme
        dark -> DarkScheme
        else -> LightScheme
    }

    SideEffect {
        val mode = when (appTheme) {
            AppThemes.DARK -> AppCompatDelegate.MODE_NIGHT_YES
            AppThemes.LIGHT, AppThemes.SEPIA -> AppCompatDelegate.MODE_NIGHT_NO
            else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
        if (AppCompatDelegate.getDefaultNightMode() != mode) AppCompatDelegate.setDefaultNightMode(mode)
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !dark
            controller.isAppearanceLightNavigationBars = !dark
        }
    }

    CompositionLocalProvider(LocalThemeInfo provides ThemeInfo(dark, sepia)) {
        MaterialTheme(colorScheme = scheme, typography = LibroTypography, content = content)
    }
}

fun applyLanguage(key: String) {
    val tags = if (key == Langs.SYSTEM) LocaleListCompat.getEmptyLocaleList() else LocaleListCompat.forLanguageTags(key)
    if (AppCompatDelegate.getApplicationLocales() != tags) AppCompatDelegate.setApplicationLocales(tags)
}
