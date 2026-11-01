package app.libro.reader.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import app.libro.reader.data.ReaderThemes

/** Colors of a reading surface. Used for the page itself and for the reader's controls. */
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

    val solarLight = ReaderPalette(ReaderThemes.SOLAR_LIGHT, Color(0xFFFDF6E3), Color(0xFF4B636B), Color(0xFF1F6FB2), Color(0x55268BD2), false)
    val solarDark = ReaderPalette(ReaderThemes.SOLAR_DARK, Color(0xFF002B36), Color(0xFF93A1A1), Color(0xFF5AB0E6), Color(0x6659A7DB), true)
    val nord = ReaderPalette(ReaderThemes.NORD, Color(0xFF2E3440), Color(0xFFD8DEE9), Color(0xFF88C0D0), Color(0x6688C0D0), true)
    val gruvbox = ReaderPalette(ReaderThemes.GRUVBOX, Color(0xFF282828), Color(0xFFEBDBB2), Color(0xFF83A598), Color(0x66FABD2F), true)
    val midnight = ReaderPalette(ReaderThemes.MIDNIGHT, Color(0xFF0B1220), Color(0xFFCBD5E1), Color(0xFF7AA2F7), Color(0x667AA2F7), true)
    val forest = ReaderPalette(ReaderThemes.FOREST, Color(0xFF1B2A22), Color(0xFFD3E0D5), Color(0xFF8FD19E), Color(0x668FD19E), true)
    val rose = ReaderPalette(ReaderThemes.ROSE, Color(0xFFFBEFEF), Color(0xFF4A2B33), Color(0xFFB03A5B), Color(0x55D96A8A), false)
    val lavender = ReaderPalette(ReaderThemes.LAVENDER, Color(0xFFF1EEF9), Color(0xFF2E2A44), Color(0xFF5B4BB7), Color(0x558B7AE0), false)

    private fun byKey(key: String): ReaderPalette? = when (key) {
        ReaderThemes.LIGHT -> light
        ReaderThemes.SEPIA -> sepia
        ReaderThemes.GRAY -> gray
        ReaderThemes.DARK -> dark
        ReaderThemes.BLACK -> black
        ReaderThemes.SOLAR_LIGHT -> solarLight
        ReaderThemes.SOLAR_DARK -> solarDark
        ReaderThemes.NORD -> nord
        ReaderThemes.GRUVBOX -> gruvbox
        ReaderThemes.MIDNIGHT -> midnight
        ReaderThemes.FOREST -> forest
        ReaderThemes.ROSE -> rose
        ReaderThemes.LAVENDER -> lavender
        else -> null
    }

    /** The palette of a theme key without any user overrides (used for the picker swatches). */
    fun base(key: String, info: ThemeInfo): ReaderPalette {
        val k = if (key == ReaderThemes.AUTO) info.readerKey else key
        return byKey(k) ?: if (info.dark) dark else light
    }

    /** The palette actually used for reading: the chosen theme plus any --page-* overrides from the custom CSS. */
    fun forReader(key: String, info: ThemeInfo): ReaderPalette {
        val p = base(key, info)
        val css = info.css
        if (css.isEmpty) return p
        val bg = css.color("page-bg") ?: p.bg
        val fg = css.color("page-fg") ?: p.fg
        return p.copy(
            bg = bg,
            fg = fg,
            link = css.color("page-link") ?: p.link,
            selection = css.color("page-selection") ?: p.selection,
            dark = bg.luminance() < 0.5f,
        )
    }
}

/** #RRGGBB or rgba() string for use inside the reader's injected CSS. */
fun Color.css(): String {
    val a = alpha
    val r = (red * 255f + 0.5f).toInt()
    val g = (green * 255f + 0.5f).toInt()
    val b = (blue * 255f + 0.5f).toInt()
    return if (a >= 0.999f) "#%02x%02x%02x".format(r, g, b) else "rgba($r,$g,$b,${"%.2f".format(java.util.Locale.US, a)})"
}
