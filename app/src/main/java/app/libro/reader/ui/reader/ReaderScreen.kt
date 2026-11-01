package app.libro.reader.ui.reader

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.TextFormat
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import app.libro.reader.LibroApp
import app.libro.reader.R
import app.libro.reader.data.BookEntry
import app.libro.reader.data.ReadModes
import app.libro.reader.epub.EpubBook
import app.libro.reader.epub.TocEntry
import app.libro.reader.ui.LocalThemeInfo
import app.libro.reader.ui.Palettes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private data class Pos(val chapter: Int = 0, val display: Double = 0.0, val page: Int = 0, val pages: Int = 1, val overall: Double = 0.0)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(app: LibroApp, bookId: String, onClose: () -> Unit) {
    val context = LocalContext.current
    val entry = remember(bookId) { app.library.find(bookId) }
    var book by remember { mutableStateOf<EpubBook?>(null) }
    var failed by remember { mutableStateOf(false) }

    LaunchedEffect(bookId) {
        if (entry == null) { failed = true; return@LaunchedEffect }
        val opened = withContext(Dispatchers.IO) {
            try { EpubBook.open(app.library.bookFile(entry)) } catch (e: Exception) { null }
        }
        if (opened == null) failed = true else {
            app.library.markOpened(bookId)
            book = opened
        }
    }

    val prefs by app.prefs.state.collectAsState()
    val themeInfo = LocalThemeInfo.current
    val palette = Palettes.forReader(prefs.readerTheme, themeInfo.dark, themeInfo.sepia)

    val b = book
    if (failed || entry == null) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(Modifier.fillMaxSize().statusBarsPadding(), Arrangement.Center, Alignment.CenterHorizontally) {
                Text(stringResource(R.string.book_open_error))
                TextButton(onClick = onClose) { Text(stringResource(R.string.back)) }
            }
        }
        return
    }
    if (b == null) {
        Box(Modifier.fillMaxSize().background(palette.bg), Alignment.Center) { CircularProgressIndicator(color = palette.link) }
        return
    }

    ReaderContent(app, entry, b, palette, onClose)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReaderContent(
    app: LibroApp,
    entry: BookEntry,
    book: EpubBook,
    palette: app.libro.reader.ui.ReaderPalette,
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    val prefs by app.prefs.state.collectAsState()
    var chrome by remember { mutableStateOf(false) }
    var showToc by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var pos by remember { mutableStateOf(Pos(entry.chapter, 0.0, 0, 1, entry.progress)) }

    val chromeState = rememberUpdatedState { chrome = !chrome }
    val posState = rememberUpdatedState<(Pos) -> Unit> { pos = it }

    val controller = remember(book) {
        ReaderController(
            context = context,
            book = book,
            entry = entry,
            library = app.library,
            onChrome = { chromeState.value() },
            onPosition = { ch, display, page, pages, overall -> posState.value(Pos(ch, display, page, pages, overall)) },
        )
    }
    DisposableEffect(controller) { onDispose { controller.destroy() } }

    val labels = ReaderLabels(
        stringResource(R.string.prev_chapter),
        stringResource(R.string.next_chapter),
        stringResource(R.string.the_end),
    )
    LaunchedEffect(prefs, palette, labels) { controller.updateStyle(prefs, palette, labels) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, controller) {
        val obs = LifecycleEventObserver { _, ev -> if (ev == Lifecycle.Event.ON_PAUSE) controller.saveNow() }
        lifecycleOwner.lifecycle.addObserver(obs)
        onDispose { lifecycleOwner.lifecycle.removeObserver(obs) }
    }

    val view = androidx.compose.ui.platform.LocalView.current
    DisposableEffect(prefs.keepScreenOn) {
        view.keepScreenOn = prefs.keepScreenOn
        onDispose { view.keepScreenOn = false }
    }
    DisposableEffect(prefs.brightness) {
        val window = (context as? Activity)?.window
        val old = window?.attributes?.screenBrightness
        window?.let { w ->
            val a = w.attributes
            a.screenBrightness = if (prefs.brightness < 0f) android.view.WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE else prefs.brightness.coerceIn(0.02f, 1f)
            w.attributes = a
        }
        onDispose {
            window?.let { w ->
                val a = w.attributes
                a.screenBrightness = android.view.WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
                w.attributes = a
            }
        }
    }

    BackHandler(enabled = chrome) { chrome = false }

    val density = LocalDensity.current
    val topInset = with(density) { WindowInsets.statusBars.getTop(this).toDp() }
    val bottomInset = with(density) { WindowInsets.navigationBars.getBottom(this).toDp() }

    val currentToc: TocEntry? = remember(pos.chapter, book) {
        book.toc.lastOrNull { it.spineIndex <= pos.chapter && it.title.isNotBlank() }
    }
    val chapterTitle = currentToc?.title ?: stringResource(R.string.chapter_n, pos.chapter + 1)

    Box(Modifier.fillMaxSize().background(palette.bg)) {
        AndroidView(
            factory = { controller.webView },
            modifier = Modifier
                .fillMaxSize()
                .padding(top = topInset + 4.dp, bottom = bottomInset + 26.dp),
        )

        Row(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = bottomInset + 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            val dim = palette.fg.copy(alpha = 0.55f)
            Text(
                chapterTitle,
                color = dim,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).padding(end = 12.dp),
            )
            val right = if (prefs.readerMode == ReadModes.PAGED && pos.pages > 1)
                stringResource(R.string.page_of, pos.page + 1, pos.pages) + "  ·  " + (pos.overall * 100).toInt() + "%"
            else (pos.overall * 100).toInt().toString() + "%"
            Text(right, color = dim, style = MaterialTheme.typography.labelSmall, maxLines = 1)
        }

        AnimatedVisibility(
            visible = chrome,
            modifier = Modifier.align(Alignment.TopCenter),
            enter = fadeIn() + slideInVertically { -it / 2 },
            exit = fadeOut() + slideOutVertically { -it / 2 },
        ) {
            Surface(color = MaterialTheme.colorScheme.surface.copy(alpha = 0.97f), shadowElevation = 4.dp) {
                Row(
                    Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 4.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onClose) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back)) }
                    Text(
                        book.title.ifBlank { entry.title },
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    val scrolling = prefs.readerMode == ReadModes.SCROLL
                    IconButton(onClick = {
                        app.prefs.update { it.copy(readerMode = if (scrolling) ReadModes.PAGED else ReadModes.SCROLL) }
                    }) {
                        Icon(
                            if (scrolling) Icons.Filled.AutoStories else Icons.Filled.SwapVert,
                            stringResource(if (scrolling) R.string.mode_paged else R.string.mode_scroll),
                        )
                    }
                    IconButton(onClick = { showToc = true }) { Icon(Icons.AutoMirrored.Filled.FormatListBulleted, stringResource(R.string.contents)) }
                    IconButton(onClick = { showSettings = true }) { Icon(Icons.Filled.TextFormat, stringResource(R.string.reader_settings)) }
                }
            }
        }

        AnimatedVisibility(
            visible = chrome,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = fadeIn() + slideInVertically { it / 2 },
            exit = fadeOut() + slideOutVertically { it / 2 },
        ) {
            var dragging by remember { mutableStateOf(false) }
            var dragValue by remember { mutableFloatStateOf(0f) }
            Surface(color = MaterialTheme.colorScheme.surface.copy(alpha = 0.97f), shadowElevation = 8.dp) {
                Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 20.dp, vertical = 10.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(
                            chapterTitle,
                            style = MaterialTheme.typography.labelLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f).padding(end = 12.dp),
                        )
                        Text(
                            stringResource(R.string.percent_read, ((if (dragging) dragValue.toDouble() else pos.overall) * 100).toInt()),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    Slider(
                        value = if (dragging) dragValue else pos.overall.toFloat(),
                        onValueChange = { dragging = true; dragValue = it },
                        onValueChangeFinished = { controller.seek(dragValue.toDouble()); dragging = false },
                    )
                }
            }
        }
    }

    if (showToc) {
        ModalBottomSheet(onDismissRequest = { showToc = false }) {
            TocList(book, pos.chapter) { e ->
                showToc = false
                chrome = false
                controller.goToToc(e)
            }
        }
    }

    if (showSettings) {
        ModalBottomSheet(
            onDismissRequest = { showSettings = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false),
            scrimColor = Color.Transparent,
        ) {
            ReaderSettingsPanel(app.prefs)
        }
    }
}

@Composable
private fun TocList(book: EpubBook, currentChapter: Int, onPick: (TocEntry) -> Unit) {
    val entries = book.toc
    val currentIdx = remember(currentChapter) { entries.indexOfLast { it.spineIndex <= currentChapter }.coerceAtLeast(0) }
    val state = rememberLazyListState()
    LaunchedEffect(Unit) { state.scrollToItem((currentIdx - 2).coerceAtLeast(0)) }
    Column(Modifier.fillMaxWidth()) {
        Text(
            stringResource(R.string.contents),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
        )
        LazyColumn(state = state, modifier = Modifier.navigationBarsPadding()) {
            itemsIndexed(entries) { i, e ->
                val title = e.title.ifBlank { stringResource(R.string.chapter_n, e.spineIndex + 1) }
                val current = i == currentIdx
                Text(
                    title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (current) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (current) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onPick(e) }
                        .padding(start = (24 + 16 * e.level.coerceAtMost(3)).dp, end = 24.dp, top = 14.dp, bottom = 14.dp),
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            }
        }
    }
}
