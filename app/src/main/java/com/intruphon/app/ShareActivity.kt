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

// Entry point for the Android share sheet: shows the format sheet over the app that shared a link; a shared name opens the search page of the main app.
class ShareActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val text = intent?.getStringExtra(Intent.EXTRA_TEXT)
        val link = LinkExtractor.firstUrl(text)
        // Shared text with no link in it (a song name) is searched instead.
        val query = if (link == null) LinkExtractor.searchQuery(text) else null
        if (query != null) {
            // A name opens the search page of the app itself, not a pop-up.
            startActivity(Intent(this, MainActivity::class.java).putExtra(MainActivity.EXTRA_SEARCH, query).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            finish()
            return
        }
        if (link == null) {
            Toast.makeText(this, R.string.share_no_link, Toast.LENGTH_LONG).show()
            finish()
            return
        }
        AppSettings.loadTheme(applicationContext)
        setContent {
            IntruphonTheme {
                DownloadSheet(link = link, onDismiss = { finish() })
            }
        }
    }
}
