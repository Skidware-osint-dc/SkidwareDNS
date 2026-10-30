#!/usr/bin/env bash
# Build SkidwareDNS. Signing is optional:
#   SKID_KEYSTORE, SKID_STOREPASS, SKID_KEYPASS, SKID_KEY_ALIAS
# If those are missing, an unsigned APK is written instead.
# NEVER generate a keystore into the repo. NEVER hardcode passwords.
set -euo pipefail

PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"
SDK_ROOT="${ANDROID_SDK_ROOT:-/opt/android-sdk}"
BUILD_TOOLS="$SDK_ROOT/build-tools/34.0.0"
PLATFORM_JAR="$SDK_ROOT/platforms/android-34/android.jar"

# d8 8.2 NPEs on anonymous classes ($1.class) produced by JDK 21 javac -source 8.
# Compile with JDK 11; keep JAVA_HOME (21) on PATH for apksigner.
if [ -n "${JAVA_HOME_11_X64:-}" ] && [ -x "${JAVA_HOME_11_X64}/bin/javac" ]; then
  JAVAC_BIN="${JAVA_HOME_11_X64}/bin/javac"
elif [ -x /usr/lib/jvm/jdk-11/bin/javac ]; then
  JAVAC_BIN=/usr/lib/jvm/jdk-11/bin/javac
elif [ -n "${JAVA_HOME:-}" ] && [ -x "$JAVA_HOME/bin/javac" ]; then
  JAVAC_BIN="$JAVA_HOME/bin/javac"
else
  JAVAC_BIN="$(command -v javac)"
fi
export PATH="${JAVA_HOME:-}/bin:$BUILD_TOOLS:$PATH"
echo "javac: $JAVAC_BIN"

VERSION_CODE="${SKID_VERSION_CODE:-601}"
VERSION_NAME="${SKID_VERSION_NAME:-6.0.1-skid}"
OUT_APK="${SKID_OUT_APK:-$PROJECT_DIR/SkidwareDNS.apk}"
UNSIGNED_APK="${SKID_UNSIGNED_APK:-$PROJECT_DIR/SkidwareDNS-unsigned.apk}"
KEYSTORE="${SKID_KEYSTORE:-$PROJECT_DIR/skidwaredns-release.jks}"
KEY_ALIAS="${SKID_KEY_ALIAS:-skidkey}"
STOREPASS="${SKID_STOREPASS:-}"
KEYPASS="${SKID_KEYPASS:-$STOREPASS}"

WORK_DIR="${SKID_WORK_DIR:-/tmp/skidwaredns_build}"
rm -rf "$WORK_DIR"
mkdir -p "$WORK_DIR/compiled_res" "$WORK_DIR/gen" "$WORK_DIR/classes" "$WORK_DIR/dex" "$WORK_DIR/apk"

echo "[1/6] Compiling Android resources with aapt2..."
aapt2 compile --dir "$PROJECT_DIR/app/src/main/res" -o "$WORK_DIR/compiled_res/res.zip"

echo "[2/6] Linking resources & AndroidManifest.xml with aapt2..."
aapt2 link \
  -I "$PLATFORM_JAR" \
  --manifest "$PROJECT_DIR/app/src/main/AndroidManifest.xml" \
  --java "$WORK_DIR/gen" \
  --min-sdk-version 24 \
  --target-sdk-version 34 \
  --version-code "$VERSION_CODE" \
  --version-name "$VERSION_NAME" \
  -o "$WORK_DIR/apk/unsigned_unaligned.apk" \
  "$WORK_DIR/compiled_res/res.zip"

echo "[3/6] Compiling Java source files against Android API 34..."
JSCH_JAR="$PROJECT_DIR/app/libs/jsch-0.1.55.jar"
find "$PROJECT_DIR/app/src/main/java" "$WORK_DIR/gen" -name "*.java" > "$WORK_DIR/sources.txt"
"$JAVAC_BIN" \
  -source 8 -target 8 \
  -bootclasspath "$PLATFORM_JAR" \
  -encoding UTF-8 \
  -classpath "$PLATFORM_JAR:$JSCH_JAR" \
  -d "$WORK_DIR/classes" \
  @"$WORK_DIR/sources.txt"

echo "[4/6] Dexing bytecode with d8 (min-api 24)..."
jar cf "$WORK_DIR/app_classes.jar" -C "$WORK_DIR/classes" .
d8 \
  --release \
  --min-api 24 \
  --lib "$PLATFORM_JAR" \
  --output "$WORK_DIR/dex" \
  "$WORK_DIR/app_classes.jar" \
  "$JSCH_JAR"

(cd "$WORK_DIR/dex" && zip -q "$WORK_DIR/apk/unsigned_unaligned.apk" classes*.dex)

echo "[5/6] Aligning APK with zipalign..."
zipalign -f -p 4 "$WORK_DIR/apk/unsigned_unaligned.apk" "$WORK_DIR/apk/aligned.apk"
cp "$WORK_DIR/apk/aligned.apk" "$UNSIGNED_APK"

if [ ! -f "$KEYSTORE" ] || [ -z "$STOREPASS" ]; then
  echo "No SKID_KEYSTORE + SKID_STOREPASS — leaving UNSIGNED apk at $UNSIGNED_APK"
  echo "SUCCESS: Built unsigned Android APK -> $UNSIGNED_APK"
  exit 0
fi

echo "[6/6] Signing APK (v2 + v3) & verifying..."
mkdir -p "$(dirname "$OUT_APK")"
apksigner sign \
  --ks "$KEYSTORE" \
  --ks-key-alias "$KEY_ALIAS" \
  --ks-pass "pass:$STOREPASS" \
  --key-pass "pass:$KEYPASS" \
  --v1-signing-enabled true \
  --v2-signing-enabled true \
  --v3-signing-enabled true \
  --out "$OUT_APK" \
  "$WORK_DIR/apk/aligned.apk"

apksigner verify --verbose --print-certs "$OUT_APK"
echo "SUCCESS: Built signed Android APK -> $OUT_APK"
