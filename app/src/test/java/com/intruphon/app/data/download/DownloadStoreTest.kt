package com.intruphon.app.data.download

import com.intruphon.app.data.media.Kind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DownloadStoreTest {
    private fun item(id: Long, tags: Boolean?, uri: String?, cover: String?, state: DownloadState) = DownloadItem(
        DownloadRecord(id, "Title \"$id\" é ü", Kind.AUDIO, "m4a", "128 kbps", "https://example.com/v?$id", uri, tags, "note [build 7]", cover),
        DownloadStatus(state, 1234L, 5678L, if (state == DownloadState.FAILED) 403 else 0),
    )

    @Test fun roundTripKeepsEverything() {
        val items = listOf(
            item(1, true, "content://media/1", "https://img/1.jpg", DownloadState.DONE),
            item(2, false, null, null, DownloadState.FAILED),
            item(3, null, null, null, DownloadState.RUNNING),
        )
        assertEquals(items, DownloadStore.decode(DownloadStore.encode(items)))
    }

    @Test fun tagsKeepThreeStates() {
        val back = DownloadStore.decode(DownloadStore.encode(listOf(
            item(1, true, null, null, DownloadState.DONE),
            item(2, false, null, null, DownloadState.DONE),
            item(3, null, null, null, DownloadState.DONE),
        )))
        assertEquals(listOf(true, false, null), back.map { it.record.tags })
    }

    @Test fun emptyStringsBecomeNull() {
        val back = DownloadStore.decode(DownloadStore.encode(listOf(item(1, null, null, null, DownloadState.WAITING)))).single()
        assertNull(back.record.uri)
        assertNull(back.record.coverUrl)
    }

    @Test fun unreadableTextGivesAnEmptyListNotACrash() {
        assertTrue(DownloadStore.decode("not json").isEmpty())
        assertTrue(DownloadStore.decode("[{\"id\":1}]").isEmpty())
        assertTrue(DownloadStore.decode("").isEmpty())
    }

    @Test fun emptyListRoundTrips() {
        assertTrue(DownloadStore.decode(DownloadStore.encode(emptyList())).isEmpty())
    }
}
