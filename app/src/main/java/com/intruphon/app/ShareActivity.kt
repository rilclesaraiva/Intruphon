package com.intruphon.app

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.intruphon.app.core.LinkExtractor
import com.intruphon.app.core.ui.IntruphonTheme
import com.intruphon.app.features.download.DownloadSheet

// Entry point for the Android share sheet: shows the format sheet over the app that shared the link.
class ShareActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val link = LinkExtractor.firstUrl(intent?.getStringExtra(Intent.EXTRA_TEXT))
        if (link == null) {
            Toast.makeText(this, R.string.share_no_link, Toast.LENGTH_LONG).show()
            finish()
            return
        }
        setContent {
            IntruphonTheme {
                DownloadSheet(link = link, onDismiss = { finish() })
            }
        }
    }
}
