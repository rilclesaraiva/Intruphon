package com.intruphon.app.data.net

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

/** What kind of connection the phone is using right now (needs ACCESS_NETWORK_STATE). */
object NetworkKind {
    fun current(context: Context): String {
        val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return "unknown"
        val caps = manager.getNetworkCapabilities(manager.activeNetwork ?: return "none") ?: return "none"
        return when {
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "wifi"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "ethernet"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "cellular"
            else -> "other"
        }
    }

    fun isWifiOrEthernet(context: Context): Boolean = current(context).let { it == "wifi" || it == "ethernet" }
}
