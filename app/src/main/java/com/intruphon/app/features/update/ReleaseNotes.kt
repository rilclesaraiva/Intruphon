package com.intruphon.app.features.update

internal sealed interface NoteLine {
    data class Highlight(val text: String) : NoteLine
    data class Heading(val text: String) : NoteLine
    data class Bullet(val text: String) : NoteLine
    data class Plain(val text: String) : NoteLine
}

/**
 * Turns the Release notes (the CHANGELOG section of the version) into lines to draw.
 * A line starting with ">" is the highlight paragraph; "#" lines are headings; "-" and "*" lines are bullets.
 */
internal fun parseNotes(notes: String): List<NoteLine> = notes.lines().mapNotNull { raw ->
    val line = raw.trim()
    when {
        line.isEmpty() -> null
        line.startsWith(">") -> NoteLine.Highlight(line.trimStart('>').trim())
        line.startsWith("#") -> NoteLine.Heading(line.trimStart('#').trim())
        line.startsWith("- ") || line.startsWith("* ") -> NoteLine.Bullet(line.substring(2).trim())
        else -> NoteLine.Plain(line)
    }
}
