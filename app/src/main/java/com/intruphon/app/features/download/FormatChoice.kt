package com.intruphon.app.features.download

import com.intruphon.app.data.media.DownloadOption

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
