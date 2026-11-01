package app.libro.reader.ui

import androidx.compose.ui.graphics.Color
import app.libro.reader.data.AppThemes
import app.libro.reader.data.ReaderThemes

data class ReaderPalette(
    val key: String,
    val bg: Color,
    val fg: Color,
    val link: Color,
    val selection: Color,
    val dark: Boolean,
)

object Palettes {
    val light = ReaderPalette(ReaderThemes.LIGHT, Color(0xFFFFFFFF), Color(0xFF1B1B1B), Color(0xFF1F6B73), Color(0x552F8F9D), false)
    val sepia = ReaderPalette(ReaderThemes.SEPIA, Color(0xFFF4ECD8), Color(0xFF4B3A26), Color(0xFF8A4B1C), Color(0x55C99A55), false)
    val gray = ReaderPalette(ReaderThemes.GRAY, Color(0xFF3A3D40), Color(0xFFDADCDD), Color(0xFF8FCBD2), Color(0x668FCBD2), true)
    val dark = ReaderPalette(ReaderThemes.DARK, Color(0xFF16191A), Color(0xFFD6D8D6), Color(0xFF7FC4CC), Color(0x667FC4CC), true)
    val black = ReaderPalette(ReaderThemes.BLACK, Color(0xFF000000), Color(0xFFBDBFBD), Color(0xFF6DB7BF), Color(0x666DB7BF), true)

    fun forReader(key: String, appDark: Boolean, appSepia: Boolean): ReaderPalette = when (key) {
        ReaderThemes.LIGHT -> light
        ReaderThemes.SEPIA -> sepia
        ReaderThemes.GRAY -> gray
        ReaderThemes.DARK -> dark
        ReaderThemes.BLACK -> black
        else -> when {
            appSepia -> sepia
            appDark -> dark
            else -> light
        }
    }

    fun isSepia(appTheme: String) = appTheme == AppThemes.SEPIA
}

fun Color.css(): String {
    val a = alpha
    val r = (red * 255f + 0.5f).toInt()
    val g = (green * 255f + 0.5f).toInt()
    val b = (blue * 255f + 0.5f).toInt()
    return if (a >= 0.999f) "#%02x%02x%02x".format(r, g, b) else "rgba($r,$g,$b,${"%.2f".format(java.util.Locale.US, a)})"
}
