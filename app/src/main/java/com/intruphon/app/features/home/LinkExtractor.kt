package com.intruphon.app.features.home

/** Finds the first web link inside text shared by other apps (which often add a title or a caption). */
object LinkExtractor {
    private val url = Regex("""https?://\S+""", RegexOption.IGNORE_CASE)

    fun firstUrl(text: String?): String? =
        text?.let { url.find(it)?.value?.trimEnd('.', ',', ';', ')', '"', '\'') }
}
