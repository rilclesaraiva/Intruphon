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

/**
 * Reads a link with NewPipeExtractor and turns it into plain download options.
 * Only qualities the source really has are offered: one audio entry per available bitrate and one
 * video entry per available resolution.
 */
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
            val audio = usableAudio(info.audioStreams)
            val options = audioOptions(audio) + mp3Options(audio, info.duration) + videoOptions(info.videoStreams, info.videoOnlyStreams, audio)
            val metadata = buildTrackMetadata(
                rawTitle = info.name,
                uploader = info.uploaderName,
                description = info.description?.content,
                uploadYear = info.uploadDate?.offsetDateTime()?.year,
                sourceUrl = link,
                coverUrl = info.thumbnails.maxByOrNull { maxOf(it.height, 0) * maxOf(it.width, 0) }?.url,
            )
            if (options.isEmpty()) LoadResult.Failed(FailReason.NO_FORMATS)
            else LoadResult.Ready(MediaInfo(info.name, metadata, options))
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

    // Only plain HTTP files are handled (no DASH/HLS manifests), and only the original audio track.
    private fun usableAudio(streams: List<AudioStream>): List<AudioStream> =
        streams.filter {
            it.deliveryMethod == DeliveryMethod.PROGRESSIVE_HTTP && it.isUrl && it.format != null &&
                (it.audioTrackType == null || it.audioTrackType == AudioTrackType.ORIGINAL)
        }

    private fun audioSize(s: AudioStream): Long? = s.itagItem?.contentLength?.takeIf { it > 0 }
    private fun videoSize(s: VideoStream): Long? = s.itagItem?.contentLength?.takeIf { it > 0 }

    private fun audioOptions(audio: List<AudioStream>): List<DownloadOption> {
        val m4a = audio.filter { it.format!!.suffix == "m4a" }
        if (m4a.isEmpty()) {
            // No M4A on this site: offer the best file as it is.
            val best = audio.maxByOrNull { it.averageBitrate } ?: return emptyList()
            val format = best.format!!
            return listOf(
                DownloadOption(
                    id = "a-${best.id}", kind = Kind.AUDIO, format = format.suffix.uppercase(),
                    quality = bitrateLabel(best.averageBitrate), extension = format.suffix,
                    mimeType = format.mimeType, sizeBytes = audioSize(best), plan = Plan.Direct(best.content),
                ),
            )
        }
        return m4a
            .groupBy { it.averageBitrate }
            .values
            .map { group -> group.first() }
            .sortedByDescending { it.averageBitrate }
            .map { s ->
                DownloadOption(
                    id = "a-${s.id}", kind = Kind.AUDIO, format = "M4A", quality = bitrateLabel(s.averageBitrate),
                    extension = "m4a", mimeType = "audio/mp4", sizeBytes = audioSize(s), plan = Plan.AudioM4a(s.content),
                )
            }
    }

    // MP3 is made from the best audio the site has, and no MP3 bitrate is offered above that source's own bitrate.
    private fun mp3Options(audio: List<AudioStream>, durationSeconds: Long): List<DownloadOption> {
        val source = audio.filter { it.format!!.suffix == "m4a" }.maxByOrNull { it.averageBitrate }
            ?: audio.maxByOrNull { it.averageBitrate }
            ?: return emptyList()
        val cap = if (source.averageBitrate > 0) source.averageBitrate else 128
        val steps = listOf(64, 96, 128, 160, 192, 256, 320).filter { it <= cap }.ifEmpty { listOf(32) }
        return steps.sortedDescending().map { kbps ->
            DownloadOption(
                id = "mp3-$kbps", kind = Kind.AUDIO, format = "MP3", quality = "$kbps kbps", extension = "mp3",
                mimeType = "audio/mpeg", sizeBytes = durationSeconds.takeIf { it > 0 }?.let { it * kbps * 125 },
                plan = Plan.AudioMp3(source.content, kbps),
            )
        }
    }

    private fun bitrateLabel(kbps: Int) = if (kbps > 0) "$kbps kbps" else ""

    private fun videoOptions(muxed: List<VideoStream>, videoOnly: List<VideoStream>, audio: List<AudioStream>): List<DownloadOption> {
        val ready = muxed.filter { it.deliveryMethod == DeliveryMethod.PROGRESSIVE_HTTP && it.isUrl && it.format != null && !it.isVideoOnly }
        val only = videoOnly.filter { it.deliveryMethod == DeliveryMethod.PROGRESSIVE_HTTP && it.isUrl && it.format != null }
        val bestM4a = audio.filter { it.format!!.suffix == "m4a" }.maxByOrNull { it.averageBitrate }
        val bestWebm = audio.filter { it.format!!.suffix == "webm" }.maxByOrNull { it.averageBitrate }

        val heights = (ready.map { it.height } + only.map { it.height }).filter { it > 0 }.distinct().sortedDescending()
        return heights.mapNotNull { h ->
            // 1. A ready file with sound is the most reliable, so it comes first.
            ready.filter { it.height == h }.firstOrNull()?.let { s ->
                val format = s.format!!
                return@mapNotNull DownloadOption(
                    id = "v-${s.id}", kind = Kind.VIDEO, format = format.suffix.uppercase(), quality = qualityLabel(h, s.fps),
                    extension = format.suffix, mimeType = format.mimeType, sizeBytes = videoSize(s), plan = Plan.Direct(s.content),
                )
            }
            // 2. H.264 video + AAC audio joined into an MP4.
            val avc = only.filter { it.height == h && it.format!!.suffix == "mp4" && it.codec?.startsWith("avc1") == true }.maxByOrNull { it.fps }
            if (avc != null && bestM4a != null) {
                return@mapNotNull DownloadOption(
                    id = "v-${avc.id}", kind = Kind.VIDEO, format = "MP4", quality = qualityLabel(h, avc.fps),
                    extension = "mp4", mimeType = "video/mp4", sizeBytes = sum(videoSize(avc), audioSize(bestM4a)),
                    plan = Plan.Mux(avc.content, bestM4a.content, Container.MP4),
                )
            }
            // 3. VP9 video + Opus audio joined into a WebM.
            val vp9 = only.filter { it.height == h && it.format!!.suffix == "webm" && (it.codec?.startsWith("vp9") == true || it.codec?.startsWith("vp09") == true) }.maxByOrNull { it.fps }
            if (vp9 != null && bestWebm != null) {
                return@mapNotNull DownloadOption(
                    id = "v-${vp9.id}", kind = Kind.VIDEO, format = "WEBM", quality = qualityLabel(h, vp9.fps),
                    extension = "webm", mimeType = "video/webm", sizeBytes = sum(videoSize(vp9), audioSize(bestWebm)),
                    plan = Plan.Mux(vp9.content, bestWebm.content, Container.WEBM),
                )
            }
            null
        }
    }

    private fun qualityLabel(height: Int, fps: Int) = if (fps > 30) "${height}p$fps" else "${height}p"

    private fun sum(a: Long?, b: Long?): Long? = if (a != null && b != null) a + b else null
}
