# Privacy Policy

**Last updated:** 2026-11-12 · **Responsible:** rilclesaraiva · **Contact:** saraivarilcle@gmail.com

This policy describes what Intruphon does, based on its source code.

- **Data collected by the maintainer:** none. The app has no account, no analytics, no advertising and no third-party SDKs, and sends nothing to the maintainer.
- **Permissions:** the app declares `INTERNET` (to read links and download), `ACCESS_NETWORK_STATE` (to know whether the phone is on Wi-Fi, for the "Wi-Fi only" option and the speed log), `POST_NOTIFICATIONS` (to show download progress; Android 13+ asks you, and downloads still work if you refuse) and `FOREGROUND_SERVICE` with `FOREGROUND_SERVICE_DATA_SYNC` (to keep a download running while the screen is off). It does not ask for access to your files, contacts, location, camera or microphone.
- **Network:** when you press Download or share a link to the app, it sends requests to the website that hosts that link (for example, a video site) and to that site's content servers, to read the available formats (and, for a playlist, its list of items and their small thumbnails) and to download the file you choose. Those sites see your IP address and the usual technical data of a web request, and their own policies apply. To put cover art in audio files, the app also downloads the link's thumbnail image from the site's image servers. The "Open repository" and other links in the About screen open the project's pages in your browser.
- **Update check:** when the app opens (and at most every 10 minutes when you come back to it), it asks GitHub (`api.github.com`) for the latest public Release of the project, to tell you if a newer version exists. GitHub sees your IP address and the usual technical data of a web request, and its own policy applies. The app sends no identifier of you or your device. It keeps the last answer, its ETag and your choices ("remind me later", "skip this version") in its private storage on the device. "Check for updates" in Settings asks again on demand. The download button of the update screen opens the Release file in your browser; Android installs it only if you allow it.
- **Profile picture:** when the About screen opens, the app downloads the maintainer's public GitHub profile picture (from `github.com`, which forwards to GitHub's image servers) and keeps it in the app's cache for up to a week. GitHub sees your IP address and the usual technical data of a web request. If the picture cannot be loaded, an icon is shown instead.
- **Downloads:** the chosen file is downloaded by the app itself and saved in `Download/Intruphon/Audio` or `Download/Intruphon/Video` on your device. It stays there until you delete it.
- **Downloads list:** to show the Downloads tab, the app keeps in its private storage a list of the downloads you started (title, link, format, status and the location of the saved file). It stays only on your device; deleting or removing an entry in the app, or uninstalling the app, erases it.
- **Local storage:** the link you type, or the link another app shares with Intruphon through the Android share sheet, is shown in the app and kept only in its temporary screen state (so it survives screen rotation). It is not saved to storage by the app.
- **Sharing, retention and deletion:** the app does not share data with anyone else. Uninstalling the app removes what it stored; downloaded files remain in your Downloads folder until you delete them.

**Speed log:** for each download the app writes a line on the device with the format, quality, connection type (Wi-Fi, cellular…), bytes, time and result, and also to the system log (logcat) under the tag `IntruphonSpeed`. It contains no link, title or personal data, is never sent anywhere by the app, and is only shared if you choose "Share log" in Settings. "Clear log" deletes it.

When a version adds a permission, a network call or a library that collects data, this policy and its date are updated in the same release.

This text is a starting point generated with AI assistance, not legal advice. It is written with Brazilian LGPD and the EU GDPR in mind.

Questions or requests: saraivarilcle@gmail.com
