# Changelog

## [Unreleased]
### Added
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
- Settings tab: download only on Wi-Fi (adds the `ACCESS_NETWORK_STATE` permission) and the number of simultaneous downloads (1 to 5, the rest wait their turn).
- Speed log: every download records format, quality, connection type, bytes, download time, processing time and result in a file on the device and in logcat (tag `IntruphonSpeed`); Settings shows the average speed per format and quality and can share the log as CSV or clear it.
- Update notice: when the app opens (and at most every 10 minutes when you come back to it) it asks GitHub for the latest public Release. If it is newer than the installed version, a full-screen panel shows the Release notes with a Download button that opens the APK. "Not now" lasts until the app is opened again; the bell reminds in 1 week or 1 month, or skips that version. The saved ETag keeps repeated checks from using GitHub's rate limit. Settings has a System section to check by hand and to turn the notices back on. The privacy policy and README describe this new network call.
- Deleting a finished download asks whether to remove it from the list only or also delete the file from the device. Deleting from the device uses Android's own delete confirmation (no storage permission).
- About screen rebuilt in the style of rows with round icons and chips, as a page opened from Settings: icon, name, version, build number and build type, developer, source code, what's new (the changelog), report a problem, privacy policy, terms, licenses and attributions, and the invitation to the repository.
### Changed
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
