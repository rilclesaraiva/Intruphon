package com.intruphon.app.data.download

import android.content.Context
import com.intruphon.app.data.media.Kind
import org.json.JSONArray
import org.json.JSONObject

/** Small list of started downloads, kept in the app's private storage. */
internal object DownloadStore {
    private const val PREFS = "downloads"
    private const val KEY = "items"

    fun load(context: Context): List<DownloadRecord> {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            (0 until array.length()).map { i ->
                val o = array.getJSONObject(i)
                DownloadRecord(
                    id = o.getLong("id"),
                    title = o.getString("title"),
                    kind = Kind.valueOf(o.getString("kind")),
                    extension = o.getString("extension"),
                    quality = o.getString("quality"),
                    sourceLink = o.getString("sourceLink"),
                )
            }
        }.getOrDefault(emptyList())
    }

    @Synchronized
    fun add(context: Context, record: DownloadRecord) = save(context, load(context) + record)

    @Synchronized
    fun remove(context: Context, id: Long) = save(context, load(context).filterNot { it.id == id })

    private fun save(context: Context, records: List<DownloadRecord>) {
        val array = JSONArray()
        records.forEach {
            array.put(
                JSONObject()
                    .put("id", it.id)
                    .put("title", it.title)
                    .put("kind", it.kind.name)
                    .put("extension", it.extension)
                    .put("quality", it.quality)
                    .put("sourceLink", it.sourceLink),
            )
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, array.toString()).apply()
    }
}
