package com.intruphon.app.data.update

/** A published Release: [version] without the "v", the notes text (the CHANGELOG section), its page and its APK file if any. */
data class ReleaseInfo(val version: String, val notes: String, val pageUrl: String, val apkUrl: String?)

/** What the person answered with the bell: remind later (until [snoozeUntilMs]) or skip one version. */
data class UpdateChoices(val snoozeUntilMs: Long = 0L, val skippedVersion: String? = null) {
    fun snoozed(nowMs: Long) = snoozeUntilMs > nowMs
}

const val CHECK_INTERVAL_MS = 10 * 60 * 1000L
const val WEEK_MS = 7 * 24 * 60 * 60 * 1000L
const val MONTH_MS = 30 * 24 * 60 * 60 * 1000L

/** Whether the panel should open by itself. */
fun shouldPrompt(
    release: ReleaseInfo?,
    installed: String,
    choices: UpdateChoices,
    nowMs: Long,
    handledThisSession: Boolean,
): Boolean = release != null &&
    isNewer(release.version, installed) &&
    !handledThisSession &&
    release.version != choices.skippedVersion &&
    !choices.snoozed(nowMs)

/** Opening the app always asks; coming back asks at most every ten minutes. */
fun dueForCheck(lastAnsweredMs: Long, nowMs: Long, force: Boolean): Boolean =
    force || lastAnsweredMs <= 0L || nowMs - lastAnsweredMs >= CHECK_INTERVAL_MS || nowMs < lastAnsweredMs
