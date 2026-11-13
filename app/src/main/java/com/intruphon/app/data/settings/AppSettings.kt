package com.intruphon.app.data.settings

import android.content.Context
import com.intruphon.app.data.net.NetworkKind
import kotlinx.coroutines.flow.MutableStateFlow

/** The few options the user can change, kept in SharedPreferences on the device. */
object AppSettings {
    const val MIN_PARALLEL = 1
    const val MAX_PARALLEL = 10
    private const val DEFAULT_PARALLEL = 3

    private fun prefs(context: Context) = context.applicationContext.getSharedPreferences("settings", Context.MODE_PRIVATE)

    fun wifiOnly(context: Context): Boolean = prefs(context).getBoolean("wifi_only", false)

    fun setWifiOnly(context: Context, value: Boolean) = prefs(context).edit().putBoolean("wifi_only", value).apply()

    // One limit for Wi-Fi (and ethernet) and one for mobile data; the old single value seeds both.
    private fun legacyParallel(context: Context) = prefs(context).getInt("max_parallel", DEFAULT_PARALLEL)

    fun maxParallelWifi(context: Context): Int =
        prefs(context).getInt("max_parallel_wifi", legacyParallel(context)).coerceIn(MIN_PARALLEL, MAX_PARALLEL)

    fun maxParallelMobile(context: Context): Int =
        prefs(context).getInt("max_parallel_mobile", legacyParallel(context)).coerceIn(MIN_PARALLEL, MAX_PARALLEL)

    fun setMaxParallelWifi(context: Context, value: Int) =
        prefs(context).edit().putInt("max_parallel_wifi", value.coerceIn(MIN_PARALLEL, MAX_PARALLEL)).apply()

    fun setMaxParallelMobile(context: Context, value: Int) =
        prefs(context).edit().putInt("max_parallel_mobile", value.coerceIn(MIN_PARALLEL, MAX_PARALLEL)).apply()

    /** The limit that applies to the connection the phone is using right now. */
    fun maxParallelNow(context: Context): Int =
        if (NetworkKind.isWifiOrEthernet(context)) maxParallelWifi(context) else maxParallelMobile(context)

    /** The folder picked with Android's folder picker (a tree Uri as text), or null for Download/Intruphon. */
    fun downloadFolder(context: Context): String? = prefs(context).getString("download_folder", null)

    fun setDownloadFolder(context: Context, value: String?) {
        prefs(context).edit().apply { if (value == null) remove("download_folder") else putString("download_folder", value) }.apply()
    }

    /** Predictive back animation between pages; on by default. Live value for the screen. */
    val predictiveBackFlow = MutableStateFlow(true)

    fun loadPredictiveBack(context: Context) {
        predictiveBackFlow.value = prefs(context).getBoolean("predictive_back", true)
    }

    fun setPredictiveBack(context: Context, value: Boolean) {
        prefs(context).edit().putBoolean("predictive_back", value).apply()
        predictiveBackFlow.value = value
    }
}
