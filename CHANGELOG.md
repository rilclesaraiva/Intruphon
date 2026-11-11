# Changelog

## [Unreleased]
### Added
- Initial project structure: Compose app shell, home screen and About screen.
- Original adaptive launcher icon (with monochrome layer).
- CI build with R8 (no obfuscation) and signed release workflow.
- Privacy policy and terms (`docs/`).
- Share target: Intruphon appears in the Android share sheet and opens a pop-up sheet over the app that shared the link (no full-screen switch).
- Real downloads: the app reads the link with NewPipeExtractor, lists the available audio and video formats and downloads the chosen one with Android's download manager (saved in Music/Intruphon or Movies/Intruphon).
- The Download button on the home screen opens the same format sheet.
- INTERNET permission (needed to read links and download).
### Changed
- Minimum Android version is now 10 (API 29).
### Known issues
- Only formats that already include audio are offered for video (usually 360p); higher resolutions need a muxing step that is not implemented yet.
- No MP3 conversion yet: audio is saved as M4A or WebM.
- Not tested on a real device yet: YouTube changes often and some links may fail to read or download.
