package com.intruphon.app.data.update

import android.content.Context
import com.intruphon.app.core.Config
import com.intruphon.app.data.net.Http
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.json.JSONObject

enum class CheckResult { UP_TO_DATE, AVAILABLE, FAILED }

/**
 * Looks for a newer public Release of the app on GitHub (the only network call that is not about a link
 * the person gave). Keeps the last answer and the person's choices on the device.
 *
 * The request carries the saved ETag, so "not changed" (304) does not count against GitHub's rate limit.
 * The time of the last check is only saved when GitHub answered; being offline is not a check.
 */
object UpdateRepository {
    private const val PREFS = "updates"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val gate = Mutex()

    private lateinit var app: Context
    private var installed = ""
    private var handledThisSession = false

    private val _prompt = MutableStateFlow<ReleaseInfo?>(null)
    /** The release the full-screen panel should show now, or null. */
    val prompt: StateFlow<ReleaseInfo?> = _prompt

    private val _choices = MutableStateFlow(UpdateChoices())
    /** The bell answers, for the Settings screen. */
    val choices: StateFlow<UpdateChoices> = _choices

    private val prefs get() = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** Called when the app opens. [fresh] is true when it starts from scratch (not a rotation or restore). */
    fun start(context: Context, installedVersion: String, fresh: Boolean) {
        app = context.applicationContext
        installed = installedVersion
        _choices.value = readChoices()
        if (fresh) handledThisSession = false
        refresh()   // show the last newer version already known, without waiting for the network
        if (fresh) scope.launch { check(force = true) }
    }

    /** Called each time the app comes back to the front. */
    fun onResume() {
        if (!::app.isInitialized) return
        scope.launch { check(force = false) }
    }

    /** Manual check from Settings: ignores "not now", snooze and skip. */
    suspend fun checkNow(): CheckResult {
        val answered = withContext(Dispatchers.IO) { check(force = true) }
        if (!answered) return CheckResult.FAILED
        val release = readRelease()
        return if (release != null && isNewer(release.version, installed)) {
            handledThisSession = false
            _prompt.value = release
            CheckResult.AVAILABLE
        } else CheckResult.UP_TO_DATE
    }

    /** "Not now": only until the app is opened again. */
    fun dismissForSession() {
        handledThisSession = true
        _prompt.value = null
    }

    fun snooze(durationMs: Long) = answer { it.copy(snoozeUntilMs = System.currentTimeMillis() + durationMs) }

    fun skip(version: String) = answer { it.copy(skippedVersion = version) }

    /** Turns the notices back on after a snooze or a skip. */
    fun undoChoices() {
        writeChoices(UpdateChoices())
        handledThisSession = false
        refresh()
    }

    private fun answer(change: (UpdateChoices) -> UpdateChoices) {
        writeChoices(change(_choices.value))
        handledThisSession = true
        _prompt.value = null
    }

    private fun refresh() {
        val release = readRelease()
        val show = shouldPrompt(release, installed, _choices.value, System.currentTimeMillis(), handledThisSession)
        _prompt.value = if (show) release else null
    }

    /** Returns true when GitHub answered (new data, unchanged, or no Release yet). */
    private suspend fun check(force: Boolean): Boolean {
        if (!gate.tryLock()) return false
        try {
            val now = System.currentTimeMillis()
            if (!dueForCheck(prefs.getLong("last_answer", 0L), now, force)) return true
            val etag = prefs.getString("etag", null)
            val builder = Request.Builder()
                .url(Config.RELEASES_API)
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", "Intruphon")
            if (etag != null && readRelease() != null) builder.header("If-None-Match", etag)
            val answered = try {
                Http.client.newCall(builder.build()).execute().use { response ->
                    when (response.code) {
                        304 -> true
                        200 -> {
                            val release = parse(response.body?.string().orEmpty()) ?: return@use false
                            // A new answer always replaces the saved one, so the panel never shows old notes.
                            save(release, response.header("ETag"))
                            true
                        }
                        404 -> { clearSaved(); true }   // no public Release yet
                        else -> false
                    }
                }
            } catch (e: Exception) {
                false
            }
            if (answered) prefs.edit().putLong("last_answer", System.currentTimeMillis()).apply()
            refresh()
            return answered
        } finally {
            gate.unlock()
        }
    }

    private fun parse(body: String): ReleaseInfo? = try {
        val json = JSONObject(body)
        val tag = json.getString("tag_name")
        val assets = json.optJSONArray("assets")
        var apk: String? = null
        if (assets != null) for (i in 0 until assets.length()) {
            val asset = assets.getJSONObject(i)
            if (asset.optString("name").endsWith(".apk")) { apk = asset.optString("browser_download_url").ifEmpty { null }; break }
        }
        ReleaseInfo(tag.removePrefix("v"), json.optString("body"), json.optString("html_url"), apk)
    } catch (e: Exception) {
        null
    }

    private fun save(release: ReleaseInfo, etag: String?) {
        prefs.edit()
            .putString("version", release.version)
            .putString("notes", release.notes)
            .putString("page", release.pageUrl)
            .putString("apk", release.apkUrl)
            .putString("etag", etag)
            .apply()
    }

    private fun clearSaved() {
        prefs.edit().remove("version").remove("notes").remove("page").remove("apk").remove("etag").apply()
    }

    private fun readRelease(): ReleaseInfo? {
        val version = prefs.getString("version", null) ?: return null
        return ReleaseInfo(version, prefs.getString("notes", "").orEmpty(), prefs.getString("page", "").orEmpty(), prefs.getString("apk", null))
    }

    private fun readChoices() = UpdateChoices(prefs.getLong("snooze_until", 0L), prefs.getString("skipped", null))

    private fun writeChoices(choices: UpdateChoices) {
        prefs.edit().putLong("snooze_until", choices.snoozeUntilMs).putString("skipped", choices.skippedVersion).apply()
        _choices.value = choices
    }
}
