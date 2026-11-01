package app.libro.reader.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.libro.reader.LibroApp
import app.libro.reader.R
import app.libro.reader.data.BookEntry
import app.libro.reader.data.ImportResult
import app.libro.reader.data.Sorts
import kotlinx.coroutines.launch
import java.text.Collator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShelfScreen(app: LibroApp, onOpen: (String) -> Unit, onSettings: () -> Unit) {
    val books by app.library.books.collectAsState()
    val prefs by app.prefs.state.collectAsState()
    val scope = rememberCoroutineScope()
    val snack = remember { SnackbarHostState() }

    var searching by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var sortMenu by remember { mutableStateOf(false) }
    var importing by remember { mutableStateOf(false) }
    var toRemove by remember { mutableStateOf<BookEntry?>(null) }

    val strAdded = stringResource(R.string.import_done)
    val strDupes = stringResource(R.string.import_dupes)
    val strBad = stringResource(R.string.import_bad)

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris: List<Uri> ->
        if (uris.isEmpty()) return@rememberLauncherForActivityResult
        scope.launch {
            importing = true
            var added = 0
            var dupes = 0
            var bad = 0
            for (u in uris) {
                when (app.library.import(u)) {
                    is ImportResult.Added -> added++
                    ImportResult.Duplicate -> dupes++
                    ImportResult.Invalid -> bad++
                }
            }
            importing = false
            val parts = buildList {
                if (added > 0) add(strAdded.format(added))
                if (dupes > 0) add(strDupes.format(dupes))
                if (bad > 0) add(strBad.format(bad))
            }
            if (parts.isNotEmpty()) snack.showSnackbar(parts.joinToString(" · "))
        }
    }
    val launchPicker = { picker.launch(arrayOf("application/epub+zip", "application/octet-stream")) }

    val collator = remember { Collator.getInstance().apply { strength = Collator.PRIMARY } }
    val shown = remember(books, query, prefs.sort) {
        val q = query.trim()
        val filtered = if (q.isEmpty()) books else books.filter {
            it.title.contains(q, ignoreCase = true) || it.author.contains(q, ignoreCase = true)
        }
        when (prefs.sort) {
            Sorts.TITLE -> filtered.sortedWith { a, b -> collator.compare(a.title, b.title) }
            Sorts.AUTHOR -> filtered.sortedWith { a, b -> collator.compare(a.author.ifBlank { "￿" }, b.author.ifBlank { "￿" }) }
            Sorts.ADDED -> filtered.sortedByDescending { it.addedAt }
            else -> filtered.sortedWith(compareByDescending<BookEntry> { it.lastOpenedAt }.thenByDescending { it.addedAt })
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snack) },
        topBar = {
            TopAppBar(
                title = {
                    if (searching) {
                        val focus = remember { FocusRequester() }
                        LaunchedEffect(Unit) { focus.requestFocus() }
                        TextField(
                            value = query,
                            onValueChange = { query = it },
                            singleLine = true,
                            placeholder = { Text(stringResource(R.string.search)) },
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                            ),
                            modifier = Modifier.fillMaxWidth().focusRequester(focus),
                        )
                    } else {
                        Text(stringResource(R.string.shelf_title), style = MaterialTheme.typography.headlineMedium)
                    }
                },
                navigationIcon = {
                    if (searching) IconButton(onClick = { searching = false; query = "" }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                },
                actions = {
                    if (!searching && books.isNotEmpty()) {
                        IconButton(onClick = { searching = true }) { Icon(Icons.Filled.Search, stringResource(R.string.search)) }
                        Box {
                            IconButton(onClick = { sortMenu = true }) { Icon(Icons.AutoMirrored.Filled.Sort, stringResource(R.string.sort)) }
                            DropdownMenu(sortMenu, { sortMenu = false }) {
                                Sorts.all.forEach { key ->
                                    DropdownMenuItem(
                                        text = { Text(sortLabel(key)) },
                                        trailingIcon = { if (prefs.sort == key) Icon(Icons.Filled.Check, null) },
                                        onClick = { app.prefs.update { it.copy(sort = key) }; sortMenu = false },
                                    )
                                }
                            }
                        }
                    }
                    if (searching && query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }) { Icon(Icons.Filled.Close, stringResource(R.string.close)) }
                    }
                    if (!searching) {
                        IconButton(onClick = onSettings) { Icon(Icons.Filled.Settings, stringResource(R.string.settings)) }
                    }
                },
            )
        },
        floatingActionButton = {
            if (books.isNotEmpty() && !searching) {
                ExtendedFloatingActionButton(
                    onClick = { if (!importing) launchPicker() },
                    icon = { Icon(Icons.Filled.Add, null) },
                    text = { Text(stringResource(if (importing) R.string.import_progress else R.string.add_books)) },
                )
            }
        },
    ) { pad ->
        Box(Modifier.fillMaxSize().padding(pad)) {
            when {
                books.isEmpty() -> EmptyShelf(importing, launchPicker)
                shown.isEmpty() -> Text(
                    stringResource(R.string.no_results),
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                else -> LazyVerticalGrid(
                    columns = GridCells.Adaptive(132.dp),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 100.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(22.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    if (importing) item(span = { GridItemSpan(maxLineSpan) }) {
                        Column {
                            Text(stringResource(R.string.import_progress), style = MaterialTheme.typography.labelLarge)
                            LinearProgressIndicator(Modifier.fillMaxWidth().padding(top = 6.dp))
                        }
                    }
                    items(shown, key = { it.id }) { book ->
                        BookTile(
                            book = book,
                            cover = app.library.coverFile(book),
                            onOpen = { onOpen(book.id) },
                            onFinished = { app.library.markFinished(book.id) },
                            onUnread = { app.library.markUnread(book.id) },
                            onRemove = { toRemove = book },
                        )
                    }
                }
            }
        }
    }

    toRemove?.let { book ->
        AlertDialog(
            onDismissRequest = { toRemove = null },
            title = { Text(stringResource(R.string.remove_title)) },
            text = { Text(book.title + "\n\n" + stringResource(R.string.remove_text)) },
            confirmButton = {
                TextButton(onClick = { app.library.remove(book.id); toRemove = null }) {
                    Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { toRemove = null }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

@Composable
private fun sortLabel(key: String) = stringResource(
    when (key) {
        Sorts.ADDED -> R.string.sort_added
        Sorts.TITLE -> R.string.sort_title
        Sorts.AUTHOR -> R.string.sort_author
        else -> R.string.sort_recent
    }
)

@Composable
private fun EmptyShelf(importing: Boolean, onAdd: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .size(96.dp)
                .clip(RoundedCornerShape(28.dp))
                .then(Modifier),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.MenuBook, null, Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
        }
        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.empty_title), style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        Text(
            stringResource(R.string.empty_sub),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp, bottom = 24.dp),
        )
        ExtendedFloatingActionButton(
            onClick = { if (!importing) onAdd() },
            icon = { Icon(Icons.Filled.Add, null) },
            text = { Text(stringResource(if (importing) R.string.import_progress else R.string.add_books)) },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BookTile(
    book: BookEntry,
    cover: java.io.File?,
    onOpen: () -> Unit,
    onFinished: () -> Unit,
    onUnread: () -> Unit,
    onRemove: () -> Unit,
) {
    var menu by remember { mutableStateOf(false) }
    Column(
        Modifier.combinedClickable(onClick = onOpen, onLongClick = { menu = true })
    ) {
        Box {
            BookCover(
                title = book.title,
                author = book.author,
                coverFile = cover,
                modifier = Modifier.fillMaxWidth().aspectRatio(2f / 3f),
            )
            if (book.finished) {
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.primary)
                        .size(24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.Check, stringResource(R.string.finished), tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(16.dp))
                }
            }
            DropdownMenu(menu, { menu = false }) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.mark_finished)) },
                    onClick = { menu = false; onFinished() },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.mark_unread)) },
                    onClick = { menu = false; onUnread() },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error) },
                    onClick = { menu = false; onRemove() },
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(book.title, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Text(
            book.author.ifBlank { stringResource(R.string.unknown_author) },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(6.dp))
        when {
            book.finished -> Text(
                stringResource(R.string.finished),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            book.started -> {
                LinearProgressIndicator(
                    progress = { book.progress.toFloat().coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    drawStopIndicator = {},
                )
                Text(
                    stringResource(R.string.percent_read, book.percent),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 3.dp),
                )
            }
            else -> Text(
                stringResource(R.string.new_book),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.secondary,
            )
        }
    }
}
