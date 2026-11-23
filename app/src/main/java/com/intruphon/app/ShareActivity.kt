package com.intruphon.app

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.intruphon.app.core.LinkExtractor
import com.intruphon.app.core.ui.IntruphonTheme
import com.intruphon.app.data.settings.AppSettings
import com.intruphon.app.features.download.DownloadSheet
import com.intruphon.app.features.download.SearchSheet

// Entry point for the Android share sheet: shows the format sheet (for a link) or the search results (for a name) over the app that shared it.
class ShareActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val text = intent?.getStringExtra(Intent.EXTRA_TEXT)
        val link = LinkExtractor.firstUrl(text)
        // Shared text with no link in it (a song name) is searched instead.
        val query = if (link == null) LinkExtractor.searchQuery(text) else null
        if (link == null && query == null) {
            Toast.makeText(this, R.string.share_no_link, Toast.LENGTH_LONG).show()
            finish()
            return
        }
        AppSettings.loadTheme(applicationContext)
        setContent {
            IntruphonTheme {
                if (link != null) DownloadSheet(link = link, onDismiss = { finish() })
                else if (query != null) SearchSheet(query = query, onDismiss = { finish() })
            }
        }
    }
}
