# Licenses and attributions

**Last updated:** 2026-11-20 · **Responsible:** rilclesaraiva · **Contact:** saraivarilcle@gmail.com

## Intruphon

Intruphon is free software under the **GNU General Public License v3.0 (GPL-3.0)**. The full text is in the [`LICENSE`](https://github.com/rilclesaraiva/Intruphon/blob/main/LICENSE) file of the repository, and the source code is at <https://github.com/rilclesaraiva/Intruphon>.

## Libraries the app uses

| Library | What it is used for | License |
|---|---|---|
| [NewPipeExtractor](https://github.com/TeamNewPipe/NewPipeExtractor) | Reading the formats of a link | GPL-3.0 |
| [OkHttp](https://square.github.io/okhttp/) | HTTP client (downloads and the extractor's requests) | Apache-2.0 |
| [Kotlin coroutines](https://github.com/Kotlin/kotlinx.coroutines) | Work off the main thread | Apache-2.0 |
| AndroidX and Jetpack Compose | Screens and Android integration | Apache-2.0 |
| [Mozilla Rhino](https://github.com/mozilla/rhino) (brought by the extractor) | Running small scripts some sites need | MPL-2.0 |
| [java-lame](third_party) | MP3 encoder, a Java port of LAME, copied into the source (`app/src/main/java/net/sourceforge/lame`) | LGPL (license in [`third_party/java-lame`](https://github.com/rilclesaraiva/Intruphon/tree/main/third_party/java-lame)) |
| Android `desugar_jdk_libs` | Lets the extractor run on Android below 13 | See [its repository](https://github.com/google/desugar_jdk_libs) for the license |

The extractor brings libraries of its own, each under its own license. Unit tests also use JUnit and `org.json`, which are not part of the app you install.

## Services

The app talks only to the website of the link you give it (and that site's content servers), and to GitHub for the update check and the developer's profile picture. Those services have their own terms; see the [Privacy Policy](privacy) for exactly what is sent.
