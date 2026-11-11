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

    fun join(inputs: List<File>, output: File, container: Container) {
        val format = if (container == Container.MP4) MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4 else MediaMuxer.OutputFormat.MUXER_OUTPUT_WEBM
        val muxer = MediaMuxer(output.path, format)
        val extractors = mutableListOf<MediaExtractor>()
        try {
            val active = mutableListOf<Pair<MediaExtractor, Int>>()   // extractor + its track in the muxer
            for (file in inputs) {
                val extractor = MediaExtractor()
                extractors += extractor
                extractor.setDataSource(file.path)
                val index = (0 until extractor.trackCount).firstOrNull {
                    val mime = extractor.getTrackFormat(it).getString(MediaFormat.KEY_MIME).orEmpty()
                    mime.startsWith("video/") || mime.startsWith("audio/")
                } ?: throw IOException("No media track in the downloaded file")
                extractor.selectTrack(index)
                active += extractor to muxer.addTrack(extractor.getTrackFormat(index))
            }
            muxer.start()
            val buffer = ByteBuffer.allocate(BUFFER_SIZE)
            val info = MediaCodec.BufferInfo()
            // Write samples in time order so audio and video stay interleaved.
            while (true) {
                active.removeAll { it.first.sampleTime < 0 }
                if (active.isEmpty()) break
                val (extractor, track) = active.minBy { it.first.sampleTime }
                buffer.clear()
                val size = extractor.readSampleData(buffer, 0)
                if (size < 0) {
                    active.removeAll { it.first === extractor }
                    continue
                }
                val key = extractor.sampleFlags and MediaExtractor.SAMPLE_FLAG_SYNC != 0
                info.set(0, size, extractor.sampleTime, if (key) MediaCodec.BUFFER_FLAG_KEY_FRAME else 0)
                muxer.writeSampleData(track, buffer, info)
                extractor.advance()
            }
            muxer.stop()
        } finally {
            runCatching { muxer.release() }
            extractors.forEach { runCatching { it.release() } }
        }
    }
}
