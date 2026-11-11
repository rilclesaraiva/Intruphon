package com.intruphon.app.data.download

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import net.sourceforge.lame.mp3.Lame
import net.sourceforge.lame.mp3.MPEGMode
import java.io.File
import java.io.IOException
import java.nio.ByteOrder

/**
 * Turns any audio file the system can decode into an MP3 at a constant bitrate. The system decoder makes
 * PCM and the bundled LAME port (third_party/java-lame) encodes it. [prefix] (an ID3 tag) goes first.
 */
internal object Mp3Encoder {
    suspend fun convert(input: File, output: File, kbps: Int, prefix: ByteArray?) {
        val extractor = MediaExtractor()
        var codec: MediaCodec? = null
        var lame: Lame? = null
        try {
            extractor.setDataSource(input.path)
            val track = (0 until extractor.trackCount).firstOrNull {
                extractor.getTrackFormat(it).getString(MediaFormat.KEY_MIME).orEmpty().startsWith("audio/")
            } ?: throw IOException("No audio track in the downloaded file")
            extractor.selectTrack(track)
            val format = extractor.getTrackFormat(track)
            val decoder = MediaCodec.createDecoderByType(format.getString(MediaFormat.KEY_MIME)!!)
            codec = decoder
            decoder.configure(format, null, null, 0)
            decoder.start()

            output.outputStream().buffered().use { out ->
                if (prefix != null) out.write(prefix)
                val info = MediaCodec.BufferInfo()
                var channels = 0
                var left = FloatArray(0)
                var right = FloatArray(0)
                var mp3 = ByteArray(0)
                var inputDone = false
                var outputDone = false
                while (!outputDone) {
                    currentCoroutineContext().ensureActive()
                    if (!inputDone) {
                        val index = decoder.dequeueInputBuffer(10_000)
                        if (index >= 0) {
                            val buffer = decoder.getInputBuffer(index)!!
                            val size = extractor.readSampleData(buffer, 0)
                            if (size < 0) {
                                decoder.queueInputBuffer(index, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                                inputDone = true
                            } else {
                                decoder.queueInputBuffer(index, 0, size, extractor.sampleTime, 0)
                                extractor.advance()
                            }
                        }
                    }
                    val outIndex = decoder.dequeueOutputBuffer(info, 10_000)
                    if (outIndex >= 0) {
                        if (info.size > 0) {
                            if (lame == null) {
                                val outFormat = decoder.outputFormat
                                channels = outFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                                if (channels !in 1..2) throw IOException("Unsupported channel count: $channels")
                                lame = newEncoder(channels, outFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE), kbps)
                            }
                            val pcm = decoder.getOutputBuffer(outIndex)!!
                            pcm.position(info.offset)
                            pcm.limit(info.offset + info.size)
                            val samples = pcm.order(ByteOrder.nativeOrder()).asShortBuffer()
                            val frames = samples.remaining() / channels
                            if (left.size < frames) {
                                left = FloatArray(frames)
                                right = FloatArray(frames)
                                mp3 = ByteArray(frames * 5 / 4 + 7200)
                            }
                            // The encoder expects 16-bit samples scaled up by 65536.
                            for (i in 0 until frames) {
                                val l = samples.get() * 65536f
                                left[i] = l
                                right[i] = if (channels == 2) samples.get() * 65536f else l
                            }
                            val written = lame!!.encodeBuffer(left, right, frames, mp3)
                            if (written < 0) throw IOException("MP3 encoder error ($written)")
                            out.write(mp3, 0, written)
                        }
                        decoder.releaseOutputBuffer(outIndex, false)
                        if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) outputDone = true
                    }
                }
                val encoder = lame ?: throw IOException("The file has no audio")
                val tail = ByteArray(8192)
                val written = encoder.encodeFlush(tail)
                if (written > 0) out.write(tail, 0, written)
            }
        } finally {
            runCatching { codec?.stop() }
            runCatching { codec?.release() }
            runCatching { lame?.close() }
            runCatching { extractor.release() }
        }
    }

    private fun newEncoder(channels: Int, sampleRate: Int, kbps: Int): Lame {
        val lame = Lame()
        val flags = lame.flags
        flags.setInNumChannels(channels)
        flags.setInSampleRate(sampleRate)
        flags.setMode(if (channels == 1) MPEGMode.MONO else MPEGMode.JOINT_STEREO)
        flags.setBitRate(kbps)
        flags.setQuality(Lame.QUALITY_MIDDLE)
        flags.setWriteId3tagAutomatic(false)   // the tag is written by Id3v2
        flags.bWriteVbrTag = false
        flags.setFindReplayGain(false)
        lame.id3.init(flags)
        if (lame.initParams() < 0) throw IOException("MP3 encoder could not start")
        return lame
    }
}
