package app.libro.reader.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.net.Uri
import android.provider.OpenableColumns
import app.libro.reader.epub.EpubBook
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest

data class BookEntry(
    val id: String,
    val title: String,
    val author: String,
    val language: String,
    val coverFile: String?,
    val addedAt: Long,
    val lastOpenedAt: Long,
    /** Reading position: chapter index + fraction inside it (0..1). */
    val chapter: Int,
    val fraction: Double,
    /** Whole-book progress, 0..1. */
    val progress: Double,
    val sizeBytes: Long,
) {
    val fileName: String get() = "$id.epub"
    val finished: Boolean get() = progress >= 0.98
    val started: Boolean get() = chapter > 0 || fraction > 0.0
    val percent: Int get() = (progress * 100.0).toInt().coerceIn(0, 100)
}

sealed interface ImportResult {
    data class Added(val entry: BookEntry) : ImportResult
    data object Duplicate : ImportResult
    data object Invalid : ImportResult
}

/**
 * The shelf. Every imported EPUB is copied into the app's private storage, so the book keeps
 * working even if the original file is moved or deleted. The index lives in library.json.
 */
class LibraryRepository(private val context: Context) {
    private val booksDir = File(context.filesDir, "books").apply { mkdirs() }
    private val coversDir = File(context.filesDir, "covers").apply { mkdirs() }
    private val indexFile = File(context.filesDir, "library.json")
    private val lock = Any()

    private val _books = MutableStateFlow(load())
    val books: StateFlow<List<BookEntry>> = _books.asStateFlow()

    fun bookFile(entry: BookEntry): File = File(booksDir, entry.fileName)
    fun coverFile(entry: BookEntry): File? = entry.coverFile?.let { File(coversDir, it).takeIf { f -> f.exists() } }
    fun find(id: String): BookEntry? = _books.value.firstOrNull { it.id == id }

    suspend fun import(uri: Uri): ImportResult = withContext(Dispatchers.IO) {
        val tmp = File(booksDir, "import_${System.nanoTime()}.tmp")
        try {
            val digest = MessageDigest.getInstance("SHA-1")
            val input = context.contentResolver.openInputStream(uri) ?: return@withContext ImportResult.Invalid
            input.use { stream ->
                tmp.outputStream().use { out ->
                    val buf = ByteArray(64 * 1024)
                    while (true) {
                        val n = stream.read(buf)
                        if (n < 0) break
                        out.write(buf, 0, n)
                        digest.update(buf, 0, n)
                    }
                }
            }
            if (tmp.length() == 0L) return@withContext ImportResult.Invalid

            val id = digest.digest().joinToString("") { "%02x".format(it) }.take(20)
            if (_books.value.any { it.id == id }) return@withContext ImportResult.Duplicate

            val target = File(booksDir, "$id.epub")
            if (!tmp.renameTo(target)) {
                tmp.copyTo(target, overwrite = true)
            }

            val book = try {
                EpubBook.open(target)
            } catch (e: Exception) {
                target.delete()
                return@withContext ImportResult.Invalid
            }
            val entry = book.use { b ->
                val fallbackTitle = displayName(uri)?.substringBeforeLast('.')?.replace('_', ' ')?.trim().orEmpty()
                BookEntry(
                    id = id,
                    title = b.title.ifBlank { fallbackTitle.ifBlank { id } },
                    author = b.author,
                    language = b.language,
                    coverFile = saveCover(b, id),
                    addedAt = System.currentTimeMillis(),
                    lastOpenedAt = 0L,
                    chapter = 0,
                    fraction = 0.0,
                    progress = 0.0,
                    sizeBytes = target.length(),
                )
            }
            synchronized(lock) {
                _books.update { it + entry }
                save()
            }
            ImportResult.Added(entry)
        } catch (e: Exception) {
            ImportResult.Invalid
        } finally {
            if (tmp.exists()) tmp.delete()
        }
    }

    fun markOpened(id: String) = mutate(id) { it.copy(lastOpenedAt = System.currentTimeMillis()) }

    fun updateProgress(id: String, chapter: Int, fraction: Double, progress: Double) =
        mutate(id) { it.copy(chapter = chapter, fraction = fraction, progress = progress) }

    fun markFinished(id: String) = mutate(id) { it.copy(progress = 1.0) }

    fun markUnread(id: String) = mutate(id) { it.copy(chapter = 0, fraction = 0.0, progress = 0.0) }

    fun remove(id: String) {
        val entry = find(id) ?: return
        synchronized(lock) {
            _books.update { list -> list.filter { it.id != id } }
            save()
        }
        File(booksDir, entry.fileName).delete()
        entry.coverFile?.let { File(coversDir, it).delete() }
    }

    private fun mutate(id: String, change: (BookEntry) -> BookEntry) {
        synchronized(lock) {
            _books.update { list -> list.map { if (it.id == id) change(it) else it } }
            save()
        }
    }

    // ------------------------------------------------------------------ covers

    private fun saveCover(book: EpubBook, id: String): String? {
        val bytes = book.readCover() ?: return null
        return try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
            var sample = 1
            while (bounds.outHeight / (sample * 2) >= 900 && bounds.outWidth / (sample * 2) >= 600) sample *= 2
            val decoded = BitmapFactory.decodeByteArray(
                bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = sample }
            ) ?: return null
            // Flatten onto white so transparent PNG covers don't turn black as JPEG.
            val flat = Bitmap.createBitmap(decoded.width, decoded.height, Bitmap.Config.ARGB_8888)
            Canvas(flat).apply {
                drawColor(Color.WHITE)
                drawBitmap(decoded, 0f, 0f, null)
            }
            val file = File(coversDir, "$id.jpg")
            file.outputStream().use { flat.compress(Bitmap.CompressFormat.JPEG, 88, it) }
            decoded.recycle()
            flat.recycle()
            file.name
        } catch (e: Exception) {
            null
        }
    }

    private fun displayName(uri: Uri): String? = try {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
            if (c.moveToFirst()) c.getString(0) else null
        }
    } catch (e: Exception) {
        null
    }

    // ------------------------------------------------------------------ persistence

    private fun load(): List<BookEntry> {
        if (!indexFile.exists()) return emptyList()
        return try {
            val arr = JSONArray(indexFile.readText())
            (0 until arr.length()).mapNotNull { i ->
                val o = arr.getJSONObject(i)
                val entry = BookEntry(
                    id = o.getString("id"),
                    title = o.optString("title"),
                    author = o.optString("author"),
                    language = o.optString("language"),
                    coverFile = o.optString("coverFile").takeIf { it.isNotEmpty() },
                    addedAt = o.optLong("addedAt"),
                    lastOpenedAt = o.optLong("lastOpenedAt"),
                    chapter = o.optInt("chapter"),
                    fraction = o.optDouble("fraction", 0.0),
                    progress = o.optDouble("progress", 0.0),
                    sizeBytes = o.optLong("sizeBytes"),
                )
                if (File(booksDir, entry.fileName).exists()) entry else null
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun save() {
        val arr = JSONArray()
        for (b in _books.value) {
            arr.put(
                JSONObject()
                    .put("id", b.id)
                    .put("title", b.title)
                    .put("author", b.author)
                    .put("language", b.language)
                    .put("coverFile", b.coverFile ?: "")
                    .put("addedAt", b.addedAt)
                    .put("lastOpenedAt", b.lastOpenedAt)
                    .put("chapter", b.chapter)
                    .put("fraction", b.fraction)
                    .put("progress", b.progress)
                    .put("sizeBytes", b.sizeBytes)
            )
        }
        val tmp = File(context.filesDir, "library.json.tmp")
        tmp.writeText(arr.toString())
        if (!tmp.renameTo(indexFile)) {
            indexFile.writeText(arr.toString())
            tmp.delete()
        }
    }
}
