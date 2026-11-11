package com.intruphon.app.data.stats

import android.content.Context
import android.util.Log
import org.json.JSONObject
import java.io.File
import java.util.Locale

/** One finished (or failed) download. Times are in milliseconds; [bytes] are the bytes fetched from the site. */
data class SpeedEntry(
    val timeMs: Long,
    val kind: String,
    val format: String,
    val quality: String,
    val plan: String,
    val network: String,
    val bytes: Long,
    val downloadMs: Long,
    val processMs: Long,
    val ok: Boolean,
    val reason: Int,
)

/** Average speed of all successful downloads of one format and quality. */
data class SpeedSummary(val format: String, val quality: String, val count: Int, val bytesPerSecond: Long)

private const val MIN_MS = 500L
private const val MIN_BYTES = 100L * 1024

/** Groups the entries by format and quality; short or tiny downloads are skipped because they say nothing about speed. */
fun summarize(entries: List<SpeedEntry>): List<SpeedSummary> =
    entries
        .filter { it.ok && it.downloadMs >= MIN_MS && it.bytes >= MIN_BYTES }
        .groupBy { it.format to it.quality }
        .map { (key, list) ->
            val bytes = list.sumOf { it.bytes }
            val ms = list.sumOf { it.downloadMs }
            SpeedSummary(key.first, key.second, list.size, bytes * 1000 / ms)
        }
        .sortedWith(compareBy({ it.format }, { it.quality }))

fun toCsv(entries: List<SpeedEntry>): String {
    val header = "time_utc,kind,format,quality,plan,network,bytes,download_ms,process_ms,kb_per_s,ok,reason"
    val rows = entries.map {
        val kbps = if (it.downloadMs > 0) it.bytes * 1000 / it.downloadMs / 1024 else 0
        val time = java.time.Instant.ofEpochMilli(it.timeMs).toString()
        listOf(time, it.kind, it.format, it.quality, it.plan, it.network, it.bytes, it.downloadMs, it.processMs, kbps, it.ok, it.reason)
            .joinToString(",") { v -> csvCell(v.toString()) }
    }
    return (listOf(header) + rows).joinToString("\n")
}

private fun csvCell(value: String): String =
    if (value.any { it == ',' || it == '"' || it == '\n' }) "\"" + value.replace("\"", "\"\"") + "\"" else value

fun formatSpeed(bytesPerSecond: Long): String =
    if (bytesPerSecond >= 1_048_576) String.format(Locale.US, "%.1f MB/s", bytesPerSecond / 1_048_576.0)
    else String.format(Locale.US, "%d KB/s", bytesPerSecond / 1024)

/**
 * Speed log kept only on this device (a text file, newest 300 lines) and also written to logcat under the
 * tag IntruphonSpeed. Nothing is sent anywhere; the user can share it as CSV from the Settings tab.
 */
object SpeedLog {
    private const val FILE = "speed_log.jsonl"
    private const val MAX_LINES = 300

    @Synchronized
    fun add(context: Context, entry: SpeedEntry) {
        Log.i("IntruphonSpeed", "${entry.format} ${entry.quality} ${entry.plan} ${entry.network} ${entry.bytes}B in ${entry.downloadMs}ms (+${entry.processMs}ms processing) ok=${entry.ok} reason=${entry.reason}")
        runCatching {
            val file = File(context.filesDir, FILE)
            val json = JSONObject()
                .put("t", entry.timeMs).put("kind", entry.kind).put("format", entry.format).put("quality", entry.quality)
                .put("plan", entry.plan).put("net", entry.network).put("bytes", entry.bytes).put("dl", entry.downloadMs)
                .put("proc", entry.processMs).put("ok", entry.ok).put("reason", entry.reason)
            val lines = (if (file.exists()) file.readLines() else emptyList()) + json.toString()
            file.writeText(lines.takeLast(MAX_LINES).joinToString("\n") + "\n")
        }
    }

    @Synchronized
    fun all(context: Context): List<SpeedEntry> {
        val file = File(context.filesDir, FILE)
        if (!file.exists()) return emptyList()
        return file.readLines().mapNotNull { line ->
            runCatching {
                val o = JSONObject(line)
                SpeedEntry(
                    o.getLong("t"), o.getString("kind"), o.getString("format"), o.getString("quality"), o.getString("plan"),
                    o.getString("net"), o.getLong("bytes"), o.getLong("dl"), o.getLong("proc"), o.getBoolean("ok"), o.getInt("reason"),
                )
            }.getOrNull()
        }
    }

    @Synchronized
    fun clear(context: Context) {
        File(context.filesDir, FILE).delete()
    }
}
