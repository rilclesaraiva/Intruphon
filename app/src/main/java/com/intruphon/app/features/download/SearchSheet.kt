package com.intruphon.app.features.download

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.intruphon.app.R
import com.intruphon.app.core.ui.AppSheet
import com.intruphon.app.data.media.FailReason
import com.intruphon.app.data.media.MediaRepository
import com.intruphon.app.data.media.PlaylistEntry
import com.intruphon.app.data.media.SearchFilter
import com.intruphon.app.data.media.SearchResult
import com.intruphon.app.data.net.Thumbnails
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Bottom sheet for a name search: the results as a list (cover, title, channel, length) with two choices on top,
 * Videos or Songs. Tapping a result opens the usual format sheet over this one, so several can be downloaded
 * without searching again.
 */
@Composable
fun SearchSheet(query: String, onDismiss: () -> Unit) {
    var filter by rememberSaveable { mutableStateOf(SearchFilter.VIDEOS) }
    var picked by rememberSaveable { mutableStateOf<String?>(null) }
    val result by produceState<SearchResult?>(initialValue = null, query, filter) {
        value = null
        value = MediaRepository.search(query, filter)
    }

    AppSheet(onDismiss = onDismiss, dragOnlyFromTop = true) {
        Column(Modifier.fillMaxWidth().weight(1f, fill = false).padding(horizontal = 24.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.search_title, query), style = MaterialTheme.typography.titleLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = filter == SearchFilter.VIDEOS, onClick = { filter = SearchFilter.VIDEOS }, label = { Text(stringResource(R.string.search_videos)) })
                FilterChip(selected = filter == SearchFilter.SONGS, onClick = { filter = SearchFilter.SONGS }, label = { Text(stringResource(R.string.search_songs)) })
            }
            when (val r = result) {
                null -> {
                    Text(stringResource(R.string.search_loading), style = MaterialTheme.typography.titleMedium)
                    CircularProgressIndicator()
                }
                SearchResult.Empty -> Text(stringResource(R.string.search_empty))
                is SearchResult.Failed -> {
                    Text(stringResource(if (r.reason == FailReason.NETWORK) R.string.download_error_network else R.string.download_error_unreadable))
                    TextButton(onClick = onDismiss) { Text(stringResource(R.string.download_close)) }
                }
                is SearchResult.Ready -> LazyColumn(Modifier.weight(1f, fill = false), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(r.entries, key = { it.url }) { entry -> SearchRow(entry) { picked = entry.url } }
                }
            }
        }
    }
    picked?.let { DownloadSheet(link = it, onDismiss = { picked = null }) }
}

@Composable
private fun SearchRow(entry: PlaylistEntry, onClick: () -> Unit) {
    val picture by produceState(entry.thumbnailUrl?.let { Thumbnails.peek(it) }, entry.thumbnailUrl) {
        val url = entry.thumbnailUrl
        if (url != null) value = withContext(Dispatchers.IO) { Thumbnails.load(url) }
    }
    Row(
        Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium).clickable(onClick = onClick).padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.size(56.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceVariant)) {
            picture?.let { Image(it.asImageBitmap(), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()) }
        }
        Column(Modifier.weight(1f)) {
            Text(entry.title, style = MaterialTheme.typography.bodyLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
            val details = listOfNotNull(entry.artist, entry.durationSeconds.takeIf { it > 0 }?.let(::formatClock)).joinToString(" · ")
            if (details.isNotEmpty()) Text(details, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}
