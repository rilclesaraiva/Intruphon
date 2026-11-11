package com.intruphon.app.data.net

import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/** One HTTP client for reading links and for downloading files. */
object Http {
    const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:128.0) Gecko/20100101 Firefox/128.0"

    val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
}
