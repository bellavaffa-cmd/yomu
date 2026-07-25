# Writing a Yomu extension

A Yomu extension is an ordinary Android APK that declares one manifest metadata entry
and ships one or more classes implementing Yomu's source API. At runtime Yomu discovers
the APK, loads its classes with a `PathClassLoader` (parented to the host), and registers
its sources — so your extension reuses the host's OkHttp stack, JSoup, and coroutines
without bundling any of them.

> **Please only write sources for content you have the right to distribute** — your own
> comics, public-domain works, or sites whose terms permit it. The bundled `xkcd`
> extension is a working example (freely readable, CC BY-NC).

## 1. Module setup

Your extension depends on `:extension-api` **`compileOnly`** — the host provides those
classes (and okhttp/jsoup/coroutines) at runtime, so they must not be packaged:

```gradle
plugins {
    id 'com.android.application'
    id 'org.jetbrains.kotlin.android'
}
android {
    namespace 'com.example.myext'
    compileSdk 34
    defaultConfig {
        applicationId 'com.example.myext'
        minSdk 26; targetSdk 34
        versionCode 1; versionName '1.0.0'
    }
    compileOptions { sourceCompatibility JavaVersion.VERSION_17; targetCompatibility JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = '17' }
}
dependencies {
    compileOnly project(':extension-api')          // host-provided ABI
    compileOnly 'com.squareup.okhttp3:okhttp:4.12.0'
    // org.json is in the Android framework — free at runtime
}
```

## 2. Manifest metadata

The value is the factory/source class, relative to your package (leading dot):

```xml
<application android:label="My Extension" android:hasCode="true">
    <meta-data android:name="com.yomu.reader.extension.class" android:value=".MyFactory" />
</application>
```

## 3. Implement a source

Implement `SourceFactory` returning your `Source`(s). The easiest base class is
`HttpSource` (supply *requests* + *parsers*); for HTML scraping use `ParsedHttpSource`
(supply CSS selectors + element mappers). Full working reference: `extension-xkcd`.

```kotlin
class MyFactory : SourceFactory {
    override fun createSources() = listOf(MySource())
}

class MySource : HttpSource() {
    override val name = "My Source"
    override val baseUrl = "https://example.com"
    override val lang = "en"
    override val supportsLatest = false

    override fun popularMangaRequest(page: Int) = GET("$baseUrl/popular?page=$page")
    override fun popularMangaParse(response: Response): MangasPage { /* build MangasPage */ }
    override fun searchMangaRequest(page: Int, query: String, filters: FilterList) =
        GET("$baseUrl/search?q=$query&page=$page")
    override fun searchMangaParse(response: Response) = popularMangaParse(response)
    override fun mangaDetailsParse(response: Response): SManga { /* ... */ }
    override fun chapterListParse(response: Response): List<SChapter> { /* ... */ }
    override fun pageListParse(response: Response): List<Page> { /* ... */ }
}
```

`SManga.url` / `SChapter.url` are opaque strings *you* choose — they are passed back to
your `*Request` builders (the default builders prepend `baseUrl`).

## 4. Build

```bash
./gradlew :extension-myext:assembleDebug
```

Install the APK on a device that also has Yomu, then Yomu → **Browse → Extensions →
Refresh**. Your source appears under **Installed** and in **Browse**.

## 5. Distribute via a repo (optional)

Yomu can list & download extensions from a static repo — the same shape Tachiyomi/Mihon
use. Host a folder with:

```
repo/
  index.min.json          # array of extension entries (see repo/index.min.json)
  apk/<pkg>-vX.Y.Z.apk     # the APKs referenced by each entry's "apk"
  icon/<pkg>.png           # optional launcher-style icon
```

Each `index.min.json` entry: `{ name, pkg, apk, lang, code, version, nsfw, sources[] }`.
The app resolves `apk` as `<repoBaseUrl>/apk/<apk>` and icons as
`<repoBaseUrl>/icon/<pkg>.png`. Push the folder to any static host (GitHub raw, Pages, …)
and add its base URL under **Extensions → Manage repos**. The included `repo/` folder is a
ready-to-host example containing the Demo and xkcd extensions.
