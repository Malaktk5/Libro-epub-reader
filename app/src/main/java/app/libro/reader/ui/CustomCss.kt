package app.libro.reader.ui

import androidx.compose.ui.graphics.Color

/**
 * The user's stylesheet.
 *
 * The raw text is injected into every chapter, so any normal CSS styles the book.
 * CSS cannot reach native Android widgets, so colour custom properties declared anywhere in the
 * text are also read here and used to recolour the app itself:
 *
 *   --app-bg  --app-fg  --app-primary  --app-on-primary  --app-secondary  --app-surface  --app-outline
 *   --page-bg --page-fg --page-link    --page-selection
 *
 * Colours may be #RGB, #RGBA, #RRGGBB, #RRGGBBAA, rgb(r,g,b) or rgba(r,g,b,a).
 */
class CustomCss private constructor(val raw: String, private val vars: Map<String, Color>) {
    val isEmpty: Boolean get() = raw.isBlank()
    fun color(name: String): Color? = vars[name]

    override fun equals(other: Any?) = other is CustomCss && other.raw == raw
    override fun hashCode() = raw.hashCode()

    companion object {
        val EMPTY = CustomCss("", emptyMap())

        private val comment = Regex("/\\*.*?\\*/", RegexOption.DOT_MATCHES_ALL)
        private val decl = Regex("--([A-Za-z0-9_-]+)\\s*:\\s*([^;}]+)")

        fun parse(raw: String): CustomCss {
            if (raw.isBlank()) return EMPTY
            val vars = HashMap<String, Color>()
            decl.findAll(raw.replace(comment, "")).forEach { m ->
                val c = parseColor(m.groupValues[2].trim().removeSuffix("!important").trim())
                if (c != null) vars[m.groupValues[1].lowercase()] = c
            }
            return CustomCss(raw, vars)
        }

        private fun parseColor(v: String): Color? {
            val s = v.lowercase()
            if (s.startsWith("#")) {
                val h = s.substring(1)
                if (!h.all { it in '0'..'9' || it in 'a'..'f' }) return null
                val full = when (h.length) {
                    3 -> h.map { "$it$it" }.joinToString("") + "ff"
                    4 -> h.map { "$it$it" }.joinToString("")
                    6 -> h + "ff"
                    8 -> h
                    else -> return null
                }
                val r = full.substring(0, 2).toInt(16)
                val g = full.substring(2, 4).toInt(16)
                val b = full.substring(4, 6).toInt(16)
                val a = full.substring(6, 8).toInt(16)
                return Color(r, g, b, a)
            }
            if (s.startsWith("rgb")) {
                val inner = s.substringAfter("(", "").substringBefore(")", "")
                val parts = inner.split(',', ' ', '/').map { it.trim() }.filter { it.isNotEmpty() }
                if (parts.size < 3) return null
                val r = parts[0].toFloatOrNull() ?: return null
                val g = parts[1].toFloatOrNull() ?: return null
                val b = parts[2].toFloatOrNull() ?: return null
                val a = parts.getOrNull(3)?.let {
                    if (it.endsWith("%")) (it.dropLast(1).toFloatOrNull() ?: return null) / 100f
                    else it.toFloatOrNull() ?: return null
                } ?: 1f
                fun c(x: Float) = x.coerceIn(0f, 255f).toInt()
                return Color(c(r), c(g), c(b), (a.coerceIn(0f, 1f) * 255f + 0.5f).toInt())
            }
            return null
        }
    }
}

/** Starter stylesheet offered by the editor's "Insert example" button. */
const val CSS_EXAMPLE = """/* Recolour the app (native UI): */
:root {
  --app-primary: #e07a5f;
  --page-bg: #1c1b22;
  --page-fg: #e6e1d6;
  --page-link: #f2cc8f;
}

/* Style the book. Use body + !important to win over the reader's own rules. */
body p { text-indent: 1.2em !important; margin: 0 0 .2em !important; }
body h1, body h2, body h3 { color: var(--libro-link) !important; letter-spacing: .02em; }
body blockquote { border-left: 3px solid var(--libro-link); padding-left: 12px; opacity: .85; }
body img { border-radius: 8px; }
"""
