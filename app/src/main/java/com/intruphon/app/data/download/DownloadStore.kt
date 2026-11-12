package com.intruphon.app.data.download

import android.content.Context
import com.intruphon.app.data.media.Kind
import org.json.JSONArray
import org.json.JSONObject

/** The downloads list (newest first), kept in the app's private storage so it survives restarts. */
internal object DownloadStore {
    private const val PREFS = "downloads_v2"
    private const val KEY = "items"

    @Synchronized
    fun load(context: Context): List<DownloadItem> {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            (0 until array.length()).map { i ->
                val o = array.getJSONObject(i)
                DownloadItem(
                    DownloadRecord(
                        id = o.getLong("id"),
                        title = o.getString("title"),
                        kind = Kind.valueOf(o.getString("kind")),
                        extension = o.getString("extension"),
                        quality = o.getString("quality"),
                        sourceLink = o.getString("sourceLink"),
                        uri = o.optString("uri").ifEmpty { null },
                        tags = when (o.optInt("tags", -1)) { 1 -> true; 0 -> false; else -> null },
                        note = o.optString("note"),
                        coverUrl = o.optString("cover").ifEmpty { null },
                    ),
                    DownloadStatus(
                        state = DownloadState.valueOf(o.getString("state")),
                        downloadedBytes = o.getLong("downloaded"),
                        totalBytes = o.getLong("total"),
                        reason = o.getInt("reason"),
                    ),
                )
            }
        }.getOrDefault(emptyList())
    }

    @Synchronized
    fun save(context: Context, items: List<DownloadItem>) {
        val array = JSONArray()
        items.forEach {
            array.put(
                JSONObject()
                    .put("id", it.record.id)
                    .put("title", it.record.title)
                    .put("kind", it.record.kind.name)
                    .put("extension", it.record.extension)
                    .put("quality", it.record.quality)
                    .put("sourceLink", it.record.sourceLink)
                    .put("uri", it.record.uri ?: "")
                    .put("tags", when (it.record.tags) { true -> 1; false -> 0; null -> -1 })
                    .put("note", it.record.note)
                    .put("cover", it.record.coverUrl ?: "")
                    .put("state", it.status.state.name)
                    .put("downloaded", it.status.downloadedBytes)
                    .put("total", it.status.totalBytes)
                    .put("reason", it.status.reason),
            )
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, array.toString()).apply()
    }
}
