# Intruphon

A lightweight, tracker-free Android app for downloading media from links, meant for your own content, freely licensed content, or content you have permission to download.

## Features

- Share a link from any app to Intruphon: a pop-up sheet appears over that app with the available audio and video formats.
- Or paste a link on the home screen and press Download.
- Files are saved in `Download/Intruphon/Audio` or `Download/Intruphon/Video`, downloaded by the app itself (no storage permission needed), with a progress notification.
- Audio as M4A at each bitrate the source has, or as MP3 (64 to 320 kbps, never above the source's bitrate); video at each resolution the source has. Higher resolutions are built on the device by joining the separate video and audio streams.
- Audio files get title, artist, album, year and cover art; video files get title, artist and year.
- A Downloads tab shows progress and status, and shows each file's cover and length, and lets you open, cancel, delete or retry each download.
- Material 3 interface with dynamic colors on Android 12+.

Intruphon is meant for content that is yours, freely licensed, or that you have permission to download. Some links cannot be read (unsupported, private or protected), and sites change often, so a link that works today may fail tomorrow.

MP3 encoding uses a bundled Java port of the LAME encoder ([java-lame](https://github.com/nwaldispuehl/java-lame), LGPL; see `third_party/java-lame`).

## Privacy and permissions

Intruphon declares only the `INTERNET`, notification (`POST_NOTIFICATIONS`) and foreground-service permissions (the last two for download progress), includes no analytics, ads or third-party SDKs, and sends nothing to the maintainer. To read a link it contacts the website that hosts it (and that site's own content servers). This is checked against the code at every release and the policy is updated with any change. See the [privacy policy](https://rilclesaraiva.github.io/Intruphon/privacy).

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
