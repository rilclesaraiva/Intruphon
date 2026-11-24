# Changelog

## [Unreleased]

### Changed
- Home: the link field keeps the same height with or without focus; its label is now one small line ("Paste the link here or a song name") that never wraps.
- Build: the Gradle wrapper (`gradlew`, Gradle 8.9) is now in the repository and the CI uses it, so the Gradle version lives in one place.
- Navigation no longer drops a tap while a page is sliding: opening a category right after Settings, or going back twice in a row, finishes the running slide at once and starts the new one, with the Back button, the system back and the back gesture alike.
- Licenses now open a page in the browser (`docs/licenses.md`, published on GitHub Pages) instead of a panel inside the app.
- Settings: the home page now lists only categories (Appearance, Downloads, System), each with its own colored icon and a one-line summary, with "About Intruphon" below. Appearance holds theme, system colors, predictive back and language; Downloads holds Wi-Fi only, simultaneous downloads and the folder; System holds updates and the speed log.
- Settings: theme and update notices now open their choices right in the row (closed by default) instead of a bottom panel.
- Bottom sheets: going back slides the sheet down with the normal closing animation instead of the system shrinking one. In the playlist sheet only the handle strip drags it, so scrolling the list and pulling on no longer closes it and loses your picks.
- Home: the link field loses focus (no more blinking cursor) when the keyboard is closed.
- Picture caches (thumbnails and covers) are now sized by memory (a sixteenth of what Android gives the app) instead of a fixed number of items.
- Internals: named motion springs in one file, the selection state and the About icon frame are now pure and unit-tested, and the downloads list encoding is tested.
- Build: all dependency and plugin versions now live in `gradle/libs.versions.toml`, and the app version lives only in `gradle.properties` (`APP_VERSION`). The CI posts the error summary as a commit comment when a build fails.

### Added
- Search by name: type a song or video name on the home screen (the button becomes Search), or share plain text with no link to Intruphon, and the app opens a Search page, laid out like YouTube's. While you edit the field it lists similar searches to pick from; sending a search lists the YouTube results with cover, title, channel and length, with a Videos / Songs choice. Tapping a result opens the usual format sheet. Only the first page of results is shown.
- Settings → Appearance: "Use system colors" (Android 12+), off by default. By default the app now uses its own colors, taken from the icon (pale ice blue on deep blue-black, with a light variant); turn it on to follow the wallpaper colors.

## [0.1.0] - 2026-11-15

### Added
- New app icon: an intruder peeking from behind a funnel of three bars, one color on a deep blue-black background, with a one-color layer for Android 13+ themed icons. The drawing is a little larger than the launcher's safe zone on purpose.
- About: the icon is animated. The intruder looks left, right, then straight at you, and settles behind the funnel exactly as in the launcher icon (about 2 seconds). It plays every time About opens and again on every tap; with system animations turned off it just shows the final icon.
- Settings → Appearance: theme choice (system default, light, dark, AMOLED with pure black backgrounds). The system bar icons follow the chosen theme.
- README and About: a short, neutral note on sources (tested: YouTube and YouTube Music; other services the extractor knows are untested and may not work).
- Settings: choose the download folder with Android's own folder picker (no storage permission). Audio and Video subfolders are created inside it; "Back to default folder" returns to Download/Intruphon. Files already saved stay where they are. If the folder permission is lost, the app falls back to the default folder.
- Settings: two separate limits for simultaneous downloads, one for Wi-Fi and one for mobile data (1 to 10 each); the one for the current connection applies. The old single value is used as the starting point for both.
- Downloads: tap a row to open it (or retry it, if failed); long-press or the three-dot button opens a bottom panel (open, select, delete or remove). Tapping only the cover starts multi-select, with a "close + count" pill and a "select all / more" pill; for now the only bulk action is delete.
- Delete: Intruphon asks in two steps (remove from the list only, or delete from the device) and shows its own "Allow Intruphon to delete this file?" window with the cover, because Android shows no system window for files the app created itself.
- Playlists: items that cannot be downloaded are checked beforehand, shown dimmed with a "!" over the cover, cannot be selected, and are skipped silently (no failed entries).
- Downloads: a card at the top groups everything that is downloading and opens a new "Downloading" page where each download has its own cancel button and there is a "Cancel all" (with confirmation).
- Downloads: failed downloads pile up in a "Failed" section at the top of the list, with "Remove all".
- Downloads in progress and failed items show their cover from the first moment (the picture comes from the source link), not only after the file is saved.
- Clear failure messages for playlist items: "could not be read" and "no format of the kind you chose"; the playlist reader tries each item twice before giving up. Unknown failures now say "unknown error" instead of "error 0".
- Playlists: a playlist link (for example a YouTube Music playlist) opens a sheet with every item, its thumbnail and length, "Select all", and one format for the whole list (audio highest, MP3, audio smallest, video highest or smallest). Items are read and queued one by one; an item that cannot be read appears as a failed entry in Downloads.
- Initial project structure: Compose app shell, home screen and About screen.
- Original adaptive launcher icon (with monochrome layer).
- CI build with R8 (no obfuscation) and signed release workflow.
- Privacy policy and terms (`docs/`).
- Share target: Intruphon appears in the Android share sheet and opens a pop-up sheet over the app that shared the link (no full-screen switch).
- Real downloads: the app reads the link with NewPipeExtractor, lists the available audio and video formats and downloads the chosen one with the app's own downloader (saved in Download/Intruphon/Audio or Download/Intruphon/Video, in ranged chunks, no storage permission needed).
- The Download button on the home screen opens the same format sheet.
- Downloads tab: shows each download with progress, status and the HTTP error code when it fails, and lets you open the file, cancel, delete, remove the entry or try again.
- Progress notification with a Cancel all action, and a notification when a download finishes or fails.
- Permissions: INTERNET (read links and download), POST_NOTIFICATIONS (asked at the first download on Android 13+; downloads work if you refuse), FOREGROUND_SERVICE and FOREGROUND_SERVICE_DATA_SYNC (keep downloads running with the screen off).
- Video quality list: every resolution the source really has (for example 360p up to 1080p and beyond), never more. Resolutions without a ready file are built by joining the separate video and audio streams on the device (MP4 with H.264 + AAC, or WebM with VP9 + Opus).
- Audio list: one M4A entry per bitrate the source really has.
- Tags in audio files: title, artist, album, year, source link and square cover art (from the channel name, the video description and the thumbnail). Video MP4 files get title, artist, year and source link.
- The Downloads tab shows whether tags and cover were written into each file.
- MP3 downloads: pick a constant bitrate (64 to 320 kbps, never above the bitrate the source really has). The app decodes the audio with Android's own decoder and encodes it with a bundled Java port of LAME (`third_party/java-lame`, LGPL); the MP3 gets an ID3v2.3 tag with title, artist, album, year, source link and cover.
- Downloads tab: each item shows its cover (or a video frame) and its length, read from the saved file; a "Processing" state is shown while a file is being joined, converted or tagged.
- Settings tab: download only on Wi-Fi (adds the `ACCESS_NETWORK_STATE` permission) and the number of simultaneous downloads (1 to 10 for Wi-Fi and for mobile data separately, the rest wait their turn).
- Speed log: every download records format, quality, connection type, bytes, download time, processing time and result in a file on the device and in logcat (tag `IntruphonSpeed`); Settings shows the average speed per format and quality and can share the log as CSV or clear it.
- Languages: English, Spanish and Portuguese (Brazil), following the Android language. A button in Settings opens Android's own per-app language screen (on Android 12 and older, the system language screen).
- The About screen shows the maintainer's public GitHub profile picture (downloaded when the screen opens and cached for a week; an icon shows when it cannot load) and uses the same icons as the maintainer's other app (code, badge, bug, lock, article, heart). The privacy policy describes this extra request.
- Update notice: when the app opens (and at most every 10 minutes when you come back to it) it asks GitHub for the latest public Release. If it is newer than the installed version, a full-screen panel shows the Release notes with a Download button that opens the APK. "Not now" lasts until the app is opened again; the bell reminds in 1 week or 1 month, or skips that version. The saved ETag keeps repeated checks from using GitHub's rate limit. Settings has a System section to check by hand and to turn the notices back on. The privacy policy and README describe this new network call.
- Deleting a finished download asks whether to remove it from the list only or also delete the file from the device. Deleting from the device uses Android's own delete confirmation (no storage permission).
- About screen rebuilt in the style of rows with round icons and chips, as a page opened from Settings: icon, name, version, build number and build type, developer, source code, what's new (the changelog), report a problem, privacy policy, terms, licenses and attributions, and the invitation to the repository.
### Changed
- Settings: simultaneous downloads now go up to 10 (was 5).
- Downloads panel: the panel is as tall as the step it shows and grows or shrinks with a spring when the step changes (no big empty space under the shorter steps).
- Downloads: the actions, the delete question and the confirmation are now one panel with steps. Going forward, the current step slides out to the right and the next one arrives from the same side with a light spring; a small back button at the top left (or the system back) returns to the previous step the same way.
- No more windows in the middle of the screen: every question and notice (delete, cancel all, update notice choices, licenses, selection menu) now rises from the bottom like the other panels.
- Files that were deleted outside the app: the delete-from-device option and its confirmation no longer appear for them; they are checked again right when the panel or the delete flow opens, and only "Remove from list" is offered.
- Audio tags: when the site gives no album, the album tag is the song title (before, players grouped the file under the folder name "Audio"). A real album from the source is still used.
- About tagline and terms no longer say the app is only for own, freely licensed or permitted content; they say the user chooses what to download.
- Portuguese wording "Pronto" is now "Concluído".
- Settings and About forget their scroll position when left for good (back to Home); they keep it only while another page opened from them is on top.
- Removed the "Open" and "Delete file" buttons from download rows.
- The app now draws edge to edge: pages go behind the status and navigation bars (no solid bar), and each page keeps its content clear of them.
- Deleting a file from the device: the app first deletes what it can by itself (files this install created); for the rest the system asks. When it still fails, the message says what the system answered.
- README no longer says the app is only for your own, freely licensed or permitted content: you choose what you download.
- Home: the recent downloads fill the screen down to the bottom, so each device shows as many as fit (at least 3).
- Settings: the "System" section became "Updates" with two rows, "Check for updates" (shows the installed version) and "Update notices" (opens a dialog: notify normally, not for a week, not for a month).
- Downloads: tabs All (default), Audio and Video at the top of the list.
- Home: removed the tip text; the link field has a paste button on the right that becomes a clear (X) button as soon as there is text.
- New Material 3 Expressive look (large rounded shapes, heavier titles, spring page transitions) and no fixed top bar: page titles scroll with the content, Home and Downloads switch from a bottom navigation bar, and a floating Settings button sits at the top right (a floating Back button inside Settings and About).
- No bottom bar anymore: Home is a single page with the link field, the Download button and a short list of recent downloads ("See all" opens the Downloads page). Settings, Downloads and About open from Home, each with a floating Back button at the top left; page titles sit beside it. The floating buttons are slightly see-through.
- Predictive back (Settings > Appearance, on by default): in pages opened from Home, the page follows your finger with a shadow and rounded corners while the page underneath moves in with a dark veil; releasing finishes the slide, cancelling returns the page. The Back button plays the same exit.
- Every page now remembers where you left it (scroll position and typed text) when you go to another page and come back.
- The About screen moved inside Settings.
- Deleting a finished download no longer just hides it: see the new delete window under Added.
- The download pop-up first shows two audio entries and two video entries (the highest quality and the smallest file); everything else is under "More formats". The Download button stays visible and the list scrolls.
- File names now carry the quality, for example `Title (M4A_128K).m4a` and `Title (MP4_720p).mp4`.
- Downloads no longer use Android's system download manager (it stayed stuck on "Waiting"): the app downloads by itself.
- Minimum Android version is now 10 (API 29).
### Fixed
- Deleting several files at once on Android 10 now goes on with the next file after you allow the first one (the system asks one at a time), and a list that mixes files from the default folder and a picked folder no longer fails as a whole.
- On Android 15, when the system's time limit for background downloading is reached, the service stops cleanly and the unfinished downloads stay in the list as interrupted, ready to retry.
- Leftovers of a download interrupted by the app being killed are cleaned at the next start (temporary work folders and half-written files in a picked folder).
- Opening a download no longer touches the file on the main thread, and the About links no longer crash on a phone without a browser.
- Changing the download folder (or going back to the default) no longer makes earlier downloads show as missing: access to folders used before is kept, and entries marked missing are checked again, so they come back when the file is reachable.
- Picking a folder keeps the previous choice if Android refuses the new one (a message says so).
- When Android takes back access to the picked folder, Settings says so (downloads go to Download/Intruphon meanwhile) instead of silently resetting.
- Tapping outside the link field (or opening another page) takes focus off it, so the cursor stops blinking.
- Every page keeps its scroll and fields after opening another page and coming back (Settings → About → back): all stacked pages now stay composed under a stable key.
- Thumbnails no longer flash the placeholder when a page is revealed: rows start from the already-read preview.
- The floating Back and Settings buttons now belong to their page and slide with it (also during the predictive back), instead of staying over the whole screen.
- Tags failed with "Unsupported MP4 box size" on files made by the system muxer, which writes the media data box with a 64-bit size; the tag writer now reads that form.
- Joining video and audio now fails with a clear message when one of the two files gives no samples (before, a video could be saved without its sound and the app said nothing). The message lists, per file, how many samples were read and what the first read returned.
- M4A audio is no longer re-joined: the downloaded file already plays, so tags and cover are written straight into it (the re-join step was the cause of the empty files and added nothing).
- Every inspection build now has its own version code (the CI run number) so it always installs as an update, and the About tab and the notes under a download say which build is running.
- Joining audio (and video+audio) wrongly treated a negative first timestamp as the end of the file, so it found no samples and fell back to the downloaded file. Times are now shifted to start at zero and the end is detected by the read itself.
- An M4A that came out of the system muxer as a tiny, empty file (0:00, would not play): the joined file is now checked, and if it is too small the downloaded file itself is saved so it always plays. The reason is shown in the item.
- Tags can also be written into the downloaded M4A as it comes from the site (fragmented MP4), not only into a re-joined file.
- Tags and cover art were not written: files made by the system muxer keep their index before the media data, which the tag writer did not handle (it now shifts the offsets).
- Opening a downloaded file that was deleted outside the app no longer crashes; the entry is marked "This file is no longer on the device" and can be removed or downloaded again.
### Known issues
- The new look, navigation, short format list, About screen and update notice are only compiled by the CI so far; they have not been tested on a real device.
- Not tested on a real device yet: downloads from sites other than YouTube, and video resolutions other than 1080p. YouTube changes often and some links may fail to read or download.
- MP3 conversion runs on the phone, so a very long track may take a while.
