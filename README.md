# Intruphon

A lightweight, tracker-free Android app for downloading media from links. You choose what you download.

## Features

- Share a link from any app to Intruphon: a pop-up sheet appears over that app with the available audio and video formats.
- Or paste a link on the home screen and press Download.
- Playlist links (for example a YouTube or YouTube Music playlist) open a list with thumbnails: select all or only some items, pick a format (audio highest, MP3, audio smallest, video highest or smallest) and every item is queued in order.
- Files are saved in `Download/Intruphon/Audio` or `Download/Intruphon/Video` (or in a folder you pick in Settings), downloaded by the app itself (no storage permission needed), with a progress notification.
- Audio as M4A at each bitrate the source has, or as MP3 (64 to 320 kbps, never above the source's bitrate); video at each resolution the source has. Higher resolutions are built on the device by joining the separate video and audio streams.
- Audio files get title, artist, album, year and cover art; video files get title, artist and year.
- The Home page shows your latest downloads, and a Downloads page lists them all with progress and status, and shows each file's cover and length, and lets you open, cancel, delete or retry each download.
- Settings: download only on Wi-Fi, and how many downloads run at the same time.
- A local speed log (format, quality, connection type, bytes, speed) that stays on the device and can be shared as CSV from Settings.
- File names carry the quality, for example `Title (M4A_128K).m4a`.
- Available in English, Spanish and Portuguese (Brazil), following the Android language; a button in Settings opens Android's per-app language screen.
- Material 3 Expressive interface with dynamic colors on Android 12+: no fixed top bar and no bottom bar, a floating Settings button, predictive back, and every page remembers where you left it.
- The download pop-up shows the highest and the smallest audio and video choices first, with the rest under "More formats".
- An About screen (inside Settings) with version, source code, changelog, issues, privacy policy, terms and licenses.
- An update notice: the app asks GitHub for the latest public Release when it opens and tells you if there is a newer one (you can remind later, skip a version, or check by hand in Settings).

Some links cannot be read (unsupported, private or protected), and sites change often, so a link that works today may fail tomorrow.

MP3 encoding uses a bundled Java port of the LAME encoder ([java-lame](https://github.com/nwaldispuehl/java-lame), LGPL; see `third_party/java-lame`).

## Privacy and permissions

Intruphon declares only the `INTERNET`, network-state (`ACCESS_NETWORK_STATE`, for the Wi-Fi only option), notification (`POST_NOTIFICATIONS`) and foreground-service permissions (the last two for download progress), includes no analytics, ads or third-party SDKs, and sends nothing to the maintainer. To read a link it contacts the website that hosts it (and that site's own content servers), to look for updates it asks GitHub for the project's latest public Release, and the About screen loads the maintainer's public GitHub profile picture. This is checked against the code at every release and the policy is updated with any change. See the [privacy policy](https://rilclesaraiva.github.io/Intruphon/privacy).

## 🤖 Built with AI ("vibe coding")

In the spirit of transparency: **Intruphon is developed with heavy use of AI.**
Most of the code is written by [Claude](https://claude.ai) (Anthropic) under the
direction of the human maintainer, a workflow often called *vibe coding*.
We take care that it doesn't turn into AI slop:

- **The maintainer decides, the AI executes.** Every feature starts from a real
  need, and every build is tested on real devices before the next round.
- **Written rules the AI has to follow.** [`CLAUDE.md`](CLAUDE.md) sets the
  architecture, privacy limits and process, and is updated with every decision.
- **Tests and automated builds.** Every change goes through GitHub Actions,
  and a failing test means no build.
- **No release without approval.** Nothing is published without the
  maintainer's explicit approval.
- **Honest changelogs.** [`CHANGELOG.md`](CHANGELOG.md) says what changed,
  including what is still broken.

If AI-assisted code is a concern for you, now you know, and you are welcome to
read every line. Found something careless? Please [open an issue](../../issues).

## Installation

Download the latest APK from the [Releases](../../releases) page and install it. Android 10 (API 29) or newer is required.

## License

GPL-3.0. See [`LICENSE`](LICENSE).

## Legal

[Privacy policy](https://rilclesaraiva.github.io/Intruphon/privacy) · [Terms](https://rilclesaraiva.github.io/Intruphon/terms)

## ☕ Support the project

Intruphon is free and maintained independently. If it's useful to you, you can
support its development with a coffee:

[![Buy Me a Coffee](https://img.shields.io/badge/Buy%20Me%20a%20Coffee-support-FFDD00?logo=buymeacoffee&logoColor=black)](https://buymeacoffee.com/saraivarilcle)

**Contact:** [saraivarilcle@gmail.com](mailto:saraivarilcle@gmail.com)
