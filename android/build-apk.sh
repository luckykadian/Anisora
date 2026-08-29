#!/usr/bin/env bash
# =============================================================================
# Anisora — demo APK build (no Gradle / no Android Studio required)
#
# Wraps the untouched Anisora web UI (single-file Vite build) in a minimal
# native WebView shell and produces a signed, installable debug APK.
#
# Toolchain (see android/tools/): aapt2, android.jar (API 21 platform stubs),
# ecj (Eclipse Java compiler), dx (dexer), apksigner + a JRE.
# Override tool locations with env vars: TOOLS_DIR, JAVA.
# =============================================================================
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
AND="$ROOT/android"
TOOLS_DIR="${TOOLS_DIR:-$HOME/tools}"
JAVA="${JAVA:-java}"
OUT="$AND/out"

AAPT2="$TOOLS_DIR/aapt2"
ANDROID_JAR="$TOOLS_DIR/android.jar"
ECJ="$TOOLS_DIR/ecj.jar"
DX="$TOOLS_DIR/dx.jar"
APKSIGNER="$TOOLS_DIR/apksigner.jar"

echo "==> [1/7] building web UI (vite)"
cd "$ROOT"
npm run --silent build

echo "==> [2/7] staging assets"
rm -rf "$OUT" && mkdir -p "$OUT/assets/www" "$OUT/classes"
cp "$ROOT/dist/index.html" "$OUT/assets/www/index.html"

echo "==> [3/7] compiling resources (aapt2)"
mkdir -p "$OUT/flat"
find "$AND/res" -type f | while read -r f; do
  "$AAPT2" compile -o "$OUT/flat" "$f"
done
"$AAPT2" link \
  -o "$OUT/base.apk" \
  --manifest "$AND/AndroidManifest.xml" \
  -I "$ANDROID_JAR" \
  -A "$OUT/assets" \
  "$OUT"/flat/*.flat

echo "==> [4/7] compiling java (ecj)"
"$JAVA" -jar "$ECJ" -source 1.5 -target 1.5 -nowarn \
  -cp "$ANDROID_JAR" -d "$OUT/classes" \
  "$AND/src/app/anisora/MainActivity.java"

echo "==> [5/7] dexing (dx)"
"$JAVA" -jar "$DX" --dex --output="$OUT/classes.dex" "$OUT/classes"

echo "==> [6/7] packaging + aligning"
python3 "$AND/tools/pack.py" "$OUT/base.apk" "$OUT/classes.dex" "$OUT/unsigned.apk"

echo "==> [7/7] signing (apksigner, debug key)"
KS="$AND/debug.keystore"
if [ ! -f "$KS" ]; then
  "${KEYTOOL:-keytool}" -genkeypair -keystore "$KS" -alias anisora \
    -keyalg RSA -keysize 2048 -validity 10000 \
    -storepass anisora-demo -keypass anisora-demo \
    -dname "CN=Anisora Demo, OU=Dev, O=Anisora" >/dev/null 2>&1
fi
"$JAVA" -jar "$APKSIGNER" sign \
  --ks "$KS" --ks-key-alias anisora \
  --ks-pass pass:anisora-demo --key-pass pass:anisora-demo \
  --out "$ROOT/anisora-webview.apk" "$OUT/unsigned.apk"

"$JAVA" -jar "$APKSIGNER" verify --verbose "$ROOT/anisora-webview.apk" | head -5
echo
echo "OK -> $ROOT/anisora-webview.apk"
