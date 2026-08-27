#!/usr/bin/env bash
# =============================================================================
# Anisora — NATIVE demo APK build (no Gradle / no Android Studio required)
#
# Fully native replica of the web UI: real Android views, custom-drawn icons,
# bundled Inter / Space Grotesk / JetBrains Mono fonts, AniList GraphQL client.
#
# Toolchain: aapt2, android.jar (API 21), ecj, dx, apksigner + a JRE
# (fetch with android/tools/fetch-tools.sh; override with TOOLS_DIR / JAVA).
# =============================================================================
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
AND="$ROOT/android-native"
TOOLS_DIR="${TOOLS_DIR:-$HOME/tools}"
JAVA="${JAVA:-java}"
OUT="$AND/out"

AAPT2="$TOOLS_DIR/aapt2"
ANDROID_JAR="$TOOLS_DIR/android.jar"
ECJ="$TOOLS_DIR/ecj.jar"
DX="$TOOLS_DIR/dx.jar"
APKSIGNER="$TOOLS_DIR/apksigner.jar"

echo "==> [1/6] compiling resources (aapt2)"
rm -rf "$OUT" && mkdir -p "$OUT/flat" "$OUT/classes"
find "$AND/res" -type f | while read -r f; do
  "$AAPT2" compile -o "$OUT/flat" "$f"
done
"$AAPT2" link \
  -o "$OUT/base.apk" \
  --manifest "$AND/AndroidManifest.xml" \
  -I "$ANDROID_JAR" \
  -A "$AND/assets" \
  "$OUT"/flat/*.flat

echo "==> [2/6] compiling java (ecj)"
"$JAVA" -jar "$ECJ" -source 1.5 -target 1.5 -nowarn \
  -cp "$ANDROID_JAR" -d "$OUT/classes" \
  "$AND"/src/app/anisora/*.java

echo "==> [3/6] dexing (dx)"
"$JAVA" -jar "$DX" --dex --output="$OUT/classes.dex" "$OUT/classes"

echo "==> [4/6] packaging + aligning"
python3 "$ROOT/android/tools/pack.py" "$OUT/base.apk" "$OUT/classes.dex" "$OUT/unsigned.apk"

echo "==> [5/6] signing (apksigner, shared debug key)"
KS="$ROOT/android/debug.keystore"
if [ ! -f "$KS" ]; then
  "${KEYTOOL:-keytool}" -genkeypair -keystore "$KS" -alias anisora \
    -keyalg RSA -keysize 2048 -validity 10000 \
    -storepass anisora-demo -keypass anisora-demo \
    -dname "CN=Anisora Demo, OU=Dev, O=Anisora" >/dev/null 2>&1
fi
"$JAVA" -jar "$APKSIGNER" sign \
  --ks "$KS" --ks-key-alias anisora \
  --ks-pass pass:anisora-demo --key-pass pass:anisora-demo \
  --out "$ROOT/anisora-demo.apk" "$OUT/unsigned.apk"

echo "==> [6/6] verifying"
"$JAVA" -jar "$APKSIGNER" verify --verbose "$ROOT/anisora-demo.apk" | head -5
echo
echo "OK -> $ROOT/anisora-demo.apk"
