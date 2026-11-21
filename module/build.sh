#!/bin/bash
set -e
export PATH=/usr/lib/jvm/java-17-openjdk-arm64/bin:$PATH
cd /root/workspace/xiaoai-mod
AJ=/root/workspace/android30.jar
R8=/root/workspace/r8.jar
OUT=build
rm -rf $OUT && mkdir -p $OUT/stub $OUT/classes $OUT/dex

echo "[1/6] xposed stubs"
javac --release 8 -Xlint:-options -cp $AJ -d $OUT/stub $(find stubs -name '*.java')
jar cf $OUT/stubs.jar -C $OUT/stub .

echo "[2/6] module classes"
javac --release 8 -Xlint:-options -cp $OUT/stubs.jar:$AJ -d $OUT/classes $(find src -name '*.java')
jar cf $OUT/classes.jar -C $OUT/classes .

echo "[3/6] d8"
java -cp $R8 com.android.tools.r8.D8 --release --min-api 26 --lib $AJ \
  --output $OUT/dex $OUT/classes.jar

echo "[4/6] aapt"
aapt package -f -M AndroidManifest.xml -A assets -I $AJ -F $OUT/base.apk

echo "[5/6] merge + align"
python3 - <<'PY'
import zipfile, shutil
shutil.copyfile('build/base.apk','build/unsigned.apk')
with zipfile.ZipFile('build/unsigned.apk','a') as z:
    z.write('build/dex/classes.dex','classes.dex',zipfile.ZIP_DEFLATED)
print("  dex merged")
PY
zipalign -f 4 $OUT/unsigned.apk $OUT/aligned.apk

echo "[6/6] sign"
[ -f dsh.keystore ] || keytool -genkeypair -keystore dsh.keystore -alias dsh -keyalg RSA \
  -keysize 2048 -validity 10000 -storepass android -keypass android -dname "CN=DSH, OU=App" >/dev/null 2>&1
apksigner sign --ks dsh.keystore --ks-pass pass:android --key-pass pass:android \
  --v1-signing-enabled true --v2-signing-enabled true \
  --out xiaoai-bridge.apk $OUT/aligned.apk
apksigner verify --print-certs xiaoai-bridge.apk | head -3
echo "OK -> $(pwd)/xiaoai-bridge.apk"
