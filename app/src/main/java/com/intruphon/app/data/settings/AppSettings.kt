package com.intruphon.app.data.settings

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow

/** The few options the user can change, kept in SharedPreferences on the device. */
object AppSettings {
    const val MIN_PARALLEL = 1
    const val MAX_PARALLEL = 5
    private const val DEFAULT_PARALLEL = 3

    private fun prefs(context: Context) = context.applicationContext.getSharedPreferences("settings", Context.MODE_PRIVATE)

    fun wifiOnly(context: Context): Boolean = prefs(context).getBoolean("wifi_only", false)

    fun setWifiOnly(context: Context, value: Boolean) = prefs(context).edit().putBoolean("wifi_only", value).apply()

    fun maxParallel(context: Context): Int =
        prefs(context).getInt("max_parallel", DEFAULT_PARALLEL).coerceIn(MIN_PARALLEL, MAX_PARALLEL)

    fun setMaxParallel(context: Context, value: Int) =
        prefs(context).edit().putInt("max_parallel", value.coerceIn(MIN_PARALLEL, MAX_PARALLEL)).apply()

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
