# Anisora
A simple anilist tracker

The reference UI lives in `src/` (React + Vite + Tailwind). The Android app in `android-native/` is a **fully native replica** of that UI — no WebView.

## Android app (`android-app/` Gradle + `android-native/` UI)

v0.8 starts the **Gradle migration** and a real **Aniyomi extension runtime**: installed extension APKs are class-loaded (system packages + `filesDir/extensions/*.apk`), searched, and used to extract episode/chapter lists and video/page URLs. Playback uses **Media3 ExoPlayer** (HLS/MP4 with source headers). Manga pages load in the reader.

Watch / Read tab (both anime and manga):

- **Continue** pill — next episode/chapter from your AniList progress, one tap to resume
- **Range chips** (1–50, 51–100, …) — only one slice of the list is bound, so long series don’t lag
- **Wrong title** — popup search on the selected extension; pick the matching entry and Anisora binds it
- **Source settings** — if the extension implements Aniyomi’s `ConfigurableAnimeSource` / `ConfigurableSource`, a gear opens `setupPreferenceScreen` (quality, domain, servers, etc.), stored in `source_{id}` SharedPreferences exactly like Aniyomi

Build with Gradle (JDK 17 + Android SDK):

```bash
cd android-app
echo "sdk.dir=$ANDROID_HOME" > local.properties   # or copy local.properties.example
# first time: gradle wrapper --gradle-version 8.4
./gradlew assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk
```

The older no-SDK pipeline (`android-native/build-apk.sh`) still packs the Java UI as a demo, but **does not include the Kotlin runtime / ExoPlayer** — use Gradle for actual playback.

Native Android app (`android-native/`), pixel-styled after the web UI, with **real AniList integration** (OAuth client id 49241, redirect `anisora://anilist-auth`): sign in from Onboarding or Settings → Account and your actual AniList library syncs in; status/progress/score changes and removals push back to anilist.co (`SaveMediaListEntry` / `DeleteMediaListEntry`). Guests keep a local-only library.

Screens:

- **Onboarding** — hero art, gradient headline, mock AniList OAuth flow, guest mode
- **Home (Anime / Manga)** — greeting header, tracked library grid with status filters, Trending / Seasonal / Top / Community rails from the live AniList GraphQL API, loading skeletons, offline state
- **Search** — debounced live AniList search with All/Anime/Manga tabs
- **Detail** — banner + cover hero, score/popularity pills, airing countdown, status picker sheet, +1 progress logging, **0-100 rating sheet** (format-aware display), synopsis, info table, genres, **characters and staff rails** (tap any card for the full character/staff page), relations & recommendations, **demo player** with play/pause, seek, skip-intro and auto progress (Watch tab)
- **Settings** — sections (Appearance / Content / Extensions / Playback / Sync / Account): dark/AMOLED/light themes, 8 accent colors, density, poster radius, title language, toggles, extension switches, logout
- Design system matches `src/index.css`: bg `#0a0c11/#10141c/#181e29`, accent `rgb(61 180 242)`, bundled **Space Grotesk / Inter / JetBrains Mono**, hand-drawn lucide-style icons, rounded 2:3 posters, chips, toasts
- Tracked library persists on-device (SharedPreferences), same seed data as the web store
- package `app.anisora` · minSdk 21 (Android 5.0+) · targetSdk 34 · signed v1+v2+v3

### Building it

Built without Gradle or the Android SDK installer (works even where `dl.google.com` / `maven.google.com` are blocked):

```bash
# 1) fetch the toolchain: aapt2, android.jar, ecj, dx, apksigner, JRE
bash android/tools/fetch-tools.sh

# 2) build + sign the native app
JAVA="$(python3 -c 'from jdk4py import JAVA_HOME; print(JAVA_HOME)')/bin/java" \
KEYTOOL="$(python3 -c 'from jdk4py import JAVA_HOME; print(JAVA_HOME)')/bin/keytool" \
bash android-native/build-apk.sh
```

Pipeline: `aapt2 compile/link` (manifest, icons, fonts, art) → `ecj` (Java sources in `android-native/src/`) → `dx` (classes.dex) → `android/tools/pack.py` (zip + alignment) → `apksigner`.

### Installing

Copy `anisora-demo.apk` to a device and open it (allow "install from unknown sources"), or:

```bash
adb install -r anisora-demo.apk
```

## Web UI (reference design)

```bash
npm install
npm run dev      # local dev server
npm run build    # single-file build -> dist/index.html
```

`android/` additionally contains the earlier WebView-based shell (`bash android/build-apk.sh` → `anisora-webview.apk`), kept for reference.
