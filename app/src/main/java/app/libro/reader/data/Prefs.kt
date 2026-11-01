package app.libro.reader.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object Langs {
    const val SYSTEM = "system"
    const val EN = "en"
    const val FR = "fr"
    const val AR = "ar"
    val all = listOf(EN, FR, AR)
}

object AppThemes {
    const val SYSTEM = "system"
    const val LIGHT = "light"
    const val DARK = "dark"
    const val SEPIA = "sepia"
    val all = listOf(SYSTEM, LIGHT, DARK, SEPIA)
}

object ReaderThemes {
    const val AUTO = "auto"
    const val LIGHT = "light"
    const val SEPIA = "sepia"
    const val GRAY = "gray"
    const val DARK = "dark"
    const val BLACK = "black"
    val all = listOf(AUTO, LIGHT, SEPIA, GRAY, DARK, BLACK)
}

object ReadModes {
    const val PAGED = "paged"
    const val SCROLL = "scroll"
}

object Aligns {
    const val DEFAULT = "default"
    const val START = "start"
    const val JUSTIFY = "justify"
}

object Sorts {
    const val RECENT = "recent"
    const val TITLE = "title"
    const val AUTHOR = "author"
    const val ADDED = "added"
    val all = listOf(RECENT, ADDED, TITLE, AUTHOR)
}

object FontFamilies {
    const val PUBLISHER = "publisher"
    const val LITERATA = "literata"
    const val ATKINSON = "atkinson"
    const val AMIRI = "amiri"
    const val NASKH = "naskh"
    const val SERIF = "serif"
    const val MONO = "mono"
    val all = listOf(PUBLISHER, LITERATA, ATKINSON, AMIRI, NASKH, SERIF, MONO)

    fun css(key: String): String = when (key) {
        LITERATA -> "'Libro Literata','Libro Naskh',serif"
        ATKINSON -> "'Libro Atkinson','Libro Naskh',sans-serif"
        AMIRI -> "'Libro Amiri','Libro Literata',serif"
        NASKH -> "'Libro Naskh','Libro Literata',serif"
        SERIF -> "serif"
        MONO -> "monospace"
        else -> ""
    }
}

data class UiPrefs(
    val onboarded: Boolean = false,
    val language: String = Langs.SYSTEM,
    val appTheme: String = AppThemes.SYSTEM,
    val sort: String = Sorts.RECENT,

    val readerTheme: String = ReaderThemes.AUTO,
    val readerMode: String = ReadModes.SCROLL,
    val fontFamily: String = FontFamilies.LITERATA,

    val fontScale: Int = 100,
    val lineHeight: Float = 1.5f,

    val margin: Int = 20,
    val align: String = Aligns.DEFAULT,
    val hyphenate: Boolean = false,

    val forceStyle: Boolean = true,
    val keepScreenOn: Boolean = true,

    val brightness: Float = -1f,
)

class Prefs(context: Context) {
    private val sp = context.getSharedPreferences("libro_prefs", Context.MODE_PRIVATE)
    private val _state = MutableStateFlow(read())
    val state: StateFlow<UiPrefs> = _state.asStateFlow()
    val current: UiPrefs get() = _state.value

    fun update(transform: (UiPrefs) -> UiPrefs) {
        synchronized(this) {
            val next = transform(_state.value)
            if (next == _state.value) return
            _state.value = next
            write(next)
        }
    }

    private fun read(): UiPrefs {
        val d = UiPrefs()
        return UiPrefs(
            onboarded = sp.getBoolean("onboarded", d.onboarded),
            language = sp.getString("language", d.language) ?: d.language,
            appTheme = sp.getString("appTheme", d.appTheme) ?: d.appTheme,
            sort = sp.getString("sort", d.sort) ?: d.sort,
            readerTheme = sp.getString("readerTheme", d.readerTheme) ?: d.readerTheme,
            readerMode = sp.getString("readerModeV2", d.readerMode) ?: d.readerMode,
            fontFamily = sp.getString("fontFamily", d.fontFamily) ?: d.fontFamily,
            fontScale = sp.getInt("fontScale", d.fontScale).coerceIn(60, 260),
            lineHeight = sp.getFloat("lineHeight", d.lineHeight).coerceIn(1.0f, 2.4f),
            margin = sp.getInt("margin", d.margin).coerceIn(0, 72),
            align = sp.getString("align", d.align) ?: d.align,
            hyphenate = sp.getBoolean("hyphenate", d.hyphenate),
            forceStyle = sp.getBoolean("forceStyle", d.forceStyle),
            keepScreenOn = sp.getBoolean("keepScreenOn", d.keepScreenOn),
            brightness = sp.getFloat("brightness", d.brightness),
        )
    }

    private fun write(p: UiPrefs) {
        sp.edit()
            .putBoolean("onboarded", p.onboarded)
            .putString("language", p.language)
            .putString("appTheme", p.appTheme)
            .putString("sort", p.sort)
            .putString("readerTheme", p.readerTheme)
            .putString("readerModeV2", p.readerMode)
            .putString("fontFamily", p.fontFamily)
            .putInt("fontScale", p.fontScale)
            .putFloat("lineHeight", p.lineHeight)
            .putInt("margin", p.margin)
            .putString("align", p.align)
            .putBoolean("hyphenate", p.hyphenate)
            .putBoolean("forceStyle", p.forceStyle)
            .putBoolean("keepScreenOn", p.keepScreenOn)
            .putFloat("brightness", p.brightness)
            .apply()
    }
}
