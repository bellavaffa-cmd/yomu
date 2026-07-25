# Yomu

A native Android manga/comic reader in the spirit of [Mihon](https://mihon.app)/Tachiyomi —
Kotlin + Jetpack Compose, with a pluggable **extension system** built around a clean,
documented SDK.

> ⚠️ **Early WIP** (v0.4.0, debug builds only). Yomu ships **no** piracy sources. It
> includes a MangaDex built-in (public API) plus two example extensions built on the SDK:
> a placeholder-image **Demo** source and an **xkcd** source (freely readable, CC BY-NC).
> Please only write/use extensions for content you have the right to read.

## Features

- **Library** with Room-backed persistence, favourites, reading history
- **Browse** built-in and extension sources — Popular / Latest / Search with infinite scroll
- **Reader** — paged, swipeable, pinch-to-zoom, progress + history tracking
- **Extensions** — discover, download and install extension APKs from user-added repos, and
  a runtime **loader** that turns installed extension APKs into live sources
- **Extension SDK** — `HttpSource` / `ParsedHttpSource` base classes so a new source is a
  few request/parse methods (see [docs/WRITING_EXTENSIONS.md](docs/WRITING_EXTENSIONS.md))

## Modules

| Module | What it is |
| --- | --- |
| `:app` | The Android app — UI, Room DB, source manager, extension loader |
| `:extension-api` | The shared **ABI/SDK** extensions compile against (`compileOnly`) — `Source`, `CatalogueSource`, `SourceFactory`, `HttpSource`, `ParsedHttpSource`, models |
| `:extension-sample` | A demo extension (placeholder images) proving the loader end-to-end |
| `:extension-xkcd` | An xkcd extension built on `HttpSource` (real network, JSON API) |

## Architecture

- No DI framework — plain `by lazy` singletons on `YomuApplication`.
- The DB is the single source of truth; a repository bridges remote sources ↔ Room cache.
- **Extension loading**: any installed APK declaring the `com.yomu.reader.extension.class`
  manifest metadata is loaded via a `PathClassLoader` parented to the host classloader, so
  the extension's references to Yomu's ABI resolve to the host's implementations
  (the Tachiyomi/Mihon model). The host injects its OkHttp/JSoup stack, so extensions stay
  tiny and consistent.

## Build

Requires JDK 17 (Android Studio's bundled JBR works) and the Android SDK.

```bash
./gradlew :app:assembleDebug
# extensions:
./gradlew :extension-sample:assembleDebug :extension-xkcd:assembleDebug
```

Install the app APK plus any extension APKs, then open **Browse → Extensions → Refresh**.

## Distributing extensions

`repo/` is a ready-to-host extension repo (`index.min.json` + APKs) — push it to any static
host and add its URL under **Extensions → Manage repos**. See
[docs/WRITING_EXTENSIONS.md](docs/WRITING_EXTENSIONS.md).

## Not included / roadmap

Yomu deliberately does **not** implement Tachiyomi ABI compatibility (that only serves the
piracy-extension ecosystem). Planned instead: more legal built-in sources, a webtoon
(continuous vertical) reader, downloads/offline, categories, trackers.

## License

No license yet — all rights reserved for now.
