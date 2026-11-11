package com.intruphon.app.data.media

import com.intruphon.app.data.net.OkHttpDownloader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.exceptions.ExtractionException
import org.schabi.newpipe.extractor.stream.AudioStream
import org.schabi.newpipe.extractor.stream.AudioTrackType
import org.schabi.newpipe.extractor.stream.DeliveryMethod
import org.schabi.newpipe.extractor.stream.StreamInfo
import org.schabi.newpipe.extractor.stream.VideoStream
import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException

/** Reads a link with NewPipeExtractor and turns it into plain download options. */
object MediaRepository {
    private var initialized = false

    @Synchronized
    private fun ensureInit() {
        if (!initialized) {
            NewPipe.init(OkHttpDownloader())
            initialized = true
        }
    }

    suspend fun load(link: String): LoadResult = withContext(Dispatchers.IO) {
        try {
            ensureInit()
            val info = StreamInfo.getInfo(link)
            val options = audioOptions(info.audioStreams) + videoOptions(info.videoStreams)
            if (options.isEmpty()) LoadResult.Failed(FailReason.NO_FORMATS)
            else LoadResult.Ready(MediaInfo(info.name, options))
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            LoadResult.Failed(FailReason.NETWORK)
        } catch (e: ExtractionException) {
            LoadResult.Failed(FailReason.UNREADABLE)
        } catch (e: Exception) {
            LoadResult.Failed(FailReason.UNREADABLE)
        }
    }

    // Only plain HTTP files can be handed to the system download manager (no DASH/HLS manifests).
    private fun audioOptions(streams: List<AudioStream>): List<DownloadOption> =
        streams
            .filter { it.deliveryMethod == DeliveryMethod.PROGRESSIVE_HTTP && it.isUrl }
            .filter { it.audioTrackType == null || it.audioTrackType == AudioTrackType.ORIGINAL }
            .filter { it.format != null }
            .groupBy { it.format!!.suffix }
            .values
            .map { group -> group.maxBy { it.averageBitrate } }
            .sortedBy { if (it.format!!.suffix == "m4a") 0 else 1 }
            .map { s ->
                val format = s.format!!
                DownloadOption(
                    id = "a-${s.id}",
                    kind = Kind.AUDIO,
                    quality = if (s.averageBitrate > 0) "${s.averageBitrate} kbps" else "",
                    extension = format.suffix,
                    mimeType = format.mimeType,
                    sizeBytes = s.itagItem?.contentLength?.takeIf { it > 0 },
                    url = s.content,
                )
            }

    // getVideoStreams() holds streams that already include audio; video-only streams are not offered
    // because joining them with audio needs a muxing step that is not part of this version.
    private fun videoOptions(streams: List<VideoStream>): List<DownloadOption> =
        streams
            .filter { it.deliveryMethod == DeliveryMethod.PROGRESSIVE_HTTP && it.isUrl && !it.isVideoOnly }
            .filter { it.format != null }
            .groupBy { it.resolution }
            .values
            .map { group -> group.first() }
            .sortedByDescending { it.height }
            .map { s ->
                val format = s.format!!
                DownloadOption(
                    id = "v-${s.id}",
                    kind = Kind.VIDEO,
                    quality = s.resolution,
                    extension = format.suffix,
                    mimeType = format.mimeType,
                    sizeBytes = s.itagItem?.contentLength?.takeIf { it > 0 },
                    url = s.content,
                )
            }
}
