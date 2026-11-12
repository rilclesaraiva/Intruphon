package com.intruphon.app.features.update

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.intruphon.app.R
import com.intruphon.app.core.ui.FloatingCircleButton
import com.intruphon.app.data.update.MONTH_MS
import com.intruphon.app.data.update.ReleaseInfo
import com.intruphon.app.data.update.UpdateRepository
import com.intruphon.app.data.update.WEEK_MS

/** Mounted at the root of the app by the entry point, so it works over any screen. */
@Composable
fun UpdatePrompt() {
    val release by UpdateRepository.prompt.collectAsState()
    release?.let { UpdatePanel(it) }
}

@Composable
private fun UpdatePanel(release: ReleaseInfo) {
    val context = LocalContext.current
    var menu by remember { mutableStateOf(false) }
    Dialog(
        onDismissRequest = { UpdateRepository.dismissForSession() },
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
            Box(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
                Column(Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Box(Modifier.fillMaxWidth().height(80.dp).padding(end = 72.dp), contentAlignment = Alignment.CenterStart) {
                            Text(stringResource(R.string.update_title), style = MaterialTheme.typography.headlineLarge)
                        }
                        Text(stringResource(R.string.update_version, release.version), style = MaterialTheme.typography.titleMedium)
                        parseNotes(release.notes).forEach { line ->
                            when (line) {
                                is NoteLine.Highlight -> Surface(
                                    shape = MaterialTheme.shapes.large,
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                ) {
                                    Text(line.text, Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onPrimaryContainer)
                                }
                                is NoteLine.Heading -> Text(line.text, style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 8.dp))
                                is NoteLine.Bullet -> Text("•  " + line.text, style = MaterialTheme.typography.bodyMedium)
                                is NoteLine.Plain -> Text(line.text, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                    Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Button(
                            onClick = {
                                // Opens the APK of the Release (or its page); the system installs it.
                                val url = release.apkUrl ?: release.pageUrl
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                            },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                        ) { Text(stringResource(R.string.update_download), style = MaterialTheme.typography.titleMedium) }
                        TextButton(onClick = { UpdateRepository.dismissForSession() }, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.update_not_now))
                        }
                    }
                }
                Box(Modifier.align(Alignment.TopEnd).padding(12.dp)) {
                    FloatingCircleButton(
                        icon = Icons.Default.Notifications,
                        contentDescription = stringResource(R.string.update_remind),
                        onClick = { menu = true },
                    )
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.update_week)) },
                            onClick = { menu = false; UpdateRepository.snooze(WEEK_MS) },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.update_month)) },
                            onClick = { menu = false; UpdateRepository.snooze(MONTH_MS) },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.update_skip)) },
                            onClick = { menu = false; UpdateRepository.skip(release.version) },
                        )
                    }
                }
            }
        }
    }
}
