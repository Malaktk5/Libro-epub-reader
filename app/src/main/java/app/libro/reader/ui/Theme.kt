package app.libro.reader.ui

import android.app.Activity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.os.LocaleListCompat
import androidx.core.view.WindowCompat
import app.libro.reader.data.AppThemes
import app.libro.reader.data.Langs
import app.libro.reader.data.ReaderThemes

/** What the app theme resolved to, for places (the reader) that need to know. */
data class ThemeInfo(
    val dark: Boolean,
    val sepia: Boolean,
    /** Reader theme that "Same as app" resolves to. */
    val readerKey: String = ReaderThemes.LIGHT,
    val css: CustomCss = CustomCss.EMPTY,
)

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


/** Builds a full Material scheme from five colours; surfaces and containers are blended from the background. */
private fun buildScheme(dark: Boolean, bg: Long, fg: Long, primary: Long, secondary: Long): ColorScheme {
    val b = Color(bg)
    val f = Color(fg)
    val p = Color(primary)
    val sec = Color(secondary)
    val toward = if (dark) Color.White else Color.Black
    fun surf(a: Float) = lerp(b, toward, a)
    fun on(c: Color) = if (c.luminance() > 0.5f) Color(0xFF101010) else Color.White
    val base = if (dark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = p,
        onPrimary = on(p),
        primaryContainer = lerp(b, p, if (dark) 0.30f else 0.22f),
        onPrimaryContainer = f,
        secondary = sec,
        onSecondary = on(sec),
        secondaryContainer = lerp(b, sec, if (dark) 0.30f else 0.22f),
        onSecondaryContainer = f,
        background = b,
        onBackground = f,
        surface = b,
        onSurface = f,
        surfaceVariant = surf(0.09f),
        onSurfaceVariant = lerp(f, b, 0.25f),
        surfaceContainer = surf(0.04f),
        surfaceContainerHigh = surf(0.07f),
        surfaceContainerHighest = surf(0.10f),
        outline = lerp(f, b, 0.55f),
        outlineVariant = surf(0.16f),
    )
}

private class AppThemeDef(val dark: Boolean, val scheme: ColorScheme, val readerKey: String)

private val appThemeDefs: Map<String, AppThemeDef> = mapOf(
    AppThemes.LIGHT to AppThemeDef(false, LightScheme, ReaderThemes.LIGHT),
    AppThemes.DARK to AppThemeDef(true, DarkScheme, ReaderThemes.DARK),
    AppThemes.SEPIA to AppThemeDef(false, SepiaScheme, ReaderThemes.SEPIA),
    AppThemes.AMOLED to AppThemeDef(true, buildScheme(true, 0xFF000000, 0xFFE0E0E0, 0xFF8FCBD2, 0xFFEBB08A), ReaderThemes.BLACK),
    AppThemes.MIDNIGHT to AppThemeDef(true, buildScheme(true, 0xFF0B1220, 0xFFD6DEEB, 0xFF7AA2F7, 0xFFBB9AF7), ReaderThemes.MIDNIGHT),
    AppThemes.NORD to AppThemeDef(true, buildScheme(true, 0xFF2E3440, 0xFFECEFF4, 0xFF88C0D0, 0xFFEBCB8B), ReaderThemes.NORD),
    AppThemes.GRUVBOX to AppThemeDef(true, buildScheme(true, 0xFF282828, 0xFFEBDBB2, 0xFFFABD2F, 0xFF8EC07C), ReaderThemes.GRUVBOX),
    AppThemes.FOREST to AppThemeDef(true, buildScheme(true, 0xFF17231C, 0xFFDDE8DE, 0xFF8FD19E, 0xFFE0C48C), ReaderThemes.FOREST),
    AppThemes.SOLAR to AppThemeDef(false, buildScheme(false, 0xFFFDF6E3, 0xFF4B636B, 0xFF268BD2, 0xFFB58900), ReaderThemes.SOLAR_LIGHT),
    AppThemes.ROSE to AppThemeDef(false, buildScheme(false, 0xFFFBF1F1, 0xFF3D2329, 0xFFB03A5B, 0xFF7A5C3E), ReaderThemes.ROSE),
    AppThemes.LAVENDER to AppThemeDef(false, buildScheme(false, 0xFFF3F0FA, 0xFF2B2740, 0xFF5B4BB7, 0xFFB5527E), ReaderThemes.LAVENDER),
)

/** Background and text colour of an app theme, for the picker swatches. */
fun appThemeSwatch(key: String): Pair<Color, Color> {
    val d = appThemeDefs[key] ?: return Color(0xFFE9E5DC) to Color(0xFF1C1B19)
    return d.scheme.background to d.scheme.onBackground
}

/** Applies the --app-* variables of the custom CSS on top of a scheme. */
private fun ColorScheme.withCss(css: CustomCss): ColorScheme {
    if (css.isEmpty) return this
    var s = this
    css.color("app-bg")?.let { s = s.copy(background = it, surface = it) }
    css.color("app-fg")?.let { s = s.copy(onBackground = it, onSurface = it, onSurfaceVariant = lerp(it, s.background, 0.25f)) }
    css.color("app-surface")?.let {
        s = s.copy(surfaceContainer = it, surfaceContainerHigh = it, surfaceContainerHighest = it, surfaceVariant = it)
    }
    css.color("app-outline")?.let { s = s.copy(outline = it, outlineVariant = it) }
    css.color("app-primary")?.let {
        s = s.copy(
            primary = it,
            onPrimary = if (it.luminance() > 0.5f) Color(0xFF101010) else Color.White,
            primaryContainer = lerp(s.background, it, 0.25f),
            onPrimaryContainer = s.onBackground,
        )
    }
    css.color("app-on-primary")?.let { s = s.copy(onPrimary = it) }
    css.color("app-secondary")?.let {
        s = s.copy(
            secondary = it,
            onSecondary = if (it.luminance() > 0.5f) Color(0xFF101010) else Color.White,
            secondaryContainer = lerp(s.background, it, 0.25f),
            onSecondaryContainer = s.onBackground,
        )
    }
    return s
}

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
fun LibroTheme(appTheme: String, customCss: String = "", content: @Composable () -> Unit) {
    val systemDark = isSystemInDarkTheme()
    val css = remember(customCss) { CustomCss.parse(customCss) }
    val def = appThemeDefs[appTheme] ?: if (systemDark) appThemeDefs.getValue(AppThemes.DARK) else appThemeDefs.getValue(AppThemes.LIGHT)
    val dark = def.dark
    val scheme = remember(appTheme, systemDark, css) { def.scheme.withCss(css) }
    val barsDark = scheme.background.luminance() < 0.5f

    // Keep AppCompat's night mode in step so dialogs, system bars and the window background match.
    SideEffect {
        val mode = when (appTheme) {
            AppThemes.SYSTEM -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            else -> if (dark) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        }
        if (AppCompatDelegate.getDefaultNightMode() != mode) AppCompatDelegate.setDefaultNightMode(mode)
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !barsDark
            controller.isAppearanceLightNavigationBars = !barsDark
        }
    }

    val info = ThemeInfo(dark = dark, sepia = appTheme == AppThemes.SEPIA, readerKey = def.readerKey, css = css)
    CompositionLocalProvider(LocalThemeInfo provides info) {
        MaterialTheme(colorScheme = scheme, typography = LibroTypography, content = content)
    }
}

/** Applies the in-app language choice using the per-app locale API (works back to Android 5 via AppCompat). */
fun applyLanguage(key: String) {
    val tags = if (key == Langs.SYSTEM) LocaleListCompat.getEmptyLocaleList() else LocaleListCompat.forLanguageTags(key)
    if (AppCompatDelegate.getApplicationLocales() != tags) AppCompatDelegate.setApplicationLocales(tags)
}
