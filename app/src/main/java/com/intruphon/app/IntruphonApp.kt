package com.intruphon.app

import android.app.Application
import com.intruphon.app.data.download.DownloadEngine

class IntruphonApp : Application() {
    override fun onCreate() {
        super.onCreate()
        DownloadEngine.init(this)
    }
}
