package app.libro.reader.ui.reader

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import app.libro.reader.data.BookEntry
import app.libro.reader.data.FontFamilies
import app.libro.reader.data.LibraryRepository
import app.libro.reader.data.ReadModes
import app.libro.reader.data.UiPrefs
import app.libro.reader.epub.EpubBook
import app.libro.reader.epub.TocEntry
import androidx.compose.ui.graphics.toArgb
import app.libro.reader.ui.ReaderPalette
import app.libro.reader.ui.css
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.nio.charset.Charset

data class ReaderLabels(val prev: String, val next: String, val end: String)

class ReaderController(
    context: Context,
    private val book: EpubBook,
    private val entry: BookEntry,
    private val library: LibraryRepository,
    private val onChrome: () -> Unit,
    private val onPosition: (chapter: Int, display: Double, page: Int, pages: Int, overall: Double) -> Unit,
) {
    private val appContext = context.applicationContext
    private val main = Handler(Looper.getMainLooper())

    val webView: WebView = WebView(context)

    @Volatile private var settingsJson: String = "{}"
    @Volatile private var chapter = entry.chapter.coerceIn(0, book.spine.lastIndex)
    @Volatile private var pendingFraction = entry.fraction
    @Volatile private var pendingAnchor: String? = null
    @Volatile private var lastOverall = entry.progress
    @Volatile private var lastFraction = entry.fraction
    private var loadedChapter = -1
    private var destroyed = false

    private var prefs = UiPrefs()
    private var palette: ReaderPalette? = null
    private var labels = ReaderLabels("Previous chapter", "Next chapter", "The end")

    private val flush = Runnable { persist() }

    init {
        configureWebView()
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun configureWebView() {
        webView.apply {
            isVerticalScrollBarEnabled = false
            isHorizontalScrollBarEnabled = false
            overScrollMode = WebView.OVER_SCROLL_NEVER
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = false
                allowFileAccess = false
                allowContentAccess = false
                blockNetworkLoads = true
                setSupportZoom(false)
                useWideViewPort = true
                loadWithOverviewMode = false
                builtInZoomControls = false
                textZoom = 100
                mediaPlaybackRequiresUserGesture = true
                mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_NEVER_ALLOW
            }
            addJavascriptInterface(Bridge(), "LibroBridge")
            webViewClient = Client()
        }
    }

    fun updateStyle(prefs: UiPrefs, palette: ReaderPalette, labels: ReaderLabels) {
        val first = this.palette == null
        val modeChanged = !first && this.prefs.readerMode != prefs.readerMode
        this.prefs = prefs
        this.palette = palette
        this.labels = labels
        settingsJson = buildSettings()
        webView.setBackgroundColor(palette.bg.toArgb())
        if (first) {
            loadChapter(chapter, pendingFraction, null)
        } else {
            if (modeChanged) {

            }
            webView.evaluateJavascript("window.__lib&&window.__lib.apply()", null)
        }
    }

    private fun buildSettings(): String {
        val p = palette ?: return "{}"
        val o = JSONObject()
        o.put("mode", if (prefs.readerMode == ReadModes.SCROLL) "scroll" else "paged")
        o.put("size", 18.0 * prefs.fontScale / 100.0)
        o.put("lh", prefs.lineHeight.toDouble())
        o.put("margin", prefs.margin)
        o.put("align", prefs.align)
        o.put("hyph", prefs.hyphenate)
        o.put("force", prefs.forceStyle)
        o.put("font", FontFamilies.css(prefs.fontFamily))
        o.put("lang", book.language)
        o.put("bookRtl", book.rtl)
        o.put("prevLabel", labels.prev)
        o.put("nextLabel", labels.next)
        o.put("endLabel", labels.end)
        o.put(
            "theme",
            JSONObject()
                .put("bg", p.bg.css())
                .put("fg", p.fg.css())
                .put("link", p.link.css())
                .put("sel", p.selection.css())
        )
        return o.toString()
    }

    private fun urlFor(path: String): String =
        "https://$HOST/book/" + path.split('/').joinToString("/") { Uri.encode(it) }

    private fun loadChapter(index: Int, fraction: Double, anchor: String?) {
        val i = index.coerceIn(0, book.spine.lastIndex)
        chapter = i
        pendingFraction = fraction
        pendingAnchor = anchor
        loadedChapter = i
        webView.loadUrl(urlFor(book.spine[i].path))
    }

    fun goToToc(e: TocEntry) {
        if (e.spineIndex == loadedChapter && e.fragment != null) {
            val js = "window.__lib&&window.__lib.goAnchor(${JSONObject.quote(e.fragment)})"
            webView.evaluateJavascript(js, null)
        } else if (e.spineIndex == loadedChapter) {
            webView.evaluateJavascript("window.__lib&&window.__lib.gotoFraction(0)", null)
        } else {
            loadChapter(e.spineIndex, 0.0, e.fragment)
        }
    }

    fun seek(progress: Double) {
        val (idx, frac) = book.locate(progress)
        if (idx == loadedChapter) {
            webView.evaluateJavascript("window.__lib&&window.__lib.gotoFraction($frac)", null)
        } else {
            loadChapter(idx, frac, null)
        }
    }

    fun next() = webView.evaluateJavascript("window.__lib&&window.__lib.next()", null)
    fun prev() = webView.evaluateJavascript("window.__lib&&window.__lib.prev()", null)

    private fun persist() {
        library.updateProgress(entry.id, chapter, lastFraction.coerceIn(0.0, 1.0), lastOverall)
    }

    fun saveNow() {
        main.removeCallbacks(flush)
        persist()
    }

    fun destroy() {
        if (destroyed) return
        destroyed = true
        saveNow()
        (webView.parent as? android.view.ViewGroup)?.removeView(webView)
        webView.removeJavascriptInterface("LibroBridge")
        webView.stopLoading()
        webView.destroy()
        book.close()
    }

    inner class Bridge {
        @JavascriptInterface
        fun settings(): String = settingsJson

        @JavascriptInterface
        fun start(): String {
            val o = JSONObject()
            o.put("chapter", chapter)
            o.put("fraction", pendingFraction)
            o.put("anchor", pendingAnchor ?: JSONObject.NULL)
            o.put("hasPrev", chapter > 0)
            o.put("hasNext", chapter < book.spine.lastIndex)
            pendingAnchor = null
            return o.toString()
        }

        @JavascriptInterface
        fun progress(ch: Int, fraction: Double, display: Double, page: Int, pages: Int) {
            if (ch != chapter) return
            var overall = book.overallProgress(ch, fraction)
            if (ch == book.spine.lastIndex && display >= 0.999) overall = 1.0
            lastOverall = overall
            lastFraction = fraction
            main.post {
                if (destroyed) return@post
                onPosition(ch, display, page, pages, overall)
                main.removeCallbacks(flush)
                main.postDelayed(flush, 900)
            }
        }

        @JavascriptInterface
        fun edge(ch: Int, forward: Boolean) {
            main.post {
                if (destroyed || ch != chapter) return@post
                if (forward && chapter < book.spine.lastIndex) loadChapter(chapter + 1, 0.0, null)
                else if (!forward && chapter > 0) loadChapter(chapter - 1, 1.0, null)
            }
        }

        @JavascriptInterface
        fun toggleChrome() {
            main.post { if (!destroyed) onChrome() }
        }
    }

    private inner class Client : WebViewClient() {
        override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
            val uri = request.url
            if (uri.host != HOST) return empty(403, "Forbidden")
            val path = uri.path ?: return empty(404, "Not Found")
            return try {
                when {
                    path == "/__app/reader.js" -> asset("reader.js", "application/javascript")
                    path.startsWith("/__fonts/") -> asset("fonts/" + path.removePrefix("/__fonts/"), "font/ttf")
                    path.startsWith("/book/") -> bookResource(path.removePrefix("/book/"))
                    else -> empty(404, "Not Found")
                }
            } catch (e: Exception) {
                empty(500, "Error")
            }
        }

        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
            val uri = request.url
            if (uri.host == HOST && (uri.path ?: "").startsWith("/book/")) {
                val target = (uri.path ?: "").removePrefix("/book/")
                val idx = book.spineIndexOf(target)
                if (idx >= 0) {
                    val frag = uri.fragment
                    if (idx == loadedChapter && frag != null) {
                        view.evaluateJavascript("window.__lib&&window.__lib.goAnchor(${JSONObject.quote(frag)})", null)
                    } else {
                        loadChapter(idx, 0.0, frag)
                    }
                }
                return true
            }

            return true
        }
    }

    private fun empty(code: Int, reason: String) =
        WebResourceResponse("text/plain", "utf-8", code, reason, emptyMap(), ByteArrayInputStream(ByteArray(0)))

    private fun asset(name: String, mime: String): WebResourceResponse =
        WebResourceResponse(mime, null, 200, "OK", cacheHeaders(), appContext.assets.open(name))

    private fun cacheHeaders() = mapOf("Cache-Control" to "no-store")

    private fun bookResource(path: String): WebResourceResponse {
        val mime = book.mimeFor(path)
        val stream = book.open(path) ?: return empty(404, "Not Found")
        val isDoc = mime == "text/html" || mime == "application/xhtml+xml"
        if (!isDoc) {
            return WebResourceResponse(mime, null, 200, "OK", cacheHeaders(), stream)
        }
        val bytes = stream.use { it.readBytes() }
        val html = injectRuntime(bytes)
        return WebResourceResponse(mime, "utf-8", 200, "OK", cacheHeaders(), ByteArrayInputStream(html))
    }

    private fun injectRuntime(bytes: ByteArray): ByteArray {
        val charset = sniffCharset(bytes)
        var text = String(bytes, charset)

        val headInject =
            "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1, minimum-scale=1, maximum-scale=1, user-scalable=no\"/>" +
                "<style>html{visibility:hidden!important}</style>" +
                "<script>setTimeout(function(){document.documentElement.style.setProperty(" +
                "'visibility','visible','important')},2500)</script>"
        val tailInject = "<script src=\"https://$HOST/__app/reader.js\"></script>"

        val headClose = Regex("</head\\s*>", RegexOption.IGNORE_CASE).find(text)
        text = if (headClose != null) {
            text.substring(0, headClose.range.first) + headInject + text.substring(headClose.range.first)
        } else {
            val body = Regex("<body[\\s>]", RegexOption.IGNORE_CASE).find(text)
            if (body != null) text.substring(0, body.range.first) + headInject + text.substring(body.range.first)
            else text + headInject
        }

        val bodyClose = text.lastIndexOf("</body", ignoreCase = true)
        text = if (bodyClose >= 0) text.substring(0, bodyClose) + tailInject + text.substring(bodyClose)
        else text + tailInject

        return text.toByteArray(Charsets.UTF_8)
    }

    private fun sniffCharset(bytes: ByteArray): Charset {
        val head = String(bytes, 0, minOf(bytes.size, 1024), Charsets.ISO_8859_1)
        val m = Regex("""(?:encoding|charset)\s*=\s*["']?([A-Za-z0-9_\-]+)""", RegexOption.IGNORE_CASE).find(head)
        return try {
            if (m != null) Charset.forName(m.groupValues[1]) else Charsets.UTF_8
        } catch (e: Exception) {
            Charsets.UTF_8
        }
    }

    companion object {
        const val HOST = "epub.local"
    }
}
