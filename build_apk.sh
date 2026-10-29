#!/usr/bin/env bash
set -euo pipefail

PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"
SDK_ROOT="${ANDROID_SDK_ROOT:-/opt/android-sdk}"
BUILD_TOOLS="$SDK_ROOT/build-tools/34.0.0"
PLATFORM_JAR="$SDK_ROOT/platforms/android-34/android.jar"

export JAVA_HOME="${JAVA_HOME:-/usr/lib/jvm/java-21-openjdk-amd64}"
JAVAC_BIN="/usr/lib/jvm/jdk-11/bin/javac"
if [ ! -x "$JAVAC_BIN" ]; then
  JAVAC_BIN="$JAVA_HOME/bin/javac"
fi
export PATH="$JAVA_HOME/bin:$BUILD_TOOLS:$PATH"

WORK_DIR="/tmp/skidwaredns_build"
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
  --version-code 520 \
  --version-name "5.2.0-skid" \
  -o "$WORK_DIR/apk/unsigned_unaligned.apk" \
  "$WORK_DIR/compiled_res/res.zip"

echo "[3/6] Compiling Java source files against Android API 34..."
find "$PROJECT_DIR/app/src/main/java" "$WORK_DIR/gen" -name "*.java" > "$WORK_DIR/sources.txt"
"$JAVAC_BIN" \
  -source 8 -target 8 \
  -bootclasspath "$PLATFORM_JAR" \
  -encoding UTF-8 \
  -classpath "$PLATFORM_JAR" \
  -d "$WORK_DIR/classes" \
  @"$WORK_DIR/sources.txt"

echo "[4/6] Dexing bytecode with d8 (min-api 24)..."
jar cf "$WORK_DIR/app_classes.jar" -C "$WORK_DIR/classes" .
d8 \
  --release \
  --min-api 24 \
  --lib "$PLATFORM_JAR" \
  --output "$WORK_DIR/dex" \
  "$WORK_DIR/app_classes.jar"

(cd "$WORK_DIR/dex" && zip -q "$WORK_DIR/apk/unsigned_unaligned.apk" classes*.dex)

echo "[5/6] Aligning APK with zipalign..."
zipalign -f -p 4 "$WORK_DIR/apk/unsigned_unaligned.apk" "$WORK_DIR/apk/aligned.apk"

KEYSTORE="$PROJECT_DIR/skidwaredns-release.jks"
if [ ! -f "$KEYSTORE" ]; then
  echo "Generating SkidwareDNS signing keystore..."
  keytool -genkeypair \
    -keystore "$KEYSTORE" \
    -storepass skidware1337 \
    -keypass skidware1337 \
    -alias skidkey \
    -keyalg RSA \
    -keysize 2048 \
    -validity 10000 \
    -dname "CN=SkidwareDNS, OU=Security, O=Skidware, L=Seattle, ST=WA, C=US"
fi

echo "[6/6] Signing APK (v1 + v2 + v3 schemes) & verifying..."
OUT_APK_ROOT="/home/user/SkidwareDNS.apk"
OUT_APK_PROJ="$PROJECT_DIR/SkidwareDNS.apk"

apksigner sign \
  --ks "$KEYSTORE" \
  --ks-key-alias skidkey \
  --ks-pass pass:skidware1337 \
  --key-pass pass:skidware1337 \
  --v1-signing-enabled true \
  --v2-signing-enabled true \
  --v3-signing-enabled true \
  --out "$OUT_APK_ROOT" \
  "$WORK_DIR/apk/aligned.apk"

cp "$OUT_APK_ROOT" "$OUT_APK_PROJ"
apksigner verify --verbose --print-certs "$OUT_APK_ROOT"
echo "SUCCESS: Built signed Android APK -> $OUT_APK_ROOT"
