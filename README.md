# Anisora
A simple anilist tracker

The UI lives in `src/` (React + Vite + Tailwind, single-file build) and is used **unchanged** by every target below.

## Web (dev)

```bash
npm install
npm run dev      # local dev server
npm run build    # single-file build -> dist/index.html
```

## Android demo APK

`anisora-demo.apk` — a signed, installable debug APK that renders the exact same UI inside a minimal native WebView shell (no Capacitor/Cordova, no Gradle).

- package `app.anisora`, minSdk 21 (Android 5.0+), targetSdk 34
- the single-file web build ships in `assets/www/index.html` and is served from a virtual `https://` origin, so localStorage persistence and AniList GraphQL calls work exactly like in the browser
- external links open in the system browser; hardware back navigates the WebView
- signed with v1 + v2 + v3 schemes (debug key: `android/debug.keystore`)

### Building it

```bash
# 1) fetch the Gradle-free toolchain (aapt2, android.jar, ecj, dx, apksigner, JRE)
bash android/tools/fetch-tools.sh

# 2) build + sign
JAVA="$(python3 -c 'from jdk4py import JAVA_HOME; print(JAVA_HOME)')/bin/java" \
KEYTOOL="$(python3 -c 'from jdk4py import JAVA_HOME; print(JAVA_HOME)')/bin/keytool" \
bash android/build-apk.sh
```

Pipeline: `vite build` → `aapt2 compile/link` (manifest, icons, assets) → `ecj` (WebView shell, `android/src/`) → `dx` (classes.dex) → `android/tools/pack.py` (zip + 4-byte alignment) → `apksigner`.

### Installing

Copy `anisora-demo.apk` to a device and open it (allow "install from unknown sources"), or:

```bash
adb install anisora-demo.apk
```
