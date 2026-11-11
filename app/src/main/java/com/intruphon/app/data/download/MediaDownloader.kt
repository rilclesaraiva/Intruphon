package com.intruphon.app.data.download

import android.content.Context
import android.net.Uri
import com.intruphon.app.data.media.Container
import com.intruphon.app.data.media.DownloadOption
import com.intruphon.app.data.media.Kind
import com.intruphon.app.data.media.MediaInfo
import com.intruphon.app.data.media.Plan
import com.intruphon.app.data.media.TrackMetadata
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import java.io.File

/** The saved file and whether its tags were written (null when no tagging was tried). */
internal class SavedFile(val uri: Uri, val tagged: Boolean?)

/** The tag put in the file name, for example `M4A_128K` or `MP4_720p`. */
internal fun qualityTag(format: String, quality: String): String =
    listOf(format, quality.replace(" kbps", "K").replace(" ", "")).filter { it.isNotEmpty() }.joinToString("_")

/** Carries out a [Plan]: a direct save, or download + join (+ tags) in the app cache and then save. */
internal object MediaDownloader {
    suspend fun download(context: Context, info: MediaInfo, option: DownloadOption, onProgress: (Long, Long) -> Unit, onProcessing: () -> Unit): SavedFile {
        var tagged: Boolean? = null
        val name = sanitizeFileName(if (option.kind == Kind.AUDIO) info.metadata.title else info.title) +
            " (" + qualityTag(option.format, option.quality) + ")." + option.extension
        return when (val plan = option.plan) {
            is Plan.Direct -> SavedFile(
                HttpFileDownloader.save(context, name, option.mimeType, option.kind) { out ->
                    HttpFileDownloader.fetch(plan.url, out, onProgress)
                },
                null,
            )
            is Plan.AudioM4a -> processed(context, name, option, produce = { work ->
                val raw = File(work, "audio.src")
                fetchTo(raw, plan.url, 0, option.sizeBytes ?: -1, onProgress)
                currentCoroutineContext().ensureActive()
                onProcessing()
                val clean = File(work, "audio.m4a")
                MediaMuxing.join(listOf(raw), clean, Container.MP4)
                // Tags are a bonus: if they cannot be written the plain file is still saved.
                tagged = runCatching { Mp4Tags.write(clean, info.metadata, CoverArt.fetch(info.metadata.coverUrl)) }.isSuccess
                clean
            }, tagged = { tagged })
            is Plan.AudioMp3 -> processed(context, name, option, produce = { work ->
                val raw = File(work, "audio.src")
                fetchTo(raw, plan.url, 0, option.sizeBytes ?: -1, onProgress)
                onProcessing()
                val id3 = runCatching { Id3v2.build(info.metadata, CoverArt.fetch(info.metadata.coverUrl)) }.getOrNull()
                tagged = id3 != null
                val mp3 = File(work, "audio.mp3")
                Mp3Encoder.convert(raw, mp3, plan.kbps, id3)
                mp3
            }, tagged = { tagged })
            is Plan.Mux -> processed(context, name, option, produce = { work ->
                val video = File(work, "video.src")
                fetchTo(video, plan.videoUrl, 0, option.sizeBytes ?: -1, onProgress)
                val audio = File(work, "audio.src")
                fetchTo(audio, plan.audioUrl, video.length(), option.sizeBytes ?: -1, onProgress)
                currentCoroutineContext().ensureActive()
                onProcessing()
                val joined = File(work, "joined." + option.extension)
                MediaMuxing.join(listOf(video, audio), joined, plan.container)
                if (plan.container == Container.MP4) {
                    tagged = runCatching { Mp4Tags.write(joined, videoTags(info.metadata, info.title), null) }.isSuccess
                }
                joined
            }, tagged = { tagged })
        }
    }

    private fun videoTags(meta: TrackMetadata, title: String) = meta.copy(title = title, album = null)

    private suspend fun fetchTo(file: File, url: String, offset: Long, grandTotal: Long, onProgress: (Long, Long) -> Unit) {
        file.outputStream().buffered().use { out ->
            HttpFileDownloader.fetch(url, out) { done, _ -> onProgress(offset + done, grandTotal) }
        }
    }

    private suspend fun processed(
        context: Context,
        fileName: String,
        option: DownloadOption,
        produce: suspend (File) -> File,
        tagged: () -> Boolean?,
    ): SavedFile {
        val work = File(context.cacheDir, "work-${System.nanoTime()}").apply { mkdirs() }
        try {
            val result = produce(work)
            val uri = HttpFileDownloader.save(context, fileName, option.mimeType, option.kind) { out ->
                result.inputStream().use { it.copyTo(out) }
            }
            return SavedFile(uri, tagged())
        } finally {
            work.deleteRecursively()
        }
    }
}
