package com.intruphon.app.data.download

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import com.intruphon.app.data.media.Container
import java.io.File
import java.io.IOException
import java.nio.ByteBuffer

/** Joins the first audio or video track of each input file into one file, with the system muxer. */
internal object MediaMuxing {
    private const val BUFFER_SIZE = 4 * 1024 * 1024

    /** Returns the number of samples written; throws with details when none were, so the caller can fall back. */
    fun join(inputs: List<File>, output: File, container: Container): Int {
        val format = if (container == Container.MP4) MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4 else MediaMuxer.OutputFormat.MUXER_OUTPUT_WEBM
        val muxer = MediaMuxer(output.path, format)
        val extractors = mutableListOf<MediaExtractor>()
        var written = 0
        val starts = mutableListOf<Long>()   // time of the first sample of each input
        val details = StringBuilder()
        try {
            val active = mutableListOf<Pair<MediaExtractor, Int>>()   // extractor + its track in the muxer
            val perInput = mutableMapOf<MediaExtractor, Int>()
            val firstRead = mutableMapOf<MediaExtractor, Int>()
            for (file in inputs) {
                val extractor = MediaExtractor()
                extractors += extractor
                extractor.setDataSource(file.path)
                val index = (0 until extractor.trackCount).firstOrNull {
                    val mime = extractor.getTrackFormat(it).getString(MediaFormat.KEY_MIME).orEmpty()
                    mime.startsWith("video/") || mime.startsWith("audio/")
                } ?: throw IOException("No media track in the downloaded file")
                extractor.selectTrack(index)
                starts += extractor.sampleTime
                details.append("[${file.length()}B tracks=${extractor.trackCount} mime=${extractor.getTrackFormat(index).getString(MediaFormat.KEY_MIME)} t0=${extractor.sampleTime}] ")
                active += extractor to muxer.addTrack(extractor.getTrackFormat(index))
            }
            // Audio files often start with a slightly negative time (encoder delay). The muxer wants times from 0,
            // so every track is moved by the same amount, which keeps audio and video in sync.
            val shift = starts.maxOfOrNull { maxOf(0L, -it) } ?: 0L
            muxer.start()
            val buffer = ByteBuffer.allocate(BUFFER_SIZE)
            val info = MediaCodec.BufferInfo()
            // Write samples in time order so audio and video stay interleaved.
            while (true) {
                if (active.isEmpty()) break
                val (extractor, track) = active.minBy { it.first.sampleTime }
                buffer.clear()
                val size = extractor.readSampleData(buffer, 0)
                if (firstRead[extractor] == null) firstRead[extractor] = size
                if (size < 0) {
                    active.removeAll { it.first === extractor }
                    continue
                }
                val key = extractor.sampleFlags and MediaExtractor.SAMPLE_FLAG_SYNC != 0
                info.set(0, size, maxOf(0L, extractor.sampleTime + shift), if (key) MediaCodec.BUFFER_FLAG_KEY_FRAME else 0)
                muxer.writeSampleData(track, buffer, info)
                written++
                perInput[extractor] = (perInput[extractor] ?: 0) + 1
                extractor.advance()
            }
            // Every input must have contributed samples; otherwise a track (for example the sound) would silently be missing.
            val missing = extractors.indices.filter { (perInput[extractors[it]] ?: 0) == 0 }
            if (written == 0 || missing.isNotEmpty()) {
                val report = extractors.indices.joinToString(" ") { "input$it:samples=${perInput[extractors[it]] ?: 0},firstRead=${firstRead[extractors[it]]}" }
                throw IOException("A track gave no samples $report $details")
            }
            muxer.stop()
            return written
        } finally {
            runCatching { muxer.release() }
            extractors.forEach { runCatching { it.release() } }
        }
    }
}
