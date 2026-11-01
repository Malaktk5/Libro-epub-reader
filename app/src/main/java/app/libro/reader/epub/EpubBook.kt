package app.libro.reader.epub

import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.io.Closeable
import java.io.File
import java.io.InputStream
import java.net.URLDecoder
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

class EpubException(message: String) : Exception(message)

/** One reading-order document of the book. [size] is its uncompressed byte size, used to weight progress. */
data class SpineItem(val path: String, val size: Long)

/** One table-of-contents row. [spineIndex] is already resolved against the reading order. */
data class TocEntry(
    val title: String,
    val spineIndex: Int,
    val fragment: String?,
    val level: Int,
)

private data class ManifestItem(val id: String, val href: String, val type: String, val props: String)

private class Opf {
    var title = ""
    val authors = ArrayList<String>()
    var language = ""
    var coverId: String? = null
    var tocId: String? = null
    var rtl = false
    val manifest = LinkedHashMap<String, ManifestItem>()
    val spineRefs = ArrayList<String>()
}

/**
 * A parsed, opened EPUB. Keeps the zip file open so the reader can stream resources out of it;
 * call [close] when done.
 */
class EpubBook private constructor(
    private val zip: ZipFile,
    val title: String,
    val author: String,
    val language: String,
    val rtl: Boolean,
    val spine: List<SpineItem>,
    val toc: List<TocEntry>,
    val coverPath: String?,
    private val entries: Map<String, ZipEntry>,
    private val lowerEntries: Map<String, ZipEntry>,
    private val mediaTypes: Map<String, String>,
) : Closeable {

    val totalSize: Long = spine.sumOf { it.size }.coerceAtLeast(1L)

    private val cumulative: LongArray = LongArray(spine.size).also { arr ->
        var running = 0L
        for (i in spine.indices) {
            arr[i] = running
            running += spine[i].size
        }
    }

    /** Bytes of the book that come before the chapter at [index]. */
    fun sizeBefore(index: Int): Long = cumulative.getOrElse(index) { 0L }

    /** Book-level progress (0..1) for a position [fractionInChapter] into chapter [index]. */
    fun overallProgress(index: Int, fractionInChapter: Double): Double {
        val i = index.coerceIn(0, spine.lastIndex)
        val inside = fractionInChapter.coerceIn(0.0, 1.0) * spine[i].size
        return ((cumulative[i] + inside) / totalSize.toDouble()).coerceIn(0.0, 1.0)
    }

    /** Maps a book-level progress (0..1) back to a chapter index and a fraction inside that chapter. */
    fun locate(progress: Double): Pair<Int, Double> {
        val target = progress.coerceIn(0.0, 1.0) * totalSize
        var idx = spine.lastIndex
        for (i in spine.indices) {
            if (target < cumulative[i] + spine[i].size) {
                idx = i
                break
            }
        }
        val size = spine[idx].size.coerceAtLeast(1L)
        return idx to ((target - cumulative[idx]) / size).coerceIn(0.0, 1.0)
    }

    fun spineIndexOf(path: String): Int = spine.indexOfFirst { it.path == path }.let { exact ->
        if (exact >= 0) exact else spine.indexOfFirst { it.path.equals(path, ignoreCase = true) }
    }

    fun open(path: String): InputStream? {
        val entry = entries[path] ?: lowerEntries[path.lowercase(Locale.ROOT)] ?: return null
        return zip.getInputStream(entry)
    }

    fun mimeFor(path: String): String {
        val ext = path.substringAfterLast('.', "").lowercase(Locale.ROOT)
        return when (ext) {
            "xhtml", "xht" -> "application/xhtml+xml"
            "html", "htm" -> "text/html"
            "css" -> "text/css"
            "js" -> "application/javascript"
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "gif" -> "image/gif"
            "webp" -> "image/webp"
            "svg" -> "image/svg+xml"
            "ttf" -> "font/ttf"
            "otf" -> "font/otf"
            "woff" -> "font/woff"
            "woff2" -> "font/woff2"
            "mp3" -> "audio/mpeg"
            "mp4" -> "video/mp4"
            "ncx" -> "application/x-dtbncx+xml"
            else -> mediaTypes[path] ?: "application/octet-stream"
        }
    }

    fun readCover(): ByteArray? = coverPath?.let { p ->
        try {
            open(p)?.use { it.readBytes() }
        } catch (e: Exception) {
            null
        }
    }

    override fun close() {
        try {
            zip.close()
        } catch (_: Exception) {
        }
    }

    companion object {
        private val RTL_LANGS = setOf("ar", "he", "fa", "ur", "ps", "sd", "ug", "yi", "dv")

        fun open(file: File): EpubBook {
            val zip = try {
                ZipFile(file)
            } catch (e: Exception) {
                throw EpubException("Not a zip archive")
            }
            try {
                return build(zip)
            } catch (e: EpubException) {
                zip.close()
                throw e
            } catch (e: Exception) {
                zip.close()
                throw EpubException(e.message ?: "Unreadable EPUB")
            }
        }

        private fun build(zip: ZipFile): EpubBook {
            val entries = HashMap<String, ZipEntry>()
            val lower = HashMap<String, ZipEntry>()
            val en = zip.entries()
            while (en.hasMoreElements()) {
                val e = en.nextElement()
                if (!e.isDirectory) {
                    entries[e.name] = e
                    lower[e.name.lowercase(Locale.ROOT)] = e
                }
            }

            val containerEntry = entries["META-INF/container.xml"]
                ?: lower["meta-inf/container.xml"]
                ?: throw EpubException("Missing container.xml")
            val opfPath = zip.getInputStream(containerEntry).use { parseContainer(it) }
                ?.let { resolvePath("", it) }
                ?: throw EpubException("No package document")
            val opfEntry = entries[opfPath] ?: lower[opfPath.lowercase(Locale.ROOT)]
                ?: throw EpubException("Package document not found")
            val opf = zip.getInputStream(opfEntry).use { parseOpf(it) }
            val opfDir = opfPath.substringBeforeLast('/', "")

            fun exists(path: String) = entries.containsKey(path) || lower.containsKey(path.lowercase(Locale.ROOT))
            fun sizeOf(path: String): Long {
                val e = entries[path] ?: lower[path.lowercase(Locale.ROOT)]
                return (e?.size ?: 1L).coerceAtLeast(1L)
            }

            val mediaTypes = HashMap<String, String>()
            for (item in opf.manifest.values) mediaTypes[resolvePath(opfDir, item.href)] = item.type

            val spine = ArrayList<SpineItem>()
            for (ref in opf.spineRefs) {
                val item = opf.manifest[ref] ?: continue
                val path = resolvePath(opfDir, item.href)
                val looksLikeHtml = item.type.contains("html", true) ||
                    path.substringAfterLast('.', "").lowercase(Locale.ROOT) in setOf("xhtml", "html", "htm", "xht")
                if (looksLikeHtml && exists(path)) spine.add(SpineItem(path, sizeOf(path)))
            }
            if (spine.isEmpty()) throw EpubException("Empty reading order")

            val title = opf.title.ifBlank { "" }
            val author = opf.authors.joinToString(", ")
            val language = opf.language.trim()
            val rtl = opf.rtl || language.substringBefore('-').lowercase(Locale.ROOT) in RTL_LANGS

            val toc = parseToc(zip, entries, lower, opf, opfDir, spine)
            val cover = findCover(zip, opf, opfDir, spine, ::exists, entries, lower)

            return EpubBook(zip, title, author, language, rtl, spine, toc, cover, entries, lower, mediaTypes)
        }

        // ---------------------------------------------------------------- XML helpers

        private fun newParser(stream: InputStream): XmlPullParser {
            val p = Xml.newPullParser()
            try {
                p.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
            } catch (_: Exception) {
            }
            try {
                p.setFeature("http://xmlpull.org/v1/doc/features.html#relaxed", true)
            } catch (_: Exception) {
            }
            p.setInput(stream, null)
            return p
        }

        private fun XmlPullParser.attr(name: String): String? {
            for (i in 0 until attributeCount) {
                if (getAttributeName(i).substringAfter(':') == name) return getAttributeValue(i)
            }
            return null
        }

        private fun XmlPullParser.localName(): String = (name ?: "").substringAfter(':').lowercase(Locale.ROOT)

        private fun XmlPullParser.safeText(): String = try {
            nextText().trim()
        } catch (e: Exception) {
            ""
        }

        // ---------------------------------------------------------------- container + package

        private fun parseContainer(stream: InputStream): String? {
            val p = newParser(stream)
            var ev = p.eventType
            while (ev != XmlPullParser.END_DOCUMENT) {
                if (ev == XmlPullParser.START_TAG && p.localName() == "rootfile") {
                    val path = p.attr("full-path")
                    if (!path.isNullOrBlank()) return path
                }
                ev = p.next()
            }
            return null
        }

        private fun parseOpf(stream: InputStream): Opf {
            val p = newParser(stream)
            val opf = Opf()
            var inMetadata = false
            var ev = p.eventType
            while (ev != XmlPullParser.END_DOCUMENT) {
                if (ev == XmlPullParser.START_TAG) {
                    when (p.localName()) {
                        "metadata" -> inMetadata = true
                        "title" -> if (inMetadata && opf.title.isEmpty()) opf.title = p.safeText()
                        "creator" -> if (inMetadata) {
                            val t = p.safeText()
                            if (t.isNotBlank()) opf.authors.add(t)
                        }
                        "language" -> if (inMetadata && opf.language.isEmpty()) opf.language = p.safeText()
                        "meta" -> if (p.attr("name") == "cover") opf.coverId = p.attr("content")
                        "item" -> {
                            val id = p.attr("id")
                            val href = p.attr("href")
                            if (id != null && href != null) {
                                opf.manifest[id] = ManifestItem(
                                    id, href, p.attr("media-type") ?: "", p.attr("properties") ?: ""
                                )
                            }
                        }
                        "spine" -> {
                            opf.tocId = p.attr("toc")
                            opf.rtl = p.attr("page-progression-direction").equals("rtl", true)
                        }
                        "itemref" -> p.attr("idref")?.let { opf.spineRefs.add(it) }
                    }
                } else if (ev == XmlPullParser.END_TAG && p.localName() == "metadata") {
                    inMetadata = false
                }
                ev = p.next()
            }
            return opf
        }

        // ---------------------------------------------------------------- table of contents

        private fun parseToc(
            zip: ZipFile,
            entries: Map<String, ZipEntry>,
            lower: Map<String, ZipEntry>,
            opf: Opf,
            opfDir: String,
            spine: List<SpineItem>,
        ): List<TocEntry> {
            fun entryFor(path: String) = entries[path] ?: lower[path.lowercase(Locale.ROOT)]

            fun toEntries(raw: List<RawToc>, baseDir: String): List<TocEntry> {
                val out = ArrayList<TocEntry>()
                for (r in raw) {
                    if (r.title.isBlank()) continue
                    val path = resolvePath(baseDir, r.href)
                    val idx = spine.indexOfFirst { it.path.equals(path, ignoreCase = true) }
                    if (idx < 0) continue
                    val frag = r.href.substringAfter('#', "").takeIf { it.isNotEmpty() }?.let { decodeUrl(it) }
                    out.add(TocEntry(r.title.trim(), idx, frag, r.level))
                }
                return out
            }

            // EPUB 3 navigation document first.
            val navItem = opf.manifest.values.firstOrNull { it.props.split(' ').contains("nav") }
            if (navItem != null) {
                val navPath = resolvePath(opfDir, navItem.href)
                val e = entryFor(navPath)
                if (e != null) {
                    val raw = try {
                        zip.getInputStream(e).use { parseNav(it) }
                    } catch (ex: Exception) {
                        emptyList()
                    }
                    val result = toEntries(raw, navPath.substringBeforeLast('/', ""))
                    if (result.isNotEmpty()) return result
                }
            }

            // EPUB 2 NCX.
            val ncxItem = opf.tocId?.let { opf.manifest[it] }
                ?: opf.manifest.values.firstOrNull { it.type == "application/x-dtbncx+xml" }
            if (ncxItem != null) {
                val ncxPath = resolvePath(opfDir, ncxItem.href)
                val e = entryFor(ncxPath)
                if (e != null) {
                    val raw = try {
                        zip.getInputStream(e).use { parseNcx(it) }
                    } catch (ex: Exception) {
                        emptyList()
                    }
                    val result = toEntries(raw, ncxPath.substringBeforeLast('/', ""))
                    if (result.isNotEmpty()) return result
                }
            }

            // Fallback: one untitled row per spine item (the UI shows "Chapter N").
            return spine.indices.map { TocEntry("", it, null, 0) }
        }

        private class RawToc(val title: String, val href: String, val level: Int)

        private fun parseNav(stream: InputStream): List<RawToc> {
            val p = newParser(stream)
            val out = ArrayList<RawToc>()
            var inToc = false
            var olDepth = 0
            var inAnchor = false
            var href = ""
            val text = StringBuilder()
            var ev = p.eventType
            while (ev != XmlPullParser.END_DOCUMENT) {
                when (ev) {
                    XmlPullParser.START_TAG -> {
                        val n = p.localName()
                        if (n == "nav" && !inToc) {
                            val type = p.attr("type") ?: ""
                            if (type.split(' ').contains("toc")) inToc = true
                        } else if (inToc && n == "ol") {
                            olDepth++
                        } else if (inToc && n == "a") {
                            inAnchor = true
                            href = p.attr("href") ?: ""
                            text.setLength(0)
                        }
                    }
                    XmlPullParser.TEXT -> if (inAnchor) text.append(p.text)
                    XmlPullParser.END_TAG -> {
                        val n = p.localName()
                        if (inToc && n == "a" && inAnchor) {
                            inAnchor = false
                            if (href.isNotEmpty()) {
                                out.add(RawToc(text.toString().replace(Regex("\\s+"), " "), href, (olDepth - 1).coerceAtLeast(0)))
                            }
                        } else if (inToc && n == "ol") {
                            olDepth--
                        } else if (inToc && n == "nav") {
                            return out
                        }
                    }
                }
                ev = p.next()
            }
            return out
        }

        private class NcxPoint(val level: Int) {
            var label = ""
            var src = ""
        }

        private fun parseNcx(stream: InputStream): List<RawToc> {
            val p = newParser(stream)
            // navLabel and content come before any nested navPoint, so a point is complete by the time
            // its children start; adding points when they open keeps document order.
            val all = ArrayList<NcxPoint>()
            val open = ArrayList<NcxPoint>()
            var inLabel = false
            var ev = p.eventType
            while (ev != XmlPullParser.END_DOCUMENT) {
                when (ev) {
                    XmlPullParser.START_TAG -> when (p.localName()) {
                        "navpoint" -> {
                            val point = NcxPoint(open.size)
                            all.add(point)
                            open.add(point)
                        }
                        "navlabel" -> inLabel = true
                        "text" -> if (inLabel && open.isNotEmpty()) {
                            val t = p.safeText()
                            if (open.last().label.isEmpty()) open.last().label = t
                        }
                        "content" -> if (open.isNotEmpty() && open.last().src.isEmpty()) {
                            open.last().src = p.attr("src") ?: ""
                        }
                    }
                    XmlPullParser.END_TAG -> when (p.localName()) {
                        "navlabel" -> inLabel = false
                        "navpoint" -> if (open.isNotEmpty()) open.removeAt(open.size - 1)
                    }
                }
                ev = p.next()
            }
            return all.filter { it.src.isNotEmpty() }.map { RawToc(it.label, it.src, it.level) }
        }

        // ---------------------------------------------------------------- cover

        private fun findCover(
            zip: ZipFile,
            opf: Opf,
            opfDir: String,
            spine: List<SpineItem>,
            exists: (String) -> Boolean,
            entries: Map<String, ZipEntry>,
            lower: Map<String, ZipEntry>,
        ): String? {
            fun pathOf(item: ManifestItem) = resolvePath(opfDir, item.href)
            val images = opf.manifest.values.filter { it.type.startsWith("image/") }

            val byProperty = opf.manifest.values.firstOrNull { it.props.split(' ').contains("cover-image") }
            val byMeta = opf.coverId?.let { opf.manifest[it] }?.takeIf { it.type.startsWith("image/") }
            val byName = images.firstOrNull { it.id.contains("cover", true) || it.href.contains("cover", true) }

            val picked = byProperty ?: byMeta ?: byName
            if (picked != null) {
                val p = pathOf(picked)
                if (exists(p)) return p
            }

            // Last resort: the first image referenced by the first reading-order document.
            try {
                val first = spine.first().path
                val e = entries[first] ?: lower[first.lowercase(Locale.ROOT)] ?: return null
                val head = zip.getInputStream(e).use { s ->
                    val buf = ByteArray(32 * 1024)
                    val n = s.read(buf)
                    if (n > 0) String(buf, 0, n, Charsets.UTF_8) else ""
                }
                val m = Regex("""(?:src|href)\s*=\s*["']([^"']+\.(?:jpe?g|png|gif|webp))["']""", RegexOption.IGNORE_CASE)
                    .find(head)
                if (m != null) {
                    val p = resolvePath(first.substringBeforeLast('/', ""), m.groupValues[1])
                    if (exists(p)) return p
                }
            } catch (_: Exception) {
            }
            return null
        }

        // ---------------------------------------------------------------- paths

        private fun decodeUrl(s: String): String = try {
            URLDecoder.decode(s.replace("+", "%2B"), "UTF-8")
        } catch (e: Exception) {
            s
        }

        /** Resolves [href] (possibly URL-encoded, possibly with ../) against [baseDir] inside the zip. */
        fun resolvePath(baseDir: String, href: String): String {
            val clean = decodeUrl(href.substringBefore('#').substringBefore('?'))
            val raw = when {
                clean.startsWith("/") -> clean.removePrefix("/")
                baseDir.isEmpty() -> clean
                else -> "$baseDir/$clean"
            }
            val out = ArrayList<String>()
            for (seg in raw.split('/')) {
                when (seg) {
                    "", "." -> {}
                    ".." -> if (out.isNotEmpty()) out.removeAt(out.size - 1)
                    else -> out.add(seg)
                }
            }
            return out.joinToString("/")
        }
    }
}
