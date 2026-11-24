package com.intruphon.app.features.search

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.intruphon.app.R
import com.intruphon.app.core.LinkExtractor
import com.intruphon.app.core.formatClock
import com.intruphon.app.core.ui.clearFocusWhenKeyboardCloses
import com.intruphon.app.core.ui.swipeBetweenTabs
import com.intruphon.app.data.media.FailReason
import com.intruphon.app.data.media.Kind
import com.intruphon.app.data.media.MediaRepository
import com.intruphon.app.data.media.PlaylistEntry
import com.intruphon.app.data.media.SearchFilter
import com.intruphon.app.data.media.SearchResult
import com.intruphon.app.data.net.Thumbnails
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * Search page, laid out like YouTube's: the field on top; while it is being edited the list shows similar searches
 * to pick from, and once a search is sent the list shows the results (Videos or Songs). Opening it with a name
 * already typed on Home goes straight to the results. A result goes to [onPick], which opens the format sheet.
 * The field stays fixed at the top: search bars are an exception to the rule of no fixed top bar.
 */
@Composable
fun SearchScreen(initialQuery: String, onPick: (String) -> Unit, modifier: Modifier = Modifier) {
    var text by rememberSaveable(initialQuery) { mutableStateOf(initialQuery) }
    var submitted by rememberSaveable(initialQuery) { mutableStateOf(LinkExtractor.searchQuery(initialQuery)) }
    // Tabs: All (the default), Videos, Songs.
    var filter by rememberSaveable { mutableStateOf(SearchFilter.ALL) }
    var focused by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val listState = rememberLazyListState()

    fun submit(typed: String) {
        // A link typed here goes to the format sheet, like on Home.
        LinkExtractor.firstUrl(typed)?.let { focusManager.clearFocus(); onPick(it); return }
        val clean = LinkExtractor.searchQuery(typed) ?: return
        text = clean
        submitted = clean
        focusManager.clearFocus()
    }

    // Similar searches while typing; a short pause first so each keystroke does not make a request.
    val suggestions by produceState(emptyList<String>(), text, focused) {
        val q = LinkExtractor.searchQuery(text)
        if (!focused || q == null) {
            value = emptyList()
            return@produceState
        }
        delay(250)
        value = MediaRepository.suggestions(q)
    }
    // What each tab already loaded, so swiping back does not ask again.
    val loaded = remember(submitted) { mutableStateMapOf<SearchFilter, SearchResult>() }
    val result by produceState<SearchResult?>(loaded[filter], submitted, filter) {
        val q = submitted
        if (value != null || q == null) return@produceState
        val fresh = MediaRepository.search(q, filter)
        if (fresh !is SearchResult.Failed) loaded[filter] = fresh
        value = fresh
    }

    Column(modifier.fillMaxSize()) {
    // Fixed at the top, like YouTube's (the one exception to "no fixed top bar"): beside the floating Back button.
    Box(Modifier.fillMaxWidth().statusBarsPadding().height(80.dp).padding(start = 76.dp, end = 24.dp), contentAlignment = Alignment.CenterStart) {
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            placeholder = { Text(stringResource(R.string.search_hint), maxLines = 1) },
            singleLine = true,
            shape = MaterialTheme.shapes.extraLarge,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { submit(text) }),
            trailingIcon = {
                if (text.isNotEmpty()) {
                    IconButton(onClick = { text = "" }) { Icon(Icons.Default.Close, contentDescription = stringResource(R.string.home_clear)) }
                }
            },
            modifier = Modifier.fillMaxWidth().clearFocusWhenKeyboardCloses().onFocusChanged { focused = it.isFocused },
        )
    }
    LazyColumn(
        state = listState,
        modifier = Modifier.weight(1f).fillMaxWidth().swipeBetweenTabs(filter.ordinal, SearchFilter.entries.size) { filter = SearchFilter.entries[it] },
        contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 4.dp, bottom = 24.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (focused) {
            items(suggestions, key = { "s$it" }) { suggestion ->
                Row(
                    Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium).clickable { submit(suggestion) }.padding(horizontal = 8.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(suggestion, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        } else if (submitted != null) {
            item {
                Row(Modifier.padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        SearchFilter.ALL to R.string.downloads_filter_all,
                        SearchFilter.VIDEOS to R.string.search_videos,
                        SearchFilter.SONGS to R.string.search_songs,
                    ).forEach { (tab, label) ->
                        FilterChip(selected = filter == tab, onClick = { filter = tab }, label = { Text(stringResource(label)) }, shape = CircleShape)
                    }
                }
            }
            when (val r = result) {
                null -> item {
                    Row(Modifier.padding(vertical = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        CircularProgressIndicator(Modifier.size(28.dp), strokeWidth = 3.dp)
                        Text(stringResource(R.string.search_loading), style = MaterialTheme.typography.titleMedium)
                    }
                }
                SearchResult.Empty -> item { Text(stringResource(R.string.search_empty), Modifier.padding(vertical = 16.dp)) }
                is SearchResult.Failed -> item {
                    Text(
                        stringResource(if (r.reason == FailReason.NETWORK) R.string.download_error_network else R.string.download_error_unreadable),
                        Modifier.padding(vertical = 16.dp),
                    )
                }
                is SearchResult.Ready -> items(r.hits, key = { it.entry.url }) { hit ->
                    // Only the All tab mixes the two kinds, so only it says which is which.
                    ResultRow(hit.entry, kind = if (filter == SearchFilter.ALL) (if (hit.music) Kind.AUDIO else Kind.VIDEO) else null) { onPick(hit.entry.url) }
                }
            }
        }
    }
    }
}

@Composable
private fun ResultRow(entry: PlaylistEntry, kind: Kind?, onClick: () -> Unit) {
    val picture by produceState(entry.thumbnailUrl?.let { Thumbnails.peek(it) }, entry.thumbnailUrl) {
        val url = entry.thumbnailUrl
        if (url != null && value == null) value = withContext(Dispatchers.IO) { Thumbnails.load(url) }
    }
    Row(
        Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium).clickable(onClick = onClick).padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.size(64.dp).clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surfaceVariant)) {
            picture?.let { Image(it.asImageBitmap(), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()) }
        }
        Column(Modifier.weight(1f)) {
            Text(entry.title, style = MaterialTheme.typography.bodyLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
            val details = listOfNotNull(entry.artist, entry.durationSeconds.takeIf { it > 0 }?.let(::formatClock)).joinToString(" · ")
            if (details.isNotEmpty() || kind != null) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (kind != null) {
                        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.secondaryContainer) {
                            Text(
                                stringResource(if (kind == Kind.AUDIO) R.string.download_section_audio else R.string.download_section_video),
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            )
                        }
                    }
                    Text(details, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}
