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
### Changed
- Downloads no longer use Android's system download manager (it stayed stuck on "Waiting"): the app downloads by itself.
- Minimum Android version is now 10 (API 29).
### Known issues
- Only formats that already include audio are offered for video (usually 360p); higher resolutions need a muxing step that is not implemented yet.
- No MP3 conversion yet: audio is saved as M4A or WebM.
- Not tested on a real device yet: YouTube changes often and some links may fail to read or download.
