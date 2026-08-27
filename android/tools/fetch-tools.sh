#!/usr/bin/env bash
# =============================================================================
# Fetches the Gradle-free Android toolchain used by android/build-apk.sh.
#
# Everything is pulled from GitHub (via the authenticated `gh` CLI) and PyPI,
# so it works even where dl.google.com / maven.google.com are unreachable.
#
#   aapt2         iBotPeaches/Apktool prebuilt (linux x86_64)
#   android.jar   hekai/android-sdk-linux, platforms/android-21 (API 21 stubs)
#   dx.jar        hekai/android-sdk-linux, build-tools/21.0.1
#   ecj.jar       chriskmanx/qmole-packages (Eclipse Java Compiler 3.2)
#   apksigner.jar warren-bank/print-apk-signature (build-tools 29.0.2)
#   JRE + keytool jdk4py (PyPI)
#
# Usage: TOOLS_DIR=~/tools bash android/tools/fetch-tools.sh
# =============================================================================
set -euo pipefail

TOOLS_DIR="${TOOLS_DIR:-$HOME/tools}"
mkdir -p "$TOOLS_DIR"
cd "$TOOLS_DIR"

blob() { # blob <owner/repo> <blob-sha> <out>
  gh api "repos/$1/git/blobs/$2" --jq '.content' | tr -d '\n' | base64 -d > "$3"
  echo "  $3 ($(stat -c%s "$3") bytes)"
}

echo "==> aapt2 (Apktool prebuilt, linux)"
gh api 'repos/iBotPeaches/Apktool/contents/brut.apktool/apktool-lib/src/main/resources/prebuilt/linux/aapt2' \
  -H 'Accept: application/vnd.github.raw' > aapt2
chmod +x aapt2

echo "==> android.jar / dx.jar (hekai/android-sdk-linux)"
blob hekai/android-sdk-linux 04cf3c1cb64c03bb1b8b8973c4ffcc746382b6cc android.jar
blob hekai/android-sdk-linux 6e7a61162ae2d2ea604218bf21556e5ef865f532 dx.jar

echo "==> ecj.jar (Eclipse Java Compiler 3.2)"
blob chriskmanx/qmole-packages 3dbefa45c557bb5f50df4238e15cdc14a0a972e8 ecj.jar

echo "==> apksigner.jar (build-tools 29.0.2)"
gh api 'repos/warren-bank/print-apk-signature/contents/libs/apksigner/apksigner.jar' \
  -H 'Accept: application/vnd.github.raw' > apksigner.jar

echo "==> JRE (jdk4py from PyPI, provides java + keytool)"
pip3 install --quiet --break-system-packages jdk4py || pip3 install --quiet jdk4py
python3 - <<'EOF'
from jdk4py import JAVA_HOME
print(f"  JAVA={JAVA_HOME}/bin/java")
EOF

echo "done. build with:"
echo "  JAVA=\$(python3 -c 'from jdk4py import JAVA_HOME; print(JAVA_HOME)')/bin/java \\"
echo "  KEYTOOL=\$(python3 -c 'from jdk4py import JAVA_HOME; print(JAVA_HOME)')/bin/keytool \\"
echo "  TOOLS_DIR=$TOOLS_DIR bash android/build-apk.sh"
