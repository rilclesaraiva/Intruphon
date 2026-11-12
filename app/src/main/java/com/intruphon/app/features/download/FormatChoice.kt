package com.intruphon.app.features.download

import com.intruphon.app.data.media.DownloadOption
import com.intruphon.app.data.media.Kind

internal enum class End { HIGHEST, SMALLEST }

internal data class Pick(val option: DownloadOption, val end: End?)

/** The number in a quality label: 1080 for "1080p", 128 for "128 kbps". */
internal fun rank(option: DownloadOption): Int = option.quality.takeWhile { it.isDigit() }.toIntOrNull() ?: 0

/**
 * The two entries shown before "More formats": the highest and the smallest quality of [options].
 * On a tie the highest prefers a format that was not converted (M4A over MP3) and the smallest the smaller file.
 * With a single distinct choice there is one entry and no label.
 */
internal fun picks(options: List<DownloadOption>): List<Pick> {
    if (options.isEmpty()) return emptyList()
    val highest = options.maxWithOrNull(
        compareBy<DownloadOption>({ rank(it) }, { if (it.format == "MP3") 0 else 1 }, { it.sizeBytes ?: 0L }),
    )!!
    val smallest = options.minWithOrNull(compareBy<DownloadOption>({ rank(it) }, { it.sizeBytes ?: Long.MAX_VALUE }))!!
    return if (highest.id == smallest.id) listOf(Pick(highest, null))
    else listOf(Pick(highest, End.HIGHEST), Pick(smallest, End.SMALLEST))
}

/** What a whole playlist is saved as: one rule applied to every item, since each item has its own list of formats. */
internal enum class PlaylistFormat(val kind: Kind, val label: Int) {
    AUDIO_HIGHEST(Kind.AUDIO, com.intruphon.app.R.string.playlist_fmt_audio_high),
    MP3(Kind.AUDIO, com.intruphon.app.R.string.playlist_fmt_mp3),
    AUDIO_SMALLEST(Kind.AUDIO, com.intruphon.app.R.string.playlist_fmt_audio_small),
    VIDEO_HIGHEST(Kind.VIDEO, com.intruphon.app.R.string.playlist_fmt_video_high),
    VIDEO_SMALLEST(Kind.VIDEO, com.intruphon.app.R.string.playlist_fmt_video_small);

    /** The option of [options] this rule asks for, or null when the item has none of that kind. */
    fun pick(options: List<DownloadOption>): DownloadOption? {
        val ofKind = options.filter { it.kind == kind }
        return when (this) {
            MP3 -> ofKind.filter { it.format == "MP3" }.maxByOrNull { rank(it) }
            AUDIO_HIGHEST, VIDEO_HIGHEST -> picks(ofKind).firstOrNull()?.option
            AUDIO_SMALLEST, VIDEO_SMALLEST -> picks(ofKind).lastOrNull()?.option
        }
    }
}
